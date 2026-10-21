package com.avitrisphare.whiteboard.presentation.canvas.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    private val degEngine = CalculatorEngine(isDegreeMode = true)
    private val radEngine = CalculatorEngine(isDegreeMode = false)

    @Test
    fun testBasicArithmetic() {
        assertSuccess(degEngine.evaluate("2 + 3"), 5.0, "5")
        assertSuccess(degEngine.evaluate("10 - 4"), 6.0, "6")
        assertSuccess(degEngine.evaluate("6 * 7"), 42.0, "42")
        assertSuccess(degEngine.evaluate("6 × 7"), 42.0, "42")
        assertSuccess(degEngine.evaluate("15 / 3"), 5.0, "5")
        assertSuccess(degEngine.evaluate("15 ÷ 3"), 5.0, "5")
        assertSuccess(degEngine.evaluate("14 % 4"), 2.0, "2")
    }

    @Test
    fun testOperatorPrecedence() {
        assertSuccess(degEngine.evaluate("2 + 3 * 4"), 14.0, "14")
        assertSuccess(degEngine.evaluate("(2 + 3) * 4"), 20.0, "20")
        assertSuccess(degEngine.evaluate("100 - 20 / 2"), 90.0, "90")
        assertSuccess(degEngine.evaluate("(100 - 20) / 2"), 40.0, "40")
    }

    @Test
    fun testPowerAndFactorial() {
        assertSuccess(degEngine.evaluate("2^3"), 8.0, "8")
        assertSuccess(degEngine.evaluate("5!"), 120.0, "120")
        assertSuccess(degEngine.evaluate("0!"), 1.0, "1")
        assertSuccess(degEngine.evaluate("3! + 4!"), 30.0, "30")
    }

    @Test
    fun testUnaryMinus() {
        assertSuccess(degEngine.evaluate("-5 + 12"), 7.0, "7")
        assertSuccess(degEngine.evaluate("5 * -2"), -10.0, "-10")
        assertSuccess(degEngine.evaluate("-(3 + 4) * 2"), -14.0, "-14")
    }

    @Test
    fun testRootsAndLogs() {
        assertSuccess(degEngine.evaluate("sqrt(16)"), 4.0, "4")
        assertSuccess(degEngine.evaluate("√25"), 5.0, "5")
        assertSuccess(degEngine.evaluate("cbrt(27)"), 3.0, "3")
        assertSuccess(degEngine.evaluate("log(100)"), 2.0, "2")
        assertSuccess(degEngine.evaluate("ln(e)"), 1.0, "1")
    }

    @Test
    fun testTrigonometryDegreeMode() {
        val sin30 = (degEngine.evaluate("sin(30)") as CalculatorEngine.EvalResult.Success).value
        assertEquals(0.5, sin30, 0.0001)

        val cos60 = (degEngine.evaluate("cos(60)") as CalculatorEngine.EvalResult.Success).value
        assertEquals(0.5, cos60, 0.0001)

        val tan45 = (degEngine.evaluate("tan(45)") as CalculatorEngine.EvalResult.Success).value
        assertEquals(1.0, tan45, 0.0001)
    }

    @Test
    fun testTrigonometryRadianMode() {
        val sinHalfPi = (radEngine.evaluate("sin(pi / 2)") as CalculatorEngine.EvalResult.Success).value
        assertEquals(1.0, sinHalfPi, 0.0001)
    }

    @Test
    fun testPythagoreanTheoremExpression() {
        assertSuccess(degEngine.evaluate("sqrt(3^2 + 4^2)"), 5.0, "5")
    }

    @Test
    fun testDivisionByZero() {
        val res = degEngine.evaluate("10 / 0")
        assertTrue(res is CalculatorEngine.EvalResult.Error)
    }

    @Test
    fun testMismatchedParentheses() {
        val res = degEngine.evaluate("(2 + 3 * 4")
        assertTrue(res is CalculatorEngine.EvalResult.Error)
    }

    private fun assertSuccess(res: CalculatorEngine.EvalResult, expectedVal: Double, expectedFormatted: String) {
        assertTrue("Expected Success but got $res", res is CalculatorEngine.EvalResult.Success)
        val success = res as CalculatorEngine.EvalResult.Success
        assertEquals(expectedVal, success.value, 0.0001)
        assertEquals(expectedFormatted, success.formatted)
    }
}
