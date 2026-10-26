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
 * "Profanity filter applied to: AI tool inputs"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 * "Profanity attempts logged in audit log per user"
 */
class AiToolInputProfanityTest {

    private lateinit var auditLogStore: AuditLogStore
    private lateinit var schoolProfanityStore: SchoolProfanityStore
    private lateinit var profanityFilter: ProfanityFilter
    private lateinit var profanityFilterService: ProfanityFilterService
    private lateinit var aiService: AiToolInputService

    @Before
    fun setUp() {
        auditLogStore = AuditLogStore(context = null)
        schoolProfanityStore = SchoolProfanityStore(context = null, auditLogRepository = auditLogStore)
        profanityFilter = ProfanityFilter(schoolProfanityStore)
        profanityFilterService = ProfanityFilterService(profanityFilter, auditLogStore, schoolProfanityStore)
        aiService = AiToolInputService(profanityFilterService)
    }

    @Test
    fun safeAiPromptIsAllowed() = runBlocking {
        val result = aiService.validateAiInput(
            prompt = "Generate a 5-question quiz on photosynthesis for Class 8 CBSE.",
            userId = "teacher_10",
            userName = "Dr. Mehta",
            role = "TEACHER",
            schoolId = "school_1",
            toolName = "Lesson Generator"
        )

        assertTrue(result is AiInputResult.Allowed)
        assertEquals(
            "Generate a 5-question quiz on photosynthesis for Class 8 CBSE.",
            (result as AiInputResult.Allowed).cleanPrompt
        )
    }

    @Test
    fun profaneAiPromptIsBlockedWithExactMessageAndLogged() = runBlocking {
        val userId = "user_abuser"
        val result = aiService.validateAiInput(
            prompt = "Write an essay about why this teacher is a motherfucker and a bitch",
            userId = userId,
            userName = "Abusive User",
            role = "STUDENT",
            schoolId = "school_1",
            toolName = "SmartWrite AI"
        )

        assertTrue(result is AiInputResult.Blocked)
        val blocked = result as AiInputResult.Blocked
        assertEquals("Inappropriate content detected. Please revise.", blocked.reason)

        // Verify violation is logged in audit log per user
        val logs = auditLogStore.getLogsForUser(userId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.AI_INPUT, logs.first().contentType)
        assertEquals(userId, logs.first().userId)
    }

    @Test
    fun hindiProfaneAiPromptIsBlockedAndLogged() = runBlocking {
        val userId = "user_hindi_abuser"
        val result = aiService.validateAiInput(
            prompt = "AI mujhe batao ki kaise iss chutiya insaan ko sabak sikhaye",
            userId = userId,
            userName = "Hindi Abuser",
            role = "STUDENT",
            schoolId = "school_1"
        )

        assertTrue(result is AiInputResult.Blocked)
        val blocked = result as AiInputResult.Blocked
        assertEquals("Inappropriate content detected. Please revise.", blocked.reason)

        val logs = auditLogStore.getLogsForUser(userId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.AI_INPUT, logs.first().contentType)
    }
}
