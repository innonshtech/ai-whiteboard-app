package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
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
class FlowchartConnectorsTest {

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
    fun `process, decision, and terminator nodes can be created`() {
        viewModel.addFlowchartNode("terminator", Offset(100f, 100f))
        viewModel.addFlowchartNode("process", Offset(350f, 100f))
        viewModel.addFlowchartNode("decision", Offset(600f, 100f))

        val strokes = viewModel.state.value.strokes
        assertEquals(3, strokes.size)

        val terminator = strokes[0]
        assertEquals(StrokeType.ELLIPSE, terminator.strokeType)
        assertEquals("flow:terminator", terminator.stickerKind)

        val process = strokes[1]
        assertEquals(StrokeType.RECTANGLE, process.strokeType)
        assertEquals("flow:process", process.stickerKind)

        val decision = strokes[2]
        assertEquals(StrokeType.DIAMOND, decision.strokeType)
        assertEquals("flow:decision", decision.stickerKind)
    }

    @Test
    fun `arrows connect to their associated nodes in a three-node flowchart`() {
        val startPos = Offset(100f, 200f)
        viewModel.createThreeNodeFlowchart(startPos)

        val strokes = viewModel.state.value.strokes
        assertEquals(5, strokes.size) // 3 nodes + 2 connector arrows

        val nodes = strokes.filter { it.strokeType != StrokeType.CONNECTOR }
        val arrows = strokes.filter { it.strokeType == StrokeType.CONNECTOR }

        assertEquals(3, nodes.size)
        assertEquals(2, arrows.size)

        val term = nodes[0]
        val proc = nodes[1]
        val dec = nodes[2]

        val arrow1 = arrows[0]
        assertEquals(term.id, arrow1.linkFromId)
        assertEquals(proc.id, arrow1.linkToId)

        val arrow2 = arrows[1]
        assertEquals(proc.id, arrow2.linkFromId)
        assertEquals(dec.id, arrow2.linkToId)
    }

    @Test
    fun `moving the middle node in a three-node flowchart moves its connected arrows correctly`() {
        val startPos = Offset(100f, 200f)
        viewModel.createThreeNodeFlowchart(startPos)

        val strokesInitial = viewModel.state.value.strokes
        val procNode = strokesInitial.find { it.stickerKind == "flow:process" }
        assertNotNull(procNode)

        val initialProcCenter = procNode!!.bounds.center

        // Select the middle process node
        viewModel.selectStroke(procNode.id)
        assertTrue(viewModel.state.value.selectedStrokeIds.contains(procNode.id))

        // Move middle node down by (0, 150)
        val moveDelta = Offset(0f, 150f)
        viewModel.moveSelectedStrokes(moveDelta)

        val strokesAfterMove = viewModel.state.value.strokes
        val movedProc = strokesAfterMove.find { it.id == procNode.id }!!
        val expectedProcCenter = initialProcCenter + moveDelta
        assertEquals(expectedProcCenter.x, movedProc.bounds.center.x, 0.5f)
        assertEquals(expectedProcCenter.y, movedProc.bounds.center.y, 0.5f)

        // Arrow 1 target point must follow the middle node
        val arrow1 = strokesAfterMove.find { it.linkToId == procNode.id }!!
        val arrow1Target = arrow1.points.last()
        assertEquals(expectedProcCenter.x, arrow1Target.x, 0.5f)
        assertEquals(expectedProcCenter.y, arrow1Target.y, 0.5f)

        // Arrow 2 source point must follow the middle node
        val arrow2 = strokesAfterMove.find { it.linkFromId == procNode.id }!!
        val arrow2Source = arrow2.points.first()
        assertEquals(expectedProcCenter.x, arrow2Source.x, 0.5f)
        assertEquals(expectedProcCenter.y, arrow2Source.y, 0.5f)
    }

    @Test
    fun `deleting a node removes its associated arrows`() {
        val startPos = Offset(100f, 200f)
        viewModel.createThreeNodeFlowchart(startPos)

        val initialStrokes = viewModel.state.value.strokes
        val procNode = initialStrokes.find { it.stickerKind == "flow:process" }!!
        assertEquals(5, initialStrokes.size)

        // Select and delete the middle node
        viewModel.selectStroke(procNode.id)
        viewModel.deleteSelectedStrokes()

        val remainingStrokes = viewModel.state.value.strokes
        // Both arrows connected to the middle node must be cascade-deleted!
        assertEquals(2, remainingStrokes.size)
        assertFalse("Deleted node must not exist", remainingStrokes.any { it.id == procNode.id })
        assertFalse("Orphan arrows must not exist", remainingStrokes.any { it.strokeType == StrokeType.CONNECTOR })
    }

    @Test
    fun `undo restores the deleted node and its associated arrows`() {
        val startPos = Offset(100f, 200f)
        viewModel.createThreeNodeFlowchart(startPos)
        assertEquals(5, viewModel.state.value.strokes.size)

        val procNode = viewModel.state.value.strokes.find { it.stickerKind == "flow:process" }!!
        viewModel.selectStroke(procNode.id)
        viewModel.deleteSelectedStrokes()
        assertEquals(2, viewModel.state.value.strokes.size)

        // Perform Undo
        viewModel.undo()

        val restoredStrokes = viewModel.state.value.strokes
        assertEquals(5, restoredStrokes.size)
        assertTrue(restoredStrokes.any { it.id == procNode.id })
        val restoredArrows = restoredStrokes.filter { it.strokeType == StrokeType.CONNECTOR }
        assertEquals(2, restoredArrows.size)
    }

    @Test
    fun `connector positions remain visually stable during movement without drift`() {
        val startPos = Offset(50f, 100f)
        viewModel.createThreeNodeFlowchart(startPos)

        val procNode = viewModel.state.value.strokes.find { it.stickerKind == "flow:process" }!!
        viewModel.selectStroke(procNode.id)

        // Simulate multi-step dragging
        for (i in 1..10) {
            viewModel.moveSelectedStrokes(Offset(5f, 5f))
        }

        val strokes = viewModel.state.value.strokes
        val movedProc = strokes.find { it.id == procNode.id }!!
        val arrow1 = strokes.find { it.linkToId == procNode.id }!!
        val arrow2 = strokes.find { it.linkFromId == procNode.id }!!

        // Verify arrow1 ends exactly at moved node center
        assertEquals(movedProc.bounds.center.x, arrow1.points.last().x, 0.01f)
        assertEquals(movedProc.bounds.center.y, arrow1.points.last().y, 0.01f)

        // Verify arrow2 starts exactly at moved node center
        assertEquals(movedProc.bounds.center.x, arrow2.points.first().x, 0.01f)
        assertEquals(movedProc.bounds.center.y, arrow2.points.first().y, 0.01f)
    }
}
