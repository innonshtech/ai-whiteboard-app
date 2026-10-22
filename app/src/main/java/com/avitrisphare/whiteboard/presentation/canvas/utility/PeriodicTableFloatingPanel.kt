package com.avitrisphare.whiteboard.presentation.canvas.utility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                // Light / Dark Mode Toggle Pill
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

            // Base cell sizes calibrated so that all 4 lines (Number, Symbol, Name, Mass) have generous breathing room and zero cutting
            val baseCellWidth = 50.dp
            val baseCellHeight = 58.dp
            val cellW = baseCellWidth * zoomScale
            val cellH = baseCellHeight * zoomScale
            val gap = 2.5.dp * zoomScale

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

            Box(modifier = scrollModifier) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    // Periods 1 to 7
                    for (period in 1..7) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (group in 1..18) {
                                val elem = PeriodicTableData.elements.firstOrNull { it.period == period && it.group == group }
                                if (elem != null) {
                                    ElementCell(
                                        element = elem,
                                        cellWidth = cellW,
                                        cellHeight = cellH,
                                        zoomScale = zoomScale,
                                        isSelected = elem == selectedElement,
                                        isDimmed = !matchesFilter(elem, state.searchQuery, state.selectedCategory),
                                        isDarkMode = state.isDarkMode,
                                        onClick = { onStateChange(state.copy(selectedElement = elem)) }
                                    )
                                } else {
                                    if (period == 6 && group == 3) {
                                        MarkerCell("57-71", "La-Lu", "Lanthanide", cellW, cellH, zoomScale, ElementCategory.LANTHANIDE.color, state.isDarkMode)
                                    } else if (period == 7 && group == 3) {
                                        MarkerCell("89-103", "Ac-Lr", "Actinide", cellW, cellH, zoomScale, ElementCategory.ACTINIDE.color, state.isDarkMode)
                                    } else {
                                        Spacer(Modifier.size(width = cellW, height = cellH))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(gap * 1.5f))
                    Divider(color = if (state.isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0), thickness = 1.dp)
                    Spacer(Modifier.height(gap))

                    // Lanthanides Row (57-71)
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Spacer(Modifier.size(width = (cellW * 2) + (gap * 2), height = cellH))
                        PeriodicTableData.elements.filter { it.category == ElementCategory.LANTHANIDE }.forEach { elem ->
                            ElementCell(
                                element = elem,
                                cellWidth = cellW,
                                cellHeight = cellH,
                                zoomScale = zoomScale,
                                isSelected = elem == selectedElement,
                                isDimmed = !matchesFilter(elem, state.searchQuery, state.selectedCategory),
                                isDarkMode = state.isDarkMode,
                                onClick = { onStateChange(state.copy(selectedElement = elem)) }
                            )
                        }
                    }

                    // Actinides Row (89-103)
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Spacer(Modifier.size(width = (cellW * 2) + (gap * 2), height = cellH))
                        PeriodicTableData.elements.filter { it.category == ElementCategory.ACTINIDE }.forEach { elem ->
                            ElementCell(
                                element = elem,
                                cellWidth = cellW,
                                cellHeight = cellH,
                                zoomScale = zoomScale,
                                isSelected = elem == selectedElement,
                                isDimmed = !matchesFilter(elem, state.searchQuery, state.selectedCategory),
                                isDarkMode = state.isDarkMode,
                                onClick = { onStateChange(state.copy(selectedElement = elem)) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Inspector Detail Card for Selected Element (With Atomic Number, Mass Number clearly shown)
            SelectedElementCard(element = selectedElement, isDarkMode = state.isDarkMode)
        }
    }
}

private fun matchesFilter(elem: ChemicalElement, query: String, category: ElementCategory?): Boolean {
    val q = query.trim().lowercase()
    val matchesQuery = q.isEmpty() ||
        elem.symbol.lowercase().contains(q) ||
        elem.name.lowercase().contains(q) ||
        elem.atomicNumber.toString() == q
    val matchesCategory = category == null || elem.category == category
    return matchesQuery && matchesCategory
}

@Composable
private fun ElementCell(
    element: ChemicalElement,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    zoomScale: Float,
    isSelected: Boolean,
    isDimmed: Boolean,
    isDarkMode: Boolean = true,
    onClick: () -> Unit
) {
    val cellAlpha = if (isDimmed) 0.25f else 1f
    val baseColor = element.category.color

    val cellBg = if (isDarkMode) {
        baseColor.copy(alpha = if (isSelected) 0.7f else 0.22f * cellAlpha)
    } else {
        if (isSelected) baseColor.copy(alpha = 0.35f) else baseColor.copy(alpha = 0.12f * cellAlpha)
    }

    val cellBorder = if (isDarkMode) {
        if (isSelected) Color.White else baseColor.copy(alpha = 0.5f * cellAlpha)
    } else {
        if (isSelected) baseColor else baseColor.copy(alpha = 0.45f * cellAlpha)
    }

    Box(
        modifier = Modifier
            .size(width = cellWidth, height = cellHeight)
            .clip(RoundedCornerShape(5.dp))
            .background(cellBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = cellBorder,
                shape = RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Line 1: Atomic Number
            Text(
                text = "${element.atomicNumber}",
                fontSize = (8f * zoomScale.coerceAtLeast(0.85f)).sp,
                color = (if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)).copy(alpha = cellAlpha),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (8.5f * zoomScale.coerceAtLeast(0.85f)).sp,
                maxLines = 1
            )

            // Line 2: Chemical Symbol
            Text(
                text = element.symbol,
                fontSize = (13f * zoomScale.coerceAtLeast(0.85f)).sp,
                fontWeight = FontWeight.Bold,
                color = (if (isDarkMode) Color.White else Color(0xFF0F172A)).copy(alpha = cellAlpha),
                lineHeight = (13.5f * zoomScale.coerceAtLeast(0.85f)).sp,
                maxLines = 1
            )

            // Line 3: Element Name (Dynamic scaling ensures zero truncation/cutting)
            val nameLength = element.name.length
            val nameFontSize = when {
                nameLength >= 12 -> 5.8f
                nameLength >= 9 -> 6.5f
                else -> 7.4f
            } * zoomScale.coerceAtLeast(0.85f)

            Text(
                text = element.name,
                fontSize = nameFontSize.sp,
                fontWeight = FontWeight.Medium,
                color = (if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)).copy(alpha = cellAlpha),
                textAlign = TextAlign.Center,
                lineHeight = (nameFontSize + 1f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Line 4: Mass Number
            Text(
                text = "${element.massNumber}",
                fontSize = (7.5f * zoomScale.coerceAtLeast(0.85f)).sp,
                color = (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)).copy(alpha = cellAlpha),
                fontFamily = FontFamily.Monospace,
                lineHeight = (8f * zoomScale.coerceAtLeast(0.85f)).sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MarkerCell(
    label: String,
    sub: String,
    categoryName: String,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    zoomScale: Float,
    color: Color,
    isDarkMode: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(width = cellWidth, height = cellHeight)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = if (isDarkMode) 0.15f else 0.10f))
            .border(1.dp, color.copy(alpha = if (isDarkMode) 0.4f else 0.35f), RoundedCornerShape(5.dp))
            .padding(horizontal = 1.5.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = (7.5f * zoomScale.coerceAtLeast(0.85f)).sp,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = sub,
                fontSize = (11f * zoomScale.coerceAtLeast(0.85f)).sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = categoryName,
                fontSize = (6.0f * zoomScale.coerceAtLeast(0.85f)).sp,
                color = color.copy(alpha = if (isDarkMode) 0.9f else 1.0f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun SelectedElementCard(element: ChemicalElement, isDarkMode: Boolean = true) {
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
