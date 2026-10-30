package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class MathToolKind { RULER, COMPASS, PROTRACTOR, SET_SQUARE, TRIANGLE }

enum class MathToolZone {
    NONE,
    MOVE,
    ROTATE_POSITIVE,
    ROTATE_NEGATIVE,
    PENCIL,
    SCALE,
    CLOSE,
    ROTATE,
    SPREAD,
    ANGLE_ARM,
    ANGLE_MODE_1,
    ANGLE_MODE_ARC_LEFT,
    ANGLE_MODE_ARC,
    ANGLE_MODE_2,
    SIDE_1_PENCIL,
    SIDE_2_PENCIL,
    SIDE_3_PENCIL,
    DRAW_MODE_TOGGLE
}

data class PlacedMathTool(
    val id: String = UUID.randomUUID().toString(),
    val kind: MathToolKind,
    val center: Offset,
    val rotationDeg: Float = 0f,
    val span: Float,
    val toolScale: Float = 1.0f,
    val markedAngleDeg: Float? = null,
    val pointerAngleDeg: Float = 90f,
    val angleMode: Int = 0,
    val isDrawMode: Boolean = false
)

/** Size, rotation, or protractor arm sent from the Geometry panel. Null fields stay as they are. */
data class MathToolAdjust(
    val span: Float? = null,
    val rotationDeg: Float? = null,
    val pointerAngleDeg: Float? = null
)

data class MathToolHit(
    val tool: PlacedMathTool,
    val zone: MathToolZone
)

/** Gesture memory for one finger sequence on a placed instrument. */
class MathGesture {
    var toolId: String? = null
    var zone: MathToolZone = MathToolZone.NONE
    var movedPx: Float = 0f
    var pivot: Offset = Offset.Zero
    var draggingPositiveEnd: Boolean = true
    var startSpan: Float = 0f
    var startDist: Float = 0f
    var initialRay: Offset = Offset.Zero
    var startToolScale: Float = 1.0f
    var startAngleDeg: Float = 0f
    var lastAngleDeg: Float = 0f
    var sweepDeg: Float = 0f
    var arcMode: Boolean = false
    var drawDir: Offset = Offset(1f, 0f)
    var rulerDrawing: Boolean = false
    var rulerEdgeSign: Float = 1f
    var twoFinger: Boolean = false

    fun reset() {
        toolId = null
        zone = MathToolZone.NONE
        movedPx = 0f
        pivot = Offset.Zero
        draggingPositiveEnd = true
        startSpan = 0f
        startDist = 0f
        initialRay = Offset.Zero
        startToolScale = 1.0f
        startAngleDeg = 0f
        lastAngleDeg = 0f
        sweepDeg = 0f
        arcMode = false
        drawDir = Offset(1f, 0f)
        rulerDrawing = false
        rulerEdgeSign = 1f
        twoFinger = false
    }
}

private const val RULER_THICKNESS = 96f
const val RULER_INSET = 24f
private const val HANDLE_RADIUS = 24f
private const val HANDLE_TOUCH_RADIUS = 46f
const val COMPASS_MIN_RADIUS = 50f
const val COMPASS_MAX_RADIUS = 1800f
private const val HINGE_LIFT = 52f
const val MATH_TAP_SLOP = 16f
const val RULER_EDGE_SLOP = 38f
const val ARC_ANGLE_SLOP = 12f
private const val ARC_RADIAL_SLOP = 28f
private val RAD_TO_DEG = 180f / Math.PI.toFloat()

fun defaultMathSpan(kind: MathToolKind): Float = when (kind) {
    MathToolKind.RULER -> 440f
    MathToolKind.COMPASS -> 140f
    MathToolKind.PROTRACTOR -> 260f
    MathToolKind.SET_SQUARE -> 280f
    MathToolKind.TRIANGLE -> 220f
}

/** Span limits already used by the on-board scale handles, in screen pixels. */
fun mathSpanRange(kind: MathToolKind): ClosedFloatingPointRange<Float> = when (kind) {
    MathToolKind.RULER -> 200f..1800f
    MathToolKind.COMPASS -> COMPASS_MIN_RADIUS..COMPASS_MAX_RADIUS
    MathToolKind.PROTRACTOR -> 120f..800f
    MathToolKind.SET_SQUARE -> 120f..1000f
    MathToolKind.TRIANGLE -> 120f..900f
}

fun clampMathSpan(kind: MathToolKind, span: Float): Float {
    val range = mathSpanRange(kind)
    return span.coerceIn(range.start, range.endInclusive)
}

/** Equilateral side length. Matches the triangle instrument drawing. */
fun triangleSidePx(span: Float): Float = span * 1.8f

fun newMathTool(kind: MathToolKind, center: Offset): PlacedMathTool {
    return PlacedMathTool(kind = kind, center = center, span = defaultMathSpan(kind))
}

fun mathToolRadius(kind: MathToolKind, span: Float): Float = when (kind) {
    MathToolKind.RULER -> hypot(span / 2f, RULER_THICKNESS / 2f) + 16f
    MathToolKind.COMPASS -> (span + HINGE_LIFT).coerceAtLeast(160f) + 16f
    MathToolKind.PROTRACTOR -> span + 20f
    MathToolKind.SET_SQUARE -> hypot(span / 2f, span / 2f) + 36f
    MathToolKind.TRIANGLE -> {
        val side = span * 1.8f
        val height = side * 0.8660254f
        hypot(side / 2f, height * 0.6667f) + 36f
    }
}

fun mathToolRadius(tool: PlacedMathTool): Float = mathToolRadius(tool.kind, tool.span)

fun toCanvasPoint(local: Offset, center: Offset, rotationDeg: Float): Offset {
    val dir = direction(rotationDeg)
    val perp = perpendicular(rotationDeg)
    return Offset(
        center.x + dir.x * local.x + perp.x * local.y,
        center.y + dir.y * local.x + perp.y * local.y
    )
}

fun mathToolBoundingRect(tool: PlacedMathTool): Rect = when (tool.kind) {
    MathToolKind.RULER -> {
        val halfW = tool.span / 2f
        val halfH = RULER_THICKNESS / 2f
        val corners = listOf(
            toCanvasPoint(Offset(-halfW, -halfH), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(halfW, -halfH), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(halfW, halfH), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(-halfW, halfH), tool.center, tool.rotationDeg)
        )
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }
        Rect(minX, minY, maxX, maxY)
    }
    MathToolKind.PROTRACTOR -> {
        val corners = listOf(
            toCanvasPoint(Offset(-tool.span, -tool.span), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(tool.span, -tool.span), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(tool.span, 14f), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(-tool.span, 14f), tool.center, tool.rotationDeg)
        )
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }
        Rect(minX, minY, maxX, maxY)
    }
    MathToolKind.COMPASS -> {
        val needle = tool.center
        val pencil = compassPencil(tool)
        val hinge = compassHinge(tool)
        val minX = minOf(needle.x, pencil.x, hinge.x) - 18f
        val maxX = maxOf(needle.x, pencil.x, hinge.x) + 18f
        val minY = minOf(needle.y, pencil.y, hinge.y) - 18f
        val maxY = maxOf(needle.y, pencil.y, hinge.y) + 18f
        Rect(minX, minY, maxX, maxY)
    }
    MathToolKind.SET_SQUARE -> {
        val half = tool.span / 2f
        val pad = 36f
        val corners = listOf(
            toCanvasPoint(Offset(-half, half), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(half, half), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(-half, -half - 28f), tool.center, tool.rotationDeg)
        )
        Rect(
            corners.minOf { it.x } - pad,
            corners.minOf { it.y } - pad,
            corners.maxOf { it.x } + pad,
            corners.maxOf { it.y } + pad
        )
    }
    MathToolKind.TRIANGLE -> {
        val side = tool.span * 1.8f
        val height = side * 0.8660254f
        val pad = 36f
        val corners = listOf(
            toCanvasPoint(Offset(0f, -height * 0.6667f), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(-side / 2f, height * 0.3333f), tool.center, tool.rotationDeg),
            toCanvasPoint(Offset(side / 2f, height * 0.3333f), tool.center, tool.rotationDeg)
        )
        Rect(
            corners.minOf { it.x } - pad,
            corners.minOf { it.y } - pad,
            corners.maxOf { it.x } + pad,
            corners.maxOf { it.y } + pad
        )
    }
}

fun toolsOverlap(a: PlacedMathTool, b: PlacedMathTool, margin: Float = 0f): Boolean {
    val ra = mathToolBoundingRect(a)
    val rb = mathToolBoundingRect(b)
    return ra.left < rb.right - margin && ra.right > rb.left + margin &&
           ra.top < rb.bottom - margin && ra.bottom > rb.top + margin
}

fun separateFrom(candidate: PlacedMathTool, existing: PlacedMathTool, separationMargin: Float = 8f): Offset {
    val ra = mathToolBoundingRect(candidate)
    val rb = mathToolBoundingRect(existing)

    val overlapX = minOf(ra.right, rb.right) - maxOf(ra.left, rb.left)
    val overlapY = minOf(ra.bottom, rb.bottom) - maxOf(ra.top, rb.top)

    if (overlapX <= 0f || overlapY <= 0f) return candidate.center

    val raCenterX = (ra.left + ra.right) / 2f
    val raCenterY = (ra.top + ra.bottom) / 2f
    val rbCenterX = (rb.left + rb.right) / 2f
    val rbCenterY = (rb.top + rb.bottom) / 2f

    return if (overlapX < overlapY) {
        val sign = if (raCenterX >= rbCenterX) 1f else -1f
        val shift = (overlapX + separationMargin) * sign
        Offset(candidate.center.x + shift, candidate.center.y)
    } else {
        val sign = if (raCenterY >= rbCenterY) 1f else -1f
        val shift = (overlapY + separationMargin) * sign
        Offset(candidate.center.x, candidate.center.y + shift)
    }
}

fun findNonOverlappingPosition(
    desiredCenter: Offset,
    kind: MathToolKind,
    span: Float,
    existingTools: List<PlacedMathTool>,
    canvasSize: Size = Size.Zero
): Offset {
    if (existingTools.isEmpty()) return desiredCenter

    var current = PlacedMathTool(kind = kind, center = desiredCenter, span = span)

    // If it doesn't overlap any existing tool, keep it exactly where the user set it!
    if (!existingTools.any { toolsOverlap(current, it) }) {
        return desiredCenter
    }

    val minX = 40f
    val maxX = if (canvasSize.width > 0) canvasSize.width - 40f else 4000f
    val minY = 60f
    val maxY = if (canvasSize.height > 0) canvasSize.height - 60f else 3000f

    // Try tight minimum separation relaxation (nudges just enough to clear overlap, keeping tools close)
    for (iter in 0 until 5) {
        val colliding = existingTools.firstOrNull { toolsOverlap(current, it) } ?: break
        val newCenter = separateFrom(current, colliding)
        val clampedCenter = Offset(newCenter.x.coerceIn(minX, maxX), newCenter.y.coerceIn(minY, maxY))
        current = current.copy(center = clampedCenter)
    }

    if (!existingTools.any { toolsOverlap(current, it) }) {
        return current.center
    }

    // Fallback: search in fine 20f steps nearby so it stays as close as possible
    var testDist = 20f
    val angles = listOf(0f, 180f, 90f, 270f, 45f, 135f, 225f, 315f)
    while (testDist <= 800f) {
        for (deg in angles) {
            val rad = Math.toRadians(deg.toDouble())
            val cx = (desiredCenter.x + testDist * Math.cos(rad)).toFloat().coerceIn(minX, maxX)
            val cy = (desiredCenter.y + testDist * Math.sin(rad)).toFloat().coerceIn(minY, maxY)
            val cand = current.copy(center = Offset(cx, cy))
            if (!existingTools.any { toolsOverlap(cand, it) }) {
                return cand.center
            }
        }
        testDist += 20f
    }

    return current.center
}

