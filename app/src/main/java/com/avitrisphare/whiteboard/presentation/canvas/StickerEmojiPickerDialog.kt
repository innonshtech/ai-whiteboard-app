package com.avitrisphare.whiteboard.presentation.canvas

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog

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
fun StickerEmojiPickerDialog(
    onDismiss: () -> Unit,
    onSelectSticker: (kind: String, text: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF333B4F), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C3246)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.Star else Icons.Default.EmojiEmotions,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Classroom Stickers & Emoji",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Curated feedback set (not a full system keyboard)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs: Stickers vs Emoji
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF151922),
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "⭐ Teacher Stickers (4)",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "😀 Classroom Emoji",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body content based on tab
                if (selectedTab == 0) {
                    // Four Core Stickers Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StickerCard(
                                sticker = CORE_STICKERS[0],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectSticker(CORE_STICKERS[0].kind, CORE_STICKERS[0].symbol)
                                    onDismiss()
                                }
                            )
                            StickerCard(
                                sticker = CORE_STICKERS[1],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectSticker(CORE_STICKERS[1].kind, CORE_STICKERS[1].symbol)
                                    onDismiss()
                                }
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StickerCard(
                                sticker = CORE_STICKERS[2],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectSticker(CORE_STICKERS[2].kind, CORE_STICKERS[2].symbol)
                                    onDismiss()
                                }
                            )
                            StickerCard(
                                sticker = CORE_STICKERS[3],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectSticker(CORE_STICKERS[3].kind, CORE_STICKERS[3].symbol)
                                    onDismiss()
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
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(CURATED_EDUCATIONAL_EMOJI) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF282F40))
                                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectSticker("emoji", emoji)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Educational tip footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141824))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "💡 Tap any sticker or emoji to place it on the active board. Drag to move, or select to delete.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerCard(
    sticker: FeedbackStickerItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF242A3A))
            .border(1.5.dp, sticker.accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(sticker.badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sticker.symbol,
                    fontSize = 26.sp,
                    color = sticker.accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = sticker.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sticker.subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
