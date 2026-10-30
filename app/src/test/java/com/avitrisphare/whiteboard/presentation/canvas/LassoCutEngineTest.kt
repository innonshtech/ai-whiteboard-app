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

    // ────────────────────────────────────────────────────────────────────────────
    // Geometry helpers
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // 1. Fully enclosed ink stroke — selection only, no cut
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // 2. Multiple enclosed strokes — all selected, none cut
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testCutAndSelectMultipleEnclosedStrokes() {
        val s1 = Stroke(id = "s1", points = listOf(Offset(20f, 20f), Offset(40f, 40f)))
        val s2 = Stroke(id = "s2", points = listOf(Offset(50f, 50f), Offset(70f, 70f)))
        val lasso = listOf(
            Offset(10f, 10f),
            Offset(90f, 10f),
            Offset(90f, 90f),
            Offset(10f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(s1, s2), lasso)
        assertFalse(result.didCut)
        assertEquals(2, result.strokes.size)
        assertTrue(result.selectedIds.containsAll(setOf("s1", "s2")))
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 3. Stroke completely outside — not selected, not cut
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // 4+5. Stroke crossing lasso boundary — cut, interior selected, exterior kept
    // ────────────────────────────────────────────────────────────────────────────

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
        // Interior segment preserves source stroke properties
        assertEquals(0xFFFF0000.toInt(), selectedStroke!!.colorValue)
        assertEquals(30f, selectedStroke.points.first().x, 0.05f)
        assertEquals(70f, selectedStroke.points.last().x, 0.05f)
    }

    @Test
    fun testExteriorSegmentsRemainAfterCut() {
        val stroke = Stroke(
            id = "original",
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            colorValue = 0xFF0000FF.toInt()
        )
        val lasso = listOf(
            Offset(30f, 10f),
            Offset(70f, 10f),
            Offset(70f, 90f),
            Offset(30f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertTrue(result.didCut)

        val outsideStrokes = result.strokes.filter { it.id !in result.selectedIds }
        assertEquals(2, outsideStrokes.size)

        // Outside pieces should be fully outside the lasso (x < 30 or x > 70)
        for (s in outsideStrokes) {
            assertTrue(
                "Outside stroke should be left or right of lasso",
                s.points.all { it.x <= 30.05f } || s.points.all { it.x >= 69.95f }
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 6. Rectangle corner cut
    // ────────────────────────────────────────────────────────────────────────────

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
        // The cut-out corner piece is selected and preserves the yellow color
        val selected = result.strokes.filter { it.id in result.selectedIds }
        assertTrue(selected.isNotEmpty())
        assertEquals(0xFFFFFF00.toInt(), selected[0].colorValue)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 7. Rounded rectangle — lasso around a corner
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testCutAndSelectRoundedRectCorner() {
        val roundRect = Stroke(
            id = "rrect-1",
            points = listOf(Offset(0f, 0f), Offset(200f, 100f)),
            strokeType = StrokeType.ROUND_RECT,
            colorValue = 0xFF00FF00.toInt()
        )
        // Lasso around the bottom-left corner
        val lasso = listOf(
            Offset(-20f, 60f),
            Offset(60f, 60f),
            Offset(60f, 120f),
            Offset(-20f, 120f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(roundRect), lasso)
        // Should either cut (partial intersection) or select fully
        // Main assertion: the original stroke must not be duplicated
        val totalIds = result.strokes.map { it.id }.toSet()
        assertEquals(result.strokes.size, totalIds.size) // all IDs unique
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 8. Non-splittable objects (Text, StickyNote, Image) — preserved, not cut
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testTextStrokePreservedAndNotCut() {
        val textStroke = Stroke(
            id = "text-1",
            points = listOf(Offset(30f, 30f)),
            strokeType = StrokeType.TEXT,
            text = "Hello"
        )
        val lasso = listOf(
            Offset(10f, 10f),
            Offset(90f, 10f),
            Offset(90f, 90f),
            Offset(10f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(textStroke), lasso)
        assertFalse("Text strokes must never be split", result.didCut)
        assertEquals(1, result.strokes.size)
        assertEquals("text-1", result.strokes[0].id)
        assertEquals(StrokeType.TEXT, result.strokes[0].strokeType)
        // Text in the lasso region should be selected
        assertTrue(result.selectedIds.contains("text-1"))
    }

    @Test
    fun testStickyNotePreservedAndNotCut() {
        val stickyNote = Stroke(
            id = "sticky-1",
            points = listOf(Offset(30f, 30f)),
            strokeType = StrokeType.STICKY_NOTE,
            text = "Note"
        )
        val lasso = listOf(
            Offset(10f, 10f),
            Offset(200f, 10f),
            Offset(200f, 250f),
            Offset(10f, 250f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stickyNote), lasso)
        assertFalse("Sticky notes must never be split", result.didCut)
        assertEquals(1, result.strokes.size)
        assertEquals("sticky-1", result.strokes[0].id)
    }

    @Test
    fun testImageStrokePreservedAndNotCut() {
        val imageStroke = Stroke(
            id = "img-1",
            points = listOf(Offset(10f, 10f), Offset(200f, 150f)),
            strokeType = StrokeType.IMAGE,
            imageUri = "content://some/uri"
        )
        val lasso = listOf(
            Offset(0f, 0f),
            Offset(250f, 0f),
            Offset(250f, 200f),
            Offset(0f, 200f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(imageStroke), lasso)
        assertFalse("Image strokes must never be split", result.didCut)
        assertEquals(1, result.strokes.size)
        assertEquals("img-1", result.strokes[0].id)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 9+10. Empty / insufficient lasso
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testEmptyLassoReturnsUnchangedStrokes() {
        val stroke = Stroke(id = "s1", points = listOf(Offset(50f, 50f), Offset(60f, 60f)))
        val result = LassoCutEngine.cutAndSelect(listOf(stroke), emptyList())
        assertFalse(result.didCut)
        assertEquals(1, result.strokes.size)
        assertTrue(result.selectedIds.isEmpty())
    }

    @Test
    fun testTwoPointLassoReturnsUnchangedStrokes() {
        val stroke = Stroke(id = "s1", points = listOf(Offset(50f, 50f), Offset(60f, 60f)))
        val lasso = listOf(Offset(0f, 0f), Offset(100f, 100f))
        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertFalse(result.didCut)
        assertEquals(1, result.strokes.size)
        assertTrue(result.selectedIds.isEmpty())
    }

    @Test
    fun testEmptyStrokeListReturnsEmpty() {
        val lasso = listOf(
            Offset(0f, 0f), Offset(100f, 0f),
            Offset(100f, 100f), Offset(0f, 100f)
        )
        val result = LassoCutEngine.cutAndSelect(emptyList(), lasso)
        assertFalse(result.didCut)
        assertTrue(result.strokes.isEmpty())
        assertTrue(result.selectedIds.isEmpty())
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 11. Repeated intersections — a curved stroke that enters and exits lasso twice
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // 12. Unique IDs and valid metadata after splitting
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testSplitProducesUniqueIds() {
        val stroke = Stroke(
            id = "orig",
            points = listOf(Offset(0f, 50f), Offset(200f, 50f)),
            colorValue = 0xFF123456.toInt(),
            strokeWidth = 8f
        )
        val lasso = listOf(
            Offset(50f, 10f),
            Offset(150f, 10f),
            Offset(150f, 90f),
            Offset(50f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertTrue(result.didCut)

        val ids = result.strokes.map { it.id }
        assertEquals("All stroke IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun testSplitPreservesColorAndWidth() {
        val stroke = Stroke(
            id = "orig",
            points = listOf(Offset(0f, 50f), Offset(200f, 50f)),
            colorValue = 0xFFABCDEF.toInt(),
            strokeWidth = 12f
        )
        val lasso = listOf(
            Offset(50f, 10f),
            Offset(150f, 10f),
            Offset(150f, 90f),
            Offset(50f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertTrue(result.didCut)

        // All resulting strokes (including cut pieces) must preserve color and width
        for (s in result.strokes) {
            assertEquals(
                "Color must be preserved after cut",
                0xFFABCDEF.toInt(), s.colorValue
            )
            assertEquals(
                "Width must be preserved after cut",
                12f, s.strokeWidth, 0.001f
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 13. Unrelated strokes are untouched
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testUnrelatedStrokesUntouched() {
        val inside = Stroke(id = "inside", points = listOf(Offset(40f, 40f), Offset(60f, 60f)))
        val unrelated = Stroke(id = "unrelated", points = listOf(Offset(500f, 500f), Offset(600f, 600f)))
        val lasso = listOf(
            Offset(10f, 10f),
            Offset(90f, 10f),
            Offset(90f, 90f),
            Offset(10f, 90f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(inside, unrelated), lasso)

        // Unrelated stroke must still be present with original id
        assertTrue(result.strokes.any { it.id == "unrelated" })
        assertFalse(result.selectedIds.contains("unrelated"))
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 14. Selection-only (no cut) does not modify stroke list
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testSelectionOnlyDoesNotModifyStrokeList() {
        val strokes = listOf(
            Stroke(id = "a", points = listOf(Offset(20f, 20f), Offset(80f, 80f))),
            Stroke(id = "b", points = listOf(Offset(500f, 500f), Offset(600f, 600f)))
        )
        val lasso = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f)
        )

        val result = LassoCutEngine.cutAndSelect(strokes, lasso)
        assertFalse(result.didCut)
        // When no cut occurs the engine returns the original stroke list
        assertFalse("Stroke list must not change on selection-only", result.didCut)
        assertEquals(2, result.strokes.size)
        assertEquals("a", result.strokes[0].id)
        assertEquals("b", result.strokes[1].id)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 15. Supported shape types — selection when fully enclosed
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testCircleFullyEnclosedSelected() {
        val circle = Stroke(
            id = "circle-1",
            points = listOf(Offset(30f, 30f), Offset(70f, 70f)),
            strokeType = StrokeType.CIRCLE
        )
        val lasso = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(circle), lasso)
        assertFalse(result.didCut)
        assertTrue(result.selectedIds.contains("circle-1"))
    }

    @Test
    fun testStarFullyEnclosedSelected() {
        val star = Stroke(
            id = "star-1",
            points = listOf(Offset(30f, 30f), Offset(70f, 70f)),
            strokeType = StrokeType.STAR
        )
        val lasso = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(star), lasso)
        assertFalse(result.didCut)
        assertTrue(result.selectedIds.contains("star-1"))
    }

    @Test
    fun testHexagonFullyEnclosedSelected() {
        val hexagon = Stroke(
            id = "hex-1",
            points = listOf(Offset(30f, 30f), Offset(70f, 70f)),
            strokeType = StrokeType.HEXAGON
        )
        val lasso = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f)
        )

        val result = LassoCutEngine.cutAndSelect(listOf(hexagon), lasso)
        assertFalse(result.didCut)
        assertTrue(result.selectedIds.contains("hex-1"))
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 16. strokeWidth-zero / degenerate input
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testSinglePointStrokeDoesNotCrash() {
        val stroke = Stroke(id = "pt", points = listOf(Offset(50f, 50f)))
        val lasso = listOf(
            Offset(0f, 0f), Offset(100f, 0f),
            Offset(100f, 100f), Offset(0f, 100f)
        )
        val result = LassoCutEngine.cutAndSelect(listOf(stroke), lasso)
        assertFalse(result.didCut)
        assertEquals(1, result.strokes.size)
    }
}
