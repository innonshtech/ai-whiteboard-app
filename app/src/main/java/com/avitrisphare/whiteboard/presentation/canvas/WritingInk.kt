package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import com.avitrisphare.whiteboard.core.theme.BrandOrange

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Brush as BrushToolIcon
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as InkStroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

fun PointerInputChange.inkPressure(): Float {
    val raw = pressure
    if (raw.isNaN() || raw <= 0.05f) return 1f
    return raw.coerceIn(0.72f, 1f)
}

/** Half the on-canvas thickness of a stroke, in world units. */
fun strokeInkHalfWidth(stroke: Stroke): Float {
    val multiplier = when (stroke.strokeType) {
        StrokeType.PENCIL -> 1f
        StrokeType.MARKER -> 1.7f
        StrokeType.HIGHLIGHTER -> 2.4f
        StrokeType.BRUSH -> 1.25f
        else -> if (stroke.isHighlighter) 3f else 1f
    }
    return stroke.strokeWidth * multiplier / 2f
}

/**
 * In-progress ink. The path grows by one segment per point so a long stroke
 * does not rebuild its whole curve on every touch sample.
 */
class LiveInk {
    var stroke: Stroke? = null
    val path = Path()
    private var pending: Offset? = null
    private var tip: Offset? = null
    var predicted: Offset? = null

    fun begin(start: Offset) {
        path.reset()
        path.moveTo(start.x, start.y)
        pending = null
        tip = start
        predicted = null
    }

    fun extend(point: Offset) {
        val control = pending
        if (control == null) {
            pending = point
            return
        }
        val midX = (control.x + point.x) / 2f
        val midY = (control.y + point.y) / 2f
        path.quadraticBezierTo(control.x, control.y, midX, midY)
        tip = Offset(midX, midY)
        pending = point
    }

    fun tail(): Offset? = pending
    fun tipPoint(): Offset? = tip
}

