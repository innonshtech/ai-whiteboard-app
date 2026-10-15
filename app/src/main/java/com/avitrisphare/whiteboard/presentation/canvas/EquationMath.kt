package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Rect
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

sealed class Formula {
    data class Row(val parts: List<Formula>) : Formula()
    data class Atom(val text: String) : Formula()
    data class Sup(val base: Formula, val exponent: Formula) : Formula()
    data class Frac(val numerator: Formula, val denominator: Formula) : Formula()
    data class Sqrt(val body: Formula) : Formula()
}

data class FormulaSolution(
    val latex: String,
    val mathml: String,
    val steps: List<String>,
    val answerLatex: String,
    val solved: Boolean
)

private data class Term(val coefficient: Double, val variable: String?, val power: Int)

private data class Glyph(val width: Float, val height: Float, val baseline: Float)

private val greekWords = mapOf(
    "alpha" to "\\alpha ",
    "beta" to "\\beta ",
    "gamma" to "\\gamma ",
    "delta" to "\\delta ",
    "theta" to "\\theta ",
    "lambda" to "\\lambda ",
    "mu" to "\\mu ",
    "pi" to "\\pi ",
    "sigma" to "\\sigma ",
    "omega" to "\\omega "
)

private val superscripts = mapOf(
    '⁰' to "0",
    '¹' to "1",
    '²' to "2",
    '³' to "3",
    '⁴' to "4",
    '⁵' to "5",
    '⁶' to "6",
    '⁷' to "7",
    '⁸' to "8",
    '⁹' to "9"
)

/** Turns a vision reading into LaTeX the formula renderer and the solver can share. */
fun normalizeMathReading(raw: String): String {
    var text = raw.trim()
    if (text.isEmpty()) return ""
    superscripts.forEach { (symbol, digit) ->
        text = text.replace(symbol.toString(), "^{$digit}")
    }
    text = text.replace("−", "-").replace("–", "-").replace("×", "\\times ")
        .replace("÷", "\\div ").replace("·", "\\cdot ").replace("√", "\\sqrt ")
        .replace("∫", "\\int ").replace("π", "\\pi ").replace("α", "\\alpha ")
        .replace("β", "\\beta ").replace("γ", "\\gamma ").replace("Δ", "\\Delta ")
        .replace("δ", "\\delta ").replace("θ", "\\theta ").replace("λ", "\\lambda ")
        .replace("μ", "\\mu ").replace("σ", "\\sigma ").replace("Ω", "\\omega ")
        .replace("ω", "\\omega ")
    greekWords.forEach { (word, command) ->
        text = Regex("\\b$word\\b", RegexOption.IGNORE_CASE).replace(text) { command }
    }
    text = text.replace(Regex("([A-Za-z])([0-9])(?![0-9])")) { match ->
        "${match.groupValues[1]}^{${match.groupValues[2]}}"
    }
    text = text.replace(Regex("sqrt\\s*\\(([^)]+)\\)", RegexOption.IGNORE_CASE)) { match ->
        "\\sqrt{${match.groupValues[1]}}"
    }
    text = text.replace(Regex("\\\\sqrt\\s+([A-Za-z0-9]+)")) { match ->
        "\\sqrt{${match.groupValues[1]}}"
    }
    text = text.replace(Regex("([A-Za-z0-9]+)\\s*/\\s*([A-Za-z0-9]+)")) { match ->
        "\\frac{${match.groupValues[1]}}{${match.groupValues[2]}}"
    }
    return text.replace(Regex("\\s+"), " ").trim()
}

/** A long horizontal stroke with ink fully above and below it is a fraction bar. */
fun fractionBar(strokes: List<Stroke>): Stroke? {
    return strokes.filter { stroke ->
        val bounds = stroke.bounds
        bounds.width > 40f && bounds.height * 5f < bounds.width
    }.filter { bar ->
        val above = strokes.any { other ->
            other.id != bar.id && other.bounds.bottom < bar.bounds.top + 2f && overlapsX(other, bar)
        }
        val below = strokes.any { other ->
            other.id != bar.id && other.bounds.top > bar.bounds.bottom - 2f && overlapsX(other, bar)
        }
        above && below
    }.maxByOrNull { it.bounds.width }
}

