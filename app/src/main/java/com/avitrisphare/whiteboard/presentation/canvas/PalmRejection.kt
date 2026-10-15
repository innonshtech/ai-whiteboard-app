package com.avitrisphare.whiteboard.presentation.canvas

import android.view.MotionEvent
import android.view.View
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

fun isPalmContact(
    touchMajorPx: Float,
    xdpi: Float,
    stylusActive: Boolean,
    isStylus: Boolean,
    touchMinorPx: Float = touchMajorPx,
    ydpi: Float = xdpi
): Boolean {
    if (!stylusActive || isStylus) return false
    return contactAreaCm2(touchMajorPx, touchMinorPx, xdpi, ydpi) > PALM_AREA_CM2
}

/**
 * Remembers stylus contact size from the platform touch stream and asks
 * MotionEventPredictor for a short look-ahead so the ink tip is less late.
 */
class TouchContacts(view: View) {
    private val predictor = MotionEventPredictor.newInstance(view)
    private val majorPxById = HashMap<Long, Float>()
    private val minorPxById = HashMap<Long, Float>()
    private val toolById = HashMap<Long, Int>()
    var stylusActive: Boolean = false
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
        val limit = event.pointerCount.coerceAtMost(MAX_TOUCH_POINTS)
        for (index in 0 until limit) {
            val id = event.getPointerId(index).toLong()
            seen.add(id)
            val tool = event.getToolType(index)
            toolById[id] = tool
            majorPxById[id] = event.getTouchMajor(index)
            minorPxById[id] = event.getTouchMinor(index)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) {
                stylus = true
            }
        }
        majorPxById.keys.retainAll(seen)
        minorPxById.keys.retainAll(seen)
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

    fun majorPx(id: PointerId): Float = majorPxById[id.value] ?: 0f

    fun minorPx(id: PointerId): Float = minorPxById[id.value] ?: 0f

    fun toolType(id: PointerId): Int = toolById[id.value] ?: MotionEvent.TOOL_TYPE_FINGER
}
