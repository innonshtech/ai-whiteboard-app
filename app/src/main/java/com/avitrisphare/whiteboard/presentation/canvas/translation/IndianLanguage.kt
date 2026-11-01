package com.avitrisphare.whiteboard.presentation.canvas.translation

import com.google.mlkit.nl.translate.TranslateLanguage

/**
 * Enumeration of the 10 Indian Languages supported in GYOASH Whiteboard (GYO-46 / Story #43)
 * plus English as the default source language.
 */
enum class IndianLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val mlKitLanguageCode: String?
) {
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        mlKitLanguageCode = TranslateLanguage.HINDI
    ),
    MARATHI(
        code = "mr",
        displayName = "Marathi",
        nativeName = "मराठी",
        mlKitLanguageCode = TranslateLanguage.MARATHI
    ),
    GUJARATI(
        code = "gu",
        displayName = "Gujarati",
        nativeName = "ગુજરાતી",
        mlKitLanguageCode = TranslateLanguage.GUJARATI
    ),
    TAMIL(
        code = "ta",
        displayName = "Tamil",
        nativeName = "தமிழ்",
        mlKitLanguageCode = TranslateLanguage.TAMIL
    ),
    TELUGU(
        code = "te",
        displayName = "Telugu",
        nativeName = "తెలుగు",
        mlKitLanguageCode = TranslateLanguage.TELUGU
    ),
    KANNADA(
        code = "kn",
        displayName = "Kannada",
        nativeName = "ಕನ್ನಡ",
        mlKitLanguageCode = TranslateLanguage.KANNADA
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা",
        mlKitLanguageCode = TranslateLanguage.BENGALI
    ),
    URDU(
        code = "ur",
        displayName = "Urdu",
        nativeName = "اردو",
        mlKitLanguageCode = TranslateLanguage.URDU
    ),
    PUNJABI(
        code = "pa",
        displayName = "Punjabi",
        nativeName = "ਪੰਜਾਬੀ",
        mlKitLanguageCode = null // Fallback lexicon mapping
    ),
    MALAYALAM(
        code = "ml",
        displayName = "Malayalam",
        nativeName = "മലയാളം",
        mlKitLanguageCode = null // Fallback lexicon mapping
    );

    companion object {
        fun fromCode(code: String): IndianLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
        }

        fun allIndianLanguages(): List<IndianLanguage> = entries.toList()
    }
}

