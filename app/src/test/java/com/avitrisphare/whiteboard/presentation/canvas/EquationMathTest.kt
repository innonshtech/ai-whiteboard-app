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

    @Test
    fun `step 5 recognition test suite covers all required expressions`() {
        // 1. x² + 2x - 5 = 0
        val quadratic = normalizeMathReading("x² + 2x - 5 = 0")
        assertEquals("x^{2} + 2x - 5 = 0", quadratic)
        val quadraticSolved = solveLatex(quadratic)
        assertTrue(quadraticSolved.solved)
        assertTrue(formulaToMathMl(parseFormula(quadratic)).contains("msup"))

        // 2. a + b = c
        val linearLetters = normalizeMathReading("a + b = c")
        assertEquals("a + b = c", linearLetters)
        assertEquals("a + b", normalizeMathReading("a t b"))
        assertTrue(formulaToMathMl(parseFormula(linearLetters)).contains("mi"))

        // 3. 3/4 + 1/2
        val fractions = normalizeMathReading("3/4 + 1/2")
        assertEquals("\\frac{3}{4} + \\frac{1}{2}", fractions)
        assertTrue(formulaToMathMl(parseFormula(fractions)).contains("mfrac"))

        // 4. √(x + 1) = 5
        val squareRoot = normalizeMathReading("√(x + 1) = 5")
        assertEquals("\\sqrt{x + 1} = 5", squareRoot)
        assertTrue(formulaToMathMl(parseFormula(squareRoot)).contains("msqrt"))

        // 5. x₁ + x₂ = 10
        val subscripts = normalizeMathReading("x₁ + x₂ = 10")
        assertEquals("x_{1} + x_{2} = 10", subscripts)
        assertTrue(formulaToMathMl(parseFormula(subscripts)).contains("msub"))

        // 6. ∫ x² dx
        val integral = normalizeMathReading("∫ x² dx")
        assertEquals("\\int x^{2} dx", integral)
        val integralSolved = solveLatex(integral)
        assertTrue(integralSolved.solved)
        assertTrue(integralSolved.answerLatex.contains("x^{3}"))

        // 7. Equations containing Greek letters
        val greek = normalizeMathReading("α + β = π")
        assertEquals("\\alpha + \\beta = \\pi", greek)
        assertTrue(formulaToMathMl(parseFormula(greek)).contains("α"))

        val greekWords = normalizeMathReading("alpha + beta = pi")
        assertEquals("\\alpha + \\beta = \\pi", greekWords)
    }

    @Test
    fun `pythagorean equation variations normalize correctly`() {
        assertEquals("a^{2} + b^{2} =", normalizeMathReading("a² + b² ="))
        assertEquals("a^{2} + b^{2} =", normalizeMathReading("a2 + b2 ="))
        assertEquals("a^{2} + b^{2} =", normalizeMathReading("a 2 + b 2 ="))
        assertEquals("a^{2} + b^{2} =", normalizeMathReading("a2+b2="))
    }

    @Test
    fun `clusterMathStrokes groups all terms of an equation and handles out-of-order strokes`() {
        // a at x=100
        val s1 = Stroke(points = listOf(Offset(100f, 100f), Offset(130f, 150f)))
        // + at x=200
        val s2 = Stroke(points = listOf(Offset(200f, 110f), Offset(200f, 140f)))
        // b at x=270
        val s3 = Stroke(points = listOf(Offset(270f, 100f), Offset(300f, 150f)))
        // = at x=370
        val s4 = Stroke(points = listOf(Offset(370f, 120f), Offset(410f, 120f)))
        // exponent 2 for a added retroactively near x=135, y=80
        val s5 = Stroke(points = listOf(Offset(135f, 80f), Offset(150f, 95f)))
        // completely unrelated stroke far away at x=1500, y=900
        val sOther = Stroke(points = listOf(Offset(1500f, 900f), Offset(1550f, 950f)))

        val cluster = clusterMathStrokes(listOf(sOther, s1, s2, s3, s4, s5))
        // sOther must NOT be in cluster
        assertTrue(cluster.none { it.id == sOther.id })
        // All 5 equation strokes must be in cluster
        assertEquals(5, cluster.size)
        assertTrue(cluster.any { it.id == s1.id })
        assertTrue(cluster.any { it.id == s2.id })
        assertTrue(cluster.any { it.id == s3.id })
        assertTrue(cluster.any { it.id == s4.id })
        assertTrue(cluster.any { it.id == s5.id })
    }

    @Test
    fun `Y squared plus b squared and Y squared expressions normalize preserving letter case`() {
        assertEquals("Y^{2} + b^{2}", normalizeMathReading("Y² + b²"))
        assertEquals("Y^{2} + b^{2}", normalizeMathReading("Y^2 + b^2"))
        assertEquals("Y^{2} + b^{2}", normalizeMathReading("Y2 + b2"))
        assertEquals("Y^{2} + b^{2}", normalizeMathReading("Y 2 + b 2"))
        assertEquals("y^{2} + b^{2}", normalizeMathReading("y² + b²"))
        assertEquals("y^{2} + b^{2}", normalizeMathReading("y^2 + b^2"))
        assertEquals("Y^{2}", normalizeMathReading("Y²"))
        assertEquals("Y^{2}", normalizeMathReading("Y^2"))
        assertEquals("Y^{2}", normalizeMathReading("Y2"))
        assertEquals("Y^{2}", normalizeMathReading("Y 2"))
        assertEquals("y^{2}", normalizeMathReading("y²"))
        assertEquals("y^{2}", normalizeMathReading("y^2"))
        assertEquals("y^{2}", normalizeMathReading("y2"))
        assertEquals("y^{2}", normalizeMathReading("y 2"))
    }

    @Test
    fun `selectBestMathGuess selects candidate with math exponent when English LM prefers plain letter`() {
        val guessesY2 = listOf(
            HandwritingGuess("y", 0.92f),
            HandwritingGuess("y2", 0.70f),
            HandwritingGuess("Y2", 0.65f)
        )
        val selectedY2 = selectBestMathGuess(guessesY2, totalStrokes = 2)
        assertEquals("y2", selectedY2?.text)

        val guessesUpperY = listOf(
            HandwritingGuess("Y", 0.90f),
            HandwritingGuess("Y2", 0.60f)
        )
        val selectedUpper = selectBestMathGuess(guessesUpperY, totalStrokes = 2)
        assertEquals("Y2", selectedUpper?.text)

        val guessesSuperscriptUnicode = listOf(
            HandwritingGuess("y", 0.95f),
            HandwritingGuess("y²", 0.85f)
        )
        val selectedUni = selectBestMathGuess(guessesSuperscriptUnicode, totalStrokes = 2)
        assertEquals("y²", selectedUni?.text)

        // Single stroke plain letter should stay plain letter
        val singleLetter = listOf(HandwritingGuess("y", 0.90f))
        val selectedSingle = selectBestMathGuess(singleLetter, totalStrokes = 1)
        assertEquals("y", selectedSingle?.text)
    }

    @Test
    fun `detectSuperscriptStrokes accurately detects top-right exponent stroke`() {
        // Base letter Y
        val strokeY1 = Stroke(points = listOf(Offset(100f, 100f), Offset(150f, 200f), Offset(150f, 300f)))
        val strokeY2 = Stroke(points = listOf(Offset(200f, 100f), Offset(150f, 200f)))
        // Exponent 2 in top-right quadrant
        val stroke2 = Stroke(points = listOf(Offset(210f, 90f), Offset(250f, 150f)))

        val result = detectSuperscriptStrokes(listOf(strokeY1, strokeY2, stroke2))
        org.junit.Assert.assertNotNull(result)
        val (base, sup) = result!!
        assertEquals(2, base.size)
        assertEquals(1, sup.size)
        assertEquals(stroke2.id, sup.first().id)

        // Non-superscript side-by-side strokes
        val strokeA = Stroke(points = listOf(Offset(100f, 100f), Offset(100f, 300f)))
        val strokeB = Stroke(points = listOf(Offset(200f, 100f), Offset(200f, 300f)))
        val noSup = detectSuperscriptStrokes(listOf(strokeA, strokeB))
        org.junit.Assert.assertNull(noSup)
    }
}

