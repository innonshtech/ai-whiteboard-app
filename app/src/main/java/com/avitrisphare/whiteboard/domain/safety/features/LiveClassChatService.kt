package com.avitrisphare.whiteboard.domain.safety.features

import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live class chat message model.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val schoolId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ChatSendResult {
    data class Success(val message: ChatMessage) : ChatSendResult()
    data class Blocked(
        val reason: String,
        val matchedWords: List<String>
    ) : ChatSendResult()
}

/**
 * Service managing live class chat with profanity filtering.
 * Acceptance Criteria:
 * "Profanity filter applied to: live class chat"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 */
@Singleton
class LiveClassChatService @Inject constructor(
    private val profanityFilterService: ProfanityFilterService
) {
    private val inMemoryMessages = mutableListOf<ChatMessage>()

    suspend fun sendMessage(
        classId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        schoolId: String,
        text: String
    ): ChatSendResult {
        val check = profanityFilterService.filterContent(
            text = text,
            contentType = FilterContentType.LIVE_CLASS_CHAT,
            userId = senderId,
            userName = senderName,
            role = senderRole,
            schoolId = schoolId
        )

        if (check.isBlocked) {
            return ChatSendResult.Blocked(
                reason = check.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = check.matchedWords
            )
        }

        val message = ChatMessage(
            classId = classId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            schoolId = schoolId,
            text = text
        )
        inMemoryMessages.add(message)
        return ChatSendResult.Success(message)
    }

    fun getMessages(classId: String): List<ChatMessage> {
        return inMemoryMessages.filter { it.classId == classId }.toList()
    }
}
