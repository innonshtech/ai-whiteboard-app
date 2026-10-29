package com.avitrisphare.whiteboard.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class VerifiedLicense(
    val schoolName: String,
    val schoolId: String?,
    val schoolCode: String? = null,
    val schoolEmail: String? = null,
    val schoolPhone: String? = null,
    val classroomName: String,
    val classroomId: String?,
    val expiresAt: String? = null,
    val status: String = "VALID"
)

sealed class LicenseCheckResult {
    data class Valid(val license: VerifiedLicense) : LicenseCheckResult()
    data class Expired(
        val schoolName: String,
        val classroomName: String,
        val expiresAt: String?,
        val schoolEmail: String?,
        val schoolPhone: String?,
        val message: String
    ) : LicenseCheckResult()
    data class SchoolInactive(val message: String) : LicenseCheckResult()
    data class Invalid(val message: String) : LicenseCheckResult()
    data class NetworkError(val message: String) : LicenseCheckResult()
}

data class PanelTeacher(
    val id: String?,
    val name: String,
    val email: String,
    val subject: String,
    val accessToken: String,
    val refreshToken: String?
)

data class QrNonce(val nonce: String, val expiresAt: String?)

data class ClassBriefing(
    val subject: String?,
    val attendance: String,
    val lastLesson: String,
    val homework: String
) {
    companion object {
        fun waiting() = ClassBriefing(null, "Checking…", "Checking…", "Checking…")
        fun unavailable() = ClassBriefing(null, "Not marked", "No lesson yet", "Nothing to review")
    }
}

sealed class ApiCall<out T> {
    data class Ok<T>(val value: T) : ApiCall<T>()
    data class Err(val message: String, val licenseRejected: Boolean = false, val licenseExpired: Boolean = false) : ApiCall<Nothing>()
}

sealed class QrPoll {
    data object Pending : QrPoll()
    data class Ready(val teacher: PanelTeacher) : QrPoll()
    data class Expired(val message: String) : QrPoll()
}

fun parseVerifyLicenseResult(json: String): LicenseCheckResult {
    val root = JsonParser.parseString(json).asJsonObject
    val status = root.str("status") ?: if (root.get("valid")?.asBoolean == true) "VALID" else "INVALID"
    val message = root.str("message") ?: "License check failed"
    val school = root.obj("school")
    val classroom = root.obj("classroom")
    val expiresAt = root.str("expiresAt")
    val schoolName = school?.str("name") ?: "School"
    val classroomName = classroom?.str("name") ?: "Classroom"
    val schoolEmail = school?.str("contactEmail")
    val schoolPhone = school?.str("contactPhone")

    return when (status.uppercase()) {
        "VALID" -> LicenseCheckResult.Valid(
            VerifiedLicense(
                schoolName = schoolName,
                schoolId = school?.str("id"),
                schoolCode = school?.str("code"),
                schoolEmail = schoolEmail,
                schoolPhone = schoolPhone,
                classroomName = classroomName,
                classroomId = classroom?.str("id"),
                expiresAt = expiresAt,
                status = "VALID"
            )
        )
        "EXPIRED" -> LicenseCheckResult.Expired(
            schoolName = schoolName,
            classroomName = classroomName,
            expiresAt = expiresAt,
            schoolEmail = schoolEmail,
            schoolPhone = schoolPhone,
            message = message
        )
        "SCHOOL_INACTIVE" -> LicenseCheckResult.SchoolInactive(message)
        else -> LicenseCheckResult.Invalid(message)
    }
}

fun parseVerifyResponse(json: String): ApiCall<VerifiedLicense> {
    return when (val res = parseVerifyLicenseResult(json)) {
        is LicenseCheckResult.Valid -> ApiCall.Ok(res.license)
        is LicenseCheckResult.Expired -> ApiCall.Err(res.message, licenseRejected = true, licenseExpired = true)
        is LicenseCheckResult.SchoolInactive -> ApiCall.Err(res.message, licenseRejected = false)
        is LicenseCheckResult.Invalid -> ApiCall.Err(res.message, licenseRejected = true)
        is LicenseCheckResult.NetworkError -> ApiCall.Err(res.message)
    }
}

