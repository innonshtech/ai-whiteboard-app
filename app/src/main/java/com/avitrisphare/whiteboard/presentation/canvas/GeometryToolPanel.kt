package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class GeometryCategory {
    INSTRUMENTS,
    AIDS
}

@Composable
private fun localizedGeoCategory(cat: GeometryCategory): String = when (cat) {
    GeometryCategory.INSTRUMENTS -> stringResource(R.string.geo_instruments)
    GeometryCategory.AIDS -> stringResource(R.string.geo_board)
}

@Composable
private fun localizedInstrumentName(kind: MathToolKind): String = when (kind) {
    MathToolKind.RULER -> stringResource(R.string.geo_ruler)
    MathToolKind.COMPASS -> stringResource(R.string.geo_compass)
    MathToolKind.PROTRACTOR -> stringResource(R.string.geo_protractor)
    MathToolKind.SET_SQUARE -> stringResource(R.string.geo_set_square)
    MathToolKind.TRIANGLE -> stringResource(R.string.geo_triangle)
}

@Composable
private fun localizedInstrumentHint(kind: MathToolKind): String = when (kind) {
    MathToolKind.RULER -> stringResource(R.string.geo_ruler_hint)
    MathToolKind.COMPASS -> stringResource(R.string.geo_compass_hint)
    MathToolKind.PROTRACTOR -> stringResource(R.string.geo_protractor_hint)
    MathToolKind.SET_SQUARE -> stringResource(R.string.geo_set_square_hint)
    MathToolKind.TRIANGLE -> stringResource(R.string.geo_triangle_hint)
}

@Composable
private fun localizedSizeLabel(kind: MathToolKind, span: Float, pxPerCm: Float): String {
    val safe = pxPerCm.coerceAtLeast(1f)
    return when (kind) {
        MathToolKind.RULER -> stringResource(R.string.geo_length, formatCentimetres(span, safe))
        MathToolKind.COMPASS -> stringResource(R.string.geo_radius, formatCentimetres(span, safe))
        MathToolKind.PROTRACTOR -> stringResource(R.string.geo_radius, formatCentimetres(span, safe))
        MathToolKind.SET_SQUARE -> {
            val leg = span / safe
            val hyp = leg * 1.41421356f
            stringResource(R.string.geo_legs_hyp, leg, hyp)
        }
        MathToolKind.TRIANGLE -> stringResource(R.string.geo_side, formatCentimetres(triangleSidePx(span), safe))
    }
}

private data class GeoInstrument(
    val kind: MathToolKind,
    val name: String,
    val hint: String
)

private val INSTRUMENTS = listOf(
    GeoInstrument(
        MathToolKind.RULER,
        "Ruler",
        "Draw along either edge. Length is shown in centimetres."
    ),
    GeoInstrument(
        MathToolKind.COMPASS,
        "Compass",
        "Drag the pencil to set the radius. Tap for a circle, or sweep an arc."
    ),
    GeoInstrument(
        MathToolKind.PROTRACTOR,
        "Protractor",
        "Set the arm, then mark the left or right angle in degrees."
    ),
    GeoInstrument(
        MathToolKind.SET_SQUARE,
        "Set square",
        "45°-45°-90°. Draw along the base, the upright, or the hypotenuse."
    ),
    GeoInstrument(
        MathToolKind.TRIANGLE,
        "Triangle",
        "Equilateral, 60°-60°-60°. Draw along any side."
    )
)

private val INK_COLORS = listOf(
    0xFFEF4444.toInt(),
    0xFFF97316.toInt(),
    0xFFFBBF24.toInt(),
    0xFF22C55E.toInt(),
    0xFF3B82F6.toInt(),
    0xFF6366F1.toInt(),
    0xFFA855F7.toInt(),
    0xFF1B1F2A.toInt(),
    0xFFFFFFFF.toInt()
)

private val ROTATION_PRESETS = listOf(0f, 45f, 90f, 180f)
private val ANGLE_PRESETS = listOf(30f, 45f, 60f, 90f, 120f, 135f, 150f)

private val Blue = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFEFF6FF)
private val Ink = Color(0xFF374151)
private val Muted = Color(0xFF6B7280)
private val Line = Color(0xFFE5E7EB)
private val Chip = Color(0xFFF3F4F6)

