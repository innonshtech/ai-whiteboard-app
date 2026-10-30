package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.data.remote.ApiCall
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.parseMathRecognitionResponse
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
class EquationConversionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CanvasViewModel
    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<NoteRepository>(relaxed = true)
    private val panelApi = mockk<PanelApi>(relaxed = true)

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
    fun `parseMathRecognitionResponse parses valid json with latex and confidence`() {
        val json = """
            {
                "success": true,
                "latex": "x^2 + 2x - 5 = 0",
                "mathml": "<math><msup><mi>x</mi><mn>2</mn></msup></math>",
                "confidence": 0.98,
                "error": null
            }
        """.trimIndent()
        val result = parseMathRecognitionResponse(json)
        assertTrue(result is ApiCall.Ok)
        val response = (result as ApiCall.Ok).value
        assertEquals("x^2 + 2x - 5 = 0", response.latex)
        assertEquals(0.98f, response.confidence, 0.001f)
        assertEquals(null, response.error)
    }

    @Test
    fun `parseMathRecognitionResponse returns error on failed or missing latex`() {
        val failedJson = """{"success": false, "error": "Model failed to read equation"}"""
        val failedResult = parseMathRecognitionResponse(failedJson)
        assertTrue(failedResult is ApiCall.Err)
        assertEquals("Model failed to read equation", (failedResult as ApiCall.Err).message)

        val emptyLatexJson = """{"success": true, "latex": "  "}"""
        val emptyResult = parseMathRecognitionResponse(emptyLatexJson)
        assertTrue(emptyResult is ApiCall.Err)

        val malformedJson = "{ broken json: true "
        val malformedResult = parseMathRecognitionResponse(malformedJson)
        assertTrue(malformedResult is ApiCall.Err)
        assertEquals("Malformed recognition response", (malformedResult as ApiCall.Err).message)
    }

    @Test
    fun `solveLatex solves linear equation with teacher corrections`() {
        val solution = solveLatex("2x + 4 = 10")
        assertTrue(solution.solved)
        assertEquals("x = 3", solution.answerLatex)
        assertTrue(solution.steps.isNotEmpty())
    }

    @Test
    fun `solveEditedEquation updates formulaSolution with new calculation`() {
        viewModel.solveEditedEquation("3x - 9 = 0")
        val solution = viewModel.state.value.formulaSolution
        assertNotNull(solution)
        assertTrue(solution!!.solved)
        assertEquals("x = 3", solution.answerLatex)
    }

    @Test
    fun `updateEquationDraft updates formulaSolution without re-running recognition`() {
        viewModel.solveEditedEquation("x + 1 = 2")
        val original = viewModel.state.value.formulaSolution
        assertNotNull(original)

        viewModel.updateEquationDraft("x + 5 = 10")
        val updated = viewModel.state.value.formulaSolution
        assertNotNull(updated)
        assertEquals("x + 5 = 10", updated!!.latex)
        assertFalse(updated.solved)
    }

    @Test
    fun `equationBusy prevents concurrent conversion calls`() {
        val stroke = Stroke(
            points = listOf(Offset(10f, 10f), Offset(50f, 10f), Offset(100f, 10f)),
            strokeType = StrokeType.PEN
        )
        viewModel.commitStroke(stroke)
        viewModel.selectInRect(androidx.compose.ui.geometry.Rect(0f, 0f, 200f, 200f))
        assertEquals(setOf(stroke.id), viewModel.state.value.selectedStrokeIds)

        viewModel.convertSelectionToEquation()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.state.value.equationBusy)

        // Second call while busy is blocked by the guard
        viewModel.convertSelectionToEquation()
        assertTrue(viewModel.state.value.equationBusy)
    }

    @Test
    fun `convertSelectionToEquation includes single-point dots and line strokes`() {
        val dot = Stroke(
            points = listOf(Offset(10f, 10f)),
            strokeType = StrokeType.PEN
        )
        val line = Stroke(
            points = listOf(Offset(20f, 20f), Offset(50f, 20f)),
            strokeType = StrokeType.LINE
        )
        viewModel.commitStroke(dot)
        viewModel.commitStroke(line)
        viewModel.selectInRect(androidx.compose.ui.geometry.Rect(0f, 0f, 200f, 200f))
        assertEquals(setOf(dot.id, line.id), viewModel.state.value.selectedStrokeIds)

        viewModel.convertSelectionToEquation()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.state.value.equationBusy)
    }
}
