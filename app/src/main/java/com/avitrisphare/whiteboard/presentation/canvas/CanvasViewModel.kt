package com.avitrisphare.whiteboard.presentation.canvas

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import com.avitrisphare.whiteboard.data.local.StrokeDto
import com.avitrisphare.whiteboard.data.local.toDomain
import com.avitrisphare.whiteboard.data.local.toDto
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.withGraphPaperToggled
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.avitrisphare.whiteboard.data.remote.ApiCall
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import com.avitrisphare.whiteboard.domain.repository.NoteRepository
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportDestination
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportFormat
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportResult
import com.avitrisphare.whiteboard.presentation.canvas.export.WhiteboardExportManager
import com.avitrisphare.whiteboard.presentation.entry.PanelSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

enum class WhiteboardTool(val label: String, val shortcut: String) {
    LOCK("Lock", ""),
    HAND("Hand", "H"),
    SELECT("Selection", "V"),
    SHAPE("Shape", "S"),
    RECTANGLE("Rectangle", "R"),
    DIAMOND("Diamond", "D"),
    CIRCLE("Circle", "O"),
    ARROW("Arrow", "A"),
    LINE("Line", "L"),
    PEN("Pen", "P"),
    MATH_PEN("Math", "M"),
    PENCIL("Pencil", ""),
    MARKER("Marker", ""),
    HIGHLIGHTER("Highlighter", ""),
    BRUSH("Brush", ""),
    TEXT("Text", "T"),
    STICKY_NOTE("Sticky Note", "N"),
    ERASER("Eraser", "E"),
    FRAME("Frame tool", "F"),
    RULER("Ruler", "U"),
    COMPASS("Compass", ""),
    PROTRACTOR("Protractor", ""),
    SET_SQUARE("Set Square", ""),
    LASER("Laser pointer", "K"),
    BUCKET_FILL("Bucket fill", "B"),
    LASSO("Lasso selection", ""),
    SPOTLIGHT("Spotlight", ""),
    SHADE("Screen shade", ""),
    MAGNIFIER("Magnifier", ""),
    TRIANGLE("Triangle", ""),
    STAR("Star", ""),
    ELLIPSE("Ellipse", ""),
    SQUARE("Square", ""),
    HEXAGON("Hexagon", ""),
    ROUND_RECT("Rounded rect", ""),
    FLOWCHART("Flowchart", ""),
    MIND_MAP("Mind map", ""),
    CUBE("Cube", ""),
    CYLINDER("Cylinder", ""),
    CONE("Cone", ""),
    PYRAMID("Pyramid", ""),
    EMOJI("Emoji", ""),
    STICKER("Sticker", "")
}

enum class EraserMode {
    /** Removes ink inside a circle, splitting the rest of the stroke. */
    STANDARD,
    /** Removes every stroke or object the circle touches. */
    SMART
}

const val TOOLBAR_ORDER_KEY = "toolbar_tool_order"
const val TOOLBAR_HIDDEN_KEY = "toolbar_hidden_tools"

val DEFAULT_TOOLBAR_TOOLS: List<String> = listOf(
    "select",
    "pen",
    "flowchart",
    "stickers",
    "shapes",
    "eraser",
    "laser",
    "geometry",
    "tools",
    "undo",
    "redo",
    "save"
)

val ALL_CUSTOMIZABLE_TOOLS: List<String> = listOf(
    "select",
    "pen",
    "flowchart",
    "stickers",
    "shapes",
    "eraser",
    "laser",
    "geometry",
    "tools",
    "undo",
    "redo",
    "save",
    "text",
    "image",
    "clear",
    "background"
)

data class CanvasState(
    val strokes: List<Stroke> = emptyList(),
    val currentStroke: Stroke? = null,
    val selectedColor: Int = 0xFF1B1F2A.toInt(),
    val fillColor: Int = 0,
    val strokeWidth: Float = 4f,
    val currentTool: WhiteboardTool = WhiteboardTool.SELECT,
    val inkTool: WhiteboardTool = WhiteboardTool.PEN,
    val isToolLocked: Boolean = false,
    val isDrawToShapeEnabled: Boolean = true,
    val smartWriteEnabled: Boolean = false,
    val smartWriteLanguageTag: String = "en",
    val smartWriteIntervalMs: Long = 500L,
    val smartWriteOffer: SmartWriteOffer? = null,
    val smartWriteBusy: Boolean = false,
    val equationBusy: Boolean = false,
    val extractTextBusy: Boolean = false,
    val isPdfOcrActive: Boolean = false,
    val pdfOcrCurrentPage: Int = 0,
    val pdfOcrTotalPages: Int = 0,
    val pdfOcrDocument: PdfOcrDocument? = null,
    val showPdfSearchPanel: Boolean = false,
    val pdfSearchQuery: String = "",
    val formulaSolution: FormulaSolution? = null,
    val smartWriteWarning: String? = null,
    val editingTextId: String? = null,
    val laserPoints: List<Offset> = emptyList(),
    val selectedStrokeIds: Set<String> = emptySet(),
    val isEraser: Boolean = false,
    val eraserMode: EraserMode = EraserMode.STANDARD,
    val eraserSize: Float = 28f,
    val isHighlighter: Boolean = false,
    val isShapeMode: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val noteId: String = "",
    val noteTitle: String = "Untitled",
    val isLoading: Boolean = false,
    val canvasBackground: CanvasBackground = CanvasBackground.DOTTED,
    val showGrid: Boolean = false,
    val isDarkCanvas: Boolean = false,
    val isDarkTheme: Boolean = false,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
    val document: BoardDocument? = null,
    val splitCount: Int = 1,
    val splitWeights: List<Float> = emptyList(),
    val splitPanes: List<CanvasPane> = emptyList(),
    val splitActive: Int = 0,
    val paneRequest: String? = null,
    val pagePreviews: List<List<Stroke>> = listOf(emptyList()),
    val contentJson: String = "",
    val isLiveCollabActive: Boolean = false,
    val collabRoomId: String = "",
    val userName: String = "Swift Penguin",
    val connectedCollaborators: List<String> = emptyList(),
    val errorMessage: String? = null,
    val secondaryColor: Int = 0xFF3B82F6.toInt(),
    val secondaryStrokeWidth: Float = 12f,
    val secondaryInkTool: WhiteboardTool = WhiteboardTool.PEN,
    val useSecondaryPenColor: Boolean = false,
    val inkSampleGapMm: Float = DEFAULT_INK_SAMPLE_GAP_MM,
    val palmEraserEnabled: Boolean = false,
    val restrictEraserToSelection: Boolean = false,
    val pageTitles: List<String> = listOf("Page 1"),
    val pageBackgroundKeys: List<String> = listOf(CanvasBackground.DOTTED.key),
    val pageCustomBackgroundUris: List<String> = listOf(""),
    val documentFrameX: Float = 0f,
    val documentFrameY: Float = 0f,
    val documentFrameScale: Float = 1f,
    val saveToast: String? = null,
    val toolbarToolOrder: List<String> = DEFAULT_TOOLBAR_TOOLS,
    val hiddenToolbarTools: Set<String> = emptySet(),
    val showCloseSplitDialog: Boolean = false,
    val pageEpoch: Long = 0L,
    val mathTools: List<PlacedMathTool> = emptyList(),
    val uiLocaleTag: String = "en",
    val panelIdForHelp: String = "",
    val licenseStateForHelp: String = "",
    val hasStrokeClipboard: Boolean = false,
    val flowchartToolActive: Boolean = false,
    val mindMapCenterId: String? = null,
    val activeShapeType: StrokeType = StrokeType.RECTANGLE,
    val shapeOpacity: Float = 1f,
    val shapeCornerRadius: Float = 24f,
    val showShapeDimensions: Boolean = false,
    val shapeStyleMode: ShapeStyleMode = ShapeStyleMode.BORDER,
    val shapeBorderColor: Int = 0xFF1B1F2A.toInt(),
    val shapeFillColor: Int = 0
)

val WRITING_TOOLS = setOf(
    WhiteboardTool.PEN,
    WhiteboardTool.MATH_PEN,
    WhiteboardTool.PENCIL,
    WhiteboardTool.MARKER,
    WhiteboardTool.HIGHLIGHTER,
    WhiteboardTool.BRUSH
)

val PEN_PANEL_TOOLS = setOf(
    WhiteboardTool.PEN,
    WhiteboardTool.MATH_PEN,
    WhiteboardTool.PENCIL,
    WhiteboardTool.MARKER,
    WhiteboardTool.HIGHLIGHTER,
    WhiteboardTool.BRUSH,
    WhiteboardTool.LASSO
)

fun WhiteboardTool.toStrokeType(): StrokeType = when (this) {
    WhiteboardTool.PENCIL -> StrokeType.PENCIL
    WhiteboardTool.MARKER -> StrokeType.MARKER
    WhiteboardTool.HIGHLIGHTER -> StrokeType.HIGHLIGHTER
    WhiteboardTool.BRUSH -> StrokeType.BRUSH
    WhiteboardTool.MATH_PEN -> StrokeType.PEN
    else -> StrokeType.PEN
}

fun WhiteboardTool.defaultStrokeWidth(): Float = when (this) {
    WhiteboardTool.PENCIL -> 8f
    WhiteboardTool.MARKER -> 18f
    WhiteboardTool.HIGHLIGHTER -> 28f
    WhiteboardTool.BRUSH -> 12f
    WhiteboardTool.MATH_PEN -> 4f
    else -> 4f
}

val INK_PRESETS = listOf(
    0xFF1B1F2A, 0xFF4B5563, 0xFF9CA3AF, 0xFFFFFFFF,
    0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFFEAB308,
    0xFF84CC16, 0xFF22C55E, 0xFF10B981, 0xFF14B8A6,
    0xFF06B6D4, 0xFF0EA5E9, 0xFF3B82F6, 0xFF6366F1,
    0xFF8B5CF6, 0xFFA855F7, 0xFFD946EF, 0xFFEC4899,
    0xFFF43F5E, 0xFF7C2D12, 0xFF14532D, 0xFF1E3A8A
).map { it.toInt() }

val PEN_COLORS = listOf(
    0xFF1B1F2A.toInt(), // Ink
    0xFFFFFFFF.toInt(), // White
    0xFF3ECF8E.toInt(), // Brand Green
    0xFFFF6B6B.toInt(), // Red
    0xFF4ECDC4.toInt(), // Teal
    0xFFFFE66D.toInt(), // Yellow
    0xFF95E1D0.toInt(), // Mint
    0xFFF38181.toInt(), // Coral
    0xFFAA96DA.toInt() // Lavender
)

val STICKY_COLORS = listOf(
    0xFFFFF9C4.toInt(), // Soft Yellow
    0xFFFFCCBC.toInt(), // Soft Coral
    0xFFC8E6C9.toInt(), // Soft Mint
    0xFFE1BEE7.toInt(), // Soft Lavender
    0xFFB3E5FC.toInt()  // Soft Sky Blue
)

private const val SHAPE_PREFS = "whiteboard_settings"
private const val SHAPE_RECOGNITION_KEY = "shape_recognition"
private const val SMART_WRITE_KEY = "smart_write"
private const val SMART_WRITE_LANG_KEY = "smart_write_lang"
private const val SMART_WRITE_INTERVAL_KEY = "smart_write_interval"

