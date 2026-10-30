package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.avitrisphare.whiteboard.BuildConfig
import com.avitrisphare.whiteboard.domain.model.Stroke
import java.io.File
import java.io.FileOutputStream

/**
 * Development-only, privacy-conscious diagnostic logger for equation recognition.
 * Does not permanently store handwriting or log personal data / auth tokens.
 */
object EquationDiagnostics {
    private const val TAG = "EquationRecognition"
    var isEnabled: Boolean = BuildConfig.DEBUG

    private fun safeLogD(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("$tag: $msg")
        }
    }

    private fun safeLogW(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            System.err.println("$tag: $msg")
        }
    }

    fun logSelection(strokes: List<Stroke>, totalSelectedCount: Int) {
        if (!isEnabled) return
        val singlePointCount = strokes.count { it.points.size == 1 }
        val strokeTypes = strokes.groupBy { it.strokeType.name }.mapValues { it.value.size }
        val ids = strokes.take(5).map { it.id }.joinToString(", ")
        safeLogD(TAG, "Stage 1 [Selection]: TotalSelected=$totalSelectedCount, ValidIncluded=${strokes.size}, SinglePoints(Dots)=$singlePointCount, Types=$strokeTypes, FirstIDs=[$ids]")
    }

    fun logEngine(engineName: String, config: String) {
        if (!isEnabled) return
        safeLogD(TAG, "Stage 2 [Engine]: Using $engineName with config='$config'")
    }

    fun logRawResult(engine: String, rawText: String, confidence: Float, durationMs: Long) {
        if (!isEnabled) return
        val confText = if (confidence >= 0f) "%.2f".format(confidence) else "N/A"
        safeLogD(TAG, "Stage 3 [Raw Recognition]: Engine=$engine, Raw='$rawText', Confidence=$confText, Elapsed=${durationMs}ms")
    }

    fun logNormalized(raw: String, normalized: String, durationMs: Long) {
        if (!isEnabled) return
        safeLogD(TAG, "Stage 4 [Normalization]: Raw='$raw' -> NormalizedLaTeX='$normalized', Elapsed=${durationMs}ms")
    }

    fun logFailure(stage: String, error: String, durationMs: Long) {
        if (!isEnabled) return
        safeLogW(TAG, "Stage Error [$stage]: Error='$error', Elapsed=${durationMs}ms")
    }

    fun saveDebugBitmap(context: Context, bitmap: Bitmap) {
        if (!isEnabled) return
        try {
            val debugFile = File(context.cacheDir, "last_equation_debug.png")
            FileOutputStream(debugFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            safeLogD(TAG, "Visual Debug: Cropped bitmap saved to ${debugFile.absolutePath} (${bitmap.width}x${bitmap.height})")
        } catch (e: Exception) {
            safeLogW(TAG, "Failed to save debug bitmap: ${e.message}")
        }
    }
}
