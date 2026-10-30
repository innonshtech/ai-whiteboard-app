package com.avitrisphare.whiteboard.presentation.canvas

import kotlin.math.abs

enum class DocumentKind { PDF, PPT }

/**
 * The file the teacher opened is only a background. Page notes live in the
 * whiteboard stroke lists and are never written back into that file.
 */
data class BoardDocument(
    val name: String,
    val kind: DocumentKind,
    val path: String,
    val pageWidth: Float,
    val pageHeight: Float
)

fun documentKind(name: String, mime: String?): DocumentKind? {
    val lower = name.lowercase()
    val type = mime?.lowercase().orEmpty()
    if (lower.endsWith(".pdf") || type == "application/pdf") return DocumentKind.PDF
    val powerpoint = lower.endsWith(".pptx") ||
        lower.endsWith(".ppt") ||
        type.contains("powerpoint") ||
        type.contains("presentation")
    if (powerpoint) return DocumentKind.PPT
    return null
}

/** Swipe left moves to the next page. A short or mostly vertical drag stays put. */
fun swipePageDelta(dx: Float, dy: Float, threshold: Float = 120f): Int {
    if (abs(dx) < threshold || abs(dx) < abs(dy) * 1.4f) return 0
    return if (dx < 0f) 1 else -1
}

fun pptAdvancesOnTap(kind: DocumentKind?, movedPx: Float, hitStroke: Boolean): Boolean {
    return kind == DocumentKind.PPT && movedPx <= 18f && !hitStroke
}

fun pageStep(index: Int, count: Int, delta: Int): Int {
    if (count <= 0) return 0
    return (index + delta).coerceIn(0, count - 1)
}
