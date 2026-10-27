package com.avitrisphare.whiteboard.presentation.canvas

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType

@Composable
fun MindmapNodeToolbar(
    node: Stroke,
    maxAllowedWidth: Dp = 680.dp,
    onStartWriting: () -> Unit,
    onStartTyping: () -> Unit,
    onFillColorChanged: (Int) -> Unit,
    onOutlineColorChanged: (Int) -> Unit,
    onOutlineWidthChanged: (Float) -> Unit,
    onShapeChanged: (StrokeType) -> Unit,
    isLineConnecting: Boolean = false,
    onStartLineConnection: () -> Unit,
    onWireStyleToggle: () -> Unit,
    onLayoutToggle: (Boolean) -> Unit,
    onLayerReorder: (String) -> Unit,
    onLockToggle: () -> Unit,
    onSelectWholeDiagram: () -> Unit = {},
    onInsertImage: () -> Unit,
    onOpenStickers: () -> Unit,
    onDelete: () -> Unit
) {
    var activePopover by remember { mutableStateOf<String?>(null) }
    var activeAction by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // Active Sub-popover (Fill, Layout, Outline, Shape)
        when (activePopover) {
            "fill" -> {
                FillColorPopover(
                    currentColor = node.fillColor,
                    onColorSelected = {
                        onFillColorChanged(it)
                        activePopover = null
                    },
                    onClose = { activePopover = null }
                )
                Spacer(Modifier.height(8.dp))
            }
            "layout" -> {
                LayoutPopover(
                    onSelect = { isHorizontal ->
                        onLayoutToggle(isHorizontal)
                        activePopover = null
                    },
                    onClose = { activePopover = null }
                )
                Spacer(Modifier.height(8.dp))
            }
            "outline" -> {
                OutlinePopover(
                    currentColor = node.colorValue,
                    currentWidth = node.strokeWidth,
                    onColorSelected = { onOutlineColorChanged(it) },
                    onWidthSelected = { onOutlineWidthChanged(it) },
                    onClose = { activePopover = null }
                )
                Spacer(Modifier.height(8.dp))
            }
            "shape" -> {
                ShapeSelectPopover(
                    currentType = node.strokeType,
                    onSelect = {
                        onShapeChanged(it)
                        activePopover = null
                    },
                    onClose = { activePopover = null }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Floating Capsule Bar with smooth horizontal sliding when overflowing
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .height(60.dp)
                .widthIn(max = maxAllowedWidth)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                ToolbarButton(
                    icon = Icons.Default.NearMe,
                    label = "Interact",
                    highlight = activeAction == "interact",
                    onClick = {
                        activeAction = "interact"
                        activePopover = null
                        onSelectWholeDiagram()
                    }
                )

                ToolbarButton(
                    icon = Icons.Default.AccountTree,
                    label = "Layout",
                    highlight = activePopover == "layout",
                    onClick = {
                        activeAction = "layout"
                        activePopover = if (activePopover == "layout") null else "layout"
                    }
                )

                ToolbarButton(
                    icon = Icons.Default.Timeline,
                    label = "Line",
                    highlight = isLineConnecting || activeAction == "line",
                    onClick = {
                        activeAction = "line"
                        activePopover = null
                        onStartLineConnection()
                    }
                )

                ToolbarButton(
                    icon = Icons.Default.Straight,
                    label = "Connector",
                    highlight = activeAction == "connector",
                    onClick = {
                        activeAction = "connector"
                        activePopover = null
                        onWireStyleToggle()
                    }
                )

                ToolbarButton(
                    icon = when (node.strokeType) {
                        StrokeType.CIRCLE -> Icons.Default.RadioButtonUnchecked
                        StrokeType.DIAMOND -> Icons.Default.Diamond
                        else -> Icons.Default.CropSquare
                    },
                    label = "Shape",
                    highlight = activePopover == "shape",
                    onClick = {
                        activeAction = "shape"
                        activePopover = if (activePopover == "shape") null else "shape"
                    }
                )

                ToolbarButton(
                    icon = Icons.Default.Edit,
                    label = "Outline",
                    highlight = activePopover == "outline",
                    onClick = {
                        activeAction = "outline"
                        activePopover = if (activePopover == "outline") null else "outline"
                    }
                )

                ToolbarButton(
                    icon = Icons.Default.FormatColorFill,
                    label = "Fill",
                    highlight = activePopover == "fill",
                    onClick = {
                        activeAction = "fill"
                        activePopover = if (activePopover == "fill") null else "fill"
                    }
                )

                // Dedicated Teacher Pen Writing inside node
                ToolbarButton(
                    icon = Icons.Default.Edit,
                    label = "Pen",
                    highlight = activeAction == "pen",
                    onClick = {
                        activeAction = "pen"
                        activePopover = null
                        onStartWriting()
                    }
                )

                // Dedicated Keyboard Text Typing
                ToolbarButton(
                    icon = Icons.Default.Keyboard,
                    label = "Type",
                    highlight = activeAction == "type",
                    onClick = {
                        activeAction = "type"
                        activePopover = null
                        onStartTyping()
                    }
                )

                ToolbarButton(
                    icon = Icons.Default.Image,
                    label = "Image",
                    highlight = activeAction == "image",
                    onClick = {
                        activeAction = "image"
                        activePopover = null
                        onInsertImage()
                    }
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFE5E7EB))
                )

                ToolbarButton(
                    icon = Icons.Default.ThumbUp,
                    label = "React",
                    highlight = activeAction == "react",
                    onClick = {
                        activeAction = "react"
                        activePopover = null
                        onOpenStickers()
                    }
                )

                val isNodeLocked = node.stickerKind.contains(":locked")
                ToolbarButton(
                    icon = if (isNodeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    label = if (isNodeLocked) "Unlock" else "Lock",
                    highlight = isNodeLocked,
                    onClick = onLockToggle
                )

                ToolbarButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    tint = Color(0xFFEF4444),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    highlight: Boolean = false,
    tint: Color = Color(0xFF374151),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) Color(0xFFEFF6FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlight) Color(0xFF2563EB) else tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = if (highlight) Color(0xFF2563EB) else Color(0xFF6B7280),
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Fill Color Popover (Matching Reference Image 5)
 */
@Composable
fun FillColorPopover(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Bucket (solid), 1: No-fill, 2: Palette
    var pendingColor by remember { mutableStateOf(currentColor) }

    // 4 rows x 8 columns curated palette matching Image 5
    val palette = listOf(
        // Row 1: Reds, Oranges, Browns
        0xFFEF4444.toInt(), 0xFFDC2626.toInt(), 0xFFB91C1C.toInt(), 0xFFF97316.toInt(), 0xFFD97706.toInt(), 0xFF92400E.toInt(), 0xFF78350F.toInt(), 0xFF451A03.toInt(),
        // Row 2: Yellows, Light Greens, Greens
        0xFFFEF08A.toInt(), 0xFFFACC15.toInt(), 0xFFEAB308.toInt(), 0xFFA3E635.toInt(), 0xFF84CC16.toInt(), 0xFF22C55E.toInt(), 0xFF16A34A.toInt(), 0xFF15803D.toInt(),
        // Row 3: Cyans, Blues, Dark Blues
        0xFFBAE6FD.toInt(), 0xFF38BDF8.toInt(), 0xFF06B6D4.toInt(), 0xFF60A5FA.toInt(), 0xFF3B82F6.toInt(), 0xFF2563EB.toInt(), 0xFF1D4ED8.toInt(), 0xFF1E3A8A.toInt(),
        // Row 4: Pinks, Purples, Greys, Black/White
        0xFFF472B6.toInt(), 0xFFEC4899.toInt(), 0xFFA855F7.toInt(), 0xFF9333EA.toInt(), 0xFFFFFFFF.toInt(), 0xFF9CA3AF.toInt(), 0xFF4B5563.toInt(), 0xFF000000.toInt()
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF3F4F6),
        shadowElevation = 14.dp,
        modifier = Modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Tab icons (Bucket, No-fill, Palette)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) Color.White else Color.Transparent)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FormatPaint, contentDescription = "Solid", tint = Color(0xFF1F2937), modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) Color.White else Color.Transparent)
                        .clickable {
                            selectedTab = 1
                            pendingColor = 0 // Transparent
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⊘", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Color Grid (4 rows x 8 cols)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                palette.chunked(8).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { colorVal ->
                            val isSelected = pendingColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.Black else Color(0x33000000),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        pendingColor = colorVal
                                        selectedTab = 0
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Black "Select" button matching Image 5
            Button(
                onClick = { onColorSelected(pendingColor) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Select", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

/**
 * Layers Popover (Matching Reference Image 6)
 */
@Composable
fun LayersPopover(
    onAction: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "top" to "Top",
                "up" to "Up",
                "down" to "Down",
                "bottom" to "Bottom"
            ).forEach { (action, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { onAction(action) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF94A3B8), RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Layout Popover (Matching Reference Image 7: Horizontal vs Vertical tree)
 */
@Composable
fun LayoutPopover(
    onSelect: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .clickable { onSelect(true) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = "Horizontal", tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(4.dp))
                Text("Horizontal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .clickable { onSelect(false) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Straight, contentDescription = "Vertical", tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(4.dp))
                Text("Vertical", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            }
        }
    }
}

@Composable
fun OutlinePopover(
    currentColor: Int,
    currentWidth: Float,
    onColorSelected: (Int) -> Unit,
    onWidthSelected: (Float) -> Unit,
    onClose: () -> Unit
) {
    val colors = listOf(
        0xFF1E293B.toInt(), 0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt(),
        0xFF3B82F6.toInt(), 0xFFEC4899.toInt(), 0xFFF97316.toInt(), 0xFF10B981.toInt()
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 14.dp,
        modifier = Modifier.width(240.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Border Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (currentColor == c) 2.5.dp else 1.dp, Color.Black, CircleShape)
                            .clickable { onColorSelected(c) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Thickness: ${currentWidth.toInt()}px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Slider(
                value = currentWidth.coerceIn(1f, 16f),
                onValueChange = onWidthSelected,
                valueRange = 1f..16f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF2563EB), activeTrackColor = Color(0xFF2563EB))
            )
        }
    }
}

@Composable
fun ShapeSelectPopover(
    currentType: StrokeType,
    onSelect: (StrokeType) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                StrokeType.ROUND_RECT to "Rounded",
                StrokeType.RECTANGLE to "Square",
                StrokeType.CIRCLE to "Circle",
                StrokeType.DIAMOND to "Diamond"
            ).forEach { (type, name) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (currentType == type) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                        .clickable { onSelect(type) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937))
                }
            }
        }
    }
}

