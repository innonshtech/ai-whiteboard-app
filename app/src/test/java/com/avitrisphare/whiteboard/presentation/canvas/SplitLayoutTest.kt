package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitLayoutTest {
    @Test
    fun panelsTileTheScreenWithoutOverlap() {
        listOf(2, 3, 4, 6).forEach { count ->
            val frames = paneFrames(count, 1200, 800, defaultWeights(count))
            assertEquals(count, frames.size)
            val area = frames.sumOf { it.width * it.height }
            assertEquals(1200 * 800, area)
        }
    }

    @Test
    fun twoThreeFourSixPaneLayoutDimensionsMatchColumnsAndRows() {
        // 2 panes: 2 columns, 1 row
        assertEquals(2, splitColumns(2))
        assertEquals(1, splitRows(2))
        val frames2 = paneFrames(2, 1000, 600, defaultWeights(2))
        assertEquals(2, frames2.size)
        assertEquals(0, frames2[0].left)
        assertEquals(500, frames2[0].right)
        assertEquals(500, frames2[1].left)
        assertEquals(1000, frames2[1].right)

        // 3 panes: 3 columns, 1 row
        assertEquals(3, splitColumns(3))
        assertEquals(1, splitRows(3))
        val frames3 = paneFrames(3, 900, 600, defaultWeights(3))
        assertEquals(3, frames3.size)
        assertEquals(300, frames3[0].width)
        assertEquals(300, frames3[1].width)
        assertEquals(300, frames3[2].width)

        // 4 panes: 2 columns, 2 rows
        assertEquals(2, splitColumns(4))
        assertEquals(2, splitRows(4))
        val frames4 = paneFrames(4, 1000, 800, defaultWeights(4))
        assertEquals(4, frames4.size)
        assertEquals(500, frames4[0].width)
        assertEquals(400, frames4[0].height)
        assertEquals(400, frames4[2].top) // Row 1 starts at top = 400

        // 6 panes: 3 columns, 2 rows
        assertEquals(3, splitColumns(6))
        assertEquals(2, splitRows(6))
        val frames6 = paneFrames(6, 900, 800, defaultWeights(6))
        assertEquals(6, frames6.size)
        assertEquals(300, frames6[0].width)
        assertEquals(400, frames6[0].height)
    }

    @Test
    fun everyDividerCanBeResizedInVerticalAndHorizontalDirections() {
        // Test vertical divider drag in 2-pane
        val weights2 = dragDivider(defaultWeights(2), 2, vertical = true, index = 0, deltaPx = 100f, spanPx = 1000f)
        val frames2 = paneFrames(2, 1000, 500, weights2)
        assertEquals(2, frames2.size)
        assertEquals(600, frames2[0].width)
        assertEquals(400, frames2[1].width)

        // Test vertical divider 1 in 3-pane
        val weights3 = dragDivider(defaultWeights(3), 3, vertical = true, index = 0, deltaPx = 50f, spanPx = 900f)
        val frames3 = paneFrames(3, 900, 500, weights3)
        assertEquals(350, frames3[0].width)
        assertEquals(250, frames3[1].width)
        assertEquals(300, frames3[2].width)

        // Test horizontal divider in 4-pane (between row 0 and row 1)
        val weights4 = dragDivider(defaultWeights(4), 4, vertical = false, index = 0, deltaPx = 100f, spanPx = 800f)
        val frames4 = paneFrames(4, 1000, 800, weights4)
        assertEquals(500, frames4[0].height) // row 0 expanded from 400 to 500
        assertEquals(300, frames4[2].height) // row 1 shrunk from 400 to 300
    }

    @Test
    fun dividerDragSmoothlyClampsToMinimumPaneWeight() {
        // Drag far beyond the limit: 1000px on a 1000px span
        val weights = dragDivider(defaultWeights(2), 2, vertical = true, index = 0, deltaPx = 900f, spanPx = 1000f)
        val frames = paneFrames(2, 1000, 500, weights)
        // Panes must stay at or above 12% width
        assertTrue(frames[0].width >= 120)
        assertTrue(frames[1].width >= 120)
        assertEquals(1000, frames[0].width + frames[1].width)
    }

    @Test
    fun hitDividerDetectsVerticalAndHorizontalDividersWithGenerousSlop() {
        val frames4 = paneFrames(4, 1000, 800, defaultWeights(4))
        // Vertical divider is at x = 500
        val hitV = hitDivider(frames4, 4, x = 510f, y = 100f, slop = 36f)
        org.junit.Assert.assertNotNull(hitV)
        assertTrue(hitV!!.vertical)
        assertEquals(0, hitV.index)

        // Horizontal divider is at y = 400
        val hitH = hitDivider(frames4, 4, x = 200f, y = 410f, slop = 36f)
        assertNotNull(hitH)
        assertFalse(hitH!!.vertical)
        assertEquals(0, hitH.index)

        // Near intersection (x = 505, y = 401) -> closer to horizontal (dist 1 vs 5)
        val hitIntersection = hitDivider(frames4, 4, x = 505f, y = 401f, slop = 36f)
        assertNotNull(hitIntersection)
        assertFalse(hitIntersection!!.vertical) // Closer to y = 400
    }

    @Test
    fun aDividerDragKeepsEveryPanelAndTheInkListsApart() {
        val weights = dragDivider(defaultWeights(2), 2, vertical = true, index = 0, deltaPx = 100f, spanPx = 1000f)
        val frames = paneFrames(2, 1000, 500, weights)
        assertEquals(2, frames.size)
        assertTrue(frames[0].width > 500)
        assertTrue(frames[1].width > 120)
        assertEquals(1000, frames[0].width + frames[1].width)

        val first = pane(0f)
        val second = pane(400f)
        val updated = replacePaneStrokes(listOf(first, second), 1, second.strokes + pane(20f).strokes.first())
        assertSame(first.strokes, updated[0].strokes)
        assertEquals(2, updated[1].strokes.size)
        assertEquals(1, updated[0].strokes.size)
    }

    @Test
    fun hitsThePanelUnderThePen() {
        val frames = paneFrames(6, 900, 600, defaultWeights(6))
        assertEquals(0, hitPane(frames, 10f, 10f))
        assertEquals(5, hitPane(frames, 890f, 590f))
        assertEquals(3, hitPane(frames, 10f, 400f))
    }

    private fun pane(x: Float): CanvasPane {
        return CanvasPane(
            strokes = listOf(
                Stroke(
                    points = listOf(Offset(x, 0f), Offset(x + 30f, 20f)),
                    strokeType = StrokeType.PEN
                )
            )
        )
    }
}
