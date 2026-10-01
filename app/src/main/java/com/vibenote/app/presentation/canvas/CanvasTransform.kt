package com.vibenote.app.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vibenote.app.domain.model.Stroke
import kotlin.math.max
import kotlin.math.min

object CanvasTransform {

    const val MIN_SCALE = 0.001f
    const val MAX_SCALE = 100.0f
    const val DEFAULT_SCALE = 1.0f

    /**
     * Dynamically computes grid spacing based on zoom level so line/dot count stays constant and performant.
     */
    fun getAdaptiveGridSpacing(baseSpacing: Float = 80f, scale: Float): Float {
        var spacing = baseSpacing
        val safeScale = if (scale <= 0f) 1f else scale
        while (spacing * safeScale < 40f && spacing < 100_000f) {
            spacing *= 4f
        }
        while (spacing * safeScale > 240f && spacing > 2f) {
            spacing /= 4f
        }
        return spacing
    }

    /**
     * Formats zoom level into a readable percentage string.
     */
    fun formatZoom(scale: Float): String {
        val pct = scale * 100f
        return when {
            pct < 0.1f -> "0.01%"
            pct < 1f -> String.format(java.util.Locale.US, "%.1f%%", pct)
            pct < 10f -> String.format(java.util.Locale.US, "%.1f%%", pct)
            else -> "${pct.toInt()}%"
        }
    }


    /**
     * Converts a coordinate from Screen (device viewport) space to World (infinite canvas) space.
     */
    fun screenToWorld(screenPoint: Offset, scale: Float, offset: Offset): Offset {
        val safeScale = if (scale == 0f) 1f else scale
        return Offset(
            x = (screenPoint.x - offset.x) / safeScale,
            y = (screenPoint.y - offset.y) / safeScale
        )
    }

    /**
     * Converts a coordinate from World space to Screen space.
     */
    fun worldToScreen(worldPoint: Offset, scale: Float, offset: Offset): Offset {
        return Offset(
            x = worldPoint.x * scale + offset.x,
            y = worldPoint.y * scale + offset.y
        )
    }

    /**
     * Computes the visible World bounding rectangle given the current screen viewport size, scale, and offset.
     */
    fun calculateViewportWorldBounds(viewportSize: Size, scale: Float, offset: Offset): Rect {
        val safeScale = if (scale == 0f) 1f else scale
        val left = -offset.x / safeScale
        val top = -offset.y / safeScale
        val right = (viewportSize.width - offset.x) / safeScale
        val bottom = (viewportSize.height - offset.y) / safeScale
        return Rect(left, top, right, bottom)
    }

    /**
     * Computes the union bounding box across all strokes in World coordinates.
     */
    fun calculateContentBounds(strokes: List<Stroke>): Rect? {
        val validStrokes = strokes.filter { it.points.isNotEmpty() }
        if (validStrokes.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (stroke in validStrokes) {
            val b = stroke.bounds
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
        }

        if (minX > maxX || minY > maxY) return null
        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Calculates the scale and offset needed to fit and center content bounds within the screen viewport.
     * Returns Triple(targetScale, targetOffsetX, targetOffsetY).
     */
    fun calculateFitTransform(
        contentBounds: Rect?,
        viewportSize: Size,
        paddingPx: Float = 80f
    ): Triple<Float, Float, Float> {
        if (contentBounds == null || viewportSize.width <= 0 || viewportSize.height <= 0) {
            return Triple(1f, 0f, 0f)
        }

        val availableWidth = max(10f, viewportSize.width - paddingPx * 2f)
        val availableHeight = max(10f, viewportSize.height - paddingPx * 2f)

        val contentWidth = max(10f, contentBounds.width)
        val contentHeight = max(10f, contentBounds.height)

        val scaleX = availableWidth / contentWidth
        val scaleY = availableHeight / contentHeight
        val fitScale = min(scaleX, scaleY).coerceIn(MIN_SCALE, 2.5f)

        val contentCenter = contentBounds.center
        val targetOffsetX = (viewportSize.width / 2f) - (contentCenter.x * fitScale)
        val targetOffsetY = (viewportSize.height / 2f) - (contentCenter.y * fitScale)

        return Triple(fitScale, targetOffsetX, targetOffsetY)
    }

    /**
     * Calculates the updated pan offset during a pinch-to-zoom gesture keeping the centroid pinned.
     */
    fun calculatePivotZoomOffset(
        oldScale: Float,
        newScale: Float,
        oldOffset: Offset,
        centroid: Offset,
        panDelta: Offset
    ): Offset {
        val safeOldScale = if (oldScale == 0f) 1f else oldScale
        val scaleFactor = newScale / safeOldScale
        val newX = centroid.x - (centroid.x - oldOffset.x) * scaleFactor + panDelta.x
        val newY = centroid.y - (centroid.y - oldOffset.y) * scaleFactor + panDelta.y
        return Offset(newX, newY)
    }

    /**
     * Calculates squared distance from point p to line segment v-w.
     */
    fun distanceToSegmentSquared(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) {
            val dx = p.x - v.x
            val dy = p.y - v.y
            return dx * dx + dy * dy
        }
        val t = max(0f, min(1f, ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2))
        val projX = v.x + t * (w.x - v.x)
        val projY = v.y + t * (w.y - v.y)
        val dx = p.x - projX
        val dy = p.y - projY
        return dx * dx + dy * dy
    }

    /**
     * Tests if a point in World space is close enough to intersect any segment of the stroke.
     */
    fun strokeIntersectsPoint(stroke: Stroke, worldPoint: Offset, thresholdRadius: Float): Boolean {
        val thresholdSq = (thresholdRadius + stroke.strokeWidth / 2f) * (thresholdRadius + stroke.strokeWidth / 2f)
        val points = stroke.points
        if (points.isEmpty()) return false

        // Quick AABB rejection
        val expandedBounds = Rect(
            left = stroke.bounds.left - thresholdRadius,
            top = stroke.bounds.top - thresholdRadius,
            right = stroke.bounds.right + thresholdRadius,
            bottom = stroke.bounds.bottom + thresholdRadius
        )
        if (!expandedBounds.contains(worldPoint)) {
            return false
        }

        if (points.size == 1) {
            val dx = worldPoint.x - points[0].x
            val dy = worldPoint.y - points[0].y
            return (dx * dx + dy * dy) <= thresholdSq
        }

        for (i in 0 until points.size - 1) {
            if (distanceToSegmentSquared(worldPoint, points[i], points[i + 1]) <= thresholdSq) {
                return true
            }
        }
        return false
    }
}
