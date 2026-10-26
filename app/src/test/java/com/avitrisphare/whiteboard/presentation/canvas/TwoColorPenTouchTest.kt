package com.avitrisphare.whiteboard.presentation.canvas

import android.view.MotionEvent
import androidx.compose.ui.input.pointer.PointerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoColorPenTouchTest {

    @Test
    fun testStylusVsFingerColorResolution() {
        val primaryColor = 0xFF1E293B.toInt()
        val secondaryColor = 0xFF3B82F6.toInt()

        // Stylus pointer (pen nip) resolves to primary color
        val stylusStrokeColor = resolveInkColor(isStylus = true, primary = primaryColor, secondary = secondaryColor)
        assertEquals(primaryColor, stylusStrokeColor)

        // Finger/hand pointer resolves to secondary color
        val fingerStrokeColor = resolveInkColor(isStylus = false, primary = primaryColor, secondary = secondaryColor)
        assertEquals(secondaryColor, fingerStrokeColor)
    }

    @Test
    fun testSwapColorsInvertsPenAndFingerUsage() {
        var primaryColor = 0xFF1E293B.toInt()
        var secondaryColor = 0xFFEF4444.toInt()

        // Before swap
        assertEquals(primaryColor, resolveInkColor(isStylus = true, primary = primaryColor, secondary = secondaryColor))
        assertEquals(secondaryColor, resolveInkColor(isStylus = false, primary = primaryColor, secondary = secondaryColor))

        // Swap colors
        val temp = primaryColor
        primaryColor = secondaryColor
        secondaryColor = temp

        // After swap: Stylus gets new primary (was secondary), Finger gets new secondary (was primary)
        assertEquals(0xFFEF4444.toInt(), resolveInkColor(isStylus = true, primary = primaryColor, secondary = secondaryColor))
        assertEquals(0xFF1E293B.toInt(), resolveInkColor(isStylus = false, primary = primaryColor, secondary = secondaryColor))
    }

    @Test
    fun testStylusDetectionHelper() {
        // ToolType Stylus
        assertTrue(isStylusPointer(toolType = MotionEvent.TOOL_TYPE_STYLUS, pointerType = PointerType.Touch))
        // PointerType Stylus
        assertTrue(isStylusPointer(toolType = MotionEvent.TOOL_TYPE_FINGER, pointerType = PointerType.Stylus))
        // ToolType Eraser
        assertTrue(isStylusPointer(toolType = MotionEvent.TOOL_TYPE_ERASER, pointerType = PointerType.Touch))
        // Normal Finger Touch
        assertFalse(isStylusPointer(toolType = MotionEvent.TOOL_TYPE_FINGER, pointerType = PointerType.Touch))
        // Unknown touch
        assertFalse(isStylusPointer(toolType = MotionEvent.TOOL_TYPE_UNKNOWN, pointerType = PointerType.Touch))
    }

    private fun resolveInkColor(isStylus: Boolean, primary: Int, secondary: Int): Int {
        return if (isStylus) primary else secondary
    }

    private fun isStylusPointer(toolType: Int, pointerType: PointerType): Boolean {
        return toolType == MotionEvent.TOOL_TYPE_STYLUS ||
            toolType == MotionEvent.TOOL_TYPE_ERASER ||
            pointerType == PointerType.Stylus ||
            pointerType == PointerType.Eraser
    }
}
