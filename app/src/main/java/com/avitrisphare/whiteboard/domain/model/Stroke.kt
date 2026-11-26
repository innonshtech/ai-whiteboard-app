package com.avitrisphare.whiteboard.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
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
    val opacity: Float = 1f,
    val cornerRadius: Float = 0f,
    val showDimensions: Boolean = false,
    /** When non-empty, this stroke is anchored to another stroke (flowchart / mind map). */
    val linkToId: String = "",
    val linkFromId: String = "",
    /** Bundled sticker id or emoji payload. */
    val stickerKind: String = ""
) {
    @Transient
    private var _cachedSmoothedPath: Path? = null

        /**
     * Reusable path through the captured samples.
     * Prevents re-allocating Path objects on every render frame.
     */
    val cachedPath: Path
        get() {
            var p = _cachedSmoothedPath
            if (p == null) {
                p = if (strokeType == StrokeType.LINE || strokeType == StrokeType.LINE_DASHED) {
                    createStraightPath(points)
                } else {
                    createSmoothedPath(points)
                }
                _cachedSmoothedPath = p
            }
            return p
        }

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
            val size = if (strokeWidth in TextStrokeConfig.MIN_FONT_SIZE..TextStrokeConfig.MAX_FONT_SIZE) strokeWidth else TextStrokeConfig.DEFAULT_FONT_SIZE
            val lines = text.ifBlank { " " }.split("\n")
            val widest = lines.maxOf { it.length }.coerceAtLeast(1)
            val width = (widest * size * TextStrokeConfig.CHAR_WIDTH_RATIO).coerceAtLeast(size * TextStrokeConfig.MIN_WIDTH_FACTOR)
            val height = (lines.size * size * TextStrokeConfig.LINE_HEIGHT_RATIO).coerceAtLeast(size * TextStrokeConfig.MIN_HEIGHT_FACTOR)
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

    companion object {
        fun createStraightPath(points: List<Offset>): Path {
            val path = Path()
            if (points.size < 2) {
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                }
                return path
            }
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            return path
        }

        fun createSmoothedPath(points: List<Offset>): Path {
            val path = Path()
            if (points.isEmpty()) return path
            path.moveTo(points[0].x, points[0].y)
            if (points.size == 1) return path
            // Follow captured samples. Midpoint-quad smoothing pulled letters
            // off the true stroke and bent corners of handwriting.
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            return path
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
    // 2D Shapes
    ROUNDED_RECTANGLE,
    ROUND_RECT,
    SEMICIRCLE,
    RIGHT_TRIANGLE,
    SCALENE_TRIANGLE,
    TRAPEZOID,
    PARALLELOGRAM,
    RHOMBUS,
    PENTAGON,
    HEXAGON,
    OCTAGON,
    // Lines & Rays
    LINE_DASHED,
    DOUBLE_ARROW,
    DASHED_ARROW,
    ANGLE_RAY,
    // 3D Solids
    CUBE,
    CUBOID,
    PYRAMID,
    PRISM,
    CYLINDER,
    CONE,
    SPHERE,
    STICKER,
    CONNECTOR
}

object TextStrokeConfig {
    const val MIN_FONT_SIZE = 12f
    const val MAX_FONT_SIZE = 120f
    const val DEFAULT_FONT_SIZE = 20f
    const val FONT_STEP = 2f
    const val LINE_HEIGHT_RATIO = 1.35f
    const val CHAR_WIDTH_RATIO = 0.65f
    const val MIN_WIDTH_FACTOR = 2f
    const val MIN_HEIGHT_FACTOR = 1.5f
}
