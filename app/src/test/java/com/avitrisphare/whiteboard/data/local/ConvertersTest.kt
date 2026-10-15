package com.avitrisphare.whiteboard.data.local

import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromCanvasBackground converts correctly`() {
        assertEquals("dark", converters.fromCanvasBackground(CanvasBackground.DARK))
        assertEquals("white", converters.fromCanvasBackground(CanvasBackground.WHITE))
        assertEquals("lined", converters.fromCanvasBackground(CanvasBackground.LINED))
        assertEquals("dotted", converters.fromCanvasBackground(CanvasBackground.DOTTED))
        assertEquals("grid", converters.fromCanvasBackground(CanvasBackground.GRID))
        assertEquals("graph", converters.fromCanvasBackground(CanvasBackground.GRAPH))
    }

    @Test
    fun `toCanvasBackground converts correctly`() {
        assertEquals(CanvasBackground.DARK, converters.toCanvasBackground("dark"))
        assertEquals(CanvasBackground.WHITE, converters.toCanvasBackground("white"))
        assertEquals(CanvasBackground.LINED, converters.toCanvasBackground("lined"))
        assertEquals(CanvasBackground.DOTTED, converters.toCanvasBackground("dotted"))
        assertEquals(CanvasBackground.GRID, converters.toCanvasBackground("grid"))
        assertEquals(CanvasBackground.GRAPH, converters.toCanvasBackground("graph"))
    }

    @Test
    fun `toCanvasBackground handles invalid key`() {
        assertEquals(CanvasBackground.DARK, converters.toCanvasBackground("invalid"))
    }

    @Test
    fun `round trip conversion preserves value`() {
        val backgrounds = listOf(
            CanvasBackground.DARK,
            CanvasBackground.WHITE,
            CanvasBackground.LINED,
            CanvasBackground.DOTTED,
            CanvasBackground.GRID,
            CanvasBackground.GRAPH
        )
        
        backgrounds.forEach { background ->
            val key = converters.fromCanvasBackground(background)
            val converted = converters.toCanvasBackground(key)
            assertEquals(background, converted)
        }
    }
}
