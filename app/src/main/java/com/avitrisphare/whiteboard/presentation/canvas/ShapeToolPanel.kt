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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.roundToInt

@Composable
private fun localizedShapeCategory(cat: ShapeCategory): String = when (cat) {
    ShapeCategory.SHAPES_2D -> stringResource(R.string.shape_cat_2d)
    ShapeCategory.LINES_RAYS -> stringResource(R.string.shape_cat_lines_rays)
    ShapeCategory.SOLIDS_3D -> stringResource(R.string.shape_cat_3d)
}

@Composable
private fun localizedShapeName(strokeType: StrokeType): String = when (strokeType) {
    StrokeType.RECTANGLE -> stringResource(R.string.shape_rectangle)
    StrokeType.SQUARE -> stringResource(R.string.shape_square)
    StrokeType.ROUNDED_RECTANGLE -> stringResource(R.string.shape_rounded_rect)
    StrokeType.CIRCLE -> stringResource(R.string.shape_circle)
    StrokeType.ELLIPSE -> stringResource(R.string.shape_ellipse)
    StrokeType.SEMICIRCLE -> stringResource(R.string.shape_semicircle)
    StrokeType.RIGHT_TRIANGLE -> stringResource(R.string.shape_right_triangle)
    StrokeType.TRIANGLE -> stringResource(R.string.shape_triangle)
    StrokeType.SCALENE_TRIANGLE -> stringResource(R.string.shape_scalene)
    StrokeType.TRAPEZOID -> stringResource(R.string.shape_trapezoid)
    StrokeType.STAR -> stringResource(R.string.shape_star)
    StrokeType.PARALLELOGRAM -> stringResource(R.string.shape_parallelogram)
    StrokeType.RHOMBUS -> stringResource(R.string.shape_rhombus)
    StrokeType.PENTAGON -> stringResource(R.string.shape_pentagon)
    StrokeType.HEXAGON -> stringResource(R.string.shape_hexagon)
    StrokeType.OCTAGON -> stringResource(R.string.shape_octagon)
    StrokeType.LINE -> stringResource(R.string.shape_line)
    StrokeType.LINE_DASHED -> stringResource(R.string.shape_dashed)
    StrokeType.ARROW -> stringResource(R.string.shape_arrow)
    StrokeType.DOUBLE_ARROW -> stringResource(R.string.shape_double_arrow)
    StrokeType.DASHED_ARROW -> stringResource(R.string.shape_dashed_arrow)
    StrokeType.ANGLE_RAY -> stringResource(R.string.shape_angle_ray)
    StrokeType.CUBE -> stringResource(R.string.shape_cube)
    StrokeType.CUBOID -> stringResource(R.string.shape_cuboid)
    StrokeType.PYRAMID -> stringResource(R.string.shape_pyramid)
    StrokeType.PRISM -> stringResource(R.string.shape_prism)
    StrokeType.CYLINDER -> stringResource(R.string.shape_cylinder)
    StrokeType.CONE -> stringResource(R.string.shape_cone)
    StrokeType.SPHERE -> stringResource(R.string.shape_sphere)
    else -> strokeType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

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

@Composable
fun ShapeToolPanel(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onClose: () -> Unit,
    onDrag: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            .width(330.dp)
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // --- Header (Drag Handle & Close Button) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5F9))
                .then(
                    if (onDrag != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            }
                        }
                    } else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Move Shapes Window",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.shapes_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onDrag != null) {
                    Text(
                        text = stringResource(R.string.drag_to_move),
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFCBD5E1))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.dialog_close),
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Sub-category Pills (2D Shapes, Lines & Rays, 3D Solids) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShapeCategory.values().forEach { cat ->
                val isCatSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCatSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6))
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedShapeCategory(cat),
                        fontSize = 11.5.sp,
                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCatSelected) Color.White else Color(0xFF374151),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Horizontal Shape Cards Grid ---
        val shapesInCat = remember(selectedCategory) {
            ALL_SHAPES.filter { it.category == selectedCategory }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            shapesInCat.forEach { spec ->
                val isSelected = spec.strokeType == state.activeShapeType
                Column(
                    modifier = Modifier
                        .size(width = 54.dp, height = 58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            viewModel.setShapeType(spec.strokeType)
                        }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ShapeIconPreview(
                        spec = spec,
                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF374151)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = localizedShapeName(spec.strokeType),
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF4B5563),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Sliders: Stroke Width & Opacity ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.stroke_px, state.strokeWidth),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                modifier = Modifier.width(84.dp)
            )
            Slider(
                value = state.strokeWidth,
                onValueChange = { viewModel.setStrokeWidth(it) },
                valueRange = 1f..50f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.shape_opacity, (state.shapeOpacity * 100).roundToInt()),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                modifier = Modifier.width(84.dp)
            )
            Slider(
                value = state.shapeOpacity,
                onValueChange = { viewModel.setShapeOpacity(it) },
                valueRange = 0.05f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB)
                )
            )
        }

        // --- Radius slider & Dimensions Toggle ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.shape_radius, state.shapeCornerRadius.toInt()),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (activeSpec.supportsRadius) Color(0xFF374151) else Color(0xFF9CA3AF),
                modifier = Modifier.width(84.dp)
            )
            Slider(
                value = state.shapeCornerRadius,
                onValueChange = { viewModel.setShapeCornerRadius(it) },
                valueRange = 0f..80f,
                enabled = activeSpec.supportsRadius,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB)
                )
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state.showShapeDimensions) Color(0xFFEFF6FF) else Color(0xFFF3F4F6))
                    .border(
                        1.dp,
                        if (state.showShapeDimensions) Color(0xFF2563EB) else Color(0xFFD1D5DB),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { viewModel.toggleShapeDimensions() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (state.showShapeDimensions) stringResource(R.string.shape_dims_active) else stringResource(R.string.shape_dims),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.showShapeDimensions) Color(0xFF2563EB) else Color(0xFF4B5563)
                )
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
                        text = stringResource(R.string.shape_border),
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
                        text = stringResource(R.string.shape_fill),
                        fontSize = 12.sp,
                        fontWeight = if (isFill) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFill) Color(0xFF1F2937) else Color(0xFF6B7280)
                    )
                }
            }

            Text(
                text = if (state.shapeStyleMode == ShapeStyleMode.BORDER) stringResource(R.string.shape_border_color) else stringResource(R.string.shape_fill_color),
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
                    contentDescription = stringResource(R.string.custom_color),
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
            StrokeType.SQUARE -> {
                val side = minOf(box.width, box.height)
                val left = box.center.x - side / 2f
                val top = box.center.y - side / 2f
                drawRect(tint, topLeft = Offset(left, top), size = Size(side, side), style = stroke)
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
            Text(stringResource(R.string.rgb_custom_color), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
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

