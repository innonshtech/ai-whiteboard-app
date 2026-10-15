package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EquationMathTest {

    @Test
    fun `the sample quadratic becomes latex and solves`() {
        val latex = normalizeMathReading("x² + 2x - 5 = 0")
        assertEquals("x^{2} + 2x - 5 = 0", latex)
        val solved = solveLatex(latex)
        assertTrue(solved.solved)
        assertTrue(solved.answerLatex.contains("\\sqrt{24}"))
        assertTrue(solved.steps.any { it.contains("a = 1") })
        assertTrue(solved.mathml.contains("msup"))
    }

    @Test
    fun `a digit written after a letter is a power`() {
        assertEquals("x^{2} + 2x - 5 = 0", normalizeMathReading("x2 + 2x - 5 = 0"))
    }

    @Test
    fun `fractions roots integrals and greek letters are supported`() {
        assertTrue(normalizeMathReading("1/2").contains("\\frac{1}{2}"))
        assertTrue(formulaToMathMl(parseFormula("\\frac{1}{2}")).contains("mfrac"))
        assertEquals("\\sqrt{x}", normalizeMathReading("√x"))
        assertTrue(formulaToMathMl(parseFormula("\\sqrt{x+1}")).contains("msqrt"))
        val integral = solveLatex(normalizeMathReading("∫ x2 dx"))
        assertTrue(integral.solved)
        assertTrue(integral.answerLatex.contains("x^{3}"))
        assertTrue(integral.answerLatex.contains("C"))
        val greek = normalizeMathReading("alpha + beta = pi")
        assertTrue(greek.contains("\\alpha"))
        assertTrue(greek.contains("\\pi"))
        assertTrue(formulaToMathMl(parseFormula(greek)).contains("α"))
    }

    @Test
    fun `a fraction bar splits the ink above and below it`() {
        val bar = Stroke(points = listOf(Offset(20f, 50f), Offset(120f, 50f)), strokeWidth = 3f)
        val above = Stroke(points = listOf(Offset(60f, 20f), Offset(70f, 30f)), strokeWidth = 3f)
        val below = Stroke(points = listOf(Offset(60f, 70f), Offset(80f, 80f)), strokeWidth = 3f)
        val found = fractionBar(listOf(bar, above, below))
        assertEquals(bar.id, found?.id)
        val (num, den, _) = splitFraction(listOf(bar, above, below), bar)
        assertEquals(listOf(above.id), num.map { it.id })
        assertEquals(listOf(below.id), den.map { it.id })
        assertEquals(
            "\\frac{1}{2}",
            equationFromReadings(whole = "", numerator = "1", denominator = "2")
        )
    }

    @Test
    fun `arithmetic keeps multiply ahead of add`() {
        val solved = solveLatex(normalizeMathReading("2 + 3 × 4"))
        assertEquals("14", solved.answerLatex)
    }
}