fun resolveNonOverlappingPosition(
    candidate: Offset,
    candidateKind: MathToolKind,
    candidateSpan: Float,
    existingTools: List<PlacedMathTool>
): Offset = findNonOverlappingPosition(candidate, candidateKind, candidateSpan, existingTools)

fun direction(rotationDeg: Float): Offset {
    val rad = rotationDeg / RAD_TO_DEG
    return Offset(cos(rad), sin(rad))
}

/** Local +Y, which points down the screen when rotation is 0. */
fun perpendicular(rotationDeg: Float): Offset {
    val rad = rotationDeg / RAD_TO_DEG
    return Offset(-sin(rad), cos(rad))
}

fun toLocal(point: Offset, center: Offset, rotationDeg: Float): Offset {
    val dir = direction(rotationDeg)
    val perp = perpendicular(rotationDeg)
    val relX = point.x - center.x
    val relY = point.y - center.y
    return Offset(relX * dir.x + relY * dir.y, relX * perp.x + relY * perp.y)
}

fun toWorld(local: Offset, center: Offset, rotationDeg: Float): Offset {
    val dir = direction(rotationDeg)
    val perp = perpendicular(rotationDeg)
    return center + dir * local.x + perp * local.y
}

private fun near(a: Offset, b: Offset, radius: Float): Boolean {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy <= radius * radius
}

fun hitTestMathTool(tool: PlacedMathTool, point: Offset): MathToolZone = when (tool.kind) {
    MathToolKind.RULER -> hitTestRuler(tool, point)
    MathToolKind.COMPASS -> hitTestCompass(tool, point)
    MathToolKind.PROTRACTOR -> hitTestProtractor(tool, point)
    MathToolKind.SET_SQUARE -> hitTestSetSquare(tool, point)
    MathToolKind.TRIANGLE -> hitTestTriangle(tool, point)
}

fun hitTestPlacedTools(tools: List<PlacedMathTool>, point: Offset): MathToolHit? {
    for (index in tools.indices.reversed()) {
        val tool = tools[index]
        val zone = hitTestMathTool(tool, point)
        if (zone != MathToolZone.NONE) return MathToolHit(tool, zone)
    }
    return null
}

private fun hitTestRuler(tool: PlacedMathTool, point: Offset): MathToolZone {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val half = tool.span / 2f
    val halfT = RULER_THICKNESS / 2f

    val distToTopEdge = abs(local.y - (-halfT))
    val distToBottomEdge = abs(local.y - halfT)
    val isNearDrawingEdge = (distToTopEdge <= RULER_EDGE_SLOP || distToBottomEdge <= RULER_EDGE_SLOP) &&
        abs(local.x) <= half + RULER_EDGE_SLOP &&
        abs(local.y) >= 20f

    // Close button (top right inside ruler) - safe area check:
    // Only triggers when tapping inside ruler body, never when drawing along top edge
    val closeBtnCenter = Offset(half - 24f, -halfT + 22f)
    if (near(local, closeBtnCenter, 20f) && distToTopEdge > 10f) {
        return MathToolZone.CLOSE
    }

    // If touching near the top edge or bottom edge (where ticks and numbers are),
    // always return NONE so straightedge line drawing triggers seamlessly!
    if (isNearDrawingEdge) {
        return MathToolZone.NONE
    }

    // Handles at positive (+half) and negative (-half) ends in the center strip:
    // Dragging these ends extends length and rotates angle!
    if (near(local, Offset(half, 0f), HANDLE_TOUCH_RADIUS) ||
        (local.x >= half - 32f && abs(local.y) <= 24f)) return MathToolZone.ROTATE_POSITIVE
    if (near(local, Offset(-half, 0f), HANDLE_TOUCH_RADIUS) ||
        (local.x <= -half + 32f && abs(local.y) <= 24f)) return MathToolZone.ROTATE_NEGATIVE

    // Ruler body for dragging/moving across canvas
    if (abs(local.x) < half - 28f && abs(local.y) <= 24f) {
        return MathToolZone.MOVE
    }
    return MathToolZone.NONE
}

fun distanceToRulerEdge(tool: PlacedMathTool, point: Offset): Float {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    when (tool.kind) {
        MathToolKind.RULER -> {
            val half = tool.span / 2f
            if (abs(local.x) > half + RULER_EDGE_SLOP) return Float.MAX_VALUE
            val halfT = RULER_THICKNESS / 2f
            return minOf(abs(local.y - halfT), abs(local.y + halfT))
        }
        MathToolKind.SET_SQUARE -> {
            val half = tool.span / 2f
            val vCorner = Offset(-half, half)
            val vBase = Offset(half, half)
            val vTop = Offset(-half, -half)
            val dBase = distanceToSegment(local, vCorner, vBase)
            val dHeight = distanceToSegment(local, vCorner, vTop)
            val dHyp = distanceToSegment(local, vTop, vBase)
            return minOf(dBase, dHeight, dHyp)
        }
        MathToolKind.PROTRACTOR -> return Float.MAX_VALUE
        MathToolKind.TRIANGLE -> {
            val side = tool.span * 1.8f
            val h = side * 0.8660254f
            val vTop = Offset(0f, -h * 0.6667f)
            val vLeft = Offset(-side / 2f, h * 0.3333f)
            val vRight = Offset(side / 2f, h * 0.3333f)
            val dBottom = distanceToSegment(local, vLeft, vRight)
            val dLeft = distanceToSegment(local, vTop, vLeft)
            val dRight = distanceToSegment(local, vTop, vRight)
            return minOf(dBottom, dLeft, dRight)
        }
        MathToolKind.COMPASS -> return Float.MAX_VALUE
    }
}

fun projectOntoSegment(p: Offset, a: Offset, b: Offset): Offset {
    val ab = b - a
    val lenSq = ab.x * ab.x + ab.y * ab.y
    if (lenSq < 0.0001f) return a
    val t = ((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / lenSq
    val clampedT = t.coerceIn(0f, 1f)
    return a + ab * clampedT
}

fun rulerEdgeForPoint(tool: PlacedMathTool, point: Offset): Float {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    return when (tool.kind) {
        MathToolKind.SET_SQUARE -> {
            val half = tool.span / 2f
            val vCorner = Offset(-half, half)
            val vBase = Offset(half, half)
            val vTop = Offset(-half, -half)
            val edges = listOf(
                vCorner to vBase, // 0: Base
                vCorner to vTop,  // 1: Height
                vTop to vBase     // 2: Hypotenuse
            )
            val idx = edges.indices.minByOrNull { distanceToSegment(local, edges[it].first, edges[it].second) } ?: 0
            idx.toFloat()
        }
        MathToolKind.TRIANGLE -> {
            val side = tool.span * 1.8f
            val h = side * 0.8660254f
            val vTop = Offset(0f, -h * 0.6667f)
            val vLeft = Offset(-side / 2f, h * 0.3333f)
            val vRight = Offset(side / 2f, h * 0.3333f)
            val edges = listOf(
                vLeft to vRight, // 0: Bottom
                vTop to vLeft,   // 1: Left slant
                vTop to vRight   // 2: Right slant
            )
            val idx = edges.indices.minByOrNull { distanceToSegment(local, edges[it].first, edges[it].second) } ?: 0
            idx.toFloat()
        }
        else -> if (local.y < 0f) -1f else 1f
    }
}

fun snapToRulerEdge(
    tool: PlacedMathTool,
    screenPoint: Offset,
    edgeSign: Float,
    pxPerCm: Float = 0f
): Offset {
    val local = toLocal(screenPoint, tool.center, tool.rotationDeg)
    when (tool.kind) {
        MathToolKind.SET_SQUARE -> {
            val half = tool.span / 2f
            val vCorner = Offset(-half, half)
            val vBase = Offset(half, half)
            val vTop = Offset(-half, -half)
            val edges = listOf(
                vCorner to vBase, // 0: Base
                vCorner to vTop,  // 1: Height
                vTop to vBase     // 2: Hypotenuse
            )
            val edgeIdx = edgeSign.toInt().coerceIn(0, 2)
            val (a, b) = edges[edgeIdx]
            val snappedLocal = projectOntoSegment(local, a, b)
            return toWorld(snappedLocal, tool.center, tool.rotationDeg)
        }
        MathToolKind.TRIANGLE -> {
            val side = tool.span * 1.8f
            val h = side * 0.8660254f
            val vTop = Offset(0f, -h * 0.6667f)
            val vLeft = Offset(-side / 2f, h * 0.3333f)
            val vRight = Offset(side / 2f, h * 0.3333f)
            val edges = listOf(
                vLeft to vRight, // 0: Bottom
                vTop to vLeft,   // 1: Left slant
                vTop to vRight   // 2: Right slant
            )
            val edgeIdx = edgeSign.toInt().coerceIn(0, 2)
            val (a, b) = edges[edgeIdx]
            val snappedLocal = projectOntoSegment(local, a, b)
            return toWorld(snappedLocal, tool.center, tool.rotationDeg)
        }
        else -> {
            val dir = direction(tool.rotationDeg)
            val perp = perpendicular(tool.rotationDeg)
            val half = tool.span / 2f
            val halfT = RULER_THICKNESS / 2f

            var clampedX = local.x.coerceIn(-half, half)

            if (tool.kind == MathToolKind.RULER && pxPerCm > 0f) {
                val inset = RULER_INSET
                val zeroX = -half + inset
                val maxMarkX = half - inset
                val mmPx = pxPerCm / 10f

                if (clampedX <= zeroX) {
                    clampedX = zeroX
                } else if (clampedX >= maxMarkX) {
                    clampedX = maxMarkX
                } else if (mmPx >= 1.5f) {
                    val relX = clampedX - zeroX
                    val nearestMm = (relX / mmPx).roundToInt()
                    val tickX = zeroX + nearestMm * mmPx
                    val isCm = nearestMm % 10 == 0
                    val isHalf = nearestMm % 5 == 0
                    val snapThreshold = when {
                        isCm -> 6.0f
                        isHalf -> 4.5f
                        else -> 3.2f
                    }
                    if (abs(clampedX - tickX) <= snapThreshold) {
                        clampedX = tickX.coerceIn(zeroX, maxMarkX)
                    }
                }
            }

            return tool.center + dir * clampedX + perp * (edgeSign * halfT)
        }
    }
}

fun rulerForStraightedge(tools: List<PlacedMathTool>, point: Offset): PlacedMathTool? {
    return tools.lastOrNull { tool ->
        (tool.kind == MathToolKind.RULER ||
            tool.kind == MathToolKind.SET_SQUARE ||
            tool.kind == MathToolKind.TRIANGLE) &&
            distanceToRulerEdge(tool, point) <= RULER_EDGE_SLOP
    }
}

fun compassLegLength(tool: PlacedMathTool): Float {
    val base = 280f * tool.toolScale
    return maxOf(base, tool.span / 1.96f).coerceIn(180f, 1800f)
}

fun compassPencil(tool: PlacedMathTool): Offset {
    return tool.center + direction(tool.rotationDeg) * tool.span
}

fun compassHinge(tool: PlacedMathTool): Offset {
    val pencil = compassPencil(tool)
    val mid = Offset((tool.center.x + pencil.x) / 2f, (tool.center.y + pencil.y) / 2f)
    val legLen = compassLegLength(tool)
    val halfSpan = (tool.span / 2f).coerceAtMost(legLen - 0.5f)
    val h = kotlin.math.sqrt((legLen * legLen - halfSpan * halfSpan).coerceAtLeast(0f))
    return mid - perpendicular(tool.rotationDeg) * h
}

fun compassTopRotateBtn(tool: PlacedMathTool): Offset {
    val hinge = compassHinge(tool)
    val scale = tool.toolScale.coerceIn(0.8f, 1.8f)
    val up = -perpendicular(tool.rotationDeg)
    val jointSize = 34f * scale
    val handleTop = hinge + up * (jointSize / 2f + 28f * scale)
    return handleTop + up * (26f * scale)
}

fun compassScaleBtn(tool: PlacedMathTool): Offset {
    val hinge = compassHinge(tool)
    val scale = tool.toolScale.coerceIn(0.8f, 1.8f)
    return hinge - direction(tool.rotationDeg) * (48f * scale)
}

fun compassCloseBtn(tool: PlacedMathTool): Offset {
    val hinge = compassHinge(tool)
    val scale = tool.toolScale.coerceIn(0.8f, 1.8f)
    return hinge + direction(tool.rotationDeg) * (48f * scale)
}

fun compassSpreadBtn(tool: PlacedMathTool): Offset {
    val hinge = compassHinge(tool)
    val pencil = compassPencil(tool)
    return hinge + (pencil - hinge) * 0.52f
}

fun pointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val d1 = (p.x - b.x) * (a.y - b.y) - (a.x - b.x) * (p.y - b.y)
    val d2 = (p.x - c.x) * (b.y - c.y) - (b.x - c.x) * (p.y - c.y)
    val d3 = (p.x - a.x) * (c.y - a.y) - (c.x - a.x) * (p.y - a.y)
    val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
    val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)
    return !(hasNeg && hasPos)
}