fun parseQrSession(json: String): ApiCall<QrNonce> {
    val root = JsonParser.parseString(json).asJsonObject
    val nonce = root.str("nonce")
        ?: return ApiCall.Err("The server did not return a login code")
    return ApiCall.Ok(QrNonce(nonce, root.str("expiresAt")))
}

fun parseQrStatus(json: String): QrPoll {
    val root = JsonParser.parseString(json).asJsonObject
    return when (root.str("status")) {
        "PENDING" -> QrPoll.Pending
        "AUTHENTICATED" -> QrPoll.Ready(teacherFrom(root, nestedToken = true))
        else -> QrPoll.Expired(root.str("message") ?: "QR expired — refreshing")
    }
}

fun parseUserLogin(json: String): ApiCall<PanelTeacher> {
    val root = JsonParser.parseString(json).asJsonObject
    val teacher = teacherFrom(root, nestedToken = false)
    val role = root.obj("user")?.str("role")
    if (role != null && role != "TEACHER") {
        return ApiCall.Err("Only a teacher can sign in on this panel.")
    }
    if (teacher.accessToken.isBlank()) {
        return ApiCall.Err("The server did not return a session token")
    }
    return ApiCall.Ok(teacher)
}

fun parseErrorMessage(json: String, fallback: String): String {
    return try {
        JsonParser.parseString(json).asJsonObject
            .obj("error")
            ?.str("message")
            ?: fallback
    } catch (_: Exception) {
        fallback
    }
}

private fun teacherFrom(root: JsonObject, nestedToken: Boolean): PanelTeacher {
    val user = root.obj("user")
    val accessToken = if (nestedToken) {
        root.obj("token")?.str("accessToken").orEmpty()
    } else {
        root.str("accessToken").orEmpty()
    }
    val subjects = user?.getAsJsonArray("subjects")
    val subject = subjects
        ?.takeIf { it.size() > 0 }
        ?.get(0)
        ?.asString
        ?.takeIf { it.isNotBlank() }
        ?: "Class"
    return PanelTeacher(
        id = user?.str("id"),
        name = user?.str("name") ?: "Teacher",
        email = user?.str("email").orEmpty(),
        subject = subject,
        accessToken = accessToken,
        refreshToken = root.str("refreshToken")
    )
}

/**
 * Fills the class home from the same feeds the teacher app uses:
 * assigned classes, today's attendance, homework waiting for review, and lessons.
 * A missing feed stays on an honest empty label instead of sample copy.
 */
