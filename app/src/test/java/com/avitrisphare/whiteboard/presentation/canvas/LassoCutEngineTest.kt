package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoCutEngineTest {

    @Test
    fun testPointInPolygon() {
        val square = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f)
        )
        assertTrue(LassoCutEngine.pointInPolygon(Offset(50f, 50f), square))
        assertFalse(LassoCutEngine.pointInPolygon(Offset(150f, 50f), square))
        assertFalse(LassoCutEngine.pointInPolygon(Offset(-10f, 50f), square))
    }

    @Test
    fun testSplitPolylineCrossingLasso() {
        // Line from (0, 50) to (100, 50)
        val line = listOf(Offset(0f, 50f), Offset(100f, 50f))
        val squareLasso = listOf(
            Offset(25f, 25f),
            Offset(75f, 25f),
            Offset(75f, 75f),
            Offset(25f, 75f)
        )
        val split = LassoCutEngine.splitPolyline(line, emptyList(), squareLasso)

        assertEquals(1, split.insidePieces.size)
        assertEquals(2, split.outsidePieces.size)

        // Inside piece should start at x=25 and end at x=75
        val inside = split.insidePieces[0].points
        assertEquals(2, inside.size)
        assertEquals(25f, inside.first().x, 0.01f)
        assertEquals(50f, inside.first().y, 0.01f)
        assertEquals(75f, inside.last().x, 0.01f)
        assertEquals(50f, inside.last().y, 0.01f)

        // Outside piece 1: (0, 50) to (25, 50)
        val out1 = split.outsidePieces[0].points
        assertEquals(0f, out1.first().x, 0.01f)
        assertEquals(25f, out1.last().x, 0.01f)

        // Outside piece 2: (75, 50) to (100, 50)
        val out2 = split.outsidePieces[1].points
        assertEquals(75f, out2.first().x, 0.01f)
        assertEquals(100f, out2.last().x, 0.01f)
    }

    @Test
    fun testCutAndSelectCompletelyInside() {
        val stroke = Stroke(
            id = "s1",
            points = listOf(Offset(40f, 40f), Offset(60f, 60f)),
            colorValue = 0xFFFF0000.toInt()
        )
        val lasso = listOf(
            Offset(10f, 10f),
            Offset(90f, 10f),
            Offset(90f, 90f),
            Offset(10f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertFalse(result.didCut)
        assertEquals(1, result.strokes.size)
        assertEquals("s1", result.strokes[0].id)
        assertEquals(setOf("s1"), result.selectedIds)
    }

    @Test
    fun testCutAndSelectCompletelyOutside() {
        val stroke = Stroke(
            id = "s1",
            points = listOf(Offset(200f, 200f), Offset(250f, 250f)),
            colorValue = 0xFFFF0000.toInt()
        )
        val lasso = listOf(
            Offset(10f, 10f),
            Offset(90f, 10f),
            Offset(90f, 90f),
            Offset(10f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertFalse(result.didCut)
        assertEquals(1, result.strokes.size)
        assertEquals("s1", result.strokes[0].id)
        assertTrue(result.selectedIds.isEmpty())
    }

    @Test
    fun testCutAndSelectPartiallyInsideCutsAndSelectsInsidePiece() {
        val stroke = Stroke(
            id = "original-stroke",
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            colorValue = 0xFFFF0000.toInt()
        )
        val lasso = listOf(
            Offset(30f, 10f),
            Offset(70f, 10f),
            Offset(70f, 90f),
            Offset(30f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertTrue(result.didCut)
        // Original stroke was split into 2 outside pieces + 1 inside piece = 3 strokes total
        assertEquals(3, result.strokes.size)
        assertEquals(1, result.selectedIds.size)

        val selectedId = result.selectedIds.first()
        val selectedStroke = result.strokes.find { it.id == selectedId }
        assertNotNull(selectedStroke)
        assertEquals(0xFFFF0000.toInt(), selectedStroke!!.colorValue)
        assertEquals(30f, selectedStroke.points.first().x, 0.05f)
        assertEquals(70f, selectedStroke.points.last().x, 0.05f)
    }

    @Test
    fun testCutAndSelectShapeRectangle() {
        val rectStroke = Stroke(
            id = "rect-1",
            points = listOf(Offset(0f, 0f), Offset(100f, 100f)),
            strokeType = StrokeType.RECTANGLE,
            colorValue = 0xFFFFFF00.toInt()
        )
        // Lasso enclosing top-right corner of the rectangle: (80, -20) to (120, 40)
        val lasso = listOf(
            Offset(80f, -20f),
            Offset(120f, -20f),
            Offset(120f, 40f),
            Offset(80f, 40f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(rectStroke), lasso)
        assertTrue(result.didCut)
        assertTrue(result.selectedIds.isNotEmpty())
        // The cut-out corner piece is selected and has yellow color
        val selected = result.strokes.filter { it.id in result.selectedIds }
        assertTrue(selected.isNotEmpty())
        assertEquals(0xFFFFFF00.toInt(), selected[0].colorValue)
    }

    @Test
    fun testCutAndSelectLoopStrokeLikeImage1() {
        // A stroke that starts outside, loops into the lasso, and exits outside
        val stroke = Stroke(
            id = "loop-stroke",
            points = listOf(
                Offset(10f, 10f),
                Offset(50f, 50f),
                Offset(60f, 70f),
                Offset(50f, 80f),
                Offset(10f, 100f)
            ),
            colorValue = 0xFFFF0000.toInt()
        )
        // Lasso bounding box around the loop at y=35..90
        val lasso = listOf(
            Offset(30f, 35f),
            Offset(80f, 35f),
            Offset(80f, 90f),
            Offset(30f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertTrue(result.didCut)
        assertEquals(1, result.selectedIds.size)

        val selectedStroke = result.strokes.find { it.id in result.selectedIds }
        assertNotNull(selectedStroke)
        // Selected loop piece is inside the lasso
        assertTrue(selectedStroke!!.points.all { it.y >= 34.9f && it.y <= 90.1f })
    }
}
