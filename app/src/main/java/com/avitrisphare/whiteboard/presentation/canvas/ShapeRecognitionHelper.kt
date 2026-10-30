package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

const val SHAPE_PROMPT_DELAY_MS = 500L
const val SHAPE_AUTO_CONVERT_MS = 2000L

data class RecognizedShape(
    val strokeType: StrokeType,
    val points: List<Offset>,
    val confidence: Float
)

object ShapeRecognitionHelper {
    fun recognize(points: List<Offset>): RecognizedShape? {
        if (points.size < 8) return null
        val path = pathLength(points)
        if (path < 36f) return null
        val samples = resample(points, 64)
        val closed = isClosed(samples, path)
        if (!closed) {
            return recognizeOpen(samples, path)
        }
        val star = recognizeStar(samples)
        if (star != null) return star
        val corners = findCorners(samples)
        return when (corners.size) {
            3 -> triangle(corners)
            4 -> quadrilateral(corners)
            5 -> starFrom(corners)
            else -> ellipseOrCircle(samples)
        }
    }

    private fun recognizeOpen(samples: List<Offset>, path: Float): RecognizedShape? {
        val straight = straightness(samples)
        val tipIndex = indexFarthestFrom(samples, samples.first())
        val tip = samples[tipIndex]
        val tail = samples.first()
        val shaft = hypot(tip.x - tail.x, tip.y - tail.y)
        val afterTip = if (tipIndex < samples.lastIndex) {
            hypot(samples.last().x - tip.x, samples.last().y - tip.y)
        } else {
            0f
        }
        val headed = tipIndex > samples.size / 2 &&
            afterTip > 10f &&
            afterTip < shaft * 0.55f &&
            straightness(samples.subList(0, tipIndex + 1)) > 0.5f
        if (headed) {
            return RecognizedShape(
                strokeType = StrokeType.ARROW,
                points = listOf(tail, tip),
                confidence = 0.8f
            )
        }
        if (straight > 0.86f && shaft > 24f) {
            val ends = principalEnds(samples)
            return RecognizedShape(
                strokeType = StrokeType.LINE,
                points = listOf(ends.first, ends.second),
                confidence = straight
            )
        }
        if (path < 1f) return null
        return null
    }

    private fun triangle(corners: List<Offset>): RecognizedShape? {
        val area = triangleArea(corners[0], corners[1], corners[2])
        val span = max(boundsOf(corners).second, 1f)
        if (area < span * 8f) return null
        return RecognizedShape(StrokeType.TRIANGLE, corners, 0.84f)
    }

    private fun quadrilateral(corners: List<Offset>): RecognizedShape? {
        val box = axisBounds(corners)
        val width = box.right - box.left
        val height = box.bottom - box.top
        if (width < 16f || height < 16f) return null
        val diagonal = hypot(width, height)
        val cornerScore = corners.map { distanceToBoxCorner(it, box) }.average()
        val midScore = corners.map { distanceToEdgeMidpoint(it, box) }.average()
        if (midScore + 4f < cornerScore && midScore < diagonal * 0.22f) {
            return RecognizedShape(
                StrokeType.DIAMOND,
                listOf(Offset(box.left, box.top), Offset(box.right, box.bottom)),
                0.82f
            )
        }
        if (cornerScore > diagonal * 0.28f) return ellipseOrCircle(corners)
        val aspect = width / height
        return if (aspect in 0.82f..1.22f) {
            val side = max(width, height)
            val cx = (box.left + box.right) / 2f
            val cy = (box.top + box.bottom) / 2f
            RecognizedShape(
                StrokeType.SQUARE,
                listOf(
                    Offset(cx - side / 2f, cy - side / 2f),
                    Offset(cx + side / 2f, cy + side / 2f)
                ),
                0.86f
            )
        } else {
            RecognizedShape(
                StrokeType.RECTANGLE,
                listOf(Offset(box.left, box.top), Offset(box.right, box.bottom)),
                0.84f
            )
        }
    }

    private fun ellipseOrCircle(samples: List<Offset>): RecognizedShape? {
        val box = axisBounds(samples)
        val width = box.right - box.left
        val height = box.bottom - box.top
        if (width < 16f || height < 16f) return null
        val rx = width / 2f
        val ry = height / 2f
        val cx = (box.left + box.right) / 2f
        val cy = (box.top + box.bottom) / 2f
        var error = 0f
        for (point in samples) {
            val nx = (point.x - cx) / rx
            val ny = (point.y - cy) / ry
            error += abs(hypot(nx, ny) - 1f)
        }
        error /= samples.size
        if (error > 0.2f) return null
        val aspect = width / height
        return if (aspect in 0.86f..1.16f) {
            val radius = (rx + ry) / 2f
            RecognizedShape(
                StrokeType.CIRCLE,
                listOf(Offset(cx - radius, cy - radius), Offset(cx + radius, cy + radius)),
                1f - error
            )
        } else {
            RecognizedShape(
                StrokeType.ELLIPSE,
                listOf(Offset(box.left, box.top), Offset(box.right, box.bottom)),
                1f - error
            )
        }
    }