fun splitFraction(strokes: List<Stroke>, bar: Stroke): Triple<List<Stroke>, List<Stroke>, List<Stroke>> {
    val above = ArrayList<Stroke>()
    val below = ArrayList<Stroke>()
    val beside = ArrayList<Stroke>()
    strokes.forEach { stroke ->
        if (stroke.id == bar.id) return@forEach
        when {
            stroke.bounds.bottom < bar.bounds.top + 2f && overlapsX(stroke, bar) -> above.add(stroke)
            stroke.bounds.top > bar.bounds.bottom - 2f && overlapsX(stroke, bar) -> below.add(stroke)
            else -> beside.add(stroke)
        }
    }
    return Triple(above, below, beside)
}

fun equationFromReadings(whole: String, numerator: String? = null, denominator: String? = null, leading: String? = null): String {
    if (numerator != null && denominator != null) {
        val side = leading?.let { normalizeMathReading(it) }.orEmpty()
        val frac = "\\frac{${normalizeMathReading(numerator)}}{${normalizeMathReading(denominator)}}"
        return if (side.isBlank()) frac else "$side $frac"
    }
    return normalizeMathReading(whole)
}

fun parseFormula(latex: String): Formula {
    if (latex.isBlank()) return Formula.Atom("")
    return LatexCursor(latex).parseSequence()
}

fun formulaToMathMl(formula: Formula): String {
    return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">${mathMlBody(formula)}</math>"
}

fun formulaPadding(size: Float): Float = size * 0.45f

fun strokeSelectionBounds(stroke: Stroke): Rect {
    if (stroke.strokeType == StrokeType.EQUATION && stroke.points.isNotEmpty()) {
        val origin = stroke.points.first()
        val size = stroke.strokeWidth.coerceAtLeast(28f)
        val (width, height) = formulaExtent(stroke.text, size)
        return Rect(origin.x, origin.y, origin.x + width, origin.y + height)
    }
    return stroke.bounds
}

fun formulaExtent(latex: String, size: Float): Pair<Float, Float> {
    val box = layoutFormula(parseFormula(latex), size)
    val pad = formulaPadding(size)
    return (box.width + pad * 2f).coerceAtLeast(size) to (box.height + pad * 2f).coerceAtLeast(size)
}

fun drawFormula(canvas: Canvas, formula: Formula, left: Float, top: Float, size: Float, paint: Paint) {
    drawPlaced(canvas, formula, left, top, size, paint)
}

fun solveLatex(latex: String): FormulaSolution {
    val clean = latex.trim()
    val mathml = formulaToMathMl(parseFormula(clean))
    parseIntegral(clean)?.let { return it.copy(mathml = mathml) }
    val equation = polynomialEquation(clean)
    if (equation != null) return solvePolynomial(clean, mathml, equation)
    val value = evaluateArithmetic(clean)
    if (value != null) {
        val shown = formatNumber(value)
        return FormulaSolution(
            latex = clean,
            mathml = mathml,
            steps = listOf(
                "Read the expression as $clean.",
                "Evaluate the arithmetic.",
                "The value is $shown."
            ),
            answerLatex = shown,
            solved = true
        )
    }
    return FormulaSolution(
        latex = clean,
        mathml = mathml,
        steps = listOf("The handwriting is now a digital equation, ready for the formula solver."),
        answerLatex = clean,
        solved = false
    )
}

private fun overlapsX(stroke: Stroke, bar: Stroke): Boolean {
    return stroke.bounds.right > bar.bounds.left - 8f && stroke.bounds.left < bar.bounds.right + 8f
}

private class LatexCursor(val source: String) {
    var index = 0

    fun parseSequence(): Formula {
        val parts = ArrayList<Formula>()
        while (index < source.length) {
            skip()
            if (index >= source.length || source[index] == '}') break
            parts.add(parsePower())
        }
        return when (parts.size) {
            0 -> Formula.Atom("")
            1 -> parts[0]
            else -> Formula.Row(parts)
        }
    }

