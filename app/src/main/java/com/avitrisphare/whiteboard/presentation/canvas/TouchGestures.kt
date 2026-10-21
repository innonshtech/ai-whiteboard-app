package com.avitrisphare.whiteboard.presentation.canvas

import kotlin.math.hypot
import kotlin.math.min

/** Two fingers zoom, three fingers change page, four fingers toggle the AI panel. */
enum class PanelGesture {
    DRAW,
    PINCH,
    PAGE,
    AI_TAP,
    NONE
}

const val FOUR_TAP_TRAVEL_PX = 36f

/** Legacy default; prefer [inkSampleGapPx]. */
const val INK_SAMPLE_GAP_PX = 1.25f

const val DEFAULT_INK_SAMPLE_GAP_MM = 0.4f

fun inkSampleGapPx(gapMm: Float, xdpi: Float, canvasScale: Float = 1f): Float {
    val mm = gapMm.coerceIn(0.15f, 2f)
    val px = mm / 25.4f * xdpi.coerceAtLeast(1f)
    return (px / canvasScale.coerceAtLeast(0.05f)).coerceAtLeast(0.35f)
}

fun sampleGapMillimetres(gapPx: Float, xdpi: Float): Float {
    return gapPx / xdpi.coerceAtLeast(1f) * 25.4f
}

fun retainedPointerCount(reported: Int): Int = min(reported.coerceAtLeast(0), MAX_TOUCH_POINTS)

/**
 * Stylus pressure keeps a visible range. A light contact stays thin and a firm
 * contact uses the full ink width. A finger with no pressure axis stays at 1.
 */
fun stylusPressure(raw: Float): Float {
    if (raw.isNaN() || raw <= 0.02f) return 1f
    return raw.coerceIn(0.05f, 1f)
}

fun stylusPressureWidth(base: Float, pressure: Float): Float {
    val amount = stylusPressure(pressure)
    return (base * (0.35f + 0.65f * amount)).coerceAtLeast(0.5f)
}

fun classifyPanelGesture(peakFingers: Int, travel: Float, dx: Float, dy: Float): PanelGesture {
    if (peakFingers == 4) {
        return if (travel < FOUR_TAP_TRAVEL_PX) PanelGesture.AI_TAP else PanelGesture.NONE
    }
    if (peakFingers == 3) {
        return if (swipePageDelta(dx, dy, threshold = 90f) != 0) PanelGesture.PAGE else PanelGesture.NONE
    }
    if (peakFingers == 2) return PanelGesture.PINCH
    if (peakFingers <= 1) return PanelGesture.DRAW
    return PanelGesture.NONE
}

fun fingerCentroid(xs: FloatArray, ys: FloatArray, count: Int): Pair<Float, Float> {
    if (count <= 0) return 0f to 0f
    var x = 0f
    var y = 0f
    for (index in 0 until count) {
        x += xs[index]
        y += ys[index]
    }
    return x / count to y / count
}

fun centroidTravel(startX: Float, startY: Float, endX: Float, endY: Float): Float {
    return hypot(endX - startX, endY - startY)
}
