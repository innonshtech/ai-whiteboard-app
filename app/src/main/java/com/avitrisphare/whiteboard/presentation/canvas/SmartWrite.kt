package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

enum class SmartWriteScript { LATIN, DEVANAGARI, OTHER }

data class SmartWriteLanguage(
    val tag: String,
    val label: String,
    val script: SmartWriteScript
)

val SMART_WRITE_LANGUAGES = listOf(
    SmartWriteLanguage("en", "English", SmartWriteScript.LATIN),
    SmartWriteLanguage("hi", "हिन्दी", SmartWriteScript.DEVANAGARI),
    SmartWriteLanguage("mr", "मराठी", SmartWriteScript.DEVANAGARI),
    SmartWriteLanguage("ta", "தமிழ்", SmartWriteScript.OTHER),
    SmartWriteLanguage("bn", "বাংলা", SmartWriteScript.OTHER),
    SmartWriteLanguage("gu", "ગુજરાતી", SmartWriteScript.OTHER),
    SmartWriteLanguage("ml", "മലയാളം", SmartWriteScript.OTHER),
    SmartWriteLanguage("kn", "ಕನ್ನಡ", SmartWriteScript.OTHER),
    SmartWriteLanguage("te", "తెలుగు", SmartWriteScript.OTHER),
    SmartWriteLanguage("ar", "العربية", SmartWriteScript.OTHER)
)

fun smartWriteLanguage(tag: String): SmartWriteLanguage {
    return SMART_WRITE_LANGUAGES.firstOrNull { it.tag == tag } ?: SMART_WRITE_LANGUAGES.first()
}

val HANDWRITING_TYPES = setOf(
    StrokeType.PEN,
    StrokeType.PENCIL,
    StrokeType.MARKER,
    StrokeType.BRUSH
)

data class HandwritingGuess(val text: String, val score: Float)

data class SmartWriteOffer(
    val strokeIds: List<String>,
    val candidates: List<String>,
    val lowConfidence: Boolean,
    val color: Int,
    val anchor: Offset
)

const val SMART_WRITE_LOW_SCORE = 0.55f

fun isLowConfidence(score: Float): Boolean = score > 0f && score < SMART_WRITE_LOW_SCORE

fun scoreRecognition(text: String): Float {
    val visible = text.count { !it.isWhitespace() }
    val written = text.count { character ->
        character.isLetterOrDigit() || character.category == CharCategory.NON_SPACING_MARK ||
            character.category == CharCategory.COMBINING_SPACING_MARK ||
            character.category == CharCategory.ENCLOSING_MARK
    }
    if (visible == 0 || written == 0) return 0.2f
    val ratio = written.toFloat() / visible.toFloat()
    return if (ratio >= 0.8f && written >= 2) 0.93f else 0.42f
}

fun candidateTexts(raw: String): List<String> {
    val text = raw.trim().replace(Regex("\\s+"), " ")
    if (text.isEmpty()) return emptyList()
    val titled = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    return listOf(text, text.lowercase(), titled).distinct().take(3)
}

fun isStraightMark(stroke: Stroke): Boolean {
    return strokeStraightness(stroke.points) >= 0.8f
}

/**
 * Handwriting worth reading. A plain line, underline, or tick is left as ink.
 * A short straight stem inside a word, such as the letter l, stays with the word.
 */
fun readableHandwriting(strokes: List<Stroke>): List<Stroke> {
    val ink = strokes.filter { it.strokeType in HANDWRITING_TYPES && it.points.size >= 2 }
    if (ink.isEmpty()) return emptyList()
    val curved = ink.filter { !isStraightMark(it) }
    if (curved.isEmpty()) return emptyList()
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    curved.forEach { stroke ->
        val bounds = stroke.bounds
        left = min(left, bounds.left)
        top = min(top, bounds.top)
        right = max(right, bounds.right)
        bottom = max(bottom, bounds.bottom)
    }
    val boxSpan = max(right - left, bottom - top).coerceAtLeast(1f)
    val near = Rect(left - 20f, top - 20f, right + 20f, bottom + 20f)
    return ink.filter { stroke ->
        if (!isStraightMark(stroke)) return@filter true
        val bounds = stroke.bounds
        val span = max(bounds.width, bounds.height)
        span < boxSpan * 0.55f && near.overlaps(bounds)
    }
}