private fun hitTestCompass(tool: PlacedMathTool, point: Offset): MathToolZone {
    val scale = tool.toolScale.coerceIn(0.8f, 2.0f)
    val touchR = 36f * scale
    val hinge = compassHinge(tool)
    val up = -perpendicular(tool.rotationDeg)
    val jointSize = 34f * scale
    val handleTop = hinge + up * (jointSize / 2f + 28f * scale)

    // 1. Close button (X) on right side of hinge
    val closeBtn = compassCloseBtn(tool)
    if (near(point, closeBtn, touchR)) return MathToolZone.CLOSE

    // 2. Rotate handle (⟳) floating above top handle outside the compass
    val topRotate = compassTopRotateBtn(tool)
    if (near(point, topRotate, touchR * 1.2f)) return MathToolZone.ROTATE

    // 3. Scale handle (⤡) on left side of hinge
    val scaleBtn = compassScaleBtn(tool)
    if (near(point, scaleBtn, touchR)) return MathToolZone.SCALE

    // 4. Spread handle (➔ ⬅) on right leg
    val spreadBtn = compassSpreadBtn(tool)
    if (near(point, spreadBtn, touchR * 1.1f)) return MathToolZone.SPREAD

    // 5. Pencil tip handle (🖊) at pencil tip
    val pencil = compassPencil(tool)
    if (near(point, pencil, touchR * 1.2f)) return MathToolZone.PENCIL

    // 6. Needle anchor point (left tip) -> MOVE
    if (near(point, tool.center, touchR * 1.2f)) return MathToolZone.MOVE

    // 7. Touch anywhere on the compass body (hinge, handle, legs, or interior) -> MOVE smoothly across board!
    if (distanceToSegment(point, hinge, handleTop) <= 36f * scale) return MathToolZone.MOVE
    if (near(point, hinge, touchR * 1.5f)) return MathToolZone.MOVE
    if (distanceToSegment(point, hinge, tool.center) <= 36f * scale) return MathToolZone.MOVE
    if (distanceToSegment(point, hinge, pencil) <= 36f * scale) return MathToolZone.MOVE
    if (distanceToSegment(point, tool.center, pencil) <= 32f * scale) return MathToolZone.MOVE
    if (pointInTriangle(point, hinge, tool.center, pencil)) return MathToolZone.MOVE

    return MathToolZone.NONE
}

private fun hitTestProtractor(tool: PlacedMathTool, point: Offset): MathToolZone {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val span = tool.span
    val shelfH = (span * 0.22f).coerceIn(48f, 64f)
    val btnY = shelfH * 0.5f
    val btnR = (shelfH * 0.36f).coerceIn(16f, 24f)
    val touchR = btnR * 1.35f

    // 1. Floating Outer Rotate Handle above top apex outside the arc
    val topRotatePos = Offset(0f, -span - 28f)
    if (near(local, topRotatePos, touchR * 1.2f)) return MathToolZone.ROTATE

    // 2. Bottom shelf buttons (from left to right):
    val closePos = Offset(-0.75f * span, btnY)
    if (near(local, closePos, touchR)) return MathToolZone.CLOSE

    val mode1Pos = Offset(-0.45f * span, btnY)
    if (near(local, mode1Pos, touchR)) return MathToolZone.ANGLE_MODE_1

    val arcLeftPos = Offset(-0.15f * span, btnY)
    if (near(local, arcLeftPos, touchR)) return MathToolZone.ANGLE_MODE_ARC_LEFT

    val modeArcPos = Offset(0.15f * span, btnY)
    if (near(local, modeArcPos, touchR)) return MathToolZone.ANGLE_MODE_ARC

    val mode2Pos = Offset(0.45f * span, btnY)
    if (near(local, mode2Pos, touchR)) return MathToolZone.ANGLE_MODE_2

    val scalePos = Offset(0.75f * span, btnY)
    if (near(local, scalePos, touchR)) return MathToolZone.SCALE

    // 3. Interactive Pointer Arm handle tab on the arc
    val pRad = Math.toRadians(tool.pointerAngleDeg.toDouble()).toFloat()
    val armTip = Offset(span * cos(pRad), -span * sin(pRad))
    if (near(local, armTip, 36f)) return MathToolZone.ANGLE_ARM

    // 4. Baseline handle tabs at 0° and 180°
    val base0Tip = Offset(span, 0f)
    if (near(local, base0Tip, 36f)) return MathToolZone.ANGLE_ARM

    val base180Tip = Offset(-span, 0f)
    if (near(local, base180Tip, 36f)) return MathToolZone.ANGLE_ARM

    // 5. Pointer arm segment drag
    if (distanceToSegment(local, Offset.Zero, armTip) <= 24f) return MathToolZone.ANGLE_ARM

    // 6. Sliding along outer rim to adjust angle
    val r = hypot(local.x, local.y)
    if (local.y <= 12f && abs(r - span) <= 28f) return MathToolZone.ANGLE_ARM

    // 7. Inside protractor body -> MOVE
    if (local.y <= 0f && r <= span + 14f) return MathToolZone.MOVE
    if (local.y in 0f..(shelfH + 10f) && abs(local.x) <= span + 18f) return MathToolZone.MOVE

    return MathToolZone.NONE
}

private fun hitTestSetSquare(tool: PlacedMathTool, point: Offset): MathToolZone {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val span = tool.span
    val half = span / 2f
    val touchR = 24f

    // 1. Floating Outer Rotate handle [⟳] above top apex outside the figure
    val rotPos = Offset(-half, -half - 28f)
    if (near(local, rotPos, touchR * 1.2f)) return MathToolZone.ROTATE

    // 2. Close [X] inside
    val closePos = Offset(-half * 0.50f, -half * 0.30f)
    if (near(local, closePos, touchR)) return MathToolZone.CLOSE

    // 3. Middle Scale handle [⤡] (comfortably inside the set square interior)
    val scalePos = Offset(half * 0.10f, half * 0.55f)
    if (near(local, scalePos, touchR) || near(local, Offset(half * 0.40f, half * 0.40f), touchR)) return MathToolZone.SCALE

    // 4. Edges: If touching near ANY of the 3 edges (Base, Height, Hypotenuse),
    // return NONE so edge drawing triggers seamlessly exactly like a ruler!
    val vCorner = Offset(-half, half)
    val vBase = Offset(half, half)
    val vTop = Offset(-half, -half)
    val dBase = distanceToSegment(local, vCorner, vBase)
    val dHeight = distanceToSegment(local, vCorner, vTop)
    val dHyp = distanceToSegment(local, vTop, vBase)
    val distToEdge = minOf(dBase, dHeight, dHyp)
    if (distToEdge <= 24f) {
        return MathToolZone.NONE
    }

    // 5. Inside right triangle body -> MOVE
    val inX = local.x in (-half - 12f)..(half + 12f)
    val inY = local.y in (-half - 12f)..(half + 12f)
    val inHyp = local.x - local.y <= 16f
    if (inX && inY && inHyp) return MathToolZone.MOVE

    return MathToolZone.NONE
}

private fun hitTestTriangle(tool: PlacedMathTool, point: Offset): MathToolZone {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val side = tool.span * 1.8f
    val h = side * 0.8660254f
    val touchR = 24f

    // 1. Floating Outer Rotate handle [⟳] above top apex outside the figure
    val rotatePos = Offset(0f, -h * 0.6667f - 28f)
    if (near(local, rotatePos, touchR * 1.2f)) return MathToolZone.ROTATE

    // 2. Close button [X]
    val closePos = Offset(0f, -h * 0.25f)
    if (near(local, closePos, touchR * 1.2f)) return MathToolZone.CLOSE

    // 3. Middle Scale handle [⤡] (in lower-right body)
    val scalePos = Offset(side * 0.22f, h * 0.08f)
    if (near(local, scalePos, touchR * 1.2f)) return MathToolZone.SCALE

    // 4. Edges: If touching near ANY of the 3 edges (Bottom, Left, Right slant),
    // return NONE so edge drawing triggers seamlessly exactly like a ruler!
    val vTop = Offset(0f, -h * 0.6667f)
    val vLeft = Offset(-side / 2f, h * 0.3333f)
    val vRight = Offset(side / 2f, h * 0.3333f)
    val dBottom = distanceToSegment(local, vLeft, vRight)
    val dLeft = distanceToSegment(local, vTop, vLeft)
    val dRight = distanceToSegment(local, vTop, vRight)
    val distToEdge = minOf(dBottom, dLeft, dRight)
    if (distToEdge <= 24f) {
        return MathToolZone.NONE
    }

    if (pointInTriangle(local, vTop, vLeft, vRight, 16f)) {
        return MathToolZone.MOVE
    }

    return MathToolZone.NONE
}