    private fun starFrom(corners: List<Offset>): RecognizedShape {
        val box = axisBounds(corners)
        return RecognizedShape(
            StrokeType.STAR,
            listOf(Offset(box.left, box.top), Offset(box.right, box.bottom)),
            0.8f
        )
    }

    private fun recognizeStar(samples: List<Offset>): RecognizedShape? {
        val center = centroid(samples)
        val radii = samples.map { hypot(it.x - center.x, it.y - center.y) }
        val maxRadius = radii.maxOrNull() ?: return null
        if (maxRadius < 18f) return null
        val prominence = maxRadius * 0.18f
        val window = 3
        val peaks = ArrayList<Int>()
        for (index in radii.indices) {
            val radius = radii[index]
            if (radius < maxRadius * 0.72f) continue
            var isPeak = true
            for (offset in 1..window) {
                val before = radii[(index - offset + radii.size) % radii.size]
                val after = radii[(index + offset) % radii.size]
                if (radius + 0.5f < before || radius + 0.5f < after) {
                    isPeak = false
                    break
                }
            }
            if (!isPeak) continue
            val around = (index - window * 2 + radii.size) % radii.size
            val later = (index + window * 2) % radii.size
            if (radius - radii[around] < prominence && radius - radii[later] < prominence) continue
            if (peaks.isEmpty() || circularDistance(peaks.last(), index, radii.size) > 6) {
                peaks.add(index)
            }
        }
        if (peaks.size != 5) return null
        val valleys = peaks.map { peak ->
            val next = peaks[(peaks.indexOf(peak) + 1) % peaks.size]
            var lowest = Float.MAX_VALUE
            var cursor = (peak + 1) % radii.size
            var guard = 0
            while (cursor != next && guard < radii.size) {
                if (radii[cursor] < lowest) lowest = radii[cursor]
                cursor = (cursor + 1) % radii.size
                guard++
            }
            lowest
        }
        val peakAverage = peaks.map { radii[it] }.average()
        val valleyAverage = valleys.average()
        if (valleyAverage > peakAverage * 0.72) return null
        val box = axisBounds(samples)
        return RecognizedShape(
            StrokeType.STAR,
            listOf(Offset(box.left, box.top), Offset(box.right, box.bottom)),
            0.8f
        )
    }
}

private data class AxisBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun axisBounds(points: List<Offset>): AxisBounds {
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    for (point in points) {
        left = min(left, point.x)
        top = min(top, point.y)
        right = max(right, point.x)
        bottom = max(bottom, point.y)
    }
    return AxisBounds(left, top, right, bottom)
}

private fun boundsOf(points: List<Offset>): Pair<Float, Float> {
    val box = axisBounds(points)
    return (box.right - box.left) to (box.bottom - box.top)
}

private fun pathLength(points: List<Offset>): Float {
    var length = 0f
    for (index in 1 until points.size) {
        length += hypot(points[index].x - points[index - 1].x, points[index].y - points[index - 1].y)
    }
    return length
}

private fun straightness(points: List<Offset>): Float {
    if (points.size < 2) return 1f
    val direct = hypot(points.last().x - points.first().x, points.last().y - points.first().y)
    val length = pathLength(points)
    return if (length <= 1f) 1f else direct / length
}

private fun isClosed(samples: List<Offset>, path: Float): Boolean {
    val gap = hypot(samples.last().x - samples.first().x, samples.last().y - samples.first().y)
    val box = axisBounds(samples)
    val diagonal = hypot(box.right - box.left, box.bottom - box.top)
    return gap < path * 0.2f || gap < diagonal * 0.28f
}

