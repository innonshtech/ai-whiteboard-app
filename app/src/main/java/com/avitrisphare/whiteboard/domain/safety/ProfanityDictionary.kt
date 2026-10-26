package com.avitrisphare.whiteboard.domain.safety

/**
 * Accessor for curated dictionaries loaded from external JSON asset files,
 * including English, Hindi, and Marathi profanity, as well as educational & Scunthorpe safe lists.
 */
object ProfanityDictionary {

    private var customProvider: SafetyDictionaryProvider? = null
    private val defaultProvider by lazy { SafetyDictionaryProvider() }

    val provider: SafetyDictionaryProvider
        get() = customProvider ?: defaultProvider

    fun setProvider(provider: SafetyDictionaryProvider) {
        customProvider = provider
    }

    val ENGLISH_BLOCKED_WORDS: Set<String>
        get() = provider.getEnglishBlockedWords()

    val HINDI_BLOCKED_WORDS: Set<String>
        get() = provider.getHindiBlockedWords()

    val MARATHI_BLOCKED_WORDS: Set<String>
        get() = provider.getMarathiBlockedWords()

    val EDUCATIONAL_ALLOWLIST_TERMS: Set<String>
        get() = provider.getEducationalAllowlist()

    val SCUNTHORPE_SAFE_WORDS: Set<String>
        get() = provider.getScunthorpeSafeWords()
}
