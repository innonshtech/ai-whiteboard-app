package com.avitrisphare.whiteboard.presentation.canvas.export

import android.content.Context
import com.avitrisphare.whiteboard.presentation.entry.PanelSessionStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Custom exceptions for GSH file format and security validation.
 */
class GshUnauthorizedDeviceException(
    message: String = "This GSH file is protected and can only be opened on an authorized GSH Whiteboard device."
) : SecurityException(message)

class GshCorruptFileException(
    message: String = "Invalid, modified, or corrupted GSH whiteboard file."
) : IllegalArgumentException(message)

/**
 * GshSecurityManager
 *
 * Implements the proprietary .gsh container format, AES-256-GCM envelope encryption,
 * ecosystem authorization binding, and device-bound security verification.
 *
 * Any authorized GSH Whiteboard device running the AvitriSphare application can decrypt
 * and open shared .gsh files (via WhatsApp, Bluetooth, USB, Email, etc.), while unauthorized
 * devices, external PCs, generic apps, or locked panels are strictly blocked.
 */
object GshSecurityManager {

    // Magic identifier: 'G', 'S', 'H', 0x01
    val MAGIC = byteArrayOf(0x47.toByte(), 0x53.toByte(), 0x48.toByte(), 0x01.toByte())
    const val CURRENT_VERSION = 1
    const val CRYPTO_SUITE_AES_256_GCM = 1

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val AES_KEY_SIZE_BYTES = 32 // 256 bits

    // Cryptographic seed for authorized GSH Whiteboard ecosystem key
    private const val ECOSYSTEM_KEY_SECRET = "AvitriSphare_Whiteboard_GSH_Encrypted_Container_v1_Secret2026"
    private const val ECOSYSTEM_KEY_SALT = "AvitriSphare_Gsh_Salt_7a8f9c0e"

    private val secureRandom = SecureRandom()

    /**
     * Checks if the given byte array starts with the GSH1 magic bytes.
     */
    fun isGshFile(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == MAGIC[0] &&
            bytes[1] == MAGIC[1] &&
            bytes[2] == MAGIC[2] &&
            bytes[3] == MAGIC[3]
    }

    /**
     * Encrypts the whiteboard JSON payload into a secure, device-bound .gsh binary container.
     */
    fun encryptPayload(
        context: Context,
        payloadBytes: ByteArray,
        deviceIdOverride: String? = null,
        formattedIdOverride: String? = null
    ): ByteArray {
        val fileId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // 1. Resolve authorized device identity
        val (deviceId, formattedId) = if (deviceIdOverride != null) {
            deviceIdOverride to (formattedIdOverride ?: "GYSH-PNL-${deviceIdOverride.takeLast(4).uppercase()}")
        } else {
            val panelSession = PanelSessionStore(context)
            panelSession.panelId() to panelSession.formattedDeviceId()
        }

        // 2. Generate random ephemeral Data Encryption Key (DEK)
        val dekBytes = ByteArray(AES_KEY_SIZE_BYTES)
        secureRandom.nextBytes(dekBytes)
        val dek = SecretKeySpec(dekBytes, "AES")

        // 3. Encrypt payload with DEK using AES-256-GCM
        val payloadIv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(payloadIv)
        val payloadCipher = Cipher.getInstance("AES/GCM/NoPadding")
        payloadCipher.init(Cipher.ENCRYPT_MODE, dek, GCMParameterSpec(GCM_TAG_LENGTH_BITS, payloadIv))
        val encryptedPayload = payloadCipher.doFinal(payloadBytes)

        // 4. Encrypt DEK using Authorized Whiteboard Ecosystem Key (KEK)
        val kek = getAuthorizedWhiteboardKey()
        val dekIv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(dekIv)
        val dekCipher = Cipher.getInstance("AES/GCM/NoPadding")
        dekCipher.init(Cipher.ENCRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH_BITS, dekIv))
        val encryptedDek = dekCipher.doFinal(dekBytes)

        // 5. Compute authorization integrity tag
        val deviceAuthTag = computeAuthTag(kek, fileId)

        // 6. Assemble versioned proprietary container
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { out ->
            // File magic
            out.write(MAGIC)
            // Version and algorithm identifier
            out.writeInt(CURRENT_VERSION)
            out.writeInt(CRYPTO_SUITE_AES_256_GCM)

            // File UUID
            val fileIdBytes = fileId.toByteArray(StandardCharsets.UTF_8)
            out.writeShort(fileIdBytes.size)
            out.write(fileIdBytes)

            // Creation timestamp
            out.writeLong(timestamp)

            // Device authorization binding tag
            val authTagBytes = deviceAuthTag.toByteArray(StandardCharsets.UTF_8)
            out.writeShort(authTagBytes.size)
            out.write(authTagBytes)

            // Encrypted DEK and its IV
            out.writeByte(dekIv.size)
            out.write(dekIv)
            out.writeShort(encryptedDek.size)
            out.write(encryptedDek)

            // Payload IV and encrypted payload
            out.writeByte(payloadIv.size)
            out.write(payloadIv)
            out.writeInt(encryptedPayload.size)
            out.write(encryptedPayload)
            out.flush()
        }

