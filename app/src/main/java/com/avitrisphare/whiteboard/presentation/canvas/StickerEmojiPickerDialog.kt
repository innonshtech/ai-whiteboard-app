package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FeedbackStickerItem(
    val kind: String,
    val symbol: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val badgeBg: Color
)

private val CORE_STICKERS = listOf(
    FeedbackStickerItem(
        kind = "star",
        symbol = "⭐",
        title = "Star",
        subtitle = "Excellence & Praise",
        accentColor = Color(0xFFF59E0B),
        badgeBg = Color(0xFFFEF3C7)
    ),
    FeedbackStickerItem(
        kind = "tick",
        symbol = "✓",
        title = "Tick",
        subtitle = "Correct & Approved",
        accentColor = Color(0xFF10B981),
        badgeBg = Color(0xFFD1FAE5)
    ),
    FeedbackStickerItem(
        kind = "cross",
        symbol = "✕",
        title = "Cross",
        subtitle = "Correction & Review",
        accentColor = Color(0xFFEF4444),
        badgeBg = Color(0xFFFEE2E2)
    ),
    FeedbackStickerItem(
        kind = "thumb",
        symbol = "👍",
        title = "Thumbs-Up",
        subtitle = "Good Job & Agree",
        accentColor = Color(0xFF3B82F6),
        badgeBg = Color(0xFFDBEAFE)
    )
)

private val CURATED_EDUCATIONAL_EMOJI = listOf(
    "😀", "🎉", "💡", "👏", "💯", "🎯", "📚", "🏆",
    "❤️", "❓", "🤔", "🔥", "🌟", "✍️", "👍", "👎",
    "⚡", "🚀", "🥇", "🥈", "🥉", "🎓", "🧪", "📐"
)

@Composable
fun StickerEmojiPanelContent(
    onSelectSticker: (kind: String, text: String) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .width(330.dp)
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 2.dp, vertical = 2.dp)
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
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.Star else Icons.Default.EmojiEmotions,
                        contentDescription = null,
                        tint = if (selectedTab == 0) Color(0xFFF59E0B) else Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Stickers & Emoji",
                    color = Color(0xFF1E293B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onClose != null) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs: Stickers vs Emoji
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFF1F5F9),
            contentColor = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "⭐ " + stringResource(R.string.stickers_tab),
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) Color(0xFFD97706) else Color(0xFF64748B),
                        fontSize = 11.5.sp
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "😀 " + stringResource(R.string.emoji_tab),
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) Color(0xFF2563EB) else Color(0xFF64748B),
                        fontSize = 11.5.sp
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Body content based on tab
        if (selectedTab == 0) {
            // Four Core Stickers Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactStickerCard(
                        sticker = CORE_STICKERS[0],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectSticker(CORE_STICKERS[0].kind, CORE_STICKERS[0].symbol)
                            onClose?.invoke()
                        }
                    )
                    CompactStickerCard(
                        sticker = CORE_STICKERS[1],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectSticker(CORE_STICKERS[1].kind, CORE_STICKERS[1].symbol)
                            onClose?.invoke()
                        }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactStickerCard(
                        sticker = CORE_STICKERS[2],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectSticker(CORE_STICKERS[2].kind, CORE_STICKERS[2].symbol)
                            onClose?.invoke()
                        }
                    )
                    CompactStickerCard(
                        sticker = CORE_STICKERS[3],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectSticker(CORE_STICKERS[3].kind, CORE_STICKERS[3].symbol)
                            onClose?.invoke()
                        }
                    )
                }
            }
        } else {
            // Curated Emoji Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(CURATED_EDUCATIONAL_EMOJI) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectSticker("emoji", emoji)
                                onClose?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Educational tip footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "💡 " + stringResource(R.string.stickers_tap_hint),
                color = Color(0xFF64748B),
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StickerEmojiPickerDialog(
    onDismiss: () -> Unit,
    onSelectSticker: (kind: String, text: String) -> Unit,
    isDark: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            StickerEmojiPanelContent(
                onSelectSticker = onSelectSticker,
                onClose = onDismiss,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun CompactStickerCard(
    sticker: FeedbackStickerItem,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (isDark) sticker.accentColor.copy(alpha = 0.6f) else sticker.accentColor.copy(alpha = 0.45f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isDark) sticker.badgeBg.copy(alpha = 0.25f) else sticker.badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sticker.symbol,
                    fontSize = 17.sp,
                    color = sticker.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = sticker.title,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sticker.subtitle,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 9.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
