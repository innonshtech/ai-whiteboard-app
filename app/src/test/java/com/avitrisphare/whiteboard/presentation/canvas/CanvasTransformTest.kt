package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.avitrisphare.whiteboard.domain.model.Stroke
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

    @Test
    fun `pinch-to-zoom limits enforce 10 percent to 500 percent zoom level`() {
        assertEquals(0.1f, CanvasTransform.MIN_SCALE, 0.0001f)
        assertEquals(5.0f, CanvasTransform.MAX_SCALE, 0.0001f)
        assertEquals("10%", CanvasTransform.formatZoom(0.1f))
        assertEquals("500%", CanvasTransform.formatZoom(5.0f))

        // Clamping below 10%
        val zoomedOut = (1.0f * 0.05f).coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)
        assertEquals(0.1f, zoomedOut, 0.0001f)

        // Clamping above 500%
        val zoomedIn = (1.0f * 8.0f).coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)
        assertEquals(5.0f, zoomedIn, 0.0001f)
    }

    @Test
    fun `two-finger pan moves canvas smoothly in any direction`() {
        val oldScale = 1.0f
        val oldOffset = Offset(100f, 200f)
        val centroid = Offset(400f, 300f)

        // Pan in positive X and Y
        val panPositive = CanvasTransform.calculatePivotZoomOffset(
            oldScale = oldScale,
            newScale = oldScale,
            oldOffset = oldOffset,
            centroid = centroid,
            panDelta = Offset(50f, 75f)
        )
        assertEquals(150f, panPositive.x, 0.001f)
        assertEquals(275f, panPositive.y, 0.001f)

        // Pan in negative X and Y
        val panNegative = CanvasTransform.calculatePivotZoomOffset(
            oldScale = oldScale,
            newScale = oldScale,
            oldOffset = oldOffset,
            centroid = centroid,
            panDelta = Offset(-80f, -120f)
        )
        assertEquals(20f, panNegative.x, 0.001f)
        assertEquals(80f, panNegative.y, 0.001f)

        // Pan in diagonal quadrants at 10% minimum zoom
        val panAtMinZoom = CanvasTransform.calculatePivotZoomOffset(
            oldScale = CanvasTransform.MIN_SCALE,
            newScale = CanvasTransform.MIN_SCALE,
            oldOffset = Offset(0f, 0f),
            centroid = centroid,
            panDelta = Offset(-250f, 350f)
        )
        assertEquals(-250f, panAtMinZoom.x, 0.001f)
        assertEquals(350f, panAtMinZoom.y, 0.001f)

        // Pan in diagonal quadrants at 500% maximum zoom
        val panAtMaxZoom = CanvasTransform.calculatePivotZoomOffset(
            oldScale = CanvasTransform.MAX_SCALE,
            newScale = CanvasTransform.MAX_SCALE,
            oldOffset = Offset(0f, 0f),
            centroid = centroid,
            panDelta = Offset(120f, -400f)
        )
        assertEquals(120f, panAtMaxZoom.x, 0.001f)
        assertEquals(-400f, panAtMaxZoom.y, 0.001f)
    }

    @Test
    fun `simultaneous pinch-to-zoom and two-finger pan maintains pivot point`() {
        val oldScale = 1.0f
        val newScale = 2.5f
        val oldOffset = Offset(50f, 80f)
        val centroid = Offset(300f, 400f)
        val panDelta = Offset(20f, -15f)

        // The point under centroid in world space before zoom:
        val worldUnderCentroid = CanvasTransform.screenToWorld(centroid, oldScale, oldOffset)

        val newOffset = CanvasTransform.calculatePivotZoomOffset(
            oldScale = oldScale,
            newScale = newScale,
            oldOffset = oldOffset,
            centroid = centroid,
            panDelta = panDelta
        )

        // After zoom and pan, that world point should be at (centroid + panDelta) in screen space:
        val expectedScreenPos = centroid + panDelta
        val actualScreenPos = CanvasTransform.worldToScreen(worldUnderCentroid, newScale, newOffset)

        assertEquals(expectedScreenPos.x, actualScreenPos.x, 0.001f)
        assertEquals(expectedScreenPos.y, actualScreenPos.y, 0.001f)
    }
}
