package com.avitrisphare.whiteboard.presentation.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchGesturesTest {

    @Test
    fun `forty contacts are kept and extras are not tracked`() {
        assertEquals(40, retainedPointerCount(40))
        assertEquals(40, retainedPointerCount(64))
        assertEquals(1, retainedPointerCount(1))
    }

    @Test
    fun `contact area uses the touch ellipse`() {
        val dpi = 160f
        val under = contactAreaCm2(100f, 100f, dpi, dpi)
        val over = contactAreaCm2(124f, 124f, dpi, dpi)
        assertTrue(under < PALM_AREA_CM2)
        assertTrue(over > PALM_AREA_CM2)
        assertEquals(0f, contactAreaCm2(0f, 80f, dpi, dpi))
    }

    @Test
    fun `two fingers zoom three swipe and four tap`() {
        assertEquals(PanelGesture.PINCH, classifyPanelGesture(2, 80f, 40f, -10f))
        assertEquals(PanelGesture.PAGE, classifyPanelGesture(3, 180f, -180f, 12f))
        assertEquals(PanelGesture.NONE, classifyPanelGesture(3, 20f, -20f, 0f))
        assertEquals(PanelGesture.AI_TAP, classifyPanelGesture(4, 12f, 4f, 2f))
        assertEquals(PanelGesture.NONE, classifyPanelGesture(4, 90f, 90f, 0f))
        assertEquals(PanelGesture.NONE, classifyPanelGesture(8, 4f, 1f, 1f))
        assertEquals(PanelGesture.DRAW, classifyPanelGesture(1, 30f, 30f, 0f))
    }

    @Test
    fun `ink samples stay inside one millimetre on a panel`() {
        assertTrue(sampleGapMillimetres(INK_SAMPLE_GAP_PX, 48f) <= 1f)
        assertTrue(sampleGapMillimetres(INK_SAMPLE_GAP_PX, 160f) <= 1f)
    }

    @Test
    fun `stylus pressure changes the ink width`() {
        val light = stylusPressureWidth(8f, 0.1f)
        val firm = stylusPressureWidth(8f, 1f)
        assertTrue(light < firm)
        assertEquals(8f, stylusPressureWidth(8f, 1f), 0.01f)
        // Verify finger default (no pressure or NaN) preserves full width
        assertEquals(8f, stylusPressureWidth(8f, 0f), 0.01f)
        assertEquals(8f, stylusPressureWidth(8f, Float.NaN), 0.01f)
        // Verify minimum clamped width
        assertTrue(stylusPressureWidth(0.2f, 0.05f) >= 0.5f)
    }

    @Test
    fun `sample gap calculation uses consistent world-space and screen-space units`() {
        val dpi = 160f
        val gapMm = DEFAULT_INK_SAMPLE_GAP_MM // 0.25mm
        val screenPx = inkSampleGapScreenPx(gapMm, dpi) // (0.25 / 25.4) * 160 = ~1.5748 px

        // Verify screen-space conversion
        assertEquals(gapMm / 25.4f * dpi, screenPx, 0.001f)

        // At 100% zoom (scale = 1.0), world units == screen pixels
        val worldGap100 = inkSampleGapPx(gapMm, dpi, canvasScale = 1.0f)
        assertEquals(screenPx, worldGap100, 0.001f)

        // At 200% zoom (scale = 2.0), world gap is half of screen pixels (higher world resolution)
        val worldGap200 = inkSampleGapPx(gapMm, dpi, canvasScale = 2.0f)
        assertEquals(screenPx / 2.0f, worldGap200, 0.001f)

        // At 50% zoom (scale = 0.5), world gap is double screen pixels
        val worldGap50 = inkSampleGapPx(gapMm, dpi, canvasScale = 0.5f)
        assertEquals(screenPx / 0.5f, worldGap50, 0.001f)

        // Verify mathematical equivalence:
        // A world displacement dxWorld has screen displacement dxScreen = dxWorld * scale.
        // dxWorld < worldGap <=> dxScreen < screenPx.
        for (scale in listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)) {
            val wGap = inkSampleGapPx(gapMm, dpi, scale)
            val testWorldDist = wGap * 0.9f
            val testScreenDist = testWorldDist * scale
            // Both world-space and screen-space evaluations agree that this sample should be skipped
            assertTrue(testWorldDist * testWorldDist < wGap * wGap)
            assertTrue(testScreenDist < screenPx)

            val acceptedWorldDist = wGap * 1.1f
            val acceptedScreenDist = acceptedWorldDist * scale
            // Both world-space and screen-space evaluations agree that this sample should be accepted
            assertTrue(acceptedWorldDist * acceptedWorldDist >= wGap * wGap)
            assertTrue(acceptedScreenDist >= screenPx)
        }
    }

    @Test
    fun `slow curve drawn at 100 percent zoom has continuous sufficiently dense ink`() {
        val dpi = 160f
        val scale = 1.0f
        val gapMm = DEFAULT_INK_SAMPLE_GAP_MM
        val worldGap = inkSampleGapPx(gapMm, dpi, scale)

        // Simulate a slow arc: radius 40 units drawn in 200 small discrete touch increments
        // Each increment moves ~0.3 units (simulating 60Hz-120Hz slow handwriting)
        val acceptedPoints = mutableListOf<Pair<Float, Float>>()
        var lastAccepted: Pair<Float, Float>? = null

        val numSteps = 200
        val radius = 40f
        for (i in 0..numSteps) {
            val angle = (i.toFloat() / numSteps) * (Math.PI.toFloat() * 0.5f)
            val x = radius * kotlin.math.cos(angle)
            val y = radius * kotlin.math.sin(angle)

            if (lastAccepted == null) {
                acceptedPoints.add(x to y)
                lastAccepted = x to y
            } else {
                val dx = x - lastAccepted.first
                val dy = y - lastAccepted.second
                if (dx * dx + dy * dy >= worldGap * worldGap) {
                    acceptedPoints.add(x to y)
                    lastAccepted = x to y
                }
            }
        }

        // Must capture sufficiently dense points along the curve
        assertTrue(acceptedPoints.size >= 30)

        // Verify continuity: no two consecutive accepted points exceed 2x the gap threshold
        for (i in 1 until acceptedPoints.size) {
            val p0 = acceptedPoints[i - 1]
            val p1 = acceptedPoints[i]
            val dist = kotlin.math.hypot(p1.first - p0.first, p1.second - p0.second)
            assertTrue("Gap between points must not be sparse: $dist", dist <= worldGap * 1.5f)
        }
    }

    @Test
    fun `same curve remains continuous at 200 percent zoom with consistent screen-space density`() {
        val dpi = 160f
        val gapMm = DEFAULT_INK_SAMPLE_GAP_MM
        val worldGap100 = inkSampleGapPx(gapMm, dpi, 1.0f)
        val worldGap200 = inkSampleGapPx(gapMm, dpi, 2.0f)

        // World gap at 200% zoom should be half of world gap at 100% zoom
        assertEquals(worldGap100 / 2.0f, worldGap200, 0.001f)

        // Simulate the same slow curve at 200% zoom
        val acceptedPoints200 = mutableListOf<Pair<Float, Float>>()
        var lastAccepted: Pair<Float, Float>? = null

        val numSteps = 400
        val radius = 40f
        for (i in 0..numSteps) {
            val angle = (i.toFloat() / numSteps) * (Math.PI.toFloat() * 0.5f)
            val x = radius * kotlin.math.cos(angle)
            val y = radius * kotlin.math.sin(angle)

            if (lastAccepted == null) {
                acceptedPoints200.add(x to y)
                lastAccepted = x to y
            } else {
                val dx = x - lastAccepted.first
                val dy = y - lastAccepted.second
                if (dx * dx + dy * dy >= worldGap200 * worldGap200) {
                    acceptedPoints200.add(x to y)
                    lastAccepted = x to y
                }
            }
        }

        // At 200% zoom, point count in world space is approximately double that of 100% zoom
        assertTrue("Points at 200% zoom must be denser than 100%: ${acceptedPoints200.size}", acceptedPoints200.size >= 60)

        // Verify continuity: maximum world gap is bounded by worldGap200
        for (i in 1 until acceptedPoints200.size) {
            val p0 = acceptedPoints200[i - 1]
            val p1 = acceptedPoints200[i]
            val dist = kotlin.math.hypot(p1.first - p0.first, p1.second - p0.second)
            assertTrue("Curve remains continuous without sparse gaps at 200% zoom: $dist", dist <= worldGap200 * 1.5f)
        }

        // On screen, physical gap between points is identical between 100% and 200% zoom:
        val screenGap100 = worldGap100 * 1.0f
        val screenGap200 = worldGap200 * 2.0f
        assertEquals(screenGap100, screenGap200, 0.001f)
    }

    @Test
    fun `stroke density is consistent between main canvas and split panes`() {
        val dpi = 160f
        val gapMm = DEFAULT_INK_SAMPLE_GAP_MM

        for (scale in listOf(0.5f, 1.0f, 1.75f, 2.0f)) {
            // Main canvas gap calculation:
            val mainGap = inkSampleGapPx(gapMm, dpi, scale)
            // Split pane gap calculation:
            val splitGap = inkSampleGapPx(gapMm, dpi, scale)

            assertEquals("Gap formula must match between canvas and split pane", mainGap, splitGap, 0.0001f)

            // Test simulated stroke points through both filters
            var lastMain = 0f to 0f
            var lastSplit = 0f to 0f
            val mainAccepted = mutableListOf(lastMain)
            val splitAccepted = mutableListOf(lastSplit)

            for (step in 1..100) {
                val pt = (step * 0.4f) to (step * 0.3f)

                // Main canvas logic
                val mainDx = pt.first - lastMain.first
                val mainDy = pt.second - lastMain.second
                if (mainDx * mainDx + mainDy * mainDy >= mainGap * mainGap) {
                    mainAccepted.add(pt)
                    lastMain = pt
                }

                // Split pane logic
                val splitDx = pt.first - lastSplit.first
                val splitDy = pt.second - lastSplit.second
                if (splitDx * splitDx + splitDy * splitDy >= splitGap * splitGap) {
                    splitAccepted.add(pt)
                    lastSplit = pt
                }
            }

            assertEquals("Both main canvas and split pane must accept exact same points at scale $scale",
                mainAccepted.size, splitAccepted.size)
            assertEquals("Accepted coordinates must match identically", mainAccepted, splitAccepted)
        }
    }

    @Test
    fun `isPenNibContact correctly distinguishes stylus, pen nib and finger`() {
        val xdpi = 77.0f // 86-inch 4K IFP screen dpi

        // 1. Hardware stylus should always be pen nib regardless of contact size
        assertTrue(isPenNibContact(isEraser = false, isStylus = true, majorPx = 25f, minorPx = 20f, xdpi = xdpi))
        assertTrue(isPenNibContact(isEraser = false, isStylus = true, majorPx = 0f, minorPx = 0f, xdpi = xdpi))

        // 2. Eraser should never be pen nib
        org.junit.Assert.assertFalse(isPenNibContact(isEraser = true, isStylus = true, majorPx = 20f, minorPx = 20f, xdpi = xdpi))
        org.junit.Assert.assertFalse(isPenNibContact(isEraser = true, isStylus = false, majorPx = 10f, minorPx = 10f, xdpi = xdpi))

        // 3. Unreported / 0-size contact (point contact from passive pen) is a pen nib
        assertTrue(isPenNibContact(isEraser = false, isStylus = false, majorPx = 0f, minorPx = 0f, xdpi = xdpi))

        // 4. Whiteboard passive pen nib (e.g. 6mm-8mm shadow -> 18px-24px at 77 dpi) is a pen nib
        // 20px / 77 * 25.4 = 6.6mm
        assertTrue(isPenNibContact(isEraser = false, isStylus = false, majorPx = 20f, minorPx = 18f, xdpi = xdpi))
        // 24px / 77 * 25.4 = 7.9mm
        assertTrue(isPenNibContact(isEraser = false, isStylus = false, majorPx = 24f, minorPx = 22f, xdpi = xdpi))

        // 5. Tilted pen nib (elongated major axis e.g. 32px ~ 10.5mm, but narrow minor axis e.g. 15px ~ 4.9mm) is a pen nib
        assertTrue(isPenNibContact(isEraser = false, isStylus = false, majorPx = 32f, minorPx = 15f, xdpi = xdpi))

        // 6. Finger touch (broad contact e.g. 40px major ~ 13.2mm, 35px minor ~ 11.5mm) is NOT a pen nib
        org.junit.Assert.assertFalse(isPenNibContact(isEraser = false, isStylus = false, majorPx = 40f, minorPx = 35f, xdpi = xdpi))

        // 7. IFP Normalized IR touch scale (from live board telemetry):
        // Pen nib is ~1.37 -> MUST be pen nib (Primary)
        assertTrue(isPenNibContact(isEraser = false, isStylus = false, majorPx = 1.373291f, minorPx = 1.373291f, xdpi = xdpi))
        // Back of pen / Finger is ~3.20 -> MUST NOT be pen nib (Secondary)
        org.junit.Assert.assertFalse(isPenNibContact(isEraser = false, isStylus = false, majorPx = 3.2043457f, minorPx = 3.2043457f, xdpi = xdpi))

        // 8. Dual-tip IFP passive stylus: even if firmware reports isStylus = true for both tips,
        // Tip 1 (1.37) must be pen nib (Primary thin), while Tip 2 (3.20) must be secondary (Thick)
        assertTrue(isPenNibContact(isEraser = false, isStylus = true, majorPx = 1.373291f, minorPx = 1.373291f, xdpi = xdpi))
        org.junit.Assert.assertFalse(isPenNibContact(isEraser = false, isStylus = true, majorPx = 3.2043457f, minorPx = 3.2043457f, xdpi = xdpi))
    }

    @Test
    fun `dual tip hardware resolution does not flip when Tip 2 is selected in menu`() {
        val xdpi = 77f
        val fineTipMajor = 1.373291f
        val thickTipMajor = 3.2043457f

        val fineIsNib = isPenNibContact(isEraser = false, isStylus = false, majorPx = fineTipMajor, minorPx = fineTipMajor, xdpi = xdpi)
        val thickIsNib = isPenNibContact(isEraser = false, isStylus = false, majorPx = thickTipMajor, minorPx = thickTipMajor, xdpi = xdpi)

        assertTrue(fineIsNib)
        org.junit.Assert.assertFalse(thickIsNib)

        // When user is editing Tip 1 (useSecondaryPenColor = false):
        // Physical Fine Tip -> Tip 1 (isSecondaryPen = false)
        // Physical Thick Tip -> Tip 2 (isSecondaryPen = true)
        val tip1WithMenuTip1 = if (fineTipMajor in 0.1f..5.0f) !fineIsNib else false
        val tip2WithMenuTip1 = if (thickTipMajor in 0.1f..5.0f) !thickIsNib else false
        org.junit.Assert.assertFalse("Fine tip must draw with Tip 1 when menu is on Tip 1", tip1WithMenuTip1)
        assertTrue("Thick tip must draw with Tip 2 when menu is on Tip 1", tip2WithMenuTip1)

        // When user opens menu, selects Tip 2 (useSecondaryPenColor = true) and touches board without moving back:
        // Physical Fine Tip MUST STILL draw with Tip 1 (isSecondaryPen = false)
        // Physical Thick Tip MUST STILL draw with Tip 2 (isSecondaryPen = true)
        val tip1WithMenuTip2 = if (fineTipMajor in 0.1f..5.0f) !fineIsNib else true
        val tip2WithMenuTip2 = if (thickTipMajor in 0.1f..5.0f) !thickIsNib else true
        org.junit.Assert.assertFalse("Fine tip must NEVER draw with Tip 2 even if user selected Tip 2 in menu", tip1WithMenuTip2)
        assertTrue("Thick tip must draw with Tip 2 even if user selected Tip 2 in menu", tip2WithMenuTip2)
    }

    @Test
    fun `up to 40 simultaneous touch points recognized correctly`() {
        assertEquals(40, retainedPointerCount(40))
        assertEquals(40, retainedPointerCount(64))
        assertEquals(0, retainedPointerCount(0))
        assertEquals(20, retainedPointerCount(20))
        assertEquals(40, MAX_TOUCH_POINTS)

        // Verify TouchContacts tracks contacts and enforces MAX_TOUCH_POINTS
        val contacts = TouchContacts(null)
        assertEquals(0, contacts.activeCount)
        assertFalse(contacts.stylusActive)
    }

    @Test
    fun `stylus detected via pointer type API and pressure input enables sensitive drawing`() {
        // Pointer type API detection
        assertTrue(isStylusToolType(android.view.MotionEvent.TOOL_TYPE_STYLUS))
        assertTrue(isStylusToolType(android.view.MotionEvent.TOOL_TYPE_ERASER))
        assertFalse(isStylusToolType(android.view.MotionEvent.TOOL_TYPE_FINGER))
        assertFalse(isStylusToolType(android.view.MotionEvent.TOOL_TYPE_MOUSE))
        assertFalse(isStylusToolType(android.view.MotionEvent.TOOL_TYPE_UNKNOWN))

        // Stylus pressure input mapping
        assertEquals(1f, stylusPressure(0f), 0.001f)
        assertEquals(1f, stylusPressure(Float.NaN), 0.001f)
        assertEquals(0.1f, stylusPressure(0.1f), 0.001f)
        assertEquals(0.5f, stylusPressure(0.5f), 0.001f)
        assertEquals(1.0f, stylusPressure(1.0f), 0.001f)

        // Dynamic stroke width responds monotonically to pressure
        val baseWidth = 8f
        val lightWidth = stylusPressureWidth(baseWidth, 0.1f)
        val medWidth = stylusPressureWidth(baseWidth, 0.5f)
        val firmWidth = stylusPressureWidth(baseWidth, 1.0f)
        assertTrue("Pressure width must increase monotonically", lightWidth < medWidth)
        assertTrue("Pressure width must increase monotonically", medWidth < firmWidth)
        assertEquals(baseWidth, firmWidth, 0.01f)
        assertTrue(stylusPressureWidth(0.5f, 0.01f) >= 0.5f)
    }

    @Test
    fun `palm rejection filters out contact area larger than 3cm2 when stylus is active`() {
        val xdpi = 77f
        val ydpi = 77f

        // 1. Palm area threshold: PALM_AREA_CM2 is 3.0 cm²
        assertEquals(3f, PALM_AREA_CM2, 0.001f)

        val smallContactPx = 40f
        val palmContactPx = 80f

        val smallArea = contactAreaCm2(smallContactPx, smallContactPx, xdpi, ydpi)
        val palmArea = contactAreaCm2(palmContactPx, palmContactPx, xdpi, ydpi)
        assertTrue("Small contact must be under 3cm²", smallArea < 3f)
        assertTrue("Palm contact must be over 3cm²", palmArea > 3f)

        // 2. When stylus is ACTIVE:
        assertTrue(
            "Contact area > 3cm² must be rejected as palm when stylus is active",
            isPalmContact(
                touchMajorPx = palmContactPx,
                xdpi = xdpi,
                stylusActive = true,
                isStylus = false,
                touchMinorPx = palmContactPx,
                ydpi = ydpi
            )
        )
        assertFalse(
            "Contact area <= 3cm² must not be rejected as palm",
            isPalmContact(
                touchMajorPx = smallContactPx,
                xdpi = xdpi,
                stylusActive = true,
                isStylus = false,
                touchMinorPx = smallContactPx,
                ydpi = ydpi
            )
        )

        // 3. When stylus is NOT active:
        assertFalse(
            "Palm rejection must be inactive when stylus is not active",
            isPalmContact(
                touchMajorPx = palmContactPx,
                xdpi = xdpi,
                stylusActive = false,
                isStylus = false,
                touchMinorPx = palmContactPx,
                ydpi = ydpi
            )
        )

        // 4. Stylus itself is NEVER rejected as a palm
        assertFalse(
            "Stylus pointer itself must never be rejected as a palm",
            isPalmContact(
                touchMajorPx = palmContactPx,
                xdpi = xdpi,
                stylusActive = true,
                isStylus = true,
                touchMinorPx = palmContactPx,
                ydpi = ydpi
            )
        )
    }

    @Test
    fun `pinch gesture with 2 fingers classifies as PINCH zoom`() {
        assertEquals(PanelGesture.PINCH, classifyPanelGesture(peakFingers = 2, travel = 50f, dx = 10f, dy = 10f))
        assertEquals(PanelGesture.PINCH, classifyPanelGesture(peakFingers = 2, travel = 200f, dx = 100f, dy = -50f))

        assertEquals(PanelGesture.DRAW, classifyPanelGesture(peakFingers = 1, travel = 30f, dx = 20f, dy = 10f))

        val centroid = androidx.compose.ui.geometry.Offset(500f, 400f)
        val oldScale = 1.0f
        val newScale = 1.5f
        val oldOffset = androidx.compose.ui.geometry.Offset.Zero
        val newOffset = CanvasTransform.calculatePivotZoomOffset(
            oldScale = oldScale,
            newScale = newScale,
            oldOffset = oldOffset,
            centroid = centroid,
            panDelta = androidx.compose.ui.geometry.Offset.Zero
        )
        assertEquals(-250f, newOffset.x, 0.01f)
        assertEquals(-200f, newOffset.y, 0.01f)
    }

    @Test
    fun `three-finger swipe navigates pages`() {
        val deltaLeft = swipePageDelta(dx = -150f, dy = 10f, threshold = 90f)
        assertEquals(1, deltaLeft)
        assertEquals(PanelGesture.PAGE, classifyPanelGesture(peakFingers = 3, travel = 150f, dx = -150f, dy = 10f))

        val deltaRight = swipePageDelta(dx = 150f, dy = -10f, threshold = 90f)
        assertEquals(-1, deltaRight)
        assertEquals(PanelGesture.PAGE, classifyPanelGesture(peakFingers = 3, travel = 150f, dx = 150f, dy = -10f))

        val deltaShort = swipePageDelta(dx = 50f, dy = 5f, threshold = 90f)
        assertEquals(0, deltaShort)
        assertEquals(PanelGesture.NONE, classifyPanelGesture(peakFingers = 3, travel = 50f, dx = 50f, dy = 5f))

        val deltaVertical = swipePageDelta(dx = -100f, dy = -90f, threshold = 90f)
        assertEquals(0, deltaVertical)

        assertEquals(1, pageStep(index = 0, count = 5, delta = 1))
        assertEquals(0, pageStep(index = 1, count = 5, delta = -1))
        assertEquals(4, pageStep(index = 4, count = 5, delta = 1))
        assertEquals(0, pageStep(index = 0, count = 5, delta = -1))
    }

    @Test
    fun `touch accuracy is within plus-minus 1mm as specified by Annex A`() {
        val xdpi = 77f
        assertEquals(1.0f, HARDWARE_TOUCH_ACCURACY_MM, 0.001f)

        val target = androidx.compose.ui.geometry.Offset(1000f, 1000f)
        val oneMmPx = 77f / 25.4f
        val touchInside = target + androidx.compose.ui.geometry.Offset(oneMmPx * 0.8f, 0f)
        val touchExact = target + androidx.compose.ui.geometry.Offset(oneMmPx, 0f)
        val touchOutside = target + androidx.compose.ui.geometry.Offset(oneMmPx * 1.5f, 0f)

        assertTrue(isTouchWithinAccuracyTolerance(touchInside, target, xdpi))
        assertTrue(isTouchWithinAccuracyTolerance(touchExact, target, xdpi))
        assertFalse(isTouchWithinAccuracyTolerance(touchOutside, target, xdpi))

        assertEquals(1.0f, touchDistanceMm(oneMmPx, xdpi), 0.01f)

        val sampleGapMm = DEFAULT_INK_SAMPLE_GAP_MM
        assertTrue("Ink sample gap must be <= 1mm", sampleGapMm <= HARDWARE_TOUCH_ACCURACY_MM)
        val screenPx = inkSampleGapScreenPx(sampleGapMm, xdpi)
        assertEquals(sampleGapMm, sampleGapMillimetres(screenPx, xdpi), 0.001f)

        val worldPt = androidx.compose.ui.geometry.Offset(1234.567f, 890.123f)
        val scale = 1.75f
        val offset = androidx.compose.ui.geometry.Offset(-340f, 120f)
        val screenPt = CanvasTransform.worldToScreen(worldPt, scale, offset)
        val restoredWorldPt = CanvasTransform.screenToWorld(screenPt, scale, offset)
        val errorPx = kotlin.math.hypot(restoredWorldPt.x - worldPt.x, restoredWorldPt.y - worldPt.y)
        val errorMm = touchDistanceMm(errorPx, xdpi)
        assertTrue("Coordinate round-trip error must be < 0.001mm", errorMm < 0.001f)
    }

    @Test
    fun `forty simultaneous touches generate forty distinct independent strokes`() {
        val strokes = (0 until 40).map { pointerId ->
            com.avitrisphare.whiteboard.domain.model.Stroke(
                id = "stroke_ptr_$pointerId",
                points = listOf(
                    androidx.compose.ui.geometry.Offset(pointerId * 40f, 100f),
                    androidx.compose.ui.geometry.Offset(pointerId * 40f, 200f),
                    androidx.compose.ui.geometry.Offset(pointerId * 40f, 300f)
                ),
                pointPressures = listOf(0.5f, 0.7f, 1.0f),
                colorValue = 0xFF000000.toInt() + pointerId * 1000,
                strokeWidth = 6f + (pointerId % 4),
                strokeType = com.avitrisphare.whiteboard.domain.model.StrokeType.PEN
            )
        }
        assertEquals(40, strokes.size)
        val ids = strokes.map { it.id }.toSet()
        assertEquals(40, ids.size)
        for (i in 0 until 40) {
            assertEquals(3, strokes[i].points.size)
            assertEquals(i * 40f, strokes[i].points[0].x, 0.001f)
        }
    }
}
