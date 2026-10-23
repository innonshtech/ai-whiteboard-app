package com.avitrisphare.whiteboard.presentation.canvas.export

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class WhiteboardIwbExportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBuildCffXmlGeneratesValidSvgAndLayers() {
        val strokes = listOf(
            Stroke(
                points = listOf(Offset(100f, 150f), Offset(120f, 170f), Offset(150f, 200f)),
                colorValue = 0xFFFF0000.toInt(),
                strokeWidth = 4f,
                strokeType = StrokeType.PEN
            ),
            Stroke(
                points = listOf(Offset(50f, 50f), Offset(200f, 150f)),
                colorValue = 0xFF00FF00.toInt(),
                strokeWidth = 2f,
                strokeType = StrokeType.RECTANGLE
            ),
            Stroke(
                points = listOf(Offset(300f, 300f)),
                text = "Welcome to Science Class",
                colorValue = 0xFFFFFFFF.toInt(),
                strokeWidth = 24f,
                strokeType = StrokeType.TEXT
            )
        )

        val xml = WhiteboardExportManager.buildCffXml(
            lessonTitle = "Biology Lesson 1",
            pages = listOf(strokes),
            canvasBackground = CanvasBackground.DARK,
            isDarkCanvas = true
        )

        assertNotNull(xml)
        assertTrue(xml.contains("<iwb"))
        assertTrue(xml.contains("Biology Lesson 1"))
        assertTrue(xml.contains("<page id=\"page_0\">"))
        assertTrue(xml.contains("<svg:path"))
        assertTrue(xml.contains("<svg:rect"))
        assertTrue(xml.contains("<svg:text"))
        assertTrue(xml.contains("Welcome to Science Class"))
    }

    @Test
    fun testParseCffXmlRestoresStrokesAndPages() {
        val sampleCffXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <iwb xmlns="http://www.imsglobal.org/xsd/iwbcff_v1p0" xmlns:svg="http://www.w3.org/2000/svg">
              <head>
                <meta name="title" content="Math Class Geometry" />
              </head>
              <pages>
                <page id="page_0">
                  <svg:svg width="1920" height="1080">
                    <svg:rect width="100%" height="100%" fill="#171717" />
                    <svg:g id="layer_ink_0">
                      <svg:path d="M 50 100 L 150 200 L 250 100" stroke="#FFFFFF" stroke-width="5" />
                      <svg:line x1="10" y1="20" x2="100" y2="200" stroke="#3B82F6" stroke-width="3" />
                      <svg:text x="40" y="80" fill="#FFFF00" font-size="28">Pythagoras</svg:text>
                    </svg:g>
                  </svg:svg>
                </page>
              </pages>
            </iwb>
        """.trimIndent()

        val parsedPkg = WhiteboardExportManager.parseCffXml(sampleCffXml)
        assertEquals("Math Class Geometry", parsedPkg.title)
        assertEquals(1, parsedPkg.pages.size)

        val page = parsedPkg.pages[0]
        val strokes = page.layers.flatMap { it.strokes }
        assertEquals(3, strokes.size)

        val pathStroke = strokes.find { it.strokeType == StrokeType.PEN }
        assertNotNull(pathStroke)
        assertTrue(pathStroke!!.points.contains("50,100"))
        assertTrue(pathStroke.points.contains("150,200"))

        val lineStroke = strokes.find { it.strokeType == StrokeType.LINE }
        assertNotNull(lineStroke)

        val textStroke = strokes.find { it.strokeType == StrokeType.TEXT }
        assertNotNull(textStroke)
        assertEquals("Pythagoras", textStroke!!.text)
    }

    @Test
    fun testIwbZipPackagingAndDualImportCompatibility() {
        val targetDir = tempFolder.newFolder("export_test")
        val strokes = listOf(
            Stroke(
                points = listOf(Offset(10f, 20f), Offset(30f, 40f)),
                colorValue = 0xFF3B82F6.toInt(),
                strokeWidth = 3f,
                strokeType = StrokeType.PEN
            )
        )

        val exportMethod = WhiteboardExportManager::class.java.getDeclaredMethod(
            "exportIwb",
            String::class.java,
            Long::class.javaPrimitiveType,
            String::class.java,
            List::class.java,
            CanvasBackground::class.java,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            com.avitrisphare.whiteboard.presentation.canvas.BoardDocument::class.java,
            Int::class.javaPrimitiveType,
            List::class.java,
            List::class.java,
            File::class.java,
            Function2::class.java
        ).apply { isAccessible = true }

        val exportedFile = exportMethod.invoke(
            WhiteboardExportManager,
            "Physics_Lesson",
            System.currentTimeMillis(),
            "Physics Mechanics",
            listOf(strokes),
            CanvasBackground.DARK,
            true,
            false,
            1f,
            0f,
            0f,
            0,
            null,
            1,
            emptyList<Float>(),
            null,
            targetDir,
            { _: Float, _: String -> }
        ) as File

        assertTrue(exportedFile.exists())
        assertTrue(exportedFile.name.endsWith(".iwb"))

        // Verify it is a valid ZIP archive containing mimetype, content.xml, and gyoash_lesson.json
        val zipFile = ZipFile(exportedFile)
        assertNotNull(zipFile.getEntry("mimetype"))
        assertNotNull(zipFile.getEntry("content.xml"))
        assertNotNull(zipFile.getEntry("gyoash_lesson.json"))
        zipFile.close()

        // Test parsing the exported bytes with WhiteboardExportManager
        val bytes = exportedFile.readBytes()
        val importedPkg = WhiteboardExportManager.parseIwbBytes(bytes)
        assertEquals("Physics Mechanics", importedPkg.title)
        assertEquals(1, importedPkg.pages.size)
        assertEquals(1, importedPkg.pages[0].layers[0].strokes.size)
        assertEquals(0xFF3B82F6.toInt(), importedPkg.pages[0].layers[0].strokes[0].colorValue)
    }
}
