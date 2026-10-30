package com.avitrisphare.whiteboard.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class PanelApi(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val gson: Gson = Gson()
) {
    suspend fun checkLicense(licenseKey: String, panelId: String): LicenseCheckResult {
        val body = gson.toJson(mapOf("licenseKey" to licenseKey, "panelId" to panelId))
        return when (val http = post("/api/v1/licenses/verify", body)) {
            is HttpResult.Failed -> LicenseCheckResult.NetworkError(http.message)
            is HttpResult.Body -> parseVerifyLicenseResult(http.json)
        }
    }

    suspend fun verifyLicense(licenseKey: String, panelId: String): ApiCall<VerifiedLicense> {
        return when (val res = checkLicense(licenseKey, panelId)) {
            is LicenseCheckResult.Valid -> ApiCall.Ok(res.license)
            is LicenseCheckResult.Expired -> ApiCall.Err(res.message, licenseRejected = false, licenseExpired = true)
            is LicenseCheckResult.SchoolInactive -> ApiCall.Err(res.message, licenseRejected = false)
            is LicenseCheckResult.Invalid -> ApiCall.Err(res.message, licenseRejected = true)
            is LicenseCheckResult.NetworkError -> ApiCall.Err(res.message)
        }
    }

    suspend fun createQrSession(): ApiCall<QrNonce> {
        return when (val http = get("/api/v1/auth/qr-session")) {
            is HttpResult.Failed -> ApiCall.Err(http.message)
            is HttpResult.Body -> parseQrSession(http.json)
        }
    }

    suspend fun pollQr(nonce: String): ApiCall<QrPoll> {
        return when (val http = get("/api/v1/auth/qr-status/$nonce")) {
            is HttpResult.Failed -> ApiCall.Err(http.message)
            is HttpResult.Body -> ApiCall.Ok(parseQrStatus(http.json))
        }
    }

    suspend fun login(email: String, password: String): ApiCall<PanelTeacher> {
        val body = gson.toJson(mapOf("email" to email, "password" to password))
        return when (val http = post("/api/v1/users/login", body)) {
            is HttpResult.Failed -> ApiCall.Err(http.message)
            is HttpResult.Body -> parseUserLogin(http.json)
        }
    }

    suspend fun logout(accessToken: String) {
        if (accessToken.isBlank()) return
        post("/api/v1/auth/logout", "{}", accessToken)
    }

    /**
     * Sends a base64-encoded PNG of the selected strokes to the backend.
     * The backend holds Vision API credentials; the app never touches them.
     * Returns [ApiCall.Err] on network failure, timeout, or missing latex.
     */
    suspend fun recognizeEquation(
        imageBase64: String,
        accessToken: String
    ): ApiCall<MathRecognitionResponse> {
        if (imageBase64.isBlank()) return ApiCall.Err("No image to recognize")
        val body = gson.toJson(mapOf("image" to imageBase64))
        return when (val http = post("/api/v1/math/recognize-equation", body, accessToken, callTimeoutMs = 2_000L)) {
            is HttpResult.Failed -> ApiCall.Err(http.message)
            is HttpResult.Body -> parseMathRecognitionResponse(http.json)
        }
    }

    /**
     * Class home cards. Lessons and assigned classes come from avitrisphare.webapi.
     * Attendance and homework use the same paths as the teacher app; if those
     * routes are not on the server yet, the cards stay on empty labels.
     */
    suspend fun loadClassBriefing(
        accessToken: String,
        classroomId: String?,
        classroomName: String
    ): ClassBriefing {
        if (accessToken.isBlank()) return ClassBriefing.unavailable()
        val lessonsPath = classroomId
            ?.takeIf { it.isNotBlank() }
            ?.let { "/api/v1/classrooms/$it/lessons" }
        val homeworkPath = classroomId
            ?.takeIf { it.isNotBlank() }
            ?.let { "/api/v1/homework?classroomId=$it" }
            ?: "/api/v1/homework"
        return try {
            coroutineScope {
                val classes = async { authedJson("/api/v1/me/classes", accessToken) }
                val lessons = async { lessonsPath?.let { authedJson(it, accessToken) } }
                val attendance = async { authedJson("/api/v1/attendance/classes", accessToken) }
                val homework = async { authedJson(homeworkPath, accessToken) }
                buildClassBriefing(
                    classroomId = classroomId,
                    classroomName = classroomName,
                    classesJson = classes.await(),
                    lessonsJson = lessons.await(),
                    attendanceJson = attendance.await(),
                    homeworkJson = homework.await()
                )
            }
        } catch (_: Exception) {
            ClassBriefing.unavailable()
        }
    }

    private suspend fun authedJson(path: String, bearer: String): String? {
        return when (val http = get(path, bearer)) {
            is HttpResult.Body -> http.json
            is HttpResult.Failed -> null
        }
    }

    private suspend fun get(path: String, bearer: String? = null): HttpResult {
        val builder = Request.Builder().url(baseUrl + path).get()
        if (!bearer.isNullOrBlank()) builder.header("Authorization", "Bearer $bearer")
        return exchange(builder.build())
    }

    private suspend fun post(
        path: String,
        json: String,
        bearer: String? = null,
        callTimeoutMs: Long? = null
    ): HttpResult {
        val builder = Request.Builder()
            .url(baseUrl + path)
            .post(json.toRequestBody(JSON))
        if (!bearer.isNullOrBlank()) builder.header("Authorization", "Bearer $bearer")
        val httpClient = if (callTimeoutMs == null) {
            client
        } else {
            client.newBuilder()
                .connectTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .build()
        }
        return exchange(builder.build(), httpClient)
    }

    private suspend fun exchange(request: Request, httpClient: OkHttpClient = client): HttpResult = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    HttpResult.Failed(parseErrorMessage(text, "Request failed (${response.code})"))
                } else {
                    HttpResult.Body(text)
                }
            }
        } catch (_: IOException) {
            HttpResult.Failed(
                "Can't reach the school server at $baseUrl. Start avitrisphare.webapi and check this panel can see it."
            )
        }
    }

    private sealed class HttpResult {
        data class Body(val json: String) : HttpResult()
        data class Failed(val message: String) : HttpResult()
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
