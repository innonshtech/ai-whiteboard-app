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
    fun `standard eraser deletes a text object the circle touches`() {
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
        assertTrue(result.isEmpty())
    }

    @Test
    fun `standard eraser keeps text when eraser misses`() {
        val text = Stroke(
            points = listOf(Offset(10f, 10f)),
            strokeType = StrokeType.TEXT,
            text = "Hello"
        )
        val result = StrokeEraser.apply(
            strokes = listOf(text),
            center = Offset(200f, 200f),
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

    @Test
    fun `palm contact area calculation with 86-inch IFP panel DPI`() {
        val ifpDpi = 77.0f
        // Contact size of 120px x 90px on 77 DPI panel is ~9.2 cm² -> palm contact
        assertTrue(isPalmContactArea(120f, ifpDpi, 90f, ifpDpi))
        // Fingertip touch of 20px x 20px is ~0.34 cm² -> not a palm
        assertTrue(!isPalmContactArea(20f, ifpDpi, 20f, ifpDpi))
        // 0px touch size (unreported touch major) -> not a palm
        assertTrue(!isPalmContactArea(0f, ifpDpi, 0f, ifpDpi))
    }

    @Test
    fun `palm eraser mode recognizes supported palm contact without active stylus`() {
        val ifpDpi = 77.0f
        // Palm contact area > 3 cm² is valid for palm eraser even when stylus is NOT down
        assertTrue(isPalmContactArea(150f, ifpDpi, 100f, ifpDpi))
        // Fingertip is NOT a palm contact for palm eraser
        assertTrue(!isPalmContactArea(25f, ifpDpi, 25f, ifpDpi))
    }

    @Test
    fun `selection eraser affects selected strokes only and leaves unselected strokes intact`() {
        val selectedStroke = Stroke(
            id = "stroke_selected",
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            strokeWidth = 4f
        )
        val unselectedStroke = Stroke(
            id = "stroke_unselected",
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            strokeWidth = 4f
        )
        val strokes = listOf(selectedStroke, unselectedStroke)

        val (result, updatedSelection) = StrokeEraser.applySelection(
            strokes = strokes,
            selectedIds = setOf(selectedStroke.id),
            center = Offset(50f, 50f),
            radius = 10f,
            mode = EraserMode.STANDARD,
            restrictToSelection = true
        )

        // Unselected stroke must remain completely intact
        val preservedUnselected = result.firstOrNull { it.id == unselectedStroke.id }
        assertSame(unselectedStroke, preservedUnselected)

        // Selected stroke was split into 2 pieces
        val selectedPieces = result.filter { it.id != unselectedStroke.id }
        assertEquals(2, selectedPieces.size)
        assertEquals(selectedPieces.map { it.id }.toSet(), updatedSelection)
    }

    @Test
    fun `selection eraser preserves unselected shapes completely`() {
        val rectangle = Stroke(
            id = "shape_rect",
            points = listOf(Offset(20f, 20f), Offset(80f, 80f)),
            strokeType = StrokeType.RECTANGLE,
            strokeWidth = 4f
        )
        val circle = Stroke(
            id = "shape_circle",
            points = listOf(Offset(30f, 30f), Offset(70f, 70f)),
            strokeType = StrokeType.CIRCLE,
            strokeWidth = 4f
        )
        val text = Stroke(
            id = "shape_text",
            points = listOf(Offset(40f, 40f)),
            strokeType = StrokeType.TEXT,
            text = "Keep Me",
            strokeWidth = 2f
        )
        val selectedPen = Stroke(
            id = "pen_selected",
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            strokeType = StrokeType.PEN,
            strokeWidth = 4f
        )

        val allStrokes = listOf(rectangle, circle, text, selectedPen)

        val (result, updatedSelection) = StrokeEraser.applySelection(
            strokes = allStrokes,
            selectedIds = setOf(selectedPen.id),
            center = Offset(50f, 50f),
            radius = 25f,
            mode = EraserMode.SMART,
            restrictToSelection = true
        )

        // All shapes must be intact and present in the result
        assertTrue(result.any { it.id == rectangle.id })
        assertTrue(result.any { it.id == circle.id })
        assertTrue(result.any { it.id == text.id })

        // The selected pen stroke should be erased (in SMART mode it's completely deleted)
        assertTrue(result.none { it.id == selectedPen.id })
        assertTrue(updatedSelection.isEmpty())
    }

    @Test
    fun `selection eraser with empty selection does not erase anything`() {
        val stroke = Stroke(
            id = "s1",
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeWidth = 4f
        )
        val rect = Stroke(
            id = "r1",
            points = listOf(Offset(0f, 0f), Offset(100f, 100f)),
            strokeType = StrokeType.RECTANGLE
        )
        val strokes = listOf(stroke, rect)

        val (result, updatedSelection) = StrokeEraser.applySelection(
            strokes = strokes,
            selectedIds = emptySet(),
            center = Offset(50f, 0f),
            radius = 20f,
            mode = EraserMode.STANDARD,
            restrictToSelection = true
        )

        assertEquals(strokes, result)
        assertTrue(updatedSelection.isEmpty())
    }

    @Test
    fun `selection eraser disabled erases all intersecting strokes and shapes`() {
        val stroke = Stroke(
            id = "s1",
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeWidth = 4f
        )
        val rect = Stroke(
            id = "r1",
            points = listOf(Offset(0f, 0f), Offset(100f, 100f)),
            strokeType = StrokeType.RECTANGLE
        )
        val strokes = listOf(stroke, rect)

        val (result, _) = StrokeEraser.applySelection(
            strokes = strokes,
            selectedIds = emptySet(),
            center = Offset(50f, 0f),
            radius = 20f,
            mode = EraserMode.SMART,
            restrictToSelection = false
        )

        // Both s1 and r1 cross (50, 0) and will be hit in smart mode
        assertTrue(result.none { it.id == stroke.id })
    }

    @Test
    fun `smart palm eraser deletes whole intersecting strokes with palm radius while leaving other strokes intact`() {
        val hitStroke = Stroke(
            id = "hit_stroke",
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            strokeWidth = 4f
        )
        val farStroke = Stroke(
            id = "far_stroke",
            points = listOf(Offset(0f, 300f), Offset(100f, 300f)),
            strokeWidth = 4f
        )
        val strokes = listOf(hitStroke, farStroke)

        // Palm contact at (50, 50) with palm radius 35f
        val result = StrokeEraser.apply(
            strokes = strokes,
            center = Offset(50f, 50f),
            radius = 35f,
            mode = EraserMode.SMART
        )

        // The hit stroke should be completely deleted (whole stroke removed in SMART mode)
        assertTrue(result.none { it.id == hitStroke.id })
        // The far stroke remains untouched
        assertTrue(result.any { it.id == farStroke.id })
    }
}
