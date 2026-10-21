package com.avitrisphare.whiteboard.presentation.canvas

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.drawscope.Stroke as ThumbStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlinx.coroutines.launch

private val DockColor = Color(0xFF1C2130)
private val DockLabel = Color(0xFFC9CED6)
private val PenGradient = listOf(Color(0xFF4C8DFF), Color(0xFF7A5CFF), Color(0xFFD946EF))

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AvitriSphareWhiteboardChrome(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onExit: () -> Unit,
    onOpenMenu: () -> Unit,
    onInsertImage: () -> Unit,
    onExport: () -> Unit,
    onOpenIwb: () -> Unit = {},
    onLive: () -> Unit,
    onHelp: () -> Unit,
    onClear: () -> Unit,
    onRename: () -> Unit,
    onWebEmbed: () -> Unit,
    onSearchImages: () -> Unit,
    onInsertVideo: () -> Unit,
    onYouTube: () -> Unit,
    shadeFromTop: Boolean,
    onToggleShadeSide: () -> Unit,
    spotlightRadius: Float,
    onSpotlightRadius: (Float) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
    rulerPlaced: Boolean,
    compassPlaced: Boolean,
    protractorPlaced: Boolean,
    onMathDrag: (MathToolKind, Offset) -> Unit,
    onMathDragEnd: (MathToolKind, Offset, Boolean) -> Unit,
    onDockBounds: (Rect) -> Unit,
    onOpenDocument: () -> Unit,
    demoMode: Boolean = false,
    dockDismissNonce: Int = 0,
    onSave: () -> Unit = {},
    onSaveSlides: () -> Unit = {},
    onCopySelection: () -> Unit = {},
    onPasteClipboard: () -> Unit = {},
    onToggleLocale: () -> Unit = {},
    isTimerVisible: Boolean = false,
    onToggleTimer: () -> Unit = {},
    isStopwatchVisible: Boolean = false,
    onToggleStopwatch: () -> Unit = {},
    isCalculatorVisible: Boolean = false,
    onToggleCalculator: () -> Unit = {},
    isPeriodicTableVisible: Boolean = false,
    onTogglePeriodicTable: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var panel by remember { mutableStateOf<String?>(null) }
    var pageMenuIndex by remember { mutableStateOf<Int?>(null) }
    var renamePageTitle by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(dockDismissNonce) {
        if (dockDismissNonce > 0) panel = null
    }

    fun toggle(name: String) {
        panel = if (panel == name) null else name
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (panel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { panel = null }
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!demoMode) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onExit)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back to class",
                            tint = Color(0xFF1F2937),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Class",
                            color = Color(0xFF1F2937),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                        )
                    }
                }
                Text(
                    text = state.noteTitle.ifBlank { "Untitled" },
                    color = Color(0xFF1F2937),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onRename() }
                        .padding(horizontal = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(state.selectedColor))
                )
                Text(
                    text = state.inkTool.label,
                    color = Color(0xFF1F2937),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Text(
                    text = CanvasTransform.formatZoom(state.scale),
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TopBarButton(
                    icon = Icons.Default.GridOn,
                    label = "Grid",
                    active = state.showGrid,
                    onClick = { viewModel.toggleGrid() }
                )
                TopBarButton(
                    icon = Icons.Default.GridOn,
                    label = "Graph",
                    active = state.canvasBackground == CanvasBackground.GRAPH,
                    onClick = { viewModel.toggleGraphPaper() }
                )
                TopBarButton(
                    icon = if (state.isDarkCanvas) Icons.Default.LightMode else Icons.Default.DarkMode,
                    label = if (state.isDarkCanvas) "Light" else "Dark",
                    active = state.isDarkCanvas,
                    onClick = { viewModel.toggleCanvasTheme() }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { onDockBounds(it.boundsInRoot()) }
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.selectedStrokeIds.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DockColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.selectedStrokeIds.size} selected",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        PEN_COLORS.take(6).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable { viewModel.recolorSelected(color) }
                            )
                        }
                        val canConvert = state.strokes.any {
                            it.id in state.selectedStrokeIds && it.strokeType in HANDWRITING_TYPES
                        }
                        if (canConvert) {
                            Text(
                                text = if (state.smartWriteBusy) "Reading…" else "Convert to Text",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB))
                                    .clickable(enabled = !state.smartWriteBusy) { viewModel.convertSelectionToText() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (state.equationBusy) "Reading equation…" else "Convert Equation",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF4F46E5))
                                    .clickable(enabled = !state.equationBusy) { viewModel.convertSelectionToEquation() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        val selectedEquation = state.strokes.singleOrNull {
                            it.id in state.selectedStrokeIds && it.strokeType == StrokeType.EQUATION
                        }
                        if (selectedEquation != null && state.formulaSolution?.solved != true) {
                            Text(
                                text = "Solve this",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F766E))
                                    .clickable { viewModel.solveCurrentEquation() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        val selectedImage = state.strokes.firstOrNull {
                            it.id in state.selectedStrokeIds && it.strokeType == StrokeType.IMAGE
                        }
                        if (state.selectedStrokeIds.isNotEmpty()) {
                            Text(
                                text = "Copy",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCopySelection() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                            if (state.hasStrokeClipboard) {
                                Text(
                                    text = "Paste",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onPasteClipboard() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        if (selectedImage != null) {
                            Text(
                                text = if (state.extractTextBusy) "Extracting…" else "Extract Text",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB))
                                    .clickable(enabled = !state.extractTextBusy) { viewModel.extractTextFromSelectedImage() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        val canEdit = state.strokes.count {
                            it.id in state.selectedStrokeIds && it.strokeType == StrokeType.TEXT
                        } == 1
                        if (canEdit && state.editingTextId == null) {
                            Text(
                                text = "Edit text",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.beginTextEdit() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete selection",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.deleteSelectedStrokes() }
                                .padding(5.dp)
                        )
                    }
                }
            }

            when (panel) {
                "profile" -> DockPopover {
                    Text("Profile", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.userName,
                        onValueChange = { viewModel.setUserName(it) },
                        singleLine = true,
                        label = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            focusedLabelColor = Color(0xFF6366F1)
                        )
                    )
                    PopoverRow(Icons.Default.Edit, "Rename board") {
                        panel = null
                        onRename()
                    }
                    PopoverRow(Icons.Default.Group, "Live collaboration") {
                        panel = null
                        onLive()
                    }
                }
                "shapes" -> DockPopover {
                    PopoverRow(Icons.Default.CropSquare, "Rectangle", state.currentTool == WhiteboardTool.RECTANGLE) {
                        viewModel.setTool(WhiteboardTool.RECTANGLE); panel = null
                    }
                    PopoverRow(Icons.Default.RadioButtonUnchecked, "Circle", state.currentTool == WhiteboardTool.CIRCLE) {
                        viewModel.setTool(WhiteboardTool.CIRCLE); panel = null
                    }
                    PopoverRow(Icons.Default.Diamond, "Diamond", state.currentTool == WhiteboardTool.DIAMOND) {
                        viewModel.setTool(WhiteboardTool.DIAMOND); panel = null
                    }
                    PopoverRow(Icons.Default.HorizontalRule, "Line", state.currentTool == WhiteboardTool.LINE) {
                        viewModel.setTool(WhiteboardTool.LINE); panel = null
                    }
                    PopoverRow(Icons.Default.ArrowForward, "Arrow", state.currentTool == WhiteboardTool.ARROW) {
                        viewModel.setTool(WhiteboardTool.ARROW); panel = null
                    }
                    PopoverRow(Icons.Default.ChangeHistory, "Triangle", state.currentTool == WhiteboardTool.TRIANGLE) {
                        viewModel.setTool(WhiteboardTool.TRIANGLE); panel = null
                    }
                    PopoverRow(Icons.Default.Star, "Star", state.currentTool == WhiteboardTool.STAR) {
                        viewModel.setTool(WhiteboardTool.STAR); panel = null
                    }
                    PopoverRow(Icons.Default.RadioButtonUnchecked, "Ellipse", state.currentTool == WhiteboardTool.ELLIPSE) {
                        viewModel.setTool(WhiteboardTool.ELLIPSE); panel = null
                    }
                    PopoverRow(Icons.Default.CropSquare, "Square", state.currentTool == WhiteboardTool.SQUARE) {
                        viewModel.setTool(WhiteboardTool.SQUARE); panel = null
                    }
                    PopoverRow(Icons.Default.Category, "Hexagon", state.currentTool == WhiteboardTool.HEXAGON) {
                        viewModel.setTool(WhiteboardTool.HEXAGON); panel = null
                    }
                    PopoverRow(Icons.Default.CropFree, "Rounded rect", state.currentTool == WhiteboardTool.ROUND_RECT) {
                        viewModel.setTool(WhiteboardTool.ROUND_RECT); panel = null
                    }
                    PopoverRow(Icons.Default.Architecture, "Cube", state.currentTool == WhiteboardTool.CUBE) {
                        viewModel.setTool(WhiteboardTool.CUBE); panel = null
                    }
                    PopoverRow(Icons.Default.RadioButtonUnchecked, "Cylinder", state.currentTool == WhiteboardTool.CYLINDER) {
                        viewModel.setTool(WhiteboardTool.CYLINDER); panel = null
                    }
                    PopoverRow(Icons.Default.ChangeHistory, "Cone", state.currentTool == WhiteboardTool.CONE) {
                        viewModel.setTool(WhiteboardTool.CONE); panel = null
                    }
                    PopoverRow(Icons.Default.Category, "Pyramid", state.currentTool == WhiteboardTool.PYRAMID) {
                        viewModel.setTool(WhiteboardTool.PYRAMID); panel = null
                    }
                    PopoverRow(Icons.Default.Dashboard, "Flowchart node") {
                        viewModel.addFlowchartNode("process", Offset(400f, 400f))
                        panel = null
                    }
                    PopoverRow(Icons.Default.Hub, "Mind map centre") {
                        viewModel.addMindMapCenter(Offset(480f, 360f))
                        panel = null
                    }
                    PopoverRow(Icons.Default.EmojiEmotions, "Emoji") {
                        viewModel.insertEmoji("😀", Offset(520f, 320f))
                        panel = null
                    }
                    PopoverRow(Icons.Default.Star, "Sticker") {
                        viewModel.insertSticker("star", Offset(560f, 320f))
                        panel = null
                    }
                }
                "geometry" -> DockPopover {
                    Text(
                        "Drag the ruler, compass, or protractor from the toolbar. Drag a tool back onto the toolbar to remove it.",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.height(8.dp))
                    PopoverRow(Icons.Default.GridOn, "Graph paper", state.canvasBackground == CanvasBackground.GRAPH) {
                        viewModel.toggleGraphPaper(); panel = null
                    }
                    PopoverRow(Icons.Default.AutoFixHigh, "Magic pen", state.isDrawToShapeEnabled) {
                        viewModel.toggleDrawToShape()
                    }
                    Text(
                        if (state.smartWriteEnabled) "Turn off Smart Write to use Magic pen." else "",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                    PopoverRow(Icons.Default.HorizontalRule, "Line", state.currentTool == WhiteboardTool.LINE) {
                        viewModel.setTool(WhiteboardTool.LINE); panel = null
                    }
                    PopoverRow(Icons.Default.ArrowForward, "Arrow", state.currentTool == WhiteboardTool.ARROW) {
                        viewModel.setTool(WhiteboardTool.ARROW); panel = null
                    }
                    PopoverRow(Icons.Default.CropFree, "Frame", state.currentTool == WhiteboardTool.FRAME) {
                        viewModel.setTool(WhiteboardTool.FRAME); panel = null
                    }
                }
                "media" -> DockPopover {
                    PopoverRow(Icons.Default.Description, "PDF or PowerPoint") { panel = null; onOpenDocument() }
                    PopoverRow(Icons.Default.Image, "Image from device") { panel = null; onInsertImage() }
                    PopoverRow(Icons.Default.Search, "Google Images") { panel = null; onSearchImages() }
                    PopoverRow(Icons.Default.Videocam, "Video from device") { panel = null; onInsertVideo() }
                    PopoverRow(Icons.Default.OndemandVideo, "YouTube") { panel = null; onYouTube() }
                    PopoverRow(Icons.Default.Language, "Website") { panel = null; onWebEmbed() }
                    PopoverRow(Icons.Default.CenterFocusStrong, "Spotlight", state.currentTool == WhiteboardTool.SPOTLIGHT) {
                        viewModel.setTool(WhiteboardTool.SPOTLIGHT); panel = null
                    }
                    PopoverRow(Icons.Default.VisibilityOff, "Screen shade", state.currentTool == WhiteboardTool.SHADE) {
                        viewModel.setTool(WhiteboardTool.SHADE)
                    }
                    if (state.currentTool == WhiteboardTool.SHADE) {
                        PopoverRow(
                            Icons.Default.SwapVert,
                            if (shadeFromTop) "Shade from top" else "Shade from bottom"
                        ) { onToggleShadeSide() }
                    }
                    if (state.currentTool == WhiteboardTool.SPOTLIGHT) {
                        Text("Spotlight size", fontSize = 12.sp, color = Color(0xFF6B7280))
                        Slider(
                            value = spotlightRadius.coerceIn(60f, 360f),
                            onValueChange = onSpotlightRadius,
                            valueRange = 60f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    PopoverRow(Icons.Default.ZoomIn, "Magnifier", state.currentTool == WhiteboardTool.MAGNIFIER) {
                        viewModel.setTool(WhiteboardTool.MAGNIFIER); panel = null
                    }
                    PopoverRow(Icons.Default.TextFields, "Text", state.currentTool == WhiteboardTool.TEXT) {
                        viewModel.setTool(WhiteboardTool.TEXT); panel = null
                    }
                    PopoverRow(Icons.Default.Description, "Sticky note", state.currentTool == WhiteboardTool.STICKY_NOTE) {
                        viewModel.setTool(WhiteboardTool.STICKY_NOTE); panel = null
                    }
                }
                "add" -> DockPopover {
                    PopoverRow(Icons.Default.TextFields, "Text") {
                        viewModel.setTool(WhiteboardTool.TEXT); panel = null
                    }
                    PopoverRow(Icons.Default.Description, "Sticky note") {
                        viewModel.setTool(WhiteboardTool.STICKY_NOTE); panel = null
                    }
                    PopoverRow(Icons.Default.Image, "Image") { panel = null; onInsertImage() }
                    PopoverRow(Icons.Default.Code, "Web embed") { panel = null; onWebEmbed() }
                }
                "split" -> DockPopover {
                    Text("Split screen", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Each panel keeps its own ink.",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    listOf(2, 3, 4, 6).forEach { count ->
                        PopoverRow(Icons.Default.Dashboard, "$count panels", state.splitCount == count) {
                            viewModel.setSplit(count)
                            panel = null
                        }
                    }
                    if (state.splitCount > 1) {
                        PopoverRow(Icons.Default.CropFree, "Full canvas") {
                            viewModel.requestCloseSplit()
                            panel = null
                        }
                    }
                }
                "more" -> DockPopover {
                    PopoverRow(Icons.Default.PanTool, "Hand", state.currentTool == WhiteboardTool.HAND) {
                        viewModel.setTool(WhiteboardTool.HAND); panel = null
                    }
                    PopoverRow(Icons.Default.Highlight, "Laser", state.currentTool == WhiteboardTool.LASER) {
                        viewModel.setTool(WhiteboardTool.LASER); panel = null
                    }
                    PopoverRow(Icons.Default.Gesture, "Lasso", state.currentTool == WhiteboardTool.LASSO) {
                        viewModel.setTool(WhiteboardTool.LASSO); panel = null
                    }
                    PopoverRow(Icons.Default.FormatColorFill, "Bucket fill", state.currentTool == WhiteboardTool.BUCKET_FILL) {
                        viewModel.setTool(WhiteboardTool.BUCKET_FILL); panel = null
                    }
                    PopoverRow(Icons.Default.Edit, "Stroke colour") {
                        viewModel.setTool(state.inkTool)
                        panel = "ink"
                    }
                    PopoverRow(Icons.Default.AutoFixHigh, "Magic pen", state.isDrawToShapeEnabled) {
                        viewModel.toggleDrawToShape()
                    }
                    Text(
                        if (state.smartWriteEnabled) "Turn off Smart Write to use Magic pen." else "",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                    PopoverRow(Icons.Default.GridOn, "Background") { panel = "background" }
                    PopoverRow(Icons.Default.IosShare, "Export lesson...") { panel = null; onExport() }
                    PopoverRow(Icons.Default.FolderOpen, "Open GYOASH Lesson (.iwb)...") { panel = null; onOpenIwb() }
                    PopoverRow(Icons.Default.Group, "Live collaboration") { panel = null; onLive() }
                    PopoverRow(Icons.Default.HelpOutline, "Help") { panel = null; onHelp() }
                    PopoverRow(Icons.Default.HourglassBottom, "Countdown Timer", isTimerVisible) { panel = null; onToggleTimer() }
                    PopoverRow(Icons.Default.Timer, "Stopwatch", isStopwatchVisible) { panel = null; onToggleStopwatch() }
                    PopoverRow(Icons.Default.Calculate, "Calculator", isCalculatorVisible) { panel = null; onToggleCalculator() }
                    PopoverRow(Icons.Default.Science, "Periodic Table", isPeriodicTableVisible) { panel = null; onTogglePeriodicTable() }
                    PopoverRow(Icons.Default.LayersClear, "Clear all") { panel = null; onClear() }
                    PopoverRow(Icons.Default.Save, "Save") { panel = null; onSave() }
                    PopoverRow(Icons.Default.Description, "Save slides (PDF)") { panel = null; onSaveSlides() }
                    PopoverRow(Icons.Default.Language, if (state.uiLocaleTag == "hi") "English" else "हिंदी") {
                        onToggleLocale()
                        panel = null
                    }
                    PopoverRow(Icons.Default.Add, "Zoom in") { onZoomIn() }
                    PopoverRow(Icons.Default.ChevronLeft, "Zoom out") { onZoomOut() }
                    PopoverRow(Icons.Default.CropFree, "Fit to content") { panel = null; onFit() }
                }
                "utilities" -> DockPopover {
                    Text("Class Utilities", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                    Spacer(Modifier.height(8.dp))
                    PopoverRow(Icons.Default.HourglassBottom, "Countdown Timer", isTimerVisible) {
                        panel = null
                        onToggleTimer()
                    }
                    PopoverRow(Icons.Default.Timer, "Stopwatch", isStopwatchVisible) {
                        panel = null
                        onToggleStopwatch()
                    }
                    PopoverRow(Icons.Default.Calculate, "Scientific Calculator", isCalculatorVisible) {
                        panel = null
                        onToggleCalculator()
                    }
                    PopoverRow(Icons.Default.Science, "Periodic Table", isPeriodicTableVisible) {
                        panel = null
                        onTogglePeriodicTable()
                    }
                }
                "eraser" -> DockPopover {
                    EraserToolPanel(state = state, viewModel = viewModel, onClearPage = {
                        panel = null
                        onClear()
                    })
                }
                "ink" -> DockPopover {
                    InkToolPanel(state = state, viewModel = viewModel)
                }
                "color" -> DockPopover {
                    Text("Stroke", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PEN_COLORS.take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable { viewModel.setColor(color) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = state.strokeWidth,
                        onValueChange = { viewModel.setStrokeWidth(it) },
                        valueRange = 1f..50f,
                        modifier = Modifier.width(220.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6366F1),
                            activeTrackColor = Color(0xFF6366F1)
                        )
                    )
                }
                "background" -> DockPopover {
                    Text("Background", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            CanvasBackground.DOTTED,
                            CanvasBackground.WHITE,
                            CanvasBackground.GRID,
                            CanvasBackground.GRAPH,
                            CanvasBackground.LINED,
                            CanvasBackground.DARK,
                            CanvasBackground.FOOTBALL
                        ).forEach { type ->
                            BackgroundSwatch(
                                type = type,
                                isSelected = state.canvasBackground == type,
                                onClick = {
                                    viewModel.setPageBackground(type)
                                    panel = null
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val count = state.pageCount.coerceAtLeast(1)
                val documentOpen = state.document != null
                val firstThumb = if (documentOpen) (state.pageIndex - 2).coerceAtLeast(0) else 0
                val lastThumb = if (documentOpen) (state.pageIndex + 3).coerceAtMost(count - 1) else count - 1
                for (index in firstThumb..lastThumb) {
                    val strokes = if (index == state.pageIndex) {
                        state.strokes
                    } else {
                        state.pagePreviews.getOrElse(index) { emptyList() }
                    }
                    PageThumbnail(
                        index = index,
                        title = state.pageTitles.getOrElse(index) { "Page ${index + 1}" },
                        strokes = strokes,
                        selected = index == state.pageIndex,
                        dark = state.isDarkCanvas,
                        onClick = { viewModel.selectPage(index) },
                        onLongClick = { pageMenuIndex = index }
                    )
                }
                if (state.document == null && state.pageCount < 50) {
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C2130))
                            .clickable { viewModel.addPage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add page", tint = Color.White)
                    }
                }
            }

            pageMenuIndex?.let { idx ->
                PageActionsDialog(
                    pageIndex = idx,
                    title = state.pageTitles.getOrElse(idx) { "Page ${idx + 1}" },
                    onDismiss = { pageMenuIndex = null },
                    onRename = { viewModel.renamePage(idx, it) },
                    onDelete = { viewModel.deletePage(idx) },
                    onDuplicate = { viewModel.duplicatePage(idx) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                DockBar {
                    DockItem(Icons.Default.Person, "Profile", panel == "profile") { toggle("profile") }
                    DockItem(Icons.Default.Menu, "Main Menu") { panel = null; onOpenMenu() }
                    DockItem(Icons.Default.Logout, "Exit") { panel = null; onExit() }
                }

                DockBar(modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shapeTools = setOf(
                            WhiteboardTool.RECTANGLE,
                            WhiteboardTool.CIRCLE,
                            WhiteboardTool.DIAMOND,
                            WhiteboardTool.LINE,
                            WhiteboardTool.ARROW
                        )
                        DockItem(Icons.Default.NearMe, "Select", state.currentTool == WhiteboardTool.SELECT) {
                            panel = null
                            viewModel.setTool(WhiteboardTool.SELECT)
                        }
                        DockItem(
                            icon = Icons.Default.Edit,
                            label = state.inkTool.label,
                            active = state.currentTool == state.inkTool,
                            swatch = state.selectedColor
                        ) {
                            viewModel.setTool(state.inkTool)
                            toggle("ink")
                        }
                        DockItem(Icons.Default.Category, "Shapes", state.currentTool in shapeTools || panel == "shapes") {
                            toggle("shapes")
                        }
                        DockItem(
                            Icons.Default.AutoFixNormal,
                            "Eraser",
                            state.currentTool == WhiteboardTool.ERASER || panel == "eraser"
                        ) {
                            viewModel.setTool(WhiteboardTool.ERASER)
                            toggle("eraser")
                        }
                        DockItem(Icons.Default.Highlight, "Laser", state.currentTool == WhiteboardTool.LASER) {
                            panel = null
                            viewModel.setTool(WhiteboardTool.LASER)
                        }
                        DockItem(
                            Icons.Default.ChangeHistory,
                            "Geometry",
                            state.isDrawToShapeEnabled || state.currentTool == WhiteboardTool.FRAME || panel == "geometry"
                        ) { toggle("geometry") }
                        DraggableDockItem(
                            icon = Icons.Default.Straighten,
                            label = "Ruler",
                            active = rulerPlaced || state.currentTool == WhiteboardTool.RULER,
                            onDrag = { onMathDrag(MathToolKind.RULER, it) },
                            onDragEnd = { at, moved -> onMathDragEnd(MathToolKind.RULER, at, moved) }
                        )
                        DraggableDockItem(
                            icon = Icons.Default.Architecture,
                            label = "Compass",
                            active = compassPlaced || state.currentTool == WhiteboardTool.COMPASS,
                            onDrag = { onMathDrag(MathToolKind.COMPASS, it) },
                            onDragEnd = { at, moved -> onMathDragEnd(MathToolKind.COMPASS, at, moved) }
                        )
                        DraggableDockItem(
                            icon = Icons.Default.Speed,
                            label = "Protractor",
                            active = protractorPlaced || state.currentTool == WhiteboardTool.PROTRACTOR,
                            onDrag = { onMathDrag(MathToolKind.PROTRACTOR, it) },
                            onDragEnd = { at, moved -> onMathDragEnd(MathToolKind.PROTRACTOR, at, moved) }
                        )
                        DockItem(
                            Icons.Default.Videocam,
                            "Media",
                            panel == "media" || state.currentTool == WhiteboardTool.TEXT || state.currentTool == WhiteboardTool.STICKY_NOTE
                        ) { toggle("media") }
                        DockItem(
                            Icons.Default.Widgets,
                            "Tools",
                            isTimerVisible || isStopwatchVisible || isCalculatorVisible || isPeriodicTableVisible || panel == "utilities"
                        ) { toggle("utilities") }
                        DockItem(Icons.Default.Undo, "Undo", enabled = state.canUndo) {
                            panel = null
                            viewModel.undo()
                        }
                        DockItem(Icons.Default.Redo, "Redo", enabled = state.canRedo) {
                            panel = null
                            viewModel.redo()
                        }
                        DockItem(Icons.Default.Save, "Save") {
                            panel = null
                            scope.launch {
                                viewModel.saveNow()
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                        DockItem(Icons.Default.IosShare, "Export") {
                            panel = null
                            onExport()
                        }
                        DockItem(Icons.Default.Dashboard, "Split", state.splitCount > 1 || panel == "split") {
                            toggle("split")
                        }
                        DockItem(Icons.Default.MoreHoriz, "More", panel == "more" || panel == "color" || panel == "background") {
                            toggle("more")
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DockBar {
                        DockItem(Icons.Default.ChevronLeft, "Prev", enabled = state.pageIndex > 0) {
                            viewModel.selectPage(state.pageIndex - 1)
                        }
                        Text(
                            "${state.pageIndex + 1}/${state.pageCount.coerceAtLeast(1)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A3142))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        DockItem(Icons.Default.ChevronRight, "Next", enabled = state.pageIndex < state.pageCount - 1) {
                            viewModel.selectPage(state.pageIndex + 1)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF8B5CF6).copy(alpha = 0.55f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(PenGradient))
                                .clickable { toggle("add") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = DockColor,
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = { content() }
        )
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    swatch: Int? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (active) Brush.linearGradient(PenGradient)
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
            if (swatch != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                )
            }
        }
        Text(
            text = label,
            color = if (enabled) DockLabel else Color(0xFF6B7280),
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun DraggableDockItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset, Boolean) -> Unit
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    var moved = 0f
                    var previous = down.position
                    var root = origin + down.position
                    onDrag(root)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            onDragEnd(root, moved > 12f)
                            break
                        }
                        val delta = change.position - previous
                        moved += delta.getDistance()
                        previous = change.position
                        root = origin + change.position
                        onDrag(root)
                        change.consume()
                    }
                }
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (active) Brush.linearGradient(PenGradient)
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(text = label, color = DockLabel, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun DockPopover(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .widthIn(min = 200.dp, max = 320.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun PopoverRow(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFEEF2FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1F2937), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color(0xFF1F2937), fontSize = 13.sp)
    }
}

@Composable
private fun TopBarButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0xFFEEF2FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) Color(0xFF4F46E5) else Color(0xFF374151),
            modifier = Modifier.size(18.dp)
        )
        Text(label, color = Color(0xFF4B5563), fontSize = 10.sp)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PageThumbnail(
    index: Int,
    title: String,
    strokes: List<Stroke>,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 76.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color(0xFF6366F1) else Color(0xFFD1D5DB))
                .padding(2.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
            ) {
                drawRect(if (dark) Color(0xFF171717) else Color.White)
                if (strokes.isEmpty()) return@Canvas
                var minX = Float.POSITIVE_INFINITY
                var minY = Float.POSITIVE_INFINITY
                var maxX = Float.NEGATIVE_INFINITY
                var maxY = Float.NEGATIVE_INFINITY
                strokes.forEach { stroke ->
                    stroke.points.forEach { point ->
                        if (point.x < minX) minX = point.x
                        if (point.y < minY) minY = point.y
                        if (point.x > maxX) maxX = point.x
                        if (point.y > maxY) maxY = point.y
                    }
                }
                if (minX == Float.POSITIVE_INFINITY) return@Canvas
                val spanX = (maxX - minX).coerceAtLeast(1f)
                val spanY = (maxY - minY).coerceAtLeast(1f)
                val scale = minOf(size.width / spanX, size.height / spanY) * 0.8f
                val ox = (size.width - spanX * scale) / 2f - minX * scale
                val oy = (size.height - spanY * scale) / 2f - minY * scale
                strokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach
                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.x * scale + ox, first.y * scale + oy)
                    for (i in 1 until stroke.points.size) {
                        val point = stroke.points[i]
                        path.lineTo(point.x * scale + ox, point.y * scale + oy)
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.colorValue),
                        style = ThumbStroke(width = 1.5f)
                    )
                }
            }
        }
        Text(
            text = title.take(12),
            color = if (selected) Color(0xFF4F46E5) else Color(0xFF6B7280),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PageActionsDialog(
    pageIndex: Int,
    title: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var draft by remember(title) { mutableStateOf(title) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page ${pageIndex + 1}") },
        text = {
            Column {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Name") })
            }
        },
        confirmButton = {
            Text("Save", modifier = Modifier.clickable { onRename(draft); onDismiss() })
        },
        dismissButton = {
            Row {
                Text("Duplicate", modifier = Modifier.clickable { onDuplicate(); onDismiss() }.padding(end = 12.dp))
                Text("Delete", modifier = Modifier.clickable { onDelete(); onDismiss() })
            }
        }
    )
}
