package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasInkThemeTest {

    @Test
    fun flipsDarkInkWhenSwitchingToDarkSurface() {
        val black = Color.Black.toArgb()
        val mapped = CanvasInkTheme.penColorForTheme(black, toDark = true)
        assertEquals(CanvasInkTheme.darkSurfaceInkArgb, mapped)
    }

    @Test
    fun flipsWhiteInkWhenSwitchingToLightSurface() {
        val white = Color.White.toArgb()
        val mapped = CanvasInkTheme.penColorForTheme(white, toDark = false)
        assertEquals(CanvasInkTheme.lightSurfaceInkArgb, mapped)
    }

    @Test
    fun keepsMidToneColors() {
        val green = Color(0xFF3ECF8E).toArgb()
        assertEquals(green, CanvasInkTheme.penColorForTheme(green, toDark = true))
        assertEquals(green, CanvasInkTheme.penColorForTheme(green, toDark = false))
    }

    @Test
    fun detectsDarkAndLightInk() {
        assertTrue(CanvasInkTheme.isDarkInk(Color.Black.toArgb()))
        assertTrue(CanvasInkTheme.isLightInk(Color.White.toArgb()))
    }
}
