package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties

enum class MindmapTemplateKind(val title: String, val category: String) {
    BLANK("Custom / Blank", "Custom"),
    RIGHT_ORGANIC("Rightward Radial", "Organic"),
    BILATERAL_CURVED("Bilateral Organic", "Organic"),
    RIGHT_ORTHOGONAL("Rightward Tree", "Orthogonal"),
    BILATERAL_ORTHOGONAL("Bilateral Tree", "Orthogonal"),
    DOWNWARD_ORG("Downward Org Chart", "Hierarchy"),
    DUAL_BRACKET("Dual Bracket", "Tree"),
    DOWNWARD_CURVED("Downward Radial", "Organic")
}

@Composable
fun MindmapTemplateDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (MindmapTemplateKind) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Organic", "Orthogonal", "Hierarchy")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF3F4F6),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .widthIn(max = 840.dp)
                .heightIn(max = 560.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left category indicator sidebar
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val indicatorColors = listOf(
                        Color(0xFF8B5CF6), // Purple
                        Color(0xFF06B6D4), // Cyan
                        Color(0xFF3B82F6), // Blue
                        Color(0xFFEC4899)  // Pink
                    )
                    indicatorColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(10.dp, 28.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(color)
                        )
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 24.dp, top = 20.dp, bottom = 20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.mindmap_template_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = stringResource(R.string.mindmap_template_sub),
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2x4 Template Cards Grid (Matching reference design)
                    val templates = MindmapTemplateKind.values()
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(templates) { template ->
                            TemplateCardItem(
                                template = template,
                                onSelect = { onSelectTemplate(template) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCardItem(
    template: MindmapTemplateKind,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF3F3F46)) // Dark charcoal card from reference image
            .border(1.5.dp, Color(0xFF52525B), RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            when (template) {
                MindmapTemplateKind.BLANK -> {
                    // Central (+) button style card
                    drawCircle(
                        color = Color(0xFF27272A),
                        radius = 28.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    // Draw clean plus
                    val crossHalf = 12.dp.toPx()
                    val strokeW = 3.dp.toPx()
                    drawLine(
                        Color(0xFF9CA3AF),
                        Offset(cx - crossHalf, cy),
                        Offset(cx + crossHalf, cy),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        Color(0xFF9CA3AF),
                        Offset(cx, cy - crossHalf),
                        Offset(cx, cy + crossHalf),
                        strokeWidth = strokeW
                    )
                }

                MindmapTemplateKind.RIGHT_ORGANIC -> {
                    // Central node
                    drawNodePill(Offset(cx - 38.dp.toPx(), cy), 20.dp.toPx(), 10.dp.toPx(), Color(0xFF06B6D4))
                    val start = Offset(cx - 18.dp.toPx(), cy)
                    val colors = listOf(Color(0xFFEAB308), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFEF4444))
                    val yOffsets = listOf(-24.dp.toPx(), -8.dp.toPx(), 8.dp.toPx(), 24.dp.toPx())
                    colors.forEachIndexed { i, c ->
                        val target = Offset(cx + 34.dp.toPx(), cy + yOffsets[i])
                        drawSmoothCurve(start, target, c)
                        drawDashPill(target, 12.dp.toPx(), 4.dp.toPx())
                    }
                }

                MindmapTemplateKind.BILATERAL_CURVED -> {
                    // Central node in center
                    drawNodePill(Offset(cx, cy), 24.dp.toPx(), 11.dp.toPx(), Color(0xFF10B981))
                    // Left branches
                    val leftStart = Offset(cx - 12.dp.toPx(), cy)
                    val rightStart = Offset(cx + 12.dp.toPx(), cy)
                    drawSmoothCurve(leftStart, Offset(cx - 36.dp.toPx(), cy - 16.dp.toPx()), Color(0xFFEAB308))
                    drawDashPill(Offset(cx - 36.dp.toPx(), cy - 16.dp.toPx()), 10.dp.toPx(), 3.dp.toPx())
                    drawSmoothCurve(leftStart, Offset(cx - 36.dp.toPx(), cy + 16.dp.toPx()), Color(0xFFEF4444))
                    drawDashPill(Offset(cx - 36.dp.toPx(), cy + 16.dp.toPx()), 10.dp.toPx(), 3.dp.toPx())

                    // Right branches
                    drawSmoothCurve(rightStart, Offset(cx + 36.dp.toPx(), cy - 16.dp.toPx()), Color(0xFF06B6D4))
                    drawDashPill(Offset(cx + 36.dp.toPx(), cy - 16.dp.toPx()), 10.dp.toPx(), 3.dp.toPx())
                    drawSmoothCurve(rightStart, Offset(cx + 36.dp.toPx(), cy + 16.dp.toPx()), Color(0xFFF97316))
                    drawDashPill(Offset(cx + 36.dp.toPx(), cy + 16.dp.toPx()), 10.dp.toPx(), 3.dp.toPx())
                }

                MindmapTemplateKind.RIGHT_ORTHOGONAL -> {
                    // Central node with stepped branches right
                    drawNodePill(Offset(cx - 38.dp.toPx(), cy), 20.dp.toPx(), 10.dp.toPx(), Color(0xFF06B6D4))
                    val start = Offset(cx - 18.dp.toPx(), cy)
                    val midX = cx
                    drawLine(Color(0xFF06B6D4), start, Offset(midX, cy), strokeWidth = 2.dp.toPx())
                    val colors = listOf(Color(0xFFEAB308), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFEF4444))
                    val yOffsets = listOf(-24.dp.toPx(), -8.dp.toPx(), 8.dp.toPx(), 24.dp.toPx())
                    colors.forEachIndexed { i, c ->
                        val y = cy + yOffsets[i]
                        drawLine(c, Offset(midX, cy), Offset(midX, y), strokeWidth = 2.dp.toPx())
                        drawLine(c, Offset(midX, y), Offset(cx + 34.dp.toPx(), y), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(cx + 34.dp.toPx(), y), 12.dp.toPx(), 4.dp.toPx())
                    }
                }

                MindmapTemplateKind.BILATERAL_ORTHOGONAL -> {
                    // Matching Image 2: Central node with orthogonal branches left and right
                    drawNodePill(Offset(cx, cy), 26.dp.toPx(), 12.dp.toPx(), Color(0xFFD1D5DB))
                    val leftStart = Offset(cx - 13.dp.toPx(), cy)
                    val rightStart = Offset(cx + 13.dp.toPx(), cy)

                    // Left branches (Purple, Cyan, Blue, Pink)
                    val leftMid = cx - 26.dp.toPx()
                    drawLine(Color(0xFFEC4899), leftStart, Offset(leftMid, cy), strokeWidth = 2.dp.toPx())
                    val leftColors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFEC4899))
                    val leftYs = listOf(-22.dp.toPx(), -7.dp.toPx(), 7.dp.toPx(), 22.dp.toPx())
                    leftColors.forEachIndexed { i, c ->
                        val y = cy + leftYs[i]
                        drawLine(c, Offset(leftMid, cy), Offset(leftMid, y), strokeWidth = 2.dp.toPx())
                        drawLine(c, Offset(leftMid, y), Offset(cx - 42.dp.toPx(), y), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(cx - 42.dp.toPx(), y), 12.dp.toPx(), 5.dp.toPx(), borderColor = c)
                    }

                    // Right branches (Cyan, Orange, Orange, Cyan)
                    val rightMid = cx + 26.dp.toPx()
                    drawLine(Color(0xFF06B6D4), rightStart, Offset(rightMid, cy), strokeWidth = 2.dp.toPx())
                    val rightColors = listOf(Color(0xFF06B6D4), Color(0xFFF97316), Color(0xFFF97316), Color(0xFF06B6D4))
                    val rightYs = listOf(-22.dp.toPx(), -7.dp.toPx(), 7.dp.toPx(), 22.dp.toPx())
                    rightColors.forEachIndexed { i, c ->
                        val y = cy + rightYs[i]
                        drawLine(c, Offset(rightMid, cy), Offset(rightMid, y), strokeWidth = 2.dp.toPx())
                        drawLine(c, Offset(rightMid, y), Offset(cx + 42.dp.toPx(), y), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(cx + 42.dp.toPx(), y), 12.dp.toPx(), 5.dp.toPx(), borderColor = c)
                    }
                }

                MindmapTemplateKind.DOWNWARD_ORG -> {
                    // Root at top, stepped tree downwards
                    val rootCenter = Offset(cx, cy - 24.dp.toPx())
                    drawNodePill(rootCenter, 22.dp.toPx(), 10.dp.toPx(), Color(0xFF06B6D4))
                    val midY = cy - 8.dp.toPx()
                    drawLine(Color(0xFF06B6D4), Offset(cx, rootCenter.y + 5.dp.toPx()), Offset(cx, midY), strokeWidth = 2.dp.toPx())
                    val cols = listOf(cx - 30.dp.toPx(), cx - 10.dp.toPx(), cx + 10.dp.toPx(), cx + 30.dp.toPx())
                    val colColors = listOf(Color(0xFFEAB308), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFEF4444))
                    colColors.forEachIndexed { i, c ->
                        val colX = cols[i]
                        drawLine(c, Offset(cx, midY), Offset(colX, midY), strokeWidth = 2.dp.toPx())
                        drawLine(c, Offset(colX, midY), Offset(colX, cy + 8.dp.toPx()), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(colX, cy + 16.dp.toPx()), 8.dp.toPx(), 4.dp.toPx(), borderColor = c)
                        drawLine(c, Offset(colX, cy + 18.dp.toPx()), Offset(colX, cy + 26.dp.toPx()), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(colX, cy + 28.dp.toPx()), 8.dp.toPx(), 4.dp.toPx(), borderColor = c)
                    }
                }

                MindmapTemplateKind.DUAL_BRACKET -> {
                    // Dual sided bracket tree
                    drawNodePill(Offset(cx, cy), 20.dp.toPx(), 10.dp.toPx(), Color(0xFF06B6D4))
                    val leftMid = cx - 20.dp.toPx()
                    val rightMid = cx + 20.dp.toPx()
                    drawLine(Color(0xFF06B6D4), Offset(cx - 10.dp.toPx(), cy), Offset(leftMid, cy), strokeWidth = 2.dp.toPx())
                    drawLine(Color(0xFF06B6D4), Offset(cx + 10.dp.toPx(), cy), Offset(rightMid, cy), strokeWidth = 2.dp.toPx())
                    val yOff = listOf(-16.dp.toPx(), 0f, 16.dp.toPx())
                    yOff.forEach { y ->
                        drawLine(Color(0xFFEAB308), Offset(leftMid, cy + y), Offset(cx - 36.dp.toPx(), cy + y), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(cx - 36.dp.toPx(), cy + y), 10.dp.toPx(), 3.dp.toPx())
                        drawLine(Color(0xFF3B82F6), Offset(rightMid, cy + y), Offset(cx + 36.dp.toPx(), cy + y), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(cx + 36.dp.toPx(), cy + y), 10.dp.toPx(), 3.dp.toPx())
                    }
                }

                MindmapTemplateKind.DOWNWARD_CURVED -> {
                    // Root at top with curved branches downwards
                    val rootCenter = Offset(cx, cy - 24.dp.toPx())
                    drawNodePill(rootCenter, 22.dp.toPx(), 10.dp.toPx(), Color(0xFF06B6D4))
                    val start = Offset(cx, rootCenter.y + 5.dp.toPx())
                    val cols = listOf(cx - 30.dp.toPx(), cx - 10.dp.toPx(), cx + 10.dp.toPx(), cx + 30.dp.toPx())
                    val colColors = listOf(Color(0xFFEAB308), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFEF4444))
                    colColors.forEachIndexed { i, c ->
                        val target = Offset(cols[i], cy + 12.dp.toPx())
                        val p = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(start.x, cy - 6.dp.toPx(), target.x, cy - 6.dp.toPx(), target.x, target.y)
                        }
                        drawPath(p, c, style = ComposeStroke(width = 2.dp.toPx()))
                        drawDashPill(Offset(target.x, cy + 16.dp.toPx()), 8.dp.toPx(), 4.dp.toPx(), borderColor = c)
                        drawLine(c, Offset(target.x, cy + 18.dp.toPx()), Offset(target.x, cy + 26.dp.toPx()), strokeWidth = 2.dp.toPx())
                        drawDashPill(Offset(target.x, cy + 28.dp.toPx()), 8.dp.toPx(), 4.dp.toPx(), borderColor = c)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawNodePill(
    center: Offset,
    w: Float,
    h: Float,
    borderColor: Color
) {
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    drawRoundRect(
        color = Color(0xFF1F2937),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(h / 2f, h / 2f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(h / 2f, h / 2f),
        style = ComposeStroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawDashPill(
    center: Offset,
    w: Float,
    h: Float,
    borderColor: Color = Color(0xFF9CA3AF)
) {
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    drawRoundRect(
        color = Color(0xFF27272A),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(h / 2f, h / 2f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(h / 2f, h / 2f),
        style = ComposeStroke(width = 1.2.dp.toPx())
    )
}

private fun DrawScope.drawSmoothCurve(start: Offset, end: Offset, color: Color) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val cx1 = start.x + (end.x - start.x) * 0.5f
        val cy1 = start.y
        val cx2 = start.x + (end.x - start.x) * 0.5f
        val cy2 = end.y
        cubicTo(cx1, cy1, cx2, cy2, end.x, end.y)
    }
    drawPath(path, color, style = ComposeStroke(width = 2.dp.toPx()))
}