private fun pointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset, margin: Float = 0f): Boolean {
    fun sign(p1: Offset, p2: Offset, p3: Offset): Float =
        (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)

    val d1 = sign(p, a, b)
    val d2 = sign(p, b, c)
    val d3 = sign(p, c, a)

    val hasNeg = (d1 < -margin) || (d2 < -margin) || (d3 < -margin)
    val hasPos = (d1 > margin) || (d2 > margin) || (d3 > margin)

    return !(hasNeg && hasPos)
}

private fun distanceToSegment(point: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val len2 = abx * abx + aby * aby
    if (len2 <= 1f) return hypot(point.x - a.x, point.y - a.y)
    val t = (((point.x - a.x) * abx + (point.y - a.y) * aby) / len2).coerceIn(0f, 1f)
    val px = a.x + abx * t
    val py = a.y + aby * t
    return hypot(point.x - px, point.y - py)
}

/** Distance from the tool center to either end handle. */
fun endReach(tool: PlacedMathTool): Float = if (tool.kind == MathToolKind.RULER) tool.span / 2f else tool.span

/** Rotates around the opposite end so the dragged end follows the finger. Span stays fixed. */
fun rotateToolByEnd(tool: PlacedMathTool, pointer: Offset, draggingPositiveEnd: Boolean): PlacedMathTool {
    val pivot = endPoint(tool, positive = !draggingPositiveEnd)
    val dx = pointer.x - pivot.x
    val dy = pointer.y - pivot.y
    if (dx * dx + dy * dy < 25f) return tool
    val angle = atan2(dy, dx) * RAD_TO_DEG
    val rawRotation = if (draggingPositiveEnd) angle else angle + 180f
    var norm = (rawRotation % 360f + 360f) % 360f

    // Stronger magnetic snap to cardinal angles (0°, 90°, 180°, 270°, 360°) within 3.5°
    val cardinalInterval = 90f
    val nearestCardinal = (norm / cardinalInterval).roundToInt() * cardinalInterval
    if (abs(norm - nearestCardinal) <= 3.5f) {
        norm = (nearestCardinal % 360f + 360f) % 360f
    } else {
        // Standard magnetic angle snapping: snap to nearest 15° if within 2.2°
        val snapInterval = 15f
        val nearestSnap = (norm / snapInterval).roundToInt() * snapInterval
        if (abs(norm - nearestSnap) <= 2.2f) {
            norm = (nearestSnap % 360f + 360f) % 360f
        }
    }

    val dir = direction(norm)
    val reach = endReach(tool)
    val center = if (draggingPositiveEnd) pivot + dir * reach else pivot - dir * reach
    return tool.copy(center = center, rotationDeg = norm)
}

/**
 * Dragging either end of the ruler rotates AND extends/shortens the ruler.
 * The opposite end remains anchored as the pivot point.
 */
fun dragRulerEnd(tool: PlacedMathTool, pointer: Offset, draggingPositiveEnd: Boolean): PlacedMathTool {
    val pivot = endPoint(tool, positive = !draggingPositiveEnd)
    val dx = pointer.x - pivot.x
    val dy = pointer.y - pivot.y
    if (dx * dx + dy * dy < 36f) return tool
    val dist = hypot(dx, dy)
    val newSpan = clampMathSpan(MathToolKind.RULER, dist)

    val angle = atan2(dy, dx) * RAD_TO_DEG
    val rawRotation = if (draggingPositiveEnd) angle else angle + 180f
    var norm = (rawRotation % 360f + 360f) % 360f

    // Stronger magnetic snap to cardinal angles (0°, 90°, 180°, 270°, 360°) within 3.5°
    val cardinalInterval = 90f
    val nearestCardinal = (norm / cardinalInterval).roundToInt() * cardinalInterval
    if (abs(norm - nearestCardinal) <= 3.5f) {
        norm = (nearestCardinal % 360f + 360f) % 360f
    } else {
        // Standard magnetic angle snapping: snap to nearest 15° if within 2.5°
        val snapInterval = 15f
        val nearestSnap = (norm / snapInterval).roundToInt() * snapInterval
        if (abs(norm - nearestSnap) <= 2.5f) {
            norm = (nearestSnap % 360f + 360f) % 360f
        }
    }

    val dir = direction(norm)
    val reach = newSpan / 2f
    val center = if (draggingPositiveEnd) pivot + dir * reach else pivot - dir * reach
    return tool.copy(center = center, rotationDeg = norm, span = newSpan)
}

fun endPoint(tool: PlacedMathTool, positive: Boolean): Offset {
    val sign = if (positive) 1f else -1f
    return tool.center + direction(tool.rotationDeg) * (sign * endReach(tool))
}

fun compassAngleDeg(tool: PlacedMathTool, pointer: Offset): Float {
    return atan2(pointer.y - tool.center.y, pointer.x - tool.center.x) * RAD_TO_DEG
}

fun shortestAngleDelta(fromDeg: Float, toDeg: Float): Float {
    var delta = toDeg - fromDeg
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return delta
}

/**
 * Dragging the pencil sets the radius. A sweep that stays near the current radius
 * becomes an arc instead of a radius change.
 */
fun dragCompassPencil(tool: PlacedMathTool, pointer: Offset, gesture: MathGesture): PlacedMathTool {
    val angle = compassAngleDeg(tool, pointer)
    val radius = hypot(pointer.x - tool.center.x, pointer.y - tool.center.y)
        .coerceIn(COMPASS_MIN_RADIUS, COMPASS_MAX_RADIUS)
    if (!gesture.arcMode && gesture.movedPx > MATH_TAP_SLOP) {
        val radial = abs(radius - gesture.startSpan)
        val angular = abs(shortestAngleDelta(gesture.startAngleDeg, angle))
        if (angular >= ARC_ANGLE_SLOP && radial <= ARC_RADIAL_SLOP) {
            gesture.arcMode = true
        }
    }
    gesture.sweepDeg += shortestAngleDelta(gesture.lastAngleDeg, angle)
    gesture.lastAngleDeg = angle
    return if (gesture.arcMode) {
        tool.copy(rotationDeg = angle, span = gesture.startSpan)
    } else {
        tool.copy(rotationDeg = angle, span = radius)
    }
}

fun compassArcScreenPoints(tool: PlacedMathTool, startDeg: Float, sweepDeg: Float): List<Offset> {
    val steps = (abs(sweepDeg) / 6f).roundToInt().coerceIn(8, 72)
    return (0..steps).map { index ->
        val deg = startDeg + sweepDeg * (index / steps.toFloat())
        val rad = deg / RAD_TO_DEG
        tool.center + Offset(cos(rad) * tool.span, sin(rad) * tool.span)
    }
}

fun isFullCircleSweep(sweepDeg: Float): Boolean = abs(sweepDeg) >= 345f

/** Angle from the protractor baseline, 0° at the positive end through 180° at the negative end. */
fun protractorAngle(tool: PlacedMathTool, point: Offset): Float? {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val y = if (local.y >= -16f) local.y.coerceAtLeast(0f) else kotlin.math.abs(local.y)
    var deg = Math.toDegrees(atan2(y.toDouble(), local.x.toDouble())).toFloat()
    if (deg < 0f) deg += 360f
    if (deg > 180.5f) return null
    return deg.coerceIn(0f, 180f)
}

fun rimPoint(center: Offset, rotationDeg: Float, angleDeg: Float, radius: Float): Offset {
    val rad = angleDeg / RAD_TO_DEG
    val dir = direction(rotationDeg)
    val down = perpendicular(rotationDeg)
    return center + dir * (cos(rad) * radius) + down * (sin(rad) * radius)
}

fun translateAndRotate(tool: PlacedMathTool, pan: Offset, rotationDeltaDeg: Float): PlacedMathTool {
    return tool.copy(
        center = tool.center + pan,
        rotationDeg = tool.rotationDeg + rotationDeltaDeg
    )
}

fun droppedOnToolbar(fingerInRoot: Offset, dockInRoot: Rect?): Boolean {
    if (dockInRoot == null) return false
    return dockInRoot.inflate(28f).contains(fingerInRoot)
}

fun formatCentimetres(screenPx: Float, pxPerCm: Float): String {
    val cm = screenPx / pxPerCm.coerceAtLeast(1f)
    return String.format(Locale.US, "%.1f cm", cm)
}

fun formatDegrees(angle: Float): String = "${angle.roundToInt().coerceIn(0, 180)}°"

fun screenLengthPx(startWorld: Offset, endWorld: Offset, scale: Float): Float {
    val dx = (endWorld.x - startWorld.x) * scale
    val dy = (endWorld.y - startWorld.y) * scale
    return hypot(dx, dy)
}

/** Projects the freehand point onto the ruler's long axis. World axes match the screen. */
fun projectOntoRuler(startWorld: Offset, currentWorld: Offset, dir: Offset, scale: Float): Offset {
    val safe = scale.coerceAtLeast(0.001f)
    val relX = (currentWorld.x - startWorld.x) * safe
    val relY = (currentWorld.y - startWorld.y) * safe
    val along = relX * dir.x + relY * dir.y
    val worldT = along / safe
    return Offset(startWorld.x + dir.x * worldT, startWorld.y + dir.y * worldT)
}

fun circleStroke(centerWorld: Offset, radiusWorld: Float, color: Int, width: Float): Stroke {
    return Stroke(
        points = listOf(
            Offset(centerWorld.x - radiusWorld, centerWorld.y - radiusWorld),
            Offset(centerWorld.x + radiusWorld, centerWorld.y + radiusWorld)
        ),
        colorValue = color,
        strokeWidth = width,
        strokeType = StrokeType.CIRCLE
    )
}

fun arcStroke(points: List<Offset>, color: Int, width: Float): Stroke {
    return Stroke(
        points = points,
        colorValue = color,
        strokeWidth = width,
        strokeType = StrokeType.PEN
    )
}

fun straightStroke(start: Offset, end: Offset, color: Int, width: Float): Stroke {
    return Stroke(
        points = listOf(start, end),
        colorValue = color,
        strokeWidth = width,
        strokeType = StrokeType.LINE
    )
}

fun measurementLabel(text: String, at: Offset, color: Int, angleDeg: Float = 0f): Stroke {
    val safeText = text.replace("┬░", "\u00B0")
    val safeColor = if (color != 0) color else 0xFF1E293B.toInt()
    val kind = if (angleDeg == 0f) "measurement" else "measurement:${angleDeg.roundToInt()}"
    return Stroke(
        points = listOf(at),
        colorValue = safeColor,
        strokeWidth = 16f,
        strokeType = StrokeType.TEXT,
        stickerKind = kind,
        text = safeText
    )
}

fun screenPointsToWorld(points: List<Offset>, scale: Float, offset: Offset): List<Offset> {
    return points.map { CanvasTransform.screenToWorld(it, scale, offset) }
}

