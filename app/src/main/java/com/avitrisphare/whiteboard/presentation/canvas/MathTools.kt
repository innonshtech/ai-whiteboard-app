package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Paint
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
    SIDE_3_PENCIL
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
    val angleMode: Int = 0
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
private const val RULER_EDGE_INSET = 18f
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

    // Close button (top right inside ruler)
    val closeBtnCenter = Offset(half - 24f, -halfT + 22f)
    if (near(local, closeBtnCenter, 22f)) return MathToolZone.CLOSE

    // If touching near the top edge or bottom edge (where ticks and numbers are),
    // always return NONE so straightedge line drawing triggers seamlessly!
    val distToTopEdge = abs(local.y - (-halfT))
    val distToBottomEdge = abs(local.y - halfT)
    if ((distToTopEdge <= RULER_EDGE_SLOP || distToBottomEdge <= RULER_EDGE_SLOP) &&
        abs(local.x) <= half + RULER_EDGE_SLOP &&
        abs(local.y) >= 16f) {
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
        MathToolKind.PROTRACTOR -> {
            val distBaseline = if (abs(local.x) <= tool.span + 10f && local.y in -15f..15f) abs(local.y) else Float.MAX_VALUE
            val r = hypot(local.x, local.y)
            val distRim = if (local.y <= 12f) abs(r - tool.span) else Float.MAX_VALUE
            return minOf(distBaseline, distRim)
        }
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

fun rulerEdgeForPoint(tool: PlacedMathTool, point: Offset): Float {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    return if (local.y < 0f) -1f else 1f
}

fun snapToRulerEdge(tool: PlacedMathTool, screenPoint: Offset, edgeSign: Float): Offset {
    val dir = direction(tool.rotationDeg)
    val perp = perpendicular(tool.rotationDeg)
    val local = toLocal(screenPoint, tool.center, tool.rotationDeg)
    val half = tool.span / 2f
    val halfT = RULER_THICKNESS / 2f
    val clampedX = local.x.coerceIn(-half, half)
    return tool.center + dir * clampedX + perp * (edgeSign * halfT)
}

fun rulerForStraightedge(tools: List<PlacedMathTool>, point: Offset): PlacedMathTool? {
    return tools.lastOrNull { tool ->
        (tool.kind == MathToolKind.RULER ||
            tool.kind == MathToolKind.PROTRACTOR ||
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

    // 4. Spread handle (➔ ⬅) on right leg - HIGH PRIORITY
    val spreadBtn = compassSpreadBtn(tool)
    if (near(point, spreadBtn, touchR * 1.3f)) return MathToolZone.SPREAD

    // 5. Pencil tip handle (🖊) at pencil tip - HIGH PRIORITY
    val pencil = compassPencil(tool)
    if (near(point, pencil, touchR * 1.4f)) return MathToolZone.PENCIL

    // 6. Right leg drag -> Treat as SPREAD to open/close angle easily!
    if (distanceToSegment(point, hinge, pencil) <= 30f * scale) return MathToolZone.SPREAD

    // 7. Top grip handle or hinge block -> Smooth whole-compass MOVE
    if (distanceToSegment(point, hinge, handleTop) <= 28f * scale) return MathToolZone.MOVE
    if (near(point, hinge, touchR * 1.2f)) return MathToolZone.MOVE

    // 8. Needle anchor point (left tip) -> MOVE
    if (near(point, tool.center, touchR)) return MathToolZone.MOVE

    // 9. Left leg -> MOVE
    if (distanceToSegment(point, hinge, tool.center) <= 24f * scale) return MathToolZone.MOVE

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
    val btnR = 24f
    val touchR = 32f

    // Corner 90°: (-half, half), Base 45°: (half, half), Top 45°: (-half, -half)
    // 1. Floating Outer Rotate handle [⟳] above top apex outside the figure
    val rotPos = Offset(-half, -half - 28f)
    if (near(local, rotPos, touchR * 1.2f)) return MathToolZone.ROTATE

    // 2. Close [X] inside near 90° corner
    val closePos = Offset(-half * 0.55f, -half * 0.45f)
    if (near(local, closePos, touchR)) return MathToolZone.CLOSE

    // 3. Scale handle [⤡] near hypotenuse bottom-right
    val scalePos = Offset(half * 0.65f, half * 0.65f)
    if (near(local, scalePos, touchR)) return MathToolZone.SCALE

    // 4. Side 1 Pencil (Horizontal Base line)
    val side1Pencil = Offset(0f, half - 18f)
    if (near(local, side1Pencil, touchR)) return MathToolZone.SIDE_1_PENCIL

    // 5. Side 2 Pencil (Vertical Height 90° line)
    val side2Pencil = Offset(-half + 18f, 0f)
    if (near(local, side2Pencil, touchR)) return MathToolZone.SIDE_2_PENCIL

    // 6. Side 3 Pencil (Hypotenuse line)
    val side3Pencil = Offset(18f, 18f)
    if (near(local, side3Pencil, touchR)) return MathToolZone.SIDE_3_PENCIL

    // 7. Center full triangle stamp
    val centerPencil = Offset(-half * 0.35f, half * 0.35f)
    if (near(local, centerPencil, touchR)) return MathToolZone.PENCIL

    // 8. Inside right triangle body -> MOVE
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
    val vTop = Offset(0f, -h * 0.6667f)
    val vLeft = Offset(-side / 2f, h * 0.3333f)
    val vRight = Offset(side / 2f, h * 0.3333f)
    val touchR = 30f

    // 1. Floating Outer Rotate handle [⟳] above top apex outside the figure
    val rotatePos = Offset(0f, -h * 0.6667f - 28f)
    if (near(local, rotatePos, touchR * 1.2f)) return MathToolZone.ROTATE

    // 2. Close button [X]
    val closePos = Offset(0f, -h * 0.35f)
    if (near(local, closePos, touchR)) return MathToolZone.CLOSE

    // 3. Scale handle [⤡]
    val scalePos = Offset(side * 0.18f, h * 0.10f)
    if (near(local, scalePos, touchR)) return MathToolZone.SCALE

    // 4. Side 1 Pencil (Bottom Base)
    val side1Pencil = Offset(0f, h * 0.3333f - 18f)
    if (near(local, side1Pencil, touchR)) return MathToolZone.SIDE_1_PENCIL

    // 5. Side 2 Pencil (Left Slant)
    val side2Pencil = Offset(-side * 0.25f + 16f, -h * 0.1667f)
    if (near(local, side2Pencil, touchR)) return MathToolZone.SIDE_2_PENCIL

    // 6. Side 3 Pencil (Right Slant)
    val side3Pencil = Offset(side * 0.25f - 16f, -h * 0.1667f)
    if (near(local, side3Pencil, touchR)) return MathToolZone.SIDE_3_PENCIL

    // 7. Center full triangle stamp
    val centerPencil = Offset(0f, 0f)
    if (near(local, centerPencil, touchR)) return MathToolZone.PENCIL

    // 8. Inside equilateral triangle body -> MOVE
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

    // Magnetic angle snapping: snap to nearest 15° if within 2.2°
    val snapInterval = 15f
    val nearestSnap = (norm / snapInterval).roundToInt() * snapInterval
    if (abs(norm - nearestSnap) <= 2.2f) {
        norm = (nearestSnap % 360f + 360f) % 360f
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
    // Dynamic extension: min 180f (~4.5 cm), max 3600f (~90 cm)
    val newSpan = dist.coerceIn(180f, 3600f)

    val angle = atan2(dy, dx) * RAD_TO_DEG
    val rawRotation = if (draggingPositiveEnd) angle else angle + 180f
    var norm = (rawRotation % 360f + 360f) % 360f

    // Magnetic angle snapping: snap to nearest 15° if within 2.5°
    val snapInterval = 15f
    val nearestSnap = (norm / snapInterval).roundToInt() * snapInterval
    if (abs(norm - nearestSnap) <= 2.5f) {
        norm = (nearestSnap % 360f + 360f) % 360f
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

fun measurementLabel(text: String, at: Offset, color: Int): Stroke {
    val safeColor = if (color != 0) color else 0xFF1E293B.toInt()
    return Stroke(
        points = listOf(at),
        colorValue = safeColor,
        strokeWidth = 18f,
        strokeType = StrokeType.TEXT,
        text = text
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
        val length = tool.span
        val thickness = RULER_THICKNESS
        val left = tool.center.x - length / 2f
        val top = tool.center.y - thickness / 2f
        val body = if (isDark) Color(0xF21E293B) else Color(0xF4FFFFFF)
        val border = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
        val tick = if (isDark) 0xFFCBD5E1.toInt() else 0xFF1E293B.toInt()
        val tickSubtle = if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
        val handle = Color(0xFF6366F1)

        // Glassmorphic body & crisp border
        drawRoundRect(body, Offset(left, top), Size(length, thickness), CornerRadius(12f, 12f))
        drawRoundRect(border, Offset(left, top), Size(length, thickness), CornerRadius(12f, 12f), style = DrawStroke(1.5f))

        // Center grip pill indicator (shows drag-to-move zone)
        val gripY = tool.center.y
        for (gx in listOf(-12f, 0f, 12f)) {
            drawRoundRect(
                color = if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0),
                topLeft = Offset(tool.center.x + gx - 2f, gripY - 14f),
                size = Size(4f, 28f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Ticks and centimeter numbers along both edges
        val mm = (pxPerCm / 10f).coerceAtLeast(1.5f)
        var index = 0
        var x = left + 14f
        val usableRight = left + length - 34f
        while (x <= usableRight) {
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
                drawLabel("$cmVal", Offset(x, top + thickness / 2f + 4.5f), tick, 11.5f)
            }
            index++
            x += mm
        }

        // Length readout label above ruler
        val rulerTitle = "Ruler  •  ${formatCentimetres(length, pxPerCm)}"
        drawLabel(rulerTitle, Offset(tool.center.x, top - 18f), tick, 13f)

        if (segmentLabel != null) {
            drawLabel(segmentLabel, Offset(tool.center.x, top + thickness + 22f), 0xFF4F46E5.toInt(), 14f)
        }

        // Close button (×) at top-right inside ruler
        val closeCenter = Offset(left + length - 24f, top + 22f)
        drawCircle(Color(0xFFEF4444).copy(alpha = 0.14f), 12f, closeCenter)
        drawCircle(Color(0xFFEF4444).copy(alpha = 0.5f), 12f, closeCenter, style = DrawStroke(1.2f))
        val xRadius = 4f
        drawLine(Color(0xFFEF4444), closeCenter - Offset(xRadius, xRadius), closeCenter + Offset(xRadius, xRadius), 1.8f)
        drawLine(Color(0xFFEF4444), closeCenter - Offset(-xRadius, xRadius), closeCenter + Offset(-xRadius, xRadius), 1.8f)

        // Rotation & extension handles at both ends with intuitive extend/rotate styling
        listOf(Offset(left, tool.center.y), Offset(left + length, tool.center.y)).forEach { handleCenter ->
            drawCircle(Color.White, HANDLE_RADIUS * 0.85f, handleCenter)
            drawCircle(handle, HANDLE_RADIUS * 0.85f, handleCenter, style = DrawStroke(2.5f))
            drawCircle(handle, 4f, handleCenter)
            // Horizontal extension arrow indicator
            drawLine(handle, handleCenter - Offset(7f, 0f), handleCenter + Offset(7f, 0f), 1.8f)
        }
    }
}

private fun DrawScope.drawCompassTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    val pencil = compassPencil(tool)
    val hinge = compassHinge(tool)
    val borderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val legColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val jointColor = if (isDark) Color(0xFF0369A1) else Color(0xFF38BDF8)
    val scaleFactor = tool.toolScale.coerceIn(0.7f, 2.2f)
    val btnR = 24f * scaleFactor

    // 0. Soft dashed guide circle around needle point
    drawCircle(
        color = Color(0x330284C7),
        radius = tool.span,
        center = tool.center,
        style = DrawStroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
    )

    // 1. Compass Legs
    val legW = 8f * scaleFactor
    // Left leg (Needle leg)
    drawLine(legColor, hinge, tool.center, strokeWidth = legW, cap = StrokeCap.Round)
    // Needle point at bottom-left
    drawLine(Color(0xFF0F172A), tool.center - (hinge - tool.center) * 0.08f, tool.center, strokeWidth = 3.5f * scaleFactor, cap = StrokeCap.Round)
    drawCircle(Color(0xFF0F172A), 4.5f * scaleFactor, tool.center)

    // Right leg (Realistic Pencil Leg)
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
        color = borderColor,
        topLeft = clampPos - Offset(clampW / 2f, clampH / 2f),
        size = Size(clampW, clampH),
        cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
    )
    drawCircle(Color(0xFF1E293B), 2.5f * scaleFactor, clampPos)

    // Realistic Pencil attached from clamp down to pencil tip
    val pencilShaftTop = clampPos - pNorm * (6f * scaleFactor)
    val pencilWoodTop = pencil - pNorm * (30f * scaleFactor)
    val pencilLeadTop = pencil - pNorm * (12f * scaleFactor)
    val shaftWidth = 10f * scaleFactor

    // Hexagonal yellow pencil body
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
    drawPath(pencilBodyPath, Color(0xFFF59E0B)) // Authentic yellow pencil shaft
    // Facet shade line along pencil body
    drawLine(Color(0xFFD97706), pencilShaftTop, pencilWoodTop, strokeWidth = 2f * scaleFactor)

    // Silver metal ferrule band
    val ferruleTop = pencilShaftTop - pNorm * (8f * scaleFactor)
    val ferrulePath = Path().apply {
        moveTo(ferruleTop.x - pPerp.x * shaftWidth / 2f, ferruleTop.y - pPerp.y * shaftWidth / 2f)
        lineTo(ferruleTop.x + pPerp.x * shaftWidth / 2f, ferruleTop.y + pPerp.y * shaftWidth / 2f)
        lineTo(pRightTop.x, pRightTop.y)
        lineTo(pLeftTop.x, pLeftTop.y)
        close()
    }
    drawPath(ferrulePath, Color(0xFF94A3B8))

    // Sharpened wooden cone (cedar tone)
    val woodConePath = Path().apply {
        moveTo(pLeftWood.x, pLeftWood.y)
        lineTo(pRightWood.x, pRightWood.y)
        lineTo(pencilLeadTop.x, pencilLeadTop.y)
        close()
    }
    drawPath(woodConePath, Color(0xFFFDE68A))

    // Graphite lead tip
    val leadPath = Path().apply {
        val leadLeft = pencilWoodTop + (pencil - pencilWoodTop) * 0.62f - pPerp * (shaftWidth * 0.2f)
        val leadRight = pencilWoodTop + (pencil - pencilWoodTop) * 0.62f + pPerp * (shaftWidth * 0.2f)
        moveTo(leadLeft.x, leadLeft.y)
        lineTo(leadRight.x, leadRight.y)
        lineTo(pencil.x, pencil.y)
        close()
    }
    drawPath(leadPath, Color(0xFF1E293B))

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
        style = DrawStroke(2f)
    )

    // Top vertical grip handle
    val up = -perpendicular(tool.rotationDeg)
    val handleBase = hinge + up * (jointSize / 2f)
    val handleTop = handleBase + up * (28f * scaleFactor)
    drawLine(legColor, handleBase, handleTop, strokeWidth = 10f * scaleFactor, cap = StrokeCap.Round)
    drawLine(borderColor, handleBase, handleTop, strokeWidth = 10f * scaleFactor, cap = StrokeCap.Round)

    // 3. Distance Readout below the hinge (matching "7.07 cm" in the user photo)
    val readoutAt = hinge - up * 26f
    val distCm = (tool.span / pxPerCm.coerceAtLeast(1f))
    drawRoundRect(
        color = if (isDark) Color(0xD90F172A) else Color(0xE6FFFFFF),
        topLeft = Offset(readoutAt.x - 36f, readoutAt.y - 12f),
        size = Size(72f, 24f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(readoutAt.x - 36f, readoutAt.y - 12f),
        size = Size(72f, 24f),
        cornerRadius = CornerRadius(6f, 6f),
        style = DrawStroke(1.2f)
    )
    drawLabel(String.format(Locale.US, "%.2f cm", distCm), Offset(readoutAt.x, readoutAt.y + 4f), if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt(), 12f)

    // 4. Action Buttons (matching the reference image):
    // [⟳] Top rotate button floating outside above handle
    val topRotate = compassTopRotateBtn(tool)
    drawLine(borderColor, handleTop, topRotate, strokeWidth = 2f)
    drawButtonCircle(topRotate, btnR, borderColor)
    drawRotateIcon(topRotate, btnR * 0.5f)

    // [⤡] Scale handle on left side of hinge
    val scaleBtn = compassScaleBtn(tool)
    drawButtonCircle(scaleBtn, btnR, borderColor)
    drawScaleIcon(scaleBtn, btnR * 0.5f)

    // [X] Standard Close button (cross on white/black circle) on right side of hinge
    val closeBtn = compassCloseBtn(tool)
    drawStandardCloseButton(closeBtn, btnR, isDark)

    // [➔ ⬅] Spread adjustment handle on right leg
    val spreadBtn = compassSpreadBtn(tool)
    drawButtonCircle(spreadBtn, btnR, borderColor)
    drawSpreadIcon(spreadBtn, btnR * 0.5f)

    // [🖊] Pencil tip handle at bottom-right
    val pencilBtn = pencil
    drawButtonCircle(pencilBtn, btnR, borderColor)
    drawPencilIcon(pencilBtn, btnR * 0.5f)
}

private fun DrawScope.drawSetSquareTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val span = tool.span
        val half = span / 2f
        val cx = tool.center.x
        val cy = tool.center.y

        val bodyColor = if (isDark) Color(0x330284C7) else Color(0x2838BDF8)
        val borderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        val tickColorInt = if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
        val tickCompose = Color(tickColorInt)

        // 1. Standard 45°-90°-45° Right Triangle:
        // Corner (90°): (-half, half)
        // Base (45°): (half, half)
        // Top (45°): (-half, -half)
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
        drawPath(triPath, borderColor, style = DrawStroke(2.5f))

        // 2. 90° Right Angle Marker at corner
        val sqSize = 22f
        val rightAnglePath = Path().apply {
            moveTo(pCorner.x, pCorner.y - sqSize)
            lineTo(pCorner.x + sqSize, pCorner.y - sqSize)
            lineTo(pCorner.x + sqSize, pCorner.y)
        }
        drawPath(rightAnglePath, borderColor, style = DrawStroke(1.5f))
        drawCircle(borderColor, 2f, pCorner + Offset(sqSize / 2f, -sqSize / 2f))
        drawLabel("90°", pCorner + Offset(sqSize + 12f, -sqSize + 2f), tickColorInt, 10f)

        // 3. 45° Angle Markers at Base and Top
        drawArc(borderColor, 180f, 45f, false, topLeft = Offset(pBase.x - 36f, pBase.y - 36f), size = Size(72f, 72f), style = DrawStroke(1.2f))
        drawLabel("45°", pBase + Offset(-32f, -14f), tickColorInt, 9.5f)

        drawArc(borderColor, 45f, 45f, false, topLeft = Offset(pTop.x - 36f, pTop.y - 36f), size = Size(72f, 72f), style = DrawStroke(1.2f))
        drawLabel("45°", pTop + Offset(16f, 32f), tickColorInt, 9.5f)

        // 4. Centimeter markings along Base (pCorner -> pBase)
        val mm = (pxPerCm / 10f).coerceAtLeast(2.5f)
        var mmIdx = 0
        val legCm = span / pxPerCm.coerceAtLeast(1f)
        while (mmIdx * mm <= span - 4f) {
            val dist = mmIdx * mm
            val isCm = mmIdx % 10 == 0
            val is5 = mmIdx % 5 == 0
            val tH = if (isCm) 14f else if (is5) 9f else 5f
            drawLine(tickCompose, Offset(pCorner.x + dist, pCorner.y), Offset(pCorner.x + dist, pCorner.y - tH), if (isCm) 1.5f else 1f)
            if (isCm && mmIdx > 0 && dist < span - 16f) {
                drawLabel("${mmIdx / 10}", Offset(pCorner.x + dist, pCorner.y - 20f), tickColorInt, 9f)
            }
            mmIdx++
        }

        // 5. Centimeter markings along Vertical Height (pCorner -> pTop)
        mmIdx = 0
        while (mmIdx * mm <= span - 4f) {
            val dist = mmIdx * mm
            val isCm = mmIdx % 10 == 0
            val is5 = mmIdx % 5 == 0
            val tW = if (isCm) 14f else if (is5) 9f else 5f
            drawLine(tickCompose, Offset(pCorner.x, pCorner.y - dist), Offset(pCorner.x + tW, pCorner.y - dist), if (isCm) 1.5f else 1f)
            if (isCm && mmIdx > 0 && dist < span - 16f) {
                drawLabel("${mmIdx / 10}", Offset(pCorner.x + 22f, pCorner.y - dist + 3f), tickColorInt, 9f)
            }
            mmIdx++
        }

        // 6. Side Labels:
        drawLabel(String.format(Locale.US, "Base: %.1f cm", legCm), Offset(cx, pCorner.y + 14f), tickColorInt, 11f)
        drawLabel(String.format(Locale.US, "Height: %.1f cm", legCm), Offset(pCorner.x - 24f, cy), tickColorInt, 11f)
        val hypCm = legCm * 1.4142f
        drawLabel(String.format(Locale.US, "Hyp: %.1f cm", hypCm), Offset(cx + 18f, cy - 14f), tickColorInt, 11f)

        // 7. Control Buttons & Side Pencils:
        val btnR = 21f

        // Side 1 Pencil (Base Line)
        val s1Pos = Offset(cx, pCorner.y - 18f)
        drawButtonCircle(s1Pos, btnR, borderColor)
        drawPencilIcon(s1Pos, btnR * 0.5f)

        // Side 2 Pencil (Height Line)
        val s2Pos = Offset(pCorner.x + 18f, cy)
        drawButtonCircle(s2Pos, btnR, borderColor)
        drawPencilIcon(s2Pos, btnR * 0.5f)

        // Side 3 Pencil (Hypotenuse Line)
        val s3Pos = Offset(cx + 18f, cy + 18f)
        drawButtonCircle(s3Pos, btnR, borderColor)
        drawPencilIcon(s3Pos, btnR * 0.5f)

        // Center Full Triangle Stamp Badge
        val centerStamp = Offset(cx - half * 0.35f, cy + half * 0.35f)
        val badgeR = 27f
        drawButtonCircle(centerStamp, badgeR, borderColor)
        drawPencilIcon(centerStamp, badgeR * 0.52f)

        // Close [X] Button (standard black/white cross button)
        val closePos = Offset(cx - half * 0.55f, cy - half * 0.45f)
        drawStandardCloseButton(closePos, btnR, isDark)

        // Floating Rotate [⟳] Button outside the figure above top apex
        val rotPos = Offset(pTop.x, pTop.y - 28f)
        drawLine(borderColor, pTop, rotPos, strokeWidth = 2f)
        drawButtonCircle(rotPos, btnR, borderColor)
        drawRotateIcon(rotPos, btnR * 0.5f)

        // Scale [⤡] Button
        val scalePos = Offset(cx + half * 0.65f, cy + half * 0.65f)
        drawButtonCircle(scalePos, btnR, borderColor)
        drawScaleIcon(scalePos, btnR * 0.5f)
    }
}

/** Semicircular Protractor (0° - 180°) with Dual Angle Measurement (Left & Right) */
private fun DrawScope.drawProtractorTool(tool: PlacedMathTool, isDark: Boolean, pxPerCm: Float) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val span = tool.span
        val cx = tool.center.x
        val cy = tool.center.y
        val shelfH = (span * 0.22f).coerceIn(48f, 68f)

        val bodyColor = if (isDark) Color(0x330284C7) else Color(0x2838BDF8)
        val borderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        val tickColorInt = if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
        val tickCompose = Color(tickColorInt)
        val faintGuide = if (isDark) Color(0x3338BDF8) else Color(0x330284C7)

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
        drawPath(bodyPath, borderColor, style = DrawStroke(2f))

        // 2. Concentric Arcs
        val midR = span - 34f
        val inR = span * 0.46f
        val hubR = span * 0.18f

        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - span, cy - span), size = Size(span * 2f, span * 2f), style = DrawStroke(1.5f))
        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - midR, cy - midR), size = Size(midR * 2f, midR * 2f), style = DrawStroke(1.2f))
        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - inR, cy - inR), size = Size(inR * 2f, inR * 2f), style = DrawStroke(1.2f))
        drawArc(borderColor, 180f, 180f, false, topLeft = Offset(cx - hubR, cy - hubR), size = Size(hubR * 2f, hubR * 2f), style = DrawStroke(1.2f))

        // 3. Baseline ruler edge & Origin crosshair
        drawLine(borderColor, Offset(cx - span, cy), Offset(cx + span, cy), strokeWidth = 2f)
        drawLine(borderColor, Offset(cx, cy - 14f), Offset(cx, cy + 14f), strokeWidth = 1.5f)
        drawCircle(borderColor, 4.5f, Offset(cx, cy))
        drawCircle(Color(0xFF0F172A), 2.5f, Offset(cx, cy))

        // 4. Degree ticks & Dual Numbers (0 to 180 CCW and 180 to 0 CW)
        for (deg in 0..180) {
            val is10 = deg % 10 == 0
            val is5 = deg % 5 == 0
            if (!is5 && span < 200f) continue
            if (!is5 && deg % 2 != 0) continue

            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val cosA = cos(rad)
            val sinA = sin(rad)

            val tickLen = if (is10) 12f else if (is5) 8f else 5f
            val pStart = Offset(cx + (span - tickLen) * cosA, cy - (span - tickLen) * sinA)
            val pEnd = Offset(cx + span * cosA, cy - span * sinA)
            drawLine(tickCompose, pStart, pEnd, strokeWidth = if (is10) 1.5f else 1f)

            if (is10) {
                val gStart = Offset(cx + inR * cosA, cy - inR * sinA)
                val gEnd = Offset(cx + (span - 54f) * cosA, cy - (span - 54f) * sinA)
                drawLine(faintGuide, gStart, gEnd, strokeWidth = 1f)

                // Outer scale (0° at right, 180° at left) - sits cleanly in outer lane
                val rOuter = span - 22f
                drawLabel("$deg", Offset(cx + rOuter * cosA, cy - rOuter * sinA + 3f), tickColorInt, 8.5f)

                // Inner scale (180° at right, 0° at left) - sits cleanly in inner lane across midR arc
                val rInner = span - 46f
                drawLabel("${180 - deg}", Offset(cx + rInner * cosA, cy - rInner * sinA + 3f), tickColorInt, 8.5f)
            }
        }

        // 5. Interactive Pointer Arm & Dual Angle Wedges
        val pDeg = tool.pointerAngleDeg.coerceIn(0f, 180f)
        val pRad = Math.toRadians(pDeg.toDouble()).toFloat()
        val pDir = Offset(cos(pRad), -sin(pRad))
        val armTip = Offset(cx, cy) + pDir * span

        // 5a. Soft translucent wedge from Right baseline (0° to pDeg)
        if (pDeg > 0.5f) {
            val wedgePath = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + span, cy)
                arcTo(Rect(cx - span, cy - span, cx + span, cy + span), 0f, -pDeg, false)
                close()
            }
            drawPath(wedgePath, if (isDark) Color(0x3838BDF8) else Color(0x330284C7))
        }

        // 5b. Soft translucent wedge from Left baseline (180° to pDeg)
        if (pDeg < 179.5f) {
            val leftWedgePath = Path().apply {
                moveTo(cx, cy)
                lineTo(cx - span, cy)
                arcTo(Rect(cx - span, cy - span, cx + span, cy + span), 180f, 180f - pDeg, false)
                close()
            }
            drawPath(leftWedgePath, if (isDark) Color(0x28A855F7) else Color(0x247C3AED))
        }

        // 5c. Bold arc rim from Right 0° up to pDeg (Right Arc)
        if (pDeg > 0.5f) {
            drawArc(
                color = Color(0xFF0284C7),
                startAngle = 0f,
                sweepAngle = -pDeg,
                useCenter = false,
                topLeft = Offset(cx - span, cy - span),
                size = Size(span * 2f, span * 2f),
                style = DrawStroke(4.5f)
            )
        }

        // 5c2. Bold arc rim from Left 180° down to pDeg (Left Arc)
        if (pDeg < 179.5f) {
            drawArc(
                color = Color(0xFF7C3AED),
                startAngle = 180f,
                sweepAngle = 180f - pDeg,
                useCenter = false,
                topLeft = Offset(cx - span, cy - span),
                size = Size(span * 2f, span * 2f),
                style = DrawStroke(4.5f)
            )
        }

        // 5d. Pointer Line from origin to current angle arm tip
        drawLine(Color(0xFF0284C7), Offset(cx, cy), armTip, strokeWidth = 3f)

        // 5e. Handle Tabs (Left 180°, Right 0°, and Arm Tip)
        val tabW = (32f * (span / 260f).coerceIn(0.85f, 1.4f)).coerceIn(28f, 44f)
        val tabH = (46f * (span / 260f).coerceIn(0.85f, 1.4f)).coerceIn(38f, 60f)
        val tabRadius = CornerRadius(7f, 7f)
        val tabColor = Color(0xDD0284C7)

        val rightTip = Offset(cx + span, cy)
        drawRoundRect(tabColor, topLeft = rightTip - Offset(tabW / 2f, tabH / 2f), size = Size(tabW, tabH), cornerRadius = tabRadius)
        val leftTip = Offset(cx - span, cy)
        drawRoundRect(Color(0xDD7C3AED), topLeft = leftTip - Offset(tabW / 2f, tabH / 2f), size = Size(tabW, tabH), cornerRadius = tabRadius)
        drawRoundRect(tabColor, topLeft = armTip - Offset(tabW / 2f, tabH / 2f), size = Size(tabW, tabH), cornerRadius = tabRadius)

        // 5f. Dual Angle Readout Badge in center
        val badgePos = Offset(cx, cy - span * 0.44f)
        val pillW = 96f
        val pillH = 42f
        drawRoundRect(Color.White, topLeft = badgePos - Offset(pillW / 2f, pillH / 2f), size = Size(pillW, pillH), cornerRadius = CornerRadius(21f, 21f))
        drawRoundRect(borderColor, topLeft = badgePos - Offset(pillW / 2f, pillH / 2f), size = Size(pillW, pillH), cornerRadius = CornerRadius(21f, 21f), style = DrawStroke(1.5f))
        drawLabel(String.format(Locale.US, "R: %.1f°", pDeg), Offset(badgePos.x, badgePos.y - 7f), 0xFF0284C7.toInt(), 11.5f)
        drawLabel(String.format(Locale.US, "L: %.1f°", 180f - pDeg), Offset(badgePos.x, badgePos.y + 7.5f), 0xFF7C3AED.toInt(), 11.5f)

        // 6. Bottom Shelf Buttons (Uniform spacing for all 7 controls)
        val btnY = cy + shelfH * 0.5f
        val btnR = (shelfH * 0.36f).coerceIn(16f, 24f)

        // Floating Outer Rotate Handle above top apex outside the arc
        val topRotatePos = Offset(cx, cy - span - 28f)
        drawLine(borderColor, Offset(cx, cy - span), topRotatePos, strokeWidth = 2f)
        drawButtonCircle(topRotatePos, btnR, borderColor)
        drawRotateIcon(topRotatePos, btnR * 0.5f)

        // 1. [X] Standard Close (cross on white/black circle)
        val closePos = Offset(cx - span * 0.75f, btnY)
        drawStandardCloseButton(closePos, btnR, isDark)

        // 2. Mode 1: Left Angle Button (L.ANG)
        val m1Pos = Offset(cx - span * 0.45f, btnY)
        drawButtonCircle(m1Pos, btnR, borderColor)
        drawAngleModeIcon(m1Pos, btnR, mode = 1)

        // 3. Mode Arc Left: Left Arc Button (L.ARC)
        val mArcLeftPos = Offset(cx - span * 0.15f, btnY)
        drawButtonCircle(mArcLeftPos, btnR, borderColor)
        drawAngleModeIcon(mArcLeftPos, btnR, mode = 3)

        // 4. Mode Arc Right: Right Arc Button (R.ARC)
        val mArcRightPos = Offset(cx + span * 0.15f, btnY)
        drawButtonCircle(mArcRightPos, btnR, borderColor)
        drawAngleModeIcon(mArcRightPos, btnR, mode = 0)

        // 5. Mode 2: Right Angle Button (R.ANG)
        val m2Pos = Offset(cx + span * 0.45f, btnY)
        drawButtonCircle(m2Pos, btnR, borderColor)
        drawAngleModeIcon(m2Pos, btnR, mode = 2)

        // 6. [⤡] Scale Handle
        val scalePos = Offset(cx + span * 0.75f, btnY)
        drawButtonCircle(scalePos, btnR, borderColor)
        drawScaleIcon(scalePos, btnR * 0.5f)
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

        val bodyColor = if (isDark) Color(0x330284C7) else Color(0x2838BDF8)
        val borderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        val tickColorInt = if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
        val tickCompose = Color(tickColorInt)

        // 1. Equilateral Triangle Body
        val triPath = Path().apply {
            moveTo(vTop.x, vTop.y)
            lineTo(vLeft.x, vLeft.y)
            lineTo(vRight.x, vRight.y)
            close()
        }
        drawPath(triPath, bodyColor)
        drawPath(triPath, borderColor, style = DrawStroke(2.5f))

        // 2. Centimeter measurements and labels on ALL 3 SIDES
        val sideCm = side / pxPerCm.coerceAtLeast(1f)
        val labelStr = String.format(Locale.US, "%.1f cm", sideCm)

        // Side A (Bottom: vLeft -> vRight)
        drawLabel("Side A: $labelStr", Offset(cx, vLeft.y + 16f), tickColorInt, 11f)

        // Side B (Left: vTop -> vLeft)
        val midLeft = Offset((vTop.x + vLeft.x) / 2f, (vTop.y + vLeft.y) / 2f)
        drawLabel("Side B: $labelStr", midLeft - Offset(26f, 0f), tickColorInt, 11f)

        // Side C (Right: vTop -> vRight)
        val midRight = Offset((vTop.x + vRight.x) / 2f, (vTop.y + vRight.y) / 2f)
        drawLabel("Side C: $labelStr", midRight + Offset(26f, 0f), tickColorInt, 11f)

        // Interior angle markers (60° at each vertex)
        drawLabel("60°", vTop + Offset(0f, 28f), tickColorInt, 10f)
        drawLabel("60°", vLeft + Offset(28f, -14f), tickColorInt, 10f)
        drawLabel("60°", vRight + Offset(-28f, -14f), tickColorInt, 10f)

        // 3. Side Pencils & Control Buttons
        val btnR = 21f

        // Side 1 Pencil (Bottom Base)
        val s1Pos = Offset(cx, vLeft.y - 18f)
        drawButtonCircle(s1Pos, btnR, borderColor)
        drawPencilIcon(s1Pos, btnR * 0.5f)

        // Side 2 Pencil (Left Slant)
        val s2Pos = Offset(cx - side * 0.25f + 16f, cy - h * 0.1667f)
        drawButtonCircle(s2Pos, btnR, borderColor)
        drawPencilIcon(s2Pos, btnR * 0.5f)

        // Side 3 Pencil (Right Slant)
        val s3Pos = Offset(cx + side * 0.25f - 16f, cy - h * 0.1667f)
        drawButtonCircle(s3Pos, btnR, borderColor)
        drawPencilIcon(s3Pos, btnR * 0.5f)

        // Center Full Triangle Stamp Badge
        val centerStamp = Offset(cx, cy)
        val badgeR = 27f
        drawButtonCircle(centerStamp, badgeR, borderColor)
        drawPencilIcon(centerStamp, badgeR * 0.52f)

        // Floating Rotate [⟳] Button above top apex outside the figure
        val rotPos = Offset(vTop.x, vTop.y - 28f)
        drawLine(borderColor, vTop, rotPos, strokeWidth = 2f)
        drawButtonCircle(rotPos, btnR, borderColor)
        drawRotateIcon(rotPos, btnR * 0.5f)

        // Close [X] Button (standard black/white cross button)
        val closePos = Offset(cx, cy - h * 0.35f)
        drawStandardCloseButton(closePos, btnR, isDark)

        // Scale [⤡]
        val scalePos = Offset(cx + side * 0.18f, cy + h * 0.10f)
        drawButtonCircle(scalePos, btnR, borderColor)
        drawScaleIcon(scalePos, btnR * 0.5f)
    }
}

