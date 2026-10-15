package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartWriteTest {
    @Test
    fun clustersNearbyLettersAndStopsAtAGap() {
        val word = listOf(
            ink(0f, 10f, 18f, 40f),
            ink(24f, 8f, 40f, 42f),
            ink(46f, 12f, 70f, 38f)
        )
        val later = ink(400f, 10f, 430f, 40f)
        val cluster = handwritingCluster(word + later)
        assertEquals(1, cluster.size)
        assertEquals(later.id, cluster.first().id)

        val onlyWord = handwritingCluster(word)
        assertEquals(3, onlyWord.size)
        assertEquals(word.map { it.id }, onlyWord.map { it.id })
    }

    @Test
    fun scoresClearWordsAboveTheWarningLine() {
        assertFalse(isLowConfidence(scoreRecognition("Lokeek")))
        assertFalse(isLowConfidence(scoreRecognition("मराठी")))
        assertTrue(isLowConfidence(scoreRecognition("?")))
        assertTrue(isLowConfidence(scoreRecognition("a_")))
        assertFalse(isLowConfidence(0f))
        assertEquals(listOf("lokeek", "Lokeek"), candidateTexts("lokeek"))
    }

    @Test
    fun replacementSitsOnTheHandwritingAndCanBeUndoneByKeepingTheOldStrokes() {
        val strokes = listOf(ink(10f, 20f, 80f, 70f), ink(90f, 18f, 140f, 68f))
        val typed = typedTextStroke(strokes, "Lokeek", 0xFF111111.toInt())
        assertEquals(StrokeType.TEXT, typed.strokeType)
        assertEquals("Lokeek", typed.text)
        val bounds = strokes.map { it.bounds }.reduce { acc, rect ->
            androidx.compose.ui.geometry.Rect(
                minOf(acc.left, rect.left),
                minOf(acc.top, rect.top),
                maxOf(acc.right, rect.right),
                maxOf(acc.bottom, rect.bottom)
            )
        }
        assertEquals(bounds.left, typed.points.first().x)
        assertEquals(bounds.top, typed.points.first().y)
        assertEquals(0xFF111111.toInt(), typed.colorValue)
        assertTrue(typed.strokeWidth >= 36f)
    }

    @Test
    fun aPlainLineIsNotHandwriting() {
        val line = Stroke(
            points = listOf(Offset(0f, 40f), Offset(80f, 42f), Offset(160f, 39f)),
            strokeType = StrokeType.PENCIL,
            strokeWidth = 4f
        )
        assertTrue(readableHandwriting(listOf(line)).isEmpty())

        val word = listOf(
            Stroke(
                points = listOf(
                    Offset(0f, 30f),
                    Offset(12f, 4f),
                    Offset(24f, 32f),
                    Offset(36f, 6f),
                    Offset(48f, 30f)
                ),
                strokeType = StrokeType.PENCIL,
                strokeWidth = 4f
            )
        )
        val underline = Stroke(
            points = listOf(Offset(0f, 70f), Offset(80f, 72f), Offset(150f, 69f)),
            strokeType = StrokeType.PENCIL,
            strokeWidth = 4f
        )
        val readable = readableHandwriting(word + underline)
        assertEquals(word.map { it.id }, readable.map { it.id })
    }

    @Test
    fun languageTagsCoverEnglishHindiAndMarathi() {
        assertEquals(SmartWriteScript.LATIN, smartWriteLanguage("en").script)
        assertEquals(SmartWriteScript.DEVANAGARI, smartWriteLanguage("hi").script)
        assertEquals(SmartWriteScript.DEVANAGARI, smartWriteLanguage("mr").script)
        assertEquals("en", smartWriteLanguage("nope").tag)
    }

    private fun ink(x1: Float, y1: Float, x2: Float, y2: Float): Stroke {
        return Stroke(
            points = listOf(Offset(x1, y1), Offset(x2, y2)),
            strokeType = StrokeType.PENCIL,
            strokeWidth = 6f
        )
    }
}
