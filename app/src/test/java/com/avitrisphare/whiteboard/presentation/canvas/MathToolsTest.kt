package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.avitrisphare.whiteboard.domain.model.withGraphPaperToggled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MathToolsTest {

    @Test
    fun rulerBodyMovesAndEndsRotate() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(200f, 200f),
            span = 400f
        )
        assertEquals(MathToolZone.MOVE, hitTestMathTool(ruler, Offset(200f, 200f)))
        assertEquals(MathToolZone.ROTATE_POSITIVE, hitTestMathTool(ruler, Offset(400f, 200f)))
        assertEquals(MathToolZone.ROTATE_NEGATIVE, hitTestMathTool(ruler, Offset(0f, 200f)))
        assertTrue(distanceToRulerEdge(ruler, Offset(200f, 232f)) < RULER_EDGE_SLOP)
    }

    @Test
    fun draggingAnEndRotatesAroundTheOppositeEnd() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(200f, 200f),
            span = 400f
        )
        val rotated = rotateToolByEnd(ruler, Offset(400f, 400f), draggingPositiveEnd = true)
        val pivot = endPoint(ruler, positive = false)
        val dragged = endPoint(rotated, positive = true)
        val pivotToDrag = Offset(dragged.x - pivot.x, dragged.y - pivot.y)
        val pivotToFinger = Offset(400f - pivot.x, 400f - pivot.y)
        val cross = pivotToDrag.x * pivotToFinger.y - pivotToDrag.y * pivotToFinger.x
        val reach = endReach(rotated)
        assertEquals(200f, reach, 0.5f)
        assertTrue(abs(cross) < 1f)
    }

    @Test
    fun compassDragSetsRadiusAndSweepDrawsArc() {
        val compass = PlacedMathTool(
            kind = MathToolKind.COMPASS,
            center = Offset(100f, 100f),
            span = 80f
        )
        assertEquals(MathToolZone.PENCIL, hitTestMathTool(compass, compassPencil(compass)))
        val radiusGesture = MathGesture().apply {
            startSpan = compass.span
            movedPx = 40f
        }
        val widened = dragCompassPencil(compass, Offset(100f, 240f), radiusGesture)
        assertFalse(radiusGesture.arcMode)
        assertEquals(140f, widened.span, 0.5f)

        val sweep = MathGesture().apply {
            startSpan = widened.span
            startAngleDeg = widened.rotationDeg
            lastAngleDeg = widened.rotationDeg
            movedPx = 30f
        }
        val swept = dragCompassPencil(widened, Offset(240f, 100f), sweep)
        assertTrue(sweep.arcMode)
        assertEquals(widened.span, swept.span, 0.5f)
        val arc = compassArcScreenPoints(swept, sweep.startAngleDeg, sweep.sweepDeg)
        assertTrue(arc.size >= 2)
    }

    @Test
    fun tapSizedCompassMotionIsNotAnArc() {
        assertFalse(isFullCircleSweep(20f))
        assertTrue(isFullCircleSweep(350f))
        val circle = circleStroke(Offset(4f, 4f), 12f, 0xFF000000.toInt(), 3f)
        assertEquals(StrokeType.CIRCLE, circle.strokeType)
        assertEquals(2, circle.points.size)
    }

    @Test
    fun protractorMarksDegreesFromTheBaseline() {
        val tool = PlacedMathTool(
            kind = MathToolKind.PROTRACTOR,
            center = Offset(0f, 0f),
            span = 150f
        )
        assertEquals(0f, protractorAngle(tool, Offset(120f, 0f))!!, 0.5f)
        assertEquals(90f, protractorAngle(tool, Offset(0f, -120f))!!, 0.5f)
        assertEquals(180f, protractorAngle(tool, Offset(-120f, 0f))!!, 0.5f)
        assertNull(protractorAngle(tool, Offset(0f, 40f)))
        assertEquals(MathToolZone.SCALE, hitTestMathTool(tool, Offset(0f, -150f)))
        assertEquals("90°", formatDegrees(90.2f))
    }

    @Test
    fun lengthsUseScreenCentimetres() {
        assertEquals("2.5 cm", formatCentimetres(100f, pxPerCm = 40f))
        val length = screenLengthPx(Offset(0f, 0f), Offset(50f, 0f), scale = 2f)
        assertEquals(100f, length, 0.01f)
        val snapped = projectOntoRuler(Offset(0f, 0f), Offset(30f, 40f), Offset(1f, 0f), scale = 1f)
        assertEquals(30f, snapped.x, 0.01f)
        assertEquals(0f, snapped.y, 0.01f)
    }

    @Test
    fun twoFingersMoveAndRotate() {
        val tool = newMathTool(MathToolKind.RULER, Offset(10f, 20f))
        val next = translateAndRotate(tool, Offset(5f, -3f), 15f)
        assertEquals(15f, next.center.x, 0.01f)
        assertEquals(17f, next.center.y, 0.01f)
        assertEquals(15f, next.rotationDeg, 0.01f)
    }

    @Test
    fun releaseOverTheToolbarRemovesTheTool() {
        val dock = Rect(0f, 400f, 800f, 500f)
        assertTrue(droppedOnToolbar(Offset(100f, 450f), dock))
        assertFalse(droppedOnToolbar(Offset(100f, 100f), dock))
        assertFalse(droppedOnToolbar(Offset(100f, 450f), null))
    }

    @Test
    fun graphPaperTogglesBackToDotted() {
        assertEquals(CanvasBackground.GRAPH, CanvasBackground.DOTTED.withGraphPaperToggled())
        assertEquals(CanvasBackground.DOTTED, CanvasBackground.GRAPH.withGraphPaperToggled())
        assertEquals(CanvasBackground.GRAPH, CanvasBackground.fromKey("graph"))
        assertNotNull(rulerForStraightedge(listOf(newMathTool(MathToolKind.RULER, Offset(80f, 80f))), Offset(80f, 112f)))
    }
}
