package com.avitrisphare.whiteboard.core.localization

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val description: String
) {
    ENGLISH("en", "English", "English", "Default"),

    // 22 Official Scheduled Languages of India
    ASSAMESE("as", "Assamese", "অসমীয়া", "Assam"),
    BENGALI("bn", "Bengali", "বাংলা", "West Bengal"),
    BODO("brx", "Bodo", "बड़ो", "Assam / Bodo"),
    DOGRI("doi", "Dogri", "डोगरी", "Jammu & Kashmir"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી", "Gujarat"),
    HINDI("hi", "Hindi", "हिन्दी", "Official"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ", "Karnataka"),
    KASHMIRI("ks", "Kashmiri", "कॉशुर", "Jammu & Kashmir"),
    KONKANI("kok", "Konkani", "कोंकणी", "Goa"),
    MAITHILI("mai", "Maithili", "मैथिली", "Bihar"),
    MALAYALAM("ml", "Malayalam", "മലയാളം", "Kerala"),
    MANIPURI("mni", "Manipuri", "মৈতৈলোন্", "Manipur"),
    MARATHI("mr", "Marathi", "मराठी", "Maharashtra"),
    NEPALI("ne", "Nepali", "नेपाली", "Sikkim / West Bengal"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ", "Odisha"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ", "Punjab"),
    SANSKRIT("sa", "Sanskrit", "संस्कृतम्", "Classical"),
    SANTALI("sat", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ", "Jharkhand"),
    SINDHI("sd", "Sindhi", "सिंधी", "Sindhi"),
    TAMIL("ta", "Tamil", "தமிழ்", "Tamil Nadu"),
    TELUGU("te", "Telugu", "తెలుగు", "Andhra Pradesh / Telangana"),
    URDU("ur", "Urdu", "اردو", "Urdu");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
