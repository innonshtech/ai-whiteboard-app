package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.roundToInt

private val SHAPE_PRESET_COLORS = listOf(
    0xFFEF4444.toInt(), // Red
    0xFFF97316.toInt(), // Orange
    0xFFFBBF24.toInt(), // Yellow
    0xFF22C55E.toInt(), // Green
    0xFF3B82F6.toInt(), // Blue
    0xFF6366F1.toInt(), // Indigo
    0xFFA855F7.toInt(), // Purple
    0xFF1B1F2A.toInt(), // Black
    0xFFFFFFFF.toInt()  // White
)

private val TOP_CATEGORIES = listOf("Media", "Tools", "Shapes", "Widgets", "Templates")

@Composable
fun ShapeToolPanel(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTopTab by remember { mutableStateOf("Shapes") }
    var selectedCategory by remember { mutableStateOf(ShapeCategory.SHAPES_2D) }
    var showColorPicker by remember { mutableStateOf(false) }

    val activeSpec = remember(state.activeShapeType) {
        ALL_SHAPES.firstOrNull { it.strokeType == state.activeShapeType } ?: ALL_SHAPES.first()
    }

    val currentModeColor = if (state.shapeStyleMode == ShapeStyleMode.BORDER) {
        state.shapeBorderColor
    } else {
        state.shapeFillColor
    }

    Column(
        modifier = modifier
            .width(440.dp)
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // --- Top Category Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TOP_CATEGORIES.forEach { tab ->
                    val isSelected = tab == selectedTopTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
                            .clickable { selectedTopTab = tab }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF6B7280)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Sub-category Pills (2D Shapes, Lines & Rays, 3D Solids) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShapeCategory.values().forEach { cat ->
                val isCatSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCatSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6))
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.label,
                        fontSize = 12.sp,
                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCatSelected) Color.White else Color(0xFF374151),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Horizontal Shape Cards Grid ---
        val shapesInCat = remember(selectedCategory) {
            ALL_SHAPES.filter { it.category == selectedCategory }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            shapesInCat.forEach { spec ->
                val isSelected = spec.strokeType == state.activeShapeType
                Column(
                    modifier = Modifier
                        .size(width = 68.dp, height = 74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            viewModel.setShapeType(spec.strokeType)
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ShapeIconPreview(
                        spec = spec,
                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF374151)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = spec.name,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF4B5563),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- Sliders: Stroke Width & Opacity ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stroke: ${"%.1f".format(state.strokeWidth)} px",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Slider(
                value = state.strokeWidth,
                onValueChange = { viewModel.setStrokeWidth(it) },
                valueRange = 1f..50f,
                modifier = Modifier.width(280.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opacity: ${(state.shapeOpacity * 100).roundToInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Slider(
                value = state.shapeOpacity,
                onValueChange = { viewModel.setShapeOpacity(it) },
                valueRange = 0.05f..1f,
                modifier = Modifier.width(280.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB)
                )
            )
        }

        // --- Radius slider & Dimensions Toggle ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Radius: ${state.shapeCornerRadius.toInt()} px",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (activeSpec.supportsRadius) Color(0xFF374151) else Color(0xFF9CA3AF)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = state.shapeCornerRadius,
                    onValueChange = { viewModel.setShapeCornerRadius(it) },
                    valueRange = 0f..80f,
                    enabled = activeSpec.supportsRadius,
                    modifier = Modifier.width(180.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2563EB),
                        activeTrackColor = Color(0xFF2563EB)
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.showShapeDimensions) Color(0xFFEFF6FF) else Color(0xFFF3F4F6))
                        .border(
                            1.dp,
                            if (state.showShapeDimensions) Color(0xFF2563EB) else Color(0xFFD1D5DB),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.toggleShapeDimensions() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (state.showShapeDimensions) "✓ Dims" else "Dims",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.showShapeDimensions) Color(0xFF2563EB) else Color(0xFF4B5563)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Style Mode Toggle (Border vs Fill) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3F4F6))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isBorder = state.shapeStyleMode == ShapeStyleMode.BORDER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isBorder) Color.White else Color.Transparent)
                        .clickable { viewModel.setShapeStyleMode(ShapeStyleMode.BORDER) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "● Border",
                        fontSize = 12.sp,
                        fontWeight = if (isBorder) FontWeight.Bold else FontWeight.Medium,
                        color = if (isBorder) Color(0xFF1F2937) else Color(0xFF6B7280)
                    )
                }

                val isFill = state.shapeStyleMode == ShapeStyleMode.FILL
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFill) Color.White else Color.Transparent)
                        .clickable { viewModel.setShapeStyleMode(ShapeStyleMode.FILL) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⊘ Fill",
                        fontSize = 12.sp,
                        fontWeight = if (isFill) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFill) Color(0xFF1F2937) else Color(0xFF6B7280)
                    )
                }
            }

            Text(
                text = if (state.shapeStyleMode == ShapeStyleMode.BORDER) "Border Color" else "Fill Color",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(Modifier.height(10.dp))

        // --- Color Palette Swatches ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "None" option for fill mode
            if (state.shapeStyleMode == ShapeStyleMode.FILL) {
                val isNoneSelected = currentModeColor == 0
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                        .border(
                            width = if (isNoneSelected) 2.dp else 1.dp,
                            color = if (isNoneSelected) Color(0xFF2563EB) else Color(0xFFD1D5DB),
                            shape = CircleShape
                        )
                        .clickable { viewModel.setShapeColor(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⊘", fontSize = 12.sp, color = Color(0xFF4B5563))
                }
            }

            SHAPE_PRESET_COLORS.forEach { colorInt ->
                val isSelected = currentModeColor == colorInt
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFD1D5DB),
                            shape = CircleShape
                        )
                        .clickable { viewModel.setShapeColor(colorInt) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val checkTint = if (colorInt == 0xFFFFFFFF.toInt() || colorInt == 0xFFFBBF24.toInt()) {
                            Color(0xFF1F2937)
                        } else {
                            Color.White
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = checkTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Custom color button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                    .clickable { showColorPicker = !showColorPicker },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Custom color",
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- Custom Color Picker (expandable RGB sliders) ---
        if (showColorPicker) {
            Spacer(Modifier.height(10.dp))
            CustomColorPicker(
                currentColor = if (currentModeColor == 0) 0xFF1B1F2A.toInt() else currentModeColor,
                onColorChange = { viewModel.setShapeColor(it) }
            )
        }
    }
}

/**
 * Renders vector icon for shape cards in ShapeToolPanel.
 */
@Composable
private fun ShapeIconPreview(spec: ShapeSpec, tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val pad = 3.dp.toPx()
        val box = Rect(pad, pad, size.width - pad, size.height - pad)
        val stroke = DrawStroke(width = 1.8f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val dashedStroke = DrawStroke(
            width = 1.5f.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        )

        when (spec.strokeType) {
            StrokeType.RECTANGLE -> {
                drawRect(tint, topLeft = box.topLeft, size = box.size, style = stroke)
            }
            StrokeType.ROUNDED_RECTANGLE -> {
                drawRoundRect(
                    tint,
                    topLeft = box.topLeft,
                    size = box.size,
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = stroke
                )
            }
            StrokeType.CIRCLE -> {
                drawCircle(tint, radius = minOf(box.width, box.height) / 2f, center = box.center, style = stroke)
            }
            StrokeType.ELLIPSE -> {
                drawOval(tint, topLeft = box.topLeft, size = box.size, style = stroke)
            }
            StrokeType.SEMICIRCLE -> {
                val path = buildSemicirclePath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.RIGHT_TRIANGLE -> {
                val path = buildRightTrianglePath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(box.center.x, box.top)
                    lineTo(box.right, box.bottom)
                    lineTo(box.left, box.bottom)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            StrokeType.SCALENE_TRIANGLE -> {
                val path = buildScaleneTrianglePath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.TRAPEZOID -> {
                val path = buildTrapezoidPath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.STAR -> {
                val path = buildStarPath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.PARALLELOGRAM -> {
                val path = buildParallelogramPath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.RHOMBUS -> {
                val path = buildRhombusPath(box)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.PENTAGON -> {
                val path = buildRegularPolygonPath(box, 5)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.HEXAGON -> {
                val path = buildRegularPolygonPath(box, 6)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.OCTAGON -> {
                val path = buildRegularPolygonPath(box, 8)
                drawPath(path, tint, style = stroke)
            }
            StrokeType.LINE -> {
                drawLine(tint, Offset(box.left, box.bottom), Offset(box.right, box.top), strokeWidth = stroke.width)
            }
            StrokeType.LINE_DASHED -> {
                drawLine(
                    tint,
                    Offset(box.left, box.bottom),
                    Offset(box.right, box.top),
                    strokeWidth = dashedStroke.width,
                    pathEffect = dashedStroke.pathEffect
                )
            }
            StrokeType.ARROW -> {
                val start = Offset(box.left, box.bottom)
                val end = Offset(box.right, box.top)
                drawLine(tint, start, end, strokeWidth = stroke.width)
                drawArrowHead(end, start, tint, stroke.width, 1.2f)
            }
            StrokeType.DOUBLE_ARROW -> {
                val start = Offset(box.left, box.bottom)
                val end = Offset(box.right, box.top)
                drawLine(tint, start, end, strokeWidth = stroke.width)
                drawArrowHead(end, start, tint, stroke.width, 1.2f)
                drawArrowHead(start, end, tint, stroke.width, 1.2f)
            }
            StrokeType.DASHED_ARROW -> {
                val start = Offset(box.left, box.bottom)
                val end = Offset(box.right, box.top)
                drawLine(tint, start, end, strokeWidth = dashedStroke.width, pathEffect = dashedStroke.pathEffect)
                drawArrowHead(end, start, tint, stroke.width, 1.2f)
            }
            StrokeType.ANGLE_RAY -> {
                val path = buildAngleRayPath(Offset(box.left, box.top), Offset(box.right, box.bottom))
                drawPath(path, tint, style = stroke)
                drawArrowHead(Offset(box.left, box.top), Offset(box.left, box.bottom), tint, stroke.width, 1.2f)
                drawArrowHead(Offset(box.right, box.bottom), Offset(box.left, box.bottom), tint, stroke.width, 1.2f)
            }
            StrokeType.CUBE -> {
                render3DCuboid(box, tint, Color.Transparent, stroke, dashedStroke, isCube = true)
            }
            StrokeType.CUBOID -> {
                render3DCuboid(box, tint, Color.Transparent, stroke, dashedStroke, isCube = false)
            }
            StrokeType.PYRAMID -> {
                render3DPyramid(box, tint, Color.Transparent, stroke, dashedStroke)
            }
            StrokeType.PRISM -> {
                render3DPrism(box, tint, Color.Transparent, stroke, dashedStroke)
            }
            StrokeType.CYLINDER -> {
                render3DCylinder(box, tint, Color.Transparent, stroke, dashedStroke)
            }
            StrokeType.CONE -> {
                render3DCone(box, tint, Color.Transparent, stroke, dashedStroke)
            }
            StrokeType.SPHERE -> {
                render3DSphere(box, tint, Color.Transparent, stroke, dashedStroke)
            }
            else -> {
                drawRect(tint, topLeft = box.topLeft, size = box.size, style = stroke)
            }
        }
    }
}

@Composable
private fun CustomColorPicker(
    currentColor: Int,
    onColorChange: (Int) -> Unit
) {
    var red by remember(currentColor) { mutableIntStateOf((currentColor shr 16) and 0xFF) }
    var green by remember(currentColor) { mutableIntStateOf((currentColor shr 8) and 0xFF) }
    var blue by remember(currentColor) { mutableIntStateOf(currentColor and 0xFF) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9FAFB))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("RGB Custom Color", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(red, green, blue))
                    .border(1.dp, Color(0xFFD1D5DB), CircleShape)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Red slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R: $red", fontSize = 10.sp, color = Color(0xFFEF4444), modifier = Modifier.width(36.dp))
            Slider(
                value = red.toFloat(),
                onValueChange = {
                    red = it.roundToInt()
                    val newCol = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                    onColorChange(newCol)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }

        // Green slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G: $green", fontSize = 10.sp, color = Color(0xFF22C55E), modifier = Modifier.width(36.dp))
            Slider(
                value = green.toFloat(),
                onValueChange = {
                    green = it.roundToInt()
                    val newCol = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                    onColorChange(newCol)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }

        // Blue slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B: $blue", fontSize = 10.sp, color = Color(0xFF3B82F6), modifier = Modifier.width(36.dp))
            Slider(
                value = blue.toFloat(),
                onValueChange = {
                    blue = it.roundToInt()
                    val newCol = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                    onColorChange(newCol)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
