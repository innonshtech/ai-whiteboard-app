package com.avitrisphare.whiteboard.domain.safety

/**
 * Repository interface for managing school-specific profanity blocklists and allowlists.
 * Acceptance Criteria:
 * "School Admin can add custom words to the blocked list for their school"
 */
interface SchoolProfanityRepository {

    /**
     * Gets all custom blocked words configured for a specific school.
     */
    suspend fun getCustomBlockedWords(schoolId: String): Set<String>

    /**
     * Adds custom blocked words for a specific school.
     * Records the action in the audit log.
     */
    suspend fun addCustomBlockedWords(
        schoolId: String,
        words: List<String>,
        adminUserId: String
    ): Boolean

    /**
     * Removes a custom blocked word for a specific school.
     */
    suspend fun removeCustomBlockedWord(
        schoolId: String,
        word: String,
        adminUserId: String
    ): Boolean

    /**
     * Gets all custom allowed words for a specific school.
     */
    suspend fun getCustomAllowedWords(schoolId: String): Set<String>

    /**
     * Adds custom allowed words for a specific school.
     */
    suspend fun addCustomAllowedWords(
        schoolId: String,
        words: List<String>,
        adminUserId: String
    ): Boolean

    /**
     * Removes a custom allowed word for a specific school.
     */
    suspend fun removeCustomAllowedWord(
        schoolId: String,
        word: String,
        adminUserId: String
    ): Boolean
}
