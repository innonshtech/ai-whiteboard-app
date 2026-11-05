package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.R
import kotlin.math.roundToInt

data class CurtainUiState(
    val isVisible: Boolean = false,
    val isFullScreen: Boolean = false,
    val leftFrac: Float = 0.22f,
    val topFrac: Float = 0.20f,
    val rightFrac: Float = 0.78f,
    val bottomFrac: Float = 0.80f
)

/**
 * Clean, user-friendly Curtain / Screen Shade overlay.
 * Provides a movable and resizable shade box with 4 corner dots and 4 middle edge dots
 * so teachers can easily hide any specific area or the entire canvas.
 * Touches outside the box pass through to the canvas beneath.
 */
@Composable
fun CurtainOverlay(
    state: CurtainUiState,
    onStateChange: (CurtainUiState) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val density = LocalDensity.current
    val currentState by rememberUpdatedState(state)
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerW = constraints.maxWidth.toFloat().coerceAtLeast(100f)
        val containerH = constraints.maxHeight.toFloat().coerceAtLeast(100f)

        val minWPx = with(density) { 140.dp.toPx() }
        val minHPx = with(density) { 100.dp.toPx() }

        val minWFrac = (minWPx / containerW).coerceIn(0.05f, 0.4f)
        val minHFrac = (minHPx / containerH).coerceIn(0.05f, 0.4f)

        val curLeftFrac = if (state.isFullScreen) 0f else state.leftFrac.coerceIn(0f, 1f - minWFrac)
        val curTopFrac = if (state.isFullScreen) 0f else state.topFrac.coerceIn(0f, 1f - minHFrac)

        val minRight = (curLeftFrac + minWFrac).coerceAtMost(1f)
        val minBottom = (curTopFrac + minHFrac).coerceAtMost(1f)

        val curRightFrac = if (state.isFullScreen) 1f else state.rightFrac.coerceIn(minRight, 1f)
        val curBottomFrac = if (state.isFullScreen) 1f else state.bottomFrac.coerceIn(minBottom, 1f)

        val boxLeftPx = curLeftFrac * containerW
        val boxTopPx = curTopFrac * containerH
        val boxWidthPx = ((curRightFrac - curLeftFrac) * containerW).coerceAtLeast(minWPx)
        val boxHeightPx = ((curBottomFrac - curTopFrac) * containerH).coerceAtLeast(minHPx)

        val boxWidthDp = with(density) { boxWidthPx.toDp() }
        val boxHeightDp = with(density) { boxHeightPx.toDp() }

        val cornerRadius = if (state.isFullScreen) 0.dp else 14.dp

        // Curtain Box
        Box(
            modifier = Modifier
                .offset { IntOffset(boxLeftPx.roundToInt(), boxTopPx.roundToInt()) }
                .size(boxWidthDp, boxHeightDp)
                .shadow(elevation = if (state.isFullScreen) 0.dp else 16.dp, shape = RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF181E29))
                .then(
                    if (!state.isFullScreen) {
                        Modifier.border(2.5.dp, Color(0xFFF26A1B), RoundedCornerShape(cornerRadius))
                    } else Modifier
                )
                .pointerInput(state.isFullScreen) {
                    if (!state.isFullScreen) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dxFrac = dragAmount.x / containerW
                            val dyFrac = dragAmount.y / containerH

                            val s = currentState
                            val spanW = (s.rightFrac - s.leftFrac).coerceIn(minWFrac, 1f)
                            val spanH = (s.bottomFrac - s.topFrac).coerceIn(minHFrac, 1f)

                            val newLeft = (s.leftFrac + dxFrac).coerceIn(0f, 1f - spanW)
                            val newTop = (s.topFrac + dyFrac).coerceIn(0f, 1f - spanH)

                            currentOnStateChange(
                                s.copy(
                                    leftFrac = newLeft,
                                    topFrac = newTop,
                                    rightFrac = newLeft + spanW,
                                    bottomFrac = newTop + spanH
                                )
                            )
                        }
                    } else {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            // Subtle vertical blind pleats pattern
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // 100% solid opaque base
                drawRect(color = Color(0xFF181E29))
                // 100% solid opaque vertical blind pleats pattern
                val pleatW = with(density) { 18.dp.toPx() }
                var x = 0f
                var alt = false
                while (x < w) {
                    if (alt) {
                        drawRect(
                            color = Color(0xFF222A3A),
                            topLeft = Offset(x, 0f),
                            size = Size(pleatW / 2f, h)
                        )
                    }
                    x += pleatW / 2f
                    alt = !alt
                }
            }

            // Top Header Bar inside Curtain Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Curtain Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Curtains,
                        contentDescription = null,
                        tint = Color(0xFFF26A1B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.curtain_title),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action Controls: Hide All / Show Area Toggle & Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hide Canvas / Show Area toggle button
                    Button(
                        onClick = {
                            currentOnStateChange(state.copy(isFullScreen = !state.isFullScreen))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isFullScreen) Color(0xFF10B981) else Color(0xFF2E2466),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isFullScreen) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (state.isFullScreen) stringResource(R.string.curtain_show) else stringResource(R.string.curtain_hide),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Close (✕) Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x40000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Full Screen Center Notice Card (Visible when fullscreen)
            if (state.isFullScreen) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                    border = BorderStroke(1.5.dp, Color(0xFFF26A1B).copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFF26A1B).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFF26A1B),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.curtain_hidden_notice),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.curtain_reveal_hint),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                currentOnStateChange(state.copy(isFullScreen = false))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF26A1B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.curtain_show), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Center "Drag to move" Pill (Visible when not fullscreen)
            if (!state.isFullScreen) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0x991E2430), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = stringResource(R.string.curtain_drag_to_move),
                            tint = Color(0xFFF26A1B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.curtain_drag_to_move),
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4 Corner Dots & 4 Middle Edge Dots (Active when not in full-screen mode)
        if (!state.isFullScreen) {
            val dotSize = 26.dp
            val hitSize = 48.dp

            // 1) Top-Left Corner Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx, boxTopPx),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { dx, dy ->
                    val s = currentState
                    val dxFrac = dx / containerW
                    val dyFrac = dy / containerH
                    val newLeft = (s.leftFrac + dxFrac).coerceIn(0f, s.rightFrac - minWFrac)
                    val newTop = (s.topFrac + dyFrac).coerceIn(0f, s.bottomFrac - minHFrac)
                    currentOnStateChange(s.copy(leftFrac = newLeft, topFrac = newTop))
                }
            )

            // 2) Top-Right Corner Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx + boxWidthPx, boxTopPx),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { dx, dy ->
                    val s = currentState
                    val dxFrac = dx / containerW
                    val dyFrac = dy / containerH
                    val newRight = (s.rightFrac + dxFrac).coerceIn(s.leftFrac + minWFrac, 1f)
                    val newTop = (s.topFrac + dyFrac).coerceIn(0f, s.bottomFrac - minHFrac)
                    currentOnStateChange(s.copy(rightFrac = newRight, topFrac = newTop))
                }
            )

            // 3) Bottom-Left Corner Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx, boxTopPx + boxHeightPx),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { dx, dy ->
                    val s = currentState
                    val dxFrac = dx / containerW
                    val dyFrac = dy / containerH
                    val newLeft = (s.leftFrac + dxFrac).coerceIn(0f, s.rightFrac - minWFrac)
                    val newBottom = (s.bottomFrac + dyFrac).coerceIn(s.topFrac + minHFrac, 1f)
                    currentOnStateChange(s.copy(leftFrac = newLeft, bottomFrac = newBottom))
                }
            )

            // 4) Bottom-Right Corner Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx + boxWidthPx, boxTopPx + boxHeightPx),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { dx, dy ->
                    val s = currentState
                    val dxFrac = dx / containerW
                    val dyFrac = dy / containerH
                    val newRight = (s.rightFrac + dxFrac).coerceIn(s.leftFrac + minWFrac, 1f)
                    val newBottom = (s.bottomFrac + dyFrac).coerceIn(s.topFrac + minHFrac, 1f)
                    currentOnStateChange(s.copy(rightFrac = newRight, bottomFrac = newBottom))
                }
            )

            // 5) Top-Middle Edge Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx + boxWidthPx / 2f, boxTopPx),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { _, dy ->
                    val s = currentState
                    val dyFrac = dy / containerH
                    val newTop = (s.topFrac + dyFrac).coerceIn(0f, s.bottomFrac - minHFrac)
                    currentOnStateChange(s.copy(topFrac = newTop))
                }
            )

            // 6) Bottom-Middle Edge Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx + boxWidthPx / 2f, boxTopPx + boxHeightPx),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { _, dy ->
                    val s = currentState
                    val dyFrac = dy / containerH
                    val newBottom = (s.bottomFrac + dyFrac).coerceIn(s.topFrac + minHFrac, 1f)
                    currentOnStateChange(s.copy(bottomFrac = newBottom))
                }
            )

            // 7) Left-Middle Edge Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx, boxTopPx + boxHeightPx / 2f),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { dx, _ ->
                    val s = currentState
                    val dxFrac = dx / containerW
                    val newLeft = (s.leftFrac + dxFrac).coerceIn(0f, s.rightFrac - minWFrac)
                    currentOnStateChange(s.copy(leftFrac = newLeft))
                }
            )

            // 8) Right-Middle Edge Dot
            CurtainHandleDot(
                centerOffset = Offset(boxLeftPx + boxWidthPx, boxTopPx + boxHeightPx / 2f),
                sizeDp = dotSize,
                touchTargetDp = hitSize,
                onDrag = { dx, _ ->
                    val s = currentState
                    val dxFrac = dx / containerW
                    val newRight = (s.rightFrac + dxFrac).coerceIn(s.leftFrac + minWFrac, 1f)
                    currentOnStateChange(s.copy(rightFrac = newRight))
                }
            )
        }
    }
}

/**
 * Handle dot placed on corners and middle edges with generous touch target for easy moving/resizing.
 */
@Composable
private fun CurtainHandleDot(
    centerOffset: Offset,
    sizeDp: Dp = 26.dp,
    touchTargetDp: Dp = 48.dp,
    onDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val halfTouchPx = with(density) { (touchTargetDp / 2).toPx() }
    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerOffset.x - halfTouchPx).roundToInt(),
                    (centerOffset.y - halfTouchPx).roundToInt()
                )
            }
            .size(touchTargetDp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // High-contrast circular handle with brand styling
        Box(
            modifier = Modifier
                .size(sizeDp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
                .border(2.5.dp, Color(0xFFF26A1B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Core accent dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFF26A1B), CircleShape)
            )
        }
    }
}