    fun parsePower(): Formula {
        var base = parseAtom()
        skip()
        if (index < source.length && source[index] == '^') {
            index++
            skip()
            val exponent = if (index < source.length && source[index] == '{') parseGroup() else parseAtom()
            base = Formula.Sup(base, exponent)
        }
        return base
    }

    fun parseAtom(): Formula {
        skip()
        if (index >= source.length) return Formula.Atom("")
        val character = source[index]
        if (character == '{') return parseGroup()
        if (character == '\\') return parseCommand()
        if (character.isDigit() || character == '.') {
            val start = index
            while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
            return Formula.Atom(source.substring(start, index))
        }
        if (character.isLetter()) {
            val start = index
            while (index < source.length && source[index].isLetter()) index++
            return Formula.Atom(source.substring(start, index))
        }
        index++
        return Formula.Atom(character.toString())
    }

    private fun parseGroup(): Formula {
        index++
        val inner = parseSequence()
        if (index < source.length && source[index] == '}') index++
        return inner
    }

    private fun parseCommand(): Formula {
        index++
        val start = index
        while (index < source.length && source[index].isLetter()) index++
        val name = source.substring(start, index)
        return when (name) {
            "frac" -> Formula.Frac(parseGroup(), parseGroup())
            "sqrt" -> Formula.Sqrt(if (peek('{')) parseGroup() else parseAtom())
            "int" -> Formula.Atom("∫")
            "times" -> Formula.Atom("×")
            "div" -> Formula.Atom("÷")
            "cdot" -> Formula.Atom("·")
            "pm" -> Formula.Atom("±")
            "pi" -> Formula.Atom("π")
            "alpha" -> Formula.Atom("α")
            "beta" -> Formula.Atom("β")
            "gamma" -> Formula.Atom("γ")
            "delta", "Delta" -> Formula.Atom("δ")
            "theta" -> Formula.Atom("θ")
            "lambda" -> Formula.Atom("λ")
            "mu" -> Formula.Atom("μ")
            "sigma" -> Formula.Atom("σ")
            "omega" -> Formula.Atom("ω")
            else -> Formula.Atom(name)
        }
    }

    private fun peek(character: Char): Boolean {
        skip()
        return index < source.length && source[index] == character
    }

    private fun skip() {
        while (index < source.length && source[index].isWhitespace()) index++
    }
}

private fun mathMlBody(formula: Formula): String {
    return when (formula) {
        is Formula.Row -> "<mrow>${formula.parts.joinToString("") { mathMlBody(it) }}</mrow>"
        is Formula.Atom -> atomMathMl(formula.text)
        is Formula.Sup -> "<msup>${mathMlBody(formula.base)}${mathMlBody(formula.exponent)}</msup>"
        is Formula.Frac -> "<mfrac>${mathMlBody(formula.numerator)}${mathMlBody(formula.denominator)}</mfrac>"
        is Formula.Sqrt -> "<msqrt>${mathMlBody(formula.body)}</msqrt>"
    }
}

private fun atomMathMl(text: String): String {
    val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return when {
        text.isEmpty() -> "<mrow></mrow>"
        text.all { it.isDigit() || it == '.' } -> "<mn>$escaped</mn>"
        text.length == 1 && !text[0].isLetter() -> "<mo>$escaped</mo>"
        else -> "<mi>$escaped</mi>"
    }
}

