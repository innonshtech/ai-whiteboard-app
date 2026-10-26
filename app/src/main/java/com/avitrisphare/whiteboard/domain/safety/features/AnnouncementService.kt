package com.avitrisphare.whiteboard.domain.safety.features

import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Announcement entity model.
 */
data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val schoolId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val title: String,
    val messageBody: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AnnouncementResult {
    data class Success(val announcement: Announcement) : AnnouncementResult()
    data class Blocked(
        val reason: String,
        val matchedWords: List<String>
    ) : AnnouncementResult()
}

/**
 * Service managing announcements with profanity filtering.
 * Acceptance Criteria:
 * "Profanity filter applied to: announcements"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 */
@Singleton
class AnnouncementService @Inject constructor(
    private val profanityFilterService: ProfanityFilterService
) {
    private val inMemoryAnnouncements = mutableListOf<Announcement>()

    suspend fun createAnnouncement(
        schoolId: String,
        authorId: String,
        authorName: String,
        authorRole: String,
        title: String,
        messageBody: String
    ): AnnouncementResult {
        // Check title
        val titleCheck = profanityFilterService.filterContent(
            text = title,
            contentType = FilterContentType.ANNOUNCEMENT,
            userId = authorId,
            userName = authorName,
            role = authorRole,
            schoolId = schoolId
        )
        if (titleCheck.isBlocked) {
            return AnnouncementResult.Blocked(
                reason = titleCheck.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = titleCheck.matchedWords
            )
        }

        // Check message body
        val bodyCheck = profanityFilterService.filterContent(
            text = messageBody,
            contentType = FilterContentType.ANNOUNCEMENT,
            userId = authorId,
            userName = authorName,
            role = authorRole,
            schoolId = schoolId
        )
        if (bodyCheck.isBlocked) {
            return AnnouncementResult.Blocked(
                reason = bodyCheck.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = bodyCheck.matchedWords
            )
        }

        val announcement = Announcement(
            schoolId = schoolId,
            authorId = authorId,
            authorName = authorName,
            authorRole = authorRole,
            title = title,
            messageBody = messageBody
        )
        inMemoryAnnouncements.add(announcement)
        return AnnouncementResult.Success(announcement)
    }

    fun getAnnouncements(schoolId: String): List<Announcement> {
        return inMemoryAnnouncements.filter { it.schoolId == schoolId }.toList()
    }
}
