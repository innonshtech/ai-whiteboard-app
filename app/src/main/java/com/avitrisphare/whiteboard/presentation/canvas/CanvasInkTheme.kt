package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia

/** Keeps pen and committed ink readable when the board switches light/dark. */
object CanvasInkTheme {
    val lightSurfaceInkArgb = 0xFF1B1F2A.toInt()
    val darkSurfaceInkArgb = Color.White.toArgb()

    fun relativeLuminance(argb: Int): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun isDarkInk(argb: Int): Boolean = relativeLuminance(argb) < 0.28

    fun isLightInk(argb: Int): Boolean = relativeLuminance(argb) > 0.72

    fun penColorForTheme(current: Int, toDark: Boolean): Int = when {
        toDark && isDarkInk(current) -> darkSurfaceInkArgb
        !toDark && isLightInk(current) -> lightSurfaceInkArgb
        else -> current
    }

    fun remapStrokesForTheme(strokes: List<Stroke>, toDark: Boolean): List<Stroke> =
        strokes.map { stroke ->
            if (stroke.isPlacedMedia()) return@map stroke
            val mapped = penColorForTheme(stroke.colorValue, toDark)
            if (mapped == stroke.colorValue) stroke else stroke.copy(colorValue = mapped)
        }
}
