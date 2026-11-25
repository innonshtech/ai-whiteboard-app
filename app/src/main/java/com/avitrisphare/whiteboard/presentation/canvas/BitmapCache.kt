package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File

/**
 * Thread-safe memory cache for decoded bitmaps (images, custom backgrounds, sticker icons).
 * Prevents redundant disk I/O and BitmapFactory decoding on the UI render thread.
 */
object BitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/8th of available runtime memory for bitmap cache
    private val cacheSize = (maxMemory / 8).coerceIn(16 * 1024, 64 * 1024)

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? {
        if (key.isBlank()) return null
        synchronized(lruCache) {
            val bmp = lruCache.get(key)
            if (bmp != null && !bmp.isRecycled) {
                return bmp
            }
            if (bmp != null && bmp.isRecycled) {
                lruCache.remove(key)
            }
            return null
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        if (key.isBlank() || bitmap.isRecycled) return
        synchronized(lruCache) {
            lruCache.put(key, bitmap)
        }
    }

    fun openInputStream(context: Context, uriString: String): java.io.InputStream? {
        if (uriString.isBlank()) return null
        return try {
            when {
                uriString.startsWith("data:image/") -> {
                    val comma = uriString.indexOf(',')
                    if (comma >= 0) {
                        val bytes = android.util.Base64.decode(uriString.substring(comma + 1), android.util.Base64.DEFAULT)
                        java.io.ByteArrayInputStream(bytes)
                    } else null
                }
                uriString.startsWith("/") -> {
                    val f = File(uriString)
                    if (f.exists() && f.canRead()) java.io.FileInputStream(f) else null
                }
                uriString.startsWith("file://") -> {
                    val path = Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                    val f = File(path)
                    if (f.exists() && f.canRead()) java.io.FileInputStream(f) else null
                }
                uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                    val conn = java.net.URL(uriString).openConnection()
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.getInputStream()
                }
                else -> {
                    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    // 1. Direct openInputStream
                    val fromStream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
                    if (fromStream != null) return fromStream

                    // 2. AssetFileDescriptor
                    val fromAfd = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.createInputStream() }.getOrNull()
                    if (fromAfd != null) return fromAfd

                    // 3. FileDescriptor
                    val fromPfd = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.let { java.io.FileInputStream(it.fileDescriptor) } }.getOrNull()
                    if (fromPfd != null) return fromPfd

                    // 4. Query _data column
                    val directPath = runCatching {
                        context.contentResolver.query(uri, arrayOf("_data"), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex("_data")
                                if (idx >= 0) cursor.getString(idx) else null
                            } else null
                        }
                    }.getOrNull()
                    if (!directPath.isNullOrBlank()) {
                        val f = File(directPath)
                        if (f.exists() && f.canRead()) return java.io.FileInputStream(f)
                    }

                    // 5. DocumentId fallback (primary:...)
                    val fromDocId = runCatching {
                        if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                            val docId = android.provider.DocumentsContract.getDocumentId(uri)
                            if (docId.startsWith("primary:")) {
                                val rel = docId.removePrefix("primary:")
                                val f = File(android.os.Environment.getExternalStorageDirectory(), rel)
                                if (f.exists() && f.canRead()) java.io.FileInputStream(f) else null
                            } else null
                        } else null
                    }.getOrNull()
                    if (fromDocId != null) return fromDocId

                    // 6. Direct file fallback if uri path exists
                    val uriPath = uri.path ?: ""
                    if (uriPath.contains("/storage/") || uriPath.contains("/mnt/")) {
                        val sub = uriPath.substring(uriPath.indexOf("/storage/").takeIf { it >= 0 } ?: uriPath.indexOf("/mnt/"))
                        val f = File(sub)
                        if (f.exists() && f.canRead()) return java.io.FileInputStream(f)
                    }

                    // 7. Download folder fallback by filename
                    val lastSegment = uri.lastPathSegment
                    if (!lastSegment.isNullOrBlank()) {
                        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                        val candidate = File(downloadDir, lastSegment)
                        if (candidate.exists() && candidate.canRead()) return java.io.FileInputStream(candidate)
                    }

                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BitmapCache", "Failed to open input stream for: $uriString", e)
            null
        }
    }

    fun openStreamBytes(context: Context, uriString: String): ByteArray? {
        if (uriString.isBlank()) return null
        return try {
            openInputStream(context, uriString)?.use { it.readBytes() }
        } catch (e: Exception) {
            android.util.Log.e("BitmapCache", "Failed to read bytes for: $uriString", e)
            null
        }
    }

    fun decodeSampledBitmap(
        context: Context,
        uriString: String,
        maxDim: Int = 2048
    ): Bitmap? {
        if (uriString.isBlank()) return null
        val cached = get(uriString)
        if (cached != null) return cached

        return try {
            // Case 1: Direct file on disk (fastest, no intermediate memory allocations)
            val filePath = when {
                uriString.startsWith("file://") -> Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                uriString.startsWith("/") -> uriString
                else -> {
                    val f = File(uriString)
                    if (f.exists()) f.absolutePath else null
                }
            }

            if (!filePath.isNullOrBlank()) {
                val file = File(filePath)
                if (file.exists() && file.canRead() && file.length() > 0) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                        var sample = 1
                        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                            sample *= 2
                        }
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val decoded = BitmapFactory.decodeFile(file.absolutePath, opts)
                        if (decoded != null) {
                            put(uriString, decoded)
                            if (filePath != uriString) put(filePath, decoded)
                            return decoded
                        }
                    }
                }
            }

            // Case 2: Content URI or stream
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openInputStream(context, uriString)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = openInputStream(context, uriString)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (decoded != null) {
                put(uriString, decoded)
            }
            decoded
        } catch (e: Exception) {
            android.util.Log.e("BitmapCache", "Failed to decode bitmap: $uriString", e)
            null
        }
    }

    fun clear() {
        synchronized(lruCache) {
            lruCache.evictAll()
        }
    }
}

