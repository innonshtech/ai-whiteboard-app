package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class ShapePrompt(
    val strokeId: String,
    val replacement: Stroke
)

enum class ShapeHandle {
    START,
    END,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    VERTEX
}

data class ShapeHandleHit(
    val kind: ShapeHandle,
    val vertexIndex: Int = -1
)

private val boxShapes = setOf(
    StrokeType.RECTANGLE,
    StrokeType.SQUARE,
    StrokeType.CIRCLE,
    StrokeType.ELLIPSE,
    StrokeType.DIAMOND,
    StrokeType.STAR
)

private val lockedAspect = setOf(StrokeType.SQUARE, StrokeType.CIRCLE)

fun isEditableShape(type: StrokeType): Boolean {
    return type in boxShapes ||
        type == StrokeType.TRIANGLE ||
        type == StrokeType.LINE ||
        type == StrokeType.ARROW
}

fun hitShapeHandle(stroke: Stroke, point: Offset, slop: Float): ShapeHandleHit? {
    if (!isEditableShape(stroke.strokeType) || stroke.points.isEmpty()) return null
    val slopSq = slop * slop
    fun near(target: Offset) = hypot(point.x - target.x, point.y - target.y).let { it * it <= slopSq }
    return when (stroke.strokeType) {
        StrokeType.LINE, StrokeType.ARROW -> {
            val start = stroke.points.first()
            val end = stroke.points.last()
            when {
                near(start) -> ShapeHandleHit(ShapeHandle.START)
                near(end) -> ShapeHandleHit(ShapeHandle.END)
                else -> null
            }
        }
        StrokeType.TRIANGLE -> {
            val index = stroke.points.indices.minByOrNull { vertex ->
                hypot(point.x - stroke.points[vertex].x, point.y - stroke.points[vertex].y)
            } ?: return null
            if (near(stroke.points[index])) ShapeHandleHit(ShapeHandle.VERTEX, index) else null
        }
        else -> {
            if (stroke.points.size < 2) return null
            val box = normalizedBox(stroke.points.first(), stroke.points.last())
            when {
                near(Offset(box.left, box.top)) -> ShapeHandleHit(ShapeHandle.TOP_LEFT)
                near(Offset(box.right, box.top)) -> ShapeHandleHit(ShapeHandle.TOP_RIGHT)
                near(Offset(box.right, box.bottom)) -> ShapeHandleHit(ShapeHandle.BOTTOM_RIGHT)
                near(Offset(box.left, box.bottom)) -> ShapeHandleHit(ShapeHandle.BOTTOM_LEFT)
                else -> null
            }
        }
    }
}

fun resizeShape(stroke: Stroke, hit: ShapeHandleHit, point: Offset): Stroke {
    val points = when (stroke.strokeType) {
        StrokeType.LINE, StrokeType.ARROW -> when (hit.kind) {
            ShapeHandle.START -> listOf(point, stroke.points.last())
            ShapeHandle.END -> listOf(stroke.points.first(), point)
            else -> stroke.points
        }
        StrokeType.TRIANGLE -> {
            if (hit.kind != ShapeHandle.VERTEX || hit.vertexIndex !in stroke.points.indices) {
                stroke.points
            } else {
                stroke.points.mapIndexed { index, vertex -> if (index == hit.vertexIndex) point else vertex }
            }
        }
        else -> {
            if (stroke.points.size < 2) return stroke
            val box = normalizedBox(stroke.points.first(), stroke.points.last())
            val locked = resizeBox(box, hit.kind, point, stroke.strokeType in lockedAspect)
            listOf(Offset(locked.left, locked.top), Offset(locked.right, locked.bottom))
        }
    }
    return stroke.copy(points = points, pointPressures = emptyList())
}

private data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun normalizedBox(a: Offset, b: Offset): Box {
    return Box(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
}

private fun resizeBox(box: Box, handle: ShapeHandle, point: Offset, lockAspect: Boolean): Box {
    val anchor = when (handle) {
        ShapeHandle.TOP_LEFT -> Offset(box.right, box.bottom)
        ShapeHandle.TOP_RIGHT -> Offset(box.left, box.bottom)
        ShapeHandle.BOTTOM_LEFT -> Offset(box.right, box.top)
        ShapeHandle.BOTTOM_RIGHT -> Offset(box.left, box.top)
        else -> return box
    }
    val dragged = if (lockAspect) lockToSquare(anchor, point) else point
    var left = min(anchor.x, dragged.x)
    var top = min(anchor.y, dragged.y)
    var right = max(anchor.x, dragged.x)
    var bottom = max(anchor.y, dragged.y)
    if (right - left < 12f) {
        if (dragged.x >= anchor.x) right = left + 12f else left = right - 12f
    }
    if (bottom - top < 12f) {
        if (dragged.y >= anchor.y) bottom = top + 12f else top = bottom - 12f
    }
    return Box(left, top, right, bottom)
}

private fun lockToSquare(anchor: Offset, point: Offset): Offset {
    val dx = point.x - anchor.x
    val dy = point.y - anchor.y
    val side = max(abs(dx), abs(dy)).coerceAtLeast(12f)
    val signX = if (dx < 0f) -1f else 1f
    val signY = if (dy < 0f) -1f else 1f
    return Offset(anchor.x + signX * side, anchor.y + signY * side)
}

fun regularStarPath(first: Offset, second: Offset): Path {
    val left = min(first.x, second.x)
    val top = min(first.y, second.y)
    val right = max(first.x, second.x)
    val bottom = max(first.y, second.y)
    val cx = (left + right) / 2f
    val cy = (top + bottom) / 2f
    val rx = (right - left) / 2f
    val ry = (bottom - top) / 2f
    val path = Path()
    for (index in 0 until 10) {
        val turn = -Math.PI / 2.0 + index * Math.PI / 5.0
        val scale = if (index % 2 == 0) 1f else 0.4f
        val x = cx + (rx * scale * cos(turn)).toFloat()
        val y = cy + (ry * scale * sin(turn)).toFloat()
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
