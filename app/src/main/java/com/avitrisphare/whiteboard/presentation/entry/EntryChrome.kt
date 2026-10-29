package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.R

internal val EntryBg = Color(0xFFFFFFFF)
internal val EntryCard = Color(0xFFFFFFFF)
internal val EntryCardBorder = Color(0xFFE2E8F0)
internal val EntryKey = Color(0xFFFFFFFF)
internal val EntryKeyBorder = Color(0xFFE2E8F0)
internal val EntryMuted = Color(0xFF64748B)
internal val EntryText = Color(0xFF0F172A)
internal val EntryPlaceholder = Color(0xFF94A3B8)

// Brand Colors: F26A1B (Orange) and 2E2466 (Deep Purple)
internal val BrandOrange = Color(0xFFF26A1B)
internal val BrandPurple = Color(0xFF2E2466)

internal val EntryCyan = Color(0xFFF26A1B)
internal val EntryBlue = Color(0xFF8B3A44)
internal val EntryPurple = Color(0xFF2E2466)

internal val EntryGradient = Brush.horizontalGradient(
    listOf(BrandOrange, BrandPurple)
)

internal val EntryActiveBorder = Brush.horizontalGradient(
    listOf(BrandOrange, BrandPurple)
)

@Composable
internal fun GyoashFullLogo(
    modifier: Modifier = Modifier,
    contentDescription: String = "GYOASH - Redefining Trust"
) {
    Image(
        painter = painterResource(R.drawable.ic_gyoash_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
internal fun GyoashMark(
    schoolName: String? = null,
    markSize: Dp = 32.dp,
    labelSize: TextUnit = if (schoolName == null) 16.sp else 15.sp,
    showLabel: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (schoolName == null && showLabel) {
            Image(
                painter = painterResource(R.drawable.ic_gyoash_logo_compact),
                contentDescription = "GYOASH",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(markSize)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_gyoash_mark),
                contentDescription = "GYOASH Mark",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(markSize)
            )
            if (showLabel && schoolName != null) {
                Spacer(Modifier.width(markSize * 0.35f))
                Text(
                    text = schoolName,
                    color = EntryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = labelSize
                )
            }
        }
    }
}

internal fun Modifier.gradientBorder(
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    width: Dp = 2.dp
): Modifier {
    return this.border(width = width, brush = EntryActiveBorder, shape = shape)
}
