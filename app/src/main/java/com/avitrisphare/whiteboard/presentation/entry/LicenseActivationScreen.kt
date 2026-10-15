package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.avitrisphare.whiteboard.data.remote.ApiCall
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.VerifiedLicense
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val rows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M", "⌫")
)

@Composable
fun LicenseActivationScreen(
    panelApi: PanelApi,
    panelId: String,
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
    val key = "${LicenseKey.PREFIX}-${segments.joinToString("-")}"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EntryBg)
    ) {
        val boardWidth = (maxWidth * 0.72f).coerceIn(640.dp, 1320.dp)
        val gap = (boardWidth * 0.014f).coerceIn(8.dp, 16.dp)
        val keyWidth = (boardWidth - gap * 9) / 10
        val keyHeight = (keyWidth * 0.78f).coerceAtMost(maxHeight * 0.072f).coerceAtLeast(48.dp)
        val slotWidth = keyWidth * 1.85f
        val slotHeight = keyHeight * 1.15f
        val keyboardKeyWidth = keyWidth * 0.68f
        val keyboardKeyHeight = keyHeight * 0.72f
        val keyboardGap = gap * 0.72f
        val titleSize = (keyHeight.value * 0.62f).sp
        val bodySize = (keyHeight.value * 0.30f).sp
        val keyLabelSize = (keyHeight.value * 0.36f).sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.55f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF152038), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = maxHeight * 0.035f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GyoashMark(markSize = keyHeight * 0.72f, labelSize = bodySize)
                Spacer(Modifier.height(keyHeight * 0.45f))
                Text(
                    "Welcome to GYOASH Whiteboard",
                    color = EntryText,
                    fontSize = titleSize,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(keyHeight * 0.16f))
                Text(
                    "Enter the license key to activate this panel",
                    color = EntryMuted,
                    fontSize = bodySize
                )
                Spacer(Modifier.height(keyHeight * 0.55f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    KeySlot(
                        text = LicenseKey.PREFIX,
                        active = false,
                        width = slotWidth,
                        height = slotHeight,
                        fontSize = keyLabelSize,
                        onClick = {}
                    )
                    Dash(gap)
                    segments.forEachIndexed { index, value ->
                        KeySlot(
                            text = value.ifEmpty { "– – – –" },
                            active = focus == index,
                            placeholder = value.isEmpty(),
                            width = slotWidth,
                            height = slotHeight,
                            fontSize = keyLabelSize,
                            onClick = { focus = index }
                        )
                        if (index < segments.lastIndex) Dash(gap)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(gap))
                    Text(
                        error!!,
                        color = Color(0xFFF87171),
                        fontSize = bodySize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(boardWidth)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(keyboardGap)) {
                        row.forEach { keyLabel ->
                            KeyboardKey(
                                label = keyLabel,
                                width = keyboardKeyWidth,
                                height = keyboardKeyHeight,
                                fontSize = keyLabelSize,
                                onClick = {
                                    if (keyLabel == "⌫") backspace() else type(keyLabel)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(keyboardGap))
                }
                KeyboardKey(
                    label = "space",
                    width = keyboardKeyWidth * 6.4f,
                    height = keyboardKeyHeight,
                    fontSize = bodySize,
                    onClick = {}
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(boardWidth * 0.28f)
                        .height(keyHeight * 1.05f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (complete) EntryGradient
                            else Brush.horizontalGradient(listOf(Color(0xFF243044), Color(0xFF243044)))
                        )
                        .clickable(enabled = complete && !busy) {
                            if (!LicenseKey.isWellFormed(key)) {
                                error = "Enter the full key in the form GYSH-XXXX-XXXX-XXXX."
                                return@clickable
                            }
                            busy = true
                            error = null
                            scope.launch {
                                when (val result = panelApi.verifyLicense(key.uppercase(), panelId)) {
                                    is ApiCall.Ok -> onActivated(key.uppercase(), result.value)
                                    is ApiCall.Err -> {
                                        error = result.message
                                        busy = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (busy) "Checking…" else "Activate Panel",
                        color = if (complete) Color.White else EntryMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = bodySize
                    )
                }
                Spacer(Modifier.height(keyHeight * 0.35f))
                Text(
                    "Checked with the school server",
                    color = EntryMuted.copy(alpha = 0.85f),
                    fontSize = (bodySize.value * 0.85f).sp
                )
                Spacer(Modifier.height(keyHeight * 0.12f))
                Text(
                    "Panel ID: $panelId",
                    color = EntryMuted.copy(alpha = 0.7f),
                    fontSize = (bodySize.value * 0.78f).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun Dash(gap: Dp) {
    Text(
        "—",
        color = Color(0xFF3A445C),
        modifier = Modifier.padding(horizontal = gap * 0.7f)
    )
}

@Composable
private fun KeySlot(
    text: String,
    active: Boolean,
    placeholder: Boolean = false,
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(height * 0.22f)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .then(if (active) Modifier.gradientBorder(shape) else Modifier)
            .clip(shape)
            .background(EntryKey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (placeholder) Color(0xFF4B5568) else EntryText,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height * 0.22f))
            .background(EntryKey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                Icons.Outlined.Backspace,
                contentDescription = "Delete",
                tint = EntryMuted,
                modifier = Modifier.size(height * 0.42f)
            )
        } else {
            Text(label, color = EntryText, fontSize = fontSize, fontWeight = FontWeight.Medium)
        }
    }
}
