package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Extracts text from images using Google ML Kit Vision Text Recognition.
 * Supports printed text, handwritten text, and text inside diagrams for
 * English, Hindi, and Marathi.
 */
class ImageTextExtractor {
    private var devanagariRecognizer: com.google.mlkit.vision.text.TextRecognizer? = null
    private var latinRecognizer: com.google.mlkit.vision.text.TextRecognizer? = null

    suspend fun extractText(bitmap: Bitmap): String {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Devanagari recognizer supports Devanagari script (Hindi, Marathi)
        // and Latin script (English) simultaneously.
        val devanagari = devanagariRecognizer ?: TextRecognition.getClient(
            DevanagariTextRecognizerOptions.Builder().build()
        ).also { devanagariRecognizer = it }

        var resultText = try {
            devanagari.process(inputImage).awaitResult().text.trim()
        } catch (_: Exception) {
            ""
        }

        // Fallback to Latin text recognizer if Devanagari recognizer returned empty text
        if (resultText.isBlank()) {
            val latin = latinRecognizer ?: TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            ).also { latinRecognizer = it }

            resultText = try {
                latin.process(inputImage).awaitResult().text.trim()
            } catch (_: Exception) {
                ""
            }
        }

        return resultText
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
        addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
        addOnCanceledListener { if (cont.isActive) cont.cancel() }
    }
}