/**
 * Dedicated Floating Capsule Toolbar for Connection Lines / Wires
 */
@Composable
fun MindmapConnectorToolbar(
    connector: Stroke,
    onToggleStyle: () -> Unit,
    onColorChanged: (Int) -> Unit,
    onWidthChanged: (Float) -> Unit,
    onDelete: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        if (showColorPicker) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val palette = listOf(
                        0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt(), 0xFF3B82F6.toInt(),
                        0xFFEC4899.toInt(), 0xFFF97316.toInt(), 0xFF10B981.toInt(),
                        0xFFEF4444.toInt(), 0xFF1E293B.toInt()
                    )
                    palette.forEach { colorVal ->
                        val isSel = connector.colorValue == colorVal
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) Color.Black else Color(0x33000000),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onColorChanged(colorVal)
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier.height(52.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val styleLabel = when (connector.stickerKind) {
                    "curved" -> "Curved"
                    "straight" -> "Straight"
                    else -> "Orthogonal"
                }
                ToolbarButton(
                    icon = Icons.Default.Timeline,
                    label = styleLabel,
                    highlight = true,
                    onClick = onToggleStyle
                )

                ToolbarButton(
                    icon = Icons.Default.FormatColorFill,
                    label = "Color",
                    onClick = { showColorPicker = !showColorPicker }
                )

                ToolbarButton(
                    icon = Icons.Default.Straight,
                    label = "${connector.strokeWidth.toInt().coerceAtLeast(2)}px",
                    onClick = {
                        val nextW = when {
                            connector.strokeWidth < 4f -> 5f
                            connector.strokeWidth < 7f -> 8f
                            else -> 3f
                        }
                        onWidthChanged(nextW)
                    }
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFFE5E7EB))
                )

                ToolbarButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    onClick = onDelete
                )
            }
        }
    }
}
