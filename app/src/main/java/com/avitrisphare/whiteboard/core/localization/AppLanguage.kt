package com.avitrisphare.whiteboard.core.localization

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val description: String
) {
    ENGLISH("en", "English", "English", "Default"),
    HINDI("hi", "Hindi", "हिन्दी", "हिन्दी"),
    MARATHI("mr", "Marathi", "मराठी", "मराठी");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
