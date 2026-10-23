package com.avitrisphare.whiteboard.presentation.canvas.utility

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val PanelBg = Color(0xF5181E2C)
private val PanelBorder = Color(0xFF333E54)
private val HeaderBg = Color(0xFF1E2638)

@Composable
fun FloatingToolPanel(
    title: String,
    icon: ImageVector,
    position: Offset,
    isMinimized: Boolean,
    onPositionChange: (Offset) -> Unit,
    onToggleMinimize: () -> Unit,
    onClose: () -> Unit,
    minimizedSummary: String? = null,
    headerActions: (@Composable () -> Unit)? = null,
    onBringToFront: (() -> Unit)? = null,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val panelBg = if (isDarkMode) PanelBg else Color(0xFFFFFFFF)
    val panelBorder = if (isDarkMode) PanelBorder else Color(0xFFCBD5E1)
    val headerBg = if (isDarkMode) HeaderBg else Color(0xFFF1F5F9)
    val titleColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val actionBtnBg = if (isDarkMode) Color(0xFF283248) else Color(0xFFE2E8F0)
    val actionBtnTint = if (isDarkMode) Color.White else Color(0xFF334155)
    val dragIconTint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .offset { IntOffset(position.x.roundToInt().coerceAtLeast(0), position.y.roundToInt().coerceAtLeast(0)) }
            .wrapContentSize()
            // Bring this tool to top whenever touched, without consuming child events
            .pointerInput(onBringToFront) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            onBringToFront?.invoke()
                        }
                    }
                }
            }
    ) {
        if (isMinimized) {
            // Minimized Floating Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = panelBg,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .wrapContentSize()
                    .border(1.dp, panelBorder, RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onPositionChange(Offset(position.x + dragAmount.x, position.y + dragAmount.y))
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Drag",
                        tint = dragIconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        minimizedSummary ?: title,
                        color = titleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(actionBtnBg)
                            .clickable(onClick = onToggleMinimize),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = actionBtnTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF374151) else Color(0xFFFEE2E2))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            // Expanded Floating Window - perfectly sized to its content
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = panelBg,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .wrapContentSize()
                    .border(1.dp, panelBorder, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { /* consume so touches do not pass through to canvas underneath */ }
                    }
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    // Draggable Title / Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onPositionChange(Offset(position.x + dragAmount.x, position.y + dragAmount.y))
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Drag to reposition",
                                tint = dragIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                title,
                                color = titleColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            headerActions?.invoke()

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(actionBtnBg)
                                    .clickable(onClick = onToggleMinimize),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Minimize",
                                    tint = actionBtnTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0xFF283248) else Color(0xFFFEE2E2))
                                    .clickable(onClick = onClose),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Panel Body Content
                    Box(modifier = Modifier.padding(6.dp)) {
                        content()
                    }
                }
            }
        }
    }
}
