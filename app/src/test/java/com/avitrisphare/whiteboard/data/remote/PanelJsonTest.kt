package com.avitrisphare.whiteboard.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelJsonTest {
    @Test
    fun validLicenseKeepsSchoolAndClassroom() {
        val result = parseVerifyResponse(
            """
            {
              "status": "VALID",
              "valid": true,
              "message": "License is valid and active",
              "school": { "id": "sch", "name": "Delhi Public School", "code": "DPS001" },
              "classroom": { "id": "room", "name": "Grade 8 - A" }
            }
            """.trimIndent()
        )
        val license = (result as ApiCall.Ok).value
        assertEquals("Delhi Public School", license.schoolName)
        assertEquals("Grade 8 - A", license.classroomName)
    }

    @Test
    fun expiredLicenseIsRejected() {
        val result = parseVerifyResponse(
            """{"status":"EXPIRED","valid":false,"message":"License expired. Contact your administrator."}"""
        )
        val error = result as ApiCall.Err
        assertTrue(error.licenseRejected)
        assertEquals("License expired. Contact your administrator.", error.message)
    }

    @Test
    fun authenticatedQrBecomesATeacher() {
        val poll = parseQrStatus(
            """
            {
              "status": "AUTHENTICATED",
              "token": { "accessToken": "abc", "tokenType": "Bearer", "expiresIn": 3600 },
              "refreshToken": "ref",
              "user": { "id": "u1", "name": "Priya Sharma", "email": "priya@dps.edu", "subjects": ["Science"], "role": "TEACHER" }
            }
            """.trimIndent()
        )
        val teacher = (poll as QrPoll.Ready).teacher
        assertEquals("Priya Sharma", teacher.name)
        assertEquals("Science", teacher.subject)
        assertEquals("abc", teacher.accessToken)
        assertEquals("ref", teacher.refreshToken)
    }

    @Test
    fun classBriefingUsesTeacherAttendanceLessonsAndHomework() {
        val now = java.time.Instant.parse("2026-10-14T10:00:00Z").toEpochMilli()
        val brief = buildClassBriefing(
            classroomId = "room",
            classroomName = "Grade 8 - A",
            classesJson = """{"items":[{"id":"room","name":"Grade 8 - A","subject":"Science"}]}""",
            lessonsJson = """
                {"items":[
                  {"title":"Cells","scheduledAt":"2026-10-10T04:00:00Z"},
                  {"title":"Photosynthesis","scheduledAt":"2026-10-13T04:00:00Z"},
                  {"title":"Respiration","scheduledAt":"2026-10-16T04:00:00Z"}
                ]}
            """.trimIndent(),
            attendanceJson = """
                {"classes":[{"id":"room","name":"Grade 8 - A","todayMarkedCount":20,"todayPresentCount":18,"totalStudents":22}]}
            """.trimIndent(),
            homeworkJson = """
                {"data":[
                  {"classroomId":"room","pendingReviewCount":3},
                  {"classroomId":"other","pendingReviewCount":9}
                ]}
            """.trimIndent(),
            nowMillis = now
        )
        assertEquals("Science", brief.subject)
        assertEquals("18 of 22 present", brief.attendance)
        assertEquals("Photosynthesis", brief.lastLesson)
        assertEquals("3 to review", brief.homework)
    }

    @Test
    fun classBriefingStaysEmptyWhenFeedsAreMissing() {
        val brief = buildClassBriefing(
            classroomId = "room",
            classroomName = "Grade 8 - A",
            classesJson = null,
            lessonsJson = null,
            attendanceJson = null,
            homeworkJson = null
        )
        assertEquals("Not marked", brief.attendance)
        assertEquals("No lesson yet", brief.lastLesson)
        assertEquals("Nothing to review", brief.homework)
    }

    @Test
    fun studentPasswordLoginIsRefused() {
        val result = parseUserLogin(
            """
            {
              "accessToken": "tok",
              "tokenType": "Bearer",
              "expiresIn": 3600,
              "user": { "id": "u2", "name": "Aman", "email": "aman@dps.edu", "role": "STUDENT", "subjects": [] }
            }
            """.trimIndent()
        )
        assertFalse(result is ApiCall.Ok)
    }
}
