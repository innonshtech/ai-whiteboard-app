package com.avitrisphare.whiteboard.presentation.canvas.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaShieldTest {

    @Test
    fun shield_protectsLatexFormulas() {
        val input = "Calculate the energy where \$E = mc^2\$ and \$m = 5\$."
        val shielded = FormulaShield.shield(input)

        assertTrue(shielded.maskedText.contains("⟦FORMULA_"))
        assertEquals(2, shielded.formulaMap.size)

        // Simulate translation preserving tokens
        val simulatedTranslated = "ऊर्जा की गणना करें जहां ⟦FORMULA_0⟧ और ⟦FORMULA_1⟧ है।"
        val unshielded = shielded.unshield(simulatedTranslated)

        assertEquals("ऊर्जा की गणना करें जहां \$E = mc^2\$ और \$m = 5\$ है।", unshielded)
    }

    @Test
    fun shield_protectsEquationsAndChemicals() {
        val input = "Reaction of 2H2 + O2 = 2H2O produces water."
        val shielded = FormulaShield.shield(input)

        val simulatedTranslated = "⟦FORMULA_0⟧ की प्रतिक्रिया से पानी बनता है।"
        val unshielded = shielded.unshield(simulatedTranslated)

        assertTrue(unshielded.contains("2H2 + O2 = 2H2O"))
    }

    @Test
    fun shield_handlesModelAddingSpacesAroundTokens() {
        val input = "Solve 2x + 5 = 15 for x."
        val shielded = FormulaShield.shield(input)

        // Translation engines often insert spaces around unfamiliar brackets:
        val simulatedWithWhitespace = "x के लिए [ FORMULA_0 ] हल करें।"
        val unshielded = shielded.unshield(simulatedWithWhitespace)

        assertEquals("x के लिए 2x + 5 = 15 हल करें।", unshielded)
    }

    @Test
    fun indianLanguage_hasAllTenLanguages() {
        val languages = IndianLanguage.allIndianLanguages()
        assertEquals(10, languages.size)

        val codes = languages.map { it.code }
        assertTrue(codes.containsAll(listOf("hi", "mr", "gu", "ta", "te", "kn", "bn", "ur", "pa", "ml")))
    }
}
