package com.avitrisphare.whiteboard.presentation.canvas.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class TranslationState {
    object Idle : TranslationState()
    data class DownloadingModel(val language: IndianLanguage) : TranslationState()
    object Translating : TranslationState()
    data class Error(val message: String) : TranslationState()
}

/**
 * High-performance, offline-first on-device translation engine using Google ML Kit.
 * Manages model downloads, caching of translation clients, formula shielding, and fallbacks.
 */
class OnDeviceTranslatorEngine {

    private val translators = ConcurrentHashMap<Pair<String, String>, Translator>()
    private val modelManager = RemoteModelManager.getInstance()
    private val downloadedModels = ConcurrentHashMap<String, Boolean>()

    /**
     * Translates input text into the target Indian language.
     * Protects math and science formulas before translation, and unmasks them afterward.
     */
    suspend fun translate(
        text: String,
        targetLanguage: IndianLanguage,
        sourceLanguageCode: String = TranslateLanguage.ENGLISH,
        onStateChange: (TranslationState) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        val targetMlCode = targetLanguage.mlKitLanguageCode
        if (targetMlCode == null) {
            // Language handled via fallback educational dictionary (e.g., Punjabi / Malayalam)
            val fallbackTranslated = translateViaFallbackDictionary(text, targetLanguage)
            return@withContext Result.success(fallbackTranslated)
        }

        try {
            // 1. Shield formulas & math expressions
            val shielded = FormulaShield.shield(text)

            // 2. Prepare translator client
            val key = sourceLanguageCode to targetMlCode
            val translator = translators.getOrPut(key) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguageCode)
                    .setTargetLanguage(targetMlCode)
                    .build()
                Translation.getClient(options)
            }

            // 3. Ensure offline model is available
            val isDownloaded = downloadedModels[targetMlCode] ?: false
            if (!isDownloaded) {
                onStateChange(TranslationState.DownloadingModel(targetLanguage))
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions).awaitDone()
                downloadedModels[targetMlCode] = true
            }

            // 4. Translate masked prose
            onStateChange(TranslationState.Translating)
            val rawTranslated = translator.translate(shielded.maskedText).awaitResult()

            // 5. Restore original formulas
            val finalOutput = shielded.unshield(rawTranslated)
            onStateChange(TranslationState.Idle)
            Result.success(finalOutput)
        } catch (e: Exception) {
            onStateChange(TranslationState.Error(e.message ?: "Translation failed"))
            // Fallback gracefully so teaching is never blocked
            val fallback = translateViaFallbackDictionary(text, targetLanguage)
            Result.success(fallback)
        }
    }

    /**
     * Checks if a language model pack is already cached on-device.
     */
    suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        if (downloadedModels[languageCode] == true) return@withContext true
        try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            val isDownloaded = modelManager.isModelDownloaded(model).awaitResult()
            downloadedModels[languageCode] = isDownloaded
            isDownloaded
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Explicitly pre-downloads a language model pack (e.g. during school setup / Wi-Fi active).
     */
    suspend fun downloadModel(targetLanguage: IndianLanguage): Boolean = withContext(Dispatchers.IO) {
        val code = targetLanguage.mlKitLanguageCode ?: return@withContext true
        try {
            val model = TranslateRemoteModel.Builder(code).build()
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(model, conditions).awaitDone()
            downloadedModels[code] = true
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Closes all active translators and clears resources.
     */
    fun close() {
        translators.values.forEach {
            try {
                it.close()
            } catch (_: Exception) {}
        }
        translators.clear()
    }

    /**
     * Fallback dictionary translation for common classroom phrases, instructions,
     * science, and math terms for languages not yet in offline ML Kit packs (Punjabi, Malayalam).
     */
    private fun translateViaFallbackDictionary(text: String, language: IndianLanguage): String {
        val shielded = FormulaShield.shield(text)
        var translated = shielded.maskedText

        val dictionary: Map<String, String> = when (language) {
            IndianLanguage.PUNJABI -> mapOf(
                "solve" to "ਹੱਲ ਕਰੋ",
                "calculate" to "ਹਿਸਾਬ ਲਗਾਓ",
                "find the value" to "ਮੁੱਲ ਲੱਭੋ",
                "prove that" to "ਸਾਬਤ ਕਰੋ ਕਿ",
                "definition" to "ਪਰਿਭਾਸ਼ਾ",
                "equation" to "ਸਮੀਕਰਨ",
                "formula" to "ਸੂਤਰ",
                "triangle" to "ਤਿਕੋਣ",
                "circle" to "ਚੱਕਰ",
                "velocity" to "ਵੇਗ",
                "energy" to "ਊਰਜਾ",
                "force" to "ਬਲ",
                "mass" to "ਪੁੰਜ",
                "temperature" to "ਤਾਪਮਾਨ",
                "answer" to "ਜਵਾਬ",
                "question" to "ਸਵਾਲ",
                "example" to "ਉਦਾਹਰਨ",
                "homework" to "ਘਰ ਦਾ ਕੰਮ",
                "notes" to "ਨੋਟਸ",
                "class" to "ਜਮਾਤ"
            )
            IndianLanguage.MALAYALAM -> mapOf(
                "solve" to "പരിഹരിക്കുക",
                "calculate" to "കണക്കാക്കുക",
                "find the value" to "മൂല്യം കണ്ടെത്തുക",
                "prove that" to "തെളിയിക്കുക",
                "definition" to "നിർവചനം",
                "equation" to "സമവാക്യം",
                "formula" to "സൂത്രവാക്യം",
                "triangle" to "ത്രികോണം",
                "circle" to "വൃത്തം",
                "velocity" to "പ്രവേഗം",
                "energy" to "ഊർജ്ജം",
                "force" to "ബലം",
                "mass" to "പിണ്ഡം",
                "temperature" to "താപനില",
                "answer" to "ഉത്തരം",
                "question" to "ചോദ്യം",
                "example" to "ഉദാഹരണം",
                "homework" to "ഹോംവർക്ക്",
                "notes" to "കുറിപ്പുകൾ",
                "class" to "ക്ലാസ്"
            )
            else -> emptyMap()
        }

        for ((enWord, localWord) in dictionary) {
            translated = translated.replace(Regex("(?i)\\b$enWord\\b"), localWord)
        }

        return shielded.unshield(translated)
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
        addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
        addOnCanceledListener { if (cont.isActive) cont.cancel() }
    }

    private suspend fun Task<Void>.awaitDone() {
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
            addOnCanceledListener { if (cont.isActive) cont.cancel() }
        }
    }
}

