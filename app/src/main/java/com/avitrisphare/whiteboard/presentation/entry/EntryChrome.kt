package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val EntryBg = Color(0xFFFFFFFF)
internal val EntryCard = Color(0xFFFFFFFF)
internal val EntryCardBorder = Color(0xFFE2E8F0)
internal val EntryKey = Color(0xFFFFFFFF)
internal val EntryKeyBorder = Color(0xFFE2E8F0)
internal val EntryMuted = Color(0xFF64748B)
internal val EntryText = Color(0xFF0F172A)
internal val EntryPlaceholder = Color(0xFF94A3B8)

internal val EntryCyan = Color(0xFF00C6FF)
internal val EntryBlue = Color(0xFF2563EB)
internal val EntryPurple = Color(0xFF8B5CF6)

internal val EntryGradient = Brush.horizontalGradient(
    listOf(Color(0xFF00B4D8), Color(0xFF3B82F6), Color(0xFF8B5CF6))
)

internal val EntryActiveBorder = Brush.horizontalGradient(
    listOf(Color(0xFF00C6FF), Color(0xFF8B5CF6))
)

@Composable
internal fun GyoashMark(
    schoolName: String? = null,
    markSize: Dp = 32.dp,
    labelSize: TextUnit = if (schoolName == null) 16.sp else 15.sp,
    showLabel: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(markSize)
                .clip(RoundedCornerShape(markSize * 0.28f))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF00C6FF), Color(0xFF0072FF), Color(0xFF7F00FF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "G",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (labelSize.value * 1.15f).sp
            )
        }
        if (showLabel) {
            Spacer(Modifier.width(markSize * 0.35f))
            Text(
                text = schoolName ?: "GYOASH",
                color = EntryText,
                fontWeight = FontWeight.Bold,
                fontSize = labelSize,
                letterSpacing = if (schoolName == null) 1.sp else 0.sp
            )
        }
    }
}

internal fun Modifier.gradientBorder(
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    width: Dp = 2.dp
): Modifier {
    return this.border(width = width, brush = EntryActiveBorder, shape = shape)
}
