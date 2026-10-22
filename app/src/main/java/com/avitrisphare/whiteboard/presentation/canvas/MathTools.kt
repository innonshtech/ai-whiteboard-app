package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

enum class MathToolKind { RULER, COMPASS, PROTRACTOR }

enum class MathToolZone { NONE, MOVE, ROTATE_POSITIVE, ROTATE_NEGATIVE, PENCIL, SCALE, CLOSE }

data class PlacedMathTool(
    val id: String = UUID.randomUUID().toString(),
    val kind: MathToolKind,
    val center: Offset,
    val rotationDeg: Float = 0f,
    val span: Float,
    val markedAngleDeg: Float? = null
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
private const val COMPASS_MIN_RADIUS = 40f
private const val COMPASS_MAX_RADIUS = 460f
private const val HINGE_LIFT = 52f
const val MATH_TAP_SLOP = 16f
const val RULER_EDGE_SLOP = 38f
private const val ARC_ANGLE_SLOP = 12f
private const val ARC_RADIAL_SLOP = 28f
private val RAD_TO_DEG = 180f / Math.PI.toFloat()

fun defaultMathSpan(kind: MathToolKind): Float = when (kind) {
    MathToolKind.RULER -> 440f
    MathToolKind.COMPASS -> 140f
    MathToolKind.PROTRACTOR -> 168f
}

fun newMathTool(kind: MathToolKind, center: Offset): PlacedMathTool {
    return PlacedMathTool(kind = kind, center = center, span = defaultMathSpan(kind))
}

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
    if (tool.kind != MathToolKind.RULER) return Float.MAX_VALUE
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val half = tool.span / 2f
    if (abs(local.x) > half + RULER_EDGE_SLOP) return Float.MAX_VALUE
    val halfT = RULER_THICKNESS / 2f
    return minOf(abs(local.y - halfT), abs(local.y + halfT))
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
        tool.kind == MathToolKind.RULER && distanceToRulerEdge(tool, point) <= RULER_EDGE_SLOP
    }
}

private fun hitTestCompass(tool: PlacedMathTool, point: Offset): MathToolZone {
    val pencil = compassPencil(tool)
    if (near(point, pencil, HANDLE_RADIUS)) return MathToolZone.PENCIL
    if (near(point, tool.center, HANDLE_RADIUS)) return MathToolZone.MOVE
    val hinge = compassHinge(tool)
    if (near(point, hinge, HANDLE_RADIUS)) return MathToolZone.MOVE
    if (distanceToSegment(point, hinge, tool.center) <= 18f) return MathToolZone.MOVE
    if (distanceToSegment(point, hinge, pencil) <= 18f) return MathToolZone.MOVE
    return MathToolZone.NONE
}

private fun hitTestProtractor(tool: PlacedMathTool, point: Offset): MathToolZone {
    val local = toLocal(point, tool.center, tool.rotationDeg)
    val half = tool.span
    if (near(local, Offset(half, 0f), HANDLE_RADIUS)) return MathToolZone.ROTATE_POSITIVE
    if (near(local, Offset(-half, 0f), HANDLE_RADIUS)) return MathToolZone.ROTATE_NEGATIVE
    val dist = hypot(local.x, local.y)
    val onArcSide = local.y <= 12f
    if (onArcSide && abs(dist - tool.span) <= 26f && abs(local.x) <= tool.span) {
        return MathToolZone.SCALE
    }
    if (onArcSide && dist <= tool.span - 8f) return MathToolZone.MOVE
    return MathToolZone.NONE
}

fun compassPencil(tool: PlacedMathTool): Offset {
    return tool.center + direction(tool.rotationDeg) * tool.span
}

fun compassHinge(tool: PlacedMathTool): Offset {
    val pencil = compassPencil(tool)
    val mid = Offset((tool.center.x + pencil.x) / 2f, (tool.center.y + pencil.y) / 2f)
    return mid - perpendicular(tool.rotationDeg) * HINGE_LIFT
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
fun endReach(tool: PlacedMathTool): Float {
    return if (tool.kind == MathToolKind.PROTRACTOR) tool.span else tool.span / 2f
}

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
    if (local.y > 16f) return null
    var deg = Math.toDegrees(atan2((-local.y).toDouble(), local.x.toDouble())).toFloat()
    if (deg < 0f) deg += 360f
    if (deg > 180.5f) return null
    return deg.coerceIn(0f, 180f)
}

