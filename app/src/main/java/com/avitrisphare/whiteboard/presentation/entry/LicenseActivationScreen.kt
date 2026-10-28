package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.data.remote.LicenseCheckResult
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.VerifiedLicense
import kotlinx.coroutines.launch

private val keyboardRows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M", "⌫")
)

@Composable
fun LicenseActivationScreen(
    panelApi: PanelApi,
    panelId: String,
    deviceUuid: String = "GYSH-PNL-" + panelId.takeLast(4).uppercase(),
    onLicenseExpired: ((LicenseCheckResult.Expired) -> Unit)? = null,
    onActivated: (String, VerifiedLicense) -> Unit
) {
    var segments by remember { mutableStateOf(listOf("", "", "")) }
    var focus by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun type(char: String) {
        error = null
        val next = segments.toMutableList()
        if (next[focus].length >= 4) {
            if (focus < 2) focus += 1 else return
        }
        next[focus] = next[focus] + char
        if (next[focus].length == 4 && focus < 2) focus += 1
        segments = next
    }

    fun backspace() {
        error = null
        val next = segments.toMutableList()
        if (next[focus].isEmpty() && focus > 0) focus -= 1
        if (next[focus].isNotEmpty()) next[focus] = next[focus].dropLast(1)
        segments = next
    }

    val complete = segments.all { it.length == 4 }
    val fullKey = "${LicenseKey.PREFIX}-${segments.joinToString("-")}"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Soft top cyan-blue glow gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.45f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE0F2FE).copy(alpha = 0.5f),
                            Color(0xFFF0FDF4).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        val boardWidth = (maxWidth * 0.72f).coerceIn(640.dp, 1280.dp)
        val gap = (boardWidth * 0.012f).coerceIn(8.dp, 14.dp)
        val keyWidth = (boardWidth - gap * 9) / 10
        val keyHeight = (keyWidth * 0.76f).coerceAtMost(maxHeight * 0.07f).coerceAtLeast(46.dp)
        val slotWidth = keyWidth * 1.85f
        val slotHeight = keyHeight * 1.15f
        val keyboardKeyWidth = keyWidth * 0.68f
        val keyboardKeyHeight = keyHeight * 0.72f
        val keyboardGap = gap * 0.72f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = maxHeight * 0.035f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GyoashMark(markSize = 34.dp, labelSize = 17.sp)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Activate this Panel",
                    color = EntryText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Enter the 16-character licence key from your administrator",
                    color = EntryMuted,
                    fontSize = 15.sp
                )
            }

            // Key Slots Row
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Fixed GYSH Box
                    FixedPrefixSlot(
                        width = slotWidth,
                        height = slotHeight,
                        fontSize = 20.sp
                    )

                    DashSeparator(gap = gap)

                    // Slot 1
                    EditableKeySlot(
                        text = segments[0],
                        active = focus == 0,
                        width = slotWidth,
                        height = slotHeight,
                        fontSize = 20.sp,
                        onClick = { focus = 0 }
                    )

                    DashSeparator(gap = gap)

                    // Slot 2
                    EditableKeySlot(
                        text = segments[1],
                        active = focus == 1,
                        width = slotWidth,
                        height = slotHeight,
                        fontSize = 20.sp,
                        onClick = { focus = 1 }
                    )

                    DashSeparator(gap = gap)

                    // Slot 3
                    EditableKeySlot(
                        text = segments[2],
                        active = focus == 2,
                        width = slotWidth,
                        height = slotHeight,
                        fontSize = 20.sp,
                        onClick = { focus = 2 }
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Virtual Keyboard
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                keyboardRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(keyboardGap)) {
                        row.forEach { char ->
                            WhiteKeyboardKey(
                                label = char,
                                width = keyboardKeyWidth,
                                height = keyboardKeyHeight,
                                fontSize = 17.sp,
                                onClick = {
                                    if (char == "⌫") backspace() else type(char)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(keyboardGap))
                }
                // Space Bar
                WhiteKeyboardKey(
                    label = "space",
                    width = keyboardKeyWidth * 6.5f,
                    height = keyboardKeyHeight * 0.95f,
                    fontSize = 14.sp,
                    onClick = {}
                )
            }

            // Action Button & Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (complete && !busy) {
                                Modifier.shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0x668B5CF6)
                                )
                            } else Modifier
                        )
                        .background(
                            if (complete && !busy) EntryGradient
                            else Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFCBD5E1)))
                        )
                        .clickable(enabled = complete && !busy) {
                            if (!LicenseKey.isWellFormed(fullKey)) {
                                error = "Enter the full key in the form GYSH-XXXX-XXXX-XXXX."
                                return@clickable
                            }
                            busy = true
                            error = null
                            scope.launch {
                                when (val result = panelApi.checkLicense(fullKey.uppercase(), panelId)) {
                                    is LicenseCheckResult.Valid -> {
                                        onActivated(fullKey.uppercase(), result.license)
                                    }
                                    is LicenseCheckResult.Expired -> {
                                        if (onLicenseExpired != null) {
                                            onLicenseExpired(result)
                                        } else {
                                            error = "This licence has expired. Contact your administrator."
                                            busy = false
                                        }
                                    }
                                    is LicenseCheckResult.SchoolInactive -> {
                                        error = "School subscription is inactive. Contact your administrator."
                                        busy = false
                                    }
                                    is LicenseCheckResult.Invalid -> {
                                        error = result.message
                                        busy = false
                                    }
                                    is LicenseCheckResult.NetworkError -> {
                                        error = "Unable to connect to school server. Check internet connection."
                                        busy = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Activate Panel",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Device UUID  $deviceUuid  ·  support@gyoash.com",
                    color = EntryPlaceholder,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DashSeparator(gap: Dp) {
    Text(
        text = "—",
        color = Color(0xFF94A3B8),
        fontSize = 18.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.padding(horizontal = gap * 0.6f)
    )
}

@Composable
private fun FixedPrefixSlot(
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(1.5.dp, Color(0xFFE2E8F0), shape)
            .clip(shape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = LicenseKey.PREFIX,
            color = EntryText,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun EditableKeySlot(
    text: String,
    active: Boolean,
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .then(
                if (active) Modifier.gradientBorder(shape, width = 2.dp)
                else Modifier.border(1.5.dp, Color(0xFFE2E8F0), shape)
            )
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (text.isEmpty()) {
            Text(
                text = "----",
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 3.sp
            )
        } else {
            Text(
                text = text,
                color = EntryText,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun WhiteKeyboardKey(
    label: String,
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(1.dp, Color(0xFFE2E8F0), shape)
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.Outlined.Backspace,
                contentDescription = "Backspace",
                tint = Color(0xFFF43F5E), // Red/pink outline as in design
                modifier = Modifier.size(height * 0.44f)
            )
        } else {
            Text(
                text = label,
                color = if (label == "space") Color(0xFF64748B) else EntryText,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
