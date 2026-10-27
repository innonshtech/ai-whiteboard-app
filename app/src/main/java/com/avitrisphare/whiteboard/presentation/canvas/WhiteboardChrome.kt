package com.avitrisphare.whiteboard.presentation.canvas

import android.widget.Toast
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Draw
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.Stroke as ThumbStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType

val LocalDarkTheme = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun localizedToolLabel(tool: WhiteboardTool): String = when (tool) {
    WhiteboardTool.SELECT -> stringResource(R.string.tool_select)
    WhiteboardTool.PEN -> stringResource(R.string.tool_pen)
    WhiteboardTool.PENCIL -> stringResource(R.string.tool_pencil)
    WhiteboardTool.MARKER -> stringResource(R.string.tool_marker)
    WhiteboardTool.HIGHLIGHTER -> stringResource(R.string.tool_highlighter)
    WhiteboardTool.BRUSH -> stringResource(R.string.tool_brush)
    WhiteboardTool.ERASER -> stringResource(R.string.tool_eraser)
    WhiteboardTool.LASER -> stringResource(R.string.tool_laser)
    WhiteboardTool.SHAPE -> stringResource(R.string.tool_shapes)
    else -> tool.label
}

private val DockColor = Color(0x66FFFFFF) // Figma: #FFFFFF 40%
private val DockLabel = Color(0xFF475569)
// Button gradient: linear-gradient(90deg, #22D3EE 0%, #3B82F6 50%, #A855F7 100%)
private val ButtonGradient = listOf(
    Color(0xFF22D3EE), // 0%
    Color(0xFF3B82F6), // 50%
    Color(0xFFA855F7)  // 100%
)

// Figma: Plain solid white background (#FFFFFF) + blue border (#9DC1FE)
private val DockBackgroundColor = Color.White
private val DockBorderColor = Color(0xFF9DC1FE)
private val DockBorder = BorderStroke(1.dp, DockBorderColor)

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
        val paint = cachedShadowPaint(shadowColor.toArgb(), blurPx)
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

private val shadowPaintCache = HashMap<Long, Paint>(4)

