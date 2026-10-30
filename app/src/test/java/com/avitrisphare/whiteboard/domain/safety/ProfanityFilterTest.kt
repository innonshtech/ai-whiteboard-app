package com.avitrisphare.whiteboard.domain.safety

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfanityFilterTest {

    private lateinit var filter: ProfanityFilter

    @Before
    fun setUp() {
        filter = ProfanityFilter()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENGLISH PROFANITY TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun englishProfanityIsBlockedWithExactUserFacingMessage() = runBlocking {
        val badMessages = listOf(
            "This is total bullshit",
            "What the fuck are you doing",
            "Stop being such a bitch",
            "He is an absolute asshole",
            "Shut up you bastard"
        )

        for (msg in badMessages) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertTrue("Expected '$msg' to be blocked", result.isBlocked)
            assertFalse(result.isAllowed)
            assertEquals(
                "Inappropriate content detected. Please revise.",
                result.userMessage
            )
            assertTrue(result.detectedLanguages.contains(ProfanityLanguage.ENGLISH))
            assertTrue(result.matchedWords.isNotEmpty())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HINDI PROFANITY TESTS (Devanagari & Hinglish)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun hindiDevanagariProfanityIsBlockedWithExactMessage() = runBlocking {
        val badMessages = listOf(
            "तुम बहुत बड़े चूतिया हो",
            "अरे भोसड़ी के इधर आओ",
            "मादरचोद क्या कर रहा है",
            "बहनचोद चुप बैठो",
            "तू बहुत बड़ा हरामी और कमीना है",
            "वो एक रंडी है"
        )

        for (msg in badMessages) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertTrue("Expected Hindi Devanagari profanity to be blocked: '$msg'", result.isBlocked)
            assertEquals("Inappropriate content detected. Please revise.", result.userMessage)
            assertTrue(result.detectedLanguages.contains(ProfanityLanguage.HINDI))
        }
    }

    @Test
    fun hindiLatinScriptProfanityIsBlockedWithExactMessage() = runBlocking {
        val badMessages = listOf(
            "Tu bohot bada chutiya hai",
            "Abey bhosdike idhar mat aa",
            "Saala madarchod kahi ka",
            "Bhenchod chup kar",
            "Kameena and harami insaan",
            "Tu ek randi hai",
            "Gandu chup baith"
        )

        for (msg in badMessages) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertTrue("Expected Hinglish profanity to be blocked: '$msg'", result.isBlocked)
            assertEquals("Inappropriate content detected. Please revise.", result.userMessage)
            assertTrue(result.detectedLanguages.contains(ProfanityLanguage.HINDI))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MARATHI PROFANITY TESTS (Devanagari & Latin)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun marathiDevanagariProfanityIsBlockedWithExactMessage() = runBlocking {
        val badMessages = listOf(
            "तू खूप मोठा झाव्या आहेस",
            "आईघाला काय करतोयस",
            "तो भडव्या कुठला",
            "तो गांडू आहे",
            "चावटपणा बंद कर भामटा"
        )

        for (msg in badMessages) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertTrue("Expected Marathi Devanagari profanity to be blocked: '$msg'", result.isBlocked)
            assertEquals("Inappropriate content detected. Please revise.", result.userMessage)
            assertTrue(result.detectedLanguages.contains(ProfanityLanguage.MARATHI))
        }
    }

    @Test
    fun marathiLatinScriptProfanityIsBlockedWithExactMessage() = runBlocking {
        val badMessages = listOf(
            "Tu motha zavya ahes",
            "Aaighalya kay kartoys",
            "Bhadva kuthe gela",
            "To ek chavat bhamta ahe"
        )

        for (msg in badMessages) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertTrue("Expected Marathi Latin profanity to be blocked: '$msg'", result.isBlocked)
            assertEquals("Inappropriate content detected. Please revise.", result.userMessage)
            assertTrue(result.detectedLanguages.contains(ProfanityLanguage.MARATHI))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBFUSCATION & LEETSPEAK TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun obfuscatedAndLeetspeakProfanityIsBlocked() = runBlocking {
        val obfuscated = listOf(
            "f.u.c.k this",
            "f_u_c_k",
            "b!tch please",
            "b1tch",
            "sh!t happens",
            "fuuuuuck",
            "chuuuutiyaaa"
        )

        for (msg in obfuscated) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertTrue("Expected obfuscated profanity to be blocked: '$msg'", result.isBlocked)
            assertEquals("Inappropriate content detected. Please revise.", result.userMessage)
        }
    }

    @Test
    fun cleanSafeContentIsAllowed() = runBlocking {
        val cleanMessages = listOf(
            "Good morning teacher, here is my homework.",
            "Please explain Newton's third law of motion.",
            "Can we solve this calculus equation on the board?",
            "आजचा गृहपाठ काय आहे?",
            "सर मला गणिताचा प्रश्न समजला नाही."
        )

        for (msg in cleanMessages) {
            val result = filter.checkContent(msg, FilterContentType.LIVE_CLASS_CHAT)
            assertFalse("Expected clean message to NOT be blocked: '$msg'", result.isBlocked)
            assertTrue(result.isAllowed)
            assertEquals(null, result.userMessage)
            assertTrue(result.matchedWords.isEmpty())
        }
    }
}
