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
        assertEquals(MathToolZone.CLOSE, hitTestMathTool(compass, compassCloseBtn(compass)))
        assertEquals(MathToolZone.ROTATE, hitTestMathTool(compass, compassTopRotateBtn(compass)))
        assertEquals(MathToolZone.SCALE, hitTestMathTool(compass, compassScaleBtn(compass)))
        assertEquals(MathToolZone.SPREAD, hitTestMathTool(compass, compassSpreadBtn(compass)))
        assertEquals(MathToolZone.MOVE, hitTestMathTool(compass, compass.center))
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
            movedPx = 45f
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
        val protractor = PlacedMathTool(
            kind = MathToolKind.PROTRACTOR,
            center = Offset(0f, 0f),
            span = 150f,
            pointerAngleDeg = 90f
        )
        assertEquals(0f, protractorAngle(protractor, Offset(120f, 0f))!!, 0.5f)
        assertEquals(90f, protractorAngle(protractor, Offset(0f, -120f))!!, 0.5f)
        assertEquals(180f, protractorAngle(protractor, Offset(-120f, 0f))!!, 0.5f)
        assertEquals("90°", formatDegrees(90.2f))

        val pSpan = protractor.span
        val shelfY = (pSpan * 0.22f).coerceIn(48f, 64f) * 0.5f
        assertEquals(MathToolZone.CLOSE, hitTestMathTool(protractor, Offset(-0.75f * pSpan, shelfY)))
        assertEquals(MathToolZone.ANGLE_MODE_1, hitTestMathTool(protractor, Offset(-0.45f * pSpan, shelfY)))
        assertEquals(MathToolZone.ANGLE_MODE_ARC_LEFT, hitTestMathTool(protractor, Offset(-0.15f * pSpan, shelfY)))
        assertEquals(MathToolZone.ANGLE_MODE_ARC, hitTestMathTool(protractor, Offset(0.15f * pSpan, shelfY)))
        assertEquals(MathToolZone.ANGLE_MODE_2, hitTestMathTool(protractor, Offset(0.45f * pSpan, shelfY)))
        assertEquals(MathToolZone.SCALE, hitTestMathTool(protractor, Offset(0.75f * pSpan, shelfY)))
        assertEquals(MathToolZone.ROTATE, hitTestMathTool(protractor, Offset(0f, -pSpan - 28f)))
        assertEquals(MathToolZone.ANGLE_ARM, hitTestMathTool(protractor, Offset(0f, -pSpan)))
        assertEquals(MathToolZone.MOVE, hitTestMathTool(protractor, Offset(60f, -60f)))

        val setSquare = PlacedMathTool(
            kind = MathToolKind.SET_SQUARE,
            center = Offset(0f, 0f),
            span = 150f
        )
        val half = setSquare.span / 2f
        assertEquals(MathToolZone.CLOSE, hitTestMathTool(setSquare, Offset(-half * 0.55f, -half * 0.45f)))
        assertEquals(MathToolZone.SCALE, hitTestMathTool(setSquare, Offset(-half * 0.30f, half * 0.30f)))
        assertEquals(MathToolZone.ROTATE, hitTestMathTool(setSquare, Offset(-half, -half - 28f)))
        assertEquals(MathToolZone.MOVE, hitTestMathTool(setSquare, Offset(-45f, 10f)))
        // Touching near set square bottom edge returns NONE so edge line drawing flows like ruler!
        assertEquals(MathToolZone.NONE, hitTestMathTool(setSquare, Offset(0f, half)))
        assertNotNull(rulerForStraightedge(listOf(setSquare), Offset(0f, half)))

        val triangle = PlacedMathTool(
            kind = MathToolKind.TRIANGLE,
            center = Offset(0f, 0f),
            span = 150f
        )
        val side = triangle.span * 1.8f
        val h = side * 0.8660254f
        assertEquals(MathToolZone.CLOSE, hitTestMathTool(triangle, Offset(0f, -h * 0.35f)))
        assertEquals(MathToolZone.SCALE, hitTestMathTool(triangle, Offset(0f, 20f)))
        assertEquals(MathToolZone.ROTATE, hitTestMathTool(triangle, Offset(0f, -h * 0.6667f - 28f)))
        assertEquals(MathToolZone.MOVE, hitTestMathTool(triangle, Offset(0f, -20f)))
        // Touching near triangle bottom edge returns NONE so edge line drawing flows like ruler!
        assertEquals(MathToolZone.NONE, hitTestMathTool(triangle, Offset(0f, h * 0.3333f)))
        assertNotNull(rulerForStraightedge(listOf(triangle), Offset(0f, h * 0.3333f)))
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

    @Test
    fun toolsDoNotOverlapWhenPlaced() {
        val ruler = newMathTool(MathToolKind.RULER, Offset(400f, 400f))
        val candidateProtractor = Offset(400f, 400f) // Dropped directly on top of ruler
        val resolved = resolveNonOverlappingPosition(
            candidate = candidateProtractor,
            candidateKind = MathToolKind.PROTRACTOR,
            candidateSpan = defaultMathSpan(MathToolKind.PROTRACTOR),
            existingTools = listOf(ruler)
        )
        val placedProtractor = newMathTool(MathToolKind.PROTRACTOR, resolved)
        assertFalse("Tools must not overlap", toolsOverlap(ruler, placedProtractor))
        val distance = kotlin.math.hypot(resolved.x - ruler.center.x, resolved.y - ruler.center.y)
        assertTrue("Tools should separate to clear overlap", distance > 20f)
        assertTrue("Tools should stay close and not separate by major distance", distance < 200f)
    }

    @Test
    fun multipleToolsPlacedConsecutivelyDoNotOverlap() {
        val tools = mutableListOf<PlacedMathTool>()
        val kinds = listOf(MathToolKind.RULER, MathToolKind.COMPASS, MathToolKind.PROTRACTOR)
        val center = Offset(500f, 400f)
        for (kind in kinds) {
            val resolved = findNonOverlappingPosition(
                desiredCenter = center,
                kind = kind,
                span = defaultMathSpan(kind),
                existingTools = tools
            )
            tools.add(newMathTool(kind, resolved))
        }
        assertEquals(3, tools.size)
        // Verify every pair does not overlap
        for (i in 0 until tools.size) {
            for (j in i + 1 until tools.size) {
                val a = tools[i]
                val b = tools[j]
                assertFalse("Tool ${a.kind} and ${b.kind} should not overlap", toolsOverlap(a, b))
            }
        }
    }

    @Test
    fun toolsCanSitCloseWithoutBeingRepelled() {
        val ruler = newMathTool(MathToolKind.RULER, Offset(400f, 400f))
        // Place protractor right next to ruler along Y axis (just 10px below ruler's bottom edge)
        val rulerBounds = mathToolBoundingRect(ruler)
        val closeCandidate = Offset(400f, rulerBounds.bottom + defaultMathSpan(MathToolKind.PROTRACTOR) + 10f)
        val resolved = findNonOverlappingPosition(
            desiredCenter = closeCandidate,
            kind = MathToolKind.PROTRACTOR,
            span = defaultMathSpan(MathToolKind.PROTRACTOR),
            existingTools = listOf(ruler)
        )
        assertEquals("Tool should stay exactly where placed when not overlapping", closeCandidate, resolved)
    }

    @Test
    fun rulerCloseButtonHitTest() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(300f, 300f),
            span = 440f
        )
        // half = 220f, halfT = 48f. Close button is at center + Offset(220 - 24, -48 + 22) = (496, 274)
        val closeBtn = Offset(496f, 274f)
        assertEquals(MathToolZone.CLOSE, hitTestMathTool(ruler, closeBtn))
    }

    @Test
    fun rulerPhysicalEdgeSnappingAndLength() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(400f, 400f),
            span = 400f,
            rotationDeg = 0f
        )
        // halfT is 48f. Top edge is at y = 352f. Bottom edge is at y = 448f.
        val topTouch = Offset(350f, 360f)
        val bottomTouch = Offset(450f, 440f)

        assertEquals(-1f, rulerEdgeForPoint(ruler, topTouch), 0.01f)
        assertEquals(1f, rulerEdgeForPoint(ruler, bottomTouch), 0.01f)

        val snappedTop = snapToRulerEdge(ruler, topTouch, edgeSign = -1f)
        assertEquals(350f, snappedTop.x, 0.01f)
        assertEquals(352f, snappedTop.y, 0.01f)

        val snappedBottom = snapToRulerEdge(ruler, bottomTouch, edgeSign = 1f)
        assertEquals(450f, snappedBottom.x, 0.01f)
        assertEquals(448f, snappedBottom.y, 0.01f)

        // Clamping to ruler span limits
        val pastRightEdgeTouch = Offset(700f, 360f)
        val snappedRight = snapToRulerEdge(ruler, pastRightEdgeTouch, edgeSign = -1f)
        assertEquals(600f, snappedRight.x, 0.01f) // 400 + 200 = 600 max
        assertEquals(352f, snappedRight.y, 0.01f)
    }

    @Test
    fun spanClampFollowsEachInstrument() {
        assertEquals(200f, clampMathSpan(MathToolKind.RULER, 10f))
        assertEquals(1800f, clampMathSpan(MathToolKind.RULER, 9000f))
        assertEquals(COMPASS_MIN_RADIUS, clampMathSpan(MathToolKind.COMPASS, 1f))
        assertEquals(COMPASS_MAX_RADIUS, clampMathSpan(MathToolKind.COMPASS, 5000f))
        assertEquals(800f, clampMathSpan(MathToolKind.PROTRACTOR, 2000f))
        assertEquals(120f, clampMathSpan(MathToolKind.SET_SQUARE, 20f))
        assertEquals(900f, clampMathSpan(MathToolKind.TRIANGLE, 4000f))
        assertEquals(396f, triangleSidePx(220f), 0.01f)
    }

    @Test
    fun rulerZeroMarkAndTicksVisibility() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(200f, 200f),
            span = 400f
        )
        val half = ruler.span / 2f
        val zeroXLocal = -half + RULER_INSET
        assertEquals(-176f, zeroXLocal, 0.01f)

        // Handle radius is 16f centered at -half (-200f), so handle reaches -184f.
        // zeroXLocal is at -176f, giving an 8px clear gap so "0" tick and number are never covered!
        val handleInnerEdgeLocal = -half + 16f
        assertTrue(zeroXLocal > handleInnerEdgeLocal)
        assertEquals(8f, zeroXLocal - handleInnerEdgeLocal, 0.01f)
    }

    @Test
    fun rulerMagneticMillimeterSnapping() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(400f, 400f),
            span = 400f,
            rotationDeg = 0f
        )
        val half = ruler.span / 2f
        val pxPerCm = 100f
        val zeroXScreen = ruler.center.x - half + RULER_INSET // 400 - 200 + 24 = 224f

        // Touch near 0.0 cm mark (e.g. at x = 220f or x = 228f) on top edge
        val nearZeroTouch = Offset(228f, 352f)
        val snappedZero = snapToRulerEdge(ruler, nearZeroTouch, edgeSign = -1f, pxPerCm = pxPerCm)
        assertEquals(zeroXScreen, snappedZero.x, 0.01f)
        assertEquals(352f, snappedZero.y, 0.01f)

        // Touch near 2.0 cm mark (zeroX + 200px = 424f): touch at 422f
        val near2cmTouch = Offset(422f, 352f)
        val snapped2cm = snapToRulerEdge(ruler, near2cmTouch, edgeSign = -1f, pxPerCm = pxPerCm)
        assertEquals(zeroXScreen + 200f, snapped2cm.x, 0.01f)

        // Touch near 3.5 cm mark (zeroX + 350px = 574f): touch at 572f
        val near3Point5cmTouch = Offset(572f, 352f)
        val snapped3Point5cm = snapToRulerEdge(ruler, near3Point5cmTouch, edgeSign = -1f, pxPerCm = pxPerCm)
        assertEquals(zeroXScreen + 350f, snapped3Point5cm.x, 0.01f)

        // Touch past max mark (e.g. at x = 590f): snaps to max mark (zeroX + 352px = 576f)
        val pastMaxTouch = Offset(590f, 352f)
        val maxMarkScreen = ruler.center.x + half - RULER_INSET // 400 + 200 - 24 = 576f
        val snappedMax = snapToRulerEdge(ruler, pastMaxTouch, edgeSign = -1f, pxPerCm = pxPerCm)
        assertEquals(maxMarkScreen, snappedMax.x, 0.01f)
    }

    @Test
    fun rulerDrawingAlongTopEdgeDoesNotHitCloseButton() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(300f, 300f),
            span = 440f,
            rotationDeg = 0f
        )
        // half = 220f, halfT = 48f.
        // Close button center is at (300 + 220 - 24, 300 - 48 + 22) = (496f, 274f).
        // 1. Tapping directly inside at the close button returns CLOSE:
        val closeInside = Offset(496f, 274f)
        assertEquals(MathToolZone.CLOSE, hitTestMathTool(ruler, closeInside))

        // 2. Drawing along top edge right past the close button (y = 300 - 48 = 252f) returns NONE:
        val drawOnTopEdge = Offset(496f, 252f)
        assertEquals(MathToolZone.NONE, hitTestMathTool(ruler, drawOnTopEdge))
    }

    @Test
    fun rulerDragRulerEndCardinalAngleSnapping() {
        val ruler = PlacedMathTool(
            kind = MathToolKind.RULER,
            center = Offset(200f, 200f),
            span = 400f
        )
        // Rotate dragging positive end to nearly vertical (~91.5 degrees)
        // Pivot is (0, 200). Drag point (5, 600) -> dy = 400, dx = 5 -> angle ~ 89.3° -> snaps to 90.0°
        val rotated = dragRulerEnd(ruler, Offset(5f, 600f), draggingPositiveEnd = true)
        assertEquals(90f, rotated.rotationDeg, 0.01f)
    }
}
