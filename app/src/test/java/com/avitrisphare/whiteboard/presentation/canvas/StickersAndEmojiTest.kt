package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StickersAndEmojiTest {

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
    fun `four core stickers can be inserted and have correct StrokeType and kind`() {
        val stickers = listOf("star", "tick", "cross", "thumb")
        stickers.forEachIndexed { index, kind ->
            viewModel.insertSticker(kind, Offset(100f * index, 100f * index))
        }

        val state = viewModel.state.value
        assertEquals(4, state.strokes.size)
        stickers.forEachIndexed { index, kind ->
            val stroke = state.strokes[index]
            assertEquals(StrokeType.STICKER, stroke.strokeType)
            assertEquals(kind, stroke.stickerKind)
        }
    }

    @Test
    fun `classroom emoji can be inserted with correct text payload`() {
        viewModel.insertEmoji("💡", Offset(300f, 200f))

        val state = viewModel.state.value
        assertEquals(1, state.strokes.size)
        val stroke = state.strokes.first()
        assertEquals(StrokeType.STICKER, stroke.strokeType)
        assertEquals("emoji", stroke.stickerKind)
        assertEquals("💡", stroke.text)
    }

    @Test
    fun `inserted sticker is auto-selected and can be moved and deleted`() {
        val origin = Offset(200f, 200f)
        viewModel.insertSticker("star", origin)

        val state1 = viewModel.state.value
        val strokeId = state1.strokes.first().id
        assertEquals(setOf(strokeId), state1.selectedStrokeIds)

        // Move by +50, +50
        viewModel.moveSelectedStrokes(Offset(50f, 50f))
        val state2 = viewModel.state.value
        val movedStroke = state2.strokes.first()
        assertEquals(Offset(250f, 250f), movedStroke.points.first())

        // Delete selected stroke
        viewModel.deleteSelectedStrokes()
        val state3 = viewModel.state.value
        assertTrue("Sticker stroke should be deleted", state3.strokes.isEmpty())
        assertTrue(state3.selectedStrokeIds.isEmpty())
    }

    @Test
    fun `clearCanvas removes stickers according to surface clearing rules`() {
        viewModel.insertSticker("tick", Offset(100f, 100f))
        viewModel.insertEmoji("👏", Offset(200f, 200f))
        assertEquals(2, viewModel.state.value.strokes.size)

        viewModel.clearCanvas()
        assertTrue("All strokes including stickers should be cleared", viewModel.state.value.strokes.isEmpty())
    }

    @Test
    fun `sticker bounds computation provides accurate bounding box`() {
        val stroke = Stroke(
            points = listOf(Offset(100f, 100f)),
            strokeWidth = 6f,
            strokeType = StrokeType.STICKER,
            stickerKind = "star"
        )
        val bounds = stroke.bounds
        assertNotEquals(Rect.Zero, bounds)
        assertEquals(100f, bounds.left, 0.01f)
        assertEquals(100f, bounds.top, 0.01f)
        assertTrue("Sticker width should be positive", bounds.width > 50f)
        assertTrue("Sticker height should be positive", bounds.height > 50f)
    }

    @Test
    fun `split pane mode inserts sticker into active split pane`() {
        viewModel.setSplit(2)
        assertEquals(2, viewModel.state.value.splitCount)

        // Active pane is 0
        viewModel.insertSticker("thumb", Offset(150f, 150f))

        val state = viewModel.state.value
        val pane0 = state.splitPanes[0]
        assertEquals(1, pane0.strokes.size)
        assertEquals("thumb", pane0.strokes.first().stickerKind)

        // Pane 1 should remain empty
        val pane1 = state.splitPanes[1]
        assertTrue(pane1.strokes.isEmpty())
    }
}
