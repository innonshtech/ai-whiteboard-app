package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
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
