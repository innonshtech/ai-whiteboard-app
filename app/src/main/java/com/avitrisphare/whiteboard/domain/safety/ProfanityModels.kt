package com.avitrisphare.whiteboard.domain.safety

import java.util.UUID

/**
 * Standard user-facing message required by Acceptance Criteria:
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 */
const val PROFANITY_BLOCKED_USER_MESSAGE = "Inappropriate content detected. Please revise."

/**
 * Content categories covered by the profanity filter as per Acceptance Criteria:
 * - Live class chat
 * - Announcements
 * - Homework text
 * - AI tool inputs
 */
enum class FilterContentType(val displayName: String) {
    LIVE_CLASS_CHAT("Live Class Chat"),
    ANNOUNCEMENT("Announcement"),
    HOMEWORK("Homework"),
    AI_INPUT("AI Tool Input")
}

/**
 * Languages supported by the filter as per Acceptance Criteria:
 * - English
 * - Hindi
 * - Marathi
 * Plus custom school-specific lists.
 */
enum class ProfanityLanguage(val displayName: String) {
    ENGLISH("English"),
    HINDI("Hindi"),
    MARATHI("Marathi"),
    CUSTOM("Custom School Rule")
}

/**
 * Result of checking content against the profanity filter.
 */
data class ProfanityCheckResult(
    val isBlocked: Boolean,
    val userMessage: String? = if (isBlocked) PROFANITY_BLOCKED_USER_MESSAGE else null,
    val matchedWords: List<String> = emptyList(),
    val detectedLanguages: Set<ProfanityLanguage> = emptySet(),
    val contentType: FilterContentType,
    val cleanedText: String? = null
) {
    val isAllowed: Boolean get() = !isBlocked

    companion object {
        fun allowed(contentType: FilterContentType, text: String): ProfanityCheckResult {
            return ProfanityCheckResult(
                isBlocked = false,
                userMessage = null,
                matchedWords = emptyList(),
                detectedLanguages = emptySet(),
                contentType = contentType,
                cleanedText = text
            )
        }

        fun blocked(
            contentType: FilterContentType,
            matchedWords: List<String>,
            languages: Set<ProfanityLanguage>
        ): ProfanityCheckResult {
            return ProfanityCheckResult(
                isBlocked = true,
                userMessage = PROFANITY_BLOCKED_USER_MESSAGE,
                matchedWords = matchedWords,
                detectedLanguages = languages,
                contentType = contentType,
                cleanedText = null
            )
        }
    }
}

/**
 * Audit log record for tracking profanity attempts per user as per Acceptance Criteria:
 * "Profanity attempts logged in audit log per user".
 * Cross-references Story #6 (M14 Audit Logs):
 * - Records action type, user ID, role, IP address, timestamp
 * - Read-only (immutable once created)
 */
data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String,
    val userName: String,
    val role: String,
    val schoolId: String,
    val actionType: String = ACTION_PROFANITY_BLOCKED,
    val contentType: FilterContentType,
    val matchedWords: List<String>,
    val contentSnippet: String,
    val ipAddress: String = "127.0.0.1"
) {
    companion object {
        const val ACTION_PROFANITY_BLOCKED = "PROFANITY_BLOCKED"
        const val ACTION_CUSTOM_WORD_ADDED = "CUSTOM_WORD_ADDED"
        const val ACTION_CUSTOM_WORD_REMOVED = "CUSTOM_WORD_REMOVED"
    }
}

/**
 * School-specific profanity filter configuration.
 * Allows School Admins to customize blocked and allowed words.
 */
data class SchoolProfanityConfig(
    val schoolId: String,
    val customBlockedWords: Set<String> = emptySet(),
    val customAllowedWords: Set<String> = emptySet()
)
