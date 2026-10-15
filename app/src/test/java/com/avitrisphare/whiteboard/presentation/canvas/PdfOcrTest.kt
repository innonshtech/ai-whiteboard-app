package com.avitrisphare.whiteboard.presentation.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfOcrTest {

    @Test
    fun `search finds matches across pages and reports correct counts`() {
        val doc = PdfOcrDocument(
            documentPath = "/dummy/sample.pdf",
            pages = listOf(
                PdfPageOcr(pageIndex = 0, text = "Photosynthesis is the process used by plants."),
                PdfPageOcr(pageIndex = 1, text = "Cellular respiration occurs in mitochondria."),
                PdfPageOcr(pageIndex = 2, text = "Photosynthesis requires light and chlorophyll. Photosynthesis produces glucose.")
            )
        )

        val results = doc.search("photosynthesis")
        assertEquals(2, results.size)
        assertEquals(0 to 1, results[0])
        assertEquals(2 to 2, results[1])
    }

    @Test
    fun `search is case-insensitive`() {
        val doc = PdfOcrDocument(
            documentPath = "/dummy/sample.pdf",
            pages = listOf(
                PdfPageOcr(pageIndex = 0, text = "NEWTON'S FIRST LAW OF MOTION"),
                PdfPageOcr(pageIndex = 1, text = "newton formulated three laws.")
            )
        )

        val results = doc.search("Newton")
        assertEquals(2, results.size)
        assertEquals(0 to 1, results[0])
        assertEquals(1 to 1, results[1])
    }

    @Test
    fun `empty search query returns empty list`() {
        val doc = PdfOcrDocument(
            documentPath = "/dummy/sample.pdf",
            pages = listOf(
                PdfPageOcr(pageIndex = 0, text = "Some text here")
            )
        )

        assertTrue(doc.search("").isEmpty())
        assertTrue(doc.search("   ").isEmpty())
    }

    @Test
    fun `textForPage returns correct page text or empty`() {
        val doc = PdfOcrDocument(
            documentPath = "/dummy/sample.pdf",
            pages = listOf(
                PdfPageOcr(pageIndex = 0, text = "Page one text"),
                PdfPageOcr(pageIndex = 1, text = "Page two text")
            )
        )

        assertEquals("Page one text", doc.textForPage(0))
        assertEquals("Page two text", doc.textForPage(1))
        assertEquals("", doc.textForPage(99))
    }
}
