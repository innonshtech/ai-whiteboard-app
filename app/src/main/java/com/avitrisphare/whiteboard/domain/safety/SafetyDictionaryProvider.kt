package com.avitrisphare.whiteboard.domain.safety

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads profanity dictionaries and educational allowlists from external JSON asset files.
 *
 * Supports:
 * - Android Asset Manager: assets/safety/
 * - Local JVM / Unit test ClassLoader: safety/
 * - Over-The-Air (OTA) downloaded updates: filesDir/safety_cache/
 * - In-memory caching for zero-overhead repeated lookups
 */

@Singleton
class SafetyDictionaryProvider @Inject constructor(
    private val context: Context? = null,
    private val gson: Gson = Gson()
) {

    @Volatile
    private var englishWords: Set<String>? = null

    @Volatile
    private var hindiWords: Set<String>? = null

    @Volatile
    private var marathiWords: Set<String>? = null

    @Volatile
    private var educationalWords: Set<String>? = null

    @Volatile
    private var scunthorpeWords: Set<String>? = null

    fun getEnglishBlockedWords(): Set<String> {
        return englishWords ?: synchronized(this) {
            englishWords ?: loadWordSet("profanity_en.json").also { englishWords = it }
        }
    }

    fun getHindiBlockedWords(): Set<String> {
        return hindiWords ?: synchronized(this) {
            hindiWords ?: loadWordSet("profanity_hi.json").also { hindiWords = it }
        }
    }

    fun getMarathiBlockedWords(): Set<String> {
        return marathiWords ?: synchronized(this) {
            marathiWords ?: loadWordSet("profanity_mr.json").also { marathiWords = it }
        }
    }

    fun getEducationalAllowlist(): Set<String> {
        return educationalWords ?: synchronized(this) {
            educationalWords ?: loadWordSet("educational_allowlist.json").also { educationalWords = it }
        }
    }

    fun getScunthorpeSafeWords(): Set<String> {
        return scunthorpeWords ?: synchronized(this) {
            scunthorpeWords ?: loadWordSet("scunthorpe_safe.json").also { scunthorpeWords = it }
        }
    }

    /**
     * Clears cached dictionaries, forcing reload on next access (useful after OTA sync).
     */
    fun reload() {
        synchronized(this) {
            englishWords = null
            hindiWords = null
            marathiWords = null
            educationalWords = null
            scunthorpeWords = null
        }
    }

    private fun loadWordSet(fileName: String): Set<String> {
        val stream = openStream(fileName) ?: return emptySet()
        return try {
            InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<String>>() {}.type
                val list: List<String>? = gson.fromJson(reader, type)
                list?.map { it.trim().lowercase(Locale.ROOT) }?.filter { it.isNotBlank() }?.toSet()
                    ?: emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun openStream(fileName: String): InputStream? {
        val ctx = context
        if (ctx != null) {
            val cacheFile = File(ctx.filesDir, "safety_cache" + File.separator + fileName)
            if (cacheFile.exists()) {
                try {
                    return cacheFile.inputStream()
                } catch (e: Exception) {
                    // Fallback to asset stream
                }
            }

            try {
                return ctx.assets.open("safety/" + fileName)
            } catch (e: Exception) {
                // Fallback to ClassLoader
            }
        }

        val relativePath = "safety/" + fileName
        return javaClass.classLoader?.getResourceAsStream(relativePath)
            ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(relativePath)
    }
}
