package com.avitrisphare.whiteboard.presentation.canvas

import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.input.pointer.PointerId
import androidx.input.motionprediction.MotionEventPredictor
import kotlin.math.hypot

/** Contact wider than this, while a stylus is down, is treated as a palm. */
const val PALM_DIAMETER_CM = 3f

fun palmDiameterLimitPx(xdpi: Float): Float {
    return (PALM_DIAMETER_CM / 2.54f) * xdpi.coerceAtLeast(1f)
}

fun isPalmContact(
    touchMajorPx: Float,
    xdpi: Float,
    stylusActive: Boolean,
    isStylus: Boolean
): Boolean {
    if (!stylusActive || isStylus) return false
    if (touchMajorPx <= 0f) return false
    return touchMajorPx > palmDiameterLimitPx(xdpi)
}

/**
 * Remembers stylus contact size from the platform touch stream and asks
 * MotionEventPredictor for a short look-ahead so the ink tip is less late.
 */
class TouchContacts(view: View) {
    private val predictor = MotionEventPredictor.newInstance(view)
    private val majorPxById = HashMap<Long, Float>()
    private val toolById = HashMap<Long, Int>()
    var stylusActive: Boolean = false
        private set
    var predictDx: Float = 0f
        private set
    var predictDy: Float = 0f
        private set

    fun record(event: MotionEvent) {
        var stylus = false
        val seen = HashSet<Long>()
        for (index in 0 until event.pointerCount) {
            val id = event.getPointerId(index).toLong()
            seen.add(id)
            val tool = event.getToolType(index)
            toolById[id] = tool
            majorPxById[id] = event.getTouchMajor(index)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) {
                stylus = true
            }
        }
        majorPxById.keys.retainAll(seen)
        toolById.keys.retainAll(seen)
        stylusActive = stylus

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

    fun toolType(id: PointerId): Int = toolById[id.value] ?: MotionEvent.TOOL_TYPE_FINGER
}