@Composable
fun GeometryToolPanel(
    state: CanvasState,
    viewModel: CanvasViewModel,
    placed: List<PlacedMathTool>,
    pxPerCm: Float,
    onMathDrag: (MathToolKind, Offset) -> Unit,
    onMathDragEnd: (MathToolKind, Offset, Boolean) -> Unit,
    onMathAdjust: (MathToolKind, MathToolAdjust) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var category by remember { mutableStateOf(GeometryCategory.INSTRUMENTS) }
    var pinned by remember { mutableStateOf(instrumentForTool(state.currentTool, placed) ?: MathToolKind.RULER) }
    val selectedKind = instrumentForTool(state.currentTool, placed) ?: pinned
    val selected = INSTRUMENTS.first { it.kind == selectedKind }
    val live = placed.find { it.kind == selectedKind }

    var stagedSpan by remember { mutableStateOf(mapOf<MathToolKind, Float>()) }
    var stagedRotation by remember { mutableStateOf(mapOf<MathToolKind, Float>()) }
    var stagedPointer by remember { mutableStateOf(90f) }

    fun spanOf(kind: MathToolKind): Float {
        return placed.find { it.kind == kind }?.span
            ?: stagedSpan[kind]
            ?: defaultMathSpan(kind)
    }

    fun rotationOf(kind: MathToolKind): Float {
        val raw = placed.find { it.kind == kind }?.rotationDeg ?: stagedRotation[kind] ?: 0f
        val wrapped = raw % 360f
        return if (wrapped < 0f) wrapped + 360f else wrapped
    }

    fun pointerOf(): Float {
        return placed.find { it.kind == MathToolKind.PROTRACTOR }?.pointerAngleDeg ?: stagedPointer
    }

    fun stageAndApply(kind: MathToolKind, adjust: MathToolAdjust) {
        adjust.span?.let { stagedSpan = stagedSpan + (kind to clampMathSpan(kind, it)) }
        adjust.rotationDeg?.let { stagedRotation = stagedRotation + (kind to it) }
        adjust.pointerAngleDeg?.let { stagedPointer = it.coerceIn(0f, 180f) }
        if (placed.any { it.kind == kind }) onMathAdjust(kind, adjust)
    }

    fun placeOrMove(kind: MathToolKind, root: Offset, moved: Boolean) {
        val wasPlaced = placed.any { it.kind == kind }
        pinned = kind
        onMathDragEnd(kind, if (moved) root else Offset.Zero, moved)
        if (!wasPlaced) {
            onMathAdjust(
                kind,
                MathToolAdjust(
                    span = stagedSpan[kind],
                    rotationDeg = stagedRotation[kind],
                    pointerAngleDeg = if (kind == MathToolKind.PROTRACTOR) stagedPointer else null
                )
            )
        }
    }

    Column(
        modifier = modifier
            .width(330.dp)
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.geo_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Chip)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dialog_close), tint = Color(0xFF4B5563), modifier = Modifier.size(14.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GeometryCategory.entries.forEach { cat ->
                val on = cat == category
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) Blue else Chip)
                        .clickable { category = cat }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        localizedGeoCategory(cat),
                        fontSize = 11.5.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) Color.White else Ink
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (category == GeometryCategory.INSTRUMENTS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                INSTRUMENTS.forEach { spec ->
                    InstrumentCard(
                        spec = spec,
                        selected = spec.kind == selectedKind,
                        placed = placed.any { it.kind == spec.kind },
                        onTap = { placeOrMove(spec.kind, Offset.Zero, false) },
                        onDrag = { onMathDrag(spec.kind, it) },
                        onDragEnd = { root -> placeOrMove(spec.kind, root, true) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val span = spanOf(selectedKind)
            val range = mathSpanRange(selectedKind)
            MeasureSlider(
                label = localizedSizeLabel(selectedKind, span, pxPerCm),
                value = span.coerceIn(range.start, range.endInclusive),
                valueRange = range,
                onValueChange = { stageAndApply(selectedKind, MathToolAdjust(span = it)) }
            )

            val rotation = rotationOf(selectedKind)
            MeasureSlider(
                label = stringResource(R.string.geo_rotation, rotation.roundToInt()),
                value = rotation.coerceIn(0f, 359f),
                valueRange = 0f..359f,
                onValueChange = { stageAndApply(selectedKind, MathToolAdjust(rotationDeg = it)) }
            )
            ChipRow(ROTATION_PRESETS.map { "${it.roundToInt()}°" to it }, rotation) {
                stageAndApply(selectedKind, MathToolAdjust(rotationDeg = it))
            }

            if (selectedKind == MathToolKind.PROTRACTOR) {
                val pointer = pointerOf().coerceIn(0f, 180f)
                val other = (180f - pointer).coerceIn(0f, 180f)
                Spacer(Modifier.height(4.dp))
                MeasureSlider(
                    label = stringResource(R.string.geo_arm_other, pointer.roundToInt(), other.roundToInt()),
                    value = pointer,
                    valueRange = 0f..180f,
                    onValueChange = { stageAndApply(MathToolKind.PROTRACTOR, MathToolAdjust(pointerAngleDeg = it)) }
                )
                ChipRow(ANGLE_PRESETS.map { "${it.roundToInt()}°" to it }, pointer) {
                    stageAndApply(MathToolKind.PROTRACTOR, MathToolAdjust(pointerAngleDeg = it))
                }
            }

            Spacer(Modifier.height(6.dp))
            val onBoard = live != null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (onBoard) BlueSoft else Chip)
                    .border(1.dp, if (onBoard) Blue else Color(0xFFD1D5DB), RoundedCornerShape(6.dp))
                    .clickable { placeOrMove(selectedKind, Offset.Zero, false) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    if (onBoard) stringResource(R.string.geo_remove_board) else stringResource(R.string.geo_place_board),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (onBoard) Blue else Ink
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AidCard(
                    name = stringResource(R.string.geo_aid_graph),
                    selected = state.canvasBackground == CanvasBackground.GRAPH,
                    onClick = { viewModel.toggleGraphPaper() }
                ) { tint -> GraphPaperIcon(tint) }
                AidCard(
                    name = stringResource(R.string.geo_aid_line),
                    selected = state.currentTool == WhiteboardTool.LINE,
                    onClick = { viewModel.setTool(WhiteboardTool.LINE) }
                ) { tint -> LineIcon(tint) }
                AidCard(
                    name = stringResource(R.string.geo_aid_arrow),
                    selected = state.currentTool == WhiteboardTool.ARROW,
                    onClick = { viewModel.setTool(WhiteboardTool.ARROW) }
                ) { tint -> ArrowIcon(tint) }
                AidCard(
                    name = stringResource(R.string.geo_aid_frame),
                    selected = state.currentTool == WhiteboardTool.FRAME,
                    onClick = { viewModel.setTool(WhiteboardTool.FRAME) }
                ) { tint -> FrameIcon(tint) }
                AidCard(
                    name = stringResource(R.string.magic_pen),
                    selected = state.isDrawToShapeEnabled,
                    onClick = { viewModel.toggleDrawToShape() }
                ) { tint -> MagicPenIcon(tint) }
            }
            if (state.smartWriteEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.smart_write_tip), fontSize = 11.sp, color = Muted)
            }
        }

        Spacer(Modifier.height(12.dp))
        MeasureSlider(
            label = stringResource(R.string.stroke_px, state.strokeWidth),
            value = state.strokeWidth.coerceIn(1f, 50f),
            valueRange = 1f..50f,
            onValueChange = { viewModel.setStrokeWidth(it) }
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.ink_colour), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(6.dp))
        InkSwatches(selected = state.selectedColor, onPick = { viewModel.setColor(it) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstrumentCard(
    spec: GeoInstrument,
    selected: Boolean,
    placed: Boolean,
    onTap: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 58.dp)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(spec.kind, placed) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    var moved = 0f
                    var previous = down.position
                    var root = origin + down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (moved > 12f) onDragEnd(root) else onTap()
                            break
                        }
                        val delta = change.position - previous
                        moved += delta.getDistance()
                        previous = change.position
                        root = origin + change.position
                        if (moved > 12f) onDrag(root)
                        change.consume()
                    }
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BlueSoft else Color(0xFFF9FAFB))
            .border(if (selected) 2.dp else 1.dp, if (selected) Blue else Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        if (placed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Blue)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            InstrumentIcon(spec.kind, if (selected) Blue else Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                localizedInstrumentName(spec.kind),
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFF1D4ED8) else Color(0xFF4B5563),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AidCard(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 54.dp, height = 58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BlueSoft else Color(0xFFF9FAFB))
            .border(if (selected) 2.dp else 1.dp, if (selected) Blue else Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(if (selected) Blue else Ink)
        Spacer(Modifier.height(2.dp))
        Text(
            name,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFF1D4ED8) else Color(0xFF4B5563),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MeasureSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
        )
    }
}

