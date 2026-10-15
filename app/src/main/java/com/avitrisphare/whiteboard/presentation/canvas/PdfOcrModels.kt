package com.avitrisphare.whiteboard.presentation.canvas

/**
 * Represents the OCR extraction result for a single PDF page.
 */
data class PdfPageOcr(
    val pageIndex: Int,
    val text: String
)

/**
 * Represents the complete OCR result for an opened PDF document.
 */
data class PdfOcrDocument(
    val documentPath: String,
    val pages: List<PdfPageOcr>
) {
    /**
     * Finds matches for a search query across all pages.
     * Returns a list of (pageIndex, countOfMatches).
     */
    fun search(query: String): List<Pair<Int, Int>> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.trim().lowercase()
        return pages.mapNotNull { page ->
            val count = lowerQuery.toRegex().findAll(page.text.lowercase()).count()
            if (count > 0) page.pageIndex to count else null
        }
    }

    /**
     * Gets extracted text for a specific page.
     */
    fun textForPage(pageIndex: Int): String {
        return pages.firstOrNull { it.pageIndex == pageIndex }?.text.orEmpty()
    }
}
