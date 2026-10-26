package com.avitrisphare.whiteboard.domain.safety

import java.util.Locale
import java.util.regex.Pattern

/**
 * Enterprise-grade multi-lingual profanity and safety filter supporting:
 * - English, Hindi (Devanagari & Hinglish), Marathi (Devanagari & Romanized)
 * - Dictionaries loaded dynamically from external JSON assets via [SafetyDictionaryProvider]
 * - Legitimate educational & anatomical allowlist protection
 * - Scunthorpe false positive prevention
 * - Custom school-level blocklists & allowlists
 * - Leetspeak and obfuscation normalization
 */
class ProfanityFilter(
    private val schoolProfanityRepository: SchoolProfanityRepository? = null,
    private val dictionaryProvider: SafetyDictionaryProvider = ProfanityDictionary.provider
) {

    private val leetspeakMap = mapOf(
        '@' to 'a',
        '4' to 'a',
        '8' to 'b',
        '3' to 'e',
        '1' to 'i',
        '!' to 'i',
        '|' to 'i',
        '0' to 'o',
        '$' to 's',
        '5' to 's',
        '+' to 't',
        '7' to 't'
    )

    private val zeroWidthRegex = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val punctuationRegex = Regex("[.,\\-_/\\\\*^~`'\"!?:;(){}\\[\\]]")
    private val whitespaceRegex = Regex("\\s+")
    private val singleLetterSpaceRegex = Regex("\\b([\\p{L}\\p{Nd}])\\s+(?=[\\p{L}\\p{Nd}]\\b)")
    private val tripleRepeatsRegex = Regex("([\\p{L}\\p{Nd}])\\1{2,}")

    /**
     * Checks user-generated text for profanity across supported languages and custom rules.
     *
     * @param text The input string to check.
     * @param contentType The source category (chat, announcement, homework, AI input).
     * @param schoolId Optional school ID to incorporate custom school rules.
     * @return [ProfanityCheckResult] with [isBlocked], [matchedWords], [detectedLanguages].
     */
    suspend fun checkContent(
        text: String,
        contentType: FilterContentType,
        schoolId: String? = null
    ): ProfanityCheckResult {
        if (text.isBlank()) {
            return ProfanityCheckResult.allowed(contentType, text)
        }

        // Fetch school-specific custom lists if schoolId is provided
        val customBlocked = if (schoolId != null && schoolProfanityRepository != null) {
            schoolProfanityRepository.getCustomBlockedWords(schoolId)
        } else {
            emptySet()
        }

        val customAllowed = if (schoolId != null && schoolProfanityRepository != null) {
            schoolProfanityRepository.getCustomAllowedWords(schoolId)
        } else {
            emptySet()
        }

        val normalized = normalize(text)
        val deSpacedSingleLetters = normalized.replace(singleLetterSpaceRegex, "$1")
        val tokens = tokenize(text)
        val normalizedTokens = tokenize(normalized)
        val dePunctTokens = text.split(whitespaceRegex)
            .map { it.replace(punctuationRegex, "").lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }

        val matchedWords = mutableListOf<String>()
        val matchedLanguages = mutableSetOf<ProfanityLanguage>()

        // 1. Check custom school-level blocked words first
        for (customWord in customBlocked) {
            val normCustom = customWord.trim().lowercase(Locale.ROOT)
            if (normCustom.isBlank()) continue
            if (isWordMatched(normCustom, normalized, deSpacedSingleLetters, tokens, normalizedTokens, dePunctTokens, customAllowed)) {
                matchedWords.add(customWord)
                matchedLanguages.add(ProfanityLanguage.CUSTOM)
            }
        }

        // 2. Check English blocked words from JSON asset
        for (word in dictionaryProvider.getEnglishBlockedWords()) {
            if (isWordMatched(word, normalized, deSpacedSingleLetters, tokens, normalizedTokens, dePunctTokens, customAllowed)) {
                matchedWords.add(word)
                matchedLanguages.add(ProfanityLanguage.ENGLISH)
            }
        }

        // 3. Check Hindi blocked words from JSON asset (Devanagari + Hinglish)
        for (word in dictionaryProvider.getHindiBlockedWords()) {
            if (isWordMatched(word, normalized, deSpacedSingleLetters, tokens, normalizedTokens, dePunctTokens, customAllowed)) {
                matchedWords.add(word)
                matchedLanguages.add(ProfanityLanguage.HINDI)
            }
        }

        // 4. Check Marathi blocked words from JSON asset (Devanagari + Latin)
        for (word in dictionaryProvider.getMarathiBlockedWords()) {
            if (isWordMatched(word, normalized, deSpacedSingleLetters, tokens, normalizedTokens, dePunctTokens, customAllowed)) {
                matchedWords.add(word)
                matchedLanguages.add(ProfanityLanguage.MARATHI)
            }
        }

        return if (matchedWords.isNotEmpty()) {
            ProfanityCheckResult.blocked(
                contentType = contentType,
                matchedWords = matchedWords.distinct(),
                languages = matchedLanguages
            )
        } else {
            ProfanityCheckResult.allowed(contentType, text)
        }
    }

    private fun isWordMatched(
        blockedWord: String,
        normalizedText: String,
        deSpacedSingleLetters: String,
        originalTokens: List<String>,
        normalizedTokens: List<String>,
        dePunctTokens: List<String>,
        customAllowed: Set<String>
    ): Boolean {
        val target = blockedWord.lowercase(Locale.ROOT)

        // Check if explicitly allowed (school custom allowlist or global educational allowlist)
        if (isExplicitlyAllowed(target, customAllowed)) {
            return false
        }

        // Exact token matches
        val hasExactTokenMatch = originalTokens.any { it.equals(target, ignoreCase = true) } ||
            normalizedTokens.any { it.equals(target, ignoreCase = true) } ||
            dePunctTokens.any { it.equals(target, ignoreCase = true) }

        if (hasExactTokenMatch) {
            if (!isTokenSafe(target, originalTokens)) {
                return true
            }
        }

        // Word-boundary matches in normalized text and single-letter-spaced text
        if (target.length >= 3) {
            if (containsWord(normalizedText, target) || containsWord(deSpacedSingleLetters, target)) {
                if (!isTokenSafe(target, originalTokens)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Verifies that the matched token is not safe educational content or a protected Scunthorpe word.
     */
    private fun isTokenSafe(target: String, originalTokens: List<String>): Boolean {
        val educationalList = dictionaryProvider.getEducationalAllowlist()
        val scunthorpeList = dictionaryProvider.getScunthorpeSafeWords()

        // If the target is directly an educational term, protect it
        if (educationalList.any { it.equals(target, ignoreCase = true) }) {
            return true
        }

        // If the target is a Scunthorpe safe word, protect it
        if (scunthorpeList.any { it.equals(target, ignoreCase = true) }) {
            return true
        }

        // If the matched word is an anatomy term (e.g., penis, vagina, breast, sperm, uterus),
        // it must NEVER be blocked in an educational platform
        if (educationalList.any { edu ->
                edu.contains(target, ignoreCase = true) || target.contains(edu, ignoreCase = true)
            }
        ) {
            return true
        }

        // Safe words check against compound tokens: e.g. "classic" contains "ass", "peacock" contains "cock"
        for (token in originalTokens) {
            val lower = token.lowercase(Locale.ROOT)
            if (lower.contains(target)) {
                if (scunthorpeList.any { safe -> lower.contains(safe) } ||
                    educationalList.any { edu -> lower.contains(edu) }
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun isExplicitlyAllowed(word: String, customAllowed: Set<String>): Boolean {
        val lower = word.lowercase(Locale.ROOT)
        if (customAllowed.any { it.equals(lower, ignoreCase = true) }) return true
        if (dictionaryProvider.getEducationalAllowlist().any { it.equals(lower, ignoreCase = true) }) return true
        if (dictionaryProvider.getScunthorpeSafeWords().any { it.equals(lower, ignoreCase = true) }) return true
        return false
    }

    private fun containsWord(text: String, word: String): Boolean {
        val pattern = Pattern.compile("(?<=^|\\W)" + Pattern.quote(word) + "(?=$|\\W)", Pattern.CASE_INSENSITIVE)
        return pattern.matcher(text).find()
    }

    /**
     * Normalizes text by removing zero-width characters, decoding leetspeak,
     * and collapsing excessive repeated characters of 3+ (e.g. 'fuuuuck' -> 'fuck', 'chuuutiya' -> 'chutiya').
     */
    fun normalize(input: String): String {
        var result = input.replace(zeroWidthRegex, "").lowercase(Locale.ROOT)

        // Substitute leetspeak characters
        val sb = StringBuilder(result.length)
        for (ch in result) {
            sb.append(leetspeakMap[ch] ?: ch)
        }
        result = sb.toString()

        // Collapse character repetitions of 3+ into 1 (e.g. "fuuuuck" -> "fuck", "chuuutiya" -> "chutiya")
        // Preserves 2-letter repeats like 'good', 'book', 'class', 'feed'
        result = result.replace(tripleRepeatsRegex, "$1")

        return result
    }

    private fun tokenize(text: String): List<String> {
        return text
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() }
    }
}
