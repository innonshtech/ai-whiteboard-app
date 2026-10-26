package com.avitrisphare.whiteboard.domain.safety.features

import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Homework assignment entity.
 */
data class HomeworkAssignment(
    val id: String = UUID.randomUUID().toString(),
    val schoolId: String,
    val classroomId: String,
    val teacherId: String,
    val teacherName: String,
    val title: String,
    val instructions: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Student homework submission entity.
 */
data class HomeworkSubmission(
    val id: String = UUID.randomUUID().toString(),
    val homeworkId: String,
    val schoolId: String,
    val studentId: String,
    val studentName: String,
    val submissionText: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class HomeworkOpResult<out T> {
    data class Success<T>(val data: T) : HomeworkOpResult<T>()
    data class Blocked(
        val reason: String,
        val matchedWords: List<String>
    ) : HomeworkOpResult<Nothing>()
}

/**
 * Service managing homework creation and student submission with profanity filtering.
 * Acceptance Criteria:
 * "Profanity filter applied to: homework text"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 */
@Singleton
class HomeworkService @Inject constructor(
    private val profanityFilterService: ProfanityFilterService
) {
    private val assignments = mutableListOf<HomeworkAssignment>()
    private val submissions = mutableListOf<HomeworkSubmission>()

    /**
     * Teacher creates homework assignment. Both title and instructions are checked.
     */
    suspend fun createHomework(
        schoolId: String,
        classroomId: String,
        teacherId: String,
        teacherName: String,
        title: String,
        instructions: String
    ): HomeworkOpResult<HomeworkAssignment> {
        val titleCheck = profanityFilterService.filterContent(
            text = title,
            contentType = FilterContentType.HOMEWORK,
            userId = teacherId,
            userName = teacherName,
            role = "TEACHER",
            schoolId = schoolId
        )
        if (titleCheck.isBlocked) {
            return HomeworkOpResult.Blocked(
                reason = titleCheck.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = titleCheck.matchedWords
            )
        }

        val instructionsCheck = profanityFilterService.filterContent(
            text = instructions,
            contentType = FilterContentType.HOMEWORK,
            userId = teacherId,
            userName = teacherName,
            role = "TEACHER",
            schoolId = schoolId
        )
        if (instructionsCheck.isBlocked) {
            return HomeworkOpResult.Blocked(
                reason = instructionsCheck.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = instructionsCheck.matchedWords
            )
        }

        val assignment = HomeworkAssignment(
            schoolId = schoolId,
            classroomId = classroomId,
            teacherId = teacherId,
            teacherName = teacherName,
            title = title,
            instructions = instructions
        )
        assignments.add(assignment)
        return HomeworkOpResult.Success(assignment)
    }

    /**
     * Student submits homework response text. Submission text is checked.
     */
    suspend fun submitHomework(
        homeworkId: String,
        schoolId: String,
        studentId: String,
        studentName: String,
        submissionText: String
    ): HomeworkOpResult<HomeworkSubmission> {
        val check = profanityFilterService.filterContent(
            text = submissionText,
            contentType = FilterContentType.HOMEWORK,
            userId = studentId,
            userName = studentName,
            role = "STUDENT",
            schoolId = schoolId
        )

        if (check.isBlocked) {
            return HomeworkOpResult.Blocked(
                reason = check.userMessage ?: "Inappropriate content detected. Please revise.",
                matchedWords = check.matchedWords
            )
        }

        val submission = HomeworkSubmission(
            homeworkId = homeworkId,
            schoolId = schoolId,
            studentId = studentId,
            studentName = studentName,
            submissionText = submissionText
        )
        submissions.add(submission)
        return HomeworkOpResult.Success(submission)
    }

    fun getAssignmentsForClass(classroomId: String): List<HomeworkAssignment> {
        return assignments.filter { it.classroomId == classroomId }.toList()
    }

    fun getSubmissionsForHomework(homeworkId: String): List<HomeworkSubmission> {
        return submissions.filter { it.homeworkId == homeworkId }.toList()
    }
}
