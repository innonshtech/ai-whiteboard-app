package com.avitrisphare.whiteboard.presentation.canvas

import com.avitrisphare.whiteboard.domain.model.Stroke
import kotlin.math.abs

enum class PaneKind { BOARD, PDF, VIDEO, WEB }

data class CanvasPane(
    val id: Int = 0,
    val kind: PaneKind = PaneKind.BOARD,
    val strokes: List<Stroke> = emptyList(),
    val source: String = "",
    val label: String = "",
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val tool: WhiteboardTool = WhiteboardTool.PEN
)

data class PaneFrame(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class DividerHit(val vertical: Boolean, val index: Int)

fun splitColumns(count: Int): Int = when (count) {
    3, 6 -> 3
    2, 4 -> 2
    else -> 1
}

fun splitRows(count: Int): Int = when (count) {
    4, 6 -> 2
    else -> 1
}

fun defaultWeights(count: Int): List<Float> {
    val columns = splitColumns(count)
    val rows = splitRows(count)
    return List(columns) { 1f / columns } + List(rows) { 1f / rows }
}

fun normalizeWeights(count: Int, weights: List<Float>): List<Float> {
    val columns = splitColumns(count)
    val rows = splitRows(count)
    val raw = if (weights.size == columns + rows) weights else defaultWeights(count)
    val cols = FloatArray(columns) { raw[it].coerceAtLeast(0.12f) }
    val rowValues = FloatArray(rows) { raw[columns + it].coerceAtLeast(0.12f) }
    val colSum = cols.sum().coerceAtLeast(0.01f)
    val rowSum = rowValues.sum().coerceAtLeast(0.01f)
    return List(columns + rows) { index ->
        if (index < columns) cols[index] / colSum else rowValues[index - columns] / rowSum
    }
}

fun paneFrames(count: Int, width: Int, height: Int, weights: List<Float>): List<PaneFrame> {
    if (count <= 1 || width <= 0 || height <= 0) return listOf(PaneFrame(0, 0, width, height))
    val columns = splitColumns(count)
    val rows = splitRows(count)
    val sized = normalizeWeights(count, weights)
    val frames = ArrayList<PaneFrame>(count)
    var top = 0
    for (row in 0 until rows) {
        val rowHeight = if (row == rows - 1) height - top else (height * sized[columns + row]).toInt().coerceAtLeast(1)
        var left = 0
        for (column in 0 until columns) {
            val columnWidth = if (column == columns - 1) width - left else (width * sized[column]).toInt().coerceAtLeast(1)
            frames.add(PaneFrame(left, top, left + columnWidth, top + rowHeight))
            left += columnWidth
        }
        top += rowHeight
    }
    return frames
}

fun hitPane(frames: List<PaneFrame>, x: Float, y: Float): Int {
    return frames.indexOfFirst { x >= it.left && x < it.right && y >= it.top && y < it.bottom }
}

fun hitDivider(frames: List<PaneFrame>, count: Int, x: Float, y: Float, slop: Float = 18f): DividerHit? {
    if (frames.size < 2) return null
    val columns = splitColumns(count)
    val rows = splitRows(count)
    if (columns > 1) {
        for (column in 0 until columns - 1) {
            val edge = frames[column].right.toFloat()
            if (abs(x - edge) <= slop) return DividerHit(vertical = true, index = column)
        }
    }
    if (rows > 1) {
        val edge = frames[columns].top.toFloat()
        if (abs(y - edge) <= slop) return DividerHit(vertical = false, index = 0)
    }
    return null
}

fun dragDivider(
    weights: List<Float>,
    count: Int,
    vertical: Boolean,
    index: Int,
    deltaPx: Float,
    spanPx: Float
): List<Float> {
    if (spanPx <= 1f) return normalizeWeights(count, weights)
    val current = normalizeWeights(count, weights).toMutableList()
    val columns = splitColumns(count)
    val start = if (vertical) index else columns + index
    if (start < 0 || start + 1 >= current.size) return current
    val change = deltaPx / spanPx
    val nextA = current[start] + change
    val nextB = current[start + 1] - change
    if (nextA < 0.12f || nextB < 0.12f) return current
    current[start] = nextA
    current[start + 1] = nextB
    return normalizeWeights(count, current)
}

fun replacePaneStrokes(panes: List<CanvasPane>, index: Int, strokes: List<Stroke>): List<CanvasPane> {
    return panes.mapIndexed { paneIndex, pane ->
        if (paneIndex == index) pane.copy(strokes = strokes) else pane
    }
}
