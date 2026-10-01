package com.vibenote.app.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.repository.NoteRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        // Color.White.toArgb() is 0xFFFFFFFF.toInt() which is -1
        assertEquals(-1, state.selectedColor)
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
}

