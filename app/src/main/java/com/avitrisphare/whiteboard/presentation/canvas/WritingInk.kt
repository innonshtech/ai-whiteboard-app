package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    val path = points.toSmoothedPath()
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
@Composable
fun InkToolPanel(state: CanvasState, viewModel: CanvasViewModel) {
    var showSmartWrite by remember { mutableStateOf(false) }
    var editingSecondary by remember { mutableStateOf(false) }
    val currentColor = if (editingSecondary) state.secondaryColor else state.selectedColor
    var hex by remember(currentColor) {
        mutableStateOf(String.format("%06X", currentColor and 0xFFFFFF))
    }
    var red by remember(currentColor) { mutableIntStateOf((currentColor shr 16) and 0xFF) }
    var green by remember(currentColor) { mutableIntStateOf((currentColor shr 8) and 0xFF) }
    var blue by remember(currentColor) { mutableIntStateOf(currentColor and 0xFF) }

    fun applyColor(c: Int) {
        if (editingSecondary) {
            viewModel.setSecondaryColor(c)
        } else {
            viewModel.setColor(c)
        }
    }

    Column(
        modifier = Modifier
            .width(300.dp)
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(state.selectedColor))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${state.inkTool.label} · ${state.strokeWidth.toInt()}px",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                WhiteboardTool.PEN,
                WhiteboardTool.PENCIL,
                WhiteboardTool.MARKER,
                WhiteboardTool.HIGHLIGHTER,
                WhiteboardTool.BRUSH
            ).forEach { tool ->
                val selected = state.inkTool == tool
                Text(
                    text = tool.label,
                    color = if (selected) Color.White else Color(0xFF1F2937),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF6366F1) else Color(0xFFF3F4F6))
                        .clickable { viewModel.setTool(tool) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val language = smartWriteLanguage(state.smartWriteLanguageTag)
        Text(
            text = if (state.smartWriteEnabled) {
                "Smart Write · ${language.label} · ${state.smartWriteIntervalMs / 1000f}s"
            } else {
                "Smart Write"
            },
            color = if (state.smartWriteEnabled) Color.White else Color(0xFF1F2937),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (state.smartWriteEnabled) Color(0xFF2563EB) else Color(0xFFF3F4F6))
                .clickable { showSmartWrite = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
        Text(
            "Writes handwriting into editable text after you pause.",
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp)
        )
        if (showSmartWrite) {
            SmartWriteLanguageDialog(
                state = state,
                onLanguage = { viewModel.setSmartWriteLanguage(it) },
                onInterval = { viewModel.setSmartWriteInterval(it) },
                onEnabled = { viewModel.setSmartWriteEnabled(it) },
                onDismiss = { showSmartWrite = false }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Size", fontSize = 12.sp, color = Color(0xFF6B7280))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "−",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.nudgeStrokeWidth(-2f) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = state.strokeWidth.coerceIn(1f, 50f),
                onValueChange = { viewModel.setStrokeWidth(it) },
                valueRange = 1f..50f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6366F1),
                    activeTrackColor = Color(0xFF6366F1)
                )
            )
            Text(
                "+",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.nudgeStrokeWidth(2f) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("Two-colour pen (One-tap swap)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Primary Swatch
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!editingSecondary) Color(0xFFEEF2FF) else Color(0xFFF9FAFB))
                    .clickable { editingSecondary = false }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(state.selectedColor))
                        .then(
                            if (!editingSecondary) Modifier.border(2.5.dp, Color(0xFF6366F1), CircleShape)
                            else Modifier.border(1.dp, Color(0xFFD1D5DB), CircleShape)
                        )
                )
                Text("Primary (Active)", fontSize = 10.sp, fontWeight = if (!editingSecondary) FontWeight.Bold else FontWeight.Normal, color = if (!editingSecondary) Color(0xFF4F46E5) else Color(0xFF6B7280))
            }

            // Swap Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEEF2FF))
                    .clickable { viewModel.swapPenColors() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap colors", tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                Text("Swap", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
            }

            // Secondary Swatch
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (editingSecondary) Color(0xFFEEF2FF) else Color(0xFFF9FAFB))
                    .clickable { editingSecondary = true }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(state.secondaryColor))
                        .then(
                            if (editingSecondary) Modifier.border(2.5.dp, Color(0xFF6366F1), CircleShape)
                            else Modifier.border(1.dp, Color(0xFFD1D5DB), CircleShape)
                        )
                )
                Text("Secondary", fontSize = 10.sp, fontWeight = if (editingSecondary) FontWeight.Bold else FontWeight.Normal, color = if (editingSecondary) Color(0xFF4F46E5) else Color(0xFF6B7280))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (editingSecondary) "Palette sets: Secondary Color" else "Palette sets: Primary (Active) Color",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4F46E5)
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            INK_PRESETS.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { applyColor(color) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HueWheel(onPick = { applyColor(it) })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = { value ->
                val cleaned = value.removePrefix("#").take(6).filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                hex = cleaned
                if (cleaned.length == 6) {
                    cleaned.toIntOrNull(16)?.let { rgb ->
                        applyColor(rgb or 0xFF000000.toInt())
                    }
                }
            },
            singleLine = true,
            label = { Text(if (editingSecondary) "Secondary Hex #RRGGBB" else "Primary Hex #RRGGBB") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text("Ink quality (mm)", fontSize = 12.sp, color = Color(0xFF6B7280))
        Slider(
            value = state.inkSampleGapMm,
            onValueChange = { viewModel.setInkSampleGapMm(it) },
            valueRange = 0.15f..1.2f,
            modifier = Modifier.fillMaxWidth()
        )
        RgbSlider("R", red, Color(0xFFEF4444)) {
            red = it
            viewModel.setColor(Color(it, green, blue).toArgb())
        }
        RgbSlider("G", green, Color(0xFF22C55E)) {
            green = it
            viewModel.setColor(Color(red, it, blue).toArgb())
        }
        RgbSlider("B", blue, Color(0xFF3B82F6)) {
            blue = it
            viewModel.setColor(Color(red, green, it).toArgb())
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
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C313C))
                .padding(16.dp)
                .width(360.dp)
        ) {
            Text("Select Language", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SMART_WRITE_LANGUAGES.forEach { language ->
                    val selected = state.smartWriteLanguageTag == language.tag
                    Text(
                        text = language.label,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF1D4ED8) else Color(0xFF3A4150))
                            .clickable { onLanguage(language.tag) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Smart Write Interval", color = Color.White, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(500L to "0.5s", 1000L to "1s", 1500L to "1.5s").forEach { (millis, label) ->
                    RadioButton(
                        selected = state.smartWriteIntervalMs == millis,
                        onClick = { onInterval(millis) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF22C55E),
                            unselectedColor = Color(0xFF9CA3AF)
                        )
                    )
                    Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                }
            }
            Text(
                "English, Hindi, and Marathi are ready on this board.",
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (state.smartWriteEnabled) "Turn off" else "Turn on",
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onEnabled(!state.smartWriteEnabled)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EraserToolPanel(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onClearPage: () -> Unit
) {
    Column(modifier = Modifier.width(260.dp)) {
        Text("Eraser", fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                EraserMode.STANDARD to "Standard",
                EraserMode.SMART to "Smart"
            ).forEach { (mode, label) ->
                val selected = state.eraserMode == mode
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFF1F2937),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF6366F1) else Color(0xFFF3F4F6))
                        .clickable { viewModel.setEraserMode(mode) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (state.eraserMode == EraserMode.STANDARD) {
                "Erases the ink inside the circle."
            } else {
                "Erases each whole stroke or object you touch."
            },
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Palm eraser",
            fontSize = 12.sp,
            color = if (state.palmEraserEnabled) Color(0xFF2563EB) else Color(0xFF6B7280),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.setPalmEraserEnabled(!state.palmEraserEnabled) }
                .padding(vertical = 4.dp)
        )
        Text(
            "Selection eraser only",
            fontSize = 12.sp,
            color = if (state.restrictEraserToSelection) Color(0xFF2563EB) else Color(0xFF6B7280),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.setRestrictEraserToSelection(!state.restrictEraserToSelection) }
                .padding(vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Size ${state.eraserSize.toInt()}px", fontSize = 12.sp, color = Color(0xFF6B7280))
        Slider(
            value = state.eraserSize.coerceIn(8f, 80f),
            onValueChange = { viewModel.setEraserSize(it) },
            valueRange = 8f..80f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6366F1),
                activeTrackColor = Color(0xFF6366F1)
            )
        )
        Text(
            text = "Clear all",
            color = Color(0xFFB91C1C),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClearPage)
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun RgbSlider(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(16.dp), fontSize = 12.sp, color = color)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
        Text(value.toString(), modifier = Modifier.width(28.dp), fontSize = 11.sp)
    }
}

@Composable
private fun HueWheel(onPick: (Int) -> Unit) {
    val hueColors = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )
    Box(
        modifier = Modifier
            .size(150.dp)
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
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = Brush.sweepGradient(hueColors))
            drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent)))
        }
    }
}
