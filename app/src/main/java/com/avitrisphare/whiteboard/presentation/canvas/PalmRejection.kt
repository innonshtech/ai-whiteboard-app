package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.input.motionprediction.MotionEventPredictor
import kotlin.math.hypot

/** The infrared frame reports at most this many contacts at once. */
const val MAX_TOUCH_POINTS = 40

/** A resting palm, while a stylus is down, covers more than this area. */
const val PALM_AREA_CM2 = 3f

/**
 * Ellipse area from the platform touch axes. An unreported size (0) is not a palm:
 * many infrared frames omit the contact size for a fingertip.
 */
fun contactAreaCm2(majorPx: Float, minorPx: Float, xdpi: Float, ydpi: Float): Float {
    if (majorPx <= 0f) return 0f
    val dpiX = xdpi.coerceAtLeast(1f)
    val dpiY = if (ydpi > 1f) ydpi else dpiX
    val majorCm = majorPx / dpiX * 2.54f
    val minorCm = (if (minorPx > 0f) minorPx else majorPx) / dpiY * 2.54f
    return (Math.PI * (majorCm / 2.0) * (minorCm / 2.0)).toFloat()
}

fun isPalmContactArea(
    touchMajorPx: Float,
    xdpi: Float,
    touchMinorPx: Float = touchMajorPx,
    ydpi: Float = xdpi
): Boolean {
    return contactAreaCm2(touchMajorPx, touchMinorPx, xdpi, ydpi) > PALM_AREA_CM2
}

fun isPalmContact(
    touchMajorPx: Float,
    xdpi: Float,
    stylusActive: Boolean,
    isStylus: Boolean,
    touchMinorPx: Float = touchMajorPx,
    ydpi: Float = xdpi
): Boolean {
    if (!stylusActive || isStylus) return false
    return isPalmContactArea(touchMajorPx, xdpi, touchMinorPx, ydpi)
}

/** Maximum diameter in millimetres for an infrared/passive whiteboard pen nib. */
const val PEN_NIB_MAX_MM = 9.0f

/**
 * Universal adaptive touch classifier that automatically differentiates between
 * fine pen nib (Tip 1 / Primary Color) and blunt back of pen / finger (Tip 2 / Secondary Color).
 *
 * It works across all interactive whiteboard panels (MAXHUB, ViewSonic, Promethean, SMART, etc.)
 * by dynamically learning the panel's bimodal contact size distribution using 1D Otsu clustering
 * and persisting the calibrated threshold across restarts.
 */
class AdaptiveTouchClassifier(private val context: Context?) {
    private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val recentSamples = ArrayList<Float>(SAMPLE_CAPACITY)

    var dynamicThreshold: Float = prefs?.getFloat(KEY_THRESHOLD, -1f) ?: -1f
        private set

    var hasCalibrated: Boolean = dynamicThreshold > 0f
        private set

    private var minObserved: Float = prefs?.getFloat(KEY_MIN_SIZE, -1f) ?: -1f
    private var maxObserved: Float = prefs?.getFloat(KEY_MAX_SIZE, -1f) ?: -1f

    companion object {
        private const val PREFS_NAME = "avitri_touch_calibration"
        private const val KEY_THRESHOLD = "learned_touch_threshold"
        private const val KEY_MIN_SIZE = "learned_min_touch_size"
        private const val KEY_MAX_SIZE = "learned_max_touch_size"
        private const val SAMPLE_CAPACITY = 32

        @Volatile
        var defaultInstance = AdaptiveTouchClassifier(null)
            private set

        @Volatile
        private var instance: AdaptiveTouchClassifier? = null

        fun get(context: Context): AdaptiveTouchClassifier {
            return instance ?: synchronized(this) {
                instance ?: AdaptiveTouchClassifier(context.applicationContext).also {
                    instance = it
                    defaultInstance = it
                }
            }
        }

        fun effectiveContactSize(major: Float, minor: Float, size: Float): Float {
            return when {
                major > 0f && minor > 0f -> (major + minor) / 2f
                major > 0f -> major
                minor > 0f -> minor
                size > 0f -> size * 100f
                else -> 0f
            }
        }
    }