fun DrawScope.drawMathTool(
    tool: PlacedMathTool,
    isDark: Boolean,
    pxPerCm: Float,
    segmentLabel: String? = null
) {
    when (tool.kind) {
        MathToolKind.RULER -> drawRulerTool(tool, isDark, pxPerCm, segmentLabel)
        MathToolKind.COMPASS -> drawCompassTool(tool, isDark, pxPerCm)
        MathToolKind.PROTRACTOR -> drawProtractorTool(tool, isDark, pxPerCm)
        MathToolKind.SET_SQUARE -> drawSetSquareTool(tool, isDark, pxPerCm)
        MathToolKind.TRIANGLE -> drawTriangleTool(tool, isDark, pxPerCm)
    }
}

private fun DrawScope.drawRulerTool(
    tool: PlacedMathTool,
    isDark: Boolean,
    pxPerCm: Float,
    segmentLabel: String?
) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val s = (tool.span / defaultMathSpan(tool.kind)).coerceIn(0.85f, 3.5f)
        val length = tool.span
        val thickness = RULER_THICKNESS
        val left = tool.center.x - length / 2f
        val top = tool.center.y - thickness / 2f
        val body = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
        val border = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val tick = if (isDark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
        val tickSubtle = if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
        val handle = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

        // Transparent acrylic body & crisp black/white border
        drawRoundRect(body, Offset(left, top), Size(length, thickness), CornerRadius(12f, 12f))
        drawRoundRect(border, Offset(left, top), Size(length, thickness), CornerRadius(12f, 12f), style = DrawStroke(1.5f))

        // Center grip indicator
        val gripY = tool.center.y
        for (gx in listOf(-12f, 0f, 12f)) {
            drawRoundRect(
                color = if (isDark) Color(0x33FFFFFF) else Color(0x28000000),
                topLeft = Offset(tool.center.x + gx - 2f, gripY - 14f),
                size = Size(4f, 28f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Ticks and centimeter numbers along both edges
        val mm = (pxPerCm / 10f).coerceAtLeast(1.5f)
        val inset = RULER_INSET
        val x0 = left + inset
        val usableWidth = (length - 2 * inset).coerceAtLeast(0f)
        val totalMm = (usableWidth / mm).toInt()

        for (index in 0..totalMm) {
            val x = x0 + index * mm
            val isCm = index % 10 == 0
            val isHalf = index % 5 == 0
            val tickHeight = when {
                isCm -> thickness * 0.36f
                isHalf -> thickness * 0.24f
                else -> thickness * 0.14f
            }
            val width = if (isCm) 1.8f else 1f
            val curColor = if (isCm) Color(tick) else Color(tickSubtle)
            // Top edge ticks
            drawLine(curColor, Offset(x, top + 2f), Offset(x, top + 2f + tickHeight), width)
            // Bottom edge ticks
            drawLine(curColor, Offset(x, top + thickness - 2f), Offset(x, top + thickness - 2f - tickHeight), width)

            if (isCm) {
                val cmVal = index / 10
                val cmFontSize = (14f * s).coerceIn(12.5f, 32f)
                drawLabel("$cmVal", Offset(x, top + thickness / 2f + 5f), tick, cmFontSize, isBold = true)
                if (cmVal == 0) {
                    val unitFontSize = (11f * s).coerceIn(9.5f, 24f)
                    drawLabel("cm", Offset(x + 14f * s.coerceIn(1f, 1.8f), top + thickness / 2f + 5f), tickSubtle, unitFontSize, isBold = true)
                }
            }
        }

        // Length readout label above ruler matching exact usable markings
        val capacityCm = totalMm / 10f
        val capacityText = String.format(Locale.US, "%.1f cm", capacityCm)
        val rulerTitle = "Ruler  •  $capacityText"
        drawLabel(rulerTitle, Offset(tool.center.x, top - 22f * s.coerceIn(1f, 1.5f)), tick, (15f * s).coerceIn(13f, 32f), isBold = true)

        if (segmentLabel != null) {
            drawLabel(segmentLabel, Offset(tool.center.x, top + thickness + 24f * s.coerceIn(1f, 1.5f)), tick, (14f * s).coerceIn(12f, 30f), isBold = true)
        }

        // Close button (×) at top-right inside ruler
        val closeCenter = Offset(left + length - 24f, top + 22f)
        drawStandardCloseButton(closeCenter, 12f, isDark)

        // Rotation & extension handles at both ends
        listOf(Offset(left, tool.center.y), Offset(left + length, tool.center.y)).forEach { handleCenter ->
            drawCircle(if (isDark) Color(0xEE1E293B) else Color.White, 16f, handleCenter)
            drawCircle(handle, 16f, handleCenter, style = DrawStroke(1.8f))
            drawCircle(handle, 3.5f, handleCenter)
            drawLine(handle, handleCenter - Offset(6f, 0f), handleCenter + Offset(6f, 0f), 1.6f)
        }
    }
}

private fun DrawScope.drawCompassTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    val pencil = compassPencil(tool)
    val hinge = compassHinge(tool)
    val borderColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val legColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    val jointColor = if (isDark) Color(0xEE1E293B) else Color(0xFAFFFFFF)
    val scaleFactor = tool.toolScale.coerceIn(0.7f, 2.2f)
    val btnR = 24f * scaleFactor

    // 0. Soft dashed guide circle around needle point
    drawCircle(
        color = if (isDark) Color(0x28FFFFFF) else Color(0x20000000),
        radius = tool.span,
        center = tool.center,
        style = DrawStroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
    )

    // 1. Compass Legs
    val legW = 8f * scaleFactor
    // Left leg (Needle leg)
    drawLine(legColor, hinge, tool.center, strokeWidth = legW, cap = StrokeCap.Round)
    // Needle point at bottom-left
    drawLine(borderColor, tool.center - (hinge - tool.center) * 0.08f, tool.center, strokeWidth = 3.5f * scaleFactor, cap = StrokeCap.Round)
    drawCircle(borderColor, 4.5f * scaleFactor, tool.center)

    // Right leg (Pencil Leg)
    val pencilDir = (pencil - hinge)
    val pencilLen = pencilDir.getDistance().coerceAtLeast(1f)
    val pNorm = pencilDir / pencilLen
    val pPerp = Offset(-pNorm.y, pNorm.x)

    // Metal upper leg from hinge down to pencil clamp (about 50% of the leg)
    val clampPos = hinge + pNorm * (pencilLen * 0.50f)
    drawLine(legColor, hinge, clampPos, strokeWidth = legW, cap = StrokeCap.Round)

    // Pencil Clamp bracket
    val clampW = 14f * scaleFactor
    val clampH = 18f * scaleFactor
    drawRoundRect(
        color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
        topLeft = clampPos - Offset(clampW / 2f, clampH / 2f),
        size = Size(clampW, clampH),
        cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
    )
    drawCircle(borderColor, 2.5f * scaleFactor, clampPos)

    // Pencil attached from clamp down to pencil tip
    val pencilShaftTop = clampPos - pNorm * (6f * scaleFactor)
    val pencilWoodTop = pencil - pNorm * (30f * scaleFactor)
    val pencilLeadTop = pencil - pNorm * (12f * scaleFactor)
    val shaftWidth = 10f * scaleFactor

    val pLeftTop = pencilShaftTop - pPerp * (shaftWidth / 2f)
    val pRightTop = pencilShaftTop + pPerp * (shaftWidth / 2f)
    val pLeftWood = pencilWoodTop - pPerp * (shaftWidth / 2f)
    val pRightWood = pencilWoodTop + pPerp * (shaftWidth / 2f)

    val pencilBodyPath = Path().apply {
        moveTo(pLeftTop.x, pLeftTop.y)
        lineTo(pRightTop.x, pRightTop.y)
        lineTo(pRightWood.x, pRightWood.y)
        lineTo(pLeftWood.x, pLeftWood.y)
        close()
    }
    drawPath(pencilBodyPath, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    drawLine(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), pencilShaftTop, pencilWoodTop, strokeWidth = 2f * scaleFactor)

    // Silver metal ferrule band
    val ferruleTop = pencilShaftTop - pNorm * (8f * scaleFactor)
    val ferrulePath = Path().apply {
        moveTo(ferruleTop.x - pPerp.x * shaftWidth / 2f, ferruleTop.y - pPerp.y * shaftWidth / 2f)
        lineTo(ferruleTop.x + pPerp.x * shaftWidth / 2f, ferruleTop.y + pPerp.y * shaftWidth / 2f)
        lineTo(pRightTop.x, pRightTop.y)
        lineTo(pLeftTop.x, pLeftTop.y)
        close()
    }
    drawPath(ferrulePath, if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))

    // Sharpened cone
    val woodConePath = Path().apply {
        moveTo(pLeftWood.x, pLeftWood.y)
        lineTo(pRightWood.x, pRightWood.y)
        lineTo(pencilLeadTop.x, pencilLeadTop.y)
        close()
    }
    drawPath(woodConePath, if (isDark) Color(0xFF64748B) else Color(0xFFD1D5DB))

    // Graphite lead tip
    val leadPath = Path().apply {
        val leadLeft = pencilWoodTop + (pencil - pencilWoodTop) * 0.62f - pPerp * (shaftWidth * 0.2f)
        val leadRight = pencilWoodTop + (pencil - pencilWoodTop) * 0.62f + pPerp * (shaftWidth * 0.2f)
        moveTo(leadLeft.x, leadLeft.y)
        lineTo(leadRight.x, leadRight.y)
        lineTo(pencil.x, pencil.y)
        close()
    }
    drawPath(leadPath, borderColor)

    // 2. Central Hinge Block & Top Grip Handle
    val jointSize = 34f * scaleFactor
    drawRoundRect(
        color = jointColor,
        topLeft = Offset(hinge.x - jointSize / 2f, hinge.y - jointSize / 2f),
        size = Size(jointSize, jointSize),
        cornerRadius = CornerRadius(6f * scaleFactor, 6f * scaleFactor)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(hinge.x - jointSize / 2f, hinge.y - jointSize / 2f),
        size = Size(jointSize, jointSize),
        cornerRadius = CornerRadius(6f * scaleFactor, 6f * scaleFactor),
        style = DrawStroke(1.8f)
    )

    // Top vertical grip handle
    val up = -perpendicular(tool.rotationDeg)
    val handleBase = hinge + up * (jointSize / 2f)
    val handleTop = handleBase + up * (28f * scaleFactor)
    drawLine(legColor, handleBase, handleTop, strokeWidth = 10f * scaleFactor, cap = StrokeCap.Round)
    drawLine(borderColor, handleBase, handleTop, strokeWidth = 10f * scaleFactor, cap = StrokeCap.Round)

    // 3. Distance Readout below the hinge
    val s = (tool.span / defaultMathSpan(tool.kind)).coerceIn(0.85f, 3.5f)
    val readoutAt = hinge - up * (28f * s.coerceIn(1f, 2f))
    val distCm = (tool.span / pxPerCm.coerceAtLeast(1f))
    val rW = 82f * s.coerceIn(1f, 2.2f)
    val rH = 28f * s.coerceIn(1f, 2.2f)
    drawRoundRect(
        color = if (isDark) Color(0xEE1E293B) else Color(0xFAFFFFFF),
        topLeft = Offset(readoutAt.x - rW / 2f, readoutAt.y - rH / 2f),
        size = Size(rW, rH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(readoutAt.x - rW / 2f, readoutAt.y - rH / 2f),
        size = Size(rW, rH),
        cornerRadius = CornerRadius(8f, 8f),
        style = DrawStroke(1.4f)
    )
    drawLabel(String.format(Locale.US, "%.2f cm", distCm), Offset(readoutAt.x, readoutAt.y + 4f * s.coerceIn(1f, 2f)), if (isDark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt(), (13f * s).coerceIn(11f, 30f), isBold = true)

    // 4. Action Buttons
    val iconColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    // [⟳] Top rotate button floating outside above handle
    val topRotate = compassTopRotateBtn(tool)
    drawLine(borderColor, handleTop, topRotate, strokeWidth = 1.8f)
    drawButtonCircle(topRotate, btnR, borderColor, isDark)
    drawRotateIcon(topRotate, btnR * 0.5f, iconColor)

    // [⤡] Scale handle on left side of hinge
    val scaleBtn = compassScaleBtn(tool)
    drawButtonCircle(scaleBtn, btnR, borderColor, isDark)
    drawScaleIcon(scaleBtn, btnR * 0.5f, iconColor)

    // [X] Standard Close button
    val closeBtn = compassCloseBtn(tool)
    drawStandardCloseButton(closeBtn, btnR, isDark)

    // Spread adjustment handle on right leg
    val spreadBtn = compassSpreadBtn(tool)
    drawButtonCircle(spreadBtn, btnR, borderColor, isDark)
    drawSpreadIcon(spreadBtn, btnR * 0.5f, iconColor)

    // Pencil tip handle at bottom-right
    val pencilBtn = pencil
    drawButtonCircle(pencilBtn, btnR, borderColor, isDark)
    drawPencilIcon(pencilBtn, btnR * 0.5f, iconColor)
}

private fun DrawScope.drawSetSquareTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val span = tool.span
        val half = span / 2f
        val cx = tool.center.x
        val cy = tool.center.y

        val bodyColor = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
        val borderColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val tickColorInt = if (isDark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
        val tickCompose = Color(tickColorInt)

        // 1. Standard 45°-90°-45° Right Triangle
        val pCorner = Offset(cx - half, cy + half)
        val pBase = Offset(cx + half, cy + half)
        val pTop = Offset(cx - half, cy - half)

        val triPath = Path().apply {
            moveTo(pCorner.x, pCorner.y)
            lineTo(pBase.x, pBase.y)
            lineTo(pTop.x, pTop.y)
            close()
        }
        drawPath(triPath, bodyColor)
        drawPath(triPath, borderColor, style = DrawStroke(2f))

        // 2. 90° Right Angle Marker at corner (indicator lines stay strictly inside boundary, no angle text)
        val s = (tool.span / defaultMathSpan(tool.kind)).coerceIn(0.85f, 3.5f)
        val sqSize = 14f * s.coerceIn(1f, 1.4f)
        val rightAnglePath = Path().apply {
            moveTo(pCorner.x, pCorner.y - sqSize)
            lineTo(pCorner.x + sqSize, pCorner.y - sqSize)
            lineTo(pCorner.x + sqSize, pCorner.y)
        }
        drawPath(rightAnglePath, borderColor, style = DrawStroke(1.4f))
        drawCircle(borderColor, 2f * s.coerceIn(1f, 2f), pCorner + Offset(sqSize / 2f, -sqSize / 2f))

        // 3. 45° Angle Corner Arcs at Base and Top (strictly interior between edges and hypotenuse, no angle text)
        val arcR = 22f * s.coerceIn(1f, 1.4f)
        drawArc(borderColor, 180f, 45f, false, topLeft = Offset(pBase.x - arcR, pBase.y - arcR), size = Size(arcR * 2f, arcR * 2f), style = DrawStroke(1.2f))
        drawArc(borderColor, 45f, 45f, false, topLeft = Offset(pTop.x - arcR, pTop.y - arcR), size = Size(arcR * 2f, arcR * 2f), style = DrawStroke(1.2f))

        // 4. Centimeter markings along Base (pCorner -> pBase) - ALL numbers 1..maxCm
        val mm = (pxPerCm / 10f).coerceAtLeast(2.5f)
        var mmIdx = 0
        val legCm = span / pxPerCm.coerceAtLeast(1f)
        while (mmIdx * mm <= span - 4f) {
            val dist = mmIdx * mm
            val isCm = mmIdx % 10 == 0
            val is5 = mmIdx % 5 == 0
            val tH = if (isCm) 14f else if (is5) 9f else 5f
            drawLine(tickCompose, Offset(pCorner.x + dist, pCorner.y), Offset(pCorner.x + dist, pCorner.y - tH), if (isCm) 1.5f else 1f)
            if (isCm && mmIdx > 0 && dist < span - 10f) {
                drawLabel("${mmIdx / 10}", Offset(pCorner.x + dist, pCorner.y - 20f * s.coerceIn(1f, 1.8f)), tickColorInt, (12f * s).coerceIn(10f, 18f), isBold = true)
            }
            mmIdx++
        }

        // 5. Centimeter markings along Vertical Height (pCorner -> pTop) - ALL numbers 1..maxCm
        mmIdx = 0
        while (mmIdx * mm <= span - 4f) {
            val dist = mmIdx * mm
            val isCm = mmIdx % 10 == 0
            val is5 = mmIdx % 5 == 0
            val tW = if (isCm) 14f else if (is5) 9f else 5f
            drawLine(tickCompose, Offset(pCorner.x, pCorner.y - dist), Offset(pCorner.x + tW, pCorner.y - dist), if (isCm) 1.5f else 1f)
            if (isCm && mmIdx > 0 && dist < span - 10f) {
                drawLabel("${mmIdx / 10}", Offset(pCorner.x + 22f * s.coerceIn(1f, 1.8f), pCorner.y - dist + 3f), tickColorInt, (12f * s).coerceIn(10f, 18f), isBold = true)
            }
            mmIdx++
        }

        // 6. Side Labels
        val sideFontSize = (12f * s.coerceIn(1f, 1.4f)).coerceIn(11f, 18f)
        drawLabel(String.format(Locale.US, "Base: %.1f cm", legCm), Offset(cx, pCorner.y + 24f * s.coerceIn(1f, 1.4f)), tickColorInt, sideFontSize, isBold = true)
        drawLabel(
            String.format(Locale.US, "Height: %.1f cm", legCm),
            Offset(pCorner.x - 14f * s.coerceIn(1f, 1.4f), cy),
            tickColorInt,
            sideFontSize,
            isBold = true,
            align = Paint.Align.RIGHT
        )
        val hypCm = legCm * 1.4142f
        rotate(45f, pivot = Offset(cx, cy)) {
            drawLabel(
                String.format(Locale.US, "Hyp: %.1f cm", hypCm),
                Offset(cx, cy - 24f * s.coerceIn(1f, 1.4f)),
                tickColorInt,
                sideFontSize,
                isBold = true
            )
        }

        // 7. Middle Controls (responsive sizing, no extra RESIZE text)
        val btnR = (20f * (span / 280f).coerceIn(0.75f, 1.2f)).coerceIn(15f, 22f)
        val iconColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

        // Middle Scale Handle [⤡] (positioned comfortably inside the set square interior)
        val scalePos = Offset(cx + half * 0.10f, cy + half * 0.55f)
        drawButtonCircle(scalePos, btnR, borderColor, isDark)
        drawScaleIcon(scalePos, btnR * 0.5f, iconColor)

        // Close [X] Button
        val closePos = Offset(cx - half * 0.50f, cy - half * 0.30f)
        drawStandardCloseButton(closePos, btnR, isDark)

        // Floating Rotate [⟳] Button outside the figure above top apex
        val rotPos = Offset(pTop.x, pTop.y - 28f)
        drawLine(borderColor, pTop, rotPos, strokeWidth = 1.8f)
        drawButtonCircle(rotPos, btnR, borderColor, isDark)
        drawRotateIcon(rotPos, btnR * 0.5f, iconColor)
    }
}

/** Semicircular Protractor (0° - 180°) with Dual Angle Measurement (Left & Right) */
private fun DrawScope.drawProtractorTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val span = tool.span
        val cx = tool.center.x
        val cy = tool.center.y
        val shelfH = (span * 0.22f).coerceIn(48f, 68f)

        val bodyColor = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
        val borderColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val tickColorInt = if (isDark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
        val tickCompose = Color(tickColorInt)
        val faintGuide = if (isDark) Color(0x22FFFFFF) else Color(0x22000000)

        // 1. Semicircular Upper Body + Rectangular Shelf
        val bodyPath = Path().apply {
            val left = cx - span - 10f
            val right = cx + span + 10f
            val bottom = cy + shelfH
            moveTo(left, cy)
            lineTo(left, bottom - 12f)
            arcTo(Rect(left, bottom - 24f, left + 24f, bottom), 180f, -90f, false)
            lineTo(right - 12f, bottom)
            arcTo(Rect(right - 24f, bottom - 24f, right, bottom), 90f, -90f, false)
            lineTo(right, cy)
            lineTo(cx + span, cy)
            arcTo(Rect(cx - span, cy - span, cx + span, cy + span), 0f, -180f, false)
            close()
        }
        drawPath(bodyPath, bodyColor)
        drawPath(bodyPath, borderColor, style = DrawStroke(1.8f))

        // 2. Concentric Arcs - neatly spaced lanes scaling with protractor size
        val s = (span / defaultMathSpan(MathToolKind.PROTRACTOR)).coerceIn(0.85f, 3.5f)
        val outerLaneH = 28f * s.coerceIn(1f, 1.8f)
        val innerLaneH = 28f * s.coerceIn(1f, 1.8f)
        val midR = span - outerLaneH
        val inR = span - outerLaneH - innerLaneH
        val hubR = span * 0.20f

        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - span, cy - span), size = Size(span * 2f, span * 2f), style = DrawStroke(1.5f))
        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - midR, cy - midR), size = Size(midR * 2f, midR * 2f), style = DrawStroke(1.1f))
        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - inR, cy - inR), size = Size(inR * 2f, inR * 2f), style = DrawStroke(1.1f))
        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - hubR, cy - hubR), size = Size(hubR * 2f, hubR * 2f), style = DrawStroke(1.1f))

        // 3. Baseline ruler edge & Origin crosshair
        drawLine(borderColor, Offset(cx - span, cy), Offset(cx + span, cy), strokeWidth = 1.8f)
        drawLine(borderColor, Offset(cx, cy - 14f), Offset(cx, cy + 14f), strokeWidth = 1.4f)
        drawCircle(borderColor, 4.5f, Offset(cx, cy))
        drawCircle(if (isDark) Color(0xFF1E293B) else Color.White, 2.5f, Offset(cx, cy))

        // 4. Degree ticks & Non-overlapping dual numbers
        for (deg in 0..180) {
            val is10 = deg % 10 == 0
            val is5 = deg % 5 == 0
            if (!is5 && span < 200f) continue
            if (!is5 && deg % 2 != 0) continue

            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val cosA = cos(rad)
            val sinA = sin(rad)

            val tickLen = if (is10) (10f * s.coerceIn(1f, 1.6f)) else if (is5) (6f * s.coerceIn(1f, 1.6f)) else (4f * s.coerceIn(1f, 1.6f))
            val pStart = Offset(cx + (span - tickLen) * cosA, cy - (span - tickLen) * sinA)
            val pEnd = Offset(cx + span * cosA, cy - span * sinA)
            drawLine(tickCompose, pStart, pEnd, strokeWidth = if (is10) 1.5f else 1f)

            if (is10) {
                val gStart = Offset(cx + hubR * cosA, cy - hubR * sinA)
                val gEnd = Offset(cx + (inR - 4f) * cosA, cy - (inR - 4f) * sinA)
                drawLine(faintGuide, gStart, gEnd, strokeWidth = 1f)

                val degFontSize = (11f * s).coerceIn(10f, 20f)
                // Outer scale (0° at right, 180° at left) - sits cleanly centered in outer lane [midR, span]
                val rOuter = span - outerLaneH * 0.50f
                drawLabel("$deg", Offset(cx + rOuter * cosA, cy - rOuter * sinA + 3f), tickColorInt, degFontSize, isBold = true)

                // Inner scale (180° at right, 0° at left) - sits cleanly centered in inner lane [inR, midR]
                val rInner = span - outerLaneH - innerLaneH * 0.50f
                drawLabel("${180 - deg}", Offset(cx + rInner * cosA, cy - rInner * sinA + 3f), tickColorInt, degFontSize, isBold = true)
            }
        }

        // 5. Interactive Pointer Arm & Dual Angle Wedges
        val pDeg = tool.pointerAngleDeg.coerceIn(0f, 180f)
        val pRad = Math.toRadians(pDeg.toDouble()).toFloat()
        val pDir = Offset(cos(pRad), -sin(pRad))
        val armTip = Offset(cx, cy) + pDir * span

        // 5a. Translucent wedge from Right baseline (0° to pDeg)
        if (pDeg > 0.5f) {
            val wedgePath = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + span, cy)
                arcTo(Rect(cx - span, cy - span, cx + span, cy + span), 0f, -pDeg, false)
                close()
            }
            drawPath(wedgePath, if (isDark) Color(0x22FFFFFF) else Color(0x18000000))
        }

        // 5b. Translucent wedge from Left baseline (180° to pDeg)
        if (pDeg < 179.5f) {
            val leftWedgePath = Path().apply {
                moveTo(cx, cy)
                lineTo(cx - span, cy)
                arcTo(Rect(cx - span, cy - span, cx + span, cy + span), 180f, 180f - pDeg, false)
                close()
            }
            drawPath(leftWedgePath, if (isDark) Color(0x15FFFFFF) else Color(0x0E000000))
        }

        // 5c. Arc rim from Right 0° up to pDeg
        if (pDeg > 0.5f) {
            drawArc(
                color = borderColor,
                startAngle = 0f,
                sweepAngle = -pDeg,
                useCenter = false,
                topLeft = Offset(cx - span, cy - span),
                size = Size(span * 2f, span * 2f),
                style = DrawStroke(3.2f)
            )
        }

        // 5c2. Arc rim from Left 180° down to pDeg
        if (pDeg < 179.5f) {
            drawArc(
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                startAngle = 180f,
                sweepAngle = 180f - pDeg,
                useCenter = false,
                topLeft = Offset(cx - span, cy - span),
                size = Size(span * 2f, span * 2f),
                style = DrawStroke(3.2f)
            )
        }

        // 5d. Pointer Line from origin to current angle arm tip
        drawLine(borderColor, Offset(cx, cy), armTip, strokeWidth = 2.4f)

        // 5e. Handle Tabs - placed slightly outside rim so degree markings remain completely visible
        val tabW = (28f * (span / 260f).coerceIn(0.85f, 1.4f)).coerceIn(24f, 40f)
        val tabH = (40f * (span / 260f).coerceIn(0.85f, 1.4f)).coerceIn(34f, 52f)
        val tabRadius = CornerRadius(6f, 6f)
        val tabColor = if (isDark) Color(0xEE334155) else Color(0xEE0F172A)

        val rightTip = Offset(cx + span + tabW * 0.35f, cy)
        drawRoundRect(tabColor, topLeft = rightTip - Offset(tabW / 2f, tabH / 2f), size = Size(tabW, tabH), cornerRadius = tabRadius)
        val leftTip = Offset(cx - span - tabW * 0.35f, cy)
        drawRoundRect(tabColor, topLeft = leftTip - Offset(tabW / 2f, tabH / 2f), size = Size(tabW, tabH), cornerRadius = tabRadius)
        val armTabCenter = Offset(cx, cy) + pDir * (span + tabH * 0.35f)
        drawRoundRect(tabColor, topLeft = armTabCenter - Offset(tabW / 2f, tabH / 2f), size = Size(tabW, tabH), cornerRadius = tabRadius)

        // 5f. Dual Angle Readout Badge in center - clear non-overlapping lines
        val badgePos = Offset(cx, cy - span * 0.38f)
        val pillW = 96f * s.coerceIn(1f, 2.2f)
        val pillH = 50f * s.coerceIn(1f, 2.2f)
        drawRoundRect(
            if (isDark) Color(0xEE1E293B) else Color(0xFAFFFFFF),
            topLeft = badgePos - Offset(pillW / 2f, pillH / 2f),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
        )
        drawRoundRect(
            borderColor,
            topLeft = badgePos - Offset(pillW / 2f, pillH / 2f),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
            style = DrawStroke(1.5f * s.coerceIn(1f, 2f))
        )
        val badgeFontSize = (13f * s).coerceIn(11f, 32f)
        drawLabel(String.format(Locale.US, "R: %.1f°", pDeg), Offset(badgePos.x, badgePos.y - 10f * s.coerceIn(1f, 2f)), tickColorInt, badgeFontSize, isBold = true)
        drawLabel(String.format(Locale.US, "L: %.1f°", 180f - pDeg), Offset(badgePos.x, badgePos.y + 10f * s.coerceIn(1f, 2f)), if (isDark) 0xFFCBD5E1.toInt() else 0xFF475569.toInt(), badgeFontSize, isBold = true)

        // 6. Bottom Shelf Buttons
        val btnY = cy + shelfH * 0.5f
        val btnR = (shelfH * 0.34f).coerceIn(16f, 22f)
        val iconColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

        // Floating Outer Rotate Handle above top apex outside the arc
        val topRotatePos = Offset(cx, cy - span - 28f)
        drawLine(borderColor, Offset(cx, cy - span), topRotatePos, strokeWidth = 1.8f)
        drawButtonCircle(topRotatePos, btnR, borderColor, isDark)
        drawRotateIcon(topRotatePos, btnR * 0.5f, iconColor)

        // 1. [X] Standard Close
        val closePos = Offset(cx - span * 0.75f, btnY)
        drawStandardCloseButton(closePos, btnR, isDark)

        // 2. Mode 1: Left Angle Button (L.ANG)
        val m1Pos = Offset(cx - span * 0.45f, btnY)
        drawButtonCircle(m1Pos, btnR, borderColor, isDark)
        drawAngleModeIcon(m1Pos, btnR, mode = 1, isDark = isDark)

        // 3. Mode Arc Left: Left Arc Button (L.ARC)
        val mArcLeftPos = Offset(cx - span * 0.15f, btnY)
        drawButtonCircle(mArcLeftPos, btnR, borderColor, isDark)
        drawAngleModeIcon(mArcLeftPos, btnR, mode = 3, isDark = isDark)

        // 4. Mode Arc Right: Right Arc Button (R.ARC)
        val mArcRightPos = Offset(cx + span * 0.15f, btnY)
        drawButtonCircle(mArcRightPos, btnR, borderColor, isDark)
        drawAngleModeIcon(mArcRightPos, btnR, mode = 0, isDark = isDark)

        // 5. Mode 2: Right Angle Button (R.ANG)
        val m2Pos = Offset(cx + span * 0.45f, btnY)
        drawButtonCircle(m2Pos, btnR, borderColor, isDark)
        drawAngleModeIcon(m2Pos, btnR, mode = 2, isDark = isDark)

        // 6. [⤡] Scale Handle
        val scalePos = Offset(cx + span * 0.75f, btnY)
        drawButtonCircle(scalePos, btnR, borderColor, isDark)
        drawScaleIcon(scalePos, btnR * 0.5f, iconColor)
    }
}

