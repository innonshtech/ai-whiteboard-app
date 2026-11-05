package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.R

/**
 * SnipOverlay renders the subtle visual selection boundary on the Canvas
 * and a compact, simplified horizontal toolbar containing only the four universal selection modes:
 * Rectangle, Square, Freeform, and Full Canvas.
 */
@Composable
fun SnipOverlay(
    isActive: Boolean,
    isDarkTheme: Boolean,
    snipMode: SnipMode,
    selectionRect: Rect?,
    freeformPoints: List<Offset>,
    canvasScale: Float,
    canvasOffsetX: Float,
    canvasOffsetY: Float,
    isNavbarVisible: Boolean = true,
    onSetMode: (SnipMode) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val cyanColor = Color(0xFF06B6D4)
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }

    Box(modifier = modifier.fillMaxSize()) {
        // Subtle visual boundary overlay anywhere on the canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val offset = Offset(canvasOffsetX, canvasOffsetY)

            // 1. Subtle selection boundary for RECTANGLE, SQUARE, and FULL_CANVAS
            if (selectionRect != null && (snipMode == SnipMode.RECTANGLE || snipMode == SnipMode.SQUARE || snipMode == SnipMode.FULL_CANVAS)) {
                val screenTL = CanvasTransform.worldToScreen(Offset(selectionRect.left, selectionRect.top), canvasScale, offset)
                val screenW = selectionRect.width * canvasScale
                val screenH = selectionRect.height * canvasScale

                // Subtle translucent fill
                drawRect(
                    color = cyanColor.copy(alpha = 0.12f),
                    topLeft = screenTL,
                    size = Size(screenW, screenH)
                )

                // Subtle dashed border
                drawRect(
                    color = cyanColor,
                    topLeft = screenTL,
                    size = Size(screenW, screenH),
                    style = DrawStroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )
            }

            // 2. Subtle, smooth selection boundary for FREEFORM
            if (freeformPoints.size >= 2 && snipMode == SnipMode.FREEFORM) {
                val screenPoints = freeformPoints.map {
                    CanvasTransform.worldToScreen(it, canvasScale, offset)
                }

                val first = screenPoints.first()
                val last = screenPoints.last()
                val distToStart = (last - first).getDistance()
                // Near-start magnetic snapping threshold (density-aware)
                val maxSnapDist = 32.dp.toPx()
                val isNearStart = screenPoints.size >= 8 && distToStart < maxSnapDist

                // Build smooth curve using quadratic Bezier midpoints (open while actively drawing)
                val smoothPath = Path().apply {
                    moveTo(first.x, first.y)
                    if (screenPoints.size == 2) {
                        lineTo(last.x, last.y)
                    } else {
                        for (i in 1 until screenPoints.size - 1) {
                            val p0 = screenPoints[i]
                            val p1 = screenPoints[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            val midY = (p0.y + p1.y) / 2f
                            quadraticBezierTo(p0.x, p0.y, midX, midY)
                        }
                        lineTo(last.x, last.y)
                    }
                    if (isNearStart) {
                        // Smooth closing curve preview back to the start anchor
                        quadraticBezierTo(
                            (last.x + first.x) / 2f,
                            (last.y + first.y) / 2f,
                            first.x,
                            first.y
                        )
                        close()
                    }
                }

                // If user draws near the start anchor, preview translucent fill
                if (isNearStart) {
                    drawPath(
                        path = smoothPath,
                        color = cyanColor.copy(alpha = 0.12f)
                    )
                }

                // Soft outer glow for high visibility and fluid styling
                drawPath(
                    path = smoothPath,
                    color = cyanColor.copy(alpha = 0.22f),
                    style = DrawStroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Crisp dashed line following the smooth curve
                drawPath(
                    path = smoothPath,
                    color = cyanColor,
                    style = DrawStroke(
                        width = 2.dp.toPx(),
                        pathEffect = dashEffect,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Start-point anchor indicator (lets user know where loop started)
                val anchorRadius = if (isNearStart) 7.dp.toPx() else 5.dp.toPx()
                drawCircle(
                    color = cyanColor.copy(alpha = 0.35f),
                    radius = anchorRadius + 3.dp.toPx(),
                    center = first
                )
                drawCircle(
                    color = if (isNearStart) Color.White else cyanColor,
                    radius = anchorRadius,
                    center = first
                )
                if (isNearStart) {
                    drawCircle(
                        color = cyanColor,
                        radius = anchorRadius - 2.5.dp.toPx(),
                        center = first
                    )
                }

                // Tip indicator dot following active finger / stylus
                drawCircle(
                    color = cyanColor,
                    radius = 3.5.dp.toPx(),
                    center = last
                )
            }
        }

        // Simplified, compact horizontal toolbar with only the 4 selection options
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isNavbarVisible) 96.dp else 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option 1: Rectangle
                    SnipModeButton(
                        icon = Icons.Default.Crop,
                        label = stringResource(R.string.snip_mode_rectangle),
                        selected = snipMode == SnipMode.RECTANGLE,
                        isDark = isDarkTheme,
                        onClick = { onSetMode(SnipMode.RECTANGLE) }
                    )

                    // Option 2: Square
                    SnipModeButton(
                        icon = Icons.Default.CropSquare,
                        label = stringResource(R.string.snip_mode_square),
                        selected = snipMode == SnipMode.SQUARE,
                        isDark = isDarkTheme,
                        onClick = { onSetMode(SnipMode.SQUARE) }
                    )

                    // Option 3: Freeform
                    SnipModeButton(
                        icon = Icons.Default.Gesture,
                        label = stringResource(R.string.snip_mode_freeform),
                        selected = snipMode == SnipMode.FREEFORM,
                        isDark = isDarkTheme,
                        onClick = { onSetMode(SnipMode.FREEFORM) }
                    )

                    // Option 4: Full Canvas
                    SnipModeButton(
                        icon = Icons.Default.Fullscreen,
                        label = stringResource(R.string.snip_mode_full),
                        selected = snipMode == SnipMode.FULL_CANVAS,
                        isDark = isDarkTheme,
                        onClick = { onSetMode(SnipMode.FULL_CANVAS) }
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                    )

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnipModeButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeBg = Color(0xFF06B6D4)
    val inactiveText = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else inactiveText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else inactiveText
        )
    }
}
