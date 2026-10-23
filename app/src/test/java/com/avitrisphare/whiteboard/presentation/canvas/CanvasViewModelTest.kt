package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
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
    private val panelApi = mockk<com.avitrisphare.whiteboard.data.remote.PanelApi>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CanvasViewModel(context, repository, panelApi)
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
}