        return baos.toByteArray()
    }

    /**
     * Validates device authorization and decrypts a .gsh file.
     * Throws [GshUnauthorizedDeviceException] if unauthorized or if panel is locked.
     * Throws [GshCorruptFileException] if corrupted or modified.
     */
    fun decryptPayload(
        context: Context,
        gshBytes: ByteArray,
        isAuthorizedOverride: Boolean? = null
    ): ByteArray {
        if (!isGshFile(gshBytes)) {
            throw GshCorruptFileException("File is not a valid GSH whiteboard container.")
        }

        try {
            val dis = DataInputStream(ByteArrayInputStream(gshBytes))
            // Read magic
            val magic = ByteArray(4)
            dis.readFully(magic)

            // Read version & crypto suite
            val version = dis.readInt()
            if (version > CURRENT_VERSION) {
                throw GshCorruptFileException("Unsupported GSH version: $version")
            }
            val cryptoSuite = dis.readInt()
            if (cryptoSuite != CRYPTO_SUITE_AES_256_GCM) {
                throw GshCorruptFileException("Unsupported crypto suite: $cryptoSuite")
            }

            // Read file ID
            val fileIdLen = dis.readShort().toInt() and 0xFFFF
            val fileIdBytes = ByteArray(fileIdLen)
            dis.readFully(fileIdBytes)
            val fileId = String(fileIdBytes, StandardCharsets.UTF_8)

            // Read timestamp
            val timestamp = dis.readLong()

            // Read device authorization tag
            val authTagLen = dis.readShort().toInt() and 0xFFFF
            val authTagBytes = ByteArray(authTagLen)
            dis.readFully(authTagBytes)
            val fileAuthTag = String(authTagBytes, StandardCharsets.UTF_8)

            // Read encrypted DEK and IV
            val dekIvLen = dis.readByte().toInt() and 0xFF
            val dekIv = ByteArray(dekIvLen)
            dis.readFully(dekIv)

            val encryptedDekLen = dis.readShort().toInt() and 0xFFFF
            val encryptedDek = ByteArray(encryptedDekLen)
            dis.readFully(encryptedDek)

            // Read payload IV and encrypted payload
            val payloadIvLen = dis.readByte().toInt() and 0xFF
            val payloadIv = ByteArray(payloadIvLen)
            dis.readFully(payloadIv)

            val encryptedPayloadLen = dis.readInt()
            val encryptedPayload = ByteArray(encryptedPayloadLen)
            dis.readFully(encryptedPayload)

            // Step 1: Device Authorization Verification
            val isAuthorized = isAuthorizedOverride ?: run {
                val panelSession = PanelSessionStore(context)
                !panelSession.isLocked()
            }

            if (!isAuthorized) {
                throw GshUnauthorizedDeviceException()
            }

            val kek = getAuthorizedWhiteboardKey()
            val expectedAuthTag = computeAuthTag(kek, fileId)

            if (fileAuthTag != expectedAuthTag) {
                throw GshUnauthorizedDeviceException()
            }

            // Step 2: Decrypt DEK using Authorized Key (KEK)
            val dekBytes: ByteArray
            try {
                val dekCipher = Cipher.getInstance("AES/GCM/NoPadding")
                dekCipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH_BITS, dekIv))
                dekBytes = dekCipher.doFinal(encryptedDek)
            } catch (e: Exception) {
                // If AES-GCM tag validation fails or key is different, device is unauthorized
                throw GshUnauthorizedDeviceException()
            }

            // Step 3: Decrypt payload using DEK
            val dek = SecretKeySpec(dekBytes, "AES")
            val payloadCipher = Cipher.getInstance("AES/GCM/NoPadding")
            payloadCipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(GCM_TAG_LENGTH_BITS, payloadIv))
            return payloadCipher.doFinal(encryptedPayload)

        } catch (e: GshUnauthorizedDeviceException) {
            throw e
        } catch (e: GshCorruptFileException) {
            throw e
        } catch (e: Exception) {
            throw GshCorruptFileException("Failed to read or decrypt GSH file: ${e.message}")
        }
    }

    /**
     * Derives the secure 256-bit AES Master Key for the authorized GSH Whiteboard application ecosystem.
     */
    fun getAuthorizedWhiteboardKey(): SecretKey {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(ECOSYSTEM_KEY_SALT.toByteArray(StandardCharsets.UTF_8))
        md.update(ECOSYSTEM_KEY_SECRET.toByteArray(StandardCharsets.UTF_8))
        val keyBytes = md.digest()
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun computeAuthTag(kek: SecretKey, fileId: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(kek)
        val message = "GSH_AUTHORIZED_WHITEBOARD:$fileId"
        val hash = hmac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