private fun cachedShadowPaint(color: Int, blurPx: Float): Paint {
    val key = (color.toLong() shl 16) xor blurPx.toInt().toLong()
    return shadowPaintCache.getOrPut(key) {
        Paint().apply {
            this.color = color
            isAntiAlias = true
            if (blurPx > 0f) {
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }
}

private fun Rect.snapped(): Rect = Rect(
    left.toInt().toFloat(),
    top.toInt().toFloat(),
    right.toInt().toFloat(),
    bottom.toInt().toFloat()
)

/** Keeps the previous rect when the bar only moved by a fraction of a pixel. */
private fun Rect.keptIfSame(current: Rect?): Rect {
    val snapped = snapped()
    if (current != null &&
        kotlin.math.abs(current.left - snapped.left) < 1f &&
        kotlin.math.abs(current.top - snapped.top) < 1f &&
        kotlin.math.abs(current.right - snapped.right) < 1f &&
        kotlin.math.abs(current.bottom - snapped.bottom) < 1f
    ) {
        return current
    }
    return snapped
}

private enum class ActiveDockTool {
    SELECT,
    PEN,
    SHAPES,
    FLOWCHART,
    STICKERS,
    ERASER,
    LASER,
    GEOMETRY,
    TOOLS
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
    placedMathTools: List<PlacedMathTool> = emptyList(),
    pxPerCm: Float = 96f / 2.54f,
    onMathDrag: (MathToolKind, Offset) -> Unit,
    onMathDragEnd: (MathToolKind, Offset, Boolean) -> Unit,
    onMathAdjust: (MathToolKind, MathToolAdjust) -> Unit = { _, _ -> },
    onDockBounds: (Rect) -> Unit,
    onFrostedBounds: (List<Rect>) -> Unit = {},
    onOpenDocument: () -> Unit,
    demoMode: Boolean = false,
    dockDismissNonce: Int = 0,
    selectionToolbarDismissed: Boolean = false,
    onDismissSelectionToolbar: (Boolean) -> Unit = {},
    onSave: () -> Unit = {},
    onSaveSlides: () -> Unit = {},
    onCopySelection: () -> Unit = {},
    onPasteClipboard: () -> Unit = {},
    onToggleLocale: () -> Unit = {},
    onOpenLanguageDialog: () -> Unit = {},
    isTimerVisible: Boolean = false,
    onToggleTimer: () -> Unit = {},
    isStopwatchVisible: Boolean = false,
    onToggleStopwatch: () -> Unit = {},
    isCalculatorVisible: Boolean = false,
    onToggleCalculator: () -> Unit = {},
    isPeriodicTableVisible: Boolean = false,
    onTogglePeriodicTable: () -> Unit = {},
    onPickCustomBackground: () -> Unit = {},
    onOpenStickerEmoji: () -> Unit = {},
    onSelectSticker: (String, String) -> Unit = { _, _ -> },
    onEditNodeText: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var panel by remember { mutableStateOf<String?>(null) }
    var shapesWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var selectionToolbarOffset by remember { mutableStateOf(Offset.Zero) }
    var pageMenuIndex by remember { mutableStateOf<Int?>(null) }
    var pageToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var renamePageTitle by remember { mutableStateOf("") }
    var selectionColorTarget by remember { mutableStateOf("border") }

    val toolIconBounds = remember { mutableStateMapOf<String, Rect>() }
    var chromeRootBounds by remember { mutableStateOf<Rect?>(null) }
    var topBarBounds by remember { mutableStateOf<Rect?>(null) }
    var leftBarBounds by remember { mutableStateOf<Rect?>(null) }
    var centerBarBounds by remember { mutableStateOf<Rect?>(null) }
    var rightBarBounds by remember { mutableStateOf<Rect?>(null) }
    var isNavbarVisible by rememberSaveable { mutableStateOf(true) }
    var isPagesListVisible by rememberSaveable { mutableStateOf(true) }
    var leftBarOffset by remember { mutableStateOf(Offset.Zero) }
    var centerBarOffset by remember { mutableStateOf(Offset.Zero) }
    var rightBarOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedToolId by remember { mutableStateOf<String?>(null) }
    var draggedOffset by remember { mutableStateOf(Offset.Zero) }

    androidx.compose.runtime.LaunchedEffect(topBarBounds, leftBarBounds, centerBarBounds, rightBarBounds, isNavbarVisible) {
        val list = if (isNavbarVisible) {
            listOfNotNull(topBarBounds, leftBarBounds, centerBarBounds, rightBarBounds)
        } else {
            listOfNotNull(topBarBounds)
        }
        onFrostedBounds(list)
    }

    androidx.compose.runtime.LaunchedEffect(isNavbarVisible) {
        if (!isNavbarVisible) {
            onDockBounds(Rect.Zero)
        }
    }

    androidx.compose.runtime.LaunchedEffect(dockDismissNonce) {
        if (dockDismissNonce > 0) panel = null
    }

    fun toggle(name: String) {
        panel = if (panel == name) null else name
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalDarkTheme provides state.isDarkTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .onGloballyPositioned { chromeRootBounds = it.boundsInRoot() }
    ) {
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



        AnimatedVisibility(
            visible = isNavbarVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            AnimatedVisibility(
                visible = state.selectedStrokeIds.isNotEmpty() && !selectionToolbarDismissed,
                enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 }
            ) {
                val selectedStrokes = remember(state.strokes, state.selectedStrokeIds) {
                    state.strokes.filter { it.id in state.selectedStrokeIds }
                }
                val selectedImage = selectedStrokes.firstOrNull { it.strokeType == StrokeType.IMAGE }
                val onlyImageSelected = selectedImage != null && selectedStrokes.size == 1

                if (onlyImageSelected) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (state.isDarkTheme) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .offset { IntOffset(selectionToolbarOffset.x.roundToInt(), selectionToolbarOffset.y.roundToInt()) }
                            .wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        selectionToolbarOffset = Offset(
                                            (selectionToolbarOffset.x + dragAmount.x).coerceIn(-1800f, 1800f),
                                            (selectionToolbarOffset.y + dragAmount.y).coerceIn(-1800f, 300f)
                                        )
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag toolbar",
                                tint = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = if (state.extractTextBusy) "Extracting…" else "OCR",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB))
                                    .clickable(enabled = !state.extractTextBusy) { viewModel.extractTextFromSelectedImage() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                                    .clickable { onCopySelection() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Copy",
                                    color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF334155),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (state.hasStrokeClipboard) {
                                Text(
                                    text = "Paste",
                                    color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF334155),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                                        .clickable { onPasteClipboard() }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .clickable { viewModel.deleteSelectedStrokes() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Remove",
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismissSelectionToolbar(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close toolbar",
                                    tint = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    val hasFillableShape = remember(selectedStrokes) {
                        selectedStrokes.any { isFillableShape(it.strokeType) }
                    }
                    val activeShape = selectedStrokes.firstOrNull { isFillableShape(it.strokeType) }
                    val currentBorderColor = activeShape?.colorValue ?: state.selectedColor
                    val currentFillColor = activeShape?.fillColor ?: 0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.isDarkTheme) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .offset { IntOffset(selectionToolbarOffset.x.roundToInt(), selectionToolbarOffset.y.roundToInt()) }
                            .widthIn(max = 440.dp)
                    ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Top row: drag handle, status, mode switch, stroke width, actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        selectionToolbarOffset = Offset(
                                            (selectionToolbarOffset.x + dragAmount.x).coerceIn(-1800f, 1800f),
                                            (selectionToolbarOffset.y + dragAmount.y).coerceIn(-1800f, 300f)
                                        )
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag toolbar",
                                tint = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                "${state.selectedStrokeIds.size} selected",
                                color = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))

                            if (hasFillableShape) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (state.isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
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
                                            color = if (selectionColorTarget == "border") Color.White else (if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                            fontSize = 11.sp,
                                            fontWeight = if (selectionColorTarget == "border") FontWeight.Bold else FontWeight.Medium
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
                                            Text("⊘", color = if (selectionColorTarget == "fill") Color.White else (if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                                            color = if (selectionColorTarget == "fill") Color.White else (if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                            fontSize = 11.sp,
                                            fontWeight = if (selectionColorTarget == "fill") FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }

                                if (selectionColorTarget == "border") {
                                    Spacer(Modifier.width(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (state.isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "−",
                                            color = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
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
                                            color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                        Text(
                                            "+",
                                            color = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
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
                                color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF334155),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (state.isDarkTheme) Color(0xFF334155) else Color.Transparent)
                                    .clickable { onCopySelection() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            if (state.hasStrokeClipboard) {
                                Text(
                                    text = "Paste",
                                    color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF334155),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (state.isDarkTheme) Color(0xFF334155) else Color.Transparent)
                                        .clickable { onPasteClipboard() }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .clickable { viewModel.deleteSelectedStrokes() }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Remove",
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close toolbar",
                                tint = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismissSelectionToolbar(true) }
                                    .padding(2.dp)
                            )
                            val isImageSelected = state.strokes.any {
                                it.id in state.selectedStrokeIds && it.strokeType == StrokeType.IMAGE
                            }
                            if (isImageSelected) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (state.extractTextBusy) "Extracting..." else "Extract Text",
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
                                Text(
                                    text = "Set as Background",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0D9488))
                                        .clickable { viewModel.setSelectedImageAsBackground() }
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
                        }

                        // Bottom row: Color palette (hidden if only media like images are selected)
                        val onlyMediaSelected = selectedStrokes.isNotEmpty() && selectedStrokes.all { it.isPlacedMedia() }
                        if (!onlyMediaSelected) {
                            Spacer(Modifier.height(8.dp))
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
                                            .background(Color(0xFFF1F5F9))
                                            .border(
                                                if (currentFillColor == 0) 2.dp else 1.dp,
                                                if (currentFillColor == 0) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
                                                CircleShape
                                            )
                                            .clickable { viewModel.setSelectionFillColor(0) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⊘", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                                    if (isSelected) Modifier.border(2.dp, Color(0xFF3B82F6), CircleShape)
                                                    else Modifier.border(0.8.dp, Color(0x33000000), CircleShape)
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
                                                    if (isSelected) Modifier.border(2.dp, Color(0xFF3B82F6), CircleShape)
                                                    else Modifier.border(0.8.dp, Color(0x33000000), CircleShape)
                                                )
                                                .clickable {
                                                    if (hasFillableShape) viewModel.setSelectionBorderColor(color)
                                                    else viewModel.recolorSelected(color)
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Optional row 3: Contextual actions for Handwriting / Equation / Image OCR
                        val canConvert = state.strokes.any { it.id in state.selectedStrokeIds && isConvertibleEquationInk(it) }
                        val selectedEquation = state.strokes.singleOrNull { it.id in state.selectedStrokeIds && it.strokeType == StrokeType.EQUATION }
                        val selectedImage = state.strokes.firstOrNull { it.id in state.selectedStrokeIds && it.strokeType == StrokeType.IMAGE }
                        val editableStroke = state.strokes.singleOrNull {
                            it.id in state.selectedStrokeIds && (
                                it.strokeType == StrokeType.TEXT ||
                                it.stickerKind.startsWith("flow:") ||
                                it.strokeType in setOf(
                                    StrokeType.RECTANGLE,
                                    StrokeType.DIAMOND,
                                    StrokeType.ELLIPSE,
                                    StrokeType.ROUND_RECT,
                                    StrokeType.CIRCLE,
                                    StrokeType.SQUARE
                                )
                            )
                        }
                        val canEdit = editableStroke != null

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
                                        text = if (state.equationBusy) "Reading…" else "Convert Equation",
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
                                        text = "Solve this",
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
                                if (canEdit && editableStroke != null && state.editingTextId == null) {
                                    Text(
                                        text = if (editableStroke.text.isNotEmpty()) "Edit text" else "Type text",
                                        color = Color(0xFF1E293B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable {
                                                if (editableStroke.strokeType == StrokeType.TEXT) {
                                                    viewModel.beginTextEdit()
                                                } else {
                                                    onEditNodeText(editableStroke.id, editableStroke.text)
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = isPagesListVisible,
                enter = fadeIn(tween(250)) + slideInHorizontally(tween(300)) { -it } + expandHorizontally(tween(300), expandFrom = Alignment.Start),
                exit = fadeOut(tween(250)) + slideOutHorizontally(tween(300)) { -it } + shrinkHorizontally(tween(300), shrinkTowards = Alignment.Start),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start)
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
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
                        val pageBgKey = state.pageBackgroundKeys.getOrElse(index) {
                            if (state.isDarkCanvas) CanvasBackground.DARK.key else CanvasBackground.DOTTED.key
                        }
                        val isPageDark = CanvasBackground.fromKey(pageBgKey) == CanvasBackground.DARK
                        PageThumbnail(
                            index = index,
                            title = state.pageTitles.getOrElse(index) { "Page ${index + 1}" },
                            strokes = strokes,
                            selected = index == state.pageIndex,
                            dark = isPageDark,
                            onClick = { viewModel.selectPage(index) },
                            onOptionsClick = { pageMenuIndex = index },
                            onLongClick = { pageMenuIndex = index }
                        )
                    }
                    if (state.document == null && state.pageCount < 50) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.addPage() }
                        ) {
                            Surface(
                                modifier = Modifier.size(width = 76.dp, height = 48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (state.isDarkTheme) Color(0xFF262626) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (state.isDarkTheme) Color(0xFF404040) else Color(0xFFCBD5E1)),
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
                            Text(
                                text = "",
                                fontSize = 10.sp
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(ButtonGradient))
                                .clickable { isPagesListVisible = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Hide pages list",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "",
                            fontSize = 10.sp
                        )
                    }
                }
            }

            pageToDeleteIndex?.let { idx ->
                val pageTitle = state.pageTitles.getOrElse(idx) { "Page ${idx + 1}" }
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { pageToDeleteIndex = null },
                    containerColor = if (state.isDarkTheme) Color(0xFF1E293B) else Color.White,
                    titleContentColor = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF111827),
                    textContentColor = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF4B5563),
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
                            Text("Cancel", color = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF6B7280))
                        }
                    }
                )
            }

            pageMenuIndex?.let { idx ->
                PageActionsDialog(
                    pageIndex = idx,
                    title = state.pageTitles.getOrElse(idx) { "Page ${idx + 1}" },
                    documentOpen = state.document != null,
                    pageCount = state.pageCount,
                    onDismiss = { pageMenuIndex = null },
                    onRename = { viewModel.renamePage(idx, it) },
                    onDelete = { viewModel.deletePage(idx) },
                    onDuplicate = { viewModel.duplicatePage(idx) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        val next = it.boundsInRoot().snapped()
                        onDockBounds(next)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // 1) Left Bar: Main Menu, Exit
                DockBar(
                    modifier = Modifier
                        .offset { IntOffset(leftBarOffset.x.roundToInt(), leftBarOffset.y.roundToInt()) }
                        .onGloballyPositioned {
                            leftBarBounds = it.boundsInRoot().keptIfSame(leftBarBounds)
                        },
                    isDark = state.isDarkTheme
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(width = 16.dp, height = 36.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    leftBarOffset = Offset(
                                        (leftBarOffset.x + dragAmount.x).coerceIn(-1200f, 1200f),
                                        (leftBarOffset.y + dragAmount.y).coerceIn(-1600f, 100f)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag Left Bar",
                            tint = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (leftBarOffset != Offset.Zero) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                                .clickable {
                                    leftBarOffset = Offset.Zero
                                    Toast.makeText(context, "Left bar reset", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Reset position",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    DockItem(
                        icon = Icons.Default.Menu,
                        label = stringResource(R.string.tool_main_menu),
                        tint = Color(0xFF2563EB)
                    ) { panel = null; onOpenMenu() }

                    // Divider between Main Menu and Exit
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .background(Color(0x3394A3B8))
                    )

                    DockItem(
                        icon = Icons.Default.Logout,
                        label = stringResource(R.string.tool_exit),
                        tint = Color(0xFFEF4444)
                    ) { panel = null; onExit() }
                }

                // 2) Center Bar: Main Tools
                DockBar(
                    modifier = Modifier
                        .offset { IntOffset(centerBarOffset.x.roundToInt(), centerBarOffset.y.roundToInt()) }
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .onGloballyPositioned {
                            centerBarBounds = it.boundsInRoot().keptIfSame(centerBarBounds)
                        },
                    isDark = state.isDarkTheme
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
                            panel == "flowchart" -> ActiveDockTool.FLOWCHART
                            panel == "stickers" -> ActiveDockTool.STICKERS
                            panel == "eraser" -> ActiveDockTool.ERASER
                            panel == "geometry" -> ActiveDockTool.GEOMETRY
                            panel == "utilities" || panel == "background" -> ActiveDockTool.TOOLS
                            state.currentTool in WRITING_TOOLS -> ActiveDockTool.PEN
                            state.currentTool == WhiteboardTool.RULER ||
                                state.currentTool == WhiteboardTool.COMPASS ||
                                state.currentTool == WhiteboardTool.PROTRACTOR ||
                                state.currentTool == WhiteboardTool.SET_SQUARE ||
                                state.currentTool == WhiteboardTool.FRAME ||
                                (state.currentTool == WhiteboardTool.TRIANGLE &&
                                    placedMathTools.any { it.kind == MathToolKind.TRIANGLE }) -> ActiveDockTool.GEOMETRY
                            state.currentTool in shapeTools -> ActiveDockTool.SHAPES
                            state.currentTool == WhiteboardTool.ERASER -> ActiveDockTool.ERASER
                            state.currentTool == WhiteboardTool.LASER -> ActiveDockTool.LASER
                            state.currentTool == WhiteboardTool.TEXT ||
                                state.currentTool == WhiteboardTool.SPOTLIGHT ||
                                state.currentTool == WhiteboardTool.SHADE ||
                                state.currentTool == WhiteboardTool.MAGNIFIER -> ActiveDockTool.TOOLS
                            else -> ActiveDockTool.SELECT
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(width = 16.dp, height = 36.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        centerBarOffset = Offset(
                                            (centerBarOffset.x + dragAmount.x).coerceIn(-1200f, 1200f),
                                            (centerBarOffset.y + dragAmount.y).coerceIn(-1600f, 100f)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag Center Bar",
                                tint = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (centerBarOffset != Offset.Zero) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                                    .clickable {
                                        centerBarOffset = Offset.Zero
                                        Toast.makeText(context, "Tools bar reset", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Reset position",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        val visibleTools = remember(state.toolbarToolOrder, state.hiddenToolbarTools) {
                            state.toolbarToolOrder.filter { it !in state.hiddenToolbarTools }
                        }
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val itemWidthPx = with(density) { 46.dp.toPx() }
                        val swipeUpThresholdPx = with(density) { 36.dp.toPx() }

                        visibleTools.forEach { toolId ->
                            val isDraggingThis = draggedToolId == toolId
                            val isAboveNavbar = isDraggingThis && draggedOffset.y < -swipeUpThresholdPx
                            val itemOffset = if (isDraggingThis) draggedOffset else Offset.Zero

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isDraggingThis) 100f else 1f)
                                    .offset { IntOffset(itemOffset.x.roundToInt(), itemOffset.y.roundToInt()) }
                                    .graphicsLayer {
                                        if (isDraggingThis) {
                                            scaleX = 1.14f
                                            scaleY = 1.14f
                                            shadowElevation = 12f
                                        }
                                    }
                                    .pointerInput(toolId, visibleTools) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedToolId = toolId
                                                draggedOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                draggedOffset += dragAmount
                                                val currentIdx = visibleTools.indexOf(toolId)
                                                val draggingAbove = draggedOffset.y < -swipeUpThresholdPx
                                                if (!draggingAbove && currentIdx != -1) {
                                                    if (draggedOffset.x > itemWidthPx && currentIdx < visibleTools.size - 1) {
                                                        val targetTool = visibleTools[currentIdx + 1]
                                                        val orderFrom = state.toolbarToolOrder.indexOf(toolId)
                                                        val orderTo = state.toolbarToolOrder.indexOf(targetTool)
                                                        if (orderFrom != -1 && orderTo != -1) {
                                                            viewModel.moveToolbarTool(orderFrom, orderTo)
                                                            draggedOffset = Offset(draggedOffset.x - itemWidthPx, draggedOffset.y)
                                                        }
                                                    } else if (draggedOffset.x < -itemWidthPx && currentIdx > 0) {
                                                        val targetTool = visibleTools[currentIdx - 1]
                                                        val orderFrom = state.toolbarToolOrder.indexOf(toolId)
                                                        val orderTo = state.toolbarToolOrder.indexOf(targetTool)
                                                        if (orderFrom != -1 && orderTo != -1) {
                                                            viewModel.moveToolbarTool(orderFrom, orderTo)
                                                            draggedOffset = Offset(draggedOffset.x + itemWidthPx, draggedOffset.y)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (draggedOffset.y < -swipeUpThresholdPx) {
                                                    viewModel.hideToolbarTool(toolId)
                                                    val toolName = getToolName(context, toolId)
                                                    Toast.makeText(context, context.getString(R.string.toast_moved_to_more, toolName), Toast.LENGTH_SHORT).show()
                                                }
                                                draggedToolId = null
                                                draggedOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedToolId = null
                                                draggedOffset = Offset.Zero
                                            }
                                        )
                                    }
                            ) {
                                if (isAboveNavbar) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFEF4444),
                                        shadowElevation = 8.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-32).dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = stringResource(R.string.release_to_move_more),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                when (toolId) {
                                    "select" -> {
                                        DockItem(
                                            icon = Icons.Default.NearMe,
                                            label = stringResource(R.string.tool_select),
                                            active = activeDockTool == ActiveDockTool.SELECT,
                                            tint = Color(0xFF0EA5E9),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["select"] = it.boundsInRoot() }
                                        ) {
                                            panel = null
                                            viewModel.setTool(WhiteboardTool.SELECT)
                                        }
                                    }
                                    "pen" -> {
                                        PenSplitDockItem(
                                            label = localizedToolLabel(state.inkTool),
                                            active = activeDockTool == ActiveDockTool.PEN,
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["ink"] = it.boundsInRoot() },
                                            onPenClick = {
                                                viewModel.setTool(state.inkTool)
                                                panel = null
                                            },
                                            onDropdownClick = {
                                                viewModel.setTool(state.inkTool)
                                                toggle("ink")
                                            }
                                        )
                                    }
                                    "color_swap" -> {
                                        TwoColorPenQuickSwap(
                                            primaryColor = state.selectedColor,
                                            secondaryColor = state.secondaryColor,
                                            onSwap = {
                                                viewModel.swapPenColors()
                                                Toast.makeText(context, context.getString(R.string.toast_color_swapped), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    "flowchart" -> {
                                        DockItem(
                                            icon = Icons.Default.Hub,
                                            label = stringResource(R.string.tool_flowchart),
                                            active = activeDockTool == ActiveDockTool.FLOWCHART,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["flowchart"] = it.boundsInRoot() }
                                        ) {
                                            toggle("flowchart")
                                        }
                                    }
                                    "stickers" -> {
                                        DockItem(
                                            icon = Icons.Default.Star,
                                            label = stringResource(R.string.tool_stickers),
                                            active = activeDockTool == ActiveDockTool.STICKERS,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["stickers"] = it.boundsInRoot() }
                                        ) {
                                            viewModel.clearSelection()
                                            toggle("stickers")
                                        }
                                    }
                                    "shapes" -> {
                                        DockItem(
                                            icon = Icons.Default.Category,
                                            label = stringResource(R.string.tool_shapes),
                                            active = activeDockTool == ActiveDockTool.SHAPES,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["shapes"] = it.boundsInRoot() }
                                        ) {
                                            if (panel == "shapes") {
                                                panel = null
                                            } else {
                                                panel = "shapes"
                                                viewModel.setTool(WhiteboardTool.SHAPE)
                                            }
                                        }
                                    }
                                    "eraser" -> {
                                        DockItem(
                                            icon = Icons.Default.AutoFixNormal,
                                            label = stringResource(R.string.tool_eraser),
                                            active = activeDockTool == ActiveDockTool.ERASER,
                                            tint = Color(0xFFEC4899),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["eraser"] = it.boundsInRoot() }
                                        ) {
                                            viewModel.setTool(WhiteboardTool.ERASER)
                                            toggle("eraser")
                                        }
                                    }
                                    "laser" -> {
                                        DockItem(
                                            icon = Icons.Default.Highlight,
                                            label = stringResource(R.string.tool_laser),
                                            active = activeDockTool == ActiveDockTool.LASER,
                                            tint = Color(0xFFEAB308),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["laser"] = it.boundsInRoot() }
                                        ) {
                                            panel = null
                                            viewModel.setTool(WhiteboardTool.LASER)
                                        }
                                    }
                                    "geometry" -> {
                                        DockItem(
                                            icon = Icons.Default.ChangeHistory,
                                            label = stringResource(R.string.tool_geometry),
                                            active = activeDockTool == ActiveDockTool.GEOMETRY,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["geometry"] = it.boundsInRoot() }
                                        ) { toggle("geometry") }
                                    }
                                    "tools" -> {
                                        DockItem(
                                            icon = Icons.Default.Widgets,
                                            label = stringResource(R.string.tool_tools),
                                            active = activeDockTool == ActiveDockTool.TOOLS,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["utilities"] = it.boundsInRoot() }
                                        ) { toggle("utilities") }
                                    }
                                    "text" -> {
                                        DockItem(
                                            icon = Icons.Default.TextFields,
                                            label = stringResource(R.string.util_text),
                                            active = state.currentTool == WhiteboardTool.TEXT,
                                            tint = Color(0xFF6366F1)
                                        ) {
                                            panel = null
                                            viewModel.setTool(WhiteboardTool.TEXT)
                                        }
                                    }
                                    "image" -> {
                                        DockItem(
                                            icon = Icons.Default.Image,
                                            label = stringResource(R.string.util_image),
                                            active = false,
                                            tint = Color(0xFF0EA5E9)
                                        ) {
                                            panel = null
                                            onInsertImage()
                                        }
                                    }
                                    "clear" -> {
                                        DockItem(
                                            icon = Icons.Default.LayersClear,
                                            label = stringResource(R.string.clear_all),
                                            active = false,
                                            tint = Color(0xFFEF4444)
                                        ) {
                                            panel = null
                                            onClear()
                                        }
                                    }
                                    "background" -> {
                                        DockItem(
                                            icon = Icons.Default.GridOn,
                                            label = stringResource(R.string.popover_background),
                                            active = panel == "background",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.onGloballyPositioned { toolIconBounds["background"] = it.boundsInRoot() }
                                        ) {
                                            toggle("background")
                                        }
                                    }
                                    "undo" -> {
                                        DockItem(
                                            icon = Icons.Default.Undo,
                                            label = stringResource(R.string.tool_undo),
                                            enabled = state.canUndo,
                                            tint = Color(0xFFF59E0B)
                                        ) {
                                            panel = null
                                            viewModel.undo()
                                        }
                                    }
                                    "redo" -> {
                                        DockItem(
                                            icon = Icons.Default.Redo,
                                            label = stringResource(R.string.tool_redo),
                                            enabled = state.canRedo,
                                            tint = Color(0xFFF97316)
                                        ) {
                                            panel = null
                                            viewModel.redo()
                                        }
                                    }
                                    "save" -> {
                                        DockItem(
                                            icon = Icons.Default.Save,
                                            label = stringResource(R.string.tool_save),
                                            tint = Color(0xFF10B981)
                                        ) {
                                            panel = null
                                            onSave()
                                            Toast.makeText(context, context.getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        // More item (three dots)
                        DockItem(
                            icon = Icons.Default.MoreHoriz,
                            label = stringResource(R.string.tool_more),
                            active = panel == "more_tools",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.onGloballyPositioned { toolIconBounds["more_tools"] = it.boundsInRoot() }
                        ) {
                            toggle("more_tools")
                        }
                        }
                    }
                }

                // 3) Right Bar: Prev, Page Counter, Next (compact DockBar) + Hide Arrow button
                Row(
                    modifier = Modifier.offset { IntOffset(rightBarOffset.x.roundToInt(), rightBarOffset.y.roundToInt()) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockBar(
                        modifier = Modifier.onGloballyPositioned {
                            rightBarBounds = it.boundsInRoot().keptIfSame(rightBarBounds)
                        },
                        isDark = state.isDarkTheme
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(width = 16.dp, height = 36.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        rightBarOffset = Offset(
                                            (rightBarOffset.x + dragAmount.x).coerceIn(-1200f, 1200f),
                                            (rightBarOffset.y + dragAmount.y).coerceIn(-1600f, 100f)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag Right Bar",
                                tint = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (rightBarOffset != Offset.Zero) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                                    .clickable {
                                        rightBarOffset = Offset.Zero
                                        Toast.makeText(context, "Right bar reset", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Reset position",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        DockItem(
                            icon = Icons.Default.ChevronLeft,
                            label = stringResource(R.string.tool_prev_page),
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
                                .clickable { isPagesListVisible = !isPagesListVisible }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )

                        DockItem(
                            icon = Icons.Default.ChevronRight,
                            label = stringResource(R.string.tool_next_page),
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
                            .clickable {
                                panel = null
                                isNavbarVisible = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Hide navigation bar",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

        AnimatedVisibility(
            visible = !isNavbarVisible,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it },
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .width(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(ButtonGradient))
                    .clickable { isNavbarVisible = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Show navigation bar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = !isPagesListVisible && isNavbarVisible,
            enter = fadeIn(tween(250)) + slideInHorizontally(tween(300)) { -it },
            exit = fadeOut(tween(250)) + slideOutHorizontally(tween(300)) { -it },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 76.dp, start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .width(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(ButtonGradient))
                    .clickable { isPagesListVisible = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Show pages list",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        val density = LocalDensity.current
        val expectedPopoverWidthPx = remember(panel) {
            val dpWidth = when (panel) {
                "eraser" -> 280.dp
                "flowchart" -> 310.dp
                "stickers" -> 340.dp
                "shapes" -> 340.dp
                "geometry" -> 340.dp
                "ink" -> 364.dp
                "utilities" -> 440.dp
                "more" -> 320.dp
                "profile" -> 300.dp
                "background" -> 340.dp
                "color" -> 260.dp
                else -> 320.dp
            }
            with(density) { dpWidth.toPx() }
        }
        var popoverMeasuredWidthPx by remember(panel) { mutableFloatStateOf(0f) }
        val activePopoverWidthPx = if (popoverMeasuredWidthPx > 0f) popoverMeasuredWidthPx else expectedPopoverWidthPx

        if (panel != null) {
            val iconBounds = toolIconBounds[panel]
            val chromeBounds = chromeRootBounds
            val localX = if (iconBounds != null && chromeBounds != null) {
                val iconCenterXInChrome = iconBounds.center.x - chromeBounds.left
                val idealLeft = iconCenterXInChrome - (activePopoverWidthPx / 2f)
                val minX = with(density) { 6.dp.toPx() }
                val maxX = (chromeBounds.width - activePopoverWidthPx - minX).coerceAtLeast(minX)
                idealLeft.coerceIn(minX, maxX)
            } else {
                0f
            }

            val dragOffsetX = when (panel) {
                "shapes" -> shapesWindowOffset.x
                "profile" -> leftBarOffset.x
                else -> centerBarOffset.x
            }
            val dragOffsetY = when (panel) {
                "shapes" -> shapesWindowOffset.y + centerBarOffset.y
                "profile" -> leftBarOffset.y
                else -> centerBarOffset.y
            }

            Box(
                modifier = Modifier
                    .align(if (iconBounds != null) Alignment.BottomStart else Alignment.BottomCenter)
                    .padding(bottom = 86.dp)
                    .offset {
                        if (iconBounds != null) {
                            IntOffset((localX + dragOffsetX).roundToInt(), dragOffsetY.roundToInt())
                        } else {
                            IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                        }
                    }
                    .onGloballyPositioned {
                        popoverMeasuredWidthPx = it.size.width.toFloat()
                    }
            ) {
                when (panel) {
                    "profile" -> DockPopover(title = "Profile", maxWidth = 300.dp, onClose = { panel = null }) {
                        OutlinedTextField(
                            value = state.userName,
                            onValueChange = { viewModel.setUserName(it) },
                            singleLine = true,
                            label = { Text("Your name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                unfocusedTextColor = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = if (state.isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                focusedLabelColor = Color(0xFF6366F1),
                                unfocusedLabelColor = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        PopoverRow(Icons.Default.Edit, "Rename board") {
                            panel = null
                            onRename()
                        }
                    }
                    "flowchart" -> DockPopover(title = "Flowchart & Nodes", maxWidth = 310.dp, onClose = { panel = null }) {
                        PopoverRow(Icons.Default.Dashboard, "3-Node Connected Flowchart") {
                            viewModel.createThreeNodeFlowchart(Offset(320f, 360f))
                            panel = null
                        }
                        PopoverRow(Icons.Default.CropSquare, "Process Node (Rectangle)") {
                            viewModel.addFlowchartNode("process", Offset(420f, 360f))
                            panel = null
                        }
                        PopoverRow(Icons.Default.Diamond, "Decision Node (Diamond)") {
                            viewModel.addFlowchartNode("decision", Offset(420f, 360f))
                            panel = null
                        }
                        PopoverRow(Icons.Default.RadioButtonUnchecked, "Terminator Node (Start/End)") {
                            viewModel.addFlowchartNode("terminator", Offset(420f, 360f))
                            panel = null
                        }
                    }
                    "stickers" -> DockPopover(maxWidth = 340.dp, scrollable = false) {
                        StickerEmojiPanelContent(
                            onSelectSticker = { kind, text ->
                                onSelectSticker(kind, text)
                                panel = null
                            },
                            onClose = { panel = null }
                        )
                    }
                    "shapes" -> Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 14.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.widthIn(min = 200.dp, max = 340.dp)
                    ) {
                        ShapeToolPanel(
                            state = state,
                            viewModel = viewModel,
                            onClose = { panel = null },
                            onDrag = { dragAmount ->
                                shapesWindowOffset = Offset(
                                    (shapesWindowOffset.x + dragAmount.x).coerceIn(-1800f, 1800f),
                                    (shapesWindowOffset.y + dragAmount.y).coerceIn(-1800f, 60f)
                                )
                            }
                        )
                    }
                    "geometry" -> DockPopover(maxWidth = 340.dp, scrollable = false) {
                        GeometryToolPanel(
                            state = state,
                            viewModel = viewModel,
                            placed = placedMathTools,
                            pxPerCm = pxPerCm,
                            onMathDrag = onMathDrag,
                            onMathDragEnd = onMathDragEnd,
                            onMathAdjust = onMathAdjust,
                            onClose = { panel = null }
                        )
                    }
                    "media" -> DockPopover(title = "Media", onClose = { panel = null }) {
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
                    "add" -> DockPopover(title = "Add", onClose = { panel = null }) {
                        PopoverRow(Icons.Default.TextFields, "Text") {
                            viewModel.setTool(WhiteboardTool.TEXT); panel = null
                        }
                        PopoverRow(Icons.Default.Description, "Sticky note") {
                            viewModel.setTool(WhiteboardTool.STICKY_NOTE); panel = null
                        }
                        PopoverRow(Icons.Default.Image, "Image") { panel = null; onInsertImage() }
                        PopoverRow(Icons.Default.Code, "Web embed") { panel = null; onWebEmbed() }
                    }
                    "split" -> DockPopover(title = "Split Screen", onClose = { panel = null }) {
                        Text(
                            "Each panel keeps its own ink.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(Modifier.height(4.dp))
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
                    "more" -> DockPopover(title = "More Options", onClose = { panel = null }) {
                        PopoverRow(Icons.Default.PanTool, "Hand", state.currentTool == WhiteboardTool.HAND) {
                            viewModel.setTool(WhiteboardTool.HAND); panel = null
                        }
                        PopoverRow(Icons.Default.Highlight, "Laser", state.currentTool == WhiteboardTool.LASER) {
                            viewModel.setTool(WhiteboardTool.LASER); panel = null
                        }
                        PopoverRow(Icons.Default.Gesture, stringResource(R.string.util_lasso), state.currentTool == WhiteboardTool.LASSO) {
                            viewModel.setTool(WhiteboardTool.LASSO); panel = null
                        }
                        PopoverRow(Icons.Default.FormatColorFill, stringResource(R.string.util_bucket_fill), state.currentTool == WhiteboardTool.BUCKET_FILL) {
                            viewModel.setTool(WhiteboardTool.BUCKET_FILL); panel = null
                        }
                        PopoverRow(Icons.Default.Edit, stringResource(R.string.util_stroke_colour)) {
                            viewModel.setTool(state.inkTool)
                            panel = "ink"
                        }
                        PopoverRow(Icons.Default.AutoFixHigh, stringResource(R.string.magic_pen), state.isDrawToShapeEnabled) {
                            viewModel.toggleDrawToShape()
                        }
                        if (state.smartWriteEnabled) {
                            Text(
                                stringResource(R.string.smart_write_tip),
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        PopoverRow(Icons.Default.GridOn, stringResource(R.string.popover_background)) { panel = "background" }
                        PopoverRow(Icons.Default.IosShare, stringResource(R.string.menu_export_lesson)) { panel = null; onExport() }
                        PopoverRow(Icons.Default.FolderOpen, stringResource(R.string.menu_open_iwb)) { panel = null; onOpenIwb() }
                        PopoverRow(Icons.Default.HelpOutline, stringResource(R.string.menu_help)) { panel = null; onHelp() }
                        PopoverRow(Icons.Default.HourglassBottom, stringResource(R.string.util_timer), isTimerVisible) { panel = null; onToggleTimer() }
                        PopoverRow(Icons.Default.Timer, stringResource(R.string.util_stopwatch), isStopwatchVisible) { panel = null; onToggleStopwatch() }
                        PopoverRow(Icons.Default.Calculate, stringResource(R.string.util_calculator), isCalculatorVisible) { panel = null; onToggleCalculator() }
                        PopoverRow(Icons.Default.Science, stringResource(R.string.util_periodic), isPeriodicTableVisible) { panel = null; onTogglePeriodicTable() }
                        PopoverRow(Icons.Default.LayersClear, stringResource(R.string.clear_all)) { panel = null; onClear() }
                        PopoverRow(Icons.Default.Save, stringResource(R.string.tool_save)) { panel = null; onSave() }
                        PopoverRow(Icons.Default.Description, stringResource(R.string.save_slides_pdf)) { panel = null; onSaveSlides() }
                        PopoverRow(Icons.Default.Language, stringResource(R.string.change_language)) {
                            panel = null
                            onOpenLanguageDialog()
                        }
                        PopoverRow(Icons.Default.Add, stringResource(R.string.popover_zoom_in)) { onZoomIn() }
                        PopoverRow(Icons.Default.ChevronLeft, stringResource(R.string.popover_zoom_out)) { onZoomOut() }
                        PopoverRow(Icons.Default.CropFree, stringResource(R.string.popover_fit)) { panel = null; onFit() }
                    }
                    "more_tools" -> DockPopover(title = stringResource(R.string.more_tools_title), onClose = { panel = null }, maxWidth = 480.dp) {
                        val hiddenList = ALL_CUSTOMIZABLE_TOOLS.filter { it in state.hiddenToolbarTools || it !in state.toolbarToolOrder }
                        if (hiddenList.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = null,
                                    tint = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.more_tools_empty),
                                    fontSize = 12.5.sp,
                                    color = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.more_tools_hint),
                                fontSize = 11.5.sp,
                                color = if (state.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                hiddenList.forEach { toolId ->
                                    val info = getToolDisplayInfo(toolId)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (state.isDarkTheme) Color(0xFF475569) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    panel = null
                                                    when (toolId) {
                                                        "select" -> viewModel.setTool(WhiteboardTool.SELECT)
                                                        "pen" -> viewModel.setTool(state.inkTool)
                                                        "color_swap" -> viewModel.swapPenColors()
                                                        "flowchart" -> toggle("flowchart")
                                                        "stickers" -> onOpenStickerEmoji()
                                                        "shapes" -> { panel = "shapes"; viewModel.setTool(WhiteboardTool.SHAPE) }
                                                        "eraser" -> { viewModel.setTool(WhiteboardTool.ERASER); toggle("eraser") }
                                                        "laser" -> viewModel.setTool(WhiteboardTool.LASER)
                                                        "geometry" -> toggle("geometry")
                                                        "tools" -> toggle("utilities")
                                                        "text" -> viewModel.setTool(WhiteboardTool.TEXT)
                                                        "image" -> onInsertImage()
                                                        "clear" -> onClear()
                                                        "background" -> toggle("background")
                                                        "undo" -> viewModel.undo()
                                                        "redo" -> viewModel.redo()
                                                        "save" -> { onSave(); Toast.makeText(context, context.getString(R.string.toast_saved), Toast.LENGTH_SHORT).show() }
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(info.tint.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    info.icon,
                                                    contentDescription = info.name,
                                                    tint = info.tint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = info.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF2563EB),
                                                modifier = Modifier.clickable {
                                                    viewModel.showToolbarTool(toolId)
                                                    Toast.makeText(context, context.getString(R.string.toast_restored_to_toolbar, info.name), Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "Add to toolbar",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        text = stringResource(R.string.action_add),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.resetToolbarTools()
                                    Toast.makeText(context, "Toolbar reset to default", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.customize_toolbar_reset), fontSize = 12.sp)
                            }
                        }
                    }
                    "utilities" -> DockPopover(maxWidth = 580.dp) {
                        val utilityTools = setOf(
                            WhiteboardTool.SPOTLIGHT,
                            WhiteboardTool.SHADE,
                            WhiteboardTool.MAGNIFIER,
                            WhiteboardTool.TEXT
                        )
                        var toolTab by remember {
                            mutableStateOf(if (state.currentTool in utilityTools) 2 else 0)
                        }
                        val tabs = listOf(
                            stringResource(R.string.tab_board),
                            stringResource(R.string.tab_media),
                            stringResource(R.string.tab_utilities),
                            stringResource(R.string.tab_lesson),
                            stringResource(R.string.tab_view)
                        )
                        ToolCategoryTabs(tabs, toolTab) { toolTab = it }
                        Spacer(Modifier.height(12.dp))
                        val tiles = when (toolTab) {
                            0 -> listOf(
                                ToolTile(Icons.Default.PanTool, stringResource(R.string.util_hand), Color(0xFF2563EB), state.currentTool == WhiteboardTool.HAND) {
                                    viewModel.setTool(WhiteboardTool.HAND); panel = null
                                },
                                ToolTile(Icons.Default.Gesture, stringResource(R.string.util_lasso), Color(0xFF0EA5E9), state.currentTool == WhiteboardTool.LASSO) {
                                    viewModel.setTool(WhiteboardTool.LASSO); panel = null
                                },
                                ToolTile(Icons.Default.FormatColorFill, stringResource(R.string.util_bucket_fill), Color(0xFF8B5CF6), state.currentTool == WhiteboardTool.BUCKET_FILL) {
                                    viewModel.setTool(WhiteboardTool.BUCKET_FILL); panel = null
                                },
                                ToolTile(Icons.Default.Edit, stringResource(R.string.util_stroke_colour), Color(0xFF2563EB)) {
                                    viewModel.setTool(state.inkTool)
                                    panel = "ink"
                                },
                                ToolTile(Icons.Default.AutoFixHigh, stringResource(R.string.magic_pen), Color(0xFFEC4899), state.isDrawToShapeEnabled) {
                                    viewModel.toggleDrawToShape()
                                },
                                ToolTile(Icons.Default.GridOn, stringResource(R.string.popover_background), Color(0xFF10B981)) { panel = "background" },
                                ToolTile(
                                    if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    if (state.isDarkTheme) stringResource(R.string.theme_light) else stringResource(R.string.theme_dark),
                                    Color(0xFFF59E0B),
                                    state.isDarkTheme
                                ) {
                                    viewModel.toggleAppTheme()
                                },
                                ToolTile(
                                    Icons.Default.MoreHoriz,
                                    stringResource(R.string.more_tools_title),
                                    Color(0xFF8B5CF6)
                                ) {
                                    panel = "more_tools"
                                }
                            )
                            1 -> listOf(
                                ToolTile(Icons.Default.Description, stringResource(R.string.util_pdf), Color(0xFFEF4444)) { panel = null; onOpenDocument() },
                                ToolTile(Icons.Default.Image, stringResource(R.string.util_image), Color(0xFF2563EB)) { panel = null; onInsertImage() },
                                ToolTile(Icons.Default.Search, stringResource(R.string.util_google), Color(0xFF0EA5E9)) { panel = null; onSearchImages() },
                                ToolTile(Icons.Default.Videocam, stringResource(R.string.util_video), Color(0xFF8B5CF6)) { panel = null; onInsertVideo() },
                                ToolTile(Icons.Default.OndemandVideo, stringResource(R.string.util_youtube), Color(0xFFEF4444)) { panel = null; onYouTube() },
                                ToolTile(Icons.Default.Language, stringResource(R.string.util_website), Color(0xFF10B981)) { panel = null; onWebEmbed() }
                            )
                            2 -> listOf(
                                ToolTile(Icons.Default.HourglassBottom, stringResource(R.string.util_timer), Color(0xFFF59E0B), isTimerVisible) {
                                    panel = null; onToggleTimer()
                                },
                                ToolTile(Icons.Default.Timer, stringResource(R.string.util_stopwatch), Color(0xFF6366F1), isStopwatchVisible) {
                                    panel = null; onToggleStopwatch()
                                },
                                ToolTile(Icons.Default.Calculate, stringResource(R.string.util_calculator), Color(0xFF0EA5E9), isCalculatorVisible) {
                                    panel = null; onToggleCalculator()
                                },
                                ToolTile(Icons.Default.Science, stringResource(R.string.util_periodic), Color(0xFF10B981), isPeriodicTableVisible) {
                                    panel = null; onTogglePeriodicTable()
                                },
                                ToolTile(Icons.Default.CenterFocusStrong, stringResource(R.string.util_spotlight), Color(0xFFF59E0B), state.currentTool == WhiteboardTool.SPOTLIGHT) {
                                    viewModel.setTool(WhiteboardTool.SPOTLIGHT)
                                },
                                ToolTile(Icons.Default.VisibilityOff, stringResource(R.string.util_shade), Color(0xFF1F2937), state.currentTool == WhiteboardTool.SHADE) {
                                    viewModel.setTool(WhiteboardTool.SHADE)
                                },
                                ToolTile(Icons.Default.ZoomIn, stringResource(R.string.util_magnifier), Color(0xFF6366F1), state.currentTool == WhiteboardTool.MAGNIFIER) {
                                    viewModel.setTool(WhiteboardTool.MAGNIFIER); panel = null
                                },
                                ToolTile(Icons.Default.TextFields, stringResource(R.string.util_text), Color(0xFF2563EB), state.currentTool == WhiteboardTool.TEXT) {
                                    viewModel.setTool(WhiteboardTool.TEXT); panel = null
                                }
                            )
                            3 -> listOf(
                                ToolTile(Icons.Default.FolderOpen, "GYOASH", Color(0xFF2563EB)) { panel = null; onOpenIwb() },
                                ToolTile(Icons.Default.IosShare, stringResource(R.string.menu_export_lesson), Color(0xFF2563EB)) { panel = null; onExport() },
                                ToolTile(Icons.Default.Description, stringResource(R.string.save_slides_pdf), Color(0xFFEF4444)) { panel = null; onSaveSlides() },
                                ToolTile(Icons.Default.LayersClear, stringResource(R.string.clear_all), Color(0xFFEF4444)) { panel = null; onClear() },
                                ToolTile(Icons.Default.HelpOutline, stringResource(R.string.menu_help), Color(0xFF6B7280)) { panel = null; onHelp() }
                            )
                            else -> buildList {
                                add(
                                    ToolTile(
                                        Icons.Default.Language,
                                        stringResource(R.string.change_language),
                                        Color(0xFF2563EB)
                                    ) {
                                        panel = null
                                        onOpenLanguageDialog()
                                    }
                                )
                                add(ToolTile(Icons.Default.ZoomIn, stringResource(R.string.popover_zoom_in), Color(0xFF0EA5E9)) { onZoomIn() })
                                add(ToolTile(Icons.Default.ZoomOut, stringResource(R.string.popover_zoom_out), Color(0xFF6366F1)) { onZoomOut() })
                                add(ToolTile(Icons.Default.CropFree, stringResource(R.string.popover_fit), Color(0xFF10B981)) { panel = null; onFit() })
                                listOf(2, 3, 4, 6).forEach { count ->
                                    add(
                                        ToolTile(
                                            Icons.Default.Dashboard,
                                            context.getString(R.string.util_panels, count),
                                            Color(0xFF6366F1),
                                            state.splitCount == count
                                        ) {
                                            viewModel.setSplit(count)
                                            panel = null
                                        }
                                    )
                                }
                                if (state.splitCount > 1) {
                                    add(
                                        ToolTile(Icons.Default.CropFree, "Full", Color(0xFF1F2937)) {
                                            viewModel.requestCloseSplit()
                                            panel = null
                                        }
                                    )
                                }
                            }
                        }
                        ToolIconGrid(tiles)
                        if (toolTab == 0 && state.smartWriteEnabled) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.smart_write_tip),
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        if (toolTab == 2 && state.currentTool == WhiteboardTool.SHADE) {
                            Spacer(Modifier.height(4.dp))
                            PopoverRow(
                                Icons.Default.SwapVert,
                                if (shadeFromTop) "Shade from top" else "Shade from bottom"
                            ) { onToggleShadeSide() }
                        }
                        if (toolTab == 2 && state.currentTool == WhiteboardTool.SPOTLIGHT) {
                            Spacer(Modifier.height(4.dp))
                            Text("Spotlight size", fontSize = 12.sp, color = Color(0xFF6B7280))
                            Slider(
                                value = spotlightRadius.coerceIn(60f, 360f),
                                onValueChange = onSpotlightRadius,
                                valueRange = 60f..360f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (toolTab == 4) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Each split panel keeps its own ink.",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    "eraser" -> DockPopover(maxWidth = 280.dp, scrollable = false) {
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
                    "ink" -> DockPopover(maxWidth = 560.dp, scrollable = false) {
                        InkToolPanel(state = state, viewModel = viewModel, onClose = { panel = null })
                    }
                    "color" -> DockPopover(title = "Stroke & Width", onClose = { panel = null }) {
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
                    "background" -> DockPopover(title = "Background", onClose = { panel = null }) {
                        Text("Board Styles", fontSize = 11.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                CanvasBackground.WHITE to "White",
                                CanvasBackground.DARK to "Dark",
                                CanvasBackground.LINED to "Lined",
                                CanvasBackground.DOTTED to "Dotted",
                                CanvasBackground.GRID to "Grid",
                                CanvasBackground.GRAPH to "Graph",
                                CanvasBackground.FOOTBALL to "Football",
                                CanvasBackground.CUSTOM to "Image"
                            ).forEach { (type, label) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        if (type == CanvasBackground.CUSTOM) {
                                            val hasImage = state.strokes.any { it.strokeType == StrokeType.IMAGE && it.imageUri.isNotBlank() }
                                            if (hasImage) {
                                                viewModel.setSelectedImageAsBackground()
                                            } else {
                                                onPickCustomBackground()
                                            }
                                        } else {
                                            viewModel.setPageBackground(type)
                                        }
                                        panel = null
                                    }
                                ) {
                                    BackgroundSwatch(
                                        type = type,
                                        isSelected = state.canvasBackground == type,
                                        onClick = {
                                            if (type == CanvasBackground.CUSTOM) {
                                                val hasImage = state.strokes.any { it.strokeType == StrokeType.IMAGE && it.imageUri.isNotBlank() }
                                                if (hasImage) {
                                                    viewModel.setSelectedImageAsBackground()
                                                } else {
                                                    onPickCustomBackground()
                                                }
                                            } else {
                                                viewModel.setPageBackground(type)
                                            }
                                            panel = null
                                        }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        color = if (state.canvasBackground == type) Color(0xFF4F46E5) else Color(0xFF6B7280),
                                        fontWeight = if (state.canvasBackground == type) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
                        Spacer(Modifier.height(8.dp))

                        Text("Selected Image Background", fontSize = 11.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))

                        val hasSelectedImage = state.strokes.any { it.id in state.selectedStrokeIds && it.strokeType == StrokeType.IMAGE && it.imageUri.isNotBlank() }
                        val hasAnyImage = state.strokes.any { it.strokeType == StrokeType.IMAGE && it.imageUri.isNotBlank() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hasSelectedImage) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                                .clickable {
                                    if (hasSelectedImage || hasAnyImage) {
                                        viewModel.setSelectedImageAsBackground()
                                        panel = null
                                    } else {
                                        onPickCustomBackground()
                                        panel = null
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Selected image as background",
                                tint = if (hasSelectedImage) Color(0xFF2563EB) else Color(0xFF4B5563),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected Image",
                                    color = if (hasSelectedImage) Color(0xFF1D4ED8) else Color(0xFF1F2937),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasSelectedImage) "Apply selected image as canvas background"
                                    else if (hasAnyImage) "Use image from this board as background"
                                    else "Select or insert an image to set as background",
                                    color = Color(0xFF6B7280),
                                    fontSize = 10.sp
                                )
                            }
                            if (state.canvasBackground == CanvasBackground.CUSTOM) {
                                Text("Active", color = Color(0xFF16A34A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF9FAFB))
                                .clickable {
                                    onPickCustomBackground()
                                    panel = null
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Choose image file",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Choose Image from Device",
                                    color = Color(0xFF1F2937),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Pick a picture from storage / gallery",
                                    color = Color(0xFF6B7280),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (panel == "shapes") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .offset { IntOffset((shapesWindowOffset.x + centerBarOffset.x).roundToInt(), (shapesWindowOffset.y + centerBarOffset.y).roundToInt()) }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (state.isDarkTheme) Color(0xFF1E293B) else Color.White,
                    shadowElevation = 14.dp,
                    border = BorderStroke(1.dp, if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.widthIn(min = 200.dp, max = 460.dp)
                ) {
                    ShapeToolPanel(
                        state = state,
                        viewModel = viewModel,
                        onClose = { panel = null },
                        onDrag = { dragAmount ->
                            shapesWindowOffset = Offset(
                                (shapesWindowOffset.x + dragAmount.x).coerceIn(-1800f, 1800f),
                                (shapesWindowOffset.y + dragAmount.y).coerceIn(-1800f, 60f)
                            )
                        }
                    )
                }
            }
        }
    }
}

data class ToolDisplayInfo(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun getToolDisplayInfo(id: String): ToolDisplayInfo {
    return when (id) {
        "select" -> ToolDisplayInfo(id, stringResource(R.string.tool_select), Icons.Default.NearMe, Color(0xFF0EA5E9))
        "pen" -> ToolDisplayInfo(id, stringResource(R.string.tool_pen), Icons.Default.Edit, Color(0xFF3B82F6))
        "color_swap" -> ToolDisplayInfo(id, stringResource(R.string.tool_color_swap), Icons.Default.SwapHoriz, Color(0xFF6366F1))
        "flowchart" -> ToolDisplayInfo(id, stringResource(R.string.tool_flowchart), Icons.Default.Hub, Color(0xFF2563EB))
        "stickers" -> ToolDisplayInfo(id, stringResource(R.string.tool_stickers), Icons.Default.Star, Color(0xFFF59E0B))
        "shapes" -> ToolDisplayInfo(id, stringResource(R.string.tool_shapes), Icons.Default.Category, Color(0xFF8B5CF6))
        "eraser" -> ToolDisplayInfo(id, stringResource(R.string.tool_eraser), Icons.Default.AutoFixNormal, Color(0xFFEC4899))
        "laser" -> ToolDisplayInfo(id, stringResource(R.string.tool_laser), Icons.Default.Highlight, Color(0xFFEAB308))
        "geometry" -> ToolDisplayInfo(id, stringResource(R.string.tool_geometry), Icons.Default.ChangeHistory, Color(0xFF10B981))
        "tools" -> ToolDisplayInfo(id, stringResource(R.string.tool_tools), Icons.Default.Widgets, Color(0xFF8B5CF6))
        "undo" -> ToolDisplayInfo(id, stringResource(R.string.tool_undo), Icons.Default.Undo, Color(0xFFF59E0B))
        "redo" -> ToolDisplayInfo(id, stringResource(R.string.tool_redo), Icons.Default.Redo, Color(0xFFF97316))
        "save" -> ToolDisplayInfo(id, stringResource(R.string.tool_save), Icons.Default.Save, Color(0xFF10B981))
        "text" -> ToolDisplayInfo(id, stringResource(R.string.util_text), Icons.Default.TextFields, Color(0xFF2563EB))
        "image" -> ToolDisplayInfo(id, stringResource(R.string.util_image), Icons.Default.Image, Color(0xFF10B981))
        "clear" -> ToolDisplayInfo(id, stringResource(R.string.clear_all), Icons.Default.LayersClear, Color(0xFFEF4444))
        "background" -> ToolDisplayInfo(id, stringResource(R.string.popover_background), Icons.Default.GridOn, Color(0xFF6366F1))
        else -> ToolDisplayInfo(id, id.replaceFirstChar { it.uppercase() }, Icons.Default.Widgets, Color(0xFF64748B))
    }
}

fun getToolName(context: android.content.Context, id: String): String {
    return when (id) {
        "select" -> context.getString(R.string.tool_select)
        "pen" -> context.getString(R.string.tool_pen)
        "color_swap" -> context.getString(R.string.tool_color_swap)
        "flowchart" -> context.getString(R.string.tool_flowchart)
        "stickers" -> context.getString(R.string.tool_stickers)
        "shapes" -> context.getString(R.string.tool_shapes)
        "eraser" -> context.getString(R.string.tool_eraser)
        "laser" -> context.getString(R.string.tool_laser)
        "geometry" -> context.getString(R.string.tool_geometry)
        "tools" -> context.getString(R.string.tool_tools)
        "undo" -> context.getString(R.string.tool_undo)
        "redo" -> context.getString(R.string.tool_redo)
        "save" -> context.getString(R.string.tool_save)
        "text" -> context.getString(R.string.util_text)
        "image" -> context.getString(R.string.util_image)
        "clear" -> context.getString(R.string.clear_all)
        "background" -> context.getString(R.string.popover_background)
        else -> id.replaceFirstChar { it.uppercase() }
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
        color = if (isDark) Color(0xFF1E293B) else DockBackgroundColor,
        border = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else DockBorder,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Color(0xFF2563EB),
    swatch: Int? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalDarkTheme.current,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                tint = if (active) Color.White else if (enabled) tint else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
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
            color = if (!enabled) (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)) else if (active) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1E293B)) else (if (isDark) Color(0xFFCBD5E1) else DockLabel),
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun PenSplitDockItem(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalDarkTheme.current,
    onPenClick: () -> Unit,
    onDropdownClick: () -> Unit
) {
    val activeColor = Color(0xFF00BCD4)
    val activeContentColor = Color.White
    val inactiveIconColor = Color(0xFF2563EB)
    val inactiveTextColor = if (isDark) Color(0xFFCBD5E1) else DockLabel

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) activeColor else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Pen Tool Button (Activates Pen when tapped)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onPenClick)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = label,
                    tint = if (active) activeContentColor else inactiveIconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    color = if (active) activeContentColor else inactiveTextColor,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        // Middle: Vertical Divider Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .width(1.dp)
                .background(
                    if (active) Color.White.copy(alpha = 0.5f)
                    else (if (isDark) Color(0x33CBD5E1) else Color(0x3394A3B8))
                )
        )

        // Right: Dropdown Arrow (Opens/toggles the Settings panel when tapped)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onDropdownClick)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Pen settings",
                tint = if (active) activeContentColor else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DockPopover(
    title: String? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = 340.dp,
    scrollable: Boolean = true,
    isDark: Boolean = LocalDarkTheme.current,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
            .padding(bottom = 6.dp)
            .widthIn(min = 200.dp, max = maxWidth)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .heightIn(max = 380.dp)
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                    )
                    if (onClose != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close window",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                )
                Spacer(Modifier.height(8.dp))
            }
            if (scrollable) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

private data class ToolTile(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun ToolCategoryTabs(
    labels: List<String>,
    selected: Int,
    isDark: Boolean = LocalDarkTheme.current,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF3F4F6))
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) (if (isDark) Color.White else Color(0xFF111827)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ToolIconGrid(tiles: List<ToolTile>) {
    val columns = 5
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tiles.chunked(columns).forEach { row ->
            Row {
                row.forEach { tile ->
                    ToolIconTile(tile, Modifier.weight(1f))
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ToolIconTile(tile: ToolTile, modifier: Modifier = Modifier, isDark: Boolean = LocalDarkTheme.current) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (tile.selected) (if (isDark) Color(0xFF334155) else Color(0xFFEEF2FF)) else Color.Transparent)
            .clickable(onClick = tile.onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tile.tint.copy(alpha = if (isDark) 0.22f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tile.icon,
                contentDescription = tile.label,
                tint = tile.tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tile.label,
            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun PopoverRow(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    isDark: Boolean = LocalDarkTheme.current,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) (if (isDark) Color(0xFF334155) else Color(0xFFEEF2FF)) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937), fontSize = 13.sp)
    }
}



@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PageThumbnail(
    index: Int,
    title: String,
    strokes: List<Stroke>,
    backgroundKey: String = CanvasBackground.DOTTED.key,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit = {},
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
                when (backgroundKey) {
                    CanvasBackground.DARK.key -> {
                        drawRect(Color(0xFF171717))
                    }
                    CanvasBackground.FOOTBALL.key -> {
                        drawRect(Color(0xFF2E7D32))
                        val linePaint = Color.White.copy(alpha = 0.5f)
                        val pad = 4f
                        val pitchW = size.width - pad * 2
                        val pitchH = size.height - pad * 2
                        drawRect(linePaint, topLeft = Offset(pad, pad), size = Size(pitchW, pitchH), style = ThumbStroke(1f))
                        drawLine(linePaint, Offset(size.width / 2f, pad), Offset(size.width / 2f, size.height - pad), strokeWidth = 1f)
                        drawCircle(linePaint, radius = 5f, center = Offset(size.width / 2f, size.height / 2f), style = ThumbStroke(1f))
                        val boxW = pitchW * 0.18f
                        val boxH = pitchH * 0.5f
                        val boxY = (size.height - boxH) / 2f
                        drawRect(linePaint, topLeft = Offset(pad, boxY), size = Size(boxW, boxH), style = ThumbStroke(1f))
                        drawRect(linePaint, topLeft = Offset(size.width - pad - boxW, boxY), size = Size(boxW, boxH), style = ThumbStroke(1f))
                    }
                    CanvasBackground.GRAPH.key -> {
                        drawRect(Color(0xFFF7FBFF))
                        val gridColor = Color(0xFFD3E3F4).copy(alpha = 0.7f)
                        val step = 6f
                        var x = step
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
                            x += step
                        }
                        var y = step
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                            y += step
                        }
                    }
                    CanvasBackground.GRID.key -> {
                        drawRect(Color.White)
                        val gridColor = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                        val step = 8f
                        var x = step
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
                            x += step
                        }
                        var y = step
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                            y += step
                        }
                    }
                    CanvasBackground.LINED.key -> {
                        drawRect(Color.White)
                        val lineColor = Color(0xFFCBD5E1)
                        val step = 8f
                        var y = step
                        while (y < size.height) {
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                            y += step
                        }
                    }
                    CanvasBackground.DOTTED.key -> {
                        drawRect(Color.White)
                        val dotColor = Color(0xFF94A3B8)
                        val step = 10f
                        var x = step / 2f
                        while (x < size.width) {
                            var y = step / 2f
                            while (y < size.height) {
                                drawCircle(dotColor, radius = 0.75f, center = Offset(x, y))
                                y += step
                            }
                            x += step
                        }
                    }
                    CanvasBackground.CUSTOM.key -> {
                        drawRect(Color(0xFFE2E8F0))
                    }
                    else -> {
                        drawRect(if (dark) Color(0xFF171717) else Color.White)
                    }
                }
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
                    val strokeColor = if (!dark && CanvasInkTheme.isLightInk(stroke.colorValue)) {
                        Color(CanvasInkTheme.lightSurfaceInkArgb)
                    } else if (dark && CanvasInkTheme.isDarkInk(stroke.colorValue)) {
                        Color(CanvasInkTheme.darkSurfaceInkArgb)
                    } else {
                        Color(stroke.colorValue)
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = ThumbStroke(width = 1.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onOptionsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Page options",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
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
    documentOpen: Boolean,
    pageCount: Int,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var draft by remember(title) { mutableStateOf(title) }
    val canDelete = !documentOpen && pageCount > 1
    val canDuplicate = !documentOpen && pageCount < 50

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(min = 320.dp, max = 540.dp)
        ) {
            val isCompact = maxWidth < 400.dp
            val isUltraNarrow = maxWidth < 320.dp
            val cardHorizontalPadding = if (isCompact) 18.dp else 24.dp
            val buttonPadding = if (isCompact) PaddingValues(horizontal = 10.dp, vertical = 6.dp) else PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            val savePadding = if (isCompact) PaddingValues(horizontal = 16.dp, vertical = 6.dp) else PaddingValues(horizontal = 22.dp, vertical = 8.dp)
            val cancelPadding = if (isCompact) PaddingValues(horizontal = 8.dp, vertical = 6.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            val buttonSpacing = if (isCompact) 6.dp else 10.dp
            val fontSize = if (isCompact) 12.5.sp else 14.sp
            val iconSize = if (isCompact) 16.dp else 18.dp

            val deleteBtn = @Composable {
                OutlinedButton(
                    onClick = {
                        if (canDelete) {
                            onDelete()
                            onDismiss()
                        }
                    },
                    enabled = canDelete,
                    shape = CircleShape,
                    border = BorderStroke(
                        1.5.dp,
                        if (canDelete) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFEF4444),
                        disabledContentColor = Color(0xFFEF4444).copy(alpha = 0.35f)
                    ),
                    contentPadding = buttonPadding,
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(iconSize),
                        tint = if (canDelete) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        color = if (canDelete) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.35f),
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val duplicateBtn = @Composable {
                OutlinedButton(
                    onClick = {
                        if (canDuplicate) {
                            onDuplicate()
                            onDismiss()
                        }
                    },
                    enabled = canDuplicate,
                    shape = CircleShape,
                    border = BorderStroke(
                        1.5.dp,
                        if (canDuplicate) Color(0xFF405C6B) else Color(0xFF405C6B).copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.35f)
                    ),
                    contentPadding = buttonPadding,
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate",
                        modifier = Modifier.size(iconSize),
                        tint = if (canDuplicate) Color.White else Color.White.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Duplicate",
                        color = if (canDuplicate) Color.White else Color.White.copy(alpha = 0.35f),
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val cancelBtn = @Composable {
                TextButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    ),
                    contentPadding = cancelPadding,
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val saveBtn = @Composable {
                Button(
                    onClick = {
                        onRename(draft)
                        onDismiss()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF50E8A7),
                        contentColor = Color(0xFF0F1E19)
                    ),
                    contentPadding = savePadding,
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Save",
                        color = Color(0xFF0F1E19),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F1E22),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardHorizontalPadding, vertical = 22.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Page ${pageIndex + 1} Actions",
                        color = Color.White,
                        fontSize = if (isCompact) 20.sp else 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (documentOpen) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "This board is tied to an imported PDF document. Pages cannot be added, duplicated, or deleted.",
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (pageCount <= 1) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "The last remaining page cannot be deleted.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = {
                            Text(
                                text = "Page Title",
                                color = Color(0xFF8E9EA5),
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF426372),
                            unfocusedBorderColor = Color(0xFF2B414B),
                            focusedLabelColor = Color(0xFF8E9EA5),
                            unfocusedLabelColor = Color(0xFF8E9EA5),
                            cursorColor = Color(0xFF50E8A7)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onRename(draft)
                            onDismiss()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isUltraNarrow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                        ) {
                            deleteBtn()
                            duplicateBtn()
                            cancelBtn()
                            saveBtn()
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                            ) {
                                deleteBtn()
                                duplicateBtn()
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                            ) {
                                cancelBtn()
                                saveBtn()
                            }
                        }
                    }
                }
            }
        }
    }
}
