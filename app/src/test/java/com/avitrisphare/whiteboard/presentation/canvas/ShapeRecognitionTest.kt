package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ShapeRecognitionTest {

    @Test
    fun recognisesTheTeachingShapes() {
        check(StrokeType.CIRCLE, circle(0f, 0f, 80f))
        check(StrokeType.ELLIPSE, ellipse(0f, 0f, 120f, 50f))
        check(StrokeType.RECTANGLE, polygon(rect()))
        check(StrokeType.SQUARE, polygon(square()))
        check(StrokeType.TRIANGLE, polygon(triangle()))
        check(StrokeType.LINE, line())
        check(StrokeType.ARROW, arrow())
        check(StrokeType.STAR, star())
        check(StrokeType.DIAMOND, polygon(diamond()))
    }

    @Test
    fun convertedShapeCanBeResizedRecolouredAndMoved() {
        val box = Stroke(
            points = listOf(Offset(0f, 0f), Offset(100f, 80f)),
            strokeType = StrokeType.RECTANGLE,
            colorValue = 1
        )
        val handle = hitShapeHandle(box, Offset(100f, 80f), slop = 12f)
        assertNotNull(handle)
        val resized = resizeShape(box, handle!!, Offset(140f, 110f))
        assertEquals(140f, resized.points[1].x, 0.1f)
        assertEquals(110f, resized.points[1].y, 0.1f)
        val moved = resized.copy(
            points = resized.points.map { Offset(it.x + 5f, it.y + 7f) }
        )
        assertEquals(5f, moved.points[0].x, 0.1f)
        assertEquals(0xFF112233.toInt(), moved.copy(colorValue = 0xFF112233.toInt()).colorValue)
        val square = Stroke(
            points = listOf(Offset(0f, 0f), Offset(40f, 40f)),
            strokeType = StrokeType.SQUARE
        )
        val grown = resizeShape(square, ShapeHandleHit(ShapeHandle.BOTTOM_RIGHT), Offset(80f, 50f))
        val width = kotlin.math.abs(grown.points[1].x - grown.points[0].x)
        val height = kotlin.math.abs(grown.points[1].y - grown.points[0].y)
        assertEquals(width, height, 0.1f)
    }

    @Test
    fun promptWaitsThenConverts() {
        assertEquals(500L, SHAPE_PROMPT_DELAY_MS)
        assertEquals(2000L, SHAPE_AUTO_CONVERT_MS)
    }

    private fun check(expected: StrokeType, points: List<Offset>) {
        val result = ShapeRecognitionHelper.recognize(points)
        assertEquals(expected, result?.strokeType)
        assertTrue(result!!.confidence >= 0.7f)
    }

    private fun circle(cx: Float, cy: Float, radius: Float): List<Offset> {
        return (0..48).map { index ->
            val angle = index / 48f * (Math.PI * 2).toFloat()
            Offset(cx + radius * cos(angle), cy + radius * sin(angle))
        }
    }

    private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float): List<Offset> {
        return (0..48).map { index ->
            val angle = index / 48f * (Math.PI * 2).toFloat()
            Offset(cx + rx * cos(angle), cy + ry * sin(angle))
        }
    }

    private fun polygon(vertices: List<Offset>): List<Offset> {
        val points = ArrayList<Offset>()
        val loop = vertices + vertices.first()
        for (index in 0 until loop.lastIndex) {
            val from = loop[index]
            val to = loop[index + 1]
            val steps = 12
            for (step in 0 until steps) {
                val t = step / steps.toFloat()
                points.add(Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t))
            }
        }
        points.add(vertices.first())
        return points
    }

    private fun rect() = listOf(Offset(10f, 20f), Offset(180f, 20f), Offset(180f, 90f), Offset(10f, 90f))
    private fun square() = listOf(Offset(10f, 10f), Offset(110f, 10f), Offset(110f, 110f), Offset(10f, 110f))
    private fun triangle() = listOf(Offset(80f, 10f), Offset(150f, 120f), Offset(10f, 120f))
    private fun diamond() = listOf(Offset(80f, 10f), Offset(140f, 70f), Offset(80f, 130f), Offset(20f, 70f))

    private fun line(): List<Offset> {
        return (0..20).map { index ->
            val t = index / 20f
            Offset(t * 200f, 8f * sin(t * 6f))
        }
    }

    private fun arrow(): List<Offset> {
        val shaft = (0..16).map { Offset(it * 12f, 0f) }
        val tip = Offset(192f, 0f)
        return shaft + tip + Offset(164f, -28f) + tip + Offset(164f, 28f)
    }

    private fun star(): List<Offset> {
        val outer = (0 until 5).map { index ->
            val turn = -Math.PI / 2.0 + index * 2.0 * Math.PI / 5.0
            Offset((100 + 70 * cos(turn)).toFloat(), (100 + 70 * sin(turn)).toFloat())
        }
        return polygon(listOf(0, 2, 4, 1, 3).map { outer[it] })
    }
}
