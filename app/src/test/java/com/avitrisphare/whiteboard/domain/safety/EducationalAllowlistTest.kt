package com.avitrisphare.whiteboard.domain.safety

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying Acceptance Criteria:
 * "Filter does not block legitimate educational terms that might be flagged (e.g. anatomy terms)"
 */
class EducationalAllowlistTest {

    private lateinit var filter: ProfanityFilter

    @Before
    fun setUp() {
        filter = ProfanityFilter()
    }

    @Test
    fun englishAnatomyAndBiologyTermsAreNotBlocked() = runBlocking {
        val educationalSentences = listOf(
            "The human reproductive system includes penis, testicles, and sperm in males.",
            "In females, ovaries produce eggs which travel to the uterus and vagina.",
            "Human breast tissue contains mammary glands that produce milk.",
            "The digestive system ends with the rectum and anus.",
            "Sexual reproduction involves the fusion of male and female gametes.",
            "Today's biology lesson covers sex education, puberty, and hormones.",
            "Erection and ejaculation are physiological processes in human reproduction.",
            "The fallopian tube connects the ovary to the uterus for fertilization.",
            "The excretory system includes the kidneys, ureters, bladder, and urethra."
        )

        for (text in educationalSentences) {
            val result = filter.checkContent(text, FilterContentType.HOMEWORK)
            assertFalse(
                "Educational content was incorrectly blocked: '$text'",
                result.isBlocked
            )
            assertTrue(result.isAllowed)
            assertNull(result.userMessage)
        }
    }

    @Test
    fun hindiEducationalAndAnatomyTermsAreNotBlocked() = runBlocking {
        val hindiEducational = listOf(
            "मानव प्रजनन तंत्र में लिंग, अंडकोश और शुक्राणु मुख्य अंग हैं।",
            "महिला प्रजनन तंत्र में गर्भाशय, अंडाशय और योनि शामिल हैं।",
            "जीवविज्ञान की कक्षा में आज हम शरीर रचना और प्रजनन के बारे में पढ़ेंगे।",
            "स्तन ग्रंथियां नवजात शिशु के पोषण के लिए दूध बनाती हैं।",
            "उत्सर्जन तंत्र में वृक्क और गुदा का महत्वपूर्ण कार्य होता है।"
        )

        for (text in hindiEducational) {
            val result = filter.checkContent(text, FilterContentType.AI_INPUT)
            assertFalse(
                "Hindi educational content was incorrectly blocked: '$text'",
                result.isBlocked
            )
            assertTrue(result.isAllowed)
        }
    }

    @Test
    fun marathiEducationalAndAnatomyTermsAreNotBlocked() = runBlocking {
        val marathiEducational = listOf(
            "मानवी प्रजनन संस्थेमध्ये लिंग आणि शुक्राणू यांची महत्त्वपूर्ण भूमिका असते.",
            "स्त्री प्रजनन संस्थेत गर्भाशय, अंडाशय आणि योनी यांचा समावेश होतो.",
            "जीवशास्त्राच्या पाठात आज आपण शरीरशास्त्र आणि मानवी अवयव शिकणार आहोत.",
            "स्तन ग्रंथी बालकाच्या पोषणासाठी उपयुक्त असतात."
        )

        for (text in marathiEducational) {
            val result = filter.checkContent(text, FilterContentType.AI_INPUT)
            assertFalse(
                "Marathi educational content was incorrectly blocked: '$text'",
                result.isBlocked
            )
            assertTrue(result.isAllowed)
        }
    }

    @Test
    fun scunthorpeWordsAreNeverFalselyBlocked() = runBlocking {
        val scunthorpeSentences = listOf(
            "Please attend today's mathematics class in the classroom.",
            "Students should submit their assignment after completing the assessment.",
            "The teacher will assess your homework document carefully.",
            "The airplane pilot sat inside the cockpit during the flight.",
            "We saw a beautiful peacock walking on the green grass.",
            "Students need a hall pass to leave the class.",
            "We read a classic novel by Charles Dickens.",
            "The assistant gave an analytical explanation of the physics problem."
        )

        for (text in scunthorpeSentences) {
            val result = filter.checkContent(text, FilterContentType.LIVE_CLASS_CHAT)
            assertFalse(
                "Legitimate standard word triggered false positive: '$text'",
                result.isBlocked
            )
            assertTrue(result.isAllowed)
        }
    }
}
