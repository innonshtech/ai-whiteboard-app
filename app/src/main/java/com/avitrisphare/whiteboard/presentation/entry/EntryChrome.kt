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

internal val EntryBg = Color(0xFF070B16)
internal val EntryCard = Color(0xFF1B2233)
internal val EntryKey = Color(0xFF232B3E)
internal val EntryMuted = Color(0xFF8B95A8)
internal val EntryText = Color(0xFFF4F7FB)
internal val EntryCyan = Color(0xFF22D3EE)
internal val EntryPurple = Color(0xFFA855F7)
internal val EntryBlue = Color(0xFF3B82F6)

internal val EntryGradient = Brush.horizontalGradient(listOf(EntryCyan, EntryBlue, EntryPurple))

@Composable
internal fun GyoashMark(
    schoolName: String? = null,
    markSize: Dp = 28.dp,
    labelSize: TextUnit = if (schoolName == null) 13.sp else 14.sp,
    showLabel: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(markSize)
                .clip(RoundedCornerShape(markSize * 0.28f))
                .background(Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF7C3AED)))),
            contentAlignment = Alignment.Center
        ) {
            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = labelSize)
        }
        if (showLabel) {
            Spacer(Modifier.width(markSize * 0.28f))
            Text(
                text = schoolName ?: "GYOASH",
                color = EntryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = labelSize
            )
        }
    }
}

internal fun Modifier.gradientBorder(shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier {
    return this.border(width = 1.5.dp, brush = EntryGradient, shape = shape)
}
