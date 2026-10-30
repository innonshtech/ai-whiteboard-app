package com.avitrisphare.whiteboard.domain.safety

import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level coordinator service for safety and profanity moderation across all user-generated content.
 * Enforces:
 * - Content inspection across English, Hindi, and Marathi
 * - Educational allowlist immunity
 * - Custom school-level blocklists
 * - Automatic audit logging per user upon profanity detection
 * - Standardized user-facing rejection message: "Inappropriate content detected. Please revise."
 */
@Singleton
class ProfanityFilterService @Inject constructor(
    private val profanityFilter: ProfanityFilter,
    private val auditLogRepository: AuditLogRepository,
    private val schoolProfanityRepository: SchoolProfanityRepository
) {

    /**
     * Inspects text content for profanity, blocks inappropriate content,
     * and logs any detected violations to the audit log per user.
     */
    suspend fun filterContent(
        text: String,
        contentType: FilterContentType,
        userId: String,
        userName: String,
        role: String,
        schoolId: String,
        ipAddress: String = "127.0.0.1"
    ): ProfanityCheckResult {
        val result = profanityFilter.checkContent(
            text = text,
            contentType = contentType,
            schoolId = schoolId
        )

        if (result.isBlocked) {
            // Log violation attempt in the persistent audit log per user (Story #89 & Story #6)
            auditLogRepository.recordAttempt(
                AuditLogEntry(
                    userId = userId,
                    userName = userName,
                    role = role,
                    schoolId = schoolId,
                    actionType = AuditLogEntry.ACTION_PROFANITY_BLOCKED,
                    contentType = contentType,
                    matchedWords = result.matchedWords,
                    contentSnippet = createSafeSnippet(text, result.matchedWords),
                    ipAddress = ipAddress
                )
            )
        }

        return result
    }

    /**
     * School Admin adds custom blocked words for their school.
     */
    suspend fun addCustomBlockedWordsForSchool(
        schoolId: String,
        words: List<String>,
        adminUserId: String
    ): Boolean {
        return schoolProfanityRepository.addCustomBlockedWords(schoolId, words, adminUserId)
    }

    /**
     * School Admin removes a custom blocked word for their school.
     */
    suspend fun removeCustomBlockedWordForSchool(
        schoolId: String,
        word: String,
        adminUserId: String
    ): Boolean {
        return schoolProfanityRepository.removeCustomBlockedWord(schoolId, word, adminUserId)
    }

    /**
     * Retrieves custom blocked words for a school.
     */
    suspend fun getCustomBlockedWordsForSchool(schoolId: String): Set<String> {
        return schoolProfanityRepository.getCustomBlockedWords(schoolId)
    }

    /**
     * Queries audit logs for a specific user.
     */
    suspend fun getAuditLogsForUser(userId: String): List<AuditLogEntry> {
        return auditLogRepository.getLogsForUser(userId)
    }

    /**
     * Queries audit logs for a school.
     */
    suspend fun getAuditLogsForSchool(schoolId: String): List<AuditLogEntry> {
        return auditLogRepository.getLogsForSchool(schoolId)
    }

    private fun createSafeSnippet(rawText: String, matchedWords: List<String>): String {
        var masked = rawText
        for (word in matchedWords) {
            val replacement = "*".repeat(word.length)
            masked = masked.replace(Regex("(?i)" + Regex.escape(word)), replacement)
        }
        return if (masked.length > 100) masked.take(97) + "..." else masked
    }
}
