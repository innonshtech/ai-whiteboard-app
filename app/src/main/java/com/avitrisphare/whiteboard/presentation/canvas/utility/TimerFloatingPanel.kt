package com.avitrisphare.whiteboard.presentation.canvas.utility

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TimerFloatingPanel(
    state: TimerUiState,
    onStateChange: (TimerUiState) -> Unit,
    onBringToFront: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(state.isRunning) }
    var remainingSeconds by remember { mutableIntStateOf(state.remainingSeconds) }
    var totalSeconds by remember { mutableIntStateOf(state.totalSeconds) }
    var isFinished by remember { mutableStateOf(state.isFinished) }

    var isCustomMode by remember { mutableStateOf(false) }
    var customMinInput by remember(totalSeconds) { mutableStateOf((totalSeconds / 60).toString()) }
    var customSecInput by remember(totalSeconds) { mutableStateOf("%02d".format(totalSeconds % 60)) }

    // Synchronize external resets
    LaunchedEffect(state.remainingSeconds, state.isRunning, state.totalSeconds) {
        if (!isRunning && state.isRunning) isRunning = true
        if (state.totalSeconds != totalSeconds) totalSeconds = state.totalSeconds
        if (kotlin.math.abs(state.remainingSeconds - remainingSeconds) > 1 && !isRunning) {
            remainingSeconds = state.remainingSeconds
        }
        isFinished = state.isFinished
    }

    // Accurate hardware clock countdown loop
    LaunchedEffect(isRunning) {
        if (isRunning && remainingSeconds > 0) {
            val targetEndTime = SystemClock.elapsedRealtime() + (remainingSeconds * 1000L)
            while (isRunning) {
                delay(200L)
                val now = SystemClock.elapsedRealtime()
                val millisRemaining = targetEndTime - now
                val secsRemaining = if (millisRemaining <= 0) 0 else ((millisRemaining + 999) / 1000).toInt()

                if (secsRemaining <= 0) {
                    remainingSeconds = 0
                    isRunning = false
                    isFinished = true
                    SoundAlertHelper.playTimerAlert(context, scope)
                    onStateChange(state.copy(remainingSeconds = 0, isRunning = false, isFinished = true))
                    break
                } else if (secsRemaining != remainingSeconds) {
                    remainingSeconds = secsRemaining
                }
            }
        }
    }

    val progress = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    val formattedTime = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)

    FloatingToolPanel(
        title = "Class Timer",
        icon = Icons.Default.HourglassBottom,
        position = state.position,
        isMinimized = state.isMinimized,
        onPositionChange = { onStateChange(state.copy(position = it)) },
        onToggleMinimize = { onStateChange(state.copy(isMinimized = !state.isMinimized)) },
        onClose = {
            isRunning = false
            onStateChange(state.copy(isVisible = false, isRunning = false, remainingSeconds = remainingSeconds))
        },
        minimizedSummary = "Timer: $formattedTime",
        onBringToFront = onBringToFront,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.width(260.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCustomMode) {
                // --- CUSTOM DURATION INPUT VIEW ---
                val parsedMins = customMinInput.toIntOrNull() ?: 0
                val parsedSecs = customSecInput.toIntOrNull() ?: 0

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Set Custom Duration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(8.dp))

                    // Steppers for Minutes and Seconds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minutes column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // - Button (High Visibility)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                        .clickable {
                                            customMinInput = (parsedMins - 1).coerceAtLeast(0).toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Minus 1 min",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = customMinInput,
                                        onValueChange = { newVal ->
                                            if (newVal.length <= 3 && newVal.all { it.isDigit() }) {
                                                customMinInput = newVal
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.width(36.dp)
                                    )
                                }

                                // + Button (High Visibility)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                        .clickable {
                                            customMinInput = (parsedMins + 1).coerceAtMost(999).toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Plus 1 min",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text("min", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                        }

                        Text(
                            ":",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 14.dp)
                        )

                        // Seconds column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // - Button (High Visibility)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                        .clickable {
                                            customSecInput = "%02d".format((parsedSecs - 5).coerceAtLeast(0))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Minus 5 sec",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = customSecInput,
                                        onValueChange = { newVal ->
                                            if (newVal.length <= 2 && newVal.all { it.isDigit() }) {
                                                customSecInput = newVal
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.width(36.dp)
                                    )
                                }

                                // + Button (High Visibility)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                        .clickable {
                                            val next = parsedSecs + 5
                                            if (next >= 60) {
                                                customMinInput = (parsedMins + 1).toString()
                                                customSecInput = "%02d".format(next % 60)
                                            } else {
                                                customSecInput = "%02d".format(next)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Plus 5 sec",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text("sec", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quick increment shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            "+30s" to {
                                val s = parsedSecs + 30
                                if (s >= 60) {
                                    customMinInput = (parsedMins + 1).toString()
                                    customSecInput = "%02d".format(s % 60)
                                } else {
                                    customSecInput = "%02d".format(s)
                                }
                            },
                            "+1m" to { customMinInput = (parsedMins + 1).toString() },
                            "+5m" to { customMinInput = (parsedMins + 5).toString() },
                            "Clear" to {
                                customMinInput = "0"
                                customSecInput = "00"
                            }
                        ).forEach { (label, action) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF243048))
                                    .clickable { action() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, fontSize = 11.sp, color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Cancel & Set Timer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isCustomMode = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A3449),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(stringResource(R.string.cancel), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                val total = (parsedMins * 60 + parsedSecs).coerceAtLeast(1)
                                totalSeconds = total
                                remainingSeconds = total
                                isRunning = false
                                isFinished = false
                                onStateChange(
                                    state.copy(
                                        totalSeconds = total,
                                        remainingSeconds = total,
                                        isRunning = false,
                                        isFinished = false
                                    )
                                )
                                isCustomMode = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.3f).height(38.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.timer_set_timer), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // --- ROUND CLOCK VIEW ---
                val isUrgent = remainingSeconds in 1..30
                val clockColor = when {
                    isFinished -> Color(0xFFEF4444)
                    isUrgent -> Color(0xFFF59E0B)
                    else -> Color(0xFF38BDF8)
                }

                // Round Clock Dial
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.dp, Color(0xFF1E293B), CircleShape)
                        .clickable {
                            customMinInput = (totalSeconds / 60).toString()
                            customSecInput = "%02d".format(totalSeconds % 60)
                            isCustomMode = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Track & Sweep
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(144.dp),
                        color = clockColor,
                        trackColor = Color(0xFF1E293B),
                        strokeWidth = 7.dp
                    )

                    // Centered Digital Readout inside Round Clock
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isFinished) Color(0xFFEF4444) else Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when {
                                isFinished -> stringResource(R.string.timer_finished)
                                isRunning -> "COUNTING DOWN"
                                remainingSeconds < totalSeconds -> "PAUSED"
                                else -> "READY"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFinished) Color(0xFFEF4444) else if (isRunning) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Presets & Custom Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3, 5).forEach { mins ->
                        val total = mins * 60
                        val isSelected = totalSeconds == total && !isFinished
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF222B3D))
                                .clickable {
                                    totalSeconds = total
                                    remainingSeconds = total
                                    isRunning = false
                                    isFinished = false
                                    onStateChange(
                                        state.copy(
                                            totalSeconds = total,
                                            remainingSeconds = total,
                                            isRunning = false,
                                            isFinished = false
                                        )
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "${mins}m",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Custom Trigger
                    val isCustomSelected = listOf(1, 2, 3, 5).none { it * 60 == totalSeconds }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCustomSelected) Color(0xFF2563EB) else Color(0xFF222B3D))
                            .clickable {
                                customMinInput = (totalSeconds / 60).toString()
                                customSecInput = "%02d".format(totalSeconds % 60)
                                isCustomMode = true
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Custom",
                                tint = if (isCustomSelected) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Custom",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustomSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Action Controls: Reset & Play/Pause (Proper padding to prevent text truncation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button (No truncation, clearly visible)
                    Button(
                        onClick = {
                            isRunning = false
                            remainingSeconds = totalSeconds
                            isFinished = false
                            onStateChange(
                                state.copy(
                                    remainingSeconds = totalSeconds,
                                    isRunning = false,
                                    isFinished = false
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A3449),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.timer_reset),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Start / Pause Button (No truncation, clearly visible)
                    Button(
                        onClick = {
                            if (remainingSeconds <= 0) {
                                remainingSeconds = totalSeconds
                                isRunning = true
                                isFinished = false
                                onStateChange(
                                    state.copy(
                                        remainingSeconds = totalSeconds,
                                        isRunning = true,
                                        isFinished = false
                                    )
                                )
                            } else {
                                val nextRunning = !isRunning
                                isRunning = nextRunning
                                isFinished = false
                                onStateChange(
                                    state.copy(
                                        isRunning = nextRunning,
                                        remainingSeconds = remainingSeconds,
                                        isFinished = false
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFF59E0B) else Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.3f).height(40.dp)
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isRunning) stringResource(R.string.timer_pause) else stringResource(R.string.timer_start),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
