package com.avitrisphare.whiteboard.presentation.canvas.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityToolModelsTest {

    @Test
    fun testTimerUiStateFormatting() {
        val timer1 = TimerUiState(totalSeconds = 300, remainingSeconds = 300)
        assertEquals("05:00", timer1.formattedTime)
        assertEquals(1.0f, timer1.progress, 0.001f)

        val timer2 = TimerUiState(totalSeconds = 300, remainingSeconds = 150)
        assertEquals("02:30", timer2.formattedTime)
        assertEquals(0.5f, timer2.progress, 0.001f)

        val timer3 = TimerUiState(totalSeconds = 60, remainingSeconds = 7)
        assertEquals("00:07", timer3.formattedTime)
    }

    @Test
    fun testStopwatchUiStateFormatting() {
        val sw1 = StopwatchUiState(elapsedMillis = 0L)
        assertEquals("00:00.00", sw1.formattedTime)

        val sw2 = StopwatchUiState(elapsedMillis = 65430L) // 1m 5s 430ms -> 01:05.43
        assertEquals("01:05.43", sw2.formattedTime)

        val sw3 = StopwatchUiState(elapsedMillis = 3600000L) // 60 mins -> 60:00.00
        assertEquals("60:00.00", sw3.formattedTime)
    }

    @Test
    fun testCalculatorUiStateDefaults() {
        val calc = CalculatorUiState()
        assertEquals("", calc.expression)
        assertEquals("0", calc.result)
        assertFalse(calc.isScientific)
        assertTrue(calc.isDegreeMode)
    }
}