private fun resample(points: List<Offset>, count: Int): List<Offset> {
    if (points.size < 2 || count < 2) return points
    val cumulative = FloatArray(points.size)
    for (index in 1 until points.size) {
        cumulative[index] = cumulative[index - 1] + hypot(
            points[index].x - points[index - 1].x,
            points[index].y - points[index - 1].y
        )
    }
    val total = cumulative.last()
    if (total < 1f) return points
    val step = total / (count - 1)
    val sampled = ArrayList<Offset>(count)
    var cursor = 0
    for (index in 0 until count) {
        val target = step * index
        while (cursor < cumulative.lastIndex && cumulative[cursor + 1] < target) cursor++
        val start = cumulative[cursor]
        val end = cumulative[min(cursor + 1, cumulative.lastIndex)]
        val span = (end - start).coerceAtLeast(0.0001f)
        val t = ((target - start) / span).coerceIn(0f, 1f)
        val from = points[cursor]
        val to = points[min(cursor + 1, points.lastIndex)]
        sampled.add(Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t))
    }
    return sampled
}

private fun findCorners(samples: List<Offset>): List<Offset> {
    if (samples.size < 12) return emptyList()
    val gap = 4
    val scores = FloatArray(samples.size)
    for (index in samples.indices) {
        val previous = samples[(index - gap + samples.size) % samples.size]
        val current = samples[index]
        val next = samples[(index + gap) % samples.size]
        scores[index] = turningDegrees(previous, current, next)
    }
    val found = ArrayList<Int>()
    for (index in samples.indices) {
        if (scores[index] < 48f) continue
        val before = scores[(index - 1 + samples.size) % samples.size]
        val after = scores[(index + 1) % samples.size]
        if (scores[index] + 0.1f < before || scores[index] + 0.1f < after) continue
        if (found.isEmpty() || circularDistance(found.last(), index, samples.size) > 5) {
            found.add(index)
        }
    }
    if (found.size > 1 && circularDistance(found.first(), found.last(), samples.size) <= 5) {
        found.removeAt(found.lastIndex)
    }
    return found.map { samples[it] }
}

private fun turningDegrees(previous: Offset, current: Offset, next: Offset): Float {
    val first = atan2((current.y - previous.y).toDouble(), (current.x - previous.x).toDouble())
    val second = atan2((next.y - current.y).toDouble(), (next.x - current.x).toDouble())
    var delta = abs(second - first)
    if (delta > Math.PI) delta = 2 * Math.PI - delta
    return Math.toDegrees(delta).toFloat()
}

private fun circularDistance(from: Int, to: Int, size: Int): Int {
    val raw = abs(to - from)
    return min(raw, size - raw)
}

private fun indexFarthestFrom(points: List<Offset>, origin: Offset): Int {
    var best = 0
    var bestDistance = -1f
    for (index in points.indices) {
        val distance = hypot(points[index].x - origin.x, points[index].y - origin.y)
        if (distance > bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return best
}

private fun centroid(points: List<Offset>): Offset {
    var x = 0f
    var y = 0f
    for (point in points) {
        x += point.x
        y += point.y
    }
    return Offset(x / points.size, y / points.size)
}

private fun principalEnds(points: List<Offset>): Pair<Offset, Offset> {
    val center = centroid(points)
    var xx = 0f
    var xy = 0f
    var yy = 0f
    for (point in points) {
        val dx = point.x - center.x
        val dy = point.y - center.y
        xx += dx * dx
        xy += dx * dy
        yy += dy * dy
    }
    val angle = 0.5 * atan2((2f * xy).toDouble(), (xx - yy).toDouble())
    val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
    var minPoint = points.first()
    var maxPoint = points.first()
    var minProjection = Float.MAX_VALUE
    var maxProjection = -Float.MAX_VALUE
    for (point in points) {
        val projection = (point.x - center.x) * dir.x + (point.y - center.y) * dir.y
        if (projection < minProjection) {
            minProjection = projection
            minPoint = point
        }
        if (projection > maxProjection) {
            maxProjection = projection
            maxPoint = point
        }
    }
    return minPoint to maxPoint
}

private fun distanceToBoxCorner(point: Offset, box: AxisBounds): Float {
    val corners = listOf(
        Offset(box.left, box.top),
        Offset(box.right, box.top),
        Offset(box.right, box.bottom),
        Offset(box.left, box.bottom)
    )
    return corners.minOf { hypot(point.x - it.x, point.y - it.y) }
}

private fun distanceToEdgeMidpoint(point: Offset, box: AxisBounds): Float {
    val mids = listOf(
        Offset((box.left + box.right) / 2f, box.top),
        Offset(box.right, (box.top + box.bottom) / 2f),
        Offset((box.left + box.right) / 2f, box.bottom),
        Offset(box.left, (box.top + box.bottom) / 2f)
    )
    return mids.minOf { hypot(point.x - it.x, point.y - it.y) }
}

private fun triangleArea(a: Offset, b: Offset, c: Offset): Float {
    return abs((a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)) / 2f)
}
