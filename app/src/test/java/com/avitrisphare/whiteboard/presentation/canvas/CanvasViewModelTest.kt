package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.avitrisphare.whiteboard.domain.repository.NoteRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CanvasViewModel
    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<NoteRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CanvasViewModel(context, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.state.value
        assertEquals("Untitled", state.noteTitle)
        assertTrue(state.strokes.isEmpty())
        assertEquals(0xFF1B1F2A.toInt(), state.selectedColor)
        assertEquals(1, state.pageCount)
        assertFalse(state.showGrid)
    }

    @Test
    fun `finishStroke adds current stroke to list`() {
        val stroke = Stroke(
            points = listOf(Offset(0f, 0f), Offset(10f, 10f)),
            colorValue = 0xFF000000.toInt()
        )
        viewModel.startStroke(stroke)
        viewModel.finishStroke()

        val state = viewModel.state.value
        assertEquals(1, state.strokes.size)
        assertEquals(stroke, state.strokes[0])
    }

    @Test
    fun `eraseAt removes stroke when intersecting and enables undo`() {
        val stroke1 = Stroke(
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeWidth = 4f
        )
        val stroke2 = Stroke(
            points = listOf(Offset(0f, 500f), Offset(100f, 500f)),
            strokeWidth = 4f
        )
        viewModel.startStroke(stroke1)
        viewModel.finishStroke()
        viewModel.startStroke(stroke2)
        viewModel.finishStroke()

        assertEquals(2, viewModel.state.value.strokes.size)

        // Erase at (50, 0)
        val erased = viewModel.eraseAt(Offset(50f, 0f), 10f)
        assertTrue(erased)
        viewModel.finishErasing()

        val stateAfterErase = viewModel.state.value
        assertEquals(1, stateAfterErase.strokes.size)
        assertEquals(stroke2, stateAfterErase.strokes[0])
        assertTrue(stateAfterErase.canUndo)

        // Undo restores stroke1
        viewModel.undo()
        assertEquals(2, viewModel.state.value.strokes.size)
    }

    @Test
    fun `clearCanvas empties the page and undo restores it`() {
        val stroke = Stroke(points = listOf(Offset(0f, 0f), Offset(20f, 0f)))
        viewModel.commitStroke(stroke)
        viewModel.clearCanvas()
        assertTrue(viewModel.state.value.strokes.isEmpty())
        assertTrue(viewModel.state.value.canUndo)
        viewModel.undo()
        assertEquals(1, viewModel.state.value.strokes.size)
    }

    @Test
    fun `commitErasedStrokes keeps one undo step for the gesture`() {
        val stroke = Stroke(points = listOf(Offset(0f, 0f), Offset(40f, 0f)))
        viewModel.commitStroke(stroke)
        viewModel.commitErasedStrokes(emptyList())
        assertTrue(viewModel.state.value.strokes.isEmpty())
        viewModel.undo()
        assertEquals(1, viewModel.state.value.strokes.size)
    }

    @Test
    fun `fitToContent updates scale and offset to center strokes`() {
        val stroke = Stroke(
            points = listOf(Offset(100f, 100f), Offset(300f, 300f)),
            strokeWidth = 4f
        )
        viewModel.startStroke(stroke)
        viewModel.finishStroke()

        viewModel.fitToContent(1000f, 1000f)

        val state = viewModel.state.value
        assertTrue(state.scale > 0f)
    }

    @Test
    fun `extractTextFromSelectedImage does nothing if no image is selected`() {
        val initialStrokes = viewModel.state.value.strokes.size
        viewModel.extractTextFromSelectedImage()
        assertEquals(initialStrokes, viewModel.state.value.strokes.size)
        assertFalse(viewModel.state.value.extractTextBusy)
    }

    @Test
    fun `move and resize document frame independently of ink strokes`() {
        val stroke = Stroke(
            points = listOf(Offset(50f, 60f), Offset(200f, 300f)),
            colorValue = 0xFF123456.toInt()
        )
        viewModel.commitStroke(stroke)
        assertEquals(1, viewModel.state.value.strokes.size)

        // Move and resize document frame
        viewModel.setDocumentFrame(120f, 250f, 1.4f)

        val stateAfter = viewModel.state.value
        assertEquals(120f, stateAfter.documentFrameX)
        assertEquals(250f, stateAfter.documentFrameY)
        assertEquals(1.4f, stateAfter.documentFrameScale, 0.001f)

        // Verify existing ink is completely untouched
        assertEquals(1, stateAfter.strokes.size)
        assertEquals(stroke.points, stateAfter.strokes[0].points)
        assertEquals(stroke.colorValue, stateAfter.strokes[0].colorValue)
    }

    @Test
    fun `fitToContent resets document frame and centers document when document is open`() {
        val doc = BoardDocument(
            name = "lesson.pdf",
            kind = DocumentKind.PDF,
            path = "/sdcard/test.pdf",
            pageWidth = 1000f,
            pageHeight = 1500f
        )
        viewModel.setDocumentForTesting(doc)
        viewModel.setDocumentFrame(80f, 160f, 1.8f)

        // Verify document is moved and scaled
        assertEquals(80f, viewModel.state.value.documentFrameX)
        assertEquals(160f, viewModel.state.value.documentFrameY)
        assertEquals(1.8f, viewModel.state.value.documentFrameScale, 0.001f)

        // Trigger fitToContent
        viewModel.fitToContent(2000f, 1500f)

        val fittedState = viewModel.state.value
        // Frame must be reset to origin and unit scale
        assertEquals(0f, fittedState.documentFrameX)
        assertEquals(0f, fittedState.documentFrameY)
        assertEquals(1f, fittedState.documentFrameScale, 0.001f)

        // Scale and offsets must be fitted to viewport
        assertTrue("Scale must be positive", fittedState.scale > 0f)
        assertTrue("Fitted X offset must be >= 0", fittedState.offsetX >= 0f)
    }

    @Test
    fun `canvas pinch-zoom transforms viewport without altering document frame or stroke coordinates`() {
        val doc = BoardDocument(
            name = "lesson.pdf",
            kind = DocumentKind.PDF,
            path = "/sdcard/test.pdf",
            pageWidth = 1000f,
            pageHeight = 1500f
        )
        viewModel.setDocumentForTesting(doc)
        viewModel.setDocumentFrame(100f, 200f, 1.25f)

        val stroke = Stroke(
            points = listOf(Offset(10f, 20f), Offset(30f, 40f)),
            colorValue = 0xFF00FF00.toInt()
        )
        viewModel.commitStroke(stroke)

        // Simulate canvas pinch-zoom (updating canvas-level scale and offset)
        viewModel.updateTransform(2.5f, 350f, 450f)

        val transformedState = viewModel.state.value
        assertEquals(2.5f, transformedState.scale, 0.001f)
        assertEquals(350f, transformedState.offsetX, 0.001f)
        assertEquals(450f, transformedState.offsetY, 0.001f)

        // Document frame parameters remain strictly independent and preserved
        assertEquals(100f, transformedState.documentFrameX, 0.001f)
        assertEquals(200f, transformedState.documentFrameY, 0.001f)
        assertEquals(1.25f, transformedState.documentFrameScale, 0.001f)

        // Strokes remain strictly preserved in world coordinates
        assertEquals(1, transformedState.strokes.size)
        assertEquals(stroke.points, transformedState.strokes[0].points)
    }

    @Test
    fun `addPage, renamePage, and duplicatePage work as expected`() {
        val stroke = Stroke(points = listOf(Offset(5f, 5f), Offset(15f, 15f)))
        viewModel.commitStroke(stroke)
        viewModel.renamePage(0, "Introduction")
        assertEquals("Introduction", viewModel.state.value.pageTitles[0])

        viewModel.addPage()
        assertEquals(2, viewModel.state.value.pageCount)
        assertEquals(1, viewModel.state.value.pageIndex)
        assertEquals("Page 2", viewModel.state.value.pageTitles[1])
        assertTrue(viewModel.state.value.strokes.isEmpty())

        // Duplicate page 0
        viewModel.duplicatePage(0)
        assertEquals(3, viewModel.state.value.pageCount)
        assertEquals("Introduction copy", viewModel.state.value.pageTitles[1])
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(stroke.points, viewModel.state.value.strokes[0].points)
        assertFalse(stroke.id == viewModel.state.value.strokes[0].id)
    }

    @Test
    fun `deletePage prevents deleting the last remaining page`() {
        assertEquals(1, viewModel.state.value.pageCount)
        val success = viewModel.deletePage(0)
        assertFalse(success)
        assertEquals(1, viewModel.state.value.pageCount)
        assertEquals("The last remaining page cannot be deleted.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `deletePage can be undone via undo restoring page, title, and strokes`() {
        val stroke1 = Stroke(points = listOf(Offset(10f, 10f), Offset(20f, 20f)))
        viewModel.commitStroke(stroke1)
        viewModel.renamePage(0, "Page One")

        viewModel.addPage()
        val stroke2 = Stroke(points = listOf(Offset(100f, 100f), Offset(200f, 200f)))
        viewModel.commitStroke(stroke2)
        viewModel.renamePage(1, "Page Two")
        assertEquals(2, viewModel.state.value.pageCount)

        // Delete Page Two (index 1)
        val deleted = viewModel.deletePage(1)
        assertTrue(deleted)
        assertEquals(1, viewModel.state.value.pageCount)
        assertEquals(0, viewModel.state.value.pageIndex)
        assertEquals("Page One", viewModel.state.value.pageTitles[0])
        assertTrue(viewModel.state.value.canUndo)

        // Undo deletion
        viewModel.undo()
        val restoredState = viewModel.state.value
        assertEquals(2, restoredState.pageCount)
        assertEquals(1, restoredState.pageIndex)
        assertEquals("Page Two", restoredState.pageTitles[1])
        assertEquals(1, restoredState.strokes.size)
        assertEquals(stroke2.points, restoredState.strokes[0].points)
    }

    @Test
    fun `copy and paste strokes across ordinary pages applies visible offset`() {
        val originalStroke = Stroke(points = listOf(Offset(50f, 50f), Offset(100f, 100f)))
        viewModel.commitStroke(originalStroke)
        viewModel.selectStrokeAt(Offset(50f, 50f))
        assertEquals(setOf(originalStroke.id), viewModel.state.value.selectedStrokeIds)

        viewModel.copySelectedStrokes()
        assertTrue(viewModel.state.value.hasStrokeClipboard)

        // Switch to new page
        viewModel.addPage()
        assertEquals(1, viewModel.state.value.pageIndex)
        assertTrue(viewModel.state.value.strokes.isEmpty())

        // Paste onto page 2
        viewModel.pasteStrokeClipboard()
        val stateAfterPaste = viewModel.state.value
        assertEquals(1, stateAfterPaste.strokes.size)
        val pastedStroke = stateAfterPaste.strokes[0]
        assertFalse(pastedStroke.id == originalStroke.id)

        // Verify visible offset
        val expectedOffset = Offset(36f, 36f)
        assertEquals(50f + expectedOffset.x, pastedStroke.points[0].x, 0.001f)
        assertEquals(50f + expectedOffset.y, pastedStroke.points[0].y, 0.001f)
        assertEquals(100f + expectedOffset.x, pastedStroke.points[1].x, 0.001f)
        assertEquals(100f + expectedOffset.y, pastedStroke.points[1].y, 0.001f)

        // Pasted stroke should be selected
        assertEquals(setOf(pastedStroke.id), stateAfterPaste.selectedStrokeIds)
    }

    @Test
    fun `PDF-backed board refuses adding, duplicating, deleting, copying, and pasting`() {
        val doc = BoardDocument(
            name = "lesson.pdf",
            kind = DocumentKind.PDF,
            path = "/sdcard/test.pdf",
            pageWidth = 1000f,
            pageHeight = 1500f
        )
        viewModel.setDocumentForTesting(doc)

        viewModel.addPage()
        assertEquals("Adding pages is disabled for document-backed boards.", viewModel.state.value.errorMessage)

        viewModel.duplicatePage(0)
        assertEquals("Duplicating pages is disabled for document-backed boards.", viewModel.state.value.errorMessage)

        val deleted = viewModel.deletePage(0)
        assertFalse(deleted)
        assertEquals("Deleting pages is disabled for document-backed boards.", viewModel.state.value.errorMessage)

        val stroke = Stroke(points = listOf(Offset(10f, 10f), Offset(20f, 20f)))
        viewModel.commitStroke(stroke)
        viewModel.selectStrokeAt(Offset(10f, 10f))
        viewModel.copySelectedStrokes()
        assertEquals("Copying is not supported on document-backed boards because pages are tied to the PDF file.", viewModel.state.value.errorMessage)

        viewModel.pasteStrokeClipboard()
        assertEquals("Pasting is not supported on document-backed boards because pages are tied to the PDF file.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `acceptSmartWrite converts handwriting stroke into typed text stroke`() {
        val handwriting = Stroke(
            points = listOf(Offset(20f, 20f), Offset(100f, 60f)),
            colorValue = 0xFF222222.toInt(),
            strokeType = StrokeType.PEN
        )
        viewModel.commitStroke(handwriting)
        assertEquals(1, viewModel.state.value.strokes.size)

        val offer = SmartWriteOffer(
            strokeIds = listOf(handwriting.id),
            candidates = listOf("Welcome", "welcome"),
            lowConfidence = false,
            color = 0xFF222222.toInt(),
            anchor = Offset(20f, 20f)
        )
        viewModel.setSmartWriteOfferForTesting(offer)
        assertEquals(offer, viewModel.state.value.smartWriteOffer)

        viewModel.acceptSmartWrite("Welcome")

        val state = viewModel.state.value
        assertEquals(1, state.strokes.size)
        val textStroke = state.strokes.first()
        assertEquals(StrokeType.TEXT, textStroke.strokeType)
        assertEquals("Welcome", textStroke.text)
        assertEquals(0xFF222222.toInt(), textStroke.colorValue)
        assertTrue(state.canUndo)
        assertEquals(null, state.smartWriteOffer)
    }

    @Test
    fun `cancelPdfOcr terminates active OCR and resets state`() {
        viewModel.cancelPdfOcr()
        assertFalse(viewModel.state.value.isPdfOcrActive)
    }

    @Test
    fun `togglePdfSearchPanel toggles search panel visibility`() {
        assertFalse(viewModel.state.value.showPdfSearchPanel)
        viewModel.togglePdfSearchPanel(true)
        assertTrue(viewModel.state.value.showPdfSearchPanel)
        viewModel.togglePdfSearchPanel(false)
        assertFalse(viewModel.state.value.showPdfSearchPanel)
    }

    @Test
    fun `pdfOcrDocument search finds matches and supports querying after reopening`() {
        val doc = PdfOcrDocument(
            documentPath = "/sdcard/lesson.pdf",
            pages = listOf(
                PdfPageOcr(0, "Introduction to Newton's First Law"),
                PdfPageOcr(1, "Examples of Newton's laws in real life")
            )
        )
        viewModel.setPdfOcrDocumentForTesting(doc)

        viewModel.setPdfSearchQuery("Newton")
        assertEquals("Newton", viewModel.state.value.pdfSearchQuery)

        val results = viewModel.state.value.pdfOcrDocument?.search("Newton")
        assertNotNull(results)
        assertEquals(2, results!!.size)
        assertEquals(0 to 1, results[0])
        assertEquals(1 to 1, results[1])
    }

    @Test
    fun `all 8 canvas backgrounds can be applied and verified`() {
        val backgrounds = listOf(
            CanvasBackground.WHITE,
            CanvasBackground.DARK,
            CanvasBackground.LINED,
            CanvasBackground.DOTTED,
            CanvasBackground.GRID,
            CanvasBackground.GRAPH,
            CanvasBackground.FOOTBALL,
            CanvasBackground.CUSTOM
        )
        assertEquals(8, backgrounds.size)

        for (bg in backgrounds) {
            if (bg == CanvasBackground.CUSTOM) {
                viewModel.setPageBackground(bg, "content://media/external/images/bg_sample.jpg")
                assertEquals(CanvasBackground.CUSTOM, viewModel.state.value.canvasBackground)
                assertEquals("custom", viewModel.state.value.pageBackgroundKeys[0])
                assertEquals("content://media/external/images/bg_sample.jpg", viewModel.state.value.pageCustomBackgroundUris[0])
                assertFalse(viewModel.state.value.isDarkCanvas)
            } else {
                viewModel.setPageBackground(bg)
                assertEquals(bg, viewModel.state.value.canvasBackground)
                assertEquals(bg.key, viewModel.state.value.pageBackgroundKeys[0])
                if (bg == CanvasBackground.DARK) {
                    assertTrue(viewModel.state.value.isDarkCanvas)
                } else {
                    assertFalse(viewModel.state.value.isDarkCanvas)
                }
            }
        }
    }

    @Test
    fun `football pitch on page 1 and graph paper on page 2 with ink persist and switch correctly`() {
        // Page 1 (index 0): Football pitch
        viewModel.setPageBackground(CanvasBackground.FOOTBALL)
        assertEquals(CanvasBackground.FOOTBALL, viewModel.state.value.canvasBackground)
        assertEquals(CanvasBackground.FOOTBALL.key, viewModel.state.value.pageBackgroundKeys[0])

        // Draw ink over football pitch on Page 1
        val stroke1 = Stroke(points = listOf(Offset(50f, 50f), Offset(100f, 100f)), colorValue = 0xFFFFFFFF.toInt())
        viewModel.commitStroke(stroke1)
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(stroke1.points, viewModel.state.value.strokes[0].points)

        // Add Page 2 (index 1): Graph paper
        viewModel.addPage()
        assertEquals(2, viewModel.state.value.pageCount)
        assertEquals(1, viewModel.state.value.pageIndex)

        viewModel.setPageBackground(CanvasBackground.GRAPH)
        assertEquals(CanvasBackground.GRAPH, viewModel.state.value.canvasBackground)
        assertEquals(CanvasBackground.GRAPH.key, viewModel.state.value.pageBackgroundKeys[1])

        // Draw ink over graph paper on Page 2
        val stroke2 = Stroke(points = listOf(Offset(200f, 200f), Offset(350f, 350f)), colorValue = 0xFF1B1F2A.toInt())
        viewModel.commitStroke(stroke2)
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(stroke2.points, viewModel.state.value.strokes[0].points)

        // Switch back to Page 1
        viewModel.selectPage(0)
        assertEquals(0, viewModel.state.value.pageIndex)
        assertEquals(CanvasBackground.FOOTBALL, viewModel.state.value.canvasBackground)
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(stroke1.points, viewModel.state.value.strokes[0].points)

        // Switch to Page 2
        viewModel.selectPage(1)
        assertEquals(1, viewModel.state.value.pageIndex)
        assertEquals(CanvasBackground.GRAPH, viewModel.state.value.canvasBackground)
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(stroke2.points, viewModel.state.value.strokes[0].points)

        // Verify page metadata keys are preserved across all pages
        assertEquals(listOf(CanvasBackground.FOOTBALL.key, CanvasBackground.GRAPH.key), viewModel.state.value.pageBackgroundKeys)

        // Duplicate Page 1 (football pitch)
        viewModel.duplicatePage(0)
        assertEquals(3, viewModel.state.value.pageCount)
        assertEquals(1, viewModel.state.value.pageIndex) // New duplicate is at index 1
        assertEquals(CanvasBackground.FOOTBALL, viewModel.state.value.canvasBackground)
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(listOf(CanvasBackground.FOOTBALL.key, CanvasBackground.FOOTBALL.key, CanvasBackground.GRAPH.key), viewModel.state.value.pageBackgroundKeys)
    }

    @Test
    fun `setSelectedImageAsBackground sets custom background from selected image and removes foreground stroke`() {
        val imageUri = "/data/user/0/com.avitrisphare.whiteboard/files/media/cell_biology.png"
        val imageStroke = Stroke(
            id = "img-123",
            strokeType = StrokeType.IMAGE,
            imageUri = imageUri,
            points = listOf(Offset(100f, 100f), Offset(400f, 300f))
        )
        viewModel.commitStroke(imageStroke)
        assertEquals(1, viewModel.state.value.strokes.size)

        // Select the image stroke
        viewModel.selectStrokes(setOf("img-123"))
        assertTrue(viewModel.state.value.selectedStrokeIds.contains("img-123"))

        // Set as background
        val success = viewModel.setSelectedImageAsBackground()
        assertTrue(success)

        // Verify canvas background is now CUSTOM with imageUri
        assertEquals(CanvasBackground.CUSTOM, viewModel.state.value.canvasBackground)
        assertEquals(CanvasBackground.CUSTOM.key, viewModel.state.value.pageBackgroundKeys[0])
        assertEquals(imageUri, viewModel.state.value.pageCustomBackgroundUris[0])

        // Verify the floating foreground image stroke was locked into the background (removed from foreground)
        assertTrue(viewModel.state.value.strokes.isEmpty())
        assertTrue(viewModel.state.value.selectedStrokeIds.isEmpty())

        // Verify teacher can draw ink over the image background
        val inkStroke = Stroke(points = listOf(Offset(150f, 150f), Offset(200f, 250f)), colorValue = 0xFFFF0000.toInt())
        viewModel.commitStroke(inkStroke)
        assertEquals(1, viewModel.state.value.strokes.size)
        assertEquals(inkStroke.points, viewModel.state.value.strokes[0].points)
        assertEquals(CanvasBackground.CUSTOM, viewModel.state.value.canvasBackground)
        assertEquals(imageUri, viewModel.state.value.pageCustomBackgroundUris[0])
    }

    @Test
    fun `two, three, four, and six pane layouts open correctly`() {
        listOf(2, 3, 4, 6).forEach { count ->
            viewModel.setSplit(count)
            val state = viewModel.state.value
            assertEquals(count, state.splitCount)
            assertEquals(count, state.splitPanes.size)
            assertEquals(splitColumns(count) + splitRows(count), state.splitWeights.size)
            assertEquals(0, state.splitActive)
            assertFalse(state.canUndo)
            assertFalse(state.canRedo)
        }
    }

    @Test
    fun `drawing, colour changes, erasing, and undo affect the active pane only`() {
        // Open 2-pane split screen
        viewModel.setSplit(2)
        assertEquals(0, viewModel.state.value.splitActive)

        // 1. Colour changes affect active pane only
        viewModel.setColor(0xFFFF0000.toInt()) // Red on Pane 0
        assertEquals(0xFFFF0000.toInt(), viewModel.state.value.selectedColor)
        assertEquals(0xFFFF0000.toInt(), viewModel.state.value.splitPanes[0].color)
        assertEquals(0xFF1B1F2A.toInt(), viewModel.state.value.splitPanes[1].color) // Pane 1 remains default black

        // 2. Drawing affects active pane only
        val strokePane0 = Stroke(
            points = listOf(Offset(10f, 10f), Offset(50f, 50f)),
            colorValue = 0xFFFF0000.toInt()
        )
        viewModel.beginPaneEdit(0)
        viewModel.setPaneStrokes(0, listOf(strokePane0))

        assertEquals(1, viewModel.state.value.splitPanes[0].strokes.size)
        assertEquals(0, viewModel.state.value.splitPanes[1].strokes.size)
        assertTrue(viewModel.state.value.canUndo)

        // Switch to Pane 1 (active pane is visually identifiable & updates toolbar state)
        viewModel.focusPane(1)
        assertEquals(1, viewModel.state.value.splitActive)
        assertEquals(0xFF1B1F2A.toInt(), viewModel.state.value.selectedColor) // Pane 1's color restored
        assertFalse(viewModel.state.value.canUndo) // Pane 1 has no undo history yet

        // Change color on Pane 1 to Blue
        viewModel.setColor(0xFF0000FF.toInt())
        assertEquals(0xFF0000FF.toInt(), viewModel.state.value.splitPanes[1].color)
        assertEquals(0xFFFF0000.toInt(), viewModel.state.value.splitPanes[0].color) // Pane 0 color untouched!

        // Draw on Pane 1
        val strokePane1 = Stroke(
            points = listOf(Offset(100f, 100f), Offset(150f, 150f)),
            colorValue = 0xFF0000FF.toInt()
        )
        viewModel.beginPaneEdit(1)
        viewModel.setPaneStrokes(1, listOf(strokePane1))

        assertEquals(1, viewModel.state.value.splitPanes[0].strokes.size)
        assertEquals(1, viewModel.state.value.splitPanes[1].strokes.size)
        assertEquals(strokePane0, viewModel.state.value.splitPanes[0].strokes[0])
        assertEquals(strokePane1, viewModel.state.value.splitPanes[1].strokes[0])

        // 3. Undo affects active pane only
        viewModel.undo() // Undoes stroke on Pane 1
        assertEquals(1, viewModel.state.value.splitPanes[0].strokes.size) // Pane 0 untouched!
        assertEquals(0, viewModel.state.value.splitPanes[1].strokes.size) // Pane 1 undone!
        assertTrue(viewModel.state.value.canRedo)

        // Redo on active pane
        viewModel.redo()
        assertEquals(1, viewModel.state.value.splitPanes[1].strokes.size)

        // 4. Erasing (clearCanvas) affects active pane only
        viewModel.clearCanvas()
        assertEquals(1, viewModel.state.value.splitPanes[0].strokes.size) // Pane 0 untouched!
        assertEquals(0, viewModel.state.value.splitPanes[1].strokes.size) // Pane 1 cleared!

        // Undo clearCanvas on active pane restores its stroke
        viewModel.undo()
        assertEquals(1, viewModel.state.value.splitPanes[1].strokes.size)
        assertEquals(1, viewModel.state.value.splitPanes[0].strokes.size)
    }

    @Test
    fun `closing split screen retains the focused pane and content remains on main canvas`() {
        viewModel.setSplit(2)
        val stroke0 = Stroke(points = listOf(Offset(10f, 10f), Offset(20f, 20f)))
        val stroke1 = Stroke(points = listOf(Offset(100f, 100f), Offset(200f, 200f)))
        viewModel.setPaneStrokes(0, listOf(stroke0))
        viewModel.setPaneStrokes(1, listOf(stroke1))

        // Focus Pane 1
        viewModel.focusPane(1)
        assertEquals(1, viewModel.state.value.splitActive)

        // Close split screen retaining the focused pane
        viewModel.closeSplit(viewModel.state.value.splitActive)

        val stateAfterClose = viewModel.state.value
        assertEquals(1, stateAfterClose.splitCount)
        assertTrue(stateAfterClose.splitPanes.isEmpty())
        assertEquals(1, stateAfterClose.strokes.size)
        assertEquals(stroke1, stateAfterClose.strokes[0])
        assertEquals(1f, stateAfterClose.scale)
        assertEquals(0f, stateAfterClose.offsetX)
        assertEquals(0f, stateAfterClose.offsetY)
    }

    @Test
    fun `requestCloseSplit with paneIndex targets that pane and opens dialog`() {
        viewModel.setSplit(3)
        assertEquals(0, viewModel.state.value.splitActive)

        viewModel.requestCloseSplit(paneIndex = 2)
        assertTrue(viewModel.state.value.showCloseSplitDialog)
        assertEquals(2, viewModel.state.value.splitActive)

        viewModel.dismissCloseSplitDialog()
        assertFalse(viewModel.state.value.showCloseSplitDialog)
    }

    @Test
    fun `closeSplit with minus one combines strokes from all panes`() {
        viewModel.setSplit(2)
        val stroke0 = Stroke(points = listOf(Offset(10f, 10f)))
        val stroke1 = Stroke(points = listOf(Offset(200f, 200f)))
        viewModel.setPaneStrokes(0, listOf(stroke0))
        viewModel.setPaneStrokes(1, listOf(stroke1))

        viewModel.closeSplit(keepPaneIndex = -1)

        val state = viewModel.state.value
        assertEquals(1, state.splitCount)
        assertEquals(2, state.strokes.size)
        assertTrue(state.strokes.contains(stroke0))
        assertTrue(state.strokes.contains(stroke1))
    }
}

