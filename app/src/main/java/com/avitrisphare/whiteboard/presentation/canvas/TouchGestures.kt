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

/**
 * Default sample gap in millimetres for pen stroke capture.
 * 0.25 mm provides smooth, continuous ink curves for handwriting and slow drawing
 * across varying display DPIs and zoom levels without polygonal angularity.
 */
const val DEFAULT_INK_SAMPLE_GAP_MM = 0.25f

/**
 * Calculates the ink sampling gap in physical screen-space pixels based on physical mm and display DPI.
 */
fun inkSampleGapScreenPx(gapMm: Float, xdpi: Float): Float {
    val mm = gapMm.coerceIn(0.15f, 2f)
    return mm / 25.4f * xdpi.coerceAtLeast(1f)
}

/**
 * Calculates the ink sampling gap in canvas world-space units.
 *
 * Screen-space gap = (gapMm / 25.4) * xdpi (in pixels).
 * World-space gap = Screen-space gap / canvasScale.
 *
 * When comparing world-space points:
 * `(world.x - last.x)^2 + (world.y - last.y)^2 < gap^2`
 * is mathematically equivalent to:
 * `screenDist^2 < screenGap^2`.
 * This guarantees consistent physical stroke density across different zoom levels
 * and between the main canvas and split panes.
 */
fun inkSampleGapPx(gapMm: Float, xdpi: Float, canvasScale: Float = 1f): Float {
    val screenPx = inkSampleGapScreenPx(gapMm, xdpi)
    val worldGap = screenPx / canvasScale.coerceAtLeast(0.05f)
    return worldGap.coerceAtLeast(0.2f)
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
