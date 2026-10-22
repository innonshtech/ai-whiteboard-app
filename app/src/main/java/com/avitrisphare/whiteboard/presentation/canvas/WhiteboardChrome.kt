package com.avitrisphare.whiteboard.presentation.canvas

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
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
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
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

private val DockColor = Color(0x66FFFFFF) // Figma: #FFFFFF 40%
private val DockLabel = Color(0xFF475569)
// Button gradient: linear-gradient(90deg, #22D3EE 0%, #3B82F6 50%, #A855F7 100%)
private val ButtonGradient = listOf(
    Color(0xFF22D3EE), // 0%
    Color(0xFF3B82F6), // 50%
    Color(0xFFA855F7)  // 100%
)

// Figma: #FFFFFF 40% translucent + #2563EB 20% frosted glass composite (#E4ECFC at 35% opacity for authentic glass translucency)
private val DockBackgroundColor = Color(0x59E4ECFC)
private val DockGlassBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f))

// Figma: Drop shadow X: 0, Y: 8, Blur: 28, Spread: 0, Behind transparent areas: checked, Color: #2563EB 20% (#2563EB33)
// We clip at top = 0 so the shadow casts downward and does NOT bleed upwards onto the canvas!
private fun Modifier.figmaDropShadow(
    cornerRadius: androidx.compose.ui.unit.Dp = 18.dp,
    offsetY: androidx.compose.ui.unit.Dp = 8.dp,
    blurRadius: androidx.compose.ui.unit.Dp = 20.dp,
    shadowColor: Color = Color(0x332563EB)
): Modifier = this.drawBehind {
    val offsetYPx = offsetY.toPx()
    val blurPx = blurRadius.toPx()
    val cornerRadiusPx = cornerRadius.toPx()

    drawIntoCanvas { canvas ->
        canvas.save()
        // Clip so shadow casts downwards and never spreads upwards onto the canvas
        canvas.clipRect(
            left = -blurPx * 2,
            top = 0f,
            right = size.width + blurPx * 2,
            bottom = size.height + offsetYPx + blurPx * 2
        )
        val paint = Paint().apply {
            color = shadowColor.toArgb()
            isAntiAlias = true
            if (blurPx > 0f) {
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }
        canvas.nativeCanvas.drawRoundRect(
            0f,
            offsetYPx,
            size.width,
            size.height + offsetYPx,
            cornerRadiusPx,
            cornerRadiusPx,
            paint
        )
        canvas.restore()
    }
}

private enum class ActiveDockTool {
    SELECT,
    PEN,
    SHAPES,
    ERASER,
    LASER,
    GEOMETRY,
    RULER,
    COMPASS,
    PROTRACTOR,
    MEDIA,
    TOOLS,
    SPLIT,
    MORE
}

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
    onFrostedBounds: (List<Rect>) -> Unit = {},
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
    var pageToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var renamePageTitle by remember { mutableStateOf("") }
    var selectionColorTarget by remember { mutableStateOf("border") }

    var topBarBounds by remember { mutableStateOf<Rect?>(null) }
    var leftBarBounds by remember { mutableStateOf<Rect?>(null) }
    var centerBarBounds by remember { mutableStateOf<Rect?>(null) }
    var rightBarBounds by remember { mutableStateOf<Rect?>(null) }

    androidx.compose.runtime.LaunchedEffect(topBarBounds, leftBarBounds, centerBarBounds, rightBarBounds) {
        val list = listOfNotNull(topBarBounds, leftBarBounds, centerBarBounds, rightBarBounds)
        onFrostedBounds(list)
    }

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
        if (panel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        panel = null
                    }
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .onGloballyPositioned { topBarBounds = it.boundsInRoot() }
                .figmaDropShadow(
                    cornerRadius = 18.dp,
                    offsetY = 6.dp,
                    blurRadius = 16.dp,
                    shadowColor = if (state.isDarkCanvas) Color(0x40000000) else Color(0x332563EB)
                ),
            shape = RoundedCornerShape(18.dp),
            color = if (state.isDarkCanvas) Color(0x661E293B) else DockBackgroundColor,
            border = if (state.isDarkCanvas) BorderStroke(1.dp, Color(0x40FFFFFF)) else DockGlassBorder,
            shadowElevation = 0.dp
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
                val selectedStrokes = remember(state.strokes, state.selectedStrokeIds) {
                    state.strokes.filter { it.id in state.selectedStrokeIds }
                }
                val hasFillableShape = remember(selectedStrokes) {
                    selectedStrokes.any { isFillableShape(it.strokeType) }
                }
                val activeShape = selectedStrokes.firstOrNull { isFillableShape(it.strokeType) }
                val currentBorderColor = activeShape?.colorValue ?: state.selectedColor
                val currentFillColor = activeShape?.fillColor ?: 0

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DockColor,
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        // Top row: status, mode switch, stroke width, actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${state.selectedStrokeIds.size} selected",
                                color = Color(0xFFC9CED6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))

                            if (hasFillableShape) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF262C38))
                                        .padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Border Tab
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectionColorTarget == "border") Color(0xFF3B82F6) else Color.Transparent)
                                            .clickable { selectionColorTarget = "border" }
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color(currentBorderColor), CircleShape)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "Border",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectionColorTarget == "border") FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Fill Tab
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectionColorTarget == "fill") Color(0xFF3B82F6) else Color.Transparent)
                                            .clickable { selectionColorTarget = "fill" }
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (currentFillColor == 0) {
                                            Text("⊘", color = Color(0xFFD1D5DB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(currentFillColor))
                                            )
                                        }
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "Fill",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectionColorTarget == "fill") FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                if (selectionColorTarget == "border") {
                                    Spacer(Modifier.width(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF262C38))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "−",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    val cur = activeShape?.strokeWidth ?: state.strokeWidth
                                                    viewModel.setStrokeWidth((cur - 2f).coerceAtLeast(1f))
                                                }
                                                .padding(horizontal = 3.dp)
                                        )
                                        Text(
                                            "${(activeShape?.strokeWidth ?: state.strokeWidth).toInt()}px",
                                            color = Color(0xFFE5E7EB),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                        Text(
                                            "+",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    val cur = activeShape?.strokeWidth ?: state.strokeWidth
                                                    viewModel.setStrokeWidth((cur + 2f).coerceAtMost(48f))
                                                }
                                                .padding(horizontal = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Text(
                                text = "Copy",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onCopySelection() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            if (state.hasStrokeClipboard) {
                                Text(
                                    text = "Paste",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPasteClipboard() }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selection",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.deleteSelectedStrokes() }
                                    .padding(3.dp)
                            )
                        }

                        // Bottom row: Color palette
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasFillableShape && selectionColorTarget == "fill") {
                                // None / Transparent swatch
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262C38))
                                        .border(
                                            if (currentFillColor == 0) 2.dp else 1.dp,
                                            if (currentFillColor == 0) Color.White else Color(0xFF6B7280),
                                            CircleShape
                                        )
                                        .clickable { viewModel.setSelectionFillColor(0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⊘", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val fillPalette = listOf(
                                    0xFFFFFFFF.toInt(),
                                    0xFF1B1F2A.toInt(),
                                    0xFFFF6B6B.toInt(),
                                    0xFF3ECF8E.toInt(),
                                    0xFF4ECDC4.toInt(),
                                    0xFFFFE66D.toInt(),
                                    0xFF60A5FA.toInt(),
                                    0xFFAA96DA.toInt()
                                )
                                fillPalette.forEach { color ->
                                    val isSelected = currentFillColor == color
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(color))
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                                else Modifier.border(0.5.dp, Color(0x44FFFFFF), CircleShape)
                                            )
                                            .clickable { viewModel.setSelectionFillColor(color) }
                                    )
                                }
                            } else {
                                val borderPalette = if (hasFillableShape) PEN_COLORS.take(8) else PEN_COLORS.take(7)
                                borderPalette.forEach { color ->
                                    val isSelected = (if (hasFillableShape) currentBorderColor else state.selectedColor) == color
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(color))
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                                else Modifier.border(0.5.dp, Color(0x44FFFFFF), CircleShape)
                                            )
                                            .clickable {
                                                if (hasFillableShape) viewModel.setSelectionBorderColor(color)
                                                else viewModel.recolorSelected(color)
                                            }
                                    )
                                }
                            }
                        }

                        // Optional row 3: Contextual actions for Handwriting / Equation / Image OCR
                        val canConvert = state.strokes.any { it.id in state.selectedStrokeIds && it.strokeType in HANDWRITING_TYPES }
                        val selectedEquation = state.strokes.singleOrNull { it.id in state.selectedStrokeIds && it.strokeType == StrokeType.EQUATION }
                        val selectedImage = state.strokes.firstOrNull { it.id in state.selectedStrokeIds && it.strokeType == StrokeType.IMAGE }
                        val canEdit = state.strokes.count { it.id in state.selectedStrokeIds && it.strokeType == StrokeType.TEXT } == 1

                        if (canConvert || (selectedEquation != null && state.formulaSolution?.solved != true) || selectedImage != null || (canEdit && state.editingTextId == null)) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canConvert) {
                                    Text(
                                        text = if (state.smartWriteBusy) "Reading…" else "Text",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2563EB))
                                            .clickable(enabled = !state.smartWriteBusy) { viewModel.convertSelectionToText() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (state.equationBusy) "Reading…" else "Math",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF4F46E5))
                                            .clickable(enabled = !state.equationBusy) { viewModel.convertSelectionToEquation() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                if (selectedEquation != null && state.formulaSolution?.solved != true) {
                                    Text(
                                        text = "Solve",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F766E))
                                            .clickable { viewModel.solveCurrentEquation() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                if (selectedImage != null) {
                                    Text(
                                        text = if (state.extractTextBusy) "Extracting…" else "OCR",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2563EB))
                                            .clickable(enabled = !state.extractTextBusy) { viewModel.extractTextFromSelectedImage() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                if (canEdit && state.editingTextId == null) {
                                    Text(
                                        text = "Edit text",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.beginTextEdit() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
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
                    ShapeToolPanel(
                        state = state,
                        viewModel = viewModel,
                        onClose = { panel = null }
                    )
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
                    EraserToolPanel(
                        state = state,
                        viewModel = viewModel,
                        onClearPage = {
                            panel = null
                            onClear()
                        },
                        onClose = { panel = null }
                    )
                }
                "ink" -> DockPopover {
                    InkToolPanel(state = state, viewModel = viewModel, onClose = { panel = null })
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
                        canDelete = !documentOpen && count > 1,
                        onClick = { viewModel.selectPage(index) },
                        onDelete = { pageToDeleteIndex = index },
                        onLongClick = { pageMenuIndex = index }
                    )
                }
                if (state.document == null && state.pageCount < 50) {
                    Surface(
                        modifier = Modifier
                            .size(width = 72.dp, height = 52.dp)
                            .clickable { viewModel.addPage() },
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.isDarkCanvas) Color(0xFF262626) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (state.isDarkCanvas) Color(0xFF404040) else Color(0xFFCBD5E1)),
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add page",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            pageToDeleteIndex?.let { idx ->
                val pageTitle = state.pageTitles.getOrElse(idx) { "Page ${idx + 1}" }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { pageToDeleteIndex = null },
                    title = { Text("Delete $pageTitle?", fontWeight = FontWeight.SemiBold) },
                    text = { Text("Are you sure you want to remove $pageTitle? All drawings on this page will be deleted.") },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.deletePage(idx)
                                pageToDeleteIndex = null
                                Toast.makeText(context, "$pageTitle removed", Toast.LENGTH_SHORT).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { pageToDeleteIndex = null }) {
                            Text("Cancel")
                        }
                    }
                )
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
                // 1) Left Bar: Profile, Main Menu, Exit
                DockBar(
                    modifier = Modifier.onGloballyPositioned { leftBarBounds = it.boundsInRoot() },
                    isDark = state.isDarkCanvas
                ) {
                    DockItem(
                        icon = Icons.Default.Person,
                        label = "Profile",
                        active = panel == "profile",
                        tint = Color(0xFF2563EB)
                    ) { toggle("profile") }

                    DockItem(
                        icon = Icons.Default.Menu,
                        label = "Main Menu",
                        tint = Color(0xFF2563EB)
                    ) { panel = null; onOpenMenu() }

                    DockItem(
                        icon = Icons.Default.Logout,
                        label = "Exit",
                        tint = Color(0xFFEF4444)
                    ) { panel = null; onExit() }
                }

                // 2) Center Bar: Main Tools
                DockBar(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .onGloballyPositioned { centerBarBounds = it.boundsInRoot() },
                    isDark = state.isDarkCanvas
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shapeTools = setOf(
                            WhiteboardTool.RECTANGLE,
                            WhiteboardTool.CIRCLE,
                            WhiteboardTool.DIAMOND,
                            WhiteboardTool.LINE,
                            WhiteboardTool.ARROW,
                            WhiteboardTool.SHAPE,
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
                        )

                        val activeDockTool = when {
                            panel == "ink" -> ActiveDockTool.PEN
                            panel == "shapes" -> ActiveDockTool.SHAPES
                            panel == "eraser" -> ActiveDockTool.ERASER
                            panel == "geometry" -> ActiveDockTool.GEOMETRY
                            panel == "media" -> ActiveDockTool.MEDIA
                            panel == "utilities" -> ActiveDockTool.TOOLS
                            panel == "split" -> ActiveDockTool.SPLIT
                            panel == "more" || panel == "color" || panel == "background" -> ActiveDockTool.MORE
                            state.currentTool in WRITING_TOOLS -> ActiveDockTool.PEN
                            state.currentTool in shapeTools -> ActiveDockTool.SHAPES
                            state.currentTool == WhiteboardTool.ERASER -> ActiveDockTool.ERASER
                            state.currentTool == WhiteboardTool.LASER -> ActiveDockTool.LASER
                            state.currentTool == WhiteboardTool.FRAME -> ActiveDockTool.GEOMETRY
                            state.currentTool == WhiteboardTool.RULER -> ActiveDockTool.RULER
                            state.currentTool == WhiteboardTool.COMPASS -> ActiveDockTool.COMPASS
                            state.currentTool == WhiteboardTool.PROTRACTOR -> ActiveDockTool.PROTRACTOR
                            state.currentTool == WhiteboardTool.TEXT || state.currentTool == WhiteboardTool.STICKY_NOTE -> ActiveDockTool.MEDIA
                            else -> ActiveDockTool.SELECT
                        }

                        DockItem(
                            icon = Icons.Default.NearMe,
                            label = "Select",
                            active = activeDockTool == ActiveDockTool.SELECT,
                            tint = Color(0xFF0EA5E9)
                        ) {
                            panel = null
                            viewModel.setTool(WhiteboardTool.SELECT)
                        }
                        DockItem(
                            icon = Icons.Default.Edit,
                            label = state.inkTool.label,
                            active = activeDockTool == ActiveDockTool.PEN,
                            tint = Color(0xFF2563EB),
                            swatch = state.selectedColor
                        ) {
                            viewModel.setTool(state.inkTool)
                            toggle("ink")
                        }
                        DockItem(
                            icon = Icons.Default.Category,
                            label = "Shapes",
                            active = activeDockTool == ActiveDockTool.SHAPES,
                            tint = Color(0xFF8B5CF6)
                        ) {
                            toggle("shapes")
                        }
                        DockItem(
                            icon = Icons.Default.AutoFixNormal,
                            label = "Eraser",
                            active = activeDockTool == ActiveDockTool.ERASER,
                            tint = Color(0xFFEC4899)
                        ) {
                            viewModel.setTool(WhiteboardTool.ERASER)
                            toggle("eraser")
                        }
                        DockItem(
                            icon = Icons.Default.Highlight,
                            label = "Laser",
                            active = activeDockTool == ActiveDockTool.LASER,
                            tint = Color(0xFFEAB308)
                        ) {
                            panel = null
                            viewModel.setTool(WhiteboardTool.LASER)
                        }
                        DockItem(
                            icon = Icons.Default.ChangeHistory,
                            label = "Geometry",
                            active = activeDockTool == ActiveDockTool.GEOMETRY,
                            tint = Color(0xFF10B981)
                        ) { toggle("geometry") }
                        DraggableDockItem(
                            icon = Icons.Default.Straighten,
                            label = "Ruler",
                            active = activeDockTool == ActiveDockTool.RULER,
                            tint = Color(0xFF6366F1),
                            onDrag = { onMathDrag(MathToolKind.RULER, it) },
                            onDragEnd = { at: Offset, moved: Boolean -> onMathDragEnd(MathToolKind.RULER, at, moved) }
                        )
                        DraggableDockItem(
                            icon = Icons.Default.Architecture,
                            label = "Compass",
                            active = activeDockTool == ActiveDockTool.COMPASS,
                            tint = Color(0xFF6366F1),
                            onDrag = { onMathDrag(MathToolKind.COMPASS, it) },
                            onDragEnd = { at: Offset, moved: Boolean -> onMathDragEnd(MathToolKind.COMPASS, at, moved) }
                        )
                        DraggableDockItem(
                            icon = Icons.Default.Speed,
                            label = "Protractor",
                            active = activeDockTool == ActiveDockTool.PROTRACTOR,
                            tint = Color(0xFF6366F1),
                            onDrag = { onMathDrag(MathToolKind.PROTRACTOR, it) },
                            onDragEnd = { at: Offset, moved: Boolean -> onMathDragEnd(MathToolKind.PROTRACTOR, at, moved) }
                        )
                        DockItem(
                            icon = Icons.Default.Videocam,
                            label = "Media",
                            active = activeDockTool == ActiveDockTool.MEDIA,
                            tint = Color(0xFF0EA5E9)
                        ) { toggle("media") }
                        DockItem(
                            icon = Icons.Default.Widgets,
                            label = "Tools",
                            active = activeDockTool == ActiveDockTool.TOOLS,
                            tint = Color(0xFF8B5CF6)
                        ) { toggle("utilities") }
                        DockItem(
                            icon = Icons.Default.Undo,
                            label = "Undo",
                            enabled = state.canUndo,
                            tint = Color(0xFFF59E0B)
                        ) {
                            panel = null
                            viewModel.undo()
                        }
                        DockItem(
                            icon = Icons.Default.Redo,
                            label = "Redo",
                            enabled = state.canRedo,
                            tint = Color(0xFFF97316)
                        ) {
                            panel = null
                            viewModel.redo()
                        }
                        DockItem(
                            icon = Icons.Default.Save,
                            label = "Save",
                            tint = Color(0xFF10B981)
                        ) {
                            panel = null
                            scope.launch {
                                viewModel.saveNow()
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                        DockItem(
                            icon = Icons.Default.IosShare,
                            label = "Export",
                            tint = Color(0xFF2563EB)
                        ) {
                            panel = null
                            onExport()
                        }
                        DockItem(
                            icon = Icons.Default.Dashboard,
                            label = "Split",
                            active = activeDockTool == ActiveDockTool.SPLIT,
                            tint = Color(0xFF6366F1)
                        ) {
                            toggle("split")
                        }
                        DockItem(
                            icon = Icons.Default.MoreHoriz,
                            label = "More",
                            active = activeDockTool == ActiveDockTool.MORE,
                            tint = Color(0xFF8B5CF6)
                        ) {
                            toggle("more")
                        }
                    }
                }

                // 3) Right Bar: Prev, Page Counter, Next (compact DockBar) + Add button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockBar(
                        modifier = Modifier.onGloballyPositioned { rightBarBounds = it.boundsInRoot() },
                        isDark = state.isDarkCanvas
                    ) {
                        DockItem(
                            icon = Icons.Default.ChevronLeft,
                            label = "Prev",
                            enabled = state.pageIndex > 0,
                            tint = Color(0xFF2563EB)
                        ) {
                            viewModel.selectPage(state.pageIndex - 1)
                        }

                        Text(
                            text = "${state.pageIndex + 1}/${state.pageCount.coerceAtLeast(1)}",
                            color = Color(0xFF1E293B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )

                        DockItem(
                            icon = Icons.Default.ChevronRight,
                            label = "Next",
                            enabled = state.pageIndex < state.pageCount - 1,
                            tint = Color(0xFF2563EB)
                        ) {
                            viewModel.selectPage(state.pageIndex + 1)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .width(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(ButtonGradient))
                            .clickable { toggle("add") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add page",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Add",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
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
    cornerRadius: androidx.compose.ui.unit.Dp = 22.dp,
    isDark: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.figmaDropShadow(
            cornerRadius = cornerRadius,
            offsetY = 8.dp,
            blurRadius = 20.dp,
            shadowColor = if (isDark) Color(0x40000000) else Color(0x332563EB)
        ),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isDark) Color(0x661E293B) else DockBackgroundColor,
        border = if (isDark) BorderStroke(1.dp, Color(0x40FFFFFF)) else DockGlassBorder,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Color(0xFF2563EB),
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
                    if (active) Brush.horizontalGradient(ButtonGradient)
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) Color.White else if (enabled) tint else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            if (swatch != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (!enabled) Color(0xFF94A3B8) else if (active) Color(0xFF1E293B) else DockLabel,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun DraggableDockItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    tint: Color = Color(0xFF6366F1),
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
                    if (active) Brush.horizontalGradient(ButtonGradient)
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) Color.White else tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (active) Color(0xFF1E293B) else DockLabel,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun DockPopover(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier
            .padding(bottom = 6.dp)
            .widthIn(min = 200.dp, max = 460.dp)
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
    canDelete: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
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

            if (canDelete) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.92f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete page",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
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
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onDuplicate(); onDismiss() }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Duplicate")
                    }
                    androidx.compose.material3.Button(
                        onClick = { onDelete(); onDismiss() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onRename(draft); onDismiss() }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
