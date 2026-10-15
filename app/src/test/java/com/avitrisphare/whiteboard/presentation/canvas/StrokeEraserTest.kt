package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeEraserTest {

    @Test
    fun `standard eraser splits a stroke and leaves the ends`() {
        val stroke = Stroke(
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeWidth = 4f
        )
        val result = StrokeEraser.apply(
            strokes = listOf(stroke),
            center = Offset(50f, 0f),
            radius = 8f,
            mode = EraserMode.STANDARD
        )
        assertEquals(2, result.size)
        assertTrue(result[0].points.last().x < 50f)
        assertTrue(result[1].points.first().x > 50f)
        assertEquals(stroke.colorValue, result[0].colorValue)
    }

    @Test
    fun `smart eraser removes the whole stroke and keeps the rest`() {
        val hit = Stroke(points = listOf(Offset(0f, 0f), Offset(100f, 0f)), strokeWidth = 4f)
        val miss = Stroke(points = listOf(Offset(0f, 400f), Offset(100f, 400f)), strokeWidth = 4f)
        val result = StrokeEraser.apply(
            strokes = listOf(hit, miss),
            center = Offset(50f, 0f),
            radius = 8f,
            mode = EraserMode.SMART
        )
        assertEquals(1, result.size)
        assertSame(miss, result[0])
    }

    @Test
    fun `standard eraser does not delete a text object`() {
        val text = Stroke(
            points = listOf(Offset(10f, 10f)),
            strokeType = StrokeType.TEXT,
            text = "Hello"
        )
        val result = StrokeEraser.apply(
            strokes = listOf(text),
            center = Offset(20f, 20f),
            radius = 30f,
            mode = EraserMode.STANDARD
        )
        assertSame(text, result.single())
    }

    @Test
    fun `smart eraser removes a text object the circle touches`() {
        val text = Stroke(
            points = listOf(Offset(10f, 10f)),
            strokeType = StrokeType.TEXT,
            text = "Hello"
        )
        val result = StrokeEraser.apply(
            strokes = listOf(text),
            center = Offset(20f, 20f),
            radius = 30f,
            mode = EraserMode.SMART
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `palm larger than 3 square centimetres is ignored only while a stylus is active`() {
        val xdpi = 160f
        assertTrue(isPalmContact(124f, xdpi, stylusActive = true, isStylus = false))
        assertTrue(!isPalmContact(100f, xdpi, stylusActive = true, isStylus = false))
        assertTrue(!isPalmContact(0f, xdpi, stylusActive = true, isStylus = false))
        assertTrue(!isPalmContact(400f, xdpi, stylusActive = false, isStylus = false))
        assertTrue(!isPalmContact(400f, xdpi, stylusActive = true, isStylus = true))
    }
}
