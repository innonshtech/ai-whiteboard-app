package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Vector eraser. Standard mode cuts ink inside a circle and keeps the rest of the stroke.
 * Smart mode deletes every stroke or object the circle touches.
 */
object StrokeEraser {
    private val inkTypes = setOf(
        StrokeType.PEN,
        StrokeType.PENCIL,
        StrokeType.MARKER,
        StrokeType.HIGHLIGHTER,
        StrokeType.BRUSH,
        StrokeType.LINE,
        StrokeType.ARROW
    )

    private val objectTypes = setOf(
        StrokeType.TEXT,
        StrokeType.STICKY_NOTE,
        StrokeType.WEB_EMBED,
        StrokeType.IMAGE,
        StrokeType.VIDEO
    )

    fun apply(
        strokes: List<Stroke>,
        center: Offset,
        radius: Float,
        mode: EraserMode
    ): List<Stroke> {
        if (radius <= 0f || strokes.isEmpty()) return strokes
        val out = ArrayList<Stroke>(strokes.size)
        var changed = false
        for (stroke in strokes) {
            val pieces = eraseOne(stroke, center, radius, mode)
            if (pieces.size != 1 || pieces.first() !== stroke) changed = true
            out.addAll(pieces)
        }
        return if (changed) out else strokes
    }

    private fun eraseOne(
        stroke: Stroke,
        center: Offset,
        radius: Float,
        mode: EraserMode
    ): List<Stroke> {
        if (stroke.points.isEmpty()) return listOf(stroke)
        val reach = radius + strokeInkHalfWidth(stroke)
        if (!boundsMayHit(stroke, center, reach)) return listOf(stroke)

        if (mode == EraserMode.SMART) {
            val hit = if (stroke.strokeType in objectTypes || stroke.strokeType == StrokeType.FRAME) {
                circleTouchesRect(center, reach, stroke)
            } else {
                val points = outlineOf(stroke) ?: stroke.points
                centerlineHits(points, center, reach)
            }
            return if (hit) emptyList() else listOf(stroke)
        }

        if (stroke.strokeType in objectTypes) return listOf(stroke)
        val source = if (stroke.strokeType in inkTypes) stroke.points else outlineOf(stroke)
            ?: return listOf(stroke)
        if (!centerlineHits(source, center, reach)) return listOf(stroke)

        val pressures = if (source === stroke.points) stroke.pointPressures else emptyList()
        val pieces = splitPolyline(source, pressures, center, reach)
        if (pieces.isEmpty()) return emptyList()
        val keepType = stroke.strokeType in inkTypes
        return pieces.map { (points, piecePressure) ->
            stroke.copy(
                id = UUID.randomUUID().toString(),
                points = points,
                pointPressures = piecePressure,
                strokeType = if (keepType) stroke.strokeType else StrokeType.PEN,
                text = if (keepType) stroke.text else "",
                fillColor = if (keepType) stroke.fillColor else 0,
                imageUri = if (keepType) stroke.imageUri else ""
            )
        }
    }

    private fun boundsMayHit(stroke: Stroke, center: Offset, reach: Float): Boolean {
        val bounds = stroke.bounds
        return center.x >= bounds.left - reach &&
            center.x <= bounds.right + reach &&
            center.y >= bounds.top - reach &&
            center.y <= bounds.bottom + reach
    }

    private fun circleTouchesRect(center: Offset, radius: Float, stroke: Stroke): Boolean {
        val bounds = stroke.bounds
        val nearestX = center.x.coerceIn(bounds.left, bounds.right)
        val nearestY = center.y.coerceIn(bounds.top, bounds.bottom)
        val dx = center.x - nearestX
        val dy = center.y - nearestY
        return dx * dx + dy * dy <= radius * radius
    }

    private fun centerlineHits(points: List<Offset>, center: Offset, reach: Float): Boolean {
        if (points.isEmpty()) return false
        val reachSq = reach * reach
        if (points.size == 1) {
            val dx = points[0].x - center.x
            val dy = points[0].y - center.y
            return dx * dx + dy * dy <= reachSq
        }
        for (i in 0 until points.lastIndex) {
            if (CanvasTransform.distanceToSegmentSquared(center, points[i], points[i + 1]) <= reachSq) {
                return true
            }
        }
        return false
    }