/** Equilateral Triangle tool (All angles 60°) with 3-Side Measurements & Side Pencils */
private fun DrawScope.drawTriangleTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val span = tool.span
        val cx = tool.center.x
        val cy = tool.center.y
        val side = span * 1.8f
        val h = side * 0.8660254f

        val vTop = Offset(cx, cy - h * 0.6667f)
        val vLeft = Offset(cx - side / 2f, cy + h * 0.3333f)
        val vRight = Offset(cx + side / 2f, cy + h * 0.3333f)

        val bodyColor = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
        val borderColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val tickColorInt = if (isDark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
        val tickCompose = Color(tickColorInt)

        // 1. Equilateral Triangle Body
        val triPath = Path().apply {
            moveTo(vTop.x, vTop.y)
            lineTo(vLeft.x, vLeft.y)
            lineTo(vRight.x, vRight.y)
            close()
        }
        drawPath(triPath, bodyColor)
        drawPath(triPath, borderColor, style = DrawStroke(2f))

        // 2. Centimeter measurements and ruler markings along Base (vLeft -> vRight) - ALL numbers 1..maxCm
        val s = (tool.span / defaultMathSpan(tool.kind)).coerceIn(0.85f, 3.5f)
        val sideCm = side / pxPerCm.coerceAtLeast(1f)
        val labelStr = String.format(Locale.US, "%.1f cm", sideCm)

        val mm = (pxPerCm / 10f).coerceAtLeast(2.5f)
        var mmIdx = 0
        while (mmIdx * mm <= side - 4f) {
            val dist = mmIdx * mm
            val isCm = mmIdx % 10 == 0
            val is5 = mmIdx % 5 == 0
            val tH = if (isCm) 14f else if (is5) 9f else 5f
            val xPos = vLeft.x + dist
            drawLine(tickCompose, Offset(xPos, vLeft.y), Offset(xPos, vLeft.y - tH), if (isCm) 1.5f else 1f)
            if (isCm && mmIdx > 0 && dist < side - 10f) {
                drawLabel("${mmIdx / 10}", Offset(xPos, vLeft.y - 20f * s.coerceIn(1f, 1.8f)), tickColorInt, (12f * s).coerceIn(9f, 18f), isBold = true)
            }
            mmIdx++
        }

        // Side Lengths in cm clearly shown for all 3 sides:
        val sideFontSize = (12f * s.coerceIn(1f, 1.4f)).coerceIn(11f, 18f)
        drawLabel("Length: $labelStr", Offset(cx, vLeft.y + 26f * s.coerceIn(1f, 1.4f)), tickColorInt, sideFontSize, isBold = true)

        // Side B (Left slant: vTop -> vLeft) rotated parallel to edge outside
        val midLeft = Offset((vTop.x + vLeft.x) / 2f, (vTop.y + vLeft.y) / 2f)
        rotate(-60f, pivot = midLeft) {
            drawLabel("Side: $labelStr", midLeft - Offset(0f, 18f * s.coerceIn(1f, 1.4f)), tickColorInt, sideFontSize, isBold = true)
        }

        // Side C (Right slant: vTop -> vRight) rotated parallel to edge outside
        val midRight = Offset((vTop.x + vRight.x) / 2f, (vTop.y + vRight.y) / 2f)
        rotate(60f, pivot = midRight) {
            drawLabel("Side: $labelStr", midRight - Offset(0f, 18f * s.coerceIn(1f, 1.4f)), tickColorInt, sideFontSize, isBold = true)
        }

        // Interior angle corner arcs (60° at each vertex), strictly inside the triangle boundary (no angle text)
        val triArcR = 20f * s.coerceIn(1f, 1.3f)
        drawArc(borderColor, 60f, 60f, false, topLeft = Offset(vTop.x - triArcR, vTop.y - triArcR), size = Size(triArcR * 2f, triArcR * 2f), style = DrawStroke(1.4f))
        drawArc(borderColor, 300f, 60f, false, topLeft = Offset(vLeft.x - triArcR, vLeft.y - triArcR), size = Size(triArcR * 2f, triArcR * 2f), style = DrawStroke(1.4f))
        drawArc(borderColor, 180f, 60f, false, topLeft = Offset(vRight.x - triArcR, vRight.y - triArcR), size = Size(triArcR * 2f, triArcR * 2f), style = DrawStroke(1.4f))

        // 3. Middle Controls:
        val btnR = (20f * (span / 220f).coerceIn(0.75f, 1.2f)).coerceIn(15f, 22f)
        val iconColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

        // Middle Scale Handle [⤡] (positioned towards lower-right for ample runway)
        val scalePos = Offset(cx + side * 0.22f, cy + h * 0.08f)
        drawButtonCircle(scalePos, btnR, borderColor, isDark)
        drawScaleIcon(scalePos, btnR * 0.5f, iconColor)

        // Floating Rotate [⟳] Button above top apex outside the figure
        val rotPos = Offset(vTop.x, vTop.y - 28f)
        drawLine(borderColor, vTop, rotPos, strokeWidth = 1.8f)
        drawButtonCircle(rotPos, btnR, borderColor, isDark)
        drawRotateIcon(rotPos, btnR * 0.5f, iconColor)

        // Close [X] Button
        val closePos = Offset(cx, cy - h * 0.25f)
        drawStandardCloseButton(closePos, btnR, isDark)
    }
}