fun rimPoint(center: Offset, rotationDeg: Float, angleDeg: Float, radius: Float): Offset {
    val rad = angleDeg / RAD_TO_DEG
    val dir = direction(rotationDeg)
    val up = -perpendicular(rotationDeg)
    return center + dir * (cos(rad) * radius) + up * (sin(rad) * radius)
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
        MathToolKind.PROTRACTOR -> drawProtractorTool(tool, isDark)
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
    val leg = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
    val accent = Color(0xFF6366F1)
    drawLine(leg, hinge, tool.center, strokeWidth = 4f)
    drawLine(accent, hinge, pencil, strokeWidth = 4f)
    drawCircle(Color(0xFF111827), 7f, tool.center)
    drawCircle(Color.White, 11f, pencil)
    drawCircle(accent, 11f, pencil, style = DrawStroke(2.5f))
    drawCircle(if (isDark) Color(0xFF374151) else Color(0xFFD1D5DB), 8f, hinge)
    val labelAt = hinge - perpendicular(tool.rotationDeg) * 22f
    drawLabel(formatCentimetres(tool.span, pxPerCm), labelAt, 0xFF4F46E5.toInt(), 15f)
}

private fun DrawScope.drawProtractorTool(tool: PlacedMathTool, isDark: Boolean) {
    rotate(degrees = tool.rotationDeg, pivot = tool.center) {
        val radius = tool.span
        val body = if (isDark) Color(0xCC1F2937) else Color(0xD9FFFFFF)
        val border = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
        val tick = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
        val arc = Path()
        val steps = 48
        for (index in 0..steps) {
            val deg = 180f * index / steps
            val rad = deg / RAD_TO_DEG
            val x = tool.center.x + cos(rad) * radius
            val y = tool.center.y - sin(rad) * radius
            if (index == 0) arc.moveTo(x, y) else arc.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(arc)
            lineTo(tool.center.x - radius, tool.center.y)
            close()
        }
        drawPath(fill, body)
        drawPath(arc, border, style = DrawStroke(2f))
        drawLine(border, Offset(tool.center.x - radius, tool.center.y), Offset(tool.center.x + radius, tool.center.y), 2f)
        for (deg in 0..180 step 10) {
            val rad = deg / RAD_TO_DEG
            val outer = radius
            val inner = radius - if (deg % 30 == 0) 18f else 10f
            val start = Offset(tool.center.x + cos(rad) * inner, tool.center.y - sin(rad) * inner)
            val end = Offset(tool.center.x + cos(rad) * outer, tool.center.y - sin(rad) * outer)
            drawLine(Color(tick), start, end, if (deg % 30 == 0) 2f else 1f)
            if (deg % 30 == 0) {
                val labelR = radius - 32f
                drawLabel(
                    deg.toString(),
                    Offset(tool.center.x + cos(rad) * labelR, tool.center.y - sin(rad) * labelR),
                    tick,
                    12f
                )
            }
        }
        drawCircle(Color(0xFF6366F1), 5f, tool.center)
        val marked = tool.markedAngleDeg
        if (marked != null) {
            val rad = marked / RAD_TO_DEG
            val tip = Offset(tool.center.x + cos(rad) * radius, tool.center.y - sin(rad) * radius)
            drawLine(Color(0xFFEF4444), tool.center, tip, 2f)
            drawLabel(formatDegrees(marked), Offset(tool.center.x, tool.center.y - 28f), 0xFFEF4444.toInt(), 16f)
        }
        val handle = Color(0xFF6366F1)
        listOf(-radius, radius).forEach { dx ->
            val center = Offset(tool.center.x + dx, tool.center.y)
            drawCircle(Color.White, HANDLE_RADIUS * 0.65f, center)
            drawCircle(handle, HANDLE_RADIUS * 0.65f, center, style = DrawStroke(2.5f))
        }
    }
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