    private fun outlineOf(stroke: Stroke): List<Offset>? {
        if (stroke.points.size < 2) return null
        val a = stroke.points.first()
        val b = stroke.points.last()
        val left = minOf(a.x, b.x)
        val top = minOf(a.y, b.y)
        val right = maxOf(a.x, b.x)
        val bottom = maxOf(a.y, b.y)
        return when (stroke.strokeType) {
            StrokeType.TRIANGLE -> stroke.points + stroke.points.first()
            StrokeType.RECTANGLE, StrokeType.SQUARE, StrokeType.FRAME -> listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(right, bottom),
                Offset(left, bottom),
                Offset(left, top)
            )
            StrokeType.DIAMOND -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                listOf(
                    Offset(cx, top),
                    Offset(right, cy),
                    Offset(cx, bottom),
                    Offset(left, cy),
                    Offset(cx, top)
                )
            }
            StrokeType.CIRCLE, StrokeType.ELLIPSE -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rx = (right - left) / 2f
                val ry = (bottom - top) / 2f
                List(49) { index ->
                    val angle = index / 48f * (2.0 * PI).toFloat()
                    Offset(cx + rx * cos(angle), cy + ry * sin(angle))
                }
            }
            StrokeType.STAR -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val outer = kotlin.math.min(right - left, bottom - top) / 2f
                val inner = outer * 0.45f
                List(11) { i ->
                    val angle = -Math.PI / 2 + i * Math.PI / 5
                    val r = if (i % 2 == 0) outer else inner
                    Offset(cx + (r * cos(angle)).toFloat(), cy + (r * sin(angle)).toFloat())
                }
            }
            StrokeType.HEXAGON -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val r = kotlin.math.min(right - left, bottom - top) / 2f
                List(7) { i ->
                    val angle = i / 6f * (2.0 * PI).toFloat() - (PI / 2).toFloat()
                    Offset(cx + r * cos(angle), cy + r * sin(angle))
                }
            }
            StrokeType.ROUND_RECT -> listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(right, bottom),
                Offset(left, bottom),
                Offset(left, top)
            )
            else -> null
        }
    }

    private fun splitPolyline(
        points: List<Offset>,
        pressures: List<Float>,
        center: Offset,
        reach: Float
    ): List<Pair<List<Offset>, List<Float>>> {
        if (points.isEmpty()) return emptyList()
        val reachSq = reach * reach
        fun inside(point: Offset): Boolean {
            val dx = point.x - center.x
            val dy = point.y - center.y
            return dx * dx + dy * dy <= reachSq
        }
        fun pressureAt(index: Int): Float = pressures.getOrElse(index) { 1f }

        if (points.size == 1) {
            return if (inside(points[0])) emptyList() else listOf(points to listOf(pressureAt(0)))
        }

        val pieces = ArrayList<Pair<List<Offset>, List<Float>>>()
        var building: MutableList<Offset>? = null
        var buildingPressure: MutableList<Float>? = null

        fun close() {
            val current = building ?: return
            if (current.isNotEmpty()) pieces.add(current to buildingPressure!!)
            building = null
            buildingPressure = null
        }

        fun add(point: Offset, pressure: Float) {
            val current = building
            if (current == null) {
                building = mutableListOf(point)
                buildingPressure = mutableListOf(pressure)
                return
            }
            val last = current.last()
            if (last.x == point.x && last.y == point.y) return
            current.add(point)
            buildingPressure!!.add(pressure)
        }

        if (!inside(points[0])) add(points[0], pressureAt(0))

        for (i in 0 until points.lastIndex) {
            val start = points[i]
            val end = points[i + 1]
            val startPressure = pressureAt(i)
            val endPressure = pressureAt(i + 1)
            val hits = circleCrossings(start, end, center, reach)
            var outside = !inside(start)
            for (t in hits) {
                val hit = Offset(
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t
                )
                val hitPressure = startPressure + (endPressure - startPressure) * t
                if (outside) {
                    add(hit, hitPressure)
                    close()
                } else {
                    add(hit, hitPressure)
                }
                outside = !outside
            }
            if (!inside(end)) add(end, endPressure) else close()
        }
        close()
        return pieces
    }

    private fun circleCrossings(start: Offset, end: Offset, center: Offset, radius: Float): List<Float> {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val fx = start.x - center.x
        val fy = start.y - center.y
        val a = dx * dx + dy * dy
        if (a == 0f) return emptyList()
        val b = 2f * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - radius * radius
        val discriminant = b * b - 4f * a * c
        if (discriminant < 0f) return emptyList()
        val root = sqrt(discriminant)
        val t1 = (-b - root) / (2f * a)
        val t2 = (-b + root) / (2f * a)
        val hits = ArrayList<Float>(2)
        if (t1 > 0.0001f && t1 < 0.9999f) hits.add(t1)
        if (t2 > 0.0001f && t2 < 0.9999f && t2 != t1) hits.add(t2)
        hits.sort()
        return hits
    }
}