private fun layoutFormula(formula: Formula, size: Float): Glyph {
    return when (formula) {
        is Formula.Atom -> {
            val width = if (formula.text == "∫") size * 0.85f else formula.text.length.coerceAtLeast(1) * size * 0.72f
            val height = if (formula.text == "∫") size * 1.7f else size * 1.2f
            val baseline = if (formula.text == "∫") size * 1.2f else size
            Glyph(width, height, baseline)
        }
        is Formula.Row -> {
            if (formula.parts.isEmpty()) return Glyph(size, size, size)
            val boxes = formula.parts.map { layoutFormula(it, size) }
            val above = boxes.maxOf { it.baseline }
            val below = boxes.maxOf { it.height - it.baseline }
            val gap = size * 0.08f
            val width = boxes.sumOf { it.width.toDouble() }.toFloat() + gap * (boxes.size - 1)
            Glyph(width, above + below, above)
        }
        is Formula.Sup -> {
            val base = layoutFormula(formula.base, size)
            val exponent = layoutFormula(formula.exponent, size * 0.62f)
            Glyph(base.width + exponent.width, base.height + exponent.height * 0.35f, base.baseline + exponent.height * 0.35f)
        }
        is Formula.Frac -> {
            val numerator = layoutFormula(formula.numerator, size * 0.72f)
            val denominator = layoutFormula(formula.denominator, size * 0.72f)
            val width = max(numerator.width, denominator.width) + size * 0.35f
            val height = numerator.height + denominator.height + size * 0.28f
            Glyph(width, height, numerator.height + size * 0.12f)
        }
        is Formula.Sqrt -> {
            val body = layoutFormula(formula.body, size)
            Glyph(body.width + size * 0.75f, body.height + size * 0.28f, body.baseline + size * 0.16f)
        }
    }
}

private fun drawPlaced(canvas: Canvas, formula: Formula, left: Float, top: Float, size: Float, paint: Paint) {
    val previousSize = paint.textSize
    val previousStyle = paint.style
    when (formula) {
        is Formula.Atom -> {
            paint.style = Paint.Style.FILL
            paint.textSize = if (formula.text == "∫") size * 1.45f else size
            val box = layoutFormula(formula, size)
            canvas.drawText(formula.text, left, top + box.baseline, paint)
        }
        is Formula.Row -> {
            var cursor = left
            val boxes = formula.parts.map { layoutFormula(it, size) }
            val above = boxes.maxOf { it.baseline }
            formula.parts.forEachIndexed { index, part ->
                val box = boxes[index]
                drawPlaced(canvas, part, cursor, top + above - box.baseline, size, paint)
                cursor += box.width + size * 0.08f
            }
        }
        is Formula.Sup -> {
            val base = layoutFormula(formula.base, size)
            drawPlaced(canvas, formula.base, left, top + layoutFormula(formula, size).baseline - base.baseline, size, paint)
            drawPlaced(canvas, formula.exponent, left + base.width, top, size * 0.62f, paint)
        }
        is Formula.Frac -> {
            val numerator = layoutFormula(formula.numerator, size * 0.72f)
            val denominator = layoutFormula(formula.denominator, size * 0.72f)
            val width = max(numerator.width, denominator.width) + size * 0.35f
            val numLeft = left + (width - numerator.width) / 2f
            val denLeft = left + (width - denominator.width) / 2f
            drawPlaced(canvas, formula.numerator, numLeft, top, size * 0.72f, paint)
            val bar = top + numerator.height + size * 0.06f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (size * 0.045f).coerceAtLeast(1.5f)
            canvas.drawLine(left + size * 0.08f, bar, left + width - size * 0.08f, bar, paint)
            paint.style = Paint.Style.FILL
            drawPlaced(canvas, formula.denominator, denLeft, bar + size * 0.08f, size * 0.72f, paint)
        }
        is Formula.Sqrt -> {
            val body = layoutFormula(formula.body, size)
            val hook = size * 0.7f
            val barY = top + size * 0.12f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (size * 0.05f).coerceAtLeast(1.5f)
            canvas.drawLine(left + hook * 0.15f, barY + body.height * 0.45f, left + hook * 0.42f, barY + body.height * 0.85f, paint)
            canvas.drawLine(left + hook * 0.42f, barY + body.height * 0.85f, left + hook, barY, paint)
            canvas.drawLine(left + hook, barY, left + hook + body.width + size * 0.08f, barY, paint)
            paint.style = Paint.Style.FILL
            drawPlaced(canvas, formula.body, left + hook, top + size * 0.2f, size, paint)
        }
    }
    paint.textSize = previousSize
    paint.style = previousStyle
}