fun handwritingCluster(strokes: List<Stroke>, gap: Float = 140f): List<Stroke> {
    val ink = strokes.filter { it.strokeType in HANDWRITING_TYPES && it.points.isNotEmpty() }
    if (ink.isEmpty()) return emptyList()
    val grouped = ArrayList<Stroke>()
    grouped.add(ink.last())
    var bounds = ink.last().bounds
    for (index in ink.lastIndex - 1 downTo 0) {
        val stroke = ink[index]
        val dynamicGap = maxOf(gap, bounds.height * 1.25f)
        val near = Rect(
            bounds.left - dynamicGap,
            bounds.top - dynamicGap,
            bounds.right + dynamicGap,
            bounds.bottom + dynamicGap
        )
        if (!near.overlaps(stroke.bounds)) break
        grouped.add(0, stroke)
        bounds = unionBounds(bounds, stroke.bounds)
    }
    return grouped
}

fun typedTextStroke(strokes: List<Stroke>, text: String, color: Int): Stroke {
    val bounds = strokes.map { it.bounds }.reduce { acc, rect -> unionBounds(acc, rect) }
    val size = bounds.height.coerceIn(36f, 140f)
    return Stroke(
        points = listOf(Offset(bounds.left, bounds.top)),
        colorValue = color,
        strokeWidth = size,
        strokeType = StrokeType.TEXT,
        text = text
    )
}

private fun strokeStraightness(points: List<Offset>): Float {
    if (points.size < 2) return 1f
    var length = 0f
    for (index in 1 until points.size) {
        val dx = points[index].x - points[index - 1].x
        val dy = points[index].y - points[index - 1].y
        length += kotlin.math.sqrt(dx * dx + dy * dy)
    }
    val dx = points.last().x - points.first().x
    val dy = points.last().y - points.first().y
    val direct = kotlin.math.sqrt(dx * dx + dy * dy)
    return if (length <= 1f) 1f else direct / length
}

private fun unionBounds(a: Rect, b: Rect): Rect {
    return Rect(
        min(a.left, b.left),
        min(a.top, b.top),
        max(a.right, b.right),
        max(a.bottom, b.bottom)
    )
}

/**
 * Reads handwriting from stroke points. English, Hindi, and Marathi use the
 * on-device text model so a clear word returns in well under three seconds.
 * A downloaded handwriting model, when one is already on the device, supplies
 * the extra spelling candidates.
 */
class HandwritingReader(@Suppress("UNUSED_PARAMETER") context: Context) {
    private var latin: com.google.mlkit.vision.text.TextRecognizer? = null
    private var devanagari: com.google.mlkit.vision.text.TextRecognizer? = null

    suspend fun prefetch(language: SmartWriteLanguage) {
        val model = inkModel(language) ?: return
        try {
            val manager = RemoteModelManager.getInstance()
            if (manager.isModelDownloaded(model).awaitResult()) return
            manager.download(model, DownloadConditions.Builder().build()).awaitDone()
        } catch (_: Exception) {
        }
    }

    suspend fun read(strokes: List<Stroke>, language: SmartWriteLanguage): List<HandwritingGuess> {
        if (strokes.isEmpty()) return emptyList()
        val fromInk = recognizeDownloadedInk(strokes, language)
        if (fromInk.isNotEmpty()) return fromInk
        if (language.script == SmartWriteScript.OTHER) {
            return recognizeAfterDownload(strokes, language)
        }
        val fromImage = recognizeImage(strokes, language.script)
        if (fromImage.isNotEmpty()) {
            prefetch(language)
            return fromImage
        }
        return recognizeAfterDownload(strokes, language)
    }

    suspend fun readMath(strokes: List<Stroke>): List<HandwritingGuess> {
        if (strokes.isEmpty()) return emptyList()

        // 1. Unbiased on-device Digital Ink Recognition (primary engine)
        // No preContext bias so letters like Y, y, a, b are never falsely coerced into x.
        val inkGuesses = recognizeDownloadedInk(
            strokes,
            SmartWriteLanguage("en", "English", SmartWriteScript.LATIN),
            preContext = null
        )
        if (inkGuesses.isNotEmpty()) {
            return inkGuesses
        }

        prefetch(SmartWriteLanguage("en", "English", SmartWriteScript.LATIN))

        // 2. Fallback: Vision Text Recognition (OCR) on rendered bitmap
        val fromOcr = recognizeImage(strokes, SmartWriteScript.LATIN)
        if (fromOcr.isNotEmpty()) {
            return fromOcr
        }

        // 3. Fallback: Wait for downloaded Digital Ink model
        return recognizeAfterDownload(
            strokes,
            SmartWriteLanguage("en", "English", SmartWriteScript.LATIN)
        )
    }

