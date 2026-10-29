package com.avitrisphare.whiteboard.presentation.canvas.utility

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PeriodicTableFloatingPanel(
    state: PeriodicTableUiState,
    onStateChange: (PeriodicTableUiState) -> Unit,
    onBringToFront: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedElement = state.selectedElement ?: PeriodicTableData.elements.first()
    var zoomScale by remember { mutableFloatStateOf(state.zoomScale) }

    FloatingToolPanel(
        title = "Periodic Table of Elements",
        icon = Icons.Default.Science,
        position = state.position,
        isMinimized = state.isMinimized,
        onPositionChange = { onStateChange(state.copy(position = it)) },
        onToggleMinimize = { onStateChange(state.copy(isMinimized = !state.isMinimized)) },
        onClose = { onStateChange(state.copy(isVisible = false)) },
        minimizedSummary = "Table: ${selectedElement.symbol} (${selectedElement.name})",
        onBringToFront = onBringToFront,
        isDarkMode = state.isDarkMode,
        headerActions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // Light / Dark Mode Toggle Pill (Defaults to Light Mode)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0))
                        .clickable { onStateChange(state.copy(isDarkMode = !state.isDarkMode)) }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (state.isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = if (state.isDarkMode) Color(0xFFFBBF24) else Color(0xFF475569),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (state.isDarkMode) "Light" else "Dark",
                        fontSize = 11.sp,
                        color = if (state.isDarkMode) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(2.dp))

                // Zoom Out Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (state.isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0))
                        .clickable {
                            val next = (zoomScale - 0.25f).coerceAtLeast(0.75f)
                            zoomScale = next
                            onStateChange(state.copy(zoomScale = next))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = if (state.isDarkMode) Color.White else Color(0xFF334155),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Reset to 100% Fit Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (zoomScale == 1.0f) {
                                Color(0xFF2563EB)
                            } else if (state.isDarkMode) {
                                Color(0xFF283248)
                            } else {
                                Color(0xFFE2E8F0)
                            }
                        )
                        .clickable {
                            zoomScale = 1.0f
                            onStateChange(state.copy(zoomScale = 1.0f))
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(zoomScale * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        color = if (zoomScale == 1.0f) Color.White else if (state.isDarkMode) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Zoom In Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (state.isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0))
                        .clickable {
                            val next = (zoomScale + 0.25f).coerceAtMost(2.0f)
                            zoomScale = next
                            onStateChange(state.copy(zoomScale = next))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = if (state.isDarkMode) Color.White else Color(0xFF334155),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.width(956.dp)
        ) {
            // Search Bar & Filter Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isDarkMode) Color(0xFF131722) else Color(0xFFF8FAFC))
                        .border(1.dp, if (state.isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (state.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search by element name, symbol, or atomic number...",
                                    fontSize = 13.sp,
                                    color = if (state.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { onStateChange(state.copy(searchQuery = it)) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = if (state.isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (state.searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1))
                                    .clickable { onStateChange(state.copy(searchQuery = "")) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = if (state.isDarkMode) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                if (state.selectedCategory != null) {
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .clickable { onStateChange(state.copy(selectedCategory = null)) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Reset Filter",
                            fontSize = 12.sp,
                            color = if (state.isDarkMode) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ElementCategory.values().forEach { category ->
                    val isSelected = state.selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    category.color.copy(alpha = if (state.isDarkMode) 0.35f else 0.20f)
                                } else {
                                    if (state.isDarkMode) Color(0xFF1A2234) else Color(0xFFF1F5F9)
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) category.color else if (state.isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onStateChange(state.copy(selectedCategory = if (isSelected) null else category))
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(category.color)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                category.displayName,
                                fontSize = 10.5.sp,
                                color = if (state.isDarkMode) Color.White else Color(0xFF0F172A),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val scrollModifier = if (zoomScale > 1.0f) {
                Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.isDarkMode) Color(0xFF0F131D) else Color(0xFFF8FAFC))
                    .border(1.dp, if (state.isDarkMode) Color(0xFF232B3E) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(5.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.isDarkMode) Color(0xFF0F131D) else Color(0xFFF8FAFC))
                    .border(1.dp, if (state.isDarkMode) Color(0xFF232B3E) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(5.dp)
            }

            // High-performance Periodic Table Grid: renders in < 2ms without heavy LayoutNode trees
            Box(modifier = scrollModifier) {
                PeriodicTableGridCanvas(
                    zoomScale = zoomScale,
                    selectedElement = selectedElement,
                    searchQuery = state.searchQuery,
                    selectedCategory = state.selectedCategory,
                    isDarkMode = state.isDarkMode,
                    onElementSelected = { elem ->
                        onStateChange(state.copy(selectedElement = elem))
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Inspector Detail Card for Selected Element (With Atomic Number, Mass Number clearly shown)
            SelectedElementCard(element = selectedElement, isDarkMode = state.isDarkMode)
        }
    }
}

@Composable
private fun PeriodicTableGridCanvas(
    zoomScale: Float,
    selectedElement: ChemicalElement,
    searchQuery: String,
    selectedCategory: ElementCategory?,
    isDarkMode: Boolean,
    onElementSelected: (ChemicalElement) -> Unit
) {
    val density = LocalDensity.current

    val baseCellWidth = 50.dp
    val baseCellHeight = 58.dp
    val baseGap = 2.5.dp

    val cellW = baseCellWidth * zoomScale
    val cellH = baseCellHeight * zoomScale
    val gap = baseGap * zoomScale

    val cellWPx = with(density) { cellW.toPx() }
    val cellHPx = with(density) { cellH.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val cornerRadiusPx = with(density) { 5.dp.toPx() }
    val borderWidthPx = with(density) { 1.dp.toPx() }
    val selectedBorderWidthPx = with(density) { 2.dp.toPx() }

    val colStep = cellWPx + gapPx
    val rowStep = cellHPx + gapPx

    val gridWidthPx = 18 * colStep - gapPx
    val mainGridHeightPx = 7 * rowStep - gapPx

    val dividerGapTop = with(density) { (gap * 1.5f).toPx() }
    val dividerThickness = with(density) { 1.dp.toPx() }
    val dividerGapBottom = with(density) { gap.toPx() }

    val dividerY = mainGridHeightPx + dividerGapTop
    val fBlockStartY = dividerY + dividerThickness + dividerGapBottom
    val totalGridHeightPx = fBlockStartY + 2 * rowStep - gapPx

    val gridWidthDp = with(density) { gridWidthPx.toDp() }
    val totalGridHeightDp = with(density) { totalGridHeightPx.toDp() }

    val hasFilter = searchQuery.isNotEmpty() || selectedCategory != null
    val matchingIds = remember(searchQuery, selectedCategory) {
        if (hasFilter) {
            PeriodicTableData.search(searchQuery, selectedCategory).map { it.atomicNumber }.toSet()
        } else {
            emptySet()
        }
    }

    // Reuse Paint objects for zero allocation during draw passes
    val bgPaint = remember { Paint().apply { style = Paint.Style.FILL; isAntiAlias = true } }
    val borderPaint = remember { Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true } }
    val numberPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }
    val symbolPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val namePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
    }
    val massPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
    }
    val markerSubPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(
        modifier = Modifier
            .size(gridWidthDp, totalGridHeightDp)
            .pointerInput(zoomScale, searchQuery, selectedCategory) {
                detectTapGestures { offset ->
                    val x = offset.x
                    val y = offset.y

                    // 1. Check main grid (periods 1..7, groups 1..18)
                    if (y in 0f..mainGridHeightPx && x in 0f..gridWidthPx) {
                        val col = (x / colStep).toInt()
                        val row = (y / rowStep).toInt()
                        val cellLeft = col * colStep
                        val cellTop = row * rowStep
                        if (x <= cellLeft + cellWPx && y <= cellTop + cellHPx && col in 0..17 && row in 0..6) {
                            val period = row + 1
                            val group = col + 1
                            PeriodicTableData.elementByGridPosition[period to group]?.let { elem ->
                                onElementSelected(elem)
                            }
                        }
                    } else if (y in fBlockStartY..totalGridHeightPx) {
                        // 2. Check Lanthanides & Actinides
                        val startX = 2 * colStep
                        if (x >= startX) {
                            val col = ((x - startX) / colStep).toInt()
                            val cellLeft = startX + col * colStep
                            if (col in 0..14 && x <= cellLeft + cellWPx) {
                                val fRow = ((y - fBlockStartY) / rowStep).toInt()
                                val cellTop = fBlockStartY + fRow * rowStep
                                if (y <= cellTop + cellHPx) {
                                    if (fRow == 0) {
                                        PeriodicTableData.lanthanides.getOrNull(col)?.let { onElementSelected(it) }
                                    } else if (fRow == 1) {
                                        PeriodicTableData.actinides.getOrNull(col)?.let { onElementSelected(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // 1. Draw periods 1 to 7
            for (period in 1..7) {
                for (group in 1..18) {
                    val col = group - 1
                    val row = period - 1
                    val cellLeft = col * colStep
                    val cellTop = row * rowStep

                    val elem = PeriodicTableData.elementByGridPosition[period to group]
                    if (elem != null) {
                        val isSelected = elem == selectedElement
                        val isDimmed = hasFilter && elem.atomicNumber !in matchingIds
                        drawCell(
                            nativeCanvas = nativeCanvas,
                            elem = elem,
                            left = cellLeft,
                            top = cellTop,
                            width = cellWPx,
                            height = cellHPx,
                            cornerRadius = cornerRadiusPx,
                            borderWidth = borderWidthPx,
                            selectedBorderWidth = selectedBorderWidthPx,
                            isSelected = isSelected,
                            isDimmed = isDimmed,
                            isDarkMode = isDarkMode,
                            zoomScale = zoomScale,
                            density = density.density,
                            bgPaint = bgPaint,
                            borderPaint = borderPaint,
                            numberPaint = numberPaint,
                            symbolPaint = symbolPaint,
                            namePaint = namePaint,
                            massPaint = massPaint
                        )
                    } else if (period == 6 && group == 3) {
                        drawMarker(
                            nativeCanvas = nativeCanvas,
                            label = "57-71",
                            sub = "La-Lu",
                            catName = "Lanthanide",
                            categoryColor = ElementCategory.LANTHANIDE.color,
                            left = cellLeft,
                            top = cellTop,
                            width = cellWPx,
                            height = cellHPx,
                            cornerRadius = cornerRadiusPx,
                            borderWidth = borderWidthPx,
                            isDarkMode = isDarkMode,
                            zoomScale = zoomScale,
                            density = density.density,
                            bgPaint = bgPaint,
                            borderPaint = borderPaint,
                            numberPaint = numberPaint,
                            symbolPaint = markerSubPaint,
                            namePaint = namePaint
                        )
                    } else if (period == 7 && group == 3) {
                        drawMarker(
                            nativeCanvas = nativeCanvas,
                            label = "89-103",
                            sub = "Ac-Lr",
                            catName = "Actinide",
                            categoryColor = ElementCategory.ACTINIDE.color,
                            left = cellLeft,
                            top = cellTop,
                            width = cellWPx,
                            height = cellHPx,
                            cornerRadius = cornerRadiusPx,
                            borderWidth = borderWidthPx,
                            isDarkMode = isDarkMode,
                            zoomScale = zoomScale,
                            density = density.density,
                            bgPaint = bgPaint,
                            borderPaint = borderPaint,
                            numberPaint = numberPaint,
                            symbolPaint = markerSubPaint,
                            namePaint = namePaint
                        )
                    }
                }
            }

            // 2. Divider line between main table and f-block
            val dividerColor = if (isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0)
            drawLine(
                color = dividerColor,
                start = Offset(0f, dividerY),
                end = Offset(gridWidthPx, dividerY),
                strokeWidth = dividerThickness
            )

            // 3. Lanthanides row
            val startX = 2 * colStep
            PeriodicTableData.lanthanides.forEachIndexed { index, elem ->
                val cellLeft = startX + index * colStep
                val cellTop = fBlockStartY
                val isSelected = elem == selectedElement
                val isDimmed = hasFilter && elem.atomicNumber !in matchingIds
                drawCell(
                    nativeCanvas = nativeCanvas,
                    elem = elem,
                    left = cellLeft,
                    top = cellTop,
                    width = cellWPx,
                    height = cellHPx,
                    cornerRadius = cornerRadiusPx,
                    borderWidth = borderWidthPx,
                    selectedBorderWidth = selectedBorderWidthPx,
                    isSelected = isSelected,
                    isDimmed = isDimmed,
                    isDarkMode = isDarkMode,
                    zoomScale = zoomScale,
                    density = density.density,
                    bgPaint = bgPaint,
                    borderPaint = borderPaint,
                    numberPaint = numberPaint,
                    symbolPaint = symbolPaint,
                    namePaint = namePaint,
                    massPaint = massPaint
                )
            }

            // 4. Actinides row
            PeriodicTableData.actinides.forEachIndexed { index, elem ->
                val cellLeft = startX + index * colStep
                val cellTop = fBlockStartY + rowStep
                val isSelected = elem == selectedElement
                val isDimmed = hasFilter && elem.atomicNumber !in matchingIds
                drawCell(
                    nativeCanvas = nativeCanvas,
                    elem = elem,
                    left = cellLeft,
                    top = cellTop,
                    width = cellWPx,
                    height = cellHPx,
                    cornerRadius = cornerRadiusPx,
                    borderWidth = borderWidthPx,
                    selectedBorderWidth = selectedBorderWidthPx,
                    isSelected = isSelected,
                    isDimmed = isDimmed,
                    isDarkMode = isDarkMode,
                    zoomScale = zoomScale,
                    density = density.density,
                    bgPaint = bgPaint,
                    borderPaint = borderPaint,
                    numberPaint = numberPaint,
                    symbolPaint = symbolPaint,
                    namePaint = namePaint,
                    massPaint = massPaint
                )
            }
        }
    }
}

private fun drawCell(
    nativeCanvas: android.graphics.Canvas,
    elem: ChemicalElement,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    cornerRadius: Float,
    borderWidth: Float,
    selectedBorderWidth: Float,
    isSelected: Boolean,
    isDimmed: Boolean,
    isDarkMode: Boolean,
    zoomScale: Float,
    density: Float,
    bgPaint: Paint,
    borderPaint: Paint,
    numberPaint: Paint,
    symbolPaint: Paint,
    namePaint: Paint,
    massPaint: Paint
) {
    val cellAlpha = if (isDimmed) 0.25f else 1f
    val baseColor = elem.category.color
    val bgAlpha = if (isDarkMode) {
        if (isSelected) 0.70f else 0.22f * cellAlpha
    } else {
        if (isSelected) 0.35f else 0.12f * cellAlpha
    }
    val borderColor = if (isDarkMode) {
        if (isSelected) Color.White else baseColor.copy(alpha = 0.50f * cellAlpha)
    } else {
        if (isSelected) baseColor else baseColor.copy(alpha = 0.45f * cellAlpha)
    }

    // 1. Draw rounded rect background
    bgPaint.color = baseColor.copy(alpha = bgAlpha).toArgb()
    nativeCanvas.drawRoundRect(left, top, left + width, top + height, cornerRadius, cornerRadius, bgPaint)

    // 2. Draw border
    borderPaint.color = borderColor.toArgb()
    borderPaint.strokeWidth = if (isSelected) selectedBorderWidth else borderWidth
    nativeCanvas.drawRoundRect(left, top, left + width, top + height, cornerRadius, cornerRadius, borderPaint)

    val centerX = left + width / 2f
    val scaleFactor = zoomScale.coerceAtLeast(0.85f)

    // 3. Line 1: Atomic Number
    numberPaint.textSize = 8f * scaleFactor * density
    numberPaint.color = (if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)).copy(alpha = cellAlpha).toArgb()
    val y1 = top + 2.5f * scaleFactor * density + numberPaint.textSize
    nativeCanvas.drawText(elem.atomicNumber.toString(), centerX, y1, numberPaint)

    // 4. Line 2: Symbol
    symbolPaint.textSize = 13f * scaleFactor * density
    symbolPaint.color = (if (isDarkMode) Color.White else Color(0xFF0F172A)).copy(alpha = cellAlpha).toArgb()
    val y2 = top + height * 0.47f
    nativeCanvas.drawText(elem.symbol, centerX, y2, symbolPaint)

    // 5. Line 3: Name
    val nameLength = elem.name.length
    val nameFontSize = when {
        nameLength >= 12 -> 5.8f
        nameLength >= 9 -> 6.5f
        else -> 7.4f
    } * scaleFactor
    namePaint.textSize = nameFontSize * density
    namePaint.color = (if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)).copy(alpha = cellAlpha).toArgb()
    val y3 = top + height * 0.72f

    val maxTextWidth = width - 3f * density
    val displayName = if (namePaint.measureText(elem.name) > maxTextWidth) {
        var trimmed = elem.name
        while (trimmed.isNotEmpty() && namePaint.measureText("$trimmed…") > maxTextWidth) {
            trimmed = trimmed.dropLast(1)
        }
        "$trimmed…"
    } else {
        elem.name
    }
    nativeCanvas.drawText(displayName, centerX, y3, namePaint)

    // 6. Line 4: Mass Number
    massPaint.textSize = 7.5f * scaleFactor * density
    massPaint.color = (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)).copy(alpha = cellAlpha).toArgb()
    val y4 = top + height - 3f * scaleFactor * density
    nativeCanvas.drawText(elem.massNumber.toString(), centerX, y4, massPaint)
}

private fun drawMarker(
    nativeCanvas: android.graphics.Canvas,
    label: String,
    sub: String,
    catName: String,
    categoryColor: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    cornerRadius: Float,
    borderWidth: Float,
    isDarkMode: Boolean,
    zoomScale: Float,
    density: Float,
    bgPaint: Paint,
    borderPaint: Paint,
    numberPaint: Paint,
    symbolPaint: Paint,
    namePaint: Paint
) {
    val bgAlpha = if (isDarkMode) 0.15f else 0.10f
    val borderAlpha = if (isDarkMode) 0.40f else 0.35f

    bgPaint.color = categoryColor.copy(alpha = bgAlpha).toArgb()
    nativeCanvas.drawRoundRect(left, top, left + width, top + height, cornerRadius, cornerRadius, bgPaint)

    borderPaint.color = categoryColor.copy(alpha = borderAlpha).toArgb()
    borderPaint.strokeWidth = borderWidth
    nativeCanvas.drawRoundRect(left, top, left + width, top + height, cornerRadius, cornerRadius, borderPaint)

    val centerX = left + width / 2f
    val scaleFactor = zoomScale.coerceAtLeast(0.85f)

    numberPaint.textSize = 7.5f * scaleFactor * density
    numberPaint.color = categoryColor.toArgb()
    val y1 = top + 2.5f * scaleFactor * density + numberPaint.textSize
    nativeCanvas.drawText(label, centerX, y1, numberPaint)

    symbolPaint.textSize = 11f * scaleFactor * density
    symbolPaint.color = (if (isDarkMode) Color.White else Color(0xFF0F172A)).toArgb()
    val y2 = top + height * 0.48f
    nativeCanvas.drawText(sub, centerX, y2, symbolPaint)

    namePaint.textSize = 6.0f * scaleFactor * density
    namePaint.color = categoryColor.copy(alpha = if (isDarkMode) 0.9f else 1.0f).toArgb()
    val y3 = top + height * 0.75f
    nativeCanvas.drawText(catName, centerX, y3, namePaint)
}

@Composable
private fun SelectedElementCard(element: ChemicalElement, isDarkMode: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) Color(0xFF131824) else Color(0xFFF8FAFC),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Element Symbol Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(element.category.color.copy(alpha = if (isDarkMode) 0.25f else 0.15f))
                    .border(2.dp, element.category.color, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "${element.atomicNumber}",
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        element.symbol,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        "${element.atomicMass}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Element Info Details (Featuring Atomic Number, Mass Number, and Atomic Mass)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        element.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        "(${element.symbol})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(element.category.color.copy(alpha = if (isDarkMode) 0.2f else 0.12f))
                            .border(1.dp, element.category.color.copy(alpha = if (isDarkMode) 0.6f else 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            element.category.displayName,
                            fontSize = 10.sp,
                            color = element.category.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Explicit Atomic Number, Mass Number, and Atomic Mass highlighted
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Atomic Number: ${element.atomicNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                    Text(
                        text = "Mass Number: ${element.massNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669)
                    )
                    Text(
                        text = "Atomic Mass: ${element.atomicMass} u",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Period: ${element.period}", fontSize = 10.5.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                    element.group?.let {
                        Text("Group: $it", fontSize = 10.5.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                    }
                    Text("Phase: ${element.phase}", fontSize = 10.5.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                    Text(
                        "Config: ${element.electronConfig}",
                        fontSize = 10.5.sp,
                        color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (element.summary.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        element.summary,
                        fontSize = 10.5.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
