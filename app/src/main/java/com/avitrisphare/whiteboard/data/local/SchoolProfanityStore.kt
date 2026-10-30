package com.avitrisphare.whiteboard.data.local

import android.content.Context
import com.avitrisphare.whiteboard.domain.safety.AuditLogEntry
import com.avitrisphare.whiteboard.domain.safety.AuditLogRepository
import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.SchoolProfanityConfig
import com.avitrisphare.whiteboard.domain.safety.SchoolProfanityRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent thread-safe store for school-specific custom blocked words and allowlists.
 * Acceptance Criteria:
 * "School Admin can add custom words to the blocked list for their school"
 */
@Singleton
class SchoolProfanityStore @Inject constructor(
    private val context: Context? = null,
    private val auditLogRepository: AuditLogRepository? = null,
    private val gson: Gson = Gson()
) : SchoolProfanityRepository {

    private val mutex = Mutex()
    private val configsBySchool = mutableMapOf<String, SchoolProfanityConfig>()
    private val configFileName = "school_profanity_configs.json"

    init {
        loadConfigsFromDisk()
    }

    override suspend fun getCustomBlockedWords(schoolId: String): Set<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            configsBySchool[schoolId]?.customBlockedWords ?: emptySet()
        }
    }

    override suspend fun addCustomBlockedWords(
        schoolId: String,
        words: List<String>,
        adminUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val sanitized = words.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
        if (sanitized.isEmpty()) return@withContext false

        mutex.withLock {
            val current = configsBySchool[schoolId] ?: SchoolProfanityConfig(schoolId)
            val updated = current.copy(
                customBlockedWords = current.customBlockedWords + sanitized,
                customAllowedWords = current.customAllowedWords - sanitized.toSet()
            )
            configsBySchool[schoolId] = updated
            saveConfigsToDisk()
        }

        // Log admin content blocking configuration change to audit log (Story #6 & Story #89)
        auditLogRepository?.recordAttempt(
            AuditLogEntry(
                userId = adminUserId,
                userName = "School Admin",
                role = "SCHOOL_ADMIN",
                schoolId = schoolId,
                actionType = AuditLogEntry.ACTION_CUSTOM_WORD_ADDED,
                contentType = FilterContentType.ANNOUNCEMENT,
                matchedWords = sanitized,
                contentSnippet = "Added custom blocked words: ${sanitized.joinToString(", ")}"
            )
        )
        true
    }

    override suspend fun removeCustomBlockedWord(
        schoolId: String,
        word: String,
        adminUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val target = word.trim().lowercase(Locale.ROOT)
        var removed = false

        mutex.withLock {
            val current = configsBySchool[schoolId] ?: return@withContext false
            if (current.customBlockedWords.contains(target)) {
                configsBySchool[schoolId] = current.copy(
                    customBlockedWords = current.customBlockedWords - target
                )
                saveConfigsToDisk()
                removed = true
            }
        }

        if (removed) {
            auditLogRepository?.recordAttempt(
                AuditLogEntry(
                    userId = adminUserId,
                    userName = "School Admin",
                    role = "SCHOOL_ADMIN",
                    schoolId = schoolId,
                    actionType = AuditLogEntry.ACTION_CUSTOM_WORD_REMOVED,
                    contentType = FilterContentType.ANNOUNCEMENT,
                    matchedWords = listOf(target),
                    contentSnippet = "Removed custom blocked word: $target"
                )
            )
        }
        removed
    }

    override suspend fun getCustomAllowedWords(schoolId: String): Set<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            configsBySchool[schoolId]?.customAllowedWords ?: emptySet()
        }
    }

    override suspend fun addCustomAllowedWords(
        schoolId: String,
        words: List<String>,
        adminUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val sanitized = words.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
        if (sanitized.isEmpty()) return@withContext false

        mutex.withLock {
            val current = configsBySchool[schoolId] ?: SchoolProfanityConfig(schoolId)
            val updated = current.copy(
                customAllowedWords = current.customAllowedWords + sanitized,
                customBlockedWords = current.customBlockedWords - sanitized.toSet()
            )
            configsBySchool[schoolId] = updated
            saveConfigsToDisk()
        }
        true
    }

    override suspend fun removeCustomAllowedWord(
        schoolId: String,
        word: String,
        adminUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val target = word.trim().lowercase(Locale.ROOT)
        mutex.withLock {
            val current = configsBySchool[schoolId] ?: return@withContext false
            if (current.customAllowedWords.contains(target)) {
                configsBySchool[schoolId] = current.copy(
                    customAllowedWords = current.customAllowedWords - target
                )
                saveConfigsToDisk()
                true
            } else false
        }
    }

    private fun loadConfigsFromDisk() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, configFileName)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<Map<String, SchoolProfanityConfig>>() {}.type
                val map: Map<String, SchoolProfanityConfig>? = gson.fromJson(json, type)
                if (map != null) {
                    configsBySchool.putAll(map)
                }
            }
        } catch (_: Exception) {
            // Keep memory configs intact
        }
    }

    private fun saveConfigsToDisk() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, configFileName)
            val json = gson.toJson(configsBySchool)
            file.writeText(json)
        } catch (_: Exception) {
            // Memory configs preserved
        }
    }
}
