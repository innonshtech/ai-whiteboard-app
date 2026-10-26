package com.avitrisphare.whiteboard.domain.safety.features

import com.avitrisphare.whiteboard.data.local.AuditLogStore
import com.avitrisphare.whiteboard.data.local.SchoolProfanityStore
import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilter
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying Acceptance Criteria:
 * "Profanity filter applied to: announcements"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 * "Profanity attempts logged in audit log per user"
 */
class AnnouncementProfanityTest {

    private lateinit var auditLogStore: AuditLogStore
    private lateinit var schoolProfanityStore: SchoolProfanityStore
    private lateinit var profanityFilter: ProfanityFilter
    private lateinit var profanityFilterService: ProfanityFilterService
    private lateinit var announcementService: AnnouncementService

    @Before
    fun setUp() {
        auditLogStore = AuditLogStore(context = null)
        schoolProfanityStore = SchoolProfanityStore(context = null, auditLogRepository = auditLogStore)
        profanityFilter = ProfanityFilter(schoolProfanityStore)
        profanityFilterService = ProfanityFilterService(profanityFilter, auditLogStore, schoolProfanityStore)
        announcementService = AnnouncementService(profanityFilterService)
    }

    @Test
    fun safeAnnouncementIsCreatedSuccessfully() = runBlocking {
        val result = announcementService.createAnnouncement(
            schoolId = "school_1",
            authorId = "teacher_1",
            authorName = "Mrs. Sunita Verma",
            authorRole = "TEACHER",
            title = "Annual Science Exhibition Next Friday",
            messageBody = "All students from Class 8 to 10 are requested to submit their science project models."
        )

        assertTrue(result is AnnouncementResult.Success)
        val created = (result as AnnouncementResult.Success).announcement
        assertEquals("Annual Science Exhibition Next Friday", created.title)

        val list = announcementService.getAnnouncements("school_1")
        assertEquals(1, list.size)
    }

    @Test
    fun announcementWithProfaneTitleIsBlockedAndLogged() = runBlocking {
        val authorId = "user_bad_author"
        val result = announcementService.createAnnouncement(
            schoolId = "school_1",
            authorId = authorId,
            authorName = "Bad Author",
            authorRole = "TEACHER",
            title = "This school is bullshit",
            messageBody = "Normal message body here."
        )

        assertTrue(result is AnnouncementResult.Blocked)
        val blocked = result as AnnouncementResult.Blocked
        assertEquals("Inappropriate content detected. Please revise.", blocked.reason)

        val logs = auditLogStore.getLogsForUser(authorId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.ANNOUNCEMENT, logs.first().contentType)
    }

    @Test
    fun announcementWithProfaneBodyIsBlockedAndLogged() = runBlocking {
        val authorId = "user_bad_body"
        val result = announcementService.createAnnouncement(
            schoolId = "school_1",
            authorId = authorId,
            authorName = "Bad Body",
            authorRole = "TEACHER",
            title = "Exam Schedule",
            messageBody = "All students are chutiya and don't study."
        )

        assertTrue(result is AnnouncementResult.Blocked)
        val blocked = result as AnnouncementResult.Blocked
        assertEquals("Inappropriate content detected. Please revise.", blocked.reason)

        val logs = auditLogStore.getLogsForUser(authorId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.ANNOUNCEMENT, logs.first().contentType)
    }
}