@Composable
private fun ChipRow(
    chips: List<Pair<String, Float>>,
    current: Float,
    onPick: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (label, degrees) ->
            val on = abs(current - degrees) < 0.6f
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) BlueSoft else Chip)
                    .border(1.dp, if (on) Blue else Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                    .clickable { onPick(degrees) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) Blue else Color(0xFF4B5563)
                )
            }
        }
    }
}

@Composable
private fun InkSwatches(selected: Int, onPick: (Int) -> Unit) {
    val colors = if (selected != 0 && selected !in INK_COLORS) listOf(selected) + INK_COLORS else INK_COLORS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { colorInt ->
            val on = selected == colorInt
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(if (on) 2.dp else 1.dp, if (on) Blue else Color(0xFFD1D5DB), CircleShape)
                    .clickable { onPick(colorInt) },
                contentAlignment = Alignment.Center
            ) {
                if (on) {
                    val tick = if (colorInt == 0xFFFFFFFF.toInt() || colorInt == 0xFFFBBF24.toInt()) {
                        Color(0xFF1F2937)
                    } else {
                        Color.White
                    }
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = tick, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun InstrumentIcon(kind: MathToolKind, tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val pad = 3.dp.toPx()
        val box = Rect(pad, pad, size.width - pad, size.height - pad)
        val stroke = DrawStroke(width = 1.7f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            MathToolKind.RULER -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(box.left, box.center.y - box.height * 0.22f),
                    size = Size(box.width, box.height * 0.44f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = stroke
                )
                val top = box.center.y - box.height * 0.22f
                val step = box.width / 6f
                for (i in 1..5) {
                    val h = if (i % 2 == 0) box.height * 0.16f else box.height * 0.09f
                    drawLine(
                        tint,
                        Offset(box.left + step * i, top),
                        Offset(box.left + step * i, top + h),
                        strokeWidth = stroke.width
                    )
                }
            }
            MathToolKind.COMPASS -> {
                val hinge = Offset(box.center.x, box.top + 2.dp.toPx())
                val left = Offset(box.left + 1.dp.toPx(), box.bottom)
                val right = Offset(box.right - 1.dp.toPx(), box.bottom)
                drawLine(tint, hinge, left, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, hinge, right, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, radius = 1.6f.dp.toPx(), center = hinge)
                drawCircle(tint, radius = 1.6f.dp.toPx(), center = right)
            }
            MathToolKind.PROTRACTOR -> {
                val radius = box.width / 2f
                val center = Offset(box.center.x, box.bottom - 1.dp.toPx())
                drawArc(
                    tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke
                )
                drawLine(tint, Offset(box.left, center.y), Offset(box.right, center.y), strokeWidth = stroke.width)
                for (deg in listOf(30, 60, 90, 120, 150)) {
                    val rad = Math.toRadians(deg.toDouble())
                    val inner = radius * 0.72f
                    val outer = radius * 0.96f
                    val dx = cos(rad).toFloat()
                    val dy = -sin(rad).toFloat()
                    drawLine(
                        tint,
                        Offset(center.x + dx * inner, center.y + dy * inner),
                        Offset(center.x + dx * outer, center.y + dy * outer),
                        strokeWidth = stroke.width
                    )
                }
            }
            MathToolKind.SET_SQUARE -> {
                val path = Path().apply {
                    moveTo(box.left, box.bottom)
                    lineTo(box.right, box.bottom)
                    lineTo(box.left, box.top)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            MathToolKind.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(box.center.x, box.top)
                    lineTo(box.right, box.bottom)
                    lineTo(box.left, box.bottom)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
        }
    }
}

@Composable
private fun GraphPaperIcon(tint: Color) {
    Canvas(Modifier.size(28.dp)) {
        val pad = 4.dp.toPx()
        val box = Rect(pad, pad, size.width - pad, size.height - pad)
        val stroke = 1.2f.dp.toPx()
        drawRect(tint, topLeft = box.topLeft, size = box.size, style = DrawStroke(stroke))
        val stepX = box.width / 3f
        val stepY = box.height / 3f
        for (i in 1..2) {
            drawLine(tint, Offset(box.left + stepX * i, box.top), Offset(box.left + stepX * i, box.bottom), stroke)
            drawLine(tint, Offset(box.left, box.top + stepY * i), Offset(box.right, box.top + stepY * i), stroke)
        }
    }
}

@Composable
private fun MagicPenIcon(tint: Color) {
    Canvas(Modifier.size(28.dp)) {
        val stroke = 1.7f.dp.toPx()
        drawLine(
            tint,
            Offset(size.width * 0.28f, size.height * 0.72f),
            Offset(size.width * 0.72f, size.height * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(tint, radius = 2.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.22f))
    }
}

@Composable
private fun LineIcon(tint: Color) {
    Canvas(Modifier.size(28.dp)) {
        drawLine(
            tint,
            Offset(size.width * 0.18f, size.height * 0.72f),
            Offset(size.width * 0.82f, size.height * 0.28f),
            strokeWidth = 1.7f.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ArrowIcon(tint: Color) {
    Canvas(Modifier.size(28.dp)) {
        val start = Offset(size.width * 0.18f, size.height * 0.72f)
        val end = Offset(size.width * 0.78f, size.height * 0.28f)
        drawLine(tint, start, end, strokeWidth = 1.7f.dp.toPx(), cap = StrokeCap.Round)
        val back = Offset(end.x - 7.dp.toPx(), end.y + 1.dp.toPx())
        drawLine(tint, end, Offset(back.x, end.y + 5.dp.toPx()), strokeWidth = 1.7f.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, end, Offset(end.x - 2.dp.toPx(), end.y + 8.dp.toPx()), strokeWidth = 1.7f.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun FrameIcon(tint: Color) {
    Canvas(Modifier.size(28.dp)) {
        val pad = 5.dp.toPx()
        val box = Rect(pad, pad, size.width - pad, size.height - pad)
        drawRect(tint, topLeft = box.topLeft, size = box.size, style = DrawStroke(1.7f.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun instrumentForTool(tool: WhiteboardTool, placed: List<PlacedMathTool>): MathToolKind? = when (tool) {
    WhiteboardTool.RULER -> MathToolKind.RULER
    WhiteboardTool.COMPASS -> MathToolKind.COMPASS
    WhiteboardTool.PROTRACTOR -> MathToolKind.PROTRACTOR
    WhiteboardTool.SET_SQUARE -> MathToolKind.SET_SQUARE
    WhiteboardTool.TRIANGLE -> if (placed.any { it.kind == MathToolKind.TRIANGLE }) MathToolKind.TRIANGLE else null
    else -> null
}

private fun sizeLabel(kind: MathToolKind, span: Float, pxPerCm: Float): String {
    val safe = pxPerCm.coerceAtLeast(1f)
    return when (kind) {
        MathToolKind.RULER -> "Length: ${formatCentimetres((span - 2 * RULER_INSET).coerceAtLeast(0f), safe)}"
        MathToolKind.COMPASS -> "Radius: ${formatCentimetres(span, safe)}"
        MathToolKind.PROTRACTOR -> "Radius: ${formatCentimetres(span, safe)}"
        MathToolKind.SET_SQUARE -> {
            val leg = span / safe
            val hyp = leg * 1.41421356f
            String.format(Locale.US, "Legs %.1f · hyp %.1f cm", leg, hyp)
        }
        MathToolKind.TRIANGLE -> "Side: ${formatCentimetres(triangleSidePx(span), safe)}"
    }
}
