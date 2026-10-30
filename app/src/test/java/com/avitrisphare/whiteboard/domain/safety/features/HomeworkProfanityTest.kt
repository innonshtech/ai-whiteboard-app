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
 * "Profanity filter applied to: homework text"
 * "Message with profanity → blocked with a user-facing message: 'Inappropriate content detected. Please revise.'"
 * "Profanity attempts logged in audit log per user"
 */
class HomeworkProfanityTest {

    private lateinit var auditLogStore: AuditLogStore
    private lateinit var schoolProfanityStore: SchoolProfanityStore
    private lateinit var profanityFilter: ProfanityFilter
    private lateinit var profanityFilterService: ProfanityFilterService
    private lateinit var homeworkService: HomeworkService

    @Before
    fun setUp() {
        auditLogStore = AuditLogStore(context = null)
        schoolProfanityStore = SchoolProfanityStore(context = null, auditLogRepository = auditLogStore)
        profanityFilter = ProfanityFilter(schoolProfanityStore)
        profanityFilterService = ProfanityFilterService(profanityFilter, auditLogStore, schoolProfanityStore)
        homeworkService = HomeworkService(profanityFilterService)
    }

    @Test
    fun safeHomeworkAssignmentCanBeCreated() = runBlocking {
        val result = homeworkService.createHomework(
            schoolId = "school_1",
            classroomId = "class_9B",
            teacherId = "teacher_sharma",
            teacherName = "Mr. Sharma",
            title = "Chapter 4 Geometry Problems",
            instructions = "Complete questions 1 through 10 on page 75 in your notebook."
        )

        assertTrue(result is HomeworkOpResult.Success)
        val created = (result as HomeworkOpResult.Success).data
        assertEquals("Chapter 4 Geometry Problems", created.title)
    }

    @Test
    fun homeworkWithProfanityInInstructionsIsBlockedAndLogged() = runBlocking {
        val teacherId = "teacher_bad"
        val result = homeworkService.createHomework(
            schoolId = "school_1",
            classroomId = "class_9B",
            teacherId = teacherId,
            teacherName = "Bad Teacher",
            title = "Math Assignment",
            instructions = "You bitches better complete this on time or else."
        )

        assertTrue(result is HomeworkOpResult.Blocked)
        val blocked = result as HomeworkOpResult.Blocked
        assertEquals("Inappropriate content detected. Please revise.", blocked.reason)

        val logs = auditLogStore.getLogsForUser(teacherId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.HOMEWORK, logs.first().contentType)
    }

    @Test
    fun safeStudentHomeworkSubmissionIsAccepted() = runBlocking {
        val result = homeworkService.submitHomework(
            homeworkId = "hw_1",
            schoolId = "school_1",
            studentId = "student_rohit",
            studentName = "Rohit Kumar",
            submissionText = "Here is my step-by-step solution for the Pythagorean theorem question."
        )

        assertTrue(result is HomeworkOpResult.Success)
    }

    @Test
    fun studentHomeworkSubmissionWithProfanityIsBlockedAndLogged() = runBlocking {
        val studentId = "student_prankster"
        val result = homeworkService.submitHomework(
            homeworkId = "hw_1",
            schoolId = "school_1",
            studentId = studentId,
            studentName = "Prankster",
            submissionText = "This homework is stupid as fuck, I'm not doing it."
        )

        assertTrue(result is HomeworkOpResult.Blocked)
        val blocked = result as HomeworkOpResult.Blocked
        assertEquals("Inappropriate content detected. Please revise.", blocked.reason)

        val logs = auditLogStore.getLogsForUser(studentId)
        assertEquals(1, logs.size)
        assertEquals(FilterContentType.HOMEWORK, logs.first().contentType)
        assertEquals(studentId, logs.first().userId)
    }
}
