package com.avitrisphare.whiteboard.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import java.util.UUID

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Offset> = emptyList(),
    val colorValue: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val strokeType: StrokeType = StrokeType.PEN,
    val text: String = "",
    val fillColor: Int = 0,
    val imageUri: String = "",
    val pointPressures: List<Float> = emptyList(),
    /** When non-empty, this stroke is anchored to another stroke (flowchart / mind map). */
    val linkToId: String = "",
    val linkFromId: String = "",
    /** Bundled sticker id or emoji payload. */
    val stickerKind: String = ""
) {
    val bounds: Rect by lazy {
        if (points.isEmpty()) {
            Rect.Zero
        } else if (strokeType == StrokeType.STICKER) {
            val origin = points.first()
            val size = (strokeWidth * 12f).coerceIn(64f, 160f)
            Rect(origin.x, origin.y, origin.x + size, origin.y + size)
        } else if (strokeType == StrokeType.STICKY_NOTE) {
            val origin = points.first()
            Rect(origin.x, origin.y, origin.x + 220f, origin.y + 180f)
        } else if (strokeType == StrokeType.EQUATION && points.size >= 2) {
            val p1 = points.first()
            val p2 = points.last()
            Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))
        } else if (strokeType == StrokeType.TEXT) {
            val origin = points.first()
            val size = if (strokeWidth >= 12f) strokeWidth else 20f
            val lines = text.ifBlank { " " }.split("\n")
            val widest = lines.maxOf { it.length }.coerceAtLeast(1)
            val width = (widest * size * 0.62f).coerceAtLeast(size * 2f)
            val height = lines.size * size * 1.3f
            Rect(origin.x, origin.y, origin.x + width, origin.y + height)
        } else if ((strokeType == StrokeType.IMAGE || strokeType == StrokeType.VIDEO || strokeType == StrokeType.WEB_EMBED) && points.size >= 2) {
            val p1 = points.first()
            val p2 = points.last()
            Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))
        } else if (strokeType == StrokeType.WEB_EMBED || strokeType == StrokeType.VIDEO) {
            val origin = points.first()
            val w = if (strokeType == StrokeType.VIDEO) 640f else 780f
            val h = if (strokeType == StrokeType.VIDEO) 360f else 480f
            Rect(origin.x, origin.y, origin.x + w, origin.y + h)
        } else if (strokeType == StrokeType.FRAME && points.size >= 2) {
            val p1 = points.first()
            val p2 = points.last()
            Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))
        } else {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            val halfW = strokeWidth / 2f
            Rect(minX - halfW, minY - halfW, maxX + halfW, maxY + halfW)
        }
    }
}

fun Stroke.isPlacedMedia(): Boolean {
    return strokeType == StrokeType.IMAGE ||
        strokeType == StrokeType.VIDEO ||
        strokeType == StrokeType.WEB_EMBED
}

enum class StrokeType {
    PEN,
    PENCIL,
    MARKER,
    HIGHLIGHTER,
    BRUSH,
    LINE,
    ARROW,
    RECTANGLE,
    DIAMOND,
    CIRCLE,
    TEXT,
    STICKY_NOTE,
    FRAME,
    WEB_EMBED,
    IMAGE,
    VIDEO,
    ELLIPSE,
    SQUARE,
    TRIANGLE,
    STAR,
    EQUATION,
    HEXAGON,
    ROUND_RECT,
    CUBE,
    CYLINDER,
    CONE,
    PYRAMID,
    STICKER,
    CONNECTOR
}