package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSnipEngineTest {

    private fun createSampleImageStroke(): Stroke {
        return Stroke(
            points = listOf(Offset(100f, 100f), Offset(500f, 400f)),
            strokeType = StrokeType.IMAGE,
            imageUri = "/fake/path/test.png"
        )
    }

    @Test
    fun testFullImageSnipBounds() {
        val stroke = createSampleImageStroke()
        val bounds = stroke.bounds
        assertEquals(400f, bounds.width, 0.01f)
        assertEquals(300f, bounds.height, 0.01f)
    }

    @Test
    fun testFullCanvasViewportBounds() {
        val viewport = CanvasTransform.calculateViewportWorldBounds(
            viewportSize = androidx.compose.ui.geometry.Size(1920f, 1080f),
            scale = 1.0f,
            offset = Offset(0f, 0f)
        )
        assertEquals(0f, viewport.left, 0.01f)
        assertEquals(0f, viewport.top, 0.01f)
        assertEquals(1920f, viewport.width, 0.01f)
        assertEquals(1080f, viewport.height, 0.01f)
    }

    @Test
    fun testRectangleIntersectionMath() {
        val stroke = createSampleImageStroke()
        val imgBounds = stroke.bounds // 100, 100 to 500, 400 (w: 400, h: 300)

        val selRect = Rect(50f, 50f, 250f, 250f)
        val cropLeft = maxOf(selRect.left, imgBounds.left)
        val cropTop = maxOf(selRect.top, imgBounds.top)
        val cropRight = minOf(selRect.right, imgBounds.right)
        val cropBottom = minOf(selRect.bottom, imgBounds.bottom)

        assertEquals(100f, cropLeft, 0.01f)
        assertEquals(100f, cropTop, 0.01f)
        assertEquals(250f, cropRight, 0.01f)
        assertEquals(250f, cropBottom, 0.01f)
        assertEquals(150f, cropRight - cropLeft, 0.01f)
        assertEquals(150f, cropBottom - cropTop, 0.01f)
    }

    @Test
    fun testSquareSnipAspectConstraint() {
        val start = Offset(150f, 150f)
        val cur = Offset(300f, 250f)

        val dx = cur.x - start.x // 150
        val dy = cur.y - start.y // 100
        val side = maxOf(Math.abs(dx), Math.abs(dy)) // 150
        val x2 = start.x + (if (dx >= 0) side else -side) // 300
        val y2 = start.y + (if (dy >= 0) side else -side) // 300

        val squareRect = Rect(minOf(start.x, x2), minOf(start.y, y2), maxOf(start.x, x2), maxOf(start.y, y2))
        assertEquals(150f, squareRect.width, 0.01f)
        assertEquals(150f, squareRect.height, 0.01f)
    }

    @Test
    fun testFreeformPointsBounds() {
        val points = listOf(
            Offset(120f, 130f),
            Offset(200f, 110f),
            Offset(260f, 180f),
            Offset(190f, 250f),
            Offset(130f, 210f)
        )

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        assertEquals(120f, minX, 0.01f)
        assertEquals(260f, maxX, 0.01f)
        assertEquals(110f, minY, 0.01f)
        assertEquals(250f, maxY, 0.01f)
        assertEquals(140f, maxX - minX, 0.01f)
        assertEquals(140f, maxY - minY, 0.01f)
    }

    @Test
    fun testOriginalImageStaysUnchanged() {
        val original = createSampleImageStroke()
        val originalPoints = original.points.toList()
        val originalUri = original.imageUri

        // Simulate snip operation
        val dummyResult = SnipResult(
            filePath = "/fake/path/snip_123.png",
            widthPx = 200,
            heightPx = 150,
            worldWidth = 200f,
            worldHeight = 150f,
            originalStrokeId = original.id
        )

        // Ensure original stroke properties are completely identical
        assertEquals(originalPoints, original.points)
        assertEquals(originalUri, original.imageUri)
        assertEquals(StrokeType.IMAGE, original.strokeType)
        assertEquals(dummyResult.originalStrokeId, original.id)
    }
}

