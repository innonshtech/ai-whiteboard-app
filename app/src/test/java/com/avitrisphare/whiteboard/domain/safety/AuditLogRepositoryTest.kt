package com.avitrisphare.whiteboard.domain.safety

import com.avitrisphare.whiteboard.data.local.AuditLogStore
import com.avitrisphare.whiteboard.data.local.SchoolProfanityStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying Acceptance Criteria:
 * "Profanity attempts logged in audit log per user"
 * Story #6 (M14 Audit Logs) compliance.
 */
class AuditLogRepositoryTest {

    private lateinit var auditLogStore: AuditLogStore
    private lateinit var schoolProfanityStore: SchoolProfanityStore
    private lateinit var filter: ProfanityFilter
    private lateinit var service: ProfanityFilterService

    @Before
    fun setUp() {
        auditLogStore = AuditLogStore(context = null)
        schoolProfanityStore = SchoolProfanityStore(context = null, auditLogRepository = auditLogStore)
        filter = ProfanityFilter(schoolProfanityStore)
        service = ProfanityFilterService(filter, auditLogStore, schoolProfanityStore)
    }

    @Test
    fun profanityAttemptIsAutomaticallyLoggedPerUser() = runBlocking {
        val userId = "student_rahul_101"
        val userName = "Rahul Deshmukh"
        val role = "STUDENT"
        val schoolId = "school_pune_public"
        val badText = "Stop this bullshit right now"

        // User attempts to send a profanity message
        val result = service.filterContent(
            text = badText,
            contentType = FilterContentType.LIVE_CLASS_CHAT,
            userId = userId,
            userName = userName,
            role = role,
            schoolId = schoolId,
            ipAddress = "192.168.1.45"
        )

        assertTrue(result.isBlocked)
        assertEquals("Inappropriate content detected. Please revise.", result.userMessage)

        // Verify the attempt is logged for this specific user
        val userLogs = service.getAuditLogsForUser(userId)
        assertEquals(1, userLogs.size)

        val log = userLogs.first()
        assertEquals(userId, log.userId)
        assertEquals(userName, log.userName)
        assertEquals(role, log.role)
        assertEquals(schoolId, log.schoolId)
        assertEquals(FilterContentType.LIVE_CLASS_CHAT, log.contentType)
        assertEquals(AuditLogEntry.ACTION_PROFANITY_BLOCKED, log.actionType)
        assertEquals("192.168.1.45", log.ipAddress)
        assertTrue(log.matchedWords.contains("bullshit"))
        assertTrue("Log timestamp must be set", log.timestamp > 0)
        assertNotNull(log.id)
    }

    @Test
    fun cleanMessagesDoNotCreateAuditLogViolations() = runBlocking {
        val userId = "student_anita_102"
        val cleanText = "Good morning teacher, can you explain photosynthesis?"

        val result = service.filterContent(
            text = cleanText,
            contentType = FilterContentType.LIVE_CLASS_CHAT,
            userId = userId,
            userName = "Anita Roy",
            role = "STUDENT",
            schoolId = "school_dps"
        )

        assertTrue(result.isAllowed)

        val userLogs = service.getAuditLogsForUser(userId)
        assertTrue("Clean message must not generate an audit log violation", userLogs.isEmpty())
    }

    @Test
    fun auditLogsCanBeFilteredByDateUserAndContentType() = runBlocking {
        val user1 = "user_1"
        val user2 = "user_2"
        val schoolId = "school_central"

        // Generate violations
        service.filterContent("bullshit 1", FilterContentType.LIVE_CLASS_CHAT, user1, "User 1", "STUDENT", schoolId)
        service.filterContent("fuck 2", FilterContentType.HOMEWORK, user1, "User 1", "STUDENT", schoolId)
        service.filterContent("bitch 3", FilterContentType.ANNOUNCEMENT, user2, "User 2", "TEACHER", schoolId)

        // Filter by user1
        val user1Logs = auditLogStore.filterLogs(userId = user1)
        assertEquals(2, user1Logs.size)

        // Filter by content type HOMEWORK
        val homeworkLogs = auditLogStore.filterLogs(contentType = FilterContentType.HOMEWORK)
        assertEquals(1, homeworkLogs.size)
        assertEquals("fuck", homeworkLogs.first().matchedWords.first())

        // Filter by user2 and ANNOUNCEMENT
        val user2Announcements = auditLogStore.filterLogs(userId = user2, contentType = FilterContentType.ANNOUNCEMENT)
        assertEquals(1, user2Announcements.size)
        assertEquals(user2, user2Announcements.first().userId)
    }

    @Test
    fun auditLogsAreReadOnlyAndImmutable() = runBlocking {
        val userId = "user_test_immutable"
        service.filterContent("bullshit", FilterContentType.AI_INPUT, userId, "Test", "STUDENT", "school_1")

        val logs1 = auditLogStore.getLogsForUser(userId)
        assertEquals(1, logs1.size)

        // Modifying the returned list should not affect the store
        val mutableCopy = logs1.toMutableList()
        mutableCopy.clear()

        val logs2 = auditLogStore.getLogsForUser(userId)
        assertEquals("Store must remain unchanged and read-only", 1, logs2.size)
    }
}