fun buildClassBriefing(
    classroomId: String?,
    classroomName: String,
    classesJson: String?,
    lessonsJson: String?,
    attendanceJson: String?,
    homeworkJson: String?,
    nowMillis: Long = System.currentTimeMillis()
): ClassBriefing {
    val classes = jsonObjects(classesJson, "items")
    val match = classes.firstOrNull { room ->
        val id = room.str("id")
        val name = room.str("name")
        (classroomId != null && id == classroomId) ||
            name.equals(classroomName, ignoreCase = true)
    }
    val subject = match?.str("subject")?.takeIf { it.isNotBlank() }

    val attendanceRooms = jsonObjects(attendanceJson, "classes")
    val attendance = attendanceRooms.firstOrNull { room ->
        val id = room.str("id")
        val name = room.str("name")
        (classroomId != null && id == classroomId) ||
            name.equals(classroomName, ignoreCase = true)
    }
    val attendanceLabel = when {
        attendance == null -> "Not marked"
        (attendance.int("todayMarkedCount") ?: 0) <= 0 -> "Not marked"
        else -> {
            val present = attendance.int("todayPresentCount") ?: 0
            val total = attendance.int("totalStudents") ?: 0
            if (total > 0) "$present of $total present" else "Marked"
        }
    }

    val lessons = jsonObjects(lessonsJson, "items")
    val lastLesson = lessons
        .mapNotNull { lesson ->
            val title = lesson.str("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val at = lesson.str("scheduledAt")?.let(::parseInstantMillis) ?: return@mapNotNull null
            title to at
        }
        .let { timed ->
            val past = timed.filter { it.second <= nowMillis }.maxByOrNull { it.second }
            val next = timed.filter { it.second > nowMillis }.minByOrNull { it.second }
            when {
                past != null -> past.first
                next != null -> "Next: ${next.first}"
                else -> "No lesson yet"
            }
        }

    val homeworkItems = jsonObjects(homeworkJson, "data", "items")
    val toReview = homeworkItems
        .filter { item ->
            val id = item.str("classroomId")
            val name = item.str("classroomName")
            when {
                classroomId != null && id == classroomId -> true
                name.equals(classroomName, ignoreCase = true) -> true
                classroomId == null && name.isNullOrBlank() && id.isNullOrBlank() -> true
                else -> false
            }
        }
        .sumOf { it.int("pendingReviewCount") ?: 0 }
    val homeworkLabel = when (toReview) {
        0 -> "Nothing to review"
        1 -> "1 to review"
        else -> "$toReview to review"
    }

    return ClassBriefing(
        subject = subject,
        attendance = attendanceLabel,
        lastLesson = lastLesson,
        homework = homeworkLabel
    )
}

private fun jsonObjects(json: String?, vararg keys: String): List<JsonObject> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val root = JsonParser.parseString(json)
        val array = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val obj = root.asJsonObject
                keys.firstNotNullOfOrNull { key ->
                    obj.get(key)?.takeIf { it.isJsonArray }?.asJsonArray
                }
            }
            else -> null
        } ?: return emptyList()
        array.mapNotNull { element ->
            if (element.isJsonObject) element.asJsonObject else null
        }
    } catch (_: Exception) {
        emptyList()
    }
}

// ── Equation recognition ─────────────────────────────────────────────────────

/**
 * Structured result from POST /api/v1/math/recognize-equation.
 * [latex] is always present on success; [confidence] is 0..1 from the provider,
 * or -1f when the backend does not report it.
 */
data class MathRecognitionResponse(
    val latex: String,
    val mathml: String?,
    val confidence: Float,
    val error: String?
)

fun parseMathRecognitionResponse(json: String): ApiCall<MathRecognitionResponse> {
    return try {
        val root = JsonParser.parseString(json).asJsonObject
        val success = root.get("success")?.asBoolean ?: true
        val error = root.str("error")
        if (!success) {
            return ApiCall.Err(error ?: "Recognition failed")
        }
        val latex = root.str("latex")?.trim()
        if (latex.isNullOrBlank()) {
            return ApiCall.Err(error ?: "Recognition returned no LaTeX")
        }
        val confidence = try {
            root.get("confidence")?.takeIf { !it.isJsonNull }?.asFloat ?: -1f
        } catch (_: Exception) {
            -1f
        }
        ApiCall.Ok(
            MathRecognitionResponse(
                latex = latex,
                mathml = root.str("mathml"),
                confidence = confidence,
                error = error
            )
        )
    } catch (_: Exception) {
        ApiCall.Err("Malformed recognition response")
    }
}

// ── Internal helpers ──────────────────────────────────────────────────────────

private fun parseInstantMillis(value: String): Long? {
    return try {
        java.time.Instant.parse(value).toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

private fun JsonObject.obj(name: String): JsonObject? {
    val value = get(name) ?: return null
    if (value.isJsonNull || !value.isJsonObject) return null
    return value.asJsonObject
}

private fun JsonObject.str(name: String): String? {
    val value = get(name) ?: return null
    if (value.isJsonNull || !value.isJsonPrimitive) return null
    return value.asString
}

private fun JsonObject.int(name: String): Int? {
    val value = get(name) ?: return null
    if (value.isJsonNull || !value.isJsonPrimitive) return null
    return try {
        value.asInt
    } catch (_: Exception) {
        null
    }
}