fun DrawScope.drawStandardCloseButton(center: Offset, radius: Float, isDark: Boolean = false) {
    val r = radius.coerceIn(12f, 18f)
    drawCircle(Color(0xFFEF4444).copy(alpha = 0.14f), r, center)
    drawCircle(Color(0xFFEF4444).copy(alpha = 0.5f), r, center, style = DrawStroke(1.2f))
    val xRadius = r * 0.38f
    drawLine(Color(0xFFEF4444), center - Offset(xRadius, xRadius), center + Offset(xRadius, xRadius), strokeWidth = 1.8f, cap = StrokeCap.Round)
    drawLine(Color(0xFFEF4444), center - Offset(-xRadius, xRadius), center + Offset(xRadius, -xRadius), strokeWidth = 1.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawAngleModeIcon(center: Offset, radius: Float, mode: Int, isDark: Boolean = false) {
    val iconColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val textInt = if (isDark) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
    val r = radius * 0.5f
    val topCenter = center - Offset(0f, r * 0.32f)
    val labelPos = center + Offset(0f, r * 0.70f)
    when (mode) {
        1 -> { // Left Angle Mode (L.ANG)
            val baseStart = topCenter + Offset(-r * 0.7f, r * 0.35f)
            val baseEnd = topCenter + Offset(r * 0.7f, r * 0.35f)
            val armEnd = baseEnd + Offset(-r * 0.9f, -r * 0.85f)
            drawLine(iconColor, baseStart, baseEnd, strokeWidth = 1.8f)
            drawLine(iconColor, baseEnd, armEnd, strokeWidth = 1.8f)
            drawArc(accentColor, 180f, 45f, false, topLeft = Offset(baseEnd.x - r * 0.45f, baseEnd.y - r * 0.45f), size = Size(r * 0.9f, r * 0.9f), style = DrawStroke(1.2f))
            drawLabel("L.ANG", labelPos, textInt, 7.5f, isBold = true)
        }
        3 -> { // Left Arc Mode (L.ARC)
            val rect = Rect(topCenter.x - r * 0.65f, topCenter.y - r * 0.65f, topCenter.x + r * 0.65f, topCenter.y + r * 0.65f)
            drawArc(iconColor, 180f, 65f, false, topLeft = rect.topLeft, size = rect.size, style = DrawStroke(2f))
            drawLabel("L.ARC", labelPos, textInt, 7.5f, isBold = true)
        }
        0 -> { // Right Arc Mode (R.ARC)
            val rect = Rect(topCenter.x - r * 0.65f, topCenter.y - r * 0.65f, topCenter.x + r * 0.65f, topCenter.y + r * 0.65f)
            drawArc(iconColor, 295f, 65f, false, topLeft = rect.topLeft, size = rect.size, style = DrawStroke(2f))
            drawLabel("R.ARC", labelPos, textInt, 7.5f, isBold = true)
        }
        2 -> { // Right Angle Mode (R.ANG)
            val baseStart = topCenter + Offset(-r * 0.7f, r * 0.35f)
            val baseEnd = topCenter + Offset(r * 0.7f, r * 0.35f)
            val armEnd = baseStart + Offset(r * 0.9f, -r * 0.85f)
            drawLine(iconColor, baseStart, baseEnd, strokeWidth = 1.8f)
            drawLine(iconColor, baseStart, armEnd, strokeWidth = 1.8f)
            drawArc(accentColor, 315f, 45f, false, topLeft = Offset(baseStart.x - r * 0.45f, baseStart.y - r * 0.45f), size = Size(r * 0.9f, r * 0.9f), style = DrawStroke(1.2f))
            drawLabel("R.ANG", labelPos, textInt, 7.5f, isBold = true)
        }
    }
}

private fun DrawScope.drawButtonCircle(center: Offset, radius: Float, borderColor: Color, isDark: Boolean = false) {
    drawCircle(if (isDark) Color(0xEE1E293B) else Color.White, radius, center)
    drawCircle(borderColor, radius, center, style = DrawStroke(1.8f))
}

private fun DrawScope.drawPencilIcon(center: Offset, size: Float, iconColor: Color = Color(0xFF0F172A)) {
    val path = Path().apply {
        val p1 = center + Offset(-size * 0.7f, size * 0.7f)
        val p2 = center + Offset(-size * 0.35f, size * 0.8f)
        val p3 = center + Offset(size * 0.65f, -size * 0.2f)
        val p4 = center + Offset(size * 0.2f, -size * 0.65f)
        val p5 = center + Offset(-size * 0.8f, size * 0.35f)
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        lineTo(p5.x, p5.y)
        close()
    }
    drawPath(path, iconColor)
}

private fun DrawScope.drawRotateIcon(center: Offset, size: Float, iconColor: Color = Color(0xFF0F172A)) {
    val rect = Rect(center.x - size, center.y - size, center.x + size, center.y + size)
    drawArc(
        color = iconColor,
        startAngle = 30f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = rect.topLeft,
        size = rect.size,
        style = DrawStroke(2.2f)
    )
    val arrowRad = Math.toRadians((30.0 + 270.0)).toFloat()
    val tip = Offset(center.x + cos(arrowRad) * size, center.y + sin(arrowRad) * size)
    val head1 = tip + Offset(-5f, -6f)
    val head2 = tip + Offset(5f, -6f)
    val headPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(head1.x, head1.y)
        lineTo(head2.x, head2.y)
        close()
    }
    drawPath(headPath, iconColor)
}

private fun DrawScope.drawScaleIcon(center: Offset, size: Float, iconColor: Color = Color(0xFF0F172A)) {
    val p1 = center + Offset(-size * 0.7f, size * 0.7f)
    val p2 = center + Offset(size * 0.7f, -size * 0.7f)
    drawLine(iconColor, p1, p2, strokeWidth = 2.2f)
    val a1 = p2 + Offset(-6f, 0f)
    val a2 = p2 + Offset(0f, 6f)
    drawLine(iconColor, p2, a1, strokeWidth = 2.2f)
    drawLine(iconColor, p2, a2, strokeWidth = 2.2f)

    val b1 = p1 + Offset(6f, 0f)
    val b2 = p1 + Offset(0f, -6f)
    drawLine(iconColor, p1, b1, strokeWidth = 2.2f)
    drawLine(iconColor, p1, b2, strokeWidth = 2.2f)
}

private fun DrawScope.drawSpreadIcon(center: Offset, size: Float, iconColor: Color = Color(0xFF0F172A)) {
    val d = size * 0.65f
    val leftStart = center - Offset(d, 0f)
    val leftEnd = center - Offset(2f, 0f)
    drawLine(iconColor, leftStart, leftEnd, strokeWidth = 2.2f)
    drawLine(iconColor, leftEnd, leftEnd + Offset(-5f, -4f), strokeWidth = 2.2f)
    drawLine(iconColor, leftEnd, leftEnd + Offset(-5f, 4f), strokeWidth = 2.2f)

    val rightStart = center + Offset(d, 0f)
    val rightEnd = center + Offset(2f, 0f)
    drawLine(iconColor, rightStart, rightEnd, strokeWidth = 2.2f)
    drawLine(iconColor, rightEnd, rightEnd + Offset(5f, -4f), strokeWidth = 2.2f)
    drawLine(iconColor, rightEnd, rightEnd + Offset(5f, 4f), strokeWidth = 2.2f)
}

private fun DrawScope.drawLabel(
    text: String,
    at: Offset,
    color: Int,
    textSize: Float,
    isBold: Boolean = true,
    align: Paint.Align = Paint.Align.CENTER
) {
    val displayScale = 1.45f
    val paint = Paint().apply {
        this.color = color
        this.textSize = textSize * displayScale
        this.isAntiAlias = true
        this.isFakeBoldText = isBold
        this.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.textAlign = align
    }
    drawContext.canvas.nativeCanvas.drawText(
        text,
        at.x,
        at.y,
        paint
    )
}

fun DrawScope.drawMathReadout(text: String, at: Offset) {
    val textPaint = Paint().apply {
        textSize = 22f
        isAntiAlias = true
        isFakeBoldText = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val measuredW = textPaint.measureText(text)
    val pillW = (measuredW + 36f).coerceAtLeast(84f)
    val pillH = 38f
    val left = at.x - pillW / 2f
    val top = at.y - pillH / 2f

    drawRoundRect(
        color = Color(0xF20F172A),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(19f, 19f)
    )
    drawRoundRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(19f, 19f),
        style = DrawStroke(1.6f)
    )
    drawLabel(text, Offset(at.x, at.y + 5.5f), 0xFFFFFFFF.toInt(), 13.5f)
}