@HiltViewModel
class CanvasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val panelApi: PanelApi
) : ViewModel() {

    private val _state = MutableStateFlow(CanvasState())
    val state: StateFlow<CanvasState> = _state.asStateFlow()
    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    sealed interface CanvasUndoEntry {
        data class StrokeState(val strokes: List<Stroke>, val mathTools: List<PlacedMathTool> = emptyList()) : CanvasUndoEntry
        data class DeletedPage(
            val index: Int,
            val strokes: List<Stroke>,
            val title: String,
            val backgroundKey: String,
            val customBackgroundUri: String,
            val previousActivePage: Int
        ) : CanvasUndoEntry
    }

    private val undoActionStack = mutableListOf<CanvasUndoEntry>()
    private val redoActionStack = mutableListOf<CanvasUndoEntry>()
    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()
    private val gson = Gson()
    private val MAX_UNDO = 50
    private val MAX_PAGES = 50
    private var saveJob: Job? = null
    private var autoSaveJob: Job? = null
    private var laserJob: Job? = null
    private var smartJob: Job? = null
    private var textEditUndoCaptured = false
    private val handwritingReader by lazy { HandwritingReader(context) }
    private val imageTextExtractor by lazy { ImageTextExtractor() }
    private val pages = mutableListOf<List<Stroke>>(emptyList())
    private var activePage = 0
    private var selectionUndoCaptured = false
    private var plainPages: List<List<Stroke>>? = null
    private val renderedPages = java.util.Collections.synchronizedMap(HashMap<Int, Bitmap>())
    private var renderJob: Job? = null
    private var pdfOcrJob: Job? = null
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    var targetMindmapBoxId: String? = null
    private val paneUndo = Array(6) { ArrayDeque<List<Stroke>>() }
    private val paneRedo = Array(6) { ArrayDeque<List<Stroke>>() }
    private val toolWidths = mutableMapOf(
        WhiteboardTool.PEN to 4f,
        WhiteboardTool.MATH_PEN to 4f,
        WhiteboardTool.PENCIL to 8f,
        WhiteboardTool.MARKER to 18f,
        WhiteboardTool.HIGHLIGHTER to 28f,
        WhiteboardTool.BRUSH to 12f
    )
    private val pageTitles = mutableListOf("Page 1")
    private val pageBackgroundKeys = mutableListOf(CanvasBackground.DOTTED.key)
    private val pageCustomBackgroundUris = mutableListOf("")
    private val pageMathTools = mutableListOf<List<PlacedMathTool>>(emptyList())
    private var strokeClipboard: List<Stroke> = emptyList()
    private var pdfOcrCancelled = false

    init {
        val prefs = context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(SHAPE_RECOGNITION_KEY, true)
        val interval = prefs.getLong(SMART_WRITE_INTERVAL_KEY, 500L).let { if (it < 400L) 500L else it }
        val savedOrderStr = prefs.getString(TOOLBAR_ORDER_KEY, null)
        val savedOrder = if (!savedOrderStr.isNullOrBlank()) {
            val list = savedOrderStr.split(",").filter { it.isNotBlank() && it != "color_swap" && it in ALL_CUSTOMIZABLE_TOOLS }
            val missing = DEFAULT_TOOLBAR_TOOLS.filter { it !in list }
            val sanitized = list + missing
            if (savedOrderStr.contains("color_swap")) {
                prefs.edit().putString(TOOLBAR_ORDER_KEY, sanitized.joinToString(",")).apply()
            }
            sanitized
        } else {
            DEFAULT_TOOLBAR_TOOLS
        }
        val savedHidden = prefs.getStringSet(TOOLBAR_HIDDEN_KEY, emptySet()) ?: emptySet()
        _state.update {
            it.copy(
                isDrawToShapeEnabled = enabled,
                smartWriteEnabled = prefs.getBoolean(SMART_WRITE_KEY, false),
                smartWriteLanguageTag = prefs.getString(SMART_WRITE_LANG_KEY, "en") ?: "en",
                smartWriteIntervalMs = interval,
                toolbarToolOrder = savedOrder,
                hiddenToolbarTools = savedHidden
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            handwritingReader.prefetch(SmartWriteLanguage("en", "English", SmartWriteScript.LATIN))
        }
    }

    fun setTool(tool: WhiteboardTool) {
        if (tool == WhiteboardTool.LOCK) {
            _state.update { it.copy(isToolLocked = !it.isToolLocked) }
            return
        }
        val width = toolWidths[tool]
        val isMathPen = tool == WhiteboardTool.MATH_PEN
        if (isMathPen) {
            viewModelScope.launch(Dispatchers.IO) {
                handwritingReader.prefetch(SmartWriteLanguage("en", "English", SmartWriteScript.LATIN))
            }
        } else if (_state.value.inkTool == WhiteboardTool.MATH_PEN) {
            mathPenJob?.cancel()
            mathPenSessionStrokeIds.clear()
        }
        _state.update {
            val panes = if (it.splitCount > 1) {
                it.splitPanes.mapIndexed { index, pane ->
                    if (index == it.splitActive) pane.copy(tool = tool) else pane
                }
            } else {
                it.splitPanes
            }
            it.copy(
                currentTool = tool,
                inkTool = if (tool in PEN_PANEL_TOOLS) tool else it.inkTool,
                strokeWidth = width ?: it.strokeWidth,
                isEraser = tool == WhiteboardTool.ERASER,
                isHighlighter = tool == WhiteboardTool.HIGHLIGHTER,
                isShapeMode = tool in listOf(
                    WhiteboardTool.RECTANGLE,
                    WhiteboardTool.DIAMOND,
                    WhiteboardTool.CIRCLE,
                    WhiteboardTool.ARROW,
                    WhiteboardTool.LINE,
                    WhiteboardTool.FRAME,
                    WhiteboardTool.TRIANGLE,
                    WhiteboardTool.STAR,
                    WhiteboardTool.ELLIPSE,
                    WhiteboardTool.SQUARE,
                    WhiteboardTool.HEXAGON,
                    WhiteboardTool.ROUND_RECT,
                    WhiteboardTool.CUBE,
                    WhiteboardTool.CYLINDER,
                    WhiteboardTool.CONE,
                    WhiteboardTool.PYRAMID
                ),
                splitPanes = panes
            )
        }
    }

    fun setShapeType(type: StrokeType) {
        _state.update {
            it.copy(
                activeShapeType = type,
                currentTool = WhiteboardTool.SHAPE,
                isShapeMode = true
            )
        }
    }

    fun setShapeOpacity(opacity: Float) {
        _state.update { it.copy(shapeOpacity = opacity.coerceIn(0.05f, 1f)) }
    }

    fun setShapeCornerRadius(radius: Float) {
        _state.update { it.copy(shapeCornerRadius = radius.coerceIn(0f, 120f)) }
    }

    fun toggleShapeDimensions() {
        _state.update { it.copy(showShapeDimensions = !it.showShapeDimensions) }
    }

    fun setShapeStyleMode(mode: ShapeStyleMode) {
        _state.update { it.copy(shapeStyleMode = mode) }
    }

    fun setShapeColor(color: Int) {
        _state.update {
            if (it.shapeStyleMode == ShapeStyleMode.BORDER) {
                it.copy(shapeBorderColor = color, selectedColor = color)
            } else {
                it.copy(shapeFillColor = color, fillColor = color)
            }
        }
    }

    fun setSplit(count: Int) {
        if (count !in setOf(2, 3, 4, 6)) return
        val current = _state.value
        val existing = current.splitPanes
        val panes = List(count) { index ->
            existing.getOrNull(index) ?: CanvasPane(
                id = index,
                strokes = if (index == 0 && existing.isEmpty()) current.strokes else emptyList(),
                tool = if (index == 0) current.currentTool else WhiteboardTool.PEN,
                color = if (index == 0) current.selectedColor else 0xFF1B1F2A.toInt(),
                strokeWidth = if (index == 0) current.strokeWidth else 4f
            )
        }
        val targetActive = current.splitActive.coerceIn(0, count - 1)
        val activePane = panes[targetActive]
        paneUndo.forEach { it.clear() }
        paneRedo.forEach { it.clear() }
        _state.update {
            it.copy(
                splitCount = count,
                splitWeights = defaultWeights(count),
                splitPanes = panes,
                splitActive = targetActive,
                currentTool = activePane.tool,
                selectedColor = activePane.color,
                strokeWidth = activePane.strokeWidth,
                canUndo = false,
                canRedo = false
            )
        }
        scheduleSave()
    }

    fun requestCloseSplit(paneIndex: Int? = null) {
        if (_state.value.splitCount <= 1) return
        val target = (paneIndex ?: _state.value.splitActive).coerceIn(0, (_state.value.splitPanes.size - 1).coerceAtLeast(0))
        _state.update { it.copy(showCloseSplitDialog = true, splitActive = target) }
    }

    fun dismissCloseSplitDialog() {
        _state.update { it.copy(showCloseSplitDialog = false) }
    }

    fun closeSplit(keepPaneIndex: Int? = null) {
        val current = _state.value
        if (current.splitPanes.isEmpty()) {
            _state.update { it.copy(showCloseSplitDialog = false, splitCount = 1) }
            return
        }
        val targetIndex = (keepPaneIndex ?: current.splitActive).coerceIn(0, current.splitPanes.lastIndex)
        val strokesToKeep: List<Stroke> = if (keepPaneIndex == -1) {
            current.splitPanes.flatMap { it.strokes }
        } else {
            val kept = current.splitPanes.getOrNull(targetIndex)
            kept?.strokes ?: current.strokes
        }
        val keptPane = current.splitPanes.getOrNull(targetIndex)
        val freshStrokes = ArrayList(strokesToKeep)
        if (pages.isEmpty()) pages.add(emptyList())
        if (activePage !in pages.indices) activePage = 0
        pages[activePage] = freshStrokes

        paneUndo.forEach { it.clear() }
        paneRedo.forEach { it.clear() }
        undoActionStack.clear()
        redoActionStack.clear()
        if (freshStrokes.isNotEmpty()) {
            undoActionStack.add(CanvasUndoEntry.StrokeState(emptyList()))
        }
        _state.update {
            it.copy(
                strokes = freshStrokes,
                pagePreviews = pages.toList(),
                splitCount = 1,
                splitWeights = emptyList(),
                splitPanes = emptyList(),
                splitActive = 0,
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                currentTool = keptPane?.tool ?: it.currentTool,
                selectedColor = keptPane?.color ?: it.selectedColor,
                strokeWidth = keptPane?.strokeWidth ?: it.strokeWidth,
                paneRequest = null,
                showCloseSplitDialog = false,
                canUndo = undoActionStack.isNotEmpty(),
                canRedo = false
            )
        }
        scheduleSave()
    }

    fun focusPane(index: Int) {
        val pane = _state.value.splitPanes.getOrNull(index) ?: return
        if (_state.value.splitActive == index &&
            _state.value.currentTool == pane.tool &&
            _state.value.selectedColor == pane.color &&
            _state.value.strokeWidth == pane.strokeWidth
        ) return
        _state.update {
            it.copy(
                splitActive = index,
                currentTool = pane.tool,
                selectedColor = pane.color,
                strokeWidth = pane.strokeWidth,
                canUndo = paneUndo[index].isNotEmpty(),
                canRedo = paneRedo[index].isNotEmpty(),
                isEraser = pane.tool == WhiteboardTool.ERASER,
                isHighlighter = pane.tool == WhiteboardTool.HIGHLIGHTER
            )
        }
    }

    fun beginPaneEdit(index: Int) {
        val strokes = _state.value.splitPanes.getOrNull(index)?.strokes ?: return
        paneUndo[index].addLast(strokes)
        if (paneUndo[index].size > 40) paneUndo[index].removeAt(0)
        paneRedo[index].clear()
        val active = _state.value.splitActive
        _state.update {
            it.copy(
                canUndo = if (index == active) true else paneUndo[active].isNotEmpty(),
                canRedo = if (index == active) false else paneRedo[active].isNotEmpty()
            )
        }
    }

    fun cancelPaneEdit(index: Int) {
        if (index !in paneUndo.indices) return
        paneUndo[index].removeLastOrNull()
        val active = _state.value.splitActive
        _state.update {
            it.copy(
                canUndo = paneUndo[active].isNotEmpty(),
                canRedo = paneRedo[active].isNotEmpty()
            )
        }
    }

    fun setPaneStrokes(index: Int, strokes: List<Stroke>) {
        _state.update { state ->
            if (index !in state.splitPanes.indices) return@update state
            val active = state.splitActive.coerceIn(0, 5)
            state.copy(
                splitPanes = replacePaneStrokes(state.splitPanes, index, strokes),
                canUndo = paneUndo[active].isNotEmpty(),
                canRedo = paneRedo[active].isNotEmpty()
            )
        }
        scheduleSave()
    }

    fun resizeSplit(weights: List<Float>) {
        _state.update { it.copy(splitWeights = weights) }
        scheduleSave()
    }

    fun setPaneKind(index: Int, kind: PaneKind) {
        _state.update { state ->
            if (index !in state.splitPanes.indices) return@update state
            state.copy(
                splitPanes = state.splitPanes.mapIndexed { paneIndex, pane ->
                    if (paneIndex == index) pane.copy(kind = kind) else pane
                }
            )
        }
        scheduleSave()
    }

    fun requestPanePick(index: Int, kind: PaneKind) {
        _state.update { it.copy(paneRequest = "${kind.name}:$index", splitActive = index) }
    }

    fun clearPaneRequest() {
        if (_state.value.paneRequest == null) return
        _state.update { it.copy(paneRequest = null) }
    }

    fun stepPanePage(index: Int, delta: Int) {
        _state.update { state ->
            val pane = state.splitPanes.getOrNull(index) ?: return@update state
            val count = pane.pageCount.coerceAtLeast(1)
            state.copy(
                splitPanes = state.splitPanes.mapIndexed { paneIndex, item ->
                    if (paneIndex == index) item.copy(pageIndex = pageStep(item.pageIndex, count, delta)) else item
                }
            )
        }
    }

    fun openPaneDocument(index: Int, uri: Uri) {
        viewModelScope.launch {
            try {
                val opened = withContext(Dispatchers.IO) { copyAndInspect(uri) }
                _state.update { state ->
                    if (index !in state.splitPanes.indices) return@update state
                    state.copy(
                        splitPanes = state.splitPanes.mapIndexed { paneIndex, pane ->
                            if (paneIndex != index) pane
                            else pane.copy(
                                kind = PaneKind.PDF,
                                source = opened.file.absolutePath,
                                label = opened.name,
                                pageIndex = 0,
                                pageCount = opened.pageCount
                            )
                        }
                    )
                }
                scheduleSave()
            } catch (error: Exception) {
                Toast.makeText(context, error.message ?: "Could not open that file", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openPaneVideo(index: Int, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        _state.update { state ->
            if (index !in state.splitPanes.indices) return@update state
            state.copy(
                splitPanes = state.splitPanes.mapIndexed { paneIndex, pane ->
                    if (paneIndex != index) pane
                    else pane.copy(kind = PaneKind.VIDEO, source = uri.toString(), label = "Video")
                }
            )
        }
        scheduleSave()
    }

    fun openPaneWeb(index: Int, url: String) {
        val address = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        _state.update { state ->
            if (index !in state.splitPanes.indices) return@update state
            state.copy(
                splitPanes = state.splitPanes.mapIndexed { paneIndex, pane ->
                    if (paneIndex != index) pane
                    else pane.copy(kind = PaneKind.WEB, source = address, label = address)
                }
            )
        }
        scheduleSave()
    }

    fun toggleDrawToShape() {
        val enabled = !_state.value.isDrawToShapeEnabled
        _state.update { it.copy(isDrawToShapeEnabled = enabled) }
        context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SHAPE_RECOGNITION_KEY, enabled)
            .apply()
    }

    fun setSmartWriteEnabled(enabled: Boolean) {
        _state.update { it.copy(smartWriteEnabled = enabled, smartWriteOffer = if (enabled) it.smartWriteOffer else null) }
        smartPrefs().edit().putBoolean(SMART_WRITE_KEY, enabled).apply()
        if (enabled) prefetchSmartWrite()
    }

    fun setSmartWriteLanguage(tag: String) {
        _state.update { it.copy(smartWriteLanguageTag = tag, smartWriteEnabled = true) }
        smartPrefs().edit().putString(SMART_WRITE_LANG_KEY, tag).putBoolean(SMART_WRITE_KEY, true).apply()
        prefetchSmartWrite()
    }

    fun setSmartWriteInterval(intervalMs: Long) {
        val clamped = if (intervalMs < 750L) 500L else if (intervalMs < 1250L) 1000L else 1500L
        _state.update { it.copy(smartWriteIntervalMs = clamped) }
        smartPrefs().edit().putLong(SMART_WRITE_INTERVAL_KEY, clamped).apply()
    }

    fun dismissSmartWrite() {
        smartJob?.cancel()
        _state.update { it.copy(smartWriteOffer = null, smartWriteBusy = false) }
    }

    fun acceptSmartWrite(text: String) {
        val offer = _state.value.smartWriteOffer ?: return
        val selected = _state.value.strokes.filter { it.id in offer.strokeIds }
        if (selected.isEmpty() || text.isBlank()) {
            dismissSmartWrite()
            return
        }
        replaceHandwriting(selected, text, offer.color, offer.lowConfidence)
    }

    fun convertSelectionToText() {
        if (_state.value.smartWriteBusy) return
        val selectedInk = _state.value.strokes.filter {
            it.id in _state.value.selectedStrokeIds && it.strokeType in HANDWRITING_TYPES && it.points.size >= 2
        }
        val selected = readableHandwriting(selectedInk)
        if (selected.isEmpty()) {
            val message = if (selectedInk.isEmpty()) "Select handwritten text first" else "That line stays as ink"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(smartWriteBusy = true, smartWriteOffer = null) }
            val language = smartWriteLanguage(_state.value.smartWriteLanguageTag)
            val guesses = withContext(Dispatchers.Default) { handwritingReader.read(selected, language) }
            if (!isActive) return@launch
            val best = guesses.firstOrNull()
            if (best == null) {
                _state.update { it.copy(smartWriteBusy = false) }
                Toast.makeText(context, "Could not read that handwriting", Toast.LENGTH_SHORT).show()
                return@launch
            }
            replaceHandwriting(selected, best.text, selected.first().colorValue, isLowConfidence(best.score))
        }
    }

    fun convertSelectionToEquation() {
        if (_state.value.equationBusy) return
        val totalSelected = _state.value.selectedStrokeIds.size
        val selected = _state.value.strokes.filter {
            it.id in _state.value.selectedStrokeIds && isConvertibleEquationInk(it)
        }
        EquationDiagnostics.logSelection(selected, totalSelected)
        if (selected.isEmpty()) {
            Toast.makeText(context, "Select a handwritten equation first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(equationBusy = true, formulaSolution = null) }
            val latex = withTimeoutOrNull(4_000L) {
                withContext(Dispatchers.Default) { readEquation(selected) }
            }
            if (!isActive) return@launch
            if (latex.isNullOrBlank()) {
                _state.update { it.copy(equationBusy = false) }
                val message = if (latex == null) "Equation reading took too long" else "Could not read that equation"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                return@launch
            }
            placeEquation(selected, latex)
        }
    }

    fun solveCurrentEquation() {
        val latex = _state.value.formulaSolution?.latex
            ?: _state.value.strokes.singleOrNull {
                it.id in _state.value.selectedStrokeIds && it.strokeType == StrokeType.EQUATION
            }?.text
            ?: return
        solveEditedEquation(latex)
    }

    fun updateEquationDraft(latex: String) {
        val current = _state.value.formulaSolution ?: return
        if (current.latex == latex) return
        val updated = solveLatex(latex).copy(steps = emptyList(), answerLatex = latex, solved = false)
        val selectedEquationId = _state.value.selectedStrokeIds.singleOrNull()
        val updatedStrokes = if (selectedEquationId != null) {
            _state.value.strokes.map { stroke ->
                if (stroke.id == selectedEquationId && stroke.strokeType == StrokeType.EQUATION) {
                    val font = stroke.strokeWidth
                    val (w, h) = formulaExtent(latex, font)
                    stroke.copy(
                        text = latex,
                        points = listOf(stroke.points[0], Offset(stroke.points[0].x + w, stroke.points[0].y + h))
                    )
                } else stroke
            }
        } else _state.value.strokes
        _state.update { it.copy(formulaSolution = updated, strokes = updatedStrokes) }
    }

    fun solveEditedEquation(latex: String) {
        val clean = latex.trim()
        if (clean.isBlank()) return
        val solved = solveLatex(clean)
        val selectedEquationId = _state.value.selectedStrokeIds.singleOrNull()
        val updatedStrokes = if (selectedEquationId != null) {
            _state.value.strokes.map { stroke ->
                if (stroke.id == selectedEquationId && stroke.strokeType == StrokeType.EQUATION) {
                    val font = stroke.strokeWidth
                    val (w, h) = formulaExtent(clean, font)
                    stroke.copy(
                        text = clean,
                        points = listOf(stroke.points[0], Offset(stroke.points[0].x + w, stroke.points[0].y + h))
                    )
                } else stroke
            }
        } else _state.value.strokes
        _state.update { it.copy(formulaSolution = solved, strokes = updatedStrokes) }
    }

    fun retryEquationConversion() {
        val selectedEquation = _state.value.strokes.singleOrNull {
            it.id in _state.value.selectedStrokeIds && it.strokeType == StrokeType.EQUATION
        }
        if (selectedEquation != null && _state.value.canUndo) {
            undo()
            convertSelectionToEquation()
        } else {
            convertSelectionToEquation()
        }
    }

    fun dismissFormula() {
        _state.update { it.copy(formulaSolution = null, equationBusy = false) }
    }

    private suspend fun readEquation(strokes: List<Stroke>): String {
        val startTotal = System.currentTimeMillis()
        val rawLatex = withTimeoutOrNull(3_500L) {
            coroutineScope {
                val remote = async { remoteVisionRead(strokes) }
                val local = async { localMathRead(strokes) }
                val localText = local.await()
                val remoteText = when {
                    remote.isCompleted -> remote.await()
                    localText.isBlank() -> withTimeoutOrNull(600L) { remote.await() }
                    else -> {
                        remote.cancel()
                        null
                    }
                }
                remoteText?.takeIf { it.isNotBlank() } ?: localText
            }
        }

        if (rawLatex.isNullOrBlank()) {
            EquationDiagnostics.logFailure("Pipeline", "No recognition output from any engine", System.currentTimeMillis() - startTotal)
            return ""
        }

        val startNorm = System.currentTimeMillis()
        val normalized = normalizeMathReading(rawLatex)
        EquationDiagnostics.logNormalized(rawLatex, normalized, System.currentTimeMillis() - startNorm)
        return normalized
    }

    /** On-device read. A fraction bar is split so the numerator and denominator stay apart. */
    private suspend fun localMathRead(strokes: List<Stroke>): String {
        val bar = fractionBar(strokes)
        if (bar != null) {
            val (above, below, beside) = splitFraction(strokes, bar)
            val (numerator, denominator, side) = coroutineScope {
                val top = async { pieceText(above) }
                val bottom = async { pieceText(below) }
                val rest = async { pieceText(beside) }
                Triple(top.await(), bottom.await(), rest.await())
            }
            if (numerator.isNotBlank() && denominator.isNotBlank()) {
                return equationFromReadings(side, numerator, denominator, side)
            }
        }
        return visionRead(strokes)
    }

    private suspend fun pieceText(strokes: List<Stroke>): String {
        if (strokes.isEmpty()) return ""
        val guesses = handwritingReader.readMath(strokes, allowDownload = false)
        return selectBestMathGuess(guesses, strokes.size)?.text?.trim().orEmpty()
    }

    private suspend fun visionRead(strokes: List<Stroke>): String {
        if (strokes.isEmpty()) return ""
        val start = System.currentTimeMillis()
        EquationDiagnostics.logEngine("MLKit Math", "Digital Ink / Spatial")
        val guesses = handwritingReader.readMath(strokes, allowDownload = false)
        val bestGuess = selectBestMathGuess(guesses, strokes.size)
        var rawText = bestGuess?.text.orEmpty()
        val confidence = bestGuess?.score ?: 0f

        // If recognition yielded only a single character (e.g. "y" or "Y") or is empty,
        // but there are 2 or more strokes and one is in the superscript position,
        // isolate the superscript stroke so ML Kit doesn't drop the exponent as stray noise.
        if (rawText.length <= 1 && strokes.size >= 2) {
            val supPair = detectSuperscriptStrokes(strokes)
            if (supPair != null) {
                val supGuesses = handwritingReader.readMath(supPair.second)
                val supBest = supGuesses.firstOrNull()?.text?.trim().orEmpty()
                if (supBest.isNotEmpty() && supBest.any { it.isDigit() || it.isLetter() }) {
                    val baseText = if (rawText.isNotEmpty()) rawText else {
                        handwritingReader.readMath(supPair.first).firstOrNull()?.text?.trim().orEmpty()
                    }
                    if (baseText.isNotEmpty()) {
                        rawText = "${baseText.trim()}^{$supBest}"
                        EquationDiagnostics.logEngine("Spatial Superscript", "Recovered base='$baseText' sup='$supBest'")
                    }
                }
            }
        }

        val elapsed = System.currentTimeMillis() - start
        EquationDiagnostics.logRawResult("MLKit Math", rawText, confidence, elapsed)
        return rawText
    }

    private suspend fun remoteVisionRead(strokes: List<Stroke>): String? {
        val bitmap = renderHandwriting(strokes) ?: return null
        EquationDiagnostics.saveDebugBitmap(context, bitmap)
        // If API_BASE_URL points to 10.0.2.2 on a physical device, skip immediately to avoid 8-second timeout
        if (com.avitrisphare.whiteboard.BuildConfig.API_BASE_URL.contains("10.0.2.2") &&
            !android.os.Build.FINGERPRINT.contains("generic") &&
            !android.os.Build.MODEL.contains("Emulator") &&
            !android.os.Build.MODEL.contains("google_sdk")
        ) {
            EquationDiagnostics.logFailure("Backend Vision", "Skipping backend vision: 10.0.2.2 is unreachable from physical panel.", 0)
            return null
        }
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        val token = PanelSessionStore(context).snapshot().teacher?.accessToken.orEmpty()
        val start = System.currentTimeMillis()
        EquationDiagnostics.logEngine("Backend Vision", "POST /api/v1/math/recognize-equation")
        return when (val result = panelApi.recognizeEquation(base64, token)) {
            is ApiCall.Ok -> {
                val elapsed = System.currentTimeMillis() - start
                val res = result.value
                EquationDiagnostics.logRawResult("Backend Vision", res.latex, res.confidence, elapsed)
                res.latex.takeIf { it.isNotBlank() }
            }
            is ApiCall.Err -> {
                val elapsed = System.currentTimeMillis() - start
                EquationDiagnostics.logFailure("Backend Vision", result.message, elapsed)
                null
            }
        }
    }

    private fun placeEquation(selected: List<Stroke>, latex: String, showDialog: Boolean = true) {
        saveToUndoStack()
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        selected.forEach { stroke ->
            left = minOf(left, stroke.bounds.left)
            top = minOf(top, stroke.bounds.top)
            bottom = maxOf(bottom, stroke.bounds.bottom)
        }
        val font = (bottom - top).coerceIn(48f, 220f)
        val (width, height) = formulaExtent(latex, font)
        val equation = Stroke(
            points = listOf(Offset(left, top), Offset(left + width, top + height)),
            colorValue = selected.first().colorValue,
            strokeWidth = font,
            strokeType = StrokeType.EQUATION,
            text = latex
        )
        val ids = selected.map { it.id }.toSet()
        val formatted = if (showDialog) {
            solveLatex(latex).copy(steps = emptyList(), answerLatex = latex, solved = false)
        } else null
        _state.update { state ->
            state.copy(
                strokes = state.strokes.filterNot { it.id in ids } + equation,
                selectedStrokeIds = if (showDialog) setOf(equation.id) else emptySet(),
                equationBusy = false,
                formulaSolution = formatted,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun extractTextFromSelectedImage() {
        if (_state.value.extractTextBusy) return
        val selectedImage = _state.value.strokes.firstOrNull {
            it.id in _state.value.selectedStrokeIds && it.strokeType == StrokeType.IMAGE
        }
        if (selectedImage == null) {
            showToast("Select an image first")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(extractTextBusy = true) }
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampled(selectedImage.imageUri, 2048)
            }
            if (bitmap == null) {
                _state.update { it.copy(extractTextBusy = false) }
                showToast("Could not load image")
                return@launch
            }

            val extracted = withTimeoutOrNull(4_500L) {
                withContext(Dispatchers.Default) {
                    try {
                        imageTextExtractor.extractText(bitmap)
                    } finally {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                    }
                }
            }

            if (!isActive) return@launch

            if (extracted.isNullOrBlank()) {
                _state.update { it.copy(extractTextBusy = false) }
                val message = if (extracted == null) "Text extraction took too long" else "No readable text found in image"
                showToast(message)
                return@launch
            }

            placeExtractedText(selectedImage, extracted)
        }
    }

    private fun placeExtractedText(image: Stroke, text: String) {
        saveToUndoStack()
        val bounds = image.bounds
        val placement = Offset(bounds.right + 28f, bounds.top)
        val textColor = if (_state.value.isDarkCanvas) 0xFFFFFFFF.toInt() else 0xFF1B1F2A.toInt()
        val textStroke = Stroke(
            points = listOf(placement),
            colorValue = textColor,
            strokeWidth = 20f,
            strokeType = StrokeType.TEXT,
            text = text
        )

        _state.update { state ->
            state.copy(
                strokes = state.strokes + textStroke,
                selectedStrokeIds = setOf(textStroke.id),
                currentTool = if (state.isToolLocked) state.currentTool else WhiteboardTool.SELECT,
                extractTextBusy = false,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
        showToast("Text extracted")
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }

    fun makePdfEditable() {
        val doc = _state.value.document ?: return
        if (doc.kind != DocumentKind.PDF || _state.value.isPdfOcrActive) return
        val file = File(doc.path)
        if (!file.exists()) {
            showToast("PDF file not found")
            return
        }

        val pageCount = _state.value.pageCount
        val isLargePdf = pageCount > 20
        if (isLargePdf) {
            showToast("Processing $pageCount pages in background. You can continue teaching.")
        }

        pdfOcrJob?.cancel()
        pdfOcrJob = viewModelScope.launch(Dispatchers.Default) {
            _state.update {
                it.copy(
                    isPdfOcrActive = true,
                    pdfOcrCurrentPage = 0,
                    pdfOcrTotalPages = pageCount
                )
            }

            val pageResults = mutableListOf<PdfPageOcr>()
            for (pageIndex in 0 until pageCount) {
                if (!isActive) break
                _state.update { it.copy(pdfOcrCurrentPage = pageIndex + 1) }

                val pageBitmap = try {
                    DocumentRenderer.render(file, DocumentKind.PDF, pageIndex)
                } catch (_: Exception) {
                    null
                }

                val pageText = if (pageBitmap != null) {
                    try {
                        imageTextExtractor.extractText(pageBitmap)
                    } catch (_: Exception) {
                        ""
                    } finally {
                        if (!pageBitmap.isRecycled) pageBitmap.recycle()
                    }
                } else {
                    ""
                }

                pageResults.add(PdfPageOcr(pageIndex, pageText))
            }

            if (!isActive) return@launch

            val ocrDoc = PdfOcrDocument(doc.path, pageResults)
            _state.update {
                it.copy(
                    isPdfOcrActive = false,
                    pdfOcrDocument = ocrDoc,
                    showPdfSearchPanel = true
                )
            }
            scheduleSave()

            val totalWords = pageResults.sumOf { it.text.split(Regex("\\s+")).count { w -> w.isNotBlank() } }
            val message = if (isLargePdf) {
                "PDF '${doc.name}' OCR complete: text is searchable & selectable ($pageCount pages, $totalWords words)"
            } else {
                "PDF OCR complete: text is searchable & selectable ($totalWords words recognized)"
            }
            showToast(message)
        }
    }

    fun cancelPdfOcr() {
        pdfOcrJob?.cancel()
        _state.update { it.copy(isPdfOcrActive = false) }
        showToast("PDF OCR stopped")
    }

    fun togglePdfSearchPanel(show: Boolean? = null) {
        _state.update { it.copy(showPdfSearchPanel = show ?: !it.showPdfSearchPanel) }
    }

    fun setPdfSearchQuery(query: String) {
        _state.update { it.copy(pdfSearchQuery = query) }
    }

    fun copyPdfTextToCanvas(text: String) {
        if (text.isBlank()) {
            showToast("No text to copy")
            return
        }
        val doc = _state.value.document
        val placement = if (doc != null && doc.pageWidth > 1f) {
            Offset(doc.pageWidth + 32f, 40f)
        } else {
            CanvasTransform.screenToWorld(
                Offset(viewportWidth / 2f, viewportHeight / 2f),
                _state.value.scale,
                Offset(_state.value.offsetX, _state.value.offsetY)
            )
        }
        val textColor = if (_state.value.isDarkCanvas) 0xFFFFFFFF.toInt() else 0xFF1B1F2A.toInt()

        saveToUndoStack()
        val textStroke = Stroke(
            points = listOf(placement),
            colorValue = textColor,
            strokeWidth = 20f,
            strokeType = StrokeType.TEXT,
            text = text.trim()
        )

        _state.update { state ->
            state.copy(
                strokes = state.strokes + textStroke,
                selectedStrokeIds = setOf(textStroke.id),
                currentTool = if (state.isToolLocked) state.currentTool else WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
        showToast("Text placed on canvas as editable block")
    }

    fun copyPdfTextToClipboard(text: String) {
        if (text.isBlank()) {
            showToast("No text to copy")
            return
        }
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("PDF Text", text)
            clipboard.setPrimaryClip(clip)
            showToast("Copied to clipboard (ready for AI tools)")
        } catch (_: Exception) {
            showToast("Could not copy to clipboard")
        }
    }

    fun recolorSmartWrite(color: Int) {
        _state.update { state ->
            val offer = state.smartWriteOffer ?: return@update state
            state.copy(smartWriteOffer = offer.copy(color = color), selectedColor = color)
        }
    }

    fun beginTextEdit() {
        val text = _state.value.strokes.singleOrNull {
            it.id in _state.value.selectedStrokeIds && it.strokeType == StrokeType.TEXT
        } ?: return
        _state.update { it.copy(editingTextId = text.id) }
    }

    fun editStrokeText(id: String, value: String) {
        if (!textEditUndoCaptured) {
            saveToUndoStack()
            textEditUndoCaptured = true
            redoStack.clear()
        }
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map { stroke -> if (stroke.id == id) stroke.copy(text = value) else stroke },
                smartWriteWarning = null,
                canUndo = true,
                canRedo = false
            )
        }
        scheduleSave()
    }

    fun endTextEdit() {
        textEditUndoCaptured = false
        _state.update { it.copy(editingTextId = null) }
    }

    private fun prefetchSmartWrite() {
        val language = smartWriteLanguage(_state.value.smartWriteLanguageTag)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                handwritingReader.prefetch(language)
            } catch (_: Exception) {
            }
        }
    }

    private fun scheduleSmartWrite() {
        if (!_state.value.smartWriteEnabled) return
        smartJob?.cancel()
        val wait = _state.value.smartWriteIntervalMs
        smartJob = viewModelScope.launch {
            delay(wait)
            val cluster = readableHandwriting(handwritingCluster(_state.value.strokes))
            if (cluster.isEmpty()) return@launch
            _state.update { it.copy(smartWriteBusy = true) }
            val language = smartWriteLanguage(_state.value.smartWriteLanguageTag)
            val guesses = withContext(Dispatchers.Default) { handwritingReader.read(cluster, language) }
            if (!isActive) return@launch
            val texts = guesses.map { it.text }.distinct().take(3)
            if (texts.isEmpty()) {
                _state.update { it.copy(smartWriteBusy = false) }
                return@launch
            }
            val bounds = cluster.map { it.bounds }.reduce { acc, rect ->
                androidx.compose.ui.geometry.Rect(
                    minOf(acc.left, rect.left),
                    minOf(acc.top, rect.top),
                    maxOf(acc.right, rect.right),
                    maxOf(acc.bottom, rect.bottom)
                )
            }
            _state.update {
                it.copy(
                    smartWriteBusy = false,
                    smartWriteOffer = SmartWriteOffer(
                        strokeIds = cluster.map { stroke -> stroke.id },
                        candidates = texts,
                        lowConfidence = isLowConfidence(guesses.first().score),
                        color = cluster.first().colorValue,
                        anchor = Offset(bounds.left, bounds.bottom)
                    )
                )
            }
        }
    }

    private fun replaceHandwriting(selected: List<Stroke>, text: String, color: Int, lowConfidence: Boolean) {
        saveToUndoStack()
        val typed = typedTextStroke(selected, text, color)
        val ids = selected.map { it.id }.toSet()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.filterNot { it.id in ids } + typed,
                selectedStrokeIds = setOf(typed.id),
                editingTextId = typed.id,
                smartWriteOffer = null,
                smartWriteBusy = false,
                smartWriteWarning = if (lowConfidence) "Low confidence — please check" else null,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        textEditUndoCaptured = false
        scheduleSave()
    }

    private fun smartPrefs() = context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)

    fun addLaserPoint(point: Offset) {
        laserJob?.cancel()
        _state.update { it.copy(laserPoints = listOf(point)) }
        laserJob = viewModelScope.launch {
            delay(900)
            _state.update { it.copy(laserPoints = emptyList()) }
        }
    }

    fun strokeAt(worldPoint: Offset): Stroke? {
        fun testStroke(stroke: Stroke): Boolean {
            if (stroke.strokeType == StrokeType.STICKER) return false
            if (stroke.strokeType == StrokeType.IMAGE && stroke.linkToId.isNotBlank()) return false
            return if (stroke.isPlacedMedia() ||
                isMindMapNode(stroke) ||
                isFillableShape(stroke.strokeType) ||
                stroke.fillColor != 0 ||
                stroke.strokeType in listOf(
                    StrokeType.STICKY_NOTE,
                    StrokeType.TEXT,
                    StrokeType.FRAME,
                    StrokeType.EQUATION,
                    StrokeType.RECTANGLE,
                    StrokeType.ROUND_RECT,
                    StrokeType.ROUNDED_RECTANGLE,
                    StrokeType.SQUARE,
                    StrokeType.CIRCLE,
                    StrokeType.ELLIPSE,
                    StrokeType.TRIANGLE,
                    StrokeType.DIAMOND,
                    StrokeType.STAR,
                    StrokeType.HEXAGON,
                    StrokeType.CUBE,
                    StrokeType.CYLINDER,
                    StrokeType.CONE,
                    StrokeType.PYRAMID
                )
            ) {
                val b = strokeSelectionBounds(stroke)
                worldPoint.x >= b.left - 16f && worldPoint.x <= b.right + 16f &&
                worldPoint.y >= b.top - 16f && worldPoint.y <= b.bottom + 16f
            } else {
                CanvasTransform.strokeIntersectsPoint(stroke, worldPoint, thresholdRadius = 24f)
            }
        }

        // Check child / non-container strokes (images, text, handwriting) first so they can be selected independently
        val hitChild = _state.value.strokes.lastOrNull { stroke ->
            !isMindMapNode(stroke) && testStroke(stroke)
        }
        if (hitChild != null) return hitChild

        // Fallback to container mind map nodes
        return _state.value.strokes.lastOrNull { stroke ->
            isMindMapNode(stroke) && testStroke(stroke)
        }
    }

    fun selectStrokeAt(worldPoint: Offset, isMulti: Boolean = false): Boolean {
        val hit = strokeAt(worldPoint)
        if (hit != null) {
            _state.update { s ->
                val newSelection = if (isMulti) s.selectedStrokeIds + hit.id else setOf(hit.id)
                s.copy(selectedStrokeIds = newSelection)
            }
            return true
        } else {
            if (!isMulti) {
                _state.update { it.copy(selectedStrokeIds = emptySet()) }
            }
            return false
        }
    }

    fun selectStroke(id: String) {
        _state.update { it.copy(selectedStrokeIds = setOf(id)) }
    }

    fun selectStrokes(ids: Set<String>) {
        _state.update { it.copy(selectedStrokeIds = ids) }
    }

    fun selectInRect(rect: Rect) {
        if (rect.width < 8f && rect.height < 8f) return
        val selected = _state.value.strokes.filter { stroke ->
            stroke.strokeType != StrokeType.STICKER &&
            rect.overlaps(strokeSelectionBounds(stroke))
        }.map { it.id }.toSet()
        _state.update { it.copy(selectedStrokeIds = selected) }
    }

    fun lassoSelect(lassoPoints: List<Offset>) {
        if (lassoPoints.size < 3) return
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in lassoPoints) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        val lassoRect = Rect(minX, minY, maxX, maxY)

        fun pointInLasso(pt: Offset): Boolean {
            var inside = false
            var j = lassoPoints.size - 1
            for (i in lassoPoints.indices) {
                val pi = lassoPoints[i]
                val pj = lassoPoints[j]
                if ((pi.y > pt.y) != (pj.y > pt.y) &&
                    pt.x < (pj.x - pi.x) * (pt.y - pi.y) / (pj.y - pi.y + 0.00001f) + pi.x
                ) {
                    inside = !inside
                }
                j = i
            }
            return inside
        }

        val selected = _state.value.strokes.filter { stroke ->
            val b = strokeSelectionBounds(stroke)
            if (!lassoRect.overlaps(b)) {
                false
            } else if (pointInLasso(b.center)) {
                true
            } else if (stroke.points.any { pointInLasso(it) }) {
                true
            } else {
                pointInLasso(b.topLeft) || pointInLasso(b.topRight) ||
                pointInLasso(b.bottomLeft) || pointInLasso(b.bottomRight)
            }
        }.map { it.id }.toSet()

        _state.update { it.copy(selectedStrokeIds = selected) }
    }

    fun bucketFillAt(worldPoint: Offset, fillColor: Int) {
        val strokes = _state.value.strokes
        val hitIndex = strokes.indexOfLast { stroke ->
            stroke.bounds.contains(worldPoint) || CanvasTransform.strokeIntersectsPoint(stroke, worldPoint, thresholdRadius = 24f)
        }
        if (hitIndex != -1) {
            saveToUndoStack()
            _state.update { s ->
                val updated = s.strokes.toMutableList()
                val target = updated[hitIndex]
                updated[hitIndex] = target.copy(fillColor = fillColor)
                s.copy(strokes = updated, canUndo = true, canRedo = false)
            }
            redoStack.clear()
            scheduleSave()
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedStrokeIds = emptySet()) }
    }

    private var scaleSnapshot: List<Stroke>? = null

    fun beginSelectionScale() {
        if (!selectionUndoCaptured) {
            saveToUndoStack()
            selectionUndoCaptured = true
        }
        val sel = _state.value.selectedStrokeIds
        scaleSnapshot = _state.value.strokes.filter {
            it.id in sel || it.linkToId in sel ||
                (it.strokeType == StrokeType.CONNECTOR && (it.linkFromId in sel || it.linkToId in sel))
        }
    }

    fun endSelectionScale() {
        scaleSnapshot = null
        selectionUndoCaptured = false
        scheduleSave()
    }

    fun scaleSelectedStrokes(initialBounds: Rect, handle: ShapeHandle, currentPoint: Offset) {
        val snapshot = scaleSnapshot ?: return
        if (snapshot.isEmpty()) return

        val origW = initialBounds.width.coerceAtLeast(20f)
        val origH = initialBounds.height.coerceAtLeast(20f)

        val (anchor, newW, newH) = when (handle) {
            ShapeHandle.BOTTOM_RIGHT -> Triple(
                Offset(initialBounds.left, initialBounds.top),
                (currentPoint.x - initialBounds.left).coerceAtLeast(20f),
                (currentPoint.y - initialBounds.top).coerceAtLeast(20f)
            )
            ShapeHandle.TOP_LEFT -> Triple(
                Offset(initialBounds.right, initialBounds.bottom),
                (initialBounds.right - currentPoint.x).coerceAtLeast(20f),
                (initialBounds.bottom - currentPoint.y).coerceAtLeast(20f)
            )
            ShapeHandle.TOP_RIGHT -> Triple(
                Offset(initialBounds.left, initialBounds.bottom),
                (currentPoint.x - initialBounds.left).coerceAtLeast(20f),
                (initialBounds.bottom - currentPoint.y).coerceAtLeast(20f)
            )
            ShapeHandle.BOTTOM_LEFT -> Triple(
                Offset(initialBounds.right, initialBounds.top),
                (initialBounds.right - currentPoint.x).coerceAtLeast(20f),
                (currentPoint.y - initialBounds.top).coerceAtLeast(20f)
            )
            else -> Triple(
                Offset(initialBounds.left, initialBounds.top),
                (currentPoint.x - initialBounds.left).coerceAtLeast(20f),
                (currentPoint.y - initialBounds.top).coerceAtLeast(20f)
            )
        }

        val scaleX = (newW / origW).coerceIn(0.1f, 15f)
        val scaleY = (newH / origH).coerceIn(0.1f, 15f)
        val avgScale = (scaleX + scaleY) / 2f

        val scaledStrokes = snapshot.map { stroke ->
            if (stroke.stickerKind.contains(":locked")) return@map stroke
            val isMindMap = isMindMapNode(stroke) || stroke.strokeType == StrokeType.CONNECTOR
            val newPoints = if (stroke.strokeType == StrokeType.CONNECTOR) {
                stroke.points
            } else {
                stroke.points.map { pt ->
                    Offset(
                        anchor.x + (pt.x - anchor.x) * scaleX,
                        anchor.y + (pt.y - anchor.y) * scaleY
                    )
                }
            }
            val newWidth = if (isMindMap) stroke.strokeWidth else (stroke.strokeWidth * avgScale).coerceIn(1f, 60f)
            stroke.copy(points = newPoints, strokeWidth = newWidth)
        }

        val scaledMap = scaledStrokes.associateBy { it.id }
        val fullyUpdated = scaledStrokes.map { stroke ->
            if (stroke.strokeType == StrokeType.CONNECTOR) {
                val from = scaledMap[stroke.linkFromId] ?: _state.value.strokes.find { it.id == stroke.linkFromId }
                val to = scaledMap[stroke.linkToId] ?: _state.value.strokes.find { it.id == stroke.linkToId }
                if (from != null && to != null) {
                    val (p1, p2) = if (stroke.stickerKind == "straight") {
                        MindmapEngine.calculateStraightAnchors(from.bounds, to.bounds)
                    } else if (from.stickerKind.startsWith("flow:") || to.stickerKind.startsWith("flow:") || stroke.stickerKind.isEmpty()) {
                        from.bounds.center to to.bounds.center
                    } else {
                        MindmapEngine.calculateAnchors(from.bounds, to.bounds)
                    }
                    stroke.copy(points = listOf(p1, p2))
                } else stroke
            } else stroke
        }

        val updatedMap = fullyUpdated.associateBy { it.id }
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map { stroke ->
                    updatedMap[stroke.id] ?: stroke
                }
            )
        }
    }

    fun selectConnectedMindmap(nodeId: String, addToExisting: Boolean = true) {
        val strokes = _state.value.strokes
        val newConnected = mutableSetOf(nodeId)
        var changed = true
        while (changed) {
            val sizeBefore = newConnected.size
            for (s in strokes) {
                if (s.strokeType == StrokeType.CONNECTOR) {
                    if (s.linkFromId in newConnected || s.linkToId in newConnected) {
                        newConnected.add(s.id)
                        if (s.linkFromId.isNotEmpty()) newConnected.add(s.linkFromId)
                        if (s.linkToId.isNotEmpty()) newConnected.add(s.linkToId)
                    }
                } else if (s.linkToId in newConnected || s.linkFromId in newConnected) {
                    newConnected.add(s.id)
                }
            }
            changed = newConnected.size > sizeBefore
        }
        val current = _state.value.selectedStrokeIds
        val isCurrentMindmap = current.isNotEmpty() && strokes.any { it.id in current && (isMindMapNode(it) || it.strokeType == StrokeType.CONNECTOR) }
        val finalSelection = if (addToExisting && isCurrentMindmap) current + newConnected else newConnected
        _state.update { it.copy(selectedStrokeIds = finalSelection) }
    }

    fun applyMindmapLayout(nodeId: String, isHorizontal: Boolean) {
        saveToUndoStack()
        val updated = MindmapEngine.layoutTree(nodeId, _state.value.strokes, isHorizontal)
        _state.update {
            it.copy(
                strokes = updated,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun toggleLockSelection(nodeId: String? = null) {
        val targetIds = if (nodeId != null) setOf(nodeId) else _state.value.selectedStrokeIds
        if (targetIds.isEmpty()) return
        saveToUndoStack()
        _state.update { s ->
            val anyUnlocked = s.strokes.any { it.id in targetIds && !it.stickerKind.contains(":locked") }
            s.copy(
                strokes = s.strokes.map { stroke ->
                    if (stroke.id in targetIds) {
                        val newKind = if (anyUnlocked) {
                            if (!stroke.stickerKind.contains(":locked")) stroke.stickerKind + ":locked" else stroke.stickerKind
                        } else {
                            stroke.stickerKind.replace(":locked", "")
                        }
                        stroke.copy(stickerKind = newKind)
                    } else stroke
                },
                canUndo = true,
                canRedo = false
            )
        }
        scheduleSave()
    }

    fun moveSelectedStrokes(delta: Offset) {
        if (delta == Offset.Zero || _state.value.selectedStrokeIds.isEmpty()) return
        val selectedIds = _state.value.selectedStrokeIds
        _state.update { s ->
            val selectedNodes = s.strokes.filter { it.id in selectedIds && isMindMapNode(it) }
            val movedStrokes = s.strokes.map { stroke ->
                val shouldMove = (stroke.id in selectedIds ||
                    (stroke.linkToId.isNotEmpty() && stroke.linkToId in selectedIds && stroke.strokeType != StrokeType.CONNECTOR) ||
                    (stroke.strokeType in HANDWRITING_TYPES && selectedNodes.any { node -> node.bounds.contains(stroke.bounds.center) })) &&
                    !stroke.stickerKind.contains(":locked")
                if (shouldMove) {
                    val newPoints = stroke.points.map { Offset(it.x + delta.x, it.y + delta.y) }
                    stroke.copy(points = newPoints)
                } else {
                    stroke
                }
            }
            val strokeMap = movedStrokes.associateBy { it.id }
            val fullyUpdated = movedStrokes.map { stroke ->
                if (stroke.strokeType == StrokeType.CONNECTOR) {
                    val from = strokeMap[stroke.linkFromId]
                    val to = strokeMap[stroke.linkToId]
                    if (from != null && to != null && (from.id in selectedIds || to.id in selectedIds)) {
                        if (stroke.stickerKind.isEmpty() || stroke.stickerKind.startsWith("flow:")) {
                            stroke.copy(points = listOf(from.bounds.center, to.bounds.center))
                        } else {
                            val obstacles = strokeMap.values
                                .filter { it.id != from.id && it.id != to.id && (isMindMapNode(it) || it.isPlacedMedia()) }
                                .map { it.bounds }
                            val parallelCount = movedStrokes.count {
                                it.strokeType == StrokeType.CONNECTOR && it.id != stroke.id &&
                                    (it.linkFromId == from.id || it.linkToId == from.id || it.linkFromId == to.id || it.linkToId == to.id)
                            }
                            val laneOffset = if (parallelCount > 0) ((parallelCount + 1) / 2) * 14f * if (parallelCount % 2 == 1) 1f else -1f else 0f
                            val newPoints = MindmapEngine.calculateCollisionFreeRoute(
                                from = from.bounds,
                                to = to.bounds,
                                obstacles = obstacles,
                                wireStyle = stroke.stickerKind,
                                laneOffset = laneOffset
                            )
                            stroke.copy(points = newPoints)
                        }
                    } else stroke
                } else stroke
            }
            s.copy(strokes = fullyUpdated)
        }
        scheduleSave()
    }

    fun deleteSelectedStrokes() {
        val selectedIds = _state.value.selectedStrokeIds
        if (selectedIds.isEmpty()) return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.filterNot { stroke ->
                    (stroke.id in selectedIds ||
                        stroke.linkToId in selectedIds ||
                        (stroke.strokeType == StrokeType.CONNECTOR && (stroke.linkFromId in selectedIds || stroke.linkToId in selectedIds)))
                        && !stroke.stickerKind.contains(":locked")
                },
                selectedStrokeIds = emptySet(),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun addTextOrSticky(text: String, worldPoint: Offset, isSticky: Boolean, color: Int = 0xFFFFFFFF.toInt(), fill: Int = 0) {
        if (text.isBlank()) return
        saveToUndoStack()
        val stickyFill = if (isSticky && fill == 0) 0xFFFFF9C4.toInt() else fill
        val textColor = if (isSticky) 0xFF171717.toInt() else color
        val size = _state.value.strokeWidth.coerceIn(12f, 72f)
        val newStroke = Stroke(
            points = listOf(worldPoint),
            colorValue = textColor,
            strokeWidth = size,
            strokeType = if (isSticky) StrokeType.STICKY_NOTE else StrokeType.TEXT,
            text = text,
            fillColor = stickyFill
        )
        _state.update { s ->
            s.copy(
                strokes = s.strokes + newStroke,
                selectedStrokeIds = setOf(newStroke.id),
                currentTool = if (s.isToolLocked) s.currentTool else WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun addImage(uriString: String, worldPoint: Offset) {
        if (uriString.isBlank()) return
        val targetBoxId = targetMindmapBoxId
        targetMindmapBoxId = null
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { storeImage(uriString) } ?: uriString
            if (targetBoxId != null) {
                val targetBox = _state.value.strokes.firstOrNull { it.id == targetBoxId }
                if (targetBox != null) {
                    saveToUndoStack()
                    _state.update { s ->
                        val cleaned = s.strokes.filterNot { it.linkToId == targetBoxId && it.strokeType == StrokeType.IMAGE }
                        val updated = cleaned.map {
                            if (it.id == targetBoxId) it.copy(imageUri = stored) else it
                        }
                        s.copy(
                            strokes = updated,
                            selectedStrokeIds = setOf(targetBoxId),
                            canUndo = true,
                            canRedo = false
                        )
                    }
                    redoStack.clear()
                    scheduleSave()
                    return@launch
                }
            }
            placeMedia(StrokeType.IMAGE, worldPoint, 520f, 360f, imageUri = stored)
        }
    }

    fun addLocalVideo(uriString: String, worldPoint: Offset) {
        if (uriString.isBlank()) return
        try {
            context.contentResolver.takePersistableUriPermission(
                Uri.parse(uriString),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        placeMedia(StrokeType.VIDEO, worldPoint, 640f, 360f, imageUri = uriString)
    }

    fun addYouTube(raw: String, worldPoint: Offset) {
        val embed = youtubeEmbedUrl(raw) ?: return
        placeMedia(StrokeType.VIDEO, worldPoint, 640f, 360f, text = embed)
    }

    fun addWebEmbed(url: String, worldPoint: Offset) {
        val normalized = normalizeWebUrl(url)
        placeMedia(StrokeType.WEB_EMBED, worldPoint, 780f, 480f, text = normalized)
    }

    private fun placeMedia(
        type: StrokeType,
        worldPoint: Offset,
        width: Float,
        height: Float,
        imageUri: String = "",
        text: String = ""
    ) {
        val targetBox = targetMindmapBoxId?.let { id -> _state.value.strokes.firstOrNull { it.id == id } }
        targetMindmapBoxId = null

        if (targetBox != null && type == StrokeType.IMAGE) {
            saveToUndoStack()
            _state.update { s ->
                val cleaned = s.strokes.filterNot { it.linkToId == targetBox.id && it.strokeType == StrokeType.IMAGE }
                val updated = cleaned.map {
                    if (it.id == targetBox.id) it.copy(imageUri = imageUri) else it
                }
                s.copy(
                    strokes = updated,
                    selectedStrokeIds = setOf(targetBox.id),
                    canUndo = true,
                    canRedo = false
                )
            }
            redoStack.clear()
            scheduleSave()
            return
        }

        val (placedPoint, placedW, placedH) = Triple(worldPoint, width, height)

        saveToUndoStack()
        val newStroke = Stroke(
            points = listOf(placedPoint, Offset(placedPoint.x + placedW, placedPoint.y + placedH)),
            strokeType = type,
            imageUri = imageUri,
            text = text,
            colorValue = 0xFF6366F1.toInt(),
            linkToId = targetBox?.id ?: ""
        )
        _state.update { s ->
            s.copy(
                strokes = s.strokes + newStroke,
                selectedStrokeIds = setOf(newStroke.id),
                currentTool = if (s.isToolLocked) s.currentTool else WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    private fun storeImage(uriString: String): String? {
        return try {
            val bitmap = decodeSampled(uriString, 1600) ?: return null
            val dir = File(context.filesDir, "media")
            dir.mkdirs()
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 86, it) }
            bitmap.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampled(uriString: String, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openImage(uriString)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxEdge * 2 || bounds.outHeight / sample > maxEdge * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return openImage(uriString)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun openImage(uriString: String): java.io.InputStream? {
        return when {
            uriString.startsWith("/") -> File(uriString).inputStream()
            uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                val connection = java.net.URL(uriString).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.getInputStream()
            }
            else -> context.contentResolver.openInputStream(Uri.parse(uriString))
        }
    }

    fun updateStrokeText(id: String, newText: String) {
        saveToUndoStack()
        _state.update { s ->
            val cleaned = s.strokes.filterNot { it.linkToId == id && it.strokeType == StrokeType.TEXT }
            val updated = cleaned.map {
                if (it.id == id) it.copy(text = newText) else it
            }
            s.copy(strokes = updated, canUndo = true, canRedo = false)
        }
        scheduleSave()
    }

    fun applyShapeRecognition(points: List<Offset>): Stroke? {
        val result = ShapeRecognitionHelper.recognize(points) ?: return null
        if (result.confidence < 0.7f) return null
        return Stroke(
            points = result.points,
            colorValue = _state.value.selectedColor,
            strokeWidth = _state.value.strokeWidth,
            strokeType = result.strokeType
        )
    }

    fun replaceStroke(updated: Stroke) {
        if (_state.value.strokes.none { it.id == updated.id }) return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map { if (it.id == updated.id) updated else it },
                selectedStrokeIds = setOf(updated.id),
                currentTool = if (s.isToolLocked) s.currentTool else WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun resizeStroke(id: String, hit: ShapeHandleHit, point: Offset) {
        _state.update { s ->
            val orig = s.strokes.find { it.id == id }
            val isParentNode = orig != null && isMindMapNode(orig)
            val isLinkedImage = orig != null && orig.strokeType == StrokeType.IMAGE && orig.linkToId.isNotBlank()
            val parentNode = if (isLinkedImage) s.strokes.find { it.id == orig?.linkToId } else null

            var resizedTarget = if (orig != null) resizeShape(orig, hit, point) else orig

            // If resizing an image inside a parent box, clamp strictly within parent bounds
            if (isLinkedImage && parentNode != null && resizedTarget != null && resizedTarget.points.size >= 2) {
                val pb = parentNode.bounds
                val p1 = resizedTarget.points.first()
                val p2 = resizedTarget.points.last()
                val left = minOf(p1.x, p2.x).coerceAtLeast(pb.left + 4f)
                val top = minOf(p1.y, p2.y).coerceAtLeast(pb.top + 4f)
                val right = maxOf(p1.x, p2.x).coerceAtMost(pb.right - 4f).coerceAtLeast(left + 16f)
                val bottom = maxOf(p1.y, p2.y).coerceAtMost(pb.bottom - 4f).coerceAtLeast(top + 16f)
                resizedTarget = resizedTarget.copy(points = listOf(Offset(left, top), Offset(right, bottom)))
            }

            val updated = s.strokes.map { stroke ->
                if (stroke.id == id && resizedTarget != null) resizedTarget else stroke
            }

            // If parent node was resized, scale all linked child strokes proportionally
            val fullyScaled = if (isParentNode && orig != null && resizedTarget != null) {
                val oldB = orig.bounds
                val newB = resizedTarget.bounds
                if (oldB.width > 0f && oldB.height > 0f) {
                    val sx = (newB.width / oldB.width).coerceIn(0.1f, 10f)
                    val sy = (newB.height / oldB.height).coerceIn(0.1f, 10f)
                    val avgS = (sx + sy) / 2f
                    updated.map { stroke ->
                        if (stroke.linkToId == id && stroke.strokeType != StrokeType.CONNECTOR) {
                            val scaledPts = stroke.points.map { pt ->
                                Offset(newB.left + (pt.x - oldB.left) * sx, newB.top + (pt.y - oldB.top) * sy)
                            }
                            val clampedPts = if (stroke.strokeType == StrokeType.IMAGE && scaledPts.size >= 2) {
                                val pad = 4f
                                val p1 = scaledPts.first()
                                val p2 = scaledPts.last()
                                val clLeft = minOf(p1.x, p2.x).coerceAtLeast(newB.left + pad)
                                val clTop = minOf(p1.y, p2.y).coerceAtLeast(newB.top + pad)
                                val clRight = maxOf(p1.x, p2.x).coerceAtMost(newB.right - pad).coerceAtLeast(clLeft + 16f)
                                val clBottom = maxOf(p1.y, p2.y).coerceAtMost(newB.bottom - pad).coerceAtLeast(clTop + 16f)
                                listOf(Offset(clLeft, clTop), Offset(clRight, clBottom))
                            } else {
                                scaledPts
                            }
                            val newW = if (stroke.strokeType == StrokeType.IMAGE) stroke.strokeWidth
                                       else (stroke.strokeWidth * avgS).coerceIn(1f, 60f)
                            stroke.copy(points = clampedPts, strokeWidth = newW)
                        } else stroke
                    }
                } else updated
            } else updated

            val strokeMap = fullyScaled.associateBy { it.id }
            val withConnectors = fullyScaled.map { stroke ->
                if (stroke.strokeType == StrokeType.CONNECTOR && (stroke.linkFromId == id || stroke.linkToId == id)) {
                    val from = strokeMap[stroke.linkFromId]
                    val to = strokeMap[stroke.linkToId]
                    if (from != null && to != null) {
                        val (p1, p2) = if (stroke.stickerKind == "straight") {
                            MindmapEngine.calculateStraightAnchors(from.bounds, to.bounds)
                        } else {
                            MindmapEngine.calculateAnchors(from.bounds, to.bounds)
                        }
                        stroke.copy(points = listOf(p1, p2))
                    } else stroke
                } else stroke
            }
            s.copy(strokes = withConnectors)
        }
        scheduleSave()
    }

    fun recolorSelected(color: Int) {
        setSelectionBorderColor(color)
    }

    fun setSelectionBorderColor(color: Int) {
        if (_state.value.splitCount > 1) {
            val active = _state.value.splitActive.coerceIn(0, 5)
            val currentPanes = _state.value.splitPanes
            val activePane = currentPanes.getOrNull(active) ?: return
            val selected = _state.value.selectedStrokeIds
            if (selected.isEmpty()) return
            beginPaneEdit(active)
            val updatedStrokes = activePane.strokes.map { stroke ->
                if (stroke.id in selected) stroke.copy(colorValue = color) else stroke
            }
            setPaneStrokes(active, updatedStrokes)
            _state.update { s ->
                s.copy(
                    selectedColor = color,
                    shapeBorderColor = color,
                    splitPanes = s.splitPanes.mapIndexed { idx, p ->
                        if (idx == active) p.copy(color = color) else p
                    }
                )
            }
            scheduleSave()
            return
        }
        if (_state.value.selectedStrokeIds.isEmpty()) return
        saveToUndoStack()
        val selected = _state.value.selectedStrokeIds
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map { stroke ->
                    if (stroke.id in selected) stroke.copy(colorValue = color) else stroke
                },
                selectedColor = color,
                shapeBorderColor = color,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setSelectionFillColor(color: Int) {
        if (_state.value.selectedStrokeIds.isEmpty()) return
        saveToUndoStack()
        val selected = _state.value.selectedStrokeIds
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map { stroke ->
                    if (stroke.id in selected) stroke.copy(fillColor = color) else stroke
                },
                fillColor = color,
                shapeFillColor = color,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    private fun renumberDefaultPageTitles() {
        for (i in pageTitles.indices) {
            val title = pageTitles[i]
            if (title.matches(Regex("^Page\\s+\\d+$", RegexOption.IGNORE_CASE))) {
                pageTitles[i] = "Page ${i + 1}"
            }
        }
    }

    fun addPage() {
        if (_state.value.document != null) {
            _state.update { it.copy(errorMessage = "Adding pages is disabled for document-backed boards.") }
            return
        }
        if (pages.size >= MAX_PAGES) {
            _state.update { it.copy(errorMessage = "Maximum page limit of $MAX_PAGES reached.") }
            return
        }
        commitActivePage()
        pages.add(emptyList())
        pageTitles.add("Page ${pages.size + 1}")
        val newBgKey = if (_state.value.isDarkCanvas) CanvasBackground.DARK.key else CanvasBackground.DOTTED.key
        pageBackgroundKeys.add(newBgKey)
        pageMathTools.add(emptyList())
        renumberDefaultPageTitles()
        syncPageMetadataLists()
        switchTo(pages.lastIndex, resetTransform = true)
        scheduleSave()
    }

    fun renamePage(index: Int, title: String) {
        if (index !in pages.indices) return
        val trimmed = title.trim().ifBlank { "Page ${index + 1}" }
        pageTitles[index] = trimmed
        _state.update { it.copy(pageTitles = pageTitles.toList()) }
        scheduleSave()
        viewModelScope.launch(Dispatchers.IO) {
            persistSnapshot()
        }
    }

    fun deletePage(index: Int): Boolean {
        if (_state.value.document != null) {
            _state.update { it.copy(errorMessage = "Deleting pages is disabled for document-backed boards.") }
            return false
        }
        if (pages.size <= 1) {
            _state.update { it.copy(errorMessage = "The last remaining page cannot be deleted.") }
            return false
        }
        if (index !in pages.indices) return false
        commitActivePage()
        val deletedStrokes = pages[index]
        val deletedTitle = pageTitles.getOrElse(index) { "Page ${index + 1}" }
        val deletedBg = pageBackgroundKeys.getOrElse(index) { CanvasBackground.DOTTED.key }
        val deletedCustomBg = pageCustomBackgroundUris.getOrElse(index) { "" }
        val prevActive = activePage

        undoActionStack.add(
            CanvasUndoEntry.DeletedPage(
                index = index,
                strokes = deletedStrokes,
                title = deletedTitle,
                backgroundKey = deletedBg,
                customBackgroundUri = deletedCustomBg,
                previousActivePage = prevActive
            )
        )
        if (undoActionStack.size > MAX_UNDO) undoActionStack.removeAt(0)
        redoActionStack.clear()
        redoStack.clear()

        pages.removeAt(index)
        pageTitles.removeAt(index)
        pageBackgroundKeys.removeAt(index)
        pageCustomBackgroundUris.removeAt(index)
        if (index in pageMathTools.indices) pageMathTools.removeAt(index)
        renumberDefaultPageTitles()
        activePage = activePage.coerceIn(0, pages.lastIndex)
        if (activePage >= index && activePage > 0) activePage -= 1
        switchTo(activePage, clearPageUndo = false)
        _state.update { it.copy(canUndo = true, canRedo = false) }
        scheduleSave()
        return true
    }

    fun duplicatePage(index: Int) {
        if (_state.value.document != null) {
            _state.update { it.copy(errorMessage = "Duplicating pages is disabled for document-backed boards.") }
            return
        }
        if (index !in pages.indices || pages.size >= MAX_PAGES) return
        commitActivePage()
        val originalStrokes = pages[index]
        val idMap = mutableMapOf<String, String>()
        originalStrokes.forEach { idMap[it.id] = UUID.randomUUID().toString() }
        val copy = originalStrokes.map { s ->
            val newId = idMap[s.id] ?: UUID.randomUUID().toString()
            if (s.strokeType == StrokeType.CONNECTOR) {
                val newFrom = idMap[s.linkFromId] ?: s.linkFromId
                val newTo = idMap[s.linkToId] ?: s.linkToId
                s.copy(id = newId, linkFromId = newFrom, linkToId = newTo)
            } else {
                s.copy(id = newId)
            }
        }
        pages.add(index + 1, copy)
        pageTitles.add(index + 1, "${pageTitles.getOrElse(index) { "Page" }} copy")
        pageBackgroundKeys.add(index + 1, pageBackgroundKeys.getOrElse(index) { CanvasBackground.DOTTED.key })
        pageCustomBackgroundUris.add(index + 1, pageCustomBackgroundUris.getOrElse(index) { "" })
        while (pageMathTools.size <= index) pageMathTools.add(emptyList())
        val copyTools = pageMathTools.getOrElse(index) { emptyList() }.map { it.copy(id = UUID.randomUUID().toString()) }
        pageMathTools.add(index + 1, copyTools)
        switchTo(index + 1)
        scheduleSave()
    }

    fun copySelectedStrokes() {
        if (_state.value.document != null) {
            _state.update {
                it.copy(
                    errorMessage = "Copying is not supported on document-backed boards because pages are tied to the PDF file."
                )
            }
            return
        }
        val selected = _state.value.selectedStrokeIds
        if (selected.isEmpty()) return
        strokeClipboard = _state.value.strokes.filter { it.id in selected }
        _state.update { it.copy(hasStrokeClipboard = strokeClipboard.isNotEmpty()) }
    }

    fun pasteStrokeClipboard() {
        if (_state.value.document != null) {
            _state.update {
                it.copy(
                    errorMessage = "Pasting is not supported on document-backed boards because pages are tied to the PDF file."
                )
            }
            return
        }
        if (strokeClipboard.isEmpty()) return
        saveToUndoStack()
        val offset = Offset(36f, 36f)
        val idMap = mutableMapOf<String, String>()
        strokeClipboard.forEach { idMap[it.id] = UUID.randomUUID().toString() }
        val pasted = strokeClipboard.map { stroke ->
            val newId = idMap[stroke.id] ?: UUID.randomUUID().toString()
            val newPoints = stroke.points.map { Offset(it.x + offset.x, it.y + offset.y) }
            if (stroke.strokeType == StrokeType.CONNECTOR) {
                val newFrom = idMap[stroke.linkFromId] ?: stroke.linkFromId
                val newTo = idMap[stroke.linkToId] ?: stroke.linkToId
                stroke.copy(
                    id = newId,
                    points = newPoints,
                    linkFromId = newFrom,
                    linkToId = newTo
                )
            } else {
                stroke.copy(
                    id = newId,
                    points = newPoints
                )
            }
        }
        _state.update { s ->
            s.copy(
                strokes = s.strokes + pasted,
                selectedStrokeIds = pasted.map { it.id }.toSet(),
                canUndo = true,
                canRedo = false
            )
        }
        redoActionStack.clear()
        redoStack.clear()
        scheduleSave()
    }

    fun swapPenColors() {
        _state.update { s ->
            val newInkTool = s.secondaryInkTool
            val newSecondaryInkTool = s.inkTool
            s.copy(
                selectedColor = s.secondaryColor,
                secondaryColor = s.selectedColor,
                strokeWidth = s.secondaryStrokeWidth,
                secondaryStrokeWidth = s.strokeWidth,
                inkTool = newInkTool,
                secondaryInkTool = newSecondaryInkTool,
                currentTool = if (s.useSecondaryPenColor) newSecondaryInkTool else newInkTool
            )
        }
        scheduleSave()
    }

    fun setSecondaryInkTool(tool: WhiteboardTool) {
        val width = toolWidths[tool]
        _state.update {
            it.copy(
                secondaryInkTool = tool,
                secondaryStrokeWidth = width ?: it.secondaryStrokeWidth,
                currentTool = if (it.useSecondaryPenColor) tool else it.currentTool
            )
        }
        scheduleSave()
    }

    fun setActivePenSlot(slot: Int) {
        val useSecondary = slot == 2
        _state.update {
            it.copy(
                useSecondaryPenColor = useSecondary,
                currentTool = if (useSecondary) it.secondaryInkTool else it.inkTool
            )
        }
    }

    fun setSecondaryStrokeWidth(width: Float) {
        val clamped = width.coerceIn(1f, 50f)
        _state.update { it.copy(secondaryStrokeWidth = clamped) }
        scheduleSave()
    }

    fun setSecondaryColor(color: Int) {
        _state.update { it.copy(secondaryColor = color) }
        scheduleSave()
    }

    fun setInkSampleGapMm(mm: Float) {
        _state.update { it.copy(inkSampleGapMm = mm.coerceIn(0.15f, 2f)) }
        scheduleSave()
    }

    fun setPalmEraserEnabled(enabled: Boolean) {
        _state.update { it.copy(palmEraserEnabled = enabled) }
        scheduleSave()
    }

    fun setRestrictEraserToSelection(enabled: Boolean) {
        _state.update { it.copy(restrictEraserToSelection = enabled) }
        scheduleSave()
    }

    fun setDocumentFrame(x: Float, y: Float, scale: Float) {
        _state.update {
            it.copy(
                documentFrameX = x,
                documentFrameY = y,
                documentFrameScale = scale.coerceIn(0.2f, 3f)
            )
        }
        scheduleSave()
    }

    fun resetDocumentFrame() {
        setDocumentFrame(0f, 0f, 1f)
    }

    fun setDocumentForTesting(doc: BoardDocument?) {
        _state.update { it.copy(document = doc) }
    }

    fun setPdfOcrDocumentForTesting(doc: PdfOcrDocument?) {
        _state.update { it.copy(pdfOcrDocument = doc) }
    }

    fun setSmartWriteOfferForTesting(offer: SmartWriteOffer?) {
        _state.update { it.copy(smartWriteOffer = offer) }
    }

    fun setPageBackground(background: CanvasBackground, customUri: String = "") {
        syncPageMetadataLists()
        val toDark = (background == CanvasBackground.DARK)
        val wasDark = (_state.value.canvasBackground == CanvasBackground.DARK)
        pageBackgroundKeys[activePage] = background.key
        pageCustomBackgroundUris[activePage] = if (background == CanvasBackground.CUSTOM) customUri else ""
        val updatedStrokes = if (toDark != wasDark) {
            CanvasInkTheme.remapStrokesForTheme(_state.value.strokes, toDark)
        } else {
            _state.value.strokes
        }
        if (activePage in pages.indices) {
            pages[activePage] = updatedStrokes
        }
        _state.update {
            it.copy(
                canvasBackground = background,
                isDarkCanvas = toDark,
                strokes = updatedStrokes,
                pagePreviews = pages.toList(),
                pageBackgroundKeys = pageBackgroundKeys.toList(),
                pageCustomBackgroundUris = pageCustomBackgroundUris.toList()
            )
        }
        scheduleSave()
    }

    fun setSelectedImageAsBackground(imageStrokeId: String? = null): Boolean {
        val targetId = imageStrokeId ?: _state.value.selectedStrokeIds.firstOrNull()
        val imageStroke = if (targetId != null) {
            _state.value.strokes.firstOrNull { it.id == targetId && it.strokeType == StrokeType.IMAGE && it.imageUri.isNotBlank() }
        } else {
            _state.value.strokes.firstOrNull { it.strokeType == StrokeType.IMAGE && it.imageUri.isNotBlank() }
        } ?: return false

        saveToUndoStack()
        setPageBackground(CanvasBackground.CUSTOM, imageStroke.imageUri)
        val remaining = _state.value.strokes.filterNot { it.id == imageStroke.id }
        pages[activePage] = remaining
        _state.update {
            it.copy(
                strokes = remaining,
                selectedStrokeIds = emptySet(),
                canUndo = true
            )
        }
        scheduleSave()
        return true
    }

    fun configureHelpPanel(panelId: String, licenseState: String) {
        _state.update { it.copy(panelIdForHelp = panelId, licenseStateForHelp = licenseState) }
    }

    fun setUiLocaleTag(tag: String) {
        _state.update { it.copy(uiLocaleTag = tag) }
    }

    fun dismissSaveToast() {
        _state.update { it.copy(saveToast = null) }
    }

    fun confirmSave(onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            saveNow()
            _state.update { it.copy(saveToast = "Saved") }
            onDone?.invoke()
        }
    }

    fun exportAllSlidesPdf(onExported: (Uri?) -> Unit) {
        val document = _state.value.document
        if (document != null) {
            exportAnnotatedPdf { onExported(it) }
            return
        }
        commitActivePage()
        val layers = pages.map { it.toList() }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdf = android.graphics.pdf.PdfDocument()
                layers.forEachIndexed { index, strokes ->
                    val bounds = CanvasTransform.calculateContentBounds(strokes)
                    val w = (bounds?.width?.plus(200f) ?: 1920f).coerceIn(800f, 3840f).toInt()
                    val h = (bounds?.height?.plus(200f) ?: 1080f).coerceIn(600f, 3840f).toInt()
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(
                        when (CanvasBackground.fromKey(pageBackgroundKeys.getOrElse(index) { CanvasBackground.DOTTED.key })) {
                            CanvasBackground.DARK -> 0xFF171717.toInt()
                            else -> 0xFFFFFFFF.toInt()
                        }
                    )
                    val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
                    val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)
                    scope.draw(
                        androidx.compose.ui.unit.Density(1f),
                        androidx.compose.ui.unit.LayoutDirection.Ltr,
                        composeCanvas,
                        androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat())
                    ) {
                        strokes.forEach { stroke -> renderStroke(context, stroke, 1f, false) }
                    }
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, index + 1).create()
                    val page = pdf.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdf.finishPage(page)
                    bitmap.recycle()
                }
                val uri = writePdf(pdf, "${_state.value.noteTitle}_slides.pdf")
                pdf.close()
                withContext(Dispatchers.Main) { onExported(uri) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onExported(null) }
            }
        }
    }

    fun makePdfEditableCurrentPage() {
        val doc = _state.value.document ?: return
        if (doc.kind != DocumentKind.PDF || _state.value.isPdfOcrActive) return
        val file = File(doc.path)
        val pageIndex = _state.value.pageIndex
        pdfOcrJob?.cancel()
        pdfOcrJob = viewModelScope.launch(Dispatchers.Default) {
            _state.update { it.copy(isPdfOcrActive = true, pdfOcrCurrentPage = 1, pdfOcrTotalPages = 1) }
            val pageBitmap = try {
                DocumentRenderer.render(file, DocumentKind.PDF, pageIndex)
            } catch (_: Exception) {
                null
            }
            val pageText = if (pageBitmap != null) {
                try {
                    imageTextExtractor.extractText(pageBitmap)
                } catch (_: Exception) {
                    ""
                } finally {
                    if (!pageBitmap.isRecycled) pageBitmap.recycle()
                }
            } else ""
            if (!isActive) return@launch
            val existing = _state.value.pdfOcrDocument
            val merged = existing?.pages?.toMutableList() ?: mutableListOf()
            merged.removeAll { it.pageIndex == pageIndex }
            merged.add(PdfPageOcr(pageIndex, pageText))
            val ocrDoc = PdfOcrDocument(doc.path, merged.sortedBy { it.pageIndex })
            _state.update {
                it.copy(isPdfOcrActive = false, pdfOcrDocument = ocrDoc, showPdfSearchPanel = true)
            }
            scheduleSave()
        }
    }

    fun insertEmoji(text: String, world: Offset) {
        insertSticker(kind = "emoji", world = world, text = text)
    }

    fun insertSticker(kind: String, world: Offset, text: String = "") {
        val s = _state.value
        val targetNode = targetMindmapBoxId?.let { id -> s.strokes.firstOrNull { it.id == id && (it.stickerKind.startsWith("mindmap:") || it.stickerKind.startsWith("flow:")) } }
        targetMindmapBoxId = null
        val (origin, stickerSize) = if (targetNode != null) {
            val b = strokeSelectionBounds(targetNode)
            Offset(b.right - 36f, b.top - 18f) to 52f
        } else {
            world to 96f
        }
        val stroke = Stroke(
            points = listOf(origin, Offset(origin.x + stickerSize, origin.y + stickerSize)),
            colorValue = s.selectedColor,
            strokeWidth = s.strokeWidth.coerceAtLeast(4f),
            strokeType = StrokeType.STICKER,
            stickerKind = kind,
            text = text
        )
        if (s.splitCount > 1 && s.splitPanes.isNotEmpty()) {
            val activeIndex = s.splitActive.coerceIn(0, s.splitPanes.lastIndex)
            val currentPanes = s.splitPanes.toMutableList()
            val activePane = currentPanes[activeIndex]
            paneUndo[activeIndex].addLast(activePane.strokes)
            currentPanes[activeIndex] = activePane.copy(strokes = activePane.strokes + stroke)
            _state.update {
                it.copy(
                    splitPanes = currentPanes,
                    selectedStrokeIds = emptySet(),
                    canUndo = true,
                    canRedo = false
                )
            }
        } else {
            saveToUndoStack()
            _state.update {
                it.copy(
                    strokes = it.strokes + stroke,
                    selectedStrokeIds = setOf(stroke.id),
                    canUndo = true,
                    canRedo = false
                )
            }
            redoStack.clear()
        }
        scheduleSave()
    }

    fun addFlowchartNode(role: String, world: Offset, labelText: String = "") {
        val type = when (role.lowercase()) {
            "decision" -> StrokeType.DIAMOND
            "terminator" -> StrokeType.ELLIPSE
            else -> StrokeType.RECTANGLE
        }
        saveToUndoStack()
        var targetWorld = world
        val existingFlowNodes = _state.value.strokes.filter {
            it.stickerKind.startsWith("flow:") || isMindMapNode(it)
        }
        while (existingFlowNodes.any { (it.bounds.topLeft - targetWorld).getDistance() < 50f }) {
            targetWorld = Offset(targetWorld.x + 40f, targetWorld.y + 110f)
        }
        val stroke = Stroke(
            points = listOf(targetWorld, Offset(targetWorld.x + 180f, targetWorld.y + 100f)),
            colorValue = _state.value.selectedColor,
            strokeWidth = _state.value.strokeWidth.coerceAtLeast(3f),
            strokeType = type,
            stickerKind = "flow:$role",
            text = labelText
        )
        _state.update { s ->
            s.copy(strokes = s.strokes + stroke, selectedStrokeIds = setOf(stroke.id), canUndo = true, canRedo = false)
        }
        redoStack.clear()
        scheduleSave()
    }

    fun connectNodes(fromNodeId: String, toNodeId: String) {
        val s = _state.value
        val from = s.strokes.find { it.id == fromNodeId } ?: return
        val to = s.strokes.find { it.id == toNodeId } ?: return
        saveToUndoStack()
        val connector = Stroke(
            points = listOf(from.bounds.center, to.bounds.center),
            colorValue = s.selectedColor,
            strokeWidth = 3.5f,
            strokeType = StrokeType.CONNECTOR,
            linkFromId = fromNodeId,
            linkToId = toNodeId
        )
        _state.update {
            it.copy(strokes = it.strokes + connector, canUndo = true, canRedo = false)
        }
        redoStack.clear()
        scheduleSave()
    }

    fun createThreeNodeFlowchart(startPos: Offset) {
        saveToUndoStack()
        val s = _state.value
        val term = Stroke(
            points = listOf(startPos, Offset(startPos.x + 180f, startPos.y + 100f)),
            colorValue = s.selectedColor,
            strokeWidth = s.strokeWidth.coerceAtLeast(3f),
            strokeType = StrokeType.ELLIPSE,
            stickerKind = "flow:terminator",
            text = ""
        )
        val procPos = Offset(startPos.x + 280f, startPos.y)
        val proc = Stroke(
            points = listOf(procPos, Offset(procPos.x + 180f, procPos.y + 100f)),
            colorValue = s.selectedColor,
            strokeWidth = s.strokeWidth.coerceAtLeast(3f),
            strokeType = StrokeType.RECTANGLE,
            stickerKind = "flow:process",
            text = ""
        )
        val decPos = Offset(procPos.x + 280f, procPos.y)
        val dec = Stroke(
            points = listOf(decPos, Offset(decPos.x + 180f, decPos.y + 100f)),
            colorValue = s.selectedColor,
            strokeWidth = s.strokeWidth.coerceAtLeast(3f),
            strokeType = StrokeType.DIAMOND,
            stickerKind = "flow:decision",
            text = ""
        )
        val arrow1 = Stroke(
            points = listOf(term.bounds.center, proc.bounds.center),
            colorValue = s.selectedColor,
            strokeWidth = 3.5f,
            strokeType = StrokeType.CONNECTOR,
            linkFromId = term.id,
            linkToId = proc.id
        )
        val arrow2 = Stroke(
            points = listOf(proc.bounds.center, dec.bounds.center),
            colorValue = s.selectedColor,
            strokeWidth = 3.5f,
            strokeType = StrokeType.CONNECTOR,
            linkFromId = proc.id,
            linkToId = dec.id
        )
        _state.update {
            it.copy(
                strokes = it.strokes + listOf(term, arrow1, proc, arrow2, dec),
                selectedStrokeIds = setOf(proc.id),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun createMindMapTemplate(
        kind: MindmapTemplateKind,
        center: Offset,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f
    ) {
        saveToUndoStack()

        // Position within visible viewport avoiding collision with existing mind maps
        val effectiveCenter = MindmapEngine.calculateNonOverlappingCenter(
            existingStrokes = _state.value.strokes,
            requestedCenter = center
        )

        val templateStrokes = MindmapEngine.generateTemplate(kind, effectiveCenter)
        val rootNode = templateStrokes.firstOrNull { it.stickerKind == "mindmap:root" } ?: templateStrokes.firstOrNull()

        _state.update { s ->
            s.copy(
                strokes = s.strokes + templateStrokes,
                selectedStrokeIds = rootNode?.let { setOf(it.id) } ?: emptySet(),
                currentTool = WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun addMindMapBranchDirection(parentId: String, direction: String) {
        val parent = _state.value.strokes.find { it.id == parentId } ?: return
        saveToUndoStack()
        val branchIndex = _state.value.strokes.count { it.strokeType == StrokeType.CONNECTOR && it.linkFromId == parentId }
        val parentWireStyle = _state.value.strokes
            .find { it.strokeType == StrokeType.CONNECTOR && (it.linkToId == parentId || it.linkFromId == parentId) }
            ?.stickerKind ?: "orthogonal"
        val existingBoxes = _state.value.strokes.filter {
            (isMindMapNode(it) || it.isPlacedMedia()) && it.id != parentId
        }.map { it.bounds }
        val (childNode, connector) = MindmapEngine.createBranchInDirection(
            parent,
            direction,
            branchIndex,
            existingBounds = existingBoxes,
            wireStyle = parentWireStyle
        )
        _state.update { s ->
            s.copy(
                strokes = s.strokes + listOf(childNode, connector),
                selectedStrokeIds = setOf(childNode.id),
                currentTool = WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setNodeFillColor(nodeId: String, fillColor: Int) {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map {
                    if (it.id == nodeId) it.copy(fillColor = fillColor) else it
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setNodeOutlineColor(nodeId: String, outlineColor: Int) {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map {
                    if (it.id == nodeId) it.copy(colorValue = outlineColor) else it
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setNodeOutlineWidth(nodeId: String, width: Float) {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map {
                    if (it.id == nodeId) it.copy(strokeWidth = width) else it
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setNodeShape(nodeId: String, shapeType: StrokeType) {
        saveToUndoStack()
        _state.update { s ->
            val updatedNodes = s.strokes.map {
                if (it.id == nodeId) it.copy(strokeType = shapeType) else it
            }
            val strokeMap = updatedNodes.associateBy { it.id }
            val fullyUpdated = updatedNodes.map { stroke ->
                if (stroke.strokeType == StrokeType.CONNECTOR && (stroke.linkFromId == nodeId || stroke.linkToId == nodeId)) {
                    val from = strokeMap[stroke.linkFromId]
                    val to = strokeMap[stroke.linkToId]
                    if (from != null && to != null) {
                        val (p1, p2) = if (from.stickerKind.startsWith("flow:") || to.stickerKind.startsWith("flow:") || stroke.stickerKind.isEmpty()) {
                            from.bounds.center to to.bounds.center
                        } else {
                            MindmapEngine.calculateAnchors(from.bounds, to.bounds)
                        }
                        stroke.copy(points = listOf(p1, p2))
                    } else stroke
                } else stroke
            }
            s.copy(
                strokes = fullyUpdated,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setNodeText(nodeId: String, text: String) {
        updateStrokeText(nodeId, text)
    }

    fun connectMindmapBoxes(boxAId: String, boxBId: String) {
        if (boxAId == boxBId) return
        val strokes = _state.value.strokes
        val boxA = strokes.find { it.id == boxAId } ?: return
        val boxB = strokes.find { it.id == boxBId } ?: return

        val existing = strokes.find {
            it.strokeType == StrokeType.CONNECTOR &&
                ((it.linkFromId == boxAId && it.linkToId == boxBId) || (it.linkFromId == boxBId && it.linkToId == boxAId))
        }
        if (existing != null) {
            _state.update { it.copy(selectedStrokeIds = setOf(existing.id)) }
            return
        }

        val obstacles = strokes
            .filter { it.id != boxAId && it.id != boxBId && (isMindMapNode(it) || it.isPlacedMedia()) }
            .map { it.bounds }
        val parallelConnectors = strokes.filter {
            it.strokeType == StrokeType.CONNECTOR &&
                ((it.linkFromId == boxAId && it.linkToId == boxBId) || (it.linkFromId == boxBId && it.linkToId == boxAId) ||
                 (it.linkFromId == boxAId || it.linkToId == boxAId || it.linkFromId == boxBId || it.linkToId == boxBId))
        }
        val laneIndex = parallelConnectors.size
        val laneOffset = if (laneIndex > 0) ((laneIndex + 1) / 2) * 16f * if (laneIndex % 2 == 1) 1f else -1f else 0f

        val routePoints = MindmapEngine.calculateCollisionFreeRoute(
            from = boxA.bounds,
            to = boxB.bounds,
            obstacles = obstacles,
            wireStyle = "orthogonal",
            laneOffset = laneOffset
        )
        val connector = Stroke(
            points = routePoints,
            colorValue = boxA.colorValue,
            strokeWidth = 3.5f,
            strokeType = StrokeType.CONNECTOR,
            linkFromId = boxAId,
            linkToId = boxBId,
            stickerKind = "orthogonal"
        )

        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes + connector,
                selectedStrokeIds = setOf(connector.id),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun toggleConnectorWireStyle(nodeOrConnectorId: String) {
        saveToUndoStack()
        _state.update { s ->
            val strokeMap = s.strokes.associateBy { it.id }
            s.copy(
                strokes = s.strokes.map { stroke ->
                    val matches = stroke.id == nodeOrConnectorId ||
                        (stroke.strokeType == StrokeType.CONNECTOR && (stroke.linkFromId == nodeOrConnectorId || stroke.linkToId == nodeOrConnectorId))
                    if (matches && stroke.strokeType == StrokeType.CONNECTOR) {
                        val nextStyle = when (stroke.stickerKind) {
                            "orthogonal" -> "curved"
                            "curved" -> "straight"
                            else -> "orthogonal"
                        }
                        val from = strokeMap[stroke.linkFromId]
                        val to = strokeMap[stroke.linkToId]
                        val newPoints = if (from != null && to != null) {
                            val obstacles = strokeMap.values
                                .filter { it.id != from.id && it.id != to.id && (isMindMapNode(it) || it.isPlacedMedia()) }
                                .map { it.bounds }
                            MindmapEngine.calculateCollisionFreeRoute(
                                from = from.bounds,
                                to = to.bounds,
                                obstacles = obstacles,
                                wireStyle = nextStyle
                            )
                        } else stroke.points
                        stroke.copy(stickerKind = nextStyle, points = newPoints)
                    } else stroke
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun toggleSingleConnectorStyle(connectorId: String) {
        saveToUndoStack()
        _state.update { s ->
            val strokeMap = s.strokes.associateBy { it.id }
            s.copy(
                strokes = s.strokes.map {
                    if (it.id == connectorId && it.strokeType == StrokeType.CONNECTOR) {
                        val nextStyle = when (it.stickerKind) {
                            "orthogonal" -> "curved"
                            "curved" -> "straight"
                            else -> "orthogonal"
                        }
                        val from = strokeMap[it.linkFromId]
                        val to = strokeMap[it.linkToId]
                        val newPoints = if (from != null && to != null) {
                            val obstacles = strokeMap.values
                                .filter { obs -> obs.id != from.id && obs.id != to.id && (isMindMapNode(obs) || obs.isPlacedMedia()) }
                                .map { obs -> obs.bounds }
                            MindmapEngine.calculateCollisionFreeRoute(
                                from = from.bounds,
                                to = to.bounds,
                                obstacles = obstacles,
                                wireStyle = nextStyle
                            )
                        } else it.points
                        it.copy(stickerKind = nextStyle, points = newPoints)
                    } else it
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setConnectorColor(connectorId: String, color: Int) {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map {
                    if (it.id == connectorId && it.strokeType == StrokeType.CONNECTOR) {
                        it.copy(colorValue = color)
                    } else it
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun setConnectorWidth(connectorId: String, width: Float) {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.map {
                    if (it.id == connectorId && it.strokeType == StrokeType.CONNECTOR) {
                        it.copy(strokeWidth = width)
                    } else it
                },
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun deleteSingleConnector(connectorId: String) {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.filterNot { it.id == connectorId },
                selectedStrokeIds = emptySet(),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun reorderNodeLayer(nodeId: String, action: String) {
        val strokes = _state.value.strokes
        val index = strokes.indexOfFirst { it.id == nodeId }
        if (index == -1) return
        val node = strokes[index]
        val list = strokes.toMutableList()
        saveToUndoStack()
        when (action) {
            "top" -> {
                list.removeAt(index)
                list.add(node)
            }
            "bottom" -> {
                list.removeAt(index)
                list.add(0, node)
            }
            "up" -> {
                if (index < list.size - 1) {
                    list.removeAt(index)
                    list.add(index + 1, node)
                }
            }
            "down" -> {
                if (index > 0) {
                    list.removeAt(index)
                    list.add(index - 1, node)
                }
            }
        }
        _state.update { it.copy(strokes = list, canUndo = true, canRedo = false) }
        redoStack.clear()
        scheduleSave()
    }

    var activeWritingNodeId: String? = null
        private set

    fun startNodeWriting(nodeId: String) {
        activeWritingNodeId = nodeId
        _state.update { it.copy(currentTool = WhiteboardTool.PEN) }
    }

    fun stopNodeWriting() {
        activeWritingNodeId = null
    }

    fun findContainerNodeForStroke(stroke: Stroke): Stroke? {
        val b = stroke.bounds
        if (b.width <= 0f || b.height <= 0f) return null
        return _state.value.strokes.lastOrNull { node ->
            isMindMapNode(node) && node.bounds.contains(b.center)
        }
    }

    fun addMindMapCenter(world: Offset) {
        createMindMapTemplate(MindmapTemplateKind.BLANK, world)
    }

    fun addMindMapBranch(parentId: String, world: Offset) {
        addMindMapBranchDirection(parentId, "right")
    }

    fun moveLinkedConnectors(movedIds: Set<String>) {
        if (movedIds.isEmpty()) return
        _state.update { s ->
            s.copy(strokes = s.strokes.map { stroke ->
                if (stroke.strokeType != StrokeType.CONNECTOR) stroke
                else {
                    val from = s.strokes.find { it.id == stroke.linkFromId }
                    val to = s.strokes.find { it.id == stroke.linkToId }
                    if (from != null && to != null && (from.id in movedIds || to.id in movedIds)) {
                        val (p1, p2) = if (isMindMapNode(from) || isMindMapNode(to) || stroke.stickerKind in listOf("orthogonal", "curved") || from.stickerKind.startsWith("flow:") || to.stickerKind.startsWith("flow:")) {
                            MindmapEngine.calculateAnchors(from.bounds, to.bounds)
                        } else {
                            from.bounds.center to to.bounds.center
                        }
                        stroke.copy(points = listOf(p1, p2))
                    } else stroke
                }
            })
        }
    }

    fun selectPage(index: Int) {
        if (index !in pages.indices || index == activePage) return
        commitActivePage()
        switchTo(index)
        if (_state.value.document != null) renderDocumentPage(index)
        scheduleSave()
    }

    fun openDocument(uri: Uri, viewWidth: Float, viewHeight: Float) {
        viewportWidth = viewWidth
        viewportHeight = viewHeight
        viewModelScope.launch {
            try {
                val opened = withContext(Dispatchers.IO) { copyAndInspect(uri) }
                commitActivePage()
                if (_state.value.document == null) {
                    plainPages = pages.map { it.toList() }
                }
                pdfOcrJob?.cancel()
                clearRenderedPages()
                pages.clear()
                repeat(opened.pageCount) { pages.add(emptyList()) }
                activePage = 0
                _state.update {
                    it.copy(
                        document = BoardDocument(
                            name = opened.name,
                            kind = opened.kind,
                            path = opened.file.absolutePath,
                            pageWidth = 1f,
                            pageHeight = 1f
                        ),
                        strokes = emptyList(),
                        pageIndex = 0,
                        pageCount = pages.size,
                        pagePreviews = pages.toList(),
                        selectedStrokeIds = emptySet(),
                        isPdfOcrActive = false,
                        pdfOcrCurrentPage = 0,
                        pdfOcrTotalPages = 0,
                        pdfOcrDocument = null,
                        showPdfSearchPanel = false,
                        pdfSearchQuery = "",
                        errorMessage = null
                    )
                }
                renderDocumentPage(0)
                scheduleSave()
            } catch (error: Exception) {
                val message = error.message ?: "Could not open that file"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun closeDocument() {
        pdfOcrJob?.cancel()
        renderJob?.cancel()
        clearRenderedPages()
        _pageBitmap.value = null
        val restored = plainPages
        if (restored != null) {
            pages.clear()
            pages.addAll(restored.map { it.toList() })
            if (pages.isEmpty()) pages.add(emptyList())
            activePage = 0
        }
        plainPages = null
        _state.update {
            it.copy(
                document = null,
                strokes = pages.getOrElse(activePage) { emptyList() },
                pageIndex = activePage,
                pageCount = pages.size,
                pagePreviews = pages.toList(),
                selectedStrokeIds = emptySet(),
                isPdfOcrActive = false,
                pdfOcrCurrentPage = 0,
                pdfOcrTotalPages = 0,
                pdfOcrDocument = null,
                showPdfSearchPanel = false,
                pdfSearchQuery = ""
            )
        }
        scheduleSave()
    }

    fun exportAnnotatedPdf(onExported: (Uri) -> Unit) {
        val document = _state.value.document ?: return
        commitActivePage()
        val layers = pages.map { it.toList() }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdf = android.graphics.pdf.PdfDocument()
                layers.forEachIndexed { index, strokes ->
                    val cached = renderedPages[index]
                    val bitmap = cached ?: DocumentRenderer.render(File(document.path), document.kind, index)
                    try {
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val page = pdf.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
                        val composeCanvas = androidx.compose.ui.graphics.Canvas(page.canvas)
                        scope.draw(
                            androidx.compose.ui.unit.Density(1f),
                            androidx.compose.ui.unit.LayoutDirection.Ltr,
                            composeCanvas,
                            androidx.compose.ui.geometry.Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                        ) {
                            strokes.forEach { stroke -> renderStroke(context, stroke, 1f, false) }
                        }
                        pdf.finishPage(page)
                    } finally {
                        if (cached == null && bitmap !== _pageBitmap.value && !bitmap.isRecycled) bitmap.recycle()
                    }
                }
                val filename = document.name.substringBeforeLast('.') + "_annotated.pdf"
                val uri = writePdf(pdf, filename)
                pdf.close()
                if (uri != null) withContext(Dispatchers.Main) { onExported(uri) }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, error.message ?: "Could not export the PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private data class InspectedDocument(val file: File, val name: String, val kind: DocumentKind, val pageCount: Int)

    private fun copyAndInspect(uri: Uri): InspectedDocument {
        val name = documentDisplayName(uri)
        val mime = context.contentResolver.getType(uri)
        val kind = documentKind(name, mime) ?: throw IllegalArgumentException("Open a PDF or PowerPoint file")
        val dir = File(context.filesDir, "documents").apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dest = File(dir, "${_state.value.noteId}_$safe")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: throw IllegalArgumentException("Could not read that file")
        if (kind == DocumentKind.PPT && !isZip(dest)) {
            dest.delete()
            throw IllegalArgumentException("Save this PowerPoint as .pptx, then open it. The original file was not changed.")
        }
        val count = DocumentRenderer.pageCount(dest, kind)
        if (count <= 0) throw IllegalArgumentException("That file has no pages")
        return InspectedDocument(dest, name, kind, count)
    }

    private fun documentDisplayName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index) ?: "document"
            }
        }
        return uri.lastPathSegment ?: "document"
    }

    private fun isZip(file: File): Boolean {
        file.inputStream().use { input ->
            val header = ByteArray(2)
            return input.read(header) == 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
        }
    }

    fun updateViewport(width: Float, height: Float) {
        val first = viewportWidth <= 1f
        viewportWidth = width
        viewportHeight = height
        val document = _state.value.document ?: return
        if (first && document.pageWidth > 1f) {
            val fitted = fitDocument(document.pageWidth, document.pageHeight)
            _state.update { it.copy(scale = fitted.first, offsetX = fitted.second, offsetY = fitted.third) }
        }
    }

    private fun renderDocumentPage(index: Int) {
        val document = _state.value.document ?: return
        renderJob?.cancel()
        _pageBitmap.value = renderedPages[index]
        renderJob = viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                renderedPages[index] ?: DocumentRenderer.render(File(document.path), document.kind, index).also { rendered ->
                    renderedPages[index] = rendered
                    val stale = renderedPages.keys.filter { kotlin.math.abs(it - index) > 2 }.toList()
                    stale.forEach { key ->
                        val removed = renderedPages.remove(key)
                        if (removed != null && removed !== _pageBitmap.value && !removed.isRecycled) removed.recycle()
                    }
                }
            }
            if (!isActive || _state.value.pageIndex != index) return@launch
            _pageBitmap.value = bitmap
            val fitted = fitDocument(bitmap.width.toFloat(), bitmap.height.toFloat())
            _state.update {
                it.copy(
                    document = it.document?.copy(pageWidth = bitmap.width.toFloat(), pageHeight = bitmap.height.toFloat()),
                    scale = fitted.first,
                    offsetX = fitted.second,
                    offsetY = fitted.third
                )
            }
            val next = index + 1
            if (next < pages.size && renderedPages[next] == null) {
                launch(Dispatchers.IO) {
                    try {
                        renderedPages[next] = DocumentRenderer.render(File(document.path), document.kind, next)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun fitDocument(pageWidth: Float, pageHeight: Float, vw: Float = viewportWidth, vh: Float = viewportHeight): Triple<Float, Float, Float> {
        val effectiveW = if (vw > 1f) vw else viewportWidth
        val effectiveH = if (vh > 1f) vh else viewportHeight
        if (effectiveW <= 1f || effectiveH <= 1f || pageWidth <= 1f || pageHeight <= 1f) {
            return Triple(_state.value.scale, _state.value.offsetX, _state.value.offsetY)
        }
        val scale = kotlin.math.min(effectiveW / pageWidth, effectiveH / pageHeight)
            .coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)
        val offsetX = (effectiveW - pageWidth * scale) / 2f
        val offsetY = (effectiveH - pageHeight * scale) / 2f
        return Triple(scale, offsetX, offsetY)
    }

    private fun writePdf(pdf: android.graphics.pdf.PdfDocument, filename: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AvitriSphare")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            return uri
        }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, filename)
        file.outputStream().use { pdf.writeTo(it) }
        return Uri.fromFile(file)
    }

    private fun clearRenderedPages() {
        renderedPages.values.forEach { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        renderedPages.clear()
        _pageBitmap.value = null
    }

    fun toggleGrid() {
        _state.update { it.copy(showGrid = !it.showGrid) }
        scheduleSave()
    }

    fun toggleGraphPaper() {
        setCanvasBackground(_state.value.canvasBackground.withGraphPaperToggled())
    }

    fun toggleAppTheme() {
        _state.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun setAppTheme(isDark: Boolean) {
        _state.update { it.copy(isDarkTheme = isDark) }
    }

    fun toggleCanvasTheme() {
        val dark = !_state.value.isDarkCanvas
        val background = if (dark) CanvasBackground.DARK else CanvasBackground.DOTTED
        applyThemeChange(toDark = dark, background = background)
    }

    fun beginSelectionMove() {
        if (selectionUndoCaptured) return
        saveToUndoStack()
        redoActionStack.clear()
        redoStack.clear()
        selectionUndoCaptured = true
        _state.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun endSelectionMove() {
        selectionUndoCaptured = false
    }

    private fun switchTo(index: Int, clearPageUndo: Boolean = true, resetTransform: Boolean = false) {
        activePage = index
        if (clearPageUndo) {
            undoActionStack.removeAll { it is CanvasUndoEntry.StrokeState }
            redoActionStack.removeAll { it is CanvasUndoEntry.StrokeState }
            undoStack.clear()
            redoStack.clear()
        }
        selectionUndoCaptured = false
        syncPageMetadataLists()
        val bgKey = pageBackgroundKeys.getOrElse(index) {
            if (_state.value.isDarkCanvas) CanvasBackground.DARK.key else CanvasBackground.DOTTED.key
        }
        val bg = CanvasBackground.fromKey(bgKey)
        val isDark = (bg == CanvasBackground.DARK)
        while (pageMathTools.size < pages.size) pageMathTools.add(emptyList())
        val currentTools = pageMathTools.getOrElse(index) { emptyList() }
        val curTool = _state.value.currentTool
        val resetTool = if (curTool in setOf(
                WhiteboardTool.RULER,
                WhiteboardTool.COMPASS,
                WhiteboardTool.PROTRACTOR,
                WhiteboardTool.SET_SQUARE,
                WhiteboardTool.TRIANGLE
            )
        ) {
            val matchingKind = when (curTool) {
                WhiteboardTool.RULER -> MathToolKind.RULER
                WhiteboardTool.COMPASS -> MathToolKind.COMPASS
                WhiteboardTool.PROTRACTOR -> MathToolKind.PROTRACTOR
                WhiteboardTool.SET_SQUARE -> MathToolKind.SET_SQUARE
                WhiteboardTool.TRIANGLE -> MathToolKind.TRIANGLE
                else -> null
            }
            if (currentTools.any { it.kind == matchingKind }) curTool else _state.value.inkTool
        } else curTool
        val shouldReset = resetTransform || pages.getOrNull(index)?.isEmpty() == true
        _state.update {
            it.copy(
                strokes = pages[index],
                mathTools = currentTools,
                currentStroke = null,
                selectedStrokeIds = emptySet(),
                laserPoints = emptyList(),
                currentTool = resetTool,
                pageIndex = index,
                pageCount = pages.size,
                pagePreviews = pages.toList(),
                pageTitles = pageTitles.toList(),
                pageBackgroundKeys = pageBackgroundKeys.toList(),
                pageCustomBackgroundUris = pageCustomBackgroundUris.toList(),
                canvasBackground = bg,
                isDarkCanvas = isDark,
                pageEpoch = it.pageEpoch + 1,
                canUndo = undoActionStack.isNotEmpty(),
                canRedo = redoActionStack.isNotEmpty(),
                scale = if (shouldReset) 1f else it.scale,
                offsetX = if (shouldReset) 0f else it.offsetX,
                offsetY = if (shouldReset) 0f else it.offsetY
            )
        }
    }

    private fun syncPageMetadataLists() {
        val defaultBgKey = if (_state.value.isDarkCanvas) CanvasBackground.DARK.key else CanvasBackground.DOTTED.key
        while (pageTitles.size < pages.size) {
            pageTitles.add("Page ${pageTitles.size + 1}")
        }
        while (pageBackgroundKeys.size < pages.size) {
            pageBackgroundKeys.add(defaultBgKey)
        }
        while (pageCustomBackgroundUris.size < pages.size) {
            pageCustomBackgroundUris.add("")
        }
        while (pageMathTools.size < pages.size) {
            pageMathTools.add(emptyList())
        }
        while (pageTitles.size > pages.size) pageTitles.removeAt(pageTitles.lastIndex)
        while (pageBackgroundKeys.size > pages.size) pageBackgroundKeys.removeAt(pageBackgroundKeys.lastIndex)
        while (pageCustomBackgroundUris.size > pages.size) pageCustomBackgroundUris.removeAt(pageCustomBackgroundUris.lastIndex)
        while (pageMathTools.size > pages.size) pageMathTools.removeAt(pageMathTools.lastIndex)
        renumberDefaultPageTitles()
    }

    private fun commitActivePage() {
        if (pages.isEmpty()) pages.add(emptyList())
        if (activePage !in pages.indices) activePage = 0
        pages[activePage] = _state.value.strokes
        while (pageMathTools.size < pages.size) pageMathTools.add(emptyList())
        if (activePage in pageMathTools.indices) {
            pageMathTools[activePage] = _state.value.mathTools
        }
    }

    fun setMathTools(tools: List<PlacedMathTool>) {
        while (pageMathTools.size <= activePage) pageMathTools.add(emptyList())
        if (activePage in pageMathTools.indices) {
            pageMathTools[activePage] = tools
        }
        _state.update { it.copy(mathTools = tools) }
    }

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, noteId = noteId) }
            val loaded = withContext(Dispatchers.IO) { readBoard(noteId) }
            val note = withContext(Dispatchers.IO) { noteRepository.getNoteById(noteId) }
            pages.clear()
            pageTitles.clear()
            pageBackgroundKeys.clear()
            pageCustomBackgroundUris.clear()
            pageMathTools.clear()
            val restored = loaded.pages.map { page -> page.strokes.map { it.toDomain() } }
            if (restored.isEmpty()) pages.add(emptyList()) else pages.addAll(restored)
            loaded.pageTitles?.let { pageTitles.addAll(it) }
            loaded.pages.forEachIndexed { index, page ->
                if (index >= pageTitles.size) {
                    pageTitles.add(page.title ?: "Page ${index + 1}")
                }
                val defaultLegacyKey = if (index == 0 && note?.canvasBackground != null) note.canvasBackground.key else CanvasBackground.DOTTED.key
                pageBackgroundKeys.add(page.backgroundKey ?: defaultLegacyKey)
                pageCustomBackgroundUris.add(page.customBackgroundUri.orEmpty())
            }
            syncPageMetadataLists()
            activePage = loaded.activePage.coerceIn(0, pages.lastIndex)
            val ocrDoc = loaded.pdfOcrJson?.let { runCatching { gson.fromJson(it, PdfOcrDocument::class.java) }.getOrNull() }
            undoStack.clear()
            redoStack.clear()
            val activeBgKey = pageBackgroundKeys.getOrNull(activePage)
            val background = if (activeBgKey != null) {
                CanvasBackground.fromKey(activeBgKey)
            } else if (note?.canvasBackground != null) {
                note.canvasBackground
            } else if (loaded.fromFile && loaded.darkMode) {
                CanvasBackground.DARK
            } else {
                CanvasBackground.DOTTED
            }
            val dark = background == CanvasBackground.DARK
            _state.update {
                it.copy(
                    noteTitle = note?.title ?: "Untitled",
                    canvasBackground = background,
                    isDarkCanvas = dark,
                    showGrid = loaded.showGrid,
                    contentJson = note?.contentJson ?: "",
                    strokes = pages[activePage],
                    pageIndex = activePage,
                    pageCount = pages.size,
                    pagePreviews = pages.toList(),
                    scale = loaded.scale.coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE),
                    offsetX = loaded.offsetX,
                    offsetY = loaded.offsetY,
                    isLoading = false,
                    canUndo = false,
                    canRedo = false,
                    selectedStrokeIds = emptySet(),
                    document = null,
                    errorMessage = null,
                    pageTitles = pageTitles.toList(),
                    pageBackgroundKeys = pageBackgroundKeys.toList(),
                    pageCustomBackgroundUris = pageCustomBackgroundUris.toList(),
                    inkSampleGapMm = loaded.inkSampleGapMm ?: DEFAULT_INK_SAMPLE_GAP_MM,
                    secondaryColor = loaded.secondaryColor ?: 0xFF3B82F6.toInt(),
                    secondaryStrokeWidth = loaded.secondaryStrokeWidth ?: 12f,
                    pdfOcrDocument = ocrDoc,
                    documentFrameX = loaded.documentFrameX ?: 0f,
                    documentFrameY = loaded.documentFrameY ?: 0f,
                    documentFrameScale = loaded.documentFrameScale ?: 1f,
                    palmEraserEnabled = loaded.palmEraserEnabled ?: false,
                    restrictEraserToSelection = loaded.restrictEraserToSelection ?: false
                )
            }
            plainPages = loaded.plainPages?.map { page -> page.strokes.map { it.toDomain() } }
            val path = loaded.documentPath
            if (!path.isNullOrBlank() && File(path).exists()) {
                val kind = if (loaded.documentKind == DocumentKind.PPT.name) DocumentKind.PPT else DocumentKind.PDF
                _state.update {
                    it.copy(
                        document = BoardDocument(
                            name = loaded.documentName ?: File(path).name,
                            kind = kind,
                            path = path,
                            pageWidth = 1f,
                            pageHeight = 1f
                        )
                    )
                }
                renderDocumentPage(activePage)
            }
            restoreSplit(loaded)
            ensureAutoSave()
        }
    }

    private fun restoreSplit(loaded: BoardSnapshot) {
        val count = loaded.splitCount ?: return
        val saved = loaded.splitPanes ?: return
        if (count !in setOf(2, 3, 4, 6) || saved.isEmpty()) return
        val restored = saved.mapIndexed { index, pane ->
            val kind = runCatching { PaneKind.valueOf(pane.kind ?: "BOARD") }.getOrDefault(PaneKind.BOARD)
            val tool = runCatching { WhiteboardTool.valueOf(pane.tool ?: "PEN") }.getOrDefault(WhiteboardTool.PEN)
            CanvasPane(
                id = index,
                kind = kind,
                strokes = pane.strokes.orEmpty().map { it.toDomain() },
                source = pane.source.orEmpty(),
                label = pane.label.orEmpty(),
                pageIndex = pane.pageIndex ?: 0,
                pageCount = pane.pageCount ?: 0,
                tool = tool,
                color = pane.color ?: 0xFF1B1F2A.toInt(),
                strokeWidth = pane.strokeWidth ?: 4f
            )
        }
        val active = (loaded.splitActive ?: 0).coerceIn(0, restored.lastIndex)
        _state.update {
            it.copy(
                splitCount = count,
                splitWeights = loaded.splitWeights ?: defaultWeights(count),
                splitPanes = restored,
                splitActive = active,
                currentTool = restored[active].tool,
                selectedColor = restored[active].color,
                strokeWidth = restored[active].strokeWidth,
                isEraser = restored[active].tool == WhiteboardTool.ERASER,
                isHighlighter = restored[active].tool == WhiteboardTool.HIGHLIGHTER
            )
        }
    }

    fun startStroke(stroke: Stroke) {
        smartJob?.cancel()
        _state.update { it.copy(currentStroke = stroke, smartWriteOffer = null, smartWriteBusy = false) }
    }

    fun updateStroke(points: List<Offset>, pressures: List<Float> = emptyList()) {
        _state.update { current ->
            current.copy(
                currentStroke = current.currentStroke?.copy(
                    points = points,
                    pointPressures = pressures
                )
            )
        }
    }

    fun finishStroke() {
        val currentStroke = _state.value.currentStroke ?: return
        commitStroke(currentStroke)
    }

    fun cancelCurrentStroke() {
        _state.update { it.copy(currentStroke = null) }
    }

    fun commitStroke(stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        saveToUndoStack()
        val finalStroke = if (stroke.linkToId.isEmpty() && stroke.strokeType in HANDWRITING_TYPES) {
            val container = if (activeWritingNodeId != null) {
                _state.value.strokes.find { it.id == activeWritingNodeId } ?: findContainerNodeForStroke(stroke)
            } else {
                findContainerNodeForStroke(stroke)
            }
            if (container != null) stroke.copy(linkToId = container.id) else stroke
        } else stroke
        _state.update { s ->
            s.copy(
                strokes = s.strokes + finalStroke,
                currentStroke = null,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
        if (finalStroke.strokeType in HANDWRITING_TYPES && _state.value.smartWriteEnabled && _state.value.inkTool != WhiteboardTool.MATH_PEN) {
            scheduleSmartWrite()
        }
        if (finalStroke.strokeType in HANDWRITING_TYPES && _state.value.inkTool == WhiteboardTool.MATH_PEN) {
            mathPenSessionStrokeIds.add(finalStroke.id)
            scheduleMathPen()
        }
    }

    private var mathPenJob: Job? = null
    private val mathPenSessionStrokeIds = mutableListOf<String>()

    private fun scheduleMathPen() {
        if (_state.value.inkTool != WhiteboardTool.MATH_PEN) return
        mathPenJob?.cancel()
        mathPenJob = viewModelScope.launch {
            delay(2200L)
            if (!isActive) return@launch
            if (_state.value.inkTool != WhiteboardTool.MATH_PEN) return@launch

            val currentStrokes = _state.value.strokes
            val sessionStrokes = currentStrokes.filter {
                it.id in mathPenSessionStrokeIds && it.strokeType in HANDWRITING_TYPES
            }
            if (sessionStrokes.isEmpty()) {
                mathPenSessionStrokeIds.clear()
                return@launch
            }

            val cluster = clusterMathStrokes(sessionStrokes)
            if (cluster.isEmpty()) return@launch

            val totalPoints = cluster.sumOf { it.points.size }
            val bounds = unionStrokeBounds(cluster)
            if (totalPoints < 3 || (bounds.width < 25f && bounds.height < 25f)) {
                return@launch
            }

            _state.update { it.copy(equationBusy = true) }
            val latex = withTimeoutOrNull(3_500L) {
                withContext(Dispatchers.Default) { readEquation(cluster) }
            }
            if (!isActive) return@launch
            _state.update { it.copy(equationBusy = false) }

            val clean = latex?.trim().orEmpty()
            val isJunk = clean.isEmpty() || (cluster.size <= 2 && clean in setOf("/", "\\", "|", "_", "-", "c", "C", "l", "I"))
            if (!isJunk) {
                val clusterIds = cluster.map { it.id }.toSet()
                mathPenSessionStrokeIds.removeAll(clusterIds)
                placeEquation(cluster, clean, showDialog = false)
            }
        }
    }

    fun setEraserMode(mode: EraserMode) {
        _state.update {
            it.copy(
                eraserMode = mode,
                currentTool = WhiteboardTool.ERASER,
                isEraser = true
            )
        }
    }

    fun setEraserSize(size: Float) {
        _state.update {
            it.copy(
                eraserSize = size.coerceIn(8f, 80f),
                currentTool = WhiteboardTool.ERASER,
                isEraser = true
            )
        }
    }

    fun toggleEraser() {
        _state.update { it.copy(isEraser = !it.isEraser, isHighlighter = false, isShapeMode = false) }
    }

    fun toggleHighlighter() {
        _state.update { it.copy(isHighlighter = !it.isHighlighter, isEraser = false, isShapeMode = false) }
    }

    fun toggleShapeMode() {
        _state.update { it.copy(isShapeMode = !it.isShapeMode, isEraser = false, isHighlighter = false) }
    }

    fun setColor(color: Int) {
        _state.update { s ->
            val panes = if (s.splitCount > 1) {
                s.splitPanes.mapIndexed { index, pane ->
                    if (index == s.splitActive) pane.copy(color = color) else pane
                }
            } else {
                s.splitPanes
            }
            s.copy(selectedColor = color, isEraser = false, splitPanes = panes)
        }
        if (_state.value.splitCount > 1) {
            scheduleSave()
        }
    }

    fun setStrokeWidth(width: Float) {
        val clamped = width.coerceIn(1f, 50f)
        val tool = _state.value.inkTool
        if (tool in WRITING_TOOLS) toolWidths[tool] = clamped
        if (_state.value.splitCount > 1) {
            _state.update { s ->
                val panes = s.splitPanes.mapIndexed { index, pane ->
                    if (index == s.splitActive) pane.copy(strokeWidth = clamped) else pane
                }
                s.copy(strokeWidth = clamped, splitPanes = panes)
            }
            scheduleSave()
            return
        }
        val selected = _state.value.selectedStrokeIds
        if (selected.isNotEmpty()) {
            saveToUndoStack()
            _state.update { s ->
                s.copy(
                    strokeWidth = clamped,
                    strokes = s.strokes.map { stroke ->
                        if (stroke.id in selected) stroke.copy(strokeWidth = clamped) else stroke
                    },
                    canUndo = true,
                    canRedo = false
                )
            }
            redoStack.clear()
            scheduleSave()
            return
        }
        _state.update {
            it.copy(
                strokeWidth = clamped,
                currentTool = if (it.currentTool in WRITING_TOOLS || it.currentTool == tool) tool else it.currentTool,
                isHighlighter = tool == WhiteboardTool.HIGHLIGHTER && (it.currentTool in WRITING_TOOLS || it.currentTool == tool)
            )
        }
    }

    fun nudgeStrokeWidth(delta: Float) {
        setStrokeWidth(_state.value.strokeWidth + delta)
    }

    fun updateTransform(scale: Float, offsetX: Float, offsetY: Float) {
        _state.update { it.copy(scale = scale, offsetX = offsetX, offsetY = offsetY) }
    }

    fun resetTransform() {
        _state.update { it.copy(scale = 1f, offsetX = 0f, offsetY = 0f) }
    }

    fun undo() {
        if (_state.value.splitCount > 1) {
            val index = _state.value.splitActive.coerceIn(0, 5)
            val previous = paneUndo[index].removeLastOrNull() ?: return
            val current = _state.value.splitPanes.getOrNull(index)?.strokes ?: emptyList()
            paneRedo[index].addLast(current)
            setPaneStrokes(index, previous)
            return
        }
        if (undoActionStack.isEmpty()) return
        val currentStrokes = _state.value.strokes
        val action = undoActionStack.removeLastOrNull() ?: return
        when (action) {
            is CanvasUndoEntry.StrokeState -> {
                redoActionStack.add(CanvasUndoEntry.StrokeState(currentStrokes, _state.value.mathTools))
                if (redoActionStack.size > MAX_UNDO) redoActionStack.removeAt(0)
                textEditUndoCaptured = false
                _state.update { s ->
                    s.copy(
                        strokes = action.strokes,
                        mathTools = action.mathTools,
                        canUndo = undoActionStack.isNotEmpty(),
                        canRedo = true,
                        smartWriteOffer = null,
                        smartWriteWarning = null,
                        editingTextId = null
                    )
                }
                while (pageMathTools.size <= activePage) pageMathTools.add(emptyList())
                if (activePage in pageMathTools.indices) {
                    pageMathTools[activePage] = action.mathTools
                }
                pages[activePage] = action.strokes
                scheduleSave()
            }
            is CanvasUndoEntry.DeletedPage -> {
                commitActivePage()
                redoActionStack.add(
                    CanvasUndoEntry.DeletedPage(
                        index = action.index,
                        strokes = action.strokes,
                        title = action.title,
                        backgroundKey = action.backgroundKey,
                        customBackgroundUri = action.customBackgroundUri,
                        previousActivePage = activePage
                    )
                )
                if (redoActionStack.size > MAX_UNDO) redoActionStack.removeAt(0)
                val insertIndex = action.index.coerceIn(0, pages.size)
                pages.add(insertIndex, action.strokes)
                pageTitles.add(insertIndex, action.title)
                pageBackgroundKeys.add(insertIndex, action.backgroundKey)
                pageCustomBackgroundUris.add(insertIndex, action.customBackgroundUri)
                switchTo(insertIndex, clearPageUndo = false)
                _state.update { s ->
                    s.copy(
                        canUndo = undoActionStack.isNotEmpty(),
                        canRedo = true
                    )
                }
                scheduleSave()
            }
        }
        mathPenJob?.cancel()
        mathPenSessionStrokeIds.clear()
        scheduleSave()
    }

    fun redo() {
        if (_state.value.splitCount > 1) {
            val index = _state.value.splitActive.coerceIn(0, 5)
            val next = paneRedo[index].removeLastOrNull() ?: return
            val current = _state.value.splitPanes.getOrNull(index)?.strokes ?: emptyList()
            paneUndo[index].addLast(current)
            setPaneStrokes(index, next)
            return
        }
        if (redoActionStack.isEmpty()) return
        val currentStrokes = _state.value.strokes
        val action = redoActionStack.removeLastOrNull() ?: return
        when (action) {
            is CanvasUndoEntry.StrokeState -> {
                undoActionStack.add(CanvasUndoEntry.StrokeState(currentStrokes, _state.value.mathTools))
                if (undoActionStack.size > MAX_UNDO) undoActionStack.removeAt(0)
                textEditUndoCaptured = false
                _state.update { s ->
                    s.copy(
                        strokes = action.strokes,
                        mathTools = action.mathTools,
                        canUndo = true,
                        canRedo = redoActionStack.isNotEmpty(),
                        smartWriteOffer = null,
                        smartWriteWarning = null,
                        editingTextId = null
                    )
                }
                while (pageMathTools.size <= activePage) pageMathTools.add(emptyList())
                if (activePage in pageMathTools.indices) {
                    pageMathTools[activePage] = action.mathTools
                }
                pages[activePage] = action.strokes
                scheduleSave()
            }
            is CanvasUndoEntry.DeletedPage -> {
                commitActivePage()
                undoActionStack.add(
                    CanvasUndoEntry.DeletedPage(
                        index = action.index,
                        strokes = action.strokes,
                        title = action.title,
                        backgroundKey = action.backgroundKey,
                        customBackgroundUri = action.customBackgroundUri,
                        previousActivePage = activePage
                    )
                )
                if (undoActionStack.size > MAX_UNDO) undoActionStack.removeAt(0)
                if (action.index in pages.indices) {
                    pages.removeAt(action.index)
                    pageTitles.removeAt(action.index)
                    pageBackgroundKeys.removeAt(action.index)
                    pageCustomBackgroundUris.removeAt(action.index)
                    activePage = activePage.coerceIn(0, pages.lastIndex)
                    switchTo(activePage, clearPageUndo = false)
                }
                _state.update { s ->
                    s.copy(
                        canUndo = true,
                        canRedo = redoActionStack.isNotEmpty()
                    )
                }
                scheduleSave()
            }
        }
    }

    fun clearCanvas() {
        if (_state.value.splitCount > 1) {
            val index = _state.value.splitActive.coerceIn(0, 5)
            val pane = _state.value.splitPanes.getOrNull(index) ?: return
            if (pane.strokes.isEmpty()) return
            beginPaneEdit(index)
            setPaneStrokes(index, emptyList())
            return
        }
        if (_state.value.strokes.isEmpty() && _state.value.mathTools.isEmpty()) return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = emptyList(),
                currentStroke = null,
                selectedStrokeIds = emptySet(),
                mathTools = emptyList(),
                canUndo = true,
                canRedo = false
            )
        }
        while (pageMathTools.size <= activePage) pageMathTools.add(emptyList())
        if (activePage in pageMathTools.indices) {
            pageMathTools[activePage] = emptyList()
        }
        mathPenJob?.cancel()
        mathPenSessionStrokeIds.clear()
        redoStack.clear()
        scheduleSave()
    }

    /**
     * Cleans everything across the active canvas (drawings, pen strokes, shapes, geometry tools, images, etc.).
     */
    fun cleanCanvasExceptPen() {
        clearCanvas()
    }

    /**
     * Replaces the current page after an eraser gesture.
     * One undo step restores the page as it was before the gesture.
     */
    fun commitErasedStrokes(updated: List<Stroke>) {
        if (updated === _state.value.strokes) return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = updated,
                currentStroke = null,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(title = newTitle, updatedAt = System.currentTimeMillis())
                    noteRepository.updateNote(updatedNote)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(noteTitle = newTitle) }
                    }
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.deleteNote(note)
                    val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                    if (strokesFile.exists()) strokesFile.delete()
                    val boardFile = File(context.filesDir, "board_$noteId.json")
                    if (boardFile.exists()) boardFile.delete()
                    withContext(Dispatchers.Main) { onDeleted() }
                }
            }
        }
    }

    private fun saveToUndoStack() {
        undoActionStack.add(CanvasUndoEntry.StrokeState(_state.value.strokes, _state.value.mathTools))
        if (undoActionStack.size > MAX_UNDO) undoActionStack.removeAt(0)
        undoStack.add(_state.value.strokes)
        if (undoStack.size > MAX_UNDO) undoStack.removeAt(0)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            persistSnapshot()
        }
    }

    private fun ensureAutoSave() {
        if (autoSaveJob?.isActive == true) return
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                persistSnapshot()
            }
        }
    }

    suspend fun saveNow() {
        saveJob?.cancel()
        persistSnapshot()
    }

    private suspend fun persistSnapshot() {
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        commitActivePage()
        syncPageMetadataLists()
        val snapshot = BoardSnapshot(
            activePage = activePage,
            showGrid = _state.value.showGrid,
            darkMode = _state.value.isDarkCanvas,
            scale = _state.value.scale,
            offsetX = _state.value.offsetX,
            offsetY = _state.value.offsetY,
            pages = pages.mapIndexed { index, strokes ->
                PageSnapshot(
                    strokes = strokes.map { it.toDto() },
                    title = pageTitles.getOrNull(index),
                    backgroundKey = pageBackgroundKeys.getOrNull(index),
                    customBackgroundUri = pageCustomBackgroundUris.getOrNull(index)
                )
            },
            documentPath = _state.value.document?.path,
            documentName = _state.value.document?.name,
            documentKind = _state.value.document?.kind?.name,
            plainPages = plainPages?.map { strokes -> PageSnapshot(strokes.map { it.toDto() }) },
            splitCount = _state.value.splitCount.takeIf { it > 1 },
            splitWeights = _state.value.splitWeights.takeIf { _state.value.splitCount > 1 },
            splitPanes = if (_state.value.splitCount > 1) _state.value.splitPanes.map { it.toSnapshot() } else null,
            splitActive = _state.value.splitActive.takeIf { _state.value.splitCount > 1 },
            pageTitles = pageTitles.toList(),
            inkSampleGapMm = _state.value.inkSampleGapMm,
            secondaryColor = _state.value.secondaryColor,
            secondaryStrokeWidth = _state.value.secondaryStrokeWidth,
            pdfOcrJson = _state.value.pdfOcrDocument?.let { gson.toJson(it) },
            documentFrameX = _state.value.documentFrameX,
            documentFrameY = _state.value.documentFrameY,
            documentFrameScale = _state.value.documentFrameScale,
            palmEraserEnabled = _state.value.palmEraserEnabled,
            restrictEraserToSelection = _state.value.restrictEraserToSelection
        )
        val boardJson = gson.toJson(snapshot)
        val activeJson = gson.toJson(snapshot.pages.getOrElse(activePage) { PageSnapshot() }.strokes)
        val timestamp = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            try {
                File(context.filesDir, "board_$noteId.json").writeText(boardJson)
                File(context.filesDir, "strokes_$noteId.json").writeText(activeJson)
                noteRepository.updateNoteTimestamp(noteId, timestamp)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
            }
        }
    }

    private fun readBoard(noteId: String): BoardSnapshot {
        val boardFile = File(context.filesDir, "board_$noteId.json")
        if (boardFile.exists() && boardFile.length() <= 20_000_000L) {
            try {
                val type = object : TypeToken<BoardSnapshot>() {}.type
                val parsed: BoardSnapshot? = gson.fromJson(boardFile.readText(), type)
                if (parsed != null && parsed.pages.isNotEmpty()) {
                    return parsed.copy(fromFile = true)
                }
            } catch (_: Exception) {
            }
        }
        val strokesFile = File(context.filesDir, "strokes_$noteId.json")
        if (strokesFile.exists() && strokesFile.length() <= 10_000_000L) {
            try {
                val type = object : TypeToken<List<StrokeDto>>() {}.type
                val dtos: List<StrokeDto> = gson.fromJson(strokesFile.readText(), type) ?: emptyList()
                return BoardSnapshot(pages = listOf(PageSnapshot(dtos.take(10000))))
            } catch (_: Exception) {
            }
        }
        return BoardSnapshot()
    }

    override fun onCleared() {
        val noteId = _state.value.noteId
        if (noteId.isNotEmpty()) {
            try {
                commitActivePage()
                syncPageMetadataLists()
                val snapshot = BoardSnapshot(
                    activePage = activePage,
                    showGrid = _state.value.showGrid,
                    darkMode = _state.value.isDarkCanvas,
                    scale = _state.value.scale,
                    offsetX = _state.value.offsetX,
                    offsetY = _state.value.offsetY,
                    pages = pages.mapIndexed { index, strokes ->
                        PageSnapshot(
                            strokes = strokes.map { it.toDto() },
                            title = pageTitles.getOrNull(index),
                            backgroundKey = pageBackgroundKeys.getOrNull(index),
                            customBackgroundUri = pageCustomBackgroundUris.getOrNull(index)
                        )
                    },
                    documentPath = _state.value.document?.path,
                    documentName = _state.value.document?.name,
                    documentKind = _state.value.document?.kind?.name,
                    plainPages = plainPages?.map { strokes -> PageSnapshot(strokes.map { it.toDto() }) },
                    splitCount = _state.value.splitCount.takeIf { it > 1 },
                    splitWeights = _state.value.splitWeights.takeIf { _state.value.splitCount > 1 },
                    splitPanes = if (_state.value.splitCount > 1) _state.value.splitPanes.map { it.toSnapshot() } else null,
                    splitActive = _state.value.splitActive.takeIf { _state.value.splitCount > 1 },
                    pageTitles = pageTitles.toList()
                )
                File(context.filesDir, "board_$noteId.json").writeText(gson.toJson(snapshot))
            } catch (_: Exception) {
            }
        }
        super.onCleared()
    }

    private var isErasingActive = false

    fun eraseAt(worldPoint: Offset, thresholdRadius: Float = 24f): Boolean {
        val currentStrokes = _state.value.strokes
        if (currentStrokes.isEmpty()) return false

        val remaining = currentStrokes.filterNot { stroke ->
            CanvasTransform.strokeIntersectsPoint(stroke, worldPoint, thresholdRadius)
        }

        if (remaining.size != currentStrokes.size) {
            val erasedIds = currentStrokes.map { it.id }.toSet() - remaining.map { it.id }.toSet()
            val cleaned = remaining.filterNot { stroke ->
                stroke.strokeType == StrokeType.CONNECTOR && (stroke.linkFromId in erasedIds || stroke.linkToId in erasedIds)
            }
            if (!isErasingActive) {
                saveToUndoStack()
                isErasingActive = true
            }
            _state.update { s ->
                s.copy(
                    strokes = cleaned,
                    canUndo = true,
                    canRedo = false
                )
            }
            redoStack.clear()
            scheduleSave()
            return true
        }
        return false
    }

    fun finishErasing() {
        isErasingActive = false
    }

    fun fitToContent(viewportWidth: Float, viewportHeight: Float) {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        val doc = _state.value.document
        if (doc != null) {
            resetDocumentFrame()
            val pw = if (doc.pageWidth > 1f) doc.pageWidth else (_pageBitmap.value?.width?.toFloat() ?: 1600f)
            val ph = if (doc.pageHeight > 1f) doc.pageHeight else (_pageBitmap.value?.height?.toFloat() ?: 2262f)
            val fitted = fitDocument(pw, ph, viewportWidth, viewportHeight)
            _state.update {
                it.copy(
                    scale = fitted.first,
                    offsetX = fitted.second,
                    offsetY = fitted.third
                )
            }
            return
        }
        val contentBounds = CanvasTransform.calculateContentBounds(_state.value.strokes)
        val (targetScale, targetX, targetY) = CanvasTransform.calculateFitTransform(
            contentBounds = contentBounds,
            viewportSize = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
            paddingPx = 80f
        )
        _state.update { it.copy(scale = targetScale, offsetX = targetX, offsetY = targetY) }
    }

    fun exportAsPng(context: Context, onExported: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val strokes = _state.value.strokes
            val contentBounds = CanvasTransform.calculateContentBounds(strokes)

            var bitmapWidth = 2048
            var bitmapHeight = 1536
            var scaleFactor = 1f
            var transX = 0f
            var transY = 0f

            if (contentBounds != null && contentBounds.width > 0 && contentBounds.height > 0) {
                val padding = 100f
                val paddedWidth = contentBounds.width + padding * 2
                val paddedHeight = contentBounds.height + padding * 2

                val maxDim = 3840f
                val fitScale = kotlin.math.min(1f, maxDim / kotlin.math.max(paddedWidth, paddedHeight))
                bitmapWidth = (paddedWidth * fitScale).toInt().coerceIn(800, 4096)
                bitmapHeight = (paddedHeight * fitScale).toInt().coerceIn(600, 4096)

                scaleFactor = bitmapWidth.toFloat() / paddedWidth
                transX = -(contentBounds.left - padding) * scaleFactor
                transY = -(contentBounds.top - padding) * scaleFactor
            }

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val bgArgb = when (_state.value.canvasBackground) {
                CanvasBackground.DARK -> 0xFF171717.toInt()
                CanvasBackground.GRAPH -> 0xFFF7FBFF.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            canvas.drawColor(bgArgb)

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }

            strokes.forEach { stroke ->
                val pointsList = stroke.points
                if (pointsList.size >= 2) {
                    paint.color = if (stroke.isHighlighter) {
                        val c = Color(stroke.colorValue).copy(alpha = 0.4f)
                        c.toArgb()
                    } else {
                        stroke.colorValue
                    }
                    paint.strokeWidth = stroke.strokeWidth * scaleFactor

                    val path = android.graphics.Path()
                    val p0x = pointsList[0].x * scaleFactor + transX
                    val p0y = pointsList[0].y * scaleFactor + transY
                    path.moveTo(p0x, p0y)
                    for (i in 1 until pointsList.size) {
                        val px = pointsList[i].x * scaleFactor + transX
                        val py = pointsList[i].y * scaleFactor + transY
                        path.lineTo(px, py)
                    }
                    canvas.drawPath(path, paint)
                }
            }

            val filename = "avitrisphere_${_state.value.noteTitle.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AvitriSphare")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                bitmap.recycle()
                withContext(Dispatchers.Main) { onExported(it) }
            } ?: bitmap.recycle()
        }
    }

    fun setCanvasBackground(background: CanvasBackground) {
        val toDark = background == CanvasBackground.DARK
        val wasDark = _state.value.isDarkCanvas
        if (toDark != wasDark) {
            applyThemeChange(toDark = toDark, background = background)
            return
        }

        syncPageMetadataLists()
        if (activePage in pageBackgroundKeys.indices) {
            pageBackgroundKeys[activePage] = background.key
        }

        _state.update {
            it.copy(
                canvasBackground = background,
                isDarkCanvas = toDark,
                pageBackgroundKeys = pageBackgroundKeys.toList()
            )
        }

        val noteId = _state.value.noteId
        if (noteId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val note = noteRepository.getNoteById(noteId)
                    if (note != null) {
                        val updatedNote = note.copy(
                            canvasBackground = background,
                            updatedAt = System.currentTimeMillis()
                        )
                        noteRepository.updateNote(updatedNote)
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(errorMessage = "Failed to update background: ${e.message}") }
                }
            }
        }
        scheduleSave()
    }

    private fun applyThemeChange(toDark: Boolean, background: CanvasBackground) {
        commitActivePage()
        saveToUndoStack()
        redoStack.clear()
        if (pages.isEmpty()) pages.add(emptyList())
        val remappedPages = pages.map { page -> CanvasInkTheme.remapStrokesForTheme(page, toDark) }
        pages.clear()
        pages.addAll(remappedPages)
        val activeStrokes = remappedPages.getOrElse(activePage) { emptyList() }

        syncPageMetadataLists()
        for (i in pageBackgroundKeys.indices) {
            val oldBg = CanvasBackground.fromKey(pageBackgroundKeys[i])
            if (toDark) {
                if (oldBg != CanvasBackground.DARK) {
                    pageBackgroundKeys[i] = CanvasBackground.DARK.key
                }
            } else {
                if (oldBg == CanvasBackground.DARK) {
                    pageBackgroundKeys[i] = CanvasBackground.DOTTED.key
                }
            }
        }

        _state.update { s ->
            s.copy(
                isDarkCanvas = toDark,
                canvasBackground = background,
                pageBackgroundKeys = pageBackgroundKeys.toList(),
                selectedColor = CanvasInkTheme.penColorForTheme(s.selectedColor, toDark),
                strokes = activeStrokes,
                pagePreviews = pages.toList(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
        val noteId = _state.value.noteId
        if (noteId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val note = noteRepository.getNoteById(noteId) ?: return@launch
                noteRepository.updateNote(
                    note.copy(canvasBackground = background, updatedAt = System.currentTimeMillis())
                )
            }
        }
        scheduleSave()
    }

    fun startLiveSession() {
        val roomId = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        _state.update {
            it.copy(
                isLiveCollabActive = true,
                collabRoomId = roomId,
                connectedCollaborators = listOf(it.userName + " (You)")
            )
        }
    }

    fun stopLiveSession() {
        _state.update {
            it.copy(
                isLiveCollabActive = false,
                collabRoomId = "",
                connectedCollaborators = emptyList()
            )
        }
    }

    fun setUserName(name: String) {
        _state.update { it.copy(userName = name.ifBlank { "Swift Penguin" }) }
    }

    fun exportNoteJson(onSaved: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val dtos = _state.value.strokes.map { it.toDto() }
            val json = gson.toJson(dtos)
            val file = File(context.filesDir, "export_${_state.value.noteTitle.replace(" ", "_")}.whiteboard")
            file.writeText(json)
            withContext(Dispatchers.Main) { onSaved(file.absolutePath) }
        }
    }

    fun getAllPages(): List<List<Stroke>> {
        commitActivePage()
        return pages.map { it.toList() }
    }

    fun exportLesson(
        context: Context,
        lessonTitle: String,
        format: ExportFormat,
        destination: ExportDestination,
        onProgress: (Float, String) -> Unit,
        onResult: (ExportResult) -> Unit
    ) {
        viewModelScope.launch {
            commitActivePage()
            val effectivePages = pages.map { it.toList() }
            val currentState = _state.value
            val rendered = renderedPages.toMap()
            try {
                val result = WhiteboardExportManager.exportWhiteboard(
                    context = context,
                    lessonTitle = lessonTitle,
                    pages = effectivePages,
                    canvasBackground = currentState.canvasBackground,
                    isDarkCanvas = currentState.isDarkCanvas,
                    showGrid = currentState.showGrid,
                    document = currentState.document,
                    renderedPages = rendered,
                    scale = currentState.scale,
                    offsetX = currentState.offsetX,
                    offsetY = currentState.offsetY,
                    activePage = activePage,
                    splitCount = currentState.splitCount,
                    splitWeights = currentState.splitWeights,
                    splitPanes = currentState.splitPanes,
                    format = format,
                    destination = destination,
                    onProgress = onProgress
                )
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onProgress(0f, "Export failed: ${e.message}")
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importIwbFile(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pkg = WhiteboardExportManager.importIwb(context, uri)
                val restoredPages = pkg.pages.map { page ->
                    page.layers.flatMap { layer -> layer.strokes.map { it.toDomain() } }
                }
                val background = CanvasBackground.fromKey(pkg.metadata.canvasBackground)

                withContext(Dispatchers.Main) {
                    commitActivePage()
                    pages.clear()
                    if (restoredPages.isEmpty()) {
                        pages.add(emptyList())
                    } else {
                        pages.addAll(restoredPages)
                    }
                    activePage = pkg.metadata.activePage.coerceIn(0, pages.lastIndex)
                    undoStack.clear()
                    redoStack.clear()

                    val effectiveTitle = pkg.title.ifBlank { "Imported Lesson" }
                    val currentNoteId = _state.value.noteId.ifEmpty {
                        val newId = java.util.UUID.randomUUID().toString()
                        _state.update { it.copy(noteId = newId) }
                        newId
                    }

                    // Restore split panes if saved in package
                    if (pkg.splitCount != null && pkg.splitCount > 1 && !pkg.splitPanes.isNullOrEmpty()) {
                        val restoredPanes = pkg.splitPanes.mapIndexed { idx, pane ->
                            val kind = runCatching { PaneKind.valueOf(pane.kind ?: "BOARD") }.getOrDefault(PaneKind.BOARD)
                            val tool = runCatching { WhiteboardTool.valueOf(pane.tool ?: "PEN") }.getOrDefault(WhiteboardTool.PEN)
                            CanvasPane(
                                id = idx,
                                kind = kind,
                                strokes = pane.strokes.orEmpty().map { it.toDomain() },
                                source = pane.source.orEmpty(),
                                label = pane.label.orEmpty(),
                                pageIndex = pane.pageIndex ?: 0,
                                pageCount = pane.pageCount ?: 0,
                                tool = tool,
                                color = pane.color ?: 0xFF1B1F2A.toInt(),
                                strokeWidth = pane.strokeWidth ?: 4f
                            )
                        }
                        _state.update {
                            it.copy(
                                splitCount = pkg.splitCount,
                                splitWeights = pkg.splitWeights ?: defaultWeights(pkg.splitCount),
                                splitPanes = restoredPanes,
                                splitActive = 0
                            )
                        }
                    }

                    _state.update {
                        it.copy(
                            noteTitle = effectiveTitle,
                            canvasBackground = background,
                            isDarkCanvas = pkg.metadata.isDarkCanvas,
                            showGrid = pkg.metadata.showGrid,
                            strokes = pages[activePage],
                            pageIndex = activePage,
                            pageCount = pages.size,
                            pagePreviews = pages.toList(),
                            scale = pkg.metadata.scale.coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE),
                            offsetX = pkg.metadata.offsetX,
                            offsetY = pkg.metadata.offsetY,
                            document = null,
                            errorMessage = null
                        )
                    }

                    // Persist to database & file
                    withContext(Dispatchers.IO) {
                        try {
                            val existing = noteRepository.getNoteById(currentNoteId)
                            if (existing != null) {
                                noteRepository.updateNote(existing.copy(title = effectiveTitle, canvasBackground = background, updatedAt = System.currentTimeMillis()))
                            } else {
                                noteRepository.insertNote(com.avitrisphare.whiteboard.domain.model.Note(id = currentNoteId, title = effectiveTitle, canvasBackground = background))
                            }
                        } catch (_: Exception) {}
                    }
                    saveNow()
                    onResult(true, "Lesson '$effectiveTitle' loaded with ${pages.size} page(s) and all layers intact!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to load GYOASH IWB: ${e.message}")
                }
            }
        }
    }

    fun setToolbarToolOrder(newOrder: List<String>) {
        _state.update { it.copy(toolbarToolOrder = newOrder) }
        context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TOOLBAR_ORDER_KEY, newOrder.joinToString(","))
            .apply()
    }

    fun moveToolbarTool(fromIndex: Int, toIndex: Int) {
        val current = _state.value.toolbarToolOrder.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setToolbarToolOrder(current)
        }
    }

    fun toggleToolbarToolVisibility(toolId: String) {
        val currentHidden = _state.value.hiddenToolbarTools
        val newHidden = if (toolId in currentHidden) {
            currentHidden - toolId
        } else {
            currentHidden + toolId
        }
        _state.update { it.copy(hiddenToolbarTools = newHidden) }
        context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(TOOLBAR_HIDDEN_KEY, newHidden)
            .apply()
    }

    fun hideToolbarTool(toolId: String) {
        val currentHidden = _state.value.hiddenToolbarTools
        if (toolId !in currentHidden) {
            val newHidden = currentHidden + toolId
            _state.update { it.copy(hiddenToolbarTools = newHidden) }
            context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(TOOLBAR_HIDDEN_KEY, newHidden)
                .apply()
        }
    }

    fun showToolbarTool(toolId: String) {
        val currentHidden = _state.value.hiddenToolbarTools
        val newHidden = currentHidden - toolId
        val currentOrder = _state.value.toolbarToolOrder
        val newOrder = if (toolId !in currentOrder) currentOrder + toolId else currentOrder
        _state.update { it.copy(hiddenToolbarTools = newHidden, toolbarToolOrder = newOrder) }
        context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(TOOLBAR_HIDDEN_KEY, newHidden)
            .putString(TOOLBAR_ORDER_KEY, newOrder.joinToString(","))
            .apply()
    }

    fun resetToolbarTools() {
        _state.update {
            it.copy(
                toolbarToolOrder = DEFAULT_TOOLBAR_TOOLS,
                hiddenToolbarTools = emptySet()
            )
        }
        context.getSharedPreferences(SHAPE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(TOOLBAR_ORDER_KEY)
            .remove(TOOLBAR_HIDDEN_KEY)
            .apply()
    }
}

private data class PageSnapshot(
    val strokes: List<StrokeDto> = emptyList(),
    val title: String? = null,
    val backgroundKey: String? = null,
    val customBackgroundUri: String? = null
)

private data class BoardSnapshot(
    val activePage: Int = 0,
    val showGrid: Boolean = false,
    val darkMode: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val pages: List<PageSnapshot> = emptyList(),
    val documentPath: String? = null,
    val documentName: String? = null,
    val documentKind: String? = null,
    val plainPages: List<PageSnapshot>? = null,
    val splitCount: Int? = null,
    val splitWeights: List<Float>? = null,
    val splitPanes: List<PaneSnapshot>? = null,
    val splitActive: Int? = null,
    val fromFile: Boolean = false,
    val pageTitles: List<String>? = null,
    val inkSampleGapMm: Float? = null,
    val secondaryColor: Int? = null,
    val secondaryStrokeWidth: Float? = null,
    val pdfOcrJson: String? = null,
    val documentFrameX: Float? = null,
    val documentFrameY: Float? = null,
    val documentFrameScale: Float? = null,
    val palmEraserEnabled: Boolean? = null,
    val restrictEraserToSelection: Boolean? = null
)

private data class PaneSnapshot(
    val kind: String? = null,
    val strokes: List<StrokeDto>? = null,
    val source: String? = null,
    val label: String? = null,
    val pageIndex: Int? = null,
    val pageCount: Int? = null,
    val tool: String? = null,
    val color: Int? = null,
    val strokeWidth: Float? = null
)

private fun CanvasPane.toSnapshot(): PaneSnapshot {
    return PaneSnapshot(
        kind = kind.name,
        strokes = strokes.map { it.toDto() },
        source = source,
        label = label,
        pageIndex = pageIndex,
        pageCount = pageCount,
        tool = tool.name,
        color = color,
        strokeWidth = strokeWidth
    )
}