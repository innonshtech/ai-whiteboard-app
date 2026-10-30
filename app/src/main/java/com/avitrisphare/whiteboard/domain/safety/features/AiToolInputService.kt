package com.avitrisphare.whiteboard.domain.safety.features

import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import javax.inject.Inject
import javax.inject.Singleton

sealed class AiInputResult {
    data class Allowed(val cleanPrompt: String) : AiInputResult()
    data class Blocked(
        val reason: String,
        val matchedWords: List<String>
    ) : AiInputResult()
}

/**
 * Service protecting AI tools (SmartWrite, AI lesson plan generator, quiz generator,
 * AI assistant prompts, etc.) from inappropriate inputs.
 *
 * Acceptance Criteria:
 * "Profanity filter applied to: AI tool inputs"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 */
@Singleton
class AiToolInputService @Inject constructor(
    private val profanityFilterService: ProfanityFilterService
) {

    /**
     * Validates an AI tool input before sending it to any AI model or provider.
     * If profanity is detected, the request is rejected immediately, logged in the audit log,
     * and returns the exact user-facing message: "Inappropriate content detected. Please revise."
     */
    suspend fun validateAiInput(
        prompt: String,
        userId: String,
        userName: String,
        role: String,
        schoolId: String,
        toolName: String = "AI Assistant"
    ): AiInputResult {
        val check = profanityFilterService.filterContent(
            text = prompt,
            contentType = FilterContentType.AI_INPUT,
            userId = userId,
            userName = userName,
            role = role,
            schoolId = schoolId
        )

        return if (check.isBlocked) {
            AiInputResult.Blocked(
                reason = check.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = check.matchedWords
            )
        } else {
            AiInputResult.Allowed(prompt)
        }
    }
}
