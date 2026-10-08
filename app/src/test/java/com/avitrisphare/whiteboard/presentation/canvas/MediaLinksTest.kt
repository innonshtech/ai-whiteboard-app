package com.avitrisphare.whiteboard.presentation.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLinksTest {
    @Test
    fun `youtube links become an inline embed`() {
        assertEquals(
            "abc123XYZ_-".length,
            11
        )
        val embed = youtubeEmbedUrl("https://www.youtube.com/watch?v=abc123XYZ_-")
        assertEquals(
            "https://www.youtube.com/embed/abc123XYZ_-?autoplay=1&playsinline=1&rel=0",
            embed
        )
        assertEquals("dQw4w9WgXcQ", youtubeVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertNull(youtubeEmbedUrl("not a video"))
    }

    @Test
    fun `web addresses get a scheme`() {
        assertEquals("https://example.com", normalizeWebUrl("example.com"))
        assertEquals("https://example.com", normalizeWebUrl("https://example.com"))
    }
}
