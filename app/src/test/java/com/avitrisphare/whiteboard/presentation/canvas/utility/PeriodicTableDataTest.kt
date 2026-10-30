package com.avitrisphare.whiteboard.presentation.canvas.utility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodicTableDataTest {

    @Test
    fun testAll118ElementsPresent() {
        assertEquals("Periodic table must contain exactly 118 elements", 118, PeriodicTableData.elements.size)

        // Check sequential atomic numbers 1..118
        val numbers = PeriodicTableData.elements.map { it.atomicNumber }
        assertEquals((1..118).toList(), numbers)
    }

    @Test
    fun testKeyElements() {
        val hydrogen = PeriodicTableData.elements.first()
        assertEquals(1, hydrogen.atomicNumber)
        assertEquals("H", hydrogen.symbol)
        assertEquals("Hydrogen", hydrogen.name)
        assertEquals(ElementCategory.REACTIVE_NONMETAL, hydrogen.category)

        val iron = PeriodicTableData.elements.find { it.symbol == "Fe" }
        assertNotNull(iron)
        assertEquals(26, iron!!.atomicNumber)
        assertEquals("Iron", iron.name)
        assertEquals(ElementCategory.TRANSITION_METAL, iron.category)

        val oganesson = PeriodicTableData.elements.last()
        assertEquals(118, oganesson.atomicNumber)
        assertEquals("Og", oganesson.symbol)
        assertEquals("Oganesson", oganesson.name)
    }

    @Test
    fun testSearchBySymbolAndName() {
        val searchAu = PeriodicTableData.search("Au", null)
        assertTrue(searchAu.any { it.symbol == "Au" && it.name == "Gold" })

        val searchCarbon = PeriodicTableData.search("carbon", null)
        assertTrue(searchCarbon.any { it.symbol == "C" })

        val searchAtomicNumber = PeriodicTableData.search("79", null)
        assertTrue(searchAtomicNumber.any { it.symbol == "Au" })
    }

    @Test
    fun testSearchWithCategoryFilter() {
        val nobleGases = PeriodicTableData.search("", ElementCategory.NOBLE_GAS)
        assertEquals(6, nobleGases.size) // 6 confirmed noble gases: He, Ne, Ar, Kr, Xe, Rn
        assertTrue(nobleGases.any { it.symbol == "He" })
        assertTrue(nobleGases.any { it.symbol == "Ne" })
        assertTrue(nobleGases.any { it.symbol == "Ar" })
    }
}
