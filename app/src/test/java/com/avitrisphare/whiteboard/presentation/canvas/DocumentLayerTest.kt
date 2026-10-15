package com.avitrisphare.whiteboard.presentation.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentLayerTest {
    @Test
    fun swipeAndTapDecideThePage() {
        assertEquals(1, swipePageDelta(-180f, 20f))
        assertEquals(-1, swipePageDelta(180f, 10f))
        assertEquals(0, swipePageDelta(-40f, 0f))
        assertEquals(0, swipePageDelta(-180f, 160f))
        assertEquals(2, pageStep(1, 5, 1))
        assertEquals(0, pageStep(0, 5, -1))
        assertEquals(4, pageStep(4, 5, 3))
        assertTrue(pptAdvancesOnTap(DocumentKind.PPT, 8f, hitStroke = false))
        assertEquals(false, pptAdvancesOnTap(DocumentKind.PPT, 8f, hitStroke = true))
        assertEquals(false, pptAdvancesOnTap(DocumentKind.PPT, 40f, hitStroke = false))
        assertEquals(false, pptAdvancesOnTap(DocumentKind.PDF, 4f, hitStroke = false))
    }

    @Test
    fun recognisesPdfAndPowerPointNames() {
        assertEquals(DocumentKind.PDF, documentKind("lesson.pdf", null))
        assertEquals(DocumentKind.PDF, documentKind("cloud-file", "application/pdf"))
        assertEquals(DocumentKind.PPT, documentKind("deck.pptx", null))
        assertEquals(DocumentKind.PPT, documentKind("old.ppt", "application/vnd.ms-powerpoint"))
        assertNull(documentKind("photo.jpg", "image/jpeg"))
    }

    @Test
    fun readsSlideOrderTextAndImagesWithoutChangingTheFile() {
        val file = File.createTempFile("lesson", ".pptx")
        file.deleteOnExit()
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("ppt/presentation.xml"))
            zip.write(
                """
                <p:presentation>
                  <p:sldSz cx="9144000" cy="5143500"/>
                  <p:sldIdLst>
                    <p:sldId id="256" r:id="rId2"/>
                    <p:sldId id="257" r:id="rId3"/>
                  </p:sldIdLst>
                </p:presentation>
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ppt/slides/slide2.xml"))
            zip.write(slideXml("Second").toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
            zip.write(slideXml("First &amp; title").toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ppt/slides/_rels/slide1.xml.rels"))
            zip.write(
                """<Relationships><Relationship Id="rId1" Type="image" Target="../media/pic.png"/></Relationships>""".toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("ppt/media/pic.png"))
            zip.write(byteArrayOf(1, 2, 3, 4))
            zip.closeEntry()
        }
        val before = file.length()
        val deck = PptxSlides.read(file)
        assertEquals(before, file.length())
        assertEquals(9144000L, deck.widthEmu)
        assertEquals(5143500L, deck.heightEmu)
        assertEquals(2, deck.slides.size)
        assertEquals("First & title", deck.slides[0].pieces[0].text)
        assertEquals("ppt/media/pic.png", deck.slides[0].pieces[1].imagePath)
        assertEquals(byteArrayOf(1, 2, 3, 4).toList(), deck.slides[0].imageBytes["ppt/media/pic.png"]?.toList())
        assertEquals("Second", deck.slides[1].pieces.first().text)
        assertEquals(1600 to 900, PptxSlides.pixelSize(deck.widthEmu, deck.heightEmu))
    }

    private fun slideXml(title: String): String {
        return """
            <p:sld>
              <p:sp>
                <a:xfrm><a:off x="100" y="200"/><a:ext cx="300" cy="400"/></a:xfrm>
                <a:t>$title</a:t>
              </p:sp>
              <p:pic>
                <a:blip r:embed="rId1"/>
                <a:xfrm><a:off x="10" y="20"/><a:ext cx="30" cy="40"/></a:xfrm>
              </p:pic>
            </p:sld>
        """.trimIndent()
    }
}