    private suspend fun recognizeDownloadedInk(
        strokes: List<Stroke>,
        language: SmartWriteLanguage,
        preContext: String? = null
    ): List<HandwritingGuess> {
        val model = inkModel(language) ?: return emptyList()
        return try {
            val manager = RemoteModelManager.getInstance()
            if (!manager.isModelDownloaded(model).awaitResult()) return emptyList()
            recognizeInk(model, strokes, preContext)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun recognizeAfterDownload(
        strokes: List<Stroke>,
        language: SmartWriteLanguage
    ): List<HandwritingGuess> {
        val model = inkModel(language) ?: return emptyList()
        return try {
            val manager = RemoteModelManager.getInstance()
            if (!manager.isModelDownloaded(model).awaitResult()) {
                manager.download(model, DownloadConditions.Builder().build()).awaitDone()
            }
            recognizeInk(model, strokes)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun recognizeInk(
        model: DigitalInkRecognitionModel,
        strokes: List<Stroke>,
        preContext: String? = null
    ): List<HandwritingGuess> {
        val client = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
        val ink = inkFrom(strokes)
        val task = if (preContext != null) {
            val context = RecognitionContext.builder().setPreContext(preContext).build()
            client.recognize(ink, context)
        } else {
            client.recognize(ink)
        }
        val result = task.awaitResult()
        return result.candidates.mapNotNull { candidate ->
            val text = candidate.text.trim()
            if (text.isEmpty()) null else HandwritingGuess(text, candidate.score ?: 0.9f)
        }.distinctBy { it.text }.take(10)
    }

    private suspend fun recognizeImage(
        strokes: List<Stroke>,
        script: SmartWriteScript
    ): List<HandwritingGuess> {
        val bitmap = renderHandwriting(strokes) ?: return emptyList()
        return try {
            val recognizer = when (script) {
                SmartWriteScript.DEVANAGARI -> {
                    devanagari ?: TextRecognition.getClient(
                        DevanagariTextRecognizerOptions.Builder().build()
                    ).also { devanagari = it }
                }
                else -> {
                    latin ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { latin = it }
                }
            }
            val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult().text
            val score = scoreRecognition(text)
            candidateTexts(text).mapIndexed { index, value ->
                HandwritingGuess(value, if (index == 0) score else score * 0.85f)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun inkModel(language: SmartWriteLanguage): DigitalInkRecognitionModel? {
        val tags = when (language.tag) {
            "en" -> listOf("en", "en-US")
            "hi" -> listOf("hi", "hi-IN")
            "mr" -> listOf("mr", "mr-IN")
            else -> listOf(language.tag)
        }
        val identifier = tags.firstNotNullOfOrNull { DigitalInkRecognitionModelIdentifier.fromLanguageTag(it) }
            ?: return null
        return DigitalInkRecognitionModel.builder(identifier).build()
    }
}

internal fun renderHandwriting(strokes: List<Stroke>): Bitmap? {
    if (strokes.isEmpty()) return null
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    strokes.forEach { stroke ->
        val bounds = stroke.bounds
        left = min(left, bounds.left)
        top = min(top, bounds.top)
        right = max(right, bounds.right)
        bottom = max(bottom, bounds.bottom)
    }
    if (right <= left || bottom <= top) return null
    val pad = 32f
    val targetHeight = 360f
    val scale = (targetHeight / (bottom - top + pad * 2f)).coerceIn(0.5f, 2.5f)
    val width = ((right - left + pad * 2f) * scale).toInt().coerceIn(64, 3000)
    val height = ((bottom - top + pad * 2f) * scale).toInt().coerceIn(64, 1200)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = AndroidColor.BLACK
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.BLACK
    }
    strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val strokeWidth = (stroke.strokeWidth * scale).coerceAtLeast(4f)
        if (stroke.points.size == 1) {
            val point = stroke.points.first()
            val x = (point.x - left + pad) * scale
            val y = (point.y - top + pad) * scale
            canvas.drawCircle(x, y, strokeWidth / 2f, dotPaint)
            return@forEach
        }
        strokePaint.strokeWidth = strokeWidth
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val x = (point.x - left + pad) * scale
            val y = (point.y - top + pad) * scale
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, strokePaint)
    }
    return bitmap
}

private fun inkFrom(strokes: List<Stroke>): Ink {
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    strokes.forEach { stroke ->
        stroke.points.forEach { point ->
            left = min(left, point.x)
            top = min(top, point.y)
            bottom = max(bottom, point.y)
        }
    }
    val scale = 180f / (bottom - top).coerceAtLeast(1f)
    val builder = Ink.builder()
    var timeMs = 100L
    strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val strokeBuilder = Ink.Stroke.builder()
        stroke.points.forEach { point ->
            strokeBuilder.addPoint(Ink.Point.create((point.x - left) * scale, (point.y - top) * scale, timeMs))
            timeMs += 10L
        }
        if (stroke.points.size == 1) {
            val point = stroke.points.first()
            strokeBuilder.addPoint(Ink.Point.create((point.x - left) * scale + 1f, (point.y - top) * scale, timeMs))
            timeMs += 10L
        }
        builder.addStroke(strokeBuilder.build())
        timeMs += 50L
    }
    return builder.build()
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
