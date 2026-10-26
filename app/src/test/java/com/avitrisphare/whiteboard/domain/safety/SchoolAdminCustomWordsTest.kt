package com.avitrisphare.whiteboard.domain.safety

import com.avitrisphare.whiteboard.data.local.AuditLogStore
import com.avitrisphare.whiteboard.data.local.SchoolProfanityStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying Acceptance Criteria:
 * "School Admin can add custom words to the blocked list for their school"
 */
class SchoolAdminCustomWordsTest {

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
    fun schoolAdminCanAddCustomBlockedWordsForTheirSchool() = runBlocking {
        val schoolA = "school_dps_delhi"
        val schoolB = "school_dav_pune"
        val customSlang = "badnicknamedev"

        // Initially, the custom slang is not blocked in either school
        val initialCheckA = service.filterContent(
            text = "Hello $customSlang how are you",
            contentType = FilterContentType.LIVE_CLASS_CHAT,
            userId = "user_student_1",
            userName = "Aarav Sharma",
            role = "STUDENT",
            schoolId = schoolA
        )
        assertFalse(initialCheckA.isBlocked)

        // School Admin adds custom word for School A
        val added = service.addCustomBlockedWordsForSchool(
            schoolId = schoolA,
            words = listOf(customSlang),
            adminUserId = "admin_user_99"
        )
        assertTrue(added)

        // Verify the custom word is now in School A's list
        val schoolABlocked = service.getCustomBlockedWordsForSchool(schoolA)
        assertTrue(schoolABlocked.contains(customSlang))

        // Now, message in School A is BLOCKED with the exact user message
        val checkA = service.filterContent(
            text = "Hello $customSlang how are you",
            contentType = FilterContentType.LIVE_CLASS_CHAT,
            userId = "user_student_1",
            userName = "Aarav Sharma",
            role = "STUDENT",
            schoolId = schoolA
        )
        assertTrue(checkA.isBlocked)
        assertEquals("Inappropriate content detected. Please revise.", checkA.userMessage)
        assertTrue(checkA.matchedWords.contains(customSlang))

        // But School B is NOT affected by School A's custom blocked list!
        val checkB = service.filterContent(
            text = "Hello $customSlang how are you",
            contentType = FilterContentType.LIVE_CLASS_CHAT,
            userId = "user_student_2",
            userName = "Rohan Patil",
            role = "STUDENT",
            schoolId = schoolB
        )
        assertFalse("School B should not be affected by School A's custom blocklist", checkB.isBlocked)
    }

    @Test
    fun schoolAdminCanRemoveCustomBlockedWords() = runBlocking {
        val schoolId = "school_st_marys"
        val word = "prohibitedwordxyz"

        // Admin adds word
        service.addCustomBlockedWordsForSchool(schoolId, listOf(word), "admin_1")
        val blockedCheck = filter.checkContent("Test $word here", FilterContentType.ANNOUNCEMENT, schoolId)
        assertTrue(blockedCheck.isBlocked)

        // Admin removes word
        val removed = service.removeCustomBlockedWordForSchool(schoolId, word, "admin_1")
        assertTrue(removed)

        // Verify word is removed
        val unblockedCheck = filter.checkContent("Test $word here", FilterContentType.ANNOUNCEMENT, schoolId)
        assertFalse(unblockedCheck.isBlocked)
    }

    @Test
    fun adminCustomWordModificationsAreLoggedToAuditLog() = runBlocking {
        val schoolId = "school_kendriya_vidyalaya"
        val word = "unacceptablephrase"

        service.addCustomBlockedWordsForSchool(schoolId, listOf(word), "admin_kv_1")

        val schoolLogs = service.getAuditLogsForSchool(schoolId)
        val adminActionLog = schoolLogs.firstOrNull { it.actionType == AuditLogEntry.ACTION_CUSTOM_WORD_ADDED }
        assertTrue("Admin action must be logged in audit log", adminActionLog != null)
        assertEquals("admin_kv_1", adminActionLog?.userId)
        assertEquals("SCHOOL_ADMIN", adminActionLog?.role)
    }
}