private fun parseIntegral(latex: String): FormulaSolution? {
    val match = Regex("\\\\int\\s*(.*?)\\s*d\\s*([a-zA-Z])\\s*$").find(latex.replace(" ", "")) ?: return null
    val body = match.groupValues[1].ifBlank { "1" }
    val variable = match.groupValues[2]
    val terms = parseTerms(body) ?: return null
    if (terms.any { it.variable != null && it.variable != variable }) return null
    val integrated = terms.map { term ->
        val power = if (term.variable == null) 0 else term.power
        val next = power + 1
        val coefficient = term.coefficient / next
        Term(coefficient, variable, next)
    }
    val answer = termsToLatex(integrated) + " + C"
    val steps = listOf(
        "Treat the integral with respect to $variable.",
        "Raise each power by one and divide by the new power.",
        "Add the constant of integration."
    )
    return FormulaSolution(latex, "", steps, answer, solved = true)
}

private data class SolvedPoly(val variable: String, val coefficients: Map<Int, Double>)

private fun polynomialEquation(latex: String): SolvedPoly? {
    val sides = latex.split("=")
    if (sides.size != 2) return null
    val left = parseTerms(sides[0]) ?: return null
    val right = parseTerms(sides[1]) ?: return null
    val names = (left + right).mapNotNull { it.variable }.toSet()
    if (names.size > 1) return null
    val variable = names.firstOrNull() ?: return null
    val combined = HashMap<Int, Double>()
    fun add(term: Term, sign: Double) {
        val power = if (term.variable == null) 0 else term.power
        combined[power] = (combined[power] ?: 0.0) + sign * term.coefficient
    }
    left.forEach { add(it, 1.0) }
    right.forEach { add(it, -1.0) }
    val kept = combined.filterValues { abs(it) > 1e-8 }
    if (kept.keys.any { it > 2 }) return null
    return SolvedPoly(variable, kept)
}

private fun solvePolynomial(latex: String, mathml: String, poly: SolvedPoly): FormulaSolution {
    val variable = poly.variable
    val a = poly.coefficients[2] ?: 0.0
    val b = poly.coefficients[1] ?: 0.0
    val c = poly.coefficients[0] ?: 0.0
    if (abs(a) < 1e-8 && abs(b) < 1e-8) {
        val holds = abs(c) < 1e-8
        return FormulaSolution(
            latex, mathml,
            listOf(if (holds) "Both sides match." else "The two sides are not equal."),
            if (holds) latex else "no solution",
            solved = true
        )
    }
    if (abs(a) < 1e-8) {
        val value = -c / b
        val shown = formatNumber(value)
        return FormulaSolution(
            latex,
            mathml,
            listOf(
                "This is a linear equation in $variable.",
                "Move the constant to the other side and divide by ${formatNumber(b)}.",
                "$variable = $shown."
            ),
            answerLatex = "$variable = $shown",
            solved = true
        )
    }
    val discriminant = b * b - 4 * a * c
    val steps = arrayListOf(
        "Write it as a$variable² + b$variable + c = 0.",
        "a = ${formatNumber(a)}, b = ${formatNumber(b)}, c = ${formatNumber(c)}.",
        "Discriminant b² − 4ac = ${formatNumber(discriminant)}."
    )
    if (discriminant < -1e-8) {
        steps.add("The discriminant is negative, so there is no real solution.")
        return FormulaSolution(latex, mathml, steps, "no real solution", solved = true)
    }
    val root = sqrt(discriminant.coerceAtLeast(0.0))
    val perfect = abs(root - root.roundToInt()) < 1e-6
    val answer = if (perfect) {
        val radical = root.roundToInt().toDouble()
        val first = (-b + radical) / (2 * a)
        val second = (-b - radical) / (2 * a)
        steps.add("$variable = ${formatNumber(first)} or $variable = ${formatNumber(second)}.")
        "$variable = ${formatNumber(first)} \\quad $variable = ${formatNumber(second)}"
    } else {
        steps.add(
            "$variable = (${formatNumber(-b)} ± √${formatNumber(discriminant)}) / ${formatNumber(2 * a)}."
        )
        "$variable = \\frac{${formatNumber(-b)} \\pm \\sqrt{${formatNumber(discriminant)}}}{${formatNumber(2 * a)}}"
    }
    return FormulaSolution(latex, mathml, steps, answer, solved = true)
}

