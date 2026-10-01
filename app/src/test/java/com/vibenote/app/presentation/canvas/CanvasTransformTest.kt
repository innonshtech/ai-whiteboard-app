package com.vibenote.app.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vibenote.app.domain.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTransformTest {

    @Test
    fun `screenToWorld and worldToScreen transform correctly`() {
        val screenPoint = Offset(300f, 400f)
        val scale = 2f
        val offset = Offset(100f, 100f)

        // (300 - 100) / 2 = 100, (400 - 100) / 2 = 150
        val worldPoint = CanvasTransform.screenToWorld(screenPoint, scale, offset)
        assertEquals(100f, worldPoint.x, 0.001f)
        assertEquals(150f, worldPoint.y, 0.001f)

        val convertedScreen = CanvasTransform.worldToScreen(worldPoint, scale, offset)
        assertEquals(screenPoint.x, convertedScreen.x, 0.001f)
        assertEquals(screenPoint.y, convertedScreen.y, 0.001f)
    }

    @Test
    fun `calculateViewportWorldBounds produces correct bounds`() {
        val viewportSize = Size(1000f, 2000f)
        val scale = 2f
        val offset = Offset(200f, 400f)

        val bounds = CanvasTransform.calculateViewportWorldBounds(viewportSize, scale, offset)
        assertEquals(-100f, bounds.left, 0.001f)
        assertEquals(-200f, bounds.top, 0.001f)
        assertEquals(400f, bounds.right, 0.001f)
        assertEquals(800f, bounds.bottom, 0.001f)
    }

    @Test
    fun `calculateContentBounds returns union of all strokes`() {
        val stroke1 = Stroke(
            points = listOf(Offset(10f, 20f), Offset(100f, 200f)),
            strokeWidth = 4f
        )
        val stroke2 = Stroke(
            points = listOf(Offset(-50f, -60f), Offset(30f, 40f)),
            strokeWidth = 4f
        )

        val bounds = CanvasTransform.calculateContentBounds(listOf(stroke1, stroke2))
        assertNotNull(bounds)
        assertEquals(-52f, bounds!!.left, 0.001f)
        assertEquals(-62f, bounds.top, 0.001f)
        assertEquals(102f, bounds.right, 0.001f)
        assertEquals(202f, bounds.bottom, 0.001f)
    }

    @Test
    fun `calculateContentBounds returns null for empty strokes`() {
        val bounds = CanvasTransform.calculateContentBounds(emptyList())
        assertNull(bounds)
    }

    @Test
    fun `calculatePivotZoomOffset pins centroid in world space`() {
        val oldScale = 1.0f
        val newScale = 2.0f
        val oldOffset = Offset(0f, 0f)
        val centroid = Offset(500f, 500f)
        val panDelta = Offset.Zero

        val newOffset = CanvasTransform.calculatePivotZoomOffset(
            oldScale = oldScale,
            newScale = newScale,
            oldOffset = oldOffset,
            centroid = centroid,
            panDelta = panDelta
        )

        // Centroid at 500 in screen space was 500 in world space at 1x
        // At 2x, world 500 * 2 + newOffset.x should equal 500 => newOffset.x = -500
        assertEquals(-500f, newOffset.x, 0.001f)
        assertEquals(-500f, newOffset.y, 0.001f)
    }

    @Test
    fun `strokeIntersectsPoint detects collision on line segment`() {
        val stroke = Stroke(
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeWidth = 4f
        )

        // Point directly near the midpoint (50, 5) with threshold 10
        assertTrue(CanvasTransform.strokeIntersectsPoint(stroke, Offset(50f, 5f), 10f))

        // Point far away (50, 50)
        assertFalse(CanvasTransform.strokeIntersectsPoint(stroke, Offset(50f, 50f), 10f))
    }
}