    @Synchronized
    fun recordDown(major: Float, minor: Float, size: Float, tool: Int) {
        if (tool == MotionEvent.TOOL_TYPE_ERASER) return
        val contactSize = effectiveContactSize(major, minor, size)
        // Ignore unmeasured contacts or large palm/fist touches (> 150px)
        if (contactSize <= 0f || contactSize > 150f) return

        if (recentSamples.size >= SAMPLE_CAPACITY) {
            recentSamples.removeAt(0)
        }
        recentSamples.add(contactSize)

        if (minObserved <= 0f || contactSize < minObserved) {
            minObserved = contactSize
        }
        if (maxObserved <= 0f || contactSize > maxObserved) {
            maxObserved = contactSize
        }

        // Fast 2-touch immediate adaptation:
        // On any panel, the thick tip / finger is at least 1.25x larger than the fine pen nib.
        if (maxObserved >= minObserved * 1.25f && (maxObserved - minObserved) >= 0.3f) {
            val split = if (recentSamples.size >= 4) {
                calculateOtsuSplit(recentSamples) ?: ((minObserved + maxObserved) / 2f)
            } else {
                (minObserved + maxObserved) / 2f
            }

            dynamicThreshold = if (hasCalibrated && dynamicThreshold > 0f) {
                0.6f * dynamicThreshold + 0.4f * split
            } else {
                split
            }
            hasCalibrated = true
            prefs?.edit()
                ?.putFloat(KEY_THRESHOLD, dynamicThreshold)
                ?.putFloat(KEY_MIN_SIZE, minObserved)
                ?.putFloat(KEY_MAX_SIZE, maxObserved)
                ?.apply()
            android.util.Log.i("AvitriTouch", "ADAPTED THRESHOLD -> $dynamicThreshold (min=$minObserved, max=$maxObserved, sample=$contactSize)")
        }
    }

    private fun calculateOtsuSplit(samples: List<Float>): Float? {
        val sorted = samples.sorted()
        val n = sorted.size
        if (n < 2) return null

        var totalSum = 0.0
        for (v in sorted) totalSum += v

        var sumB = 0.0
        var wB = 0
        var maxVariance = -1.0
        var bestSplit: Float? = null

        for (i in 0 until n - 1) {
            wB++
            val wF = n - wB
            if (wF == 0) break

            sumB += sorted[i]
            val mB = sumB / wB
            val mF = (totalSum - sumB) / wF

            val varianceBetween = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (varianceBetween > maxVariance) {
                maxVariance = varianceBetween
                bestSplit = (sorted[i] + sorted[i + 1]) / 2f
            }
        }
        return if (maxVariance > 0.0) bestSplit else null
    }

    fun isPenNib(
        isEraser: Boolean,
        isStylus: Boolean,
        majorPx: Float,
        minorPx: Float,
        xdpi: Float,
        size: Float = 0f
    ): Boolean {
        if (isEraser) return false
        if (isStylus) return true

        val contactSize = effectiveContactSize(majorPx, minorPx, size)
        if (contactSize <= 0f) return true // Unreported contact defaults to primary pen

        // 1. If learned/calibrated threshold exists, use it
        if (hasCalibrated && dynamicThreshold > 0f) {
            return contactSize <= dynamicThreshold
        }

        // 2. Cold-start heuristic before two distinct tools are observed on this board:
        // A) Normalized IR scale (e.g. MAXHUB / SmartSchool telemetry: fine tip ~1.37, thick tip ~3.20)
        if (contactSize <= 5.0f) {
            return contactSize <= 2.25f
        }

        // B) Pixel scale with sensible DPI
        // A physical fine pen nib is typically <= 3.5mm; fingers are 8mm-15mm
        val dpi = xdpi.coerceAtLeast(1f)
        val contactMm = (contactSize / dpi) * 25.4f
        if (dpi > 50f && dpi < 500f && contactMm in 0.1f..30f) {
            return contactMm <= 4.0f
        }

        // C) Generic pixel scale fallback for fine nib
        return contactSize <= 18f
    }

    fun calibrateManual(tip1Size: Float, tip2Size: Float) {
        val split = (tip1Size + tip2Size) / 2f
        dynamicThreshold = split
        minObserved = minOf(tip1Size, tip2Size)
        maxObserved = maxOf(tip1Size, tip2Size)
        hasCalibrated = true
        prefs?.edit()
            ?.putFloat(KEY_THRESHOLD, dynamicThreshold)
            ?.putFloat(KEY_MIN_SIZE, minObserved)
            ?.putFloat(KEY_MAX_SIZE, maxObserved)
            ?.apply()
    }
}

fun isPenNibContact(
    isEraser: Boolean,
    isStylus: Boolean,
    majorPx: Float,
    minorPx: Float,
    xdpi: Float,
    size: Float = 0f,
    classifier: AdaptiveTouchClassifier? = null
): Boolean {
    val targetClassifier = classifier ?: AdaptiveTouchClassifier.defaultInstance
    return targetClassifier.isPenNib(
        isEraser = isEraser,
        isStylus = isStylus,
        majorPx = majorPx,
        minorPx = minorPx,
        xdpi = xdpi,
        size = size
    )
}

/**
 * Remembers stylus contact size from the platform touch stream and asks
 * MotionEventPredictor for a short look-ahead so the ink tip is less late.
 */
