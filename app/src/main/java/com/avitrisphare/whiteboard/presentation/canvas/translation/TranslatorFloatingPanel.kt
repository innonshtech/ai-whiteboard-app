package com.avitrisphare.whiteboard.presentation.canvas.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.core.theme.BrandOrange
import com.avitrisphare.whiteboard.presentation.canvas.utility.FloatingToolPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.TranslatorUiState
import kotlinx.coroutines.delay

@Composable
fun TranslatorFloatingPanel(
    state: TranslatorUiState,
    onStateChange: (TranslatorUiState) -> Unit,
    onTranslate: (text: String, lang: IndianLanguage) -> Unit,
    onAddToCanvas: (text: String) -> Unit,
    onBringToFront: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val context = LocalContext.current

    // Debounce translation when source text changes
    LaunchedEffect(state.sourceText, state.targetLanguage) {
        if (state.sourceText.isNotBlank()) {
            delay(400) // 400ms debounce
            onTranslate(state.sourceText, state.targetLanguage)
        } else {
            onStateChange(state.copy(translatedText = "", isTranslating = false))
        }
    }

    FloatingToolPanel(
        title = "AI Translator (10+ Languages)",
        icon = Icons.Default.Language,
        position = state.position,
        isMinimized = state.isMinimized,
        onPositionChange = { onStateChange(state.copy(position = it)) },
        onToggleMinimize = { onStateChange(state.copy(isMinimized = !state.isMinimized)) },
        onClose = { onStateChange(state.copy(isVisible = false)) },
        onBringToFront = onBringToFront,
        modifier = modifier.widthIn(min = 360.dp, max = 460.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Language Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source Language
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222B3D))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "English",
                        color = Color(0xFFF1F5F9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )

                // Offline badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ 100% Offline",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 10 Indian Language Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IndianLanguage.allIndianLanguages().forEach { lang ->
                    val isSelected = lang == state.targetLanguage
                    val chipBg = if (isSelected) BrandOrange else Color(0xFF222B3D)
                    val chipTextColor = if (isSelected) Color.White else Color(0xFFCBD5E1)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(chipBg)
                            .clickable { onStateChange(state.copy(targetLanguage = lang)) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lang.nativeName,
                            color = chipTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "(${lang.displayName})",
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Source Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141A28))
                    .border(1.dp, Color(0xFF2E3A52), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                if (state.sourceText.isEmpty()) {
                    Text(
                        text = "Type or paste text to translate...",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = state.sourceText,
                    onValueChange = { onStateChange(state.copy(sourceText = it)) },
                    textStyle = TextStyle(
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp)
                )

                if (state.sourceText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onStateChange(state.copy(sourceText = "", translatedText = "")) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Translated Output Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp, max = 140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141A28))
                    .border(1.dp, Color(0xFF2E3A52), RoundedCornerShape(8.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.targetLanguage.displayName.uppercase(),
                        color = BrandOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.isTranslating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = BrandOrange
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Translating…",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = state.translatedText.ifBlank {
                        if (state.isTranslating) "Translating on device…"
                        else "Translation appears here"
                    },
                    color = if (state.translatedText.isNotBlank()) Color(0xFFF1F5F9) else Color(0xFF64748B),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons Row: Copy and Add to Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222B3D))
                        .clickable(enabled = state.translatedText.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Translation", state.translatedText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Add to Canvas Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandOrange)
                        .clickable(enabled = state.translatedText.isNotBlank()) {
                            onAddToCanvas(state.translatedText)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Canvas",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add to Board",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

