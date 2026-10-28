package com.avitrisphare.whiteboard.presentation.canvas

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

fun isPenNibContact(
    isEraser: Boolean,
    isStylus: Boolean,
    majorPx: Float,
    minorPx: Float,
    xdpi: Float
): Boolean {
    if (isEraser) return false
    if (isStylus) return true

    // On interactive whiteboard panels with normalized IR touch scale:
    // Telemetry from SmartSchool IFP:
    // Pen nib: major ≈ 1.37
    // Back of pen / Finger: major ≈ 3.20
    if (majorPx in 0.1f..5.0f) {
        return majorPx <= 2.25f
    }

    val contactSizePx = if (majorPx > 0f) {
        if (minorPx > 0f) (majorPx + minorPx) / 2f else majorPx
    } else {
        0f
    }
    if (contactSizePx <= 0f) return true
    val dpi = xdpi.coerceAtLeast(1f)
    val majorMm = (majorPx / dpi) * 25.4f
    val minorMm = if (minorPx > 0f) (minorPx / dpi) * 25.4f else 0f
    val contactMm = (contactSizePx / dpi) * 25.4f
    return majorMm <= PEN_NIB_MAX_MM || (minorMm in 0.1f..8.0f) || (contactMm in 0.1f..PEN_NIB_MAX_MM)
}

/**
 * Remembers stylus contact size from the platform touch stream and asks
 * MotionEventPredictor for a short look-ahead so the ink tip is less late.
 */
class TouchContacts(view: View) {
    private val predictor = MotionEventPredictor.newInstance(view)
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
    fun toolType(position: Offset): Int = getContact(position).tool

    fun majorPx(id: PointerId): Float = majorPxById[id.value] ?: lastDownMajor
    fun minorPx(id: PointerId): Float = minorPxById[id.value] ?: lastDownMinor
    fun toolType(id: PointerId): Int = toolById[id.value] ?: lastDownTool
}
