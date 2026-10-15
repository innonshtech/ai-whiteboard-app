package com.avitrisphare.whiteboard.domain.model

enum class CanvasBackground(val key: String) {
    DARK("dark"),
    WHITE("white"),
    LINED("lined"),
    DOTTED("dotted"),
    GRID("grid"),
    /** Squared graph paper: light minor squares and a heavier line every fifth square. */
    GRAPH("graph");

    companion object {
        fun fromKey(key: String): CanvasBackground {
            return entries.find { it.key == key } ?: DARK
        }
    }
}

/** Graph paper replaces the current background. Choosing it again returns to dotted paper. */
fun CanvasBackground.withGraphPaperToggled(): CanvasBackground {
    return if (this == CanvasBackground.GRAPH) CanvasBackground.DOTTED else CanvasBackground.GRAPH
}