fun DrawScope.drawStandardCloseButton(center: Offset, radius: Float, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF0F172A) else Color.White
    val cross = if (isDark) Color.White else Color(0xFF0F172A)
    val ring = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    drawCircle(bg, radius, center)
    drawCircle(ring, radius, center, style = DrawStroke(1.5f))
    val xLen = radius * 0.42f
    drawLine(cross, center - Offset(xLen, xLen), center + Offset(xLen, xLen), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(cross, center - Offset(-xLen, xLen), center + Offset(xLen, -xLen), strokeWidth = 2.5f, cap = StrokeCap.Round)
}

private fun DrawScope.drawAngleModeIcon(center: Offset, radius: Float, mode: Int) {
    val iconColor = Color(0xFF1E293B)
    val r = radius * 0.5f
    when (mode) {
        1 -> { // Left Angle Mode (L.ANG)
            val baseStart = center + Offset(-r * 0.8f, r * 0.4f)
            val baseEnd = center + Offset(r * 0.8f, r * 0.4f)
            val armEnd = baseEnd + Offset(-r * 1.0f, -r * 0.95f)
            drawLine(iconColor, baseStart, baseEnd, strokeWidth = 2.2f)
            drawLine(iconColor, baseEnd, armEnd, strokeWidth = 2.2f)
            drawArc(Color(0xFF7C3AED), 180f, 45f, false, topLeft = Offset(baseEnd.x - r * 0.5f, baseEnd.y - r * 0.5f), size = Size(r, r), style = DrawStroke(1.5f))
            drawLabel("L.ANG", center - Offset(0f, r * 0.7f), 0xFF7C3AED.toInt(), 8.5f)
        }
        3 -> { // Left Arc Mode (L.ARC)
            val rect = Rect(center.x - r * 0.85f, center.y - r * 0.85f, center.x + r * 0.85f, center.y + r * 0.85f)
            drawArc(Color(0xFF7C3AED), 180f, 65f, false, topLeft = rect.topLeft, size = rect.size, style = DrawStroke(2.6f))
            drawLabel("L.ARC", center + Offset(0f, r * 0.5f), 0xFF7C3AED.toInt(), 9f)
        }
        0 -> { // Right Arc Mode (R.ARC)
            val rect = Rect(center.x - r * 0.85f, center.y - r * 0.85f, center.x + r * 0.85f, center.y + r * 0.85f)
            drawArc(Color(0xFF0284C7), 295f, 65f, false, topLeft = rect.topLeft, size = rect.size, style = DrawStroke(2.6f))
            drawLabel("R.ARC", center + Offset(0f, r * 0.5f), 0xFF0284C7.toInt(), 9f)
        }
        2 -> { // Right Angle Mode (R.ANG)
            val baseStart = center + Offset(-r * 0.8f, r * 0.4f)
            val baseEnd = center + Offset(r * 0.8f, r * 0.4f)
            val armEnd = baseStart + Offset(r * 1.0f, -r * 0.95f)
            drawLine(iconColor, baseStart, baseEnd, strokeWidth = 2.2f)
            drawLine(iconColor, baseStart, armEnd, strokeWidth = 2.2f)
            drawArc(Color(0xFF0284C7), 315f, 45f, false, topLeft = Offset(baseStart.x - r * 0.5f, baseStart.y - r * 0.5f), size = Size(r, r), style = DrawStroke(1.5f))
            drawLabel("R.ANG", center - Offset(0f, r * 0.7f), 0xFF0284C7.toInt(), 8.5f)
        }
    }
}

