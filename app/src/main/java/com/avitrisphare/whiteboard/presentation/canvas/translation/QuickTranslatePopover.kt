package com.avitrisphare.whiteboard.presentation.canvas.translation

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.avitrisphare.whiteboard.core.theme.BrandOrange

/**
 * Compact, modern popover shown when a teacher taps "Translate" on a selected text or sticky note.
 * Offers language switching with live preview and two primary educational actions:
 * 1. "Side-by-Side (Bilingual)" — renders bilingual text block adjacent to original
 * 2. "Replace Original" — overwrites the original text
 */
@Composable
fun QuickTranslatePopover(
    originalText: String,
    translatedText: String,
    selectedLanguage: IndianLanguage,
    isTranslating: Boolean,
    isDarkTheme: Boolean = true,
    onLanguageSelected: (IndianLanguage) -> Unit,
    onApplySideBySide: (String) -> Unit,
    onApplyReplace: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val bg = if (isDarkTheme) Color(0xFF1E2638) else Color.White
    val cardBg = if (isDarkTheme) Color(0xFF121824) else Color(0xFFF8FAFC)
    val textPrimary = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDarkTheme) Color(0xFF2E3A52) else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(min = 380.dp, max = 520.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Translate Text",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⚡ On-Device · 10 Indian Languages",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Original Text Box (Snippet)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ORIGINAL (ENGLISH)",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = originalText.ifBlank { "(empty text)" },
                        color = textPrimary,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Language Selection Chips
                Text(
                    text = "SELECT LANGUAGE",
                    color = textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IndianLanguage.allIndianLanguages().forEach { lang ->
                        val isSelected = lang == selectedLanguage
                        val chipBg = if (isSelected) BrandOrange else cardBg
                        val chipText = if (isSelected) Color.White else textPrimary
                        val chipBorder = if (isSelected) BrandOrange else borderColor

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                                .clickable { onLanguageSelected(lang) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang.nativeName,
                                color = chipText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "(${lang.displayName})",
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else textSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Translated Live Output Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TRANSLATION (${selectedLanguage.displayName.uppercase()})",
                            color = BrandOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (isTranslating) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = BrandOrange
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Translating…",
                                    color = textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (isTranslating && translatedText.isBlank()) "Translating on device…"
                        else translatedText.ifBlank { "No translation available" },
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Side-by-side Button (Recommended)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandOrange)
                            .clickable(enabled = !isTranslating && translatedText.isNotBlank()) {
                                onApplySideBySide(translatedText)
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewSidebar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Side-by-Side (Bilingual)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Replace Original Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) Color(0xFF2A364F) else Color(0xFFE2E8F0))
                            .clickable(enabled = !isTranslating && translatedText.isNotBlank()) {
                                onApplyReplace(translatedText)
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Replace Text",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

