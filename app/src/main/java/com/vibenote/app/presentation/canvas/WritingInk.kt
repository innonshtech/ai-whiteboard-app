package com.vibenote.app.presentation.canvas

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

fun PointerInputChange.inkPressure(): Float {
    val raw = pressure
    if (raw.isNaN() || raw <= 0.05f) return 1f
    return raw.coerceIn(0.72f, 1f)
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
    var hex by remember(state.selectedColor) {
        mutableStateOf(String.format("%06X", state.selectedColor and 0xFFFFFF))
    }
    var red by remember(state.selectedColor) { mutableIntStateOf((state.selectedColor shr 16) and 0xFF) }
    var green by remember(state.selectedColor) { mutableIntStateOf((state.selectedColor shr 8) and 0xFF) }
    var blue by remember(state.selectedColor) { mutableIntStateOf(state.selectedColor and 0xFF) }

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
        Text("Size", fontSize = 12.sp, color = Color(0xFF6B7280))
        Slider(
            value = state.strokeWidth.coerceIn(1f, 50f),
            onValueChange = { viewModel.setStrokeWidth(it) },
            valueRange = 1f..50f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6366F1),
                activeTrackColor = Color(0xFF6366F1)
            )
        )
        Text("Colour", fontSize = 12.sp, color = Color(0xFF6B7280))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            INK_PRESETS.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { viewModel.setColor(color) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HueWheel(onPick = { viewModel.setColor(it) })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = { value ->
                val cleaned = value.removePrefix("#").take(6).filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                hex = cleaned
                if (cleaned.length == 6) {
                    cleaned.toIntOrNull(16)?.let { rgb ->
                        viewModel.setColor(rgb or 0xFF000000.toInt())
                    }
                }
            },
            singleLine = true,
            label = { Text("Hex #RRGGBB") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
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
