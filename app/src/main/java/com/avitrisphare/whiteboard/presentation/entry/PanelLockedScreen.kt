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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PanelLockedScreen(
    schoolName: String,
    deviceUuid: String,
    supportEmail: String = "support@gyoash.com",
    supportPhone: String = "+91 98220 11223",
    onCheckAgain: ((onResult: (Boolean, String) -> Unit) -> Unit)? = null,
    onEnterNewKey: () -> Unit
) {
    var busy by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Soft red/pink radial glow at top center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.55f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEE2E2).copy(alpha = 0.85f),
                            Color(0xFFFFF1F2).copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        val cardWidth = (maxWidth * 0.55f).coerceIn(480.dp, 600.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Circular Red Lock Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color(0x33EF4444)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFFFCA5A5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Panel Locked",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Title
            Text(
                text = "Panel Locked",
                color = EntryText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "This panel's licence has expired. Please contact your school administrator to reactivate access.",
                color = Color(0xFF475569),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(cardWidth)
            )

            Spacer(Modifier.height(30.dp))

            // Details Card
            Column(
                modifier = Modifier
                    .width(cardWidth)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color(0x0C000000)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                // School Row
                InfoRow(label = "School", value = schoolName.ifBlank { "Delhi Public School" })

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Device UUID Row
                InfoRow(label = "Device UUID", value = deviceUuid)

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Support Row
                InfoRow(
                    label = "Support",
                    value = "$supportEmail · $supportPhone"
                )
            }

            if (feedbackMessage != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = feedbackMessage!!,
                    color = if (isError) Color(0xFFEF4444) else Color(0xFF16A34A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(28.dp))

            // Primary Button: Contact Administrator / Check Licence
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0x558B5CF6)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(EntryGradient)
                    .clickable(enabled = !busy) {
                        if (onCheckAgain != null) {
                            busy = true
                            feedbackMessage = null
                            onCheckAgain { success, msg ->
                                busy = false
                                feedbackMessage = msg
                                isError = !success
                            }
                        } else {
                            feedbackMessage = "Please reach out to $supportEmail"
                            isError = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Contact Administrator",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Secondary Option: Enter New Licence Key
            Text(
                text = "Enter New Licence Key",
                color = Color(0xFF2563EB),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = !busy, onClick = onEnterNewKey)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = EntryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