private fun parseTerms(side: String): List<Term>? {
    val expanded = Regex("\\\\frac\\{(-?\\d+(?:\\.\\d+)?)\\}\\{(-?\\d+(?:\\.\\d+)?)\\}").replace(side) { match ->
        val value = match.groupValues[1].toDouble() / match.groupValues[2].toDouble()
        formatNumber(value)
    }.replace(" ", "")
    if (expanded.isEmpty()) return emptyList()
    val chunks = Regex("(?=[+-])").split(expanded).filter { it.isNotEmpty() }
    val terms = ArrayList<Term>()
    val pattern = Regex("^([+-]?)(\\d*\\.?\\d*)([a-zA-Z])?(?:\\^\\{?(\\d+)\\}?)?$")
    chunks.forEach { chunk ->
        val match = pattern.matchEntire(chunk) ?: return null
        val sign = if (match.groupValues[1] == "-") -1.0 else 1.0
        val number = match.groupValues[2]
        val variable = match.groupValues[3].ifBlank { null }
        val powerText = match.groupValues[4]
        val coefficient = sign * when {
            number.isBlank() && variable != null -> 1.0
            number.isBlank() -> return null
            else -> number.toDouble()
        }
        val power = when {
            variable == null -> 0
            powerText.isBlank() -> 1
            else -> powerText.toInt()
        }
        terms.add(Term(coefficient, variable, power))
    }
    return terms
}

private fun termsToLatex(terms: List<Term>): String {
    return terms.mapIndexed { index, term ->
        val sign = when {
            index == 0 && term.coefficient < 0 -> "-"
            index == 0 -> ""
            term.coefficient < 0 -> " - "
            else -> " + "
        }
        val magnitude = abs(term.coefficient)
        val number = if (abs(magnitude - 1.0) < 1e-8 && term.variable != null) "" else formatNumber(magnitude)
        val body = when {
            term.variable == null -> formatNumber(magnitude)
            term.power == 1 -> "$number${term.variable}"
            else -> "$number${term.variable}^{${term.power}}"
        }
        sign + body
    }.joinToString("")
}

private fun evaluateArithmetic(latex: String): Double? {
    val text = latex.replace("\\times", "*").replace("\\div", "/").replace("\\cdot", "*").replace(" ", "")
    if (text.any { it.isLetter() }) return null
    return try {
        ArithmeticCursor(text).parseSum().also { if (it.second != text.length) error("trailing") }.first
    } catch (_: Exception) {
        null
    }
}

private class ArithmeticCursor(val source: String) {
    fun parseSum(from: Int = 0): Pair<Double, Int> {
        var (value, index) = parseProduct(from)
        while (index < source.length && (source[index] == '+' || source[index] == '-')) {
            val op = source[index]
            val next = parseProduct(index + 1)
            value = if (op == '+') value + next.first else value - next.first
            index = next.second
        }
        return value to index
    }

    private fun parseProduct(from: Int): Pair<Double, Int> {
        var (value, index) = parseNumber(from)
        while (index < source.length && (source[index] == '*' || source[index] == '/')) {
            val op = source[index]
            val next = parseNumber(index + 1)
            value = if (op == '*') value * next.first else value / next.first
            index = next.second
        }
        return value to index
    }

    private fun parseNumber(from: Int): Pair<Double, Int> {
        var index = from
        val start = index
        if (index < source.length && source[index] == '-') index++
        val digits = index
        while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
        if (index == digits) error("number")
        return source.substring(start, index).toDouble() to index
    }
}

private fun formatNumber(value: Double): String {
    val rounded = (value * 10000.0).roundToInt() / 10000.0
    if (abs(rounded - rounded.roundToInt()) < 1e-6) return rounded.roundToInt().toString()
    return rounded.toString().trimEnd('0').trimEnd('.')
}
