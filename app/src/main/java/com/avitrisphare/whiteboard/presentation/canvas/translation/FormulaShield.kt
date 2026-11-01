package com.avitrisphare.whiteboard.presentation.canvas.translation

import java.util.regex.Pattern

/**
 * Result of shielding mathematical and scientific formulas before translation.
 */
data class ShieldedText(
    val maskedText: String,
    val formulaMap: Map<String, String>
) {
    fun unshield(translatedText: String): String {
        return FormulaShield.unshield(translatedText, formulaMap)
    }
}

/**
 * FormulaShield protects educational formulas, algebraic equations, variables,
 * numbers, and scientific terms from being garbled or translated literally by
 * offline translation models.
 *
 * Example:
 * Input: "Calculate the value of x when 2x + 5 = 15."
 * Masked: "Calculate the value of x when ⟦FORMULA_0⟧."
 * Translated (Hindi): "जब ⟦FORMULA_0⟧ हो तो x का मान ज्ञात कीजिए।"
 * Unshielded: "जब 2x + 5 = 15 हो तो x का मान ज्ञात कीजिए।"
 */
object FormulaShield {

    private val TOKEN_PREFIX = "⟦FORMULA_"
    private val TOKEN_SUFFIX = "⟧"

    // Regex to match LaTeX blocks: $...$, $$...$$, \(...\), \[...\]
    private val LATEX_PATTERN = Pattern.compile(
        """(?:\$\$[\s\S]+?\$\$|\$[^\$]+?\$|\\\[[\s\S]+?\\\]|\\\([^\)]+?\\\))"""
    )

    // Regex to match equations with equality or comparison: e.g. "2x + 5 = 15", "y = mx + c", "E = mc^2"
    private val EQUATION_PATTERN = Pattern.compile(
        """(?<![A-Za-z0-9])([A-Za-z0-9_()+\-*/^]+(?:\s*[+\-*/^]\s*[A-Za-z0-9_()+\-*/^]+)*)\s*=\s*([A-Za-z0-9_()+\-*/^]+(?:\s*[+\-*/^]\s*[A-Za-z0-9_()+\-*/^]+)*)"""
    )

    // Regex to match common chemical formulas: H2O, CO2, NaCl, H2SO4, CaCO3
    private val CHEMICAL_PATTERN = Pattern.compile(
        """\b(?:[A-Z][a-z]?\d*){2,6}\b"""
    )

    // Regex to match powers/exponents: x^2, y^3, 10^5
    private val EXPONENT_PATTERN = Pattern.compile(
        """\b[A-Za-z0-9]+\^[A-Za-z0-9]+\b"""
    )

    fun shield(rawText: String): ShieldedText {
        if (rawText.isBlank()) return ShieldedText(rawText, emptyMap())

        var text = rawText
        val map = mutableMapOf<String, String>()
        var counter = 0

        // 1. First shield explicit LaTeX blocks
        val latexMatcher = LATEX_PATTERN.matcher(text)
        val sbLatex = StringBuffer()
        while (latexMatcher.find()) {
            val formula = latexMatcher.group()
            val token = "$TOKEN_PREFIX${counter++}$TOKEN_SUFFIX"
            map[token] = formula
            latexMatcher.appendReplacement(sbLatex, MatcherQuote(token))
        }
        latexMatcher.appendTail(sbLatex)
        text = sbLatex.toString()

        // 2. Shield equations (e.g., 2x + 5 = 15)
        val eqMatcher = EQUATION_PATTERN.matcher(text)
        val sbEq = StringBuffer()
        while (eqMatcher.find()) {
            val formula = eqMatcher.group().trim()
            // Ensure it has an actual operator or variable to avoid false positives on plain numbers
            if (formula.contains("=") && formula.length >= 3) {
                val token = "$TOKEN_PREFIX${counter++}$TOKEN_SUFFIX"
                map[token] = formula
                eqMatcher.appendReplacement(sbEq, MatcherQuote(token))
            }
        }
        eqMatcher.appendTail(sbEq)
        text = sbEq.toString()

        // 3. Shield exponents (e.g., x^2, 10^4)
        val expMatcher = EXPONENT_PATTERN.matcher(text)
        val sbExp = StringBuffer()
        while (expMatcher.find()) {
            val formula = expMatcher.group()
            val token = "$TOKEN_PREFIX${counter++}$TOKEN_SUFFIX"
            map[token] = formula
            expMatcher.appendReplacement(sbExp, MatcherQuote(token))
        }
        expMatcher.appendTail(sbExp)
        text = sbExp.toString()

        // 4. Shield chemical formulas (e.g., H2O, CO2, NaCl, CaCO3)
        val chemMatcher = CHEMICAL_PATTERN.matcher(text)
        val sbChem = StringBuffer()
        while (chemMatcher.find()) {
            val formula = chemMatcher.group()
            // Filter out common English acronyms like AI, IFP, PDF, OK, PPT
            val ignoredAcronyms = setOf("AI", "IFP", "PDF", "OK", "PPT", "PPTX", "SVG", "CBSE", "ICSE", "RAM", "CPU", "OTP", "QR", "UI")
            if (formula !in ignoredAcronyms && formula.any { it.isDigit() }) {
                val token = "$TOKEN_PREFIX${counter++}$TOKEN_SUFFIX"
                map[token] = formula
                chemMatcher.appendReplacement(sbChem, MatcherQuote(token))
            }
        }
        chemMatcher.appendTail(sbChem)
        text = sbChem.toString()

        return ShieldedText(text, map)
    }

    fun unshield(translatedText: String, formulaMap: Map<String, String>): String {
        if (formulaMap.isEmpty() || translatedText.isBlank()) return translatedText

        var result = translatedText
        for ((token, formula) in formulaMap) {
            // Match token with potential whitespace inserted by translation models
            // e.g. "⟦ FORMULA_0 ⟧" or "⟦FORMULA_0⟧"
            val indexNum = token.removePrefix(TOKEN_PREFIX).removeSuffix(TOKEN_SUFFIX)
            val flexiblePattern = Pattern.compile("""[⟦\[]\s*FORMULA_\s*$indexNum\s*[⟧\]]""")
            val matcher = flexiblePattern.matcher(result)
            result = matcher.replaceAll(MatcherQuote(formula))
        }
        return result
    }

    private fun MatcherQuote(str: String): String {
        return java.util.regex.Matcher.quoteReplacement(str)
    }
}

