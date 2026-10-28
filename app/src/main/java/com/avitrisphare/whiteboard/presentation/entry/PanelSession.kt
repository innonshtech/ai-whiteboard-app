package com.avitrisphare.whiteboard.presentation.entry

import android.content.Context
import android.provider.Settings
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.data.remote.VerifiedLicense

/**
 * Remembers the panel license and the teacher who signed in on this device.
 * The license is only stored after avitrisphare.webapi accepts it.
 */
class PanelSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    fun panelId(): String {
        val stored = prefs.getString(KEY_PANEL_ID, null)
        if (!stored.isNullOrBlank()) return stored
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        val id = androidId?.takeIf { it.isNotBlank() } ?: "panel"
        prefs.edit().putString(KEY_PANEL_ID, id).apply()
        return id
    }

    fun formattedDeviceId(): String {
        val id = panelId().trim()
        val suffix = if (id.length >= 4) id.takeLast(4).uppercase() else "4471"
        return "GYSH-PNL-$suffix"
    }

    fun isLicenseExpiredLocally(): Boolean {
        val expStr = prefs.getString(KEY_EXPIRES_AT, null) ?: return false
        return try {
            val exp = java.time.Instant.parse(expStr)
            java.time.Instant.now().isAfter(exp)
        } catch (_: Exception) {
            false
        }
    }

    fun isLocked(): Boolean {
        return prefs.getBoolean(KEY_PANEL_LOCKED, false) || isLicenseExpiredLocally()
    }

    fun markLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_PANEL_LOCKED, locked).apply()
    }

    fun snapshot(): PanelSnapshot = PanelSnapshot(
        licenseKey = prefs.getString(KEY_LICENSE, null),
        schoolName = prefs.getString(KEY_SCHOOL, null) ?: "Delhi Public School",
        classroomName = prefs.getString(KEY_CLASSROOM, null) ?: "Classroom",
        classroomId = prefs.getString(KEY_CLASSROOM_ID, null),
        schoolCode = prefs.getString(KEY_SCHOOL_CODE, null),
        schoolEmail = prefs.getString(KEY_SCHOOL_EMAIL, null),
        schoolPhone = prefs.getString(KEY_SCHOOL_PHONE, null),
        expiresAt = prefs.getString(KEY_EXPIRES_AT, null),
        isLocked = isLocked(),
        teacher = prefs.getString(KEY_EMAIL, null)?.let { email ->
            PanelTeacher(
                id = prefs.getString(KEY_TEACHER_ID, null),
                name = prefs.getString(KEY_NAME, null) ?: displayNameFromEmail(email),
                email = email,
                subject = prefs.getString(KEY_SUBJECT, null) ?: "Class",
                accessToken = prefs.getString(KEY_ACCESS, null).orEmpty(),
                refreshToken = prefs.getString(KEY_REFRESH, null)
            )
        }
    )

    fun saveLicense(licenseKey: String, license: VerifiedLicense) {
        prefs.edit()
            .putString(KEY_LICENSE, licenseKey.uppercase())
            .putString(KEY_SCHOOL, license.schoolName)
            .putString(KEY_SCHOOL_ID, license.schoolId)
            .putString(KEY_SCHOOL_CODE, license.schoolCode)
            .putString(KEY_SCHOOL_EMAIL, license.schoolEmail)
            .putString(KEY_SCHOOL_PHONE, license.schoolPhone)
            .putString(KEY_CLASSROOM, license.classroomName)
            .putString(KEY_CLASSROOM_ID, license.classroomId)
            .putString(KEY_EXPIRES_AT, license.expiresAt)
            .putBoolean(KEY_PANEL_LOCKED, false)
            .apply()
    }

    fun clearLicense() {
        prefs.edit()
            .remove(KEY_LICENSE)
            .remove(KEY_SCHOOL)
            .remove(KEY_SCHOOL_ID)
            .remove(KEY_SCHOOL_CODE)
            .remove(KEY_SCHOOL_EMAIL)
            .remove(KEY_SCHOOL_PHONE)
            .remove(KEY_CLASSROOM)
            .remove(KEY_CLASSROOM_ID)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_PANEL_LOCKED)
            .apply()
        logout()
    }

    fun login(teacher: PanelTeacher) {
        prefs.edit()
            .putString(KEY_TEACHER_ID, teacher.id)
            .putString(KEY_NAME, teacher.name)
            .putString(KEY_EMAIL, teacher.email)
            .putString(KEY_SUBJECT, teacher.subject)
            .putString(KEY_ACCESS, teacher.accessToken)
            .putString(KEY_REFRESH, teacher.refreshToken)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_TEACHER_ID)
            .remove(KEY_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_SUBJECT)
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .apply()
    }

    fun demoNoteId(): String? = prefs.getString(KEY_DEMO_NOTE_ID, null)

    fun saveDemoNoteId(noteId: String) {
        prefs.edit().putString(KEY_DEMO_NOTE_ID, noteId).apply()
    }

    fun uiLocaleTag(): String = prefs.getString(KEY_UI_LOCALE, "en") ?: "en"

    fun setUiLocaleTag(tag: String) {
        prefs.edit().putString(KEY_UI_LOCALE, tag).apply()
    }

    companion object {
        private const val PREFS = "gyoash_panel"
        private const val KEY_LICENSE = "license_key"
        private const val KEY_SCHOOL = "school_name"
        private const val KEY_SCHOOL_ID = "school_id"
        private const val KEY_SCHOOL_CODE = "school_code"
        private const val KEY_SCHOOL_EMAIL = "school_email"
        private const val KEY_SCHOOL_PHONE = "school_phone"
        private const val KEY_CLASSROOM = "classroom_name"
        private const val KEY_CLASSROOM_ID = "classroom_id"
        private const val KEY_PANEL_ID = "panel_id"
        private const val KEY_TEACHER_ID = "teacher_id"
        private const val KEY_NAME = "teacher_name"
        private const val KEY_EMAIL = "teacher_email"
        private const val KEY_SUBJECT = "teacher_subject"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_DEMO_NOTE_ID = "demo_note_id"
        private const val KEY_UI_LOCALE = "ui_locale"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_PANEL_LOCKED = "panel_locked"
    }
}

data class PanelSnapshot(
    val licenseKey: String?,
    val schoolName: String,
    val classroomName: String,
    val classroomId: String?,
    val teacher: PanelTeacher?,
    val schoolCode: String? = null,
    val schoolEmail: String? = null,
    val schoolPhone: String? = null,
    val expiresAt: String? = null,
    val isLocked: Boolean = false
) {
    val isLicensed: Boolean get() = !licenseKey.isNullOrBlank()
    val isSignedIn: Boolean get() = teacher != null
}

object LicenseKey {
    const val PREFIX = "GYSH"
    private val FORMAT = Regex("^GYSH-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$", RegexOption.IGNORE_CASE)

    fun isWellFormed(key: String): Boolean = FORMAT.matches(key.trim())
}

fun displayNameFromEmail(email: String): String {
    val local = email.substringBefore("@").trim()
    if (local.isEmpty()) return "Teacher"
    return local.split('.', '_', '-')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
}
