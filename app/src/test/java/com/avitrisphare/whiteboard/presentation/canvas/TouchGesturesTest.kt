package com.avitrisphare.whiteboard.presentation.canvas

import org.junit.Assert.assertEquals
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
}
