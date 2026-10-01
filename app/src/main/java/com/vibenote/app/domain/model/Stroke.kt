package com.vibenote.app.domain.model

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
    val imageUri: String = ""
) {
    val bounds: Rect by lazy {
        if (points.isEmpty()) {
            Rect.Zero
        } else if (strokeType == StrokeType.STICKY_NOTE || strokeType == StrokeType.TEXT || strokeType == StrokeType.WEB_EMBED) {
            val origin = points.first()
            val w = when (strokeType) {
                StrokeType.STICKY_NOTE -> 220f
                StrokeType.WEB_EMBED -> 280f
                else -> (text.length * 12f).coerceIn(120f, 400f)
            }
            val h = when (strokeType) {
                StrokeType.STICKY_NOTE -> 180f
                StrokeType.WEB_EMBED -> 140f
                else -> 60f
            }
            Rect(origin.x, origin.y, origin.x + w, origin.y + h)
        } else if (strokeType == StrokeType.IMAGE && points.size >= 2) {
            val p1 = points.first()
            val p2 = points.last()
            Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))
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

enum class StrokeType {
    PEN,
    HIGHLIGHTER,
    LINE,
    ARROW,
    RECTANGLE,
    DIAMOND,
    CIRCLE,
    TEXT,
    STICKY_NOTE,
    FRAME,
    WEB_EMBED,
    IMAGE
}