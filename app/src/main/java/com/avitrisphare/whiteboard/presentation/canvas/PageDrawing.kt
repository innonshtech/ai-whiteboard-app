package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import kotlin.math.ceil
import kotlin.math.floor

fun DrawScope.drawPageGrid(
    background: CanvasBackground,
    showGrid: Boolean,
    dark: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    when (background) {
        CanvasBackground.FOOTBALL -> drawFootballPitch()
        CanvasBackground.DARK -> drawRect(Color(0xFF171717), size = size)
        CanvasBackground.GRAPH -> drawRect(Color(0xFFF7FBFF), size = size)
        else -> drawRect(Color.White, size = size)
    }
    val viewport = CanvasTransform.calculateViewportWorldBounds(
        viewportSize = size,
        scale = scale,
        offset = Offset(offsetX, offsetY)
    )
    inPageSpace(offsetX, offsetY, scale) {
        val gridColor = if (background == CanvasBackground.DARK) Color(0xFF2E2E2E) else Color(0xFFD5D8DE)
        val spacing = CanvasTransform.getAdaptiveGridSpacing(80f, scale)
        when (background) {
            CanvasBackground.LINED -> horizontalLines(viewport, spacing, gridColor, scale)
            CanvasBackground.DOTTED -> dots(viewport, spacing, gridColor, scale)
            CanvasBackground.GRID -> {
                verticalLines(viewport, spacing, gridColor, scale)
                horizontalLines(viewport, spacing, gridColor, scale)
            }
            CanvasBackground.GRAPH -> {
                val minor = spacing / 5f
                verticalLines(viewport, minor, Color(0xFFD3E3F4), scale)
                horizontalLines(viewport, minor, Color(0xFFD3E3F4), scale)
                verticalLines(viewport, spacing, Color(0xFF6B93C0), scale, thicknessScale = 1.8f)
                horizontalLines(viewport, spacing, Color(0xFF6B93C0), scale, thicknessScale = 1.8f)
            }
            else -> Unit
        }
        if (showGrid && background != CanvasBackground.GRID && background != CanvasBackground.GRAPH) {
            val overlay = if (dark) Color(0xFF3A3A3A) else Color(0xFFC5C8D0)
            verticalLines(viewport, spacing, overlay, scale)
            horizontalLines(viewport, spacing, overlay, scale)
        }
    }
}

fun DrawScope.drawVectorInk(
    context: Context,
    strokes: List<Stroke>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    selectedIds: Set<String>
) {
    val viewport = CanvasTransform.calculateViewportWorldBounds(
        viewportSize = size,
        scale = scale,
        offset = Offset(offsetX, offsetY)
    )
    inPageSpace(offsetX, offsetY, scale) {
        val selectedStrokes = ArrayList<Stroke>()
        for (stroke in strokes) {
            if (stroke.isPlacedMedia()) continue
            if (stroke.id in selectedIds) {
                selectedStrokes.add(stroke)
            } else if (stroke.bounds.overlaps(viewport)) {
                renderStroke(context, stroke, scale, false)
            }
        }
        if (selectedStrokes.isNotEmpty()) drawSelectionBox(selectedStrokes)
        for (stroke in selectedStrokes) {
            renderStroke(context, stroke, scale, isEditableShape(stroke.strokeType))
        }
        for (stroke in strokes) {
            if (stroke.isPlacedMedia() && stroke.id in selectedIds) {
                renderStroke(context, stroke, scale, true)
            }
        }
    }
}

private fun DrawScope.drawSelectionBox(strokes: List<Stroke>) {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    strokes.forEach { stroke ->
        val bounds = strokeSelectionBounds(stroke)
        left = minOf(left, bounds.left)
        top = minOf(top, bounds.top)
        right = maxOf(right, bounds.right)
        bottom = maxOf(bottom, bounds.bottom)
    }
    val pad = 16f
    drawRoundRect(
        color = Color(0x738A8870),
        topLeft = Offset(left - pad, top - pad),
        size = Size(right - left + pad * 2f, bottom - top + pad * 2f),
        cornerRadius = CornerRadius(12f, 12f)
    )
}

fun DrawScope.inPageSpace(
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    block: DrawScope.() -> Unit
) {
    withTransform({
        translate(offsetX, offsetY)
        scale(scale, scale, pivot = Offset.Zero)
    }, block)
}

private fun DrawScope.horizontalLines(
    viewport: androidx.compose.ui.geometry.Rect,
    spacing: Float,
    color: Color,
    scale: Float,
    thicknessScale: Float = 1f
) {
    val startY = floor(viewport.top / spacing) * spacing
    val endY = ceil(viewport.bottom / spacing) * spacing
    val thickness = thicknessScale / scale.coerceAtLeast(0.5f)
    var y = startY
    while (y <= endY) {
        drawLine(color, Offset(viewport.left, y), Offset(viewport.right, y), thickness)
        y += spacing
    }
}

private fun DrawScope.verticalLines(
    viewport: androidx.compose.ui.geometry.Rect,
    spacing: Float,
    color: Color,
    scale: Float,
    thicknessScale: Float = 1f
) {
    val startX = floor(viewport.left / spacing) * spacing
    val endX = ceil(viewport.right / spacing) * spacing
    val thickness = thicknessScale / scale.coerceAtLeast(0.5f)
    var x = startX
    while (x <= endX) {
        drawLine(color, Offset(x, viewport.top), Offset(x, viewport.bottom), thickness)
        x += spacing
    }
}

private fun DrawScope.dots(
    viewport: androidx.compose.ui.geometry.Rect,
    spacing: Float,
    color: Color,
    scale: Float
) {
    val startX = floor(viewport.left / spacing) * spacing
    val endX = ceil(viewport.right / spacing) * spacing
    val startY = floor(viewport.top / spacing) * spacing
    val endY = ceil(viewport.bottom / spacing) * spacing
    val radius = 1.25f / scale.coerceAtLeast(0.5f)
    var y = startY
    while (y <= endY) {
        var x = startX
        while (x <= endX) {
            drawCircle(color, radius, Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}
