package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avitrisphare.whiteboard.core.theme.BrandOrange
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ColorWheelPanel(
    initialColor: Int,
    isDark: Boolean = false,
    title: String = "Color Palette",
    onColorSelected: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
            .width(260.dp)
            .heightIn(max = 280.dp)
    ) {
        ColorWheelCard(
            initialColor = initialColor,
            isDark = isDark,
            title = title,
            onColorSelected = onColorSelected,
            onClose = onClose
        )
    }
}

@Composable
fun ColorWheelCard(
    initialColor: Int,
    isDark: Boolean = false,
    title: String = "Color Palette",
    onColorSelected: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(initialColor) {
        FloatArray(3).apply {
            val safeColor = if (initialColor == 0) 0xFF3B82F6.toInt() else initialColor
            AndroidColor.colorToHSV(safeColor, this)
        }
    }

    var currentHue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var currentSat by remember(initialColor) { mutableFloatStateOf(hsv[1]) }
    var currentVal by remember(initialColor) { mutableFloatStateOf(if (hsv[2] <= 0.02f) 1f else hsv[2]) }
    var pickedColor by remember(initialColor) {
        mutableIntStateOf(if (initialColor == 0) 0xFF3B82F6.toInt() else initialColor)
    }

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Circular Color Wheel with selection indicator
        CircularColorWheel(
            selectedHue = currentHue,
            selectedSat = currentSat,
            brightness = currentVal,
            modifier = Modifier.size(130.dp),
            onColorPick = { hue, sat, col ->
                currentHue = hue
                currentSat = sat
                pickedColor = col
                onColorSelected(col)
            }
        )

        Spacer(Modifier.height(6.dp))

        // Brightness Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Brightness",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Text(
                text = "${(currentVal * 100).roundToInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            )
        }

        Slider(
            value = currentVal,
            onValueChange = { v ->
                currentVal = v
                val newCol = Color.hsv(currentHue, currentSat, currentVal).toArgb()
                pickedColor = newCol
                onColorSelected(newCol)
            },
            valueRange = 0.05f..1f,
            colors = SliderDefaults.colors(
                thumbColor = BrandOrange,
                activeTrackColor = BrandOrange,
                inactiveTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Quick Palette presets (including Black and White)
        val quickPresets = listOf(
            0xFF000000.toInt(), // Black
            0xFFFFFFFF.toInt(), // White
            0xFFEF4444.toInt(), // Red
            0xFFF97316.toInt(), // Orange
            0xFFEAB308.toInt(), // Yellow
            0xFF22C55E.toInt(), // Green
            0xFF06B6D4.toInt(), // Cyan
            0xFF3B82F6.toInt(), // Blue
            0xFFA855F7.toInt()  // Purple
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickPresets.forEach { preset ->
                val isPresetSelected = pickedColor == preset
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(preset))
                        .border(
                            width = if (isPresetSelected) 2.dp else 1.dp,
                            color = if (isPresetSelected) BrandOrange else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                            shape = CircleShape
                        )
                        .clickable {
                            val presetHsv = FloatArray(3)
                            AndroidColor.colorToHSV(preset, presetHsv)
                            currentHue = presetHsv[0]
                            currentSat = presetHsv[1]
                            currentVal = if (presetHsv[2] <= 0.02f) 0.05f else presetHsv[2]
                            pickedColor = preset
                            onColorSelected(preset)
                        }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Bottom preview pill with color swatch, hex code, and Done button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(pickedColor))
                        .border(1.5.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), CircleShape)
                )
                Text(
                    text = String.format("#%06X", pickedColor and 0xFFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandOrange)
                    .clickable {
                        onColorSelected(pickedColor)
                        onClose()
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ColorWheelDialog(
    initialColor: Int,
    isDark: Boolean = false,
    title: String = "Color Palette",
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ColorWheelPanel(
            initialColor = initialColor,
            isDark = isDark,
            title = title,
            onColorSelected = onColorSelected,
            onClose = onDismiss
        )
    }
}

@Composable
fun CircularColorWheel(
    selectedHue: Float,
    selectedSat: Float,
    brightness: Float,
    modifier: Modifier = Modifier,
    onColorPick: (hue: Float, sat: Float, color: Int) -> Unit
) {
    val hueColors = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )
    }

    Box(
        modifier = modifier
            .pointerInput(brightness) {
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
                    val sat = (distance / radius).coerceIn(0f, 1f)
                    val col = Color.hsv(angle.toFloat(), sat, brightness.coerceIn(0f, 1f)).toArgb()
                    onColorPick(angle.toFloat(), sat, col)
                }
            }
            .pointerInput(brightness) {
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
                    val sat = (distance / radius).coerceIn(0f, 1f)
                    val col = Color.hsv(angle.toFloat(), sat, brightness.coerceIn(0f, 1f)).toArgb()
                    onColorPick(angle.toFloat(), sat, col)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(cx, cy)

            // Draw full color wheel
            drawCircle(brush = Brush.sweepGradient(hueColors), radius = radius, center = Offset(cx, cy))
            drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), radius = radius), radius = radius, center = Offset(cx, cy))

            // Darken based on brightness
            if (brightness < 1f) {
                drawCircle(color = Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 1f)), radius = radius, center = Offset(cx, cy))
            }

            // Draw outer border ring
            drawCircle(color = Color(0x33000000), radius = radius, center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))

            // Draw thumb indicator for currently selected hue & saturation
            if (radius > 0f) {
                val rad = Math.toRadians(selectedHue.toDouble())
                val dist = (selectedSat * radius).coerceIn(0f, radius)
                val thumbX = cx + (dist * cos(rad)).toFloat()
                val thumbY = cy + (dist * sin(rad)).toFloat()

                // Outer white ring
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Inner dark ring for contrast
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = 5.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}
