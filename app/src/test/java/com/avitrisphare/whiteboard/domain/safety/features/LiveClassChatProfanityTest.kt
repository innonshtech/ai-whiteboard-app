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
 * "Profanity filter applied to: live class chat"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 * "Profanity attempts logged in audit log per user"
 */
class LiveClassChatProfanityTest {

    private lateinit var auditLogStore: AuditLogStore
    private lateinit var schoolProfanityStore: SchoolProfanityStore
    private lateinit var profanityFilter: ProfanityFilter
    private lateinit var profanityFilterService: ProfanityFilterService
    private lateinit var chatService: LiveClassChatService

    @Before
    fun setUp() {
        auditLogStore = AuditLogStore(context = null)
        schoolProfanityStore = SchoolProfanityStore(context = null, auditLogRepository = auditLogStore)
        profanityFilter = ProfanityFilter(schoolProfanityStore)
        profanityFilterService = ProfanityFilterService(profanityFilter, auditLogStore, schoolProfanityStore)
        chatService = LiveClassChatService(profanityFilterService)
    }

    @Test
    fun safeLiveClassChatMessageIsSentSuccessfully() = runBlocking {
        val result = chatService.sendMessage(
            classId = "class_10A",
            senderId = "student_1",
            senderName = "Sneha Joshi",
            senderRole = "STUDENT",
            schoolId = "school_1",
            text = "Hello teacher, could you repeat the formula for acceleration?"
        )

        assertTrue(result is ChatSendResult.Success)
        val sent = (result as ChatSendResult.Success).message
        assertEquals("class_10A", sent.classId)
        assertEquals("student_1", sent.senderId)
        assertEquals("Hello teacher, could you repeat the formula for acceleration?", sent.text)

        val messages = chatService.getMessages("class_10A")
        assertEquals(1, messages.size)
    }

    @Test
    fun profaneLiveClassChatMessageIsBlockedWithExactUserMessageAndLogged() = runBlocking {
        val senderId = "student_bad_actor"
        val senderName = "Troublemaker"
        val schoolId = "school_1"

        val result = chatService.sendMessage(
            classId = "class_10A",
            senderId = senderId,
            senderName = senderName,
            senderRole = "STUDENT",
            schoolId = schoolId,
            text = "Shut up you fucking idiot"
        )

        assertTrue(result is ChatSendResult.Blocked)
        val blocked = result as ChatSendResult.Blocked
        assertEquals(
            "Inappropriate content detected. Please revise.",
            blocked.reason
        )
        assertTrue(blocked.matchedWords.contains("fucking"))

        // Verify message was NOT added to chat room
        val messages = chatService.getMessages("class_10A")
        assertTrue(messages.isEmpty())

        // Verify violation was logged in audit log per user
        val logs = auditLogStore.getLogsForUser(senderId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.LIVE_CLASS_CHAT, logs.first().contentType)
        assertEquals(senderId, logs.first().userId)
    }
}