private fun DrawScope.drawButtonCircle(center: Offset, radius: Float, borderColor: Color) {
    drawCircle(Color.White, radius, center)
    drawCircle(borderColor, radius, center, style = DrawStroke(2f))
}

private fun DrawScope.drawPencilIcon(center: Offset, size: Float) {
    val iconColor = Color(0xFF1E293B)
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

private fun DrawScope.drawRotateIcon(center: Offset, size: Float) {
    val iconColor = Color(0xFF1E293B)
    val rect = Rect(center.x - size, center.y - size, center.x + size, center.y + size)
    drawArc(
        color = iconColor,
        startAngle = 30f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = rect.topLeft,
        size = rect.size,
        style = DrawStroke(2.5f)
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

private fun DrawScope.drawScaleIcon(center: Offset, size: Float) {
    val iconColor = Color(0xFF1E293B)
    val p1 = center + Offset(-size * 0.7f, size * 0.7f)
    val p2 = center + Offset(size * 0.7f, -size * 0.7f)
    drawLine(iconColor, p1, p2, strokeWidth = 2.5f)
    val a1 = p2 + Offset(-6f, 0f)
    val a2 = p2 + Offset(0f, 6f)
    drawLine(iconColor, p2, a1, strokeWidth = 2.5f)
    drawLine(iconColor, p2, a2, strokeWidth = 2.5f)

    val b1 = p1 + Offset(6f, 0f)
    val b2 = p1 + Offset(0f, -6f)
    drawLine(iconColor, p1, b1, strokeWidth = 2.5f)
    drawLine(iconColor, p1, b2, strokeWidth = 2.5f)
}

private fun DrawScope.drawSpreadIcon(center: Offset, size: Float) {
    val iconColor = Color(0xFF1E293B)
    val d = size * 0.65f
    val leftStart = center - Offset(d, 0f)
    val leftEnd = center - Offset(2f, 0f)
    drawLine(iconColor, leftStart, leftEnd, strokeWidth = 2.5f)
    drawLine(iconColor, leftEnd, leftEnd + Offset(-5f, -4f), strokeWidth = 2.5f)
    drawLine(iconColor, leftEnd, leftEnd + Offset(-5f, 4f), strokeWidth = 2.5f)

    val rightStart = center + Offset(d, 0f)
    val rightEnd = center + Offset(2f, 0f)
    drawLine(iconColor, rightStart, rightEnd, strokeWidth = 2.5f)
    drawLine(iconColor, rightEnd, rightEnd + Offset(5f, -4f), strokeWidth = 2.5f)
    drawLine(iconColor, rightEnd, rightEnd + Offset(5f, 4f), strokeWidth = 2.5f)
}

private fun DrawScope.drawLabel(text: String, at: Offset, color: Int, textSize: Float) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        at.x,
        at.y,
        Paint().apply {
            this.color = color
            this.textSize = textSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    )
}

fun DrawScope.drawMathReadout(text: String, at: Offset) {
    val textPaint = Paint().apply {
        textSize = 14f
        isAntiAlias = true
    }
    val measuredW = textPaint.measureText(text)
    val pillW = (measuredW + 28f).coerceAtLeast(64f)
    val pillH = 28f
    val left = at.x - pillW / 2f
    val top = at.y - pillH / 2f

    drawRoundRect(
        color = Color(0xF20F172A),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = Color(0xFF6366F1).copy(alpha = 0.5f),
        topLeft = Offset(left, top),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(14f, 14f),
        style = DrawStroke(1.2f)
    )
    drawLabel(text, Offset(at.x, at.y + 4.5f), 0xFFFFFFFF.toInt(), 13f)
}
