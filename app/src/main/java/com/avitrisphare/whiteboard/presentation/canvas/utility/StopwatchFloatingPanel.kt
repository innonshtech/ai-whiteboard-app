package com.avitrisphare.whiteboard.presentation.canvas.utility

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun StopwatchFloatingPanel(
    state: StopwatchUiState,
    onStateChange: (StopwatchUiState) -> Unit,
    onBringToFront: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Local state for smooth, glitch-free UI updates without parent recomposition churn
    var isRunning by remember { mutableStateOf(state.isRunning) }
    var elapsedMillis by remember { mutableLongStateOf(state.elapsedMillis) }

    // Synchronize if state externally reset or restored
    LaunchedEffect(state.isRunning) {
        if (state.isRunning != isRunning) {
            isRunning = state.isRunning
        }
    }
    LaunchedEffect(state.elapsedMillis) {
        if (kotlin.math.abs(state.elapsedMillis - elapsedMillis) > 100L) {
            elapsedMillis = state.elapsedMillis
        }
    }

    // Monotonic hardware clock ticker
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val baseTime = SystemClock.elapsedRealtime() - elapsedMillis
            while (isRunning) {
                delay(33L) // ~30 fps
                elapsedMillis = SystemClock.elapsedRealtime() - baseTime
            }
        }
    }

    val totalSecs = elapsedMillis / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hundredths = (elapsedMillis % 1000) / 10
    val formattedTime = "%02d:%02d.%02d".format(mins, secs, hundredths)

    FloatingToolPanel(
        title = "Stopwatch",
        icon = Icons.Default.Timer,
        position = state.position,
        isMinimized = state.isMinimized,
        onPositionChange = { onStateChange(state.copy(position = it)) },
        onToggleMinimize = { onStateChange(state.copy(isMinimized = !state.isMinimized)) },
        onClose = {
            isRunning = false
            onStateChange(state.copy(isVisible = false, isRunning = false, elapsedMillis = elapsedMillis))
        },
        minimizedSummary = "Stopwatch: $formattedTime",
        onBringToFront = onBringToFront,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.width(210.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-visibility compact digital display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRunning) Color(0xFF38BDF8) else Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isRunning) "RUNNING" else if (elapsedMillis > 0L) "PAUSED" else "READY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action Buttons: Reset & Start/Stop (Proper padding to prevent truncation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                Button(
                    onClick = {
                        isRunning = false
                        elapsedMillis = 0L
                        onStateChange(state.copy(isRunning = false, elapsedMillis = 0L))
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A3449),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset Stopwatch",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Reset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Primary Start/Stop Button
                Button(
                    onClick = {
                        val nextRunning = !isRunning
                        isRunning = nextRunning
                        onStateChange(state.copy(isRunning = nextRunning, elapsedMillis = elapsedMillis))
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(40.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Stop" else "Start",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isRunning) "Stop" else "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
