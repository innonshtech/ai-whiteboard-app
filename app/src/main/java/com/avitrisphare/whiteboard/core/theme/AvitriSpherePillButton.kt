package com.avitrisphare.whiteboard.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors

@Composable
fun AvitriSpherePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) LocalAvitriSphareColors.current.brand else LocalAvitriSphareColors.current.surface,
            contentColor = LocalAvitriSphareColors.current.textPrimary
        ),
        border = BorderStroke(
            1.dp,
            if (isPrimary) LocalAvitriSphareColors.current.textPrimary else LocalAvitriSphareColors.current.borderStandard
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(text = text)
    }
}