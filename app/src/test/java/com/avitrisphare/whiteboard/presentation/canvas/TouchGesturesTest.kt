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
    }
}
