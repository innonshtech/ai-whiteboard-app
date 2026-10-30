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

private val lineShapes = setOf(
    StrokeType.LINE,
    StrokeType.LINE_DASHED,
    StrokeType.ARROW,
    StrokeType.DOUBLE_ARROW,
    StrokeType.DASHED_ARROW,
    StrokeType.ANGLE_RAY
)

private val boxShapes = setOf(
    StrokeType.RECTANGLE,
    StrokeType.ROUNDED_RECTANGLE,
    StrokeType.SQUARE,
    StrokeType.CIRCLE,
    StrokeType.ELLIPSE,
    StrokeType.SEMICIRCLE,
    StrokeType.DIAMOND,
    StrokeType.STAR,
    StrokeType.RIGHT_TRIANGLE,
    StrokeType.SCALENE_TRIANGLE,
    StrokeType.TRAPEZOID,
    StrokeType.PARALLELOGRAM,
    StrokeType.RHOMBUS,
    StrokeType.PENTAGON,
    StrokeType.HEXAGON,
    StrokeType.OCTAGON,
    StrokeType.CUBE,
    StrokeType.CUBOID,
    StrokeType.PYRAMID,
    StrokeType.PRISM,
    StrokeType.CYLINDER,
    StrokeType.CONE,
    StrokeType.SPHERE,
    StrokeType.ROUND_RECT,
    StrokeType.IMAGE,
    StrokeType.VIDEO,
    StrokeType.WEB_EMBED,
    StrokeType.STICKY_NOTE,
    StrokeType.FRAME
)

val lockedAspect = setOf(
    StrokeType.SQUARE,
    StrokeType.CIRCLE,
    StrokeType.SPHERE,
    StrokeType.CUBE,
    StrokeType.PENTAGON,
    StrokeType.HEXAGON,
    StrokeType.OCTAGON
)

fun isEditableShape(type: StrokeType): Boolean {
    return type in boxShapes ||
        type in lineShapes ||
        type == StrokeType.TRIANGLE ||
        type == StrokeType.WEB_EMBED ||
        type == StrokeType.IMAGE ||
        type == StrokeType.VIDEO
}

fun hitShapeHandle(stroke: Stroke, point: Offset, slop: Float): ShapeHandleHit? {
    if (!isEditableShape(stroke.strokeType) || stroke.points.isEmpty()) return null
    val slopSq = slop * slop
    fun near(target: Offset) = hypot(point.x - target.x, point.y - target.y).let { it * it <= slopSq }
    return when {
        stroke.strokeType in lineShapes -> {
            val start = stroke.points.first()
            val end = stroke.points.last()
            when {
                near(start) -> ShapeHandleHit(ShapeHandle.START)
                near(end) -> ShapeHandleHit(ShapeHandle.END)
                else -> null
            }
        }
        stroke.strokeType == StrokeType.TRIANGLE -> {
            val index = stroke.points.indices.minByOrNull { vertex ->
                hypot(point.x - stroke.points[vertex].x, point.y - stroke.points[vertex].y)
            } ?: return null
            if (near(stroke.points[index])) ShapeHandleHit(ShapeHandle.VERTEX, index) else null
        }
        stroke.strokeType == StrokeType.SQUARE ||
        stroke.strokeType == StrokeType.WEB_EMBED ||
        stroke.strokeType == StrokeType.IMAGE ||
        stroke.strokeType == StrokeType.VIDEO -> {
            if (stroke.points.size < 2) return null
            val b = strokeSelectionBounds(stroke)
            val pad = 6f
            val boxLeft = b.left - pad
            val boxTop = b.top - pad
            val boxRight = b.right + pad
            val boxBottom = b.bottom + pad
            when {
                near(Offset(boxLeft, boxTop)) || near(Offset(b.left, b.top)) -> ShapeHandleHit(ShapeHandle.TOP_LEFT)
                near(Offset(boxRight, boxTop)) || near(Offset(b.right, b.top)) -> ShapeHandleHit(ShapeHandle.TOP_RIGHT)
                near(Offset(boxRight, boxBottom)) || near(Offset(b.right, b.bottom)) -> ShapeHandleHit(ShapeHandle.BOTTOM_RIGHT)
                near(Offset(boxLeft, boxBottom)) || near(Offset(b.left, b.bottom)) -> ShapeHandleHit(ShapeHandle.BOTTOM_LEFT)
                else -> null
            }
        }
        else -> {
            if (stroke.points.size < 2) return null
            val box = normalizedBox(stroke.points.first(), stroke.points.last())
            val pad = 6f
            val boxLeft = box.left - pad
            val boxTop = box.top - pad
            val boxRight = box.right + pad
            val boxBottom = box.bottom + pad
            when {
                near(Offset(boxLeft, boxTop)) || near(Offset(box.left, box.top)) -> ShapeHandleHit(ShapeHandle.TOP_LEFT)
                near(Offset(boxRight, boxTop)) || near(Offset(box.right, box.top)) -> ShapeHandleHit(ShapeHandle.TOP_RIGHT)
                near(Offset(boxRight, boxBottom)) || near(Offset(box.right, box.bottom)) -> ShapeHandleHit(ShapeHandle.BOTTOM_RIGHT)
                near(Offset(boxLeft, boxBottom)) || near(Offset(box.left, box.bottom)) -> ShapeHandleHit(ShapeHandle.BOTTOM_LEFT)
                else -> null
            }
        }
    }
}