fun DrawScope.drawWritingStroke(stroke: Stroke, screenScale: Float) {
    val points = stroke.points
    if (points.isEmpty()) return
    val safeScale = if (screenScale <= 0f) 1f else screenScale
    val style = inkLook(stroke.strokeType)
    val width = (stroke.strokeWidth * style.widthMul / safeScale).coerceAtLeast(1.5f / safeScale)
    val color = Color(stroke.colorValue).copy(alpha = style.alpha)
    val round = InkStroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

    if (points.size == 1) {
        drawCircle(color = color, radius = width / 2f, center = points[0])
        return
    }

    val path = stroke.cachedPath
    drawPath(path = path, color = color, style = round)

    if (style.brush && stroke.pointPressures.size == points.size) {
        val average = stroke.pointPressures.average().toFloat().coerceIn(0.72f, 1f)
        drawPath(
            path = path,
            color = color.copy(alpha = style.alpha * 0.45f),
            style = InkStroke(
                width = width * (0.55f + average),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

fun DrawScope.drawLiveWritingStroke(live: LiveInk, screenScale: Float) {
    val stroke = live.stroke ?: return
    val points = stroke.points
    if (points.isEmpty()) return
    val safeScale = if (screenScale <= 0f) 1f else screenScale
    val style = inkLook(stroke.strokeType)
    val width = (stroke.strokeWidth * style.widthMul / safeScale).coerceAtLeast(1.5f / safeScale)
    val color = Color(stroke.colorValue).copy(alpha = style.alpha)
    val round = InkStroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (points.size == 1) {
        drawCircle(color = color, radius = width / 2f, center = points[0])
        return
    }
    drawPath(path = live.path, color = color, style = round)
    val tip = live.tipPoint()
    val tail = live.tail()
    if (tip != null && tail != null) {
        drawLine(color = color, start = tip, end = tail, strokeWidth = width, cap = StrokeCap.Round)
    }
    val predicted = live.predicted
    if (tail != null && predicted != null) {
        drawLine(
            color = color.copy(alpha = style.alpha * 0.45f),
            start = tail,
            end = predicted,
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}

private data class InkLook(val alpha: Float, val widthMul: Float, val brush: Boolean)

private fun inkLook(type: StrokeType): InkLook = when (type) {
    StrokeType.PENCIL -> InkLook(1f, 1f, brush = false)
    StrokeType.MARKER -> InkLook(0.72f, 1.7f, brush = false)
    StrokeType.HIGHLIGHTER -> InkLook(0.4f, 2.4f, brush = false)
    StrokeType.BRUSH -> InkLook(1f, 1.25f, brush = true)
    else -> InkLook(1f, 1f, brush = false)
}

@OptIn(ExperimentalLayoutApi::class)
fun writingToolIcon(tool: WhiteboardTool): ImageVector = when (tool) {
    WhiteboardTool.PEN -> Icons.Default.Draw
    WhiteboardTool.MATH_PEN -> Icons.Default.Functions
    WhiteboardTool.PENCIL -> Icons.Default.Edit
    WhiteboardTool.MARKER -> Icons.Default.BorderColor
    WhiteboardTool.HIGHLIGHTER -> Icons.Default.Highlight
    WhiteboardTool.BRUSH -> Icons.Default.BrushToolIcon
    WhiteboardTool.LASSO -> Icons.Default.Gesture
    else -> Icons.Default.Draw
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InkToolPanel(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onClose: () -> Unit = {}
) {
    var editingSecondary by remember(state.useSecondaryPenColor) { mutableStateOf(state.useSecondaryPenColor) }
    var showColorWheel by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentColor = if (editingSecondary) state.secondaryColor else state.selectedColor
    val currentTipTool = if (editingSecondary) state.secondaryInkTool else state.inkTool
    val currentWidth = if (editingSecondary) state.secondaryStrokeWidth else state.strokeWidth

    fun applyColor(c: Int) {
        if (editingSecondary) {
            viewModel.setSecondaryColor(c)
        } else {
            viewModel.setColor(c)
        }
    }

    if (showLanguageDialog) {
        SmartWriteLanguageDialog(
            state = state,
            onLanguage = { viewModel.setSmartWriteLanguage(it) },
            onInterval = { viewModel.setSmartWriteInterval(it) },
            onEnabled = { viewModel.setSmartWriteEnabled(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .width(352.dp)
            .heightIn(max = 540.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        // 1. Header: Color Dot, Utensil · Size, Status Badge (DRAWING MODE), Close [X]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(currentColor))
                        .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                val displayToolLabel = when (currentTipTool) {
                    WhiteboardTool.LASSO -> stringResource(R.string.util_lasso)
                    else -> currentTipTool.label
                }
                val specText = if (currentTipTool == WhiteboardTool.LASSO) {
                    "Select"
                } else {
                    "${currentWidth.toInt()}.0px"
                }
                Text(
                    text = "$displayToolLabel · $specText",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFDCFCE7))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DRAWING MODE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. DRAWING UTENSILS Section Header + Utensil Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DRAWING UTENSILS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.6.sp
            )
            Text(
                text = "Preset: ${currentTipTool.label}",
                fontSize = 9.5.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val utensils = listOf(
                WhiteboardTool.PEN to Icons.Default.Draw,
                WhiteboardTool.MATH_PEN to Icons.Default.Functions,
                WhiteboardTool.MARKER to Icons.Default.BorderColor,
                WhiteboardTool.PENCIL to Icons.Default.Edit,
                WhiteboardTool.HIGHLIGHTER to Icons.Default.Highlight,
                WhiteboardTool.BRUSH to Icons.Default.BrushToolIcon,
                WhiteboardTool.LASSO to Icons.Default.Gesture
            )
            utensils.forEach { (tool, icon) ->
                val isSelected = currentTipTool == tool || (tool == WhiteboardTool.LASSO && state.currentTool == WhiteboardTool.LASSO)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BrandOrange else Color(0xFFF8FAFC))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) BrandOrange else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (editingSecondary) {
                                viewModel.setSecondaryInkTool(tool)
                            } else {
                                viewModel.setTool(tool)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tool.label,
                        tint = if (isSelected) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3. Intelligent Canvas Assist (Smart Write & Shape Snap)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAFAFA))
                .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Intelligent Canvas Assist",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Smart Write Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.smartWriteEnabled) Color(0xFFFFF7ED) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (state.smartWriteEnabled) BrandOrange else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!state.smartWriteEnabled) {
                                    viewModel.setSmartWriteEnabled(true)
                                }
                                showLanguageDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = if (state.smartWriteEnabled) BrandOrange else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Smart Write",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                val pauseSec = state.smartWriteIntervalMs / 1000f
                                Text(
                                    text = "${state.smartWriteLanguageTag.uppercase()} · ${pauseSec}s pause",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.clickable { showLanguageDialog = true }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.smartWriteEnabled) Color(0xFF10B981) else Color(0xFFCBD5E1))
                            )
                        }
                    }

                    // Shape Snap Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.isDrawToShapeEnabled) Color(0xFFFFF7ED) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (state.isDrawToShapeEnabled) BrandOrange else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.toggleDrawToShape()
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = if (state.isDrawToShapeEnabled) BrandOrange else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Shape Snap",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Auto-polygon",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(13.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isDrawToShapeEnabled) BrandOrange else Color(0xFFE2E8F0))
                                    .padding(horizontal = 2.dp),
                                contentAlignment = if (state.isDrawToShapeEnabled) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "ⓘ Vector ink converts to editable symbols and Bezier geometry",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4. STROKE WIDTH Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STROKE WIDTH",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.6.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(currentWidth.coerceIn(1.5f, 16f).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BrandOrange)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${currentWidth.toInt()}.0 px",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable {
                        if (editingSecondary) {
                            viewModel.setSecondaryStrokeWidth((state.secondaryStrokeWidth - 1f).coerceAtLeast(1f))
                        } else {
                            viewModel.nudgeStrokeWidth(-1f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }

            Slider(
                value = currentWidth.coerceIn(1f, 50f),
                onValueChange = {
                    if (editingSecondary) {
                        viewModel.setSecondaryStrokeWidth(it)
                    } else {
                        viewModel.setStrokeWidth(it)
                    }
                },
                valueRange = 1f..50f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                colors = SliderDefaults.colors(
                    thumbColor = BrandOrange,
                    activeTrackColor = BrandOrange,
                    inactiveTrackColor = Color(0xFFFFEDD5)
                )
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable {
                        if (editingSecondary) {
                            viewModel.setSecondaryStrokeWidth((state.secondaryStrokeWidth + 1f).coerceAtMost(50f))
                        } else {
                            viewModel.nudgeStrokeWidth(1f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val widthPresets = listOf(1f, 2f, 4f, 8f, 16f, 24f)
            widthPresets.forEach { w ->
                val isSelected = currentWidth.toInt() == w.toInt()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFFFF7ED) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) BrandOrange else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            if (editingSecondary) {
                                viewModel.setSecondaryStrokeWidth(w)
                            } else {
                                viewModel.setStrokeWidth(w)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${w.toInt()}px",
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandOrange else Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 5. Dual-Tone Quick Switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dual-Tone Quick Switch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("ⓘ", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    }
                    Text(
                        text = "Stylus Dual Tip",
                        fontSize = 9.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SLOT A
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!editingSecondary) Color(0xFFFFF7ED) else Color.White)
                            .border(
                                width = if (!editingSecondary) 1.5.dp else 1.dp,
                                color = if (!editingSecondary) BrandOrange else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                editingSecondary = false
                                viewModel.setActivePenSlot(1)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(state.selectedColor))
                                    .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "SLOT A",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!editingSecondary) BrandOrange else Color(0xFF475569)
                                )
                                Text(
                                    text = String.format("#%06X", state.selectedColor and 0xFFFFFF),
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Swap
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.swapPenColors()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap",
                                tint = BrandOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "Swap",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    // SLOT B
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (editingSecondary) Color(0xFFFFF7ED) else Color.White)
                            .border(
                                width = if (editingSecondary) 1.5.dp else 1.dp,
                                color = if (editingSecondary) BrandOrange else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                editingSecondary = true
                                viewModel.setActivePenSlot(2)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(state.secondaryColor))
                                    .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "SLOT B",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (editingSecondary) BrandOrange else Color(0xFF475569)
                                )
                                Text(
                                    text = String.format("#%06X", state.secondaryColor and 0xFFFFFF),
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 6. Active Color Palette Matrix (21 Core Hues)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Color Palette Matrix",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrandOrange
            )
            Text(
                text = "21 Core Hues",
                fontSize = 9.5.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(Modifier.height(6.dp))

        val coreHues = remember {
            listOf(
                // Row 1
                0xFF111827.toInt(), // Black
                0xFF374151.toInt(), // Dark Slate
                0xFF64748B.toInt(), // Slate Blue
                0xFFFFFFFF.toInt(), // White
                0xFFFEF3C7.toInt(), // Cream
                0xFF78716C.toInt(), // Taupe
                0xFF1E293B.toInt(), // Deep Navy
                // Row 2
                0xFFEF4444.toInt(), // Red
                0xFFF26A1B.toInt(), // Brand Orange
                0xFFF59E0B.toInt(), // Amber
                0xFF10B981.toInt(), // Emerald
                0xFF34D399.toInt(), // Mint
                0xFF06B6D4.toInt(), // Cyan
                0xFF0EA5E9.toInt(), // Sky Blue
                // Row 3
                0xFF3B82F6.toInt(), // Electric Blue
                0xFF6366F1.toInt(), // Indigo
                0xFFA855F7.toInt(), // Purple
                0xFFEC4899.toInt(), // Pink
                0xFFEA580C.toInt(), // Rust Orange
                0xFF65A30D.toInt(), // Olive Lime
                0xFF4C1D95.toInt()  // Deep Violet
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (col in 0 until 7) {
                        val colorVal = coreHues[row * 7 + col]
                        val isSelected = (currentColor and 0xFFFFFF) == (colorVal and 0xFFFFFF)
                        val isWhiteOrLight = colorVal == 0xFFFFFFFF.toInt() || colorVal == 0xFFFEF3C7.toInt()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 2.dp else if (isWhiteOrLight) 1.dp else 0.dp,
                                    color = if (isSelected) BrandOrange else if (isWhiteOrLight) Color(0xFFE2E8F0) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { applyColor(colorVal) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isWhiteOrLight) Color(0xFF1E293B) else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 7. Continuous Rainbow Hue Gradient Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val hue = fraction * 360f
                        applyColor(Color.hsv(hue, 1f, 1f).toArgb())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val hue = fraction * 360f
                        applyColor(Color.hsv(hue, 1f, 1f).toArgb())
                    }
                }
        )

        Spacer(Modifier.height(8.dp))

        // 8. Bottom Action Bar: Color Picker, Opacity, HEX code badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showColorWheel) Color(0xFFFFF7ED) else Color(0xFFF1F5F9))
                        .border(
                            width = 1.dp,
                            color = if (showColorWheel) BrandOrange else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { showColorWheel = !showColorWheel },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Color Wheel",
                        tint = if (showColorWheel) BrandOrange else Color(0xFF475569),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "💧 100%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                    .clickable { showColorWheel = !showColorWheel }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HEX ",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = String.format("#%06X", currentColor and 0xFFFFFF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        // 9. Expandable Color Wheel
        AnimatedVisibility(
            visible = showColorWheel,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Custom Color Wheel",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Spacer(Modifier.height(8.dp))
                HueWheel(
                    modifier = Modifier.size(160.dp),
                    onPick = { applyColor(it) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartWriteLanguageDialog(
    state: CanvasState,
    onLanguage: (String) -> Unit,
    onInterval: (Long) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFFEDD5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Write Settings",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Handwriting recognition & transcription",
                            color = Color(0xFF64748B),
                            fontSize = 11.5.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Enable / Disable Toggle Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.smartWriteEnabled) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                    .border(
                        width = 1.dp,
                        color = if (state.smartWriteEnabled) BrandOrange else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Smart Write",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (state.smartWriteEnabled) "Ink converts to digital text automatically" else "Recognition is currently off",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = state.smartWriteEnabled,
                        onCheckedChange = { onEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandOrange,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Section 1: Recognition Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "RECOGNITION LANGUAGE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.6.sp
                    )
                }
                Text(
                    text = "${SMART_WRITE_LANGUAGES.size} Available",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SMART_WRITE_LANGUAGES.forEach { language ->
                    val selected = state.smartWriteLanguageTag == language.tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) BrandOrange else Color(0xFFF8FAFC))
                            .border(
                                width = 1.dp,
                                color = if (selected) BrandOrange else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onLanguage(language.tag) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = language.label,
                                color = if (selected) Color.White else Color(0xFF334155),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Section 2: Recognition Latency / Interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECOGNITION LATENCY (PAUSE DURATION)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.6.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF7ED))
                        .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${state.smartWriteIntervalMs / 1000f}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Presets Row
            val intervals = listOf(
                500L to "0.5s · Fast",
                800L to "0.8s · Balanced",
                1000L to "1.0s · Steady",
                1500L to "1.5s · Relaxed"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                intervals.forEach { (millis, label) ->
                    val isSelected = state.smartWriteIntervalMs == millis
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) BrandOrange else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onInterval(millis) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BrandOrange else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Latency Slider
            Slider(
                value = (state.smartWriteIntervalMs).toFloat().coerceIn(400f, 2000f),
                onValueChange = { onInterval((it / 100).toLong() * 100L) },
                valueRange = 400f..2000f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = BrandOrange,
                    activeTrackColor = BrandOrange,
                    inactiveTrackColor = Color(0xFFFFEDD5)
                )
            )

            Spacer(Modifier.height(10.dp))

            // Info note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "💡 On-device recognition is supported for English, Hindi, and Marathi with zero network latency. Unsupported language strokes fall back automatically.",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Done button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandOrange)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}


@Composable
fun EraserToolPanel(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onClose: () -> Unit = {}
) {
    Column(modifier = Modifier.width(260.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.eraser_title), fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.dialog_close),
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                EraserMode.STANDARD to stringResource(R.string.eraser_standard),
                EraserMode.SMART to stringResource(R.string.eraser_smart)
            ).forEach { (mode, label) ->
                val selected = state.eraserMode == mode
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFF1F2937),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) BrandOrange else Color(0xFFF3F4F6))
                        .clickable { viewModel.setEraserMode(mode) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (state.eraserMode == EraserMode.STANDARD) {
                stringResource(R.string.eraser_standard_hint)
            } else {
                stringResource(R.string.eraser_smart_hint)
            },
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.eraser_size_px, state.eraserSize.toInt()), fontSize = 12.sp, color = Color(0xFF6B7280))
        Slider(
            value = state.eraserSize.coerceIn(8f, 80f),
            onValueChange = { viewModel.setEraserSize(it) },
            valueRange = 8f..80f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = BrandOrange,
                activeTrackColor = BrandOrange,
                inactiveTrackColor = Color(0xFFFFEDD5)
            )
        )
    }
}

@Composable
private fun HueWheel(
    modifier: Modifier = Modifier,
    onPick: (Int) -> Unit
) {
    val hueColors = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                detectTapGestures { offset ->
                    if (cx <= 0f || cy <= 0f) return@detectTapGestures
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val distance = sqrt(dx * dx + dy * dy)
                    val radius = min(cx, cy)
                    if (distance > radius) return@detectTapGestures
                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0
                    val saturation = (distance / radius).coerceIn(0f, 1f)
                    onPick(Color.hsv(angle.toFloat(), saturation, 1f).toArgb())
                }
            }
            .pointerInput(Unit) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                detectDragGestures { change, _ ->
                    change.consume()
                    val offset = change.position
                    if (cx <= 0f || cy <= 0f) return@detectDragGestures
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val distance = sqrt(dx * dx + dy * dy)
                    val radius = min(cx, cy)
                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0
                    val saturation = (distance / radius).coerceIn(0f, 1f)
                    onPick(Color.hsv(angle.toFloat(), saturation, 1f).toArgb())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = Brush.sweepGradient(hueColors))
            drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent)))
        }
    }
}
