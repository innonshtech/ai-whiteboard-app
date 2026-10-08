package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke

/**
 * Keeps the finished page on one bitmap. While a stroke is being drawn, each
 * frame blits that bitmap and paints only the new ink.
 */
class InkBoardCache {
    private var image: ImageBitmap? = null
    private var cachedStrokes: List<Stroke>? = null
    private var cachedScale = Float.NaN
    private var cachedOffsetX = Float.NaN
    private var cachedOffsetY = Float.NaN
    private var cachedBackground: CanvasBackground? = null
    private var cachedShowGrid = false
    private var cachedDark = false
    private var cachedSelection: Set<String>? = null
    private var cachedWidth = 0
    private var cachedHeight = 0

    fun draw(
        host: DrawScope,
        strokes: List<Stroke>,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        background: CanvasBackground,
        showGrid: Boolean,
        dark: Boolean,
        selection: Set<String>,
        block: DrawScope.() -> Unit
    ) {
        val width = host.size.width.toInt().coerceAtLeast(1)
        val height = host.size.height.toInt().coerceAtLeast(1)
        val hit = image != null &&
            cachedStrokes === strokes &&
            cachedScale == scale &&
            cachedOffsetX == offsetX &&
            cachedOffsetY == offsetY &&
            cachedBackground == background &&
            cachedShowGrid == showGrid &&
            cachedDark == dark &&
            cachedSelection === selection &&
            cachedWidth == width &&
            cachedHeight == height
        if (!hit) {
            val bitmap = if (image != null && cachedWidth == width && cachedHeight == height) {
                image!!
            } else {
                ImageBitmap(width, height)
            }
            image = bitmap
            CanvasDrawScope().draw(
                density = host,
                layoutDirection = host.layoutDirection,
                canvas = Canvas(bitmap),
                size = Size(width.toFloat(), height.toFloat()),
                block = block
            )
            cachedStrokes = strokes
            cachedScale = scale
            cachedOffsetX = offsetX
            cachedOffsetY = offsetY
            cachedBackground = background
            cachedShowGrid = showGrid
            cachedDark = dark
            cachedSelection = selection
            cachedWidth = width
            cachedHeight = height
        }
        host.drawImage(image!!)
    }
}