fun resizeShape(stroke: Stroke, hit: ShapeHandleHit, point: Offset): Stroke {
    val points = when {
        stroke.strokeType in lineShapes -> when (hit.kind) {
            ShapeHandle.START -> listOf(point, stroke.points.last())
            ShapeHandle.END -> listOf(stroke.points.first(), point)
            else -> stroke.points
        }
        stroke.strokeType == StrokeType.TRIANGLE -> {
            if (hit.kind != ShapeHandle.VERTEX || hit.vertexIndex !in stroke.points.indices) {
                stroke.points
            } else {
                stroke.points.mapIndexed { index, vertex -> if (index == hit.vertexIndex) point else vertex }
            }
        }
        else -> {
            if (stroke.points.size < 2) return stroke
            val box = normalizedBox(stroke.points.first(), stroke.points.last())
            val minW = if (stroke.strokeType == StrokeType.WEB_EMBED) 260f else 12f
            val minH = if (stroke.strokeType == StrokeType.WEB_EMBED) 180f else 12f
            val locked = resizeBox(box, hit.kind, point, stroke.strokeType in lockedAspect, minW, minH)
            listOf(Offset(locked.left, locked.top), Offset(locked.right, locked.bottom))
        }
    }
    return stroke.copy(points = points, pointPressures = emptyList())
}

private data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun normalizedBox(a: Offset, b: Offset): Box {
    return Box(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
}

private fun resizeBox(
    box: Box,
    handle: ShapeHandle,
    point: Offset,
    lockAspect: Boolean,
    minWidth: Float = 12f,
    minHeight: Float = 12f
): Box {
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
    if (right - left < minWidth) {
        if (dragged.x >= anchor.x) right = left + minWidth else left = right - minWidth
    }
    if (bottom - top < minHeight) {
        if (dragged.y >= anchor.y) bottom = top + minHeight else top = bottom - minHeight
    }
    return Box(left, top, right, bottom)
}

fun lockToSquare(anchor: Offset, point: Offset): Offset {
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

fun isContainerShape(type: StrokeType): Boolean {
    return type == StrokeType.RECTANGLE ||
        type == StrokeType.ROUND_RECT ||
        type == StrokeType.ROUNDED_RECTANGLE ||
        type == StrokeType.SQUARE ||
        type == StrokeType.CIRCLE ||
        type == StrokeType.ELLIPSE ||
        type == StrokeType.DIAMOND ||
        type == StrokeType.STICKY_NOTE
}

fun isMindMapNode(stroke: Stroke): Boolean {
    return stroke.stickerKind.startsWith("mindmap:") ||
        stroke.stickerKind.startsWith("flow:")
}

fun hitSelectionBoundsHandle(strokes: List<Stroke>, point: Offset, slop: Float): Pair<ShapeHandle, androidx.compose.ui.geometry.Rect>? {
    if (strokes.isEmpty()) return null
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    for (stroke in strokes) {
        val b = strokeSelectionBounds(stroke)
        left = minOf(left, b.left)
        top = minOf(top, b.top)
        right = maxOf(right, b.right)
        bottom = maxOf(bottom, b.bottom)
    }
    if (left >= right || top >= bottom) return null
    val pad = 6f
    val boxLeft = left - pad
    val boxTop = top - pad
    val boxRight = right + pad
    val boxBottom = bottom + pad
    val origBounds = androidx.compose.ui.geometry.Rect(left, top, right, bottom)

    val slopSq = slop * slop
    fun near(t: Offset) = hypot(point.x - t.x, point.y - t.y).let { it * it <= slopSq }

    val midX = boxLeft + (boxRight - boxLeft) / 2f
    val midY = boxTop + (boxBottom - boxTop) / 2f

    // 1. Corners (Primary handles)
    if (near(Offset(boxLeft, boxTop)) || near(Offset(left, top))) return ShapeHandle.TOP_LEFT to origBounds
    if (near(Offset(boxRight, boxTop)) || near(Offset(right, top))) return ShapeHandle.TOP_RIGHT to origBounds
    if (near(Offset(boxRight, boxBottom)) || near(Offset(right, bottom))) return ShapeHandle.BOTTOM_RIGHT to origBounds
    if (near(Offset(boxLeft, boxBottom)) || near(Offset(left, bottom))) return ShapeHandle.BOTTOM_LEFT to origBounds

    return null
}