class TouchContacts(view: View) {
    private val predictor = MotionEventPredictor.newInstance(view)
    val classifier: AdaptiveTouchClassifier = AdaptiveTouchClassifier.get(view.context)

    data class ContactData(
        val x: Float,
        val y: Float,
        val major: Float,
        val minor: Float,
        val size: Float,
        val tool: Int
    )
    private val activeContacts = ArrayList<ContactData>()
    private val majorPxById = HashMap<Long, Float>()
    private val minorPxById = HashMap<Long, Float>()
    private val sizeById = HashMap<Long, Float>()
    private val toolById = HashMap<Long, Int>()
    var lastDownMajor: Float = 0f
        private set
    var lastDownMinor: Float = 0f
        private set
    var lastDownTool: Int = MotionEvent.TOOL_TYPE_FINGER
        private set
    var lastDownSize: Float = 0f
        private set
    var stylusActive: Boolean = false
        private set
    /** True after a stylus has written on this board. A later hand contact can then erase. */
    var sawStylus: Boolean = false
        private set
    var activeCount: Int = 0
        private set
    var predictDx: Float = 0f
        private set
    var predictDy: Float = 0f
        private set

    fun record(event: MotionEvent) {
        var stylus = false
        val seen = HashSet<Long>()
        activeContacts.clear()
        val limit = event.pointerCount.coerceAtMost(MAX_TOUCH_POINTS)
        for (index in 0 until limit) {
            val id = event.getPointerId(index).toLong()
            seen.add(id)
            val tool = event.getToolType(index)
            val major = event.getTouchMajor(index)
            val minor = event.getTouchMinor(index)
            val size = event.getSize(index)
            val x = event.getX(index)
            val y = event.getY(index)
            toolById[id] = tool
            majorPxById[id] = major
            minorPxById[id] = minor
            sizeById[id] = size
            activeContacts.add(ContactData(x, y, major, minor, size, tool))
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) {
                stylus = true
                sawStylus = true
            }
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                lastDownMajor = major
                lastDownMinor = minor
                lastDownTool = tool
                lastDownSize = size
                android.util.Log.i("AvitriTouch", "ACTION_DOWN idx=$index id=$id tool=$tool major=$major minor=$minor size=$size")
                classifier.recordDown(major, minor, size, tool)
            }
        }
        majorPxById.keys.retainAll(seen)
        minorPxById.keys.retainAll(seen)
        sizeById.keys.retainAll(seen)
        toolById.keys.retainAll(seen)
        stylusActive = stylus
        activeCount = seen.size

        // One stylus tip needs a look-ahead. A full 40-point frame does not.
        if (event.pointerCount > 2) {
            predictDx = 0f
            predictDy = 0f
            return
        }
        predictor.record(event)
        val predicted = predictor.predict()
        if (predicted == null) {
            predictDx = 0f
            predictDy = 0f
            return
        }
        val index = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        var dx = predicted.x - event.getX(index)
        var dy = predicted.y - event.getY(index)
        predicted.recycle()
        val distance = hypot(dx, dy)
        val maxLead = 40f
        if (distance > maxLead) {
            val scale = maxLead / distance
            dx *= scale
            dy *= scale
        }
        predictDx = dx
        predictDy = dy
    }

    fun getContact(position: Offset): ContactData {
        if (activeContacts.isEmpty()) {
            return ContactData(position.x, position.y, lastDownMajor, lastDownMinor, lastDownSize, lastDownTool)
        }
        if (activeContacts.size == 1) {
            return activeContacts[0]
        }
        return activeContacts.minByOrNull {
            val dx = it.x - position.x
            val dy = it.y - position.y
            dx * dx + dy * dy
        } ?: ContactData(position.x, position.y, lastDownMajor, lastDownMinor, lastDownSize, lastDownTool)
    }

    fun majorPx(position: Offset): Float = getContact(position).major
    fun minorPx(position: Offset): Float = getContact(position).minor
    fun size(position: Offset): Float = getContact(position).size
    fun toolType(position: Offset): Int = getContact(position).tool

    fun majorPx(id: PointerId): Float = majorPxById[id.value] ?: lastDownMajor
    fun minorPx(id: PointerId): Float = minorPxById[id.value] ?: lastDownMinor
    fun size(id: PointerId): Float = sizeById[id.value] ?: lastDownSize
    fun toolType(id: PointerId): Int = toolById[id.value] ?: lastDownTool

    fun isPenNib(
        position: Offset,
        isEraser: Boolean,
        isStylus: Boolean,
        xdpi: Float
    ): Boolean {
        val contact = getContact(position)
        return classifier.isPenNib(
            isEraser = isEraser,
            isStylus = isStylus,
            majorPx = contact.major,
            minorPx = contact.minor,
            xdpi = xdpi,
            size = contact.size
        )
    }
}
