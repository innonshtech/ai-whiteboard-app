package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avitrisphare.whiteboard.data.remote.ApiCall
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.data.remote.QrPoll
import com.avitrisphare.whiteboard.presentation.canvas.QRCodeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class LoginStep(val number: String, val color: Color, val text: String)

private val loginSteps = listOf(
    LoginStep("1", Color(0xFFF26A1B), "Open the GYOASH Teacher App"),
    LoginStep("2", Color(0xFF98455A), "Tap 'Scan to Login'"),
    LoginStep("3", Color(0xFF2E2466), "Point your camera at this QR code")
)

@Composable
fun QrLoginScreen(
    panelApi: PanelApi,
    schoolName: String,
    onLoggedIn: (PanelTeacher) -> Unit,
    onChangeLicense: () -> Unit
) {
    var nonce by remember { mutableStateOf<String?>(null) }
    var seconds by remember { mutableIntStateOf(60) }
    var showEmail by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Connecting to school server…") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (isActive) {
            when (val created = panelApi.createQrSession()) {
                is ApiCall.Err -> {
                    status = created.message
                    delay(3000)
                }
                is ApiCall.Ok -> {
                    nonce = created.value.nonce
                    seconds = 60
                    status = "Waiting for a teacher to scan"
                    val deadline = System.currentTimeMillis() + 58_000
                    while (isActive && System.currentTimeMillis() < deadline) {
                        delay(1000)
                        seconds = ((deadline - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                        when (val poll = panelApi.pollQr(created.value.nonce)) {
                            is ApiCall.Err -> status = poll.message
                            is ApiCall.Ok -> when (val state = poll.value) {
                                QrPoll.Pending -> status = "Waiting for a teacher to scan"
                                is QrPoll.Ready -> {
                                    onLoggedIn(state.teacher)
                                    return@LaunchedEffect
                                }
                                is QrPoll.Expired -> break
                            }
                        }
                    }
                    nonce = null
                    status = "Refreshing login code…"
                }
            }
        }
    }

    val payload = nonce?.let { "avitrisphare://board-login?nonce=$it" }
    val qr = remember(payload) { payload?.let { QRCodeHelper.generateQRCodeBitmap(it, 640) } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        // Soft top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.45f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandOrange.copy(alpha = 0.08f),
                            BrandPurple.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        val cardWidth = (maxWidth * 0.76f).coerceIn(680.dp, 940.dp)
        val qrBoxSize = (maxHeight * 0.44f).coerceIn(240.dp, 310.dp)

        // Center White Card
        Box(
            modifier = Modifier
                .width(cardWidth)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(0x18000000)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp))
                .padding(horizontal = 44.dp, vertical = 38.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GyoashMark(schoolName = schoolName, markSize = 30.dp, labelSize = 15.sp)

                    Text(
                        text = stringResource(R.string.qr_change_license),
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(onClick = onChangeLicense)
                            .padding(4.dp)
                    )
                }

                Spacer(Modifier.height(30.dp))

                // Two-Column Body
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: QR Code & Timer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(qrBoxSize + 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(qrBoxSize)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qr != null) {
                                Image(
                                    bitmap = qr,
                                    contentDescription = "Login QR code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF06B6D4),
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        status,
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Countdown Pill with Gradient Border
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(
                                    1.5.dp,
                                    EntryGradient,
                                    RoundedCornerShape(999.dp)
                                )
                                .background(Color.White)
                                .padding(horizontal = 22.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${seconds}s",
                                color = BrandOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.qr_auto_refresh_hint),
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.width(44.dp))

                    // Right Column: Scan Instructions & Email Option
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Title Row with Scan Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.qr_scan_title),
                                color = EntryText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.qr_scan_sub),
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(28.dp))

                        // 3 Steps
                        loginSteps.forEach { step ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(step.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = step.number,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = step.text,
                                    color = Color(0xFF1E293B),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Fallback link
                        Text(
                            text = stringResource(R.string.qr_login_with_email),
                            color = BrandOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { showEmail = true }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Email Fallback Dialog
        if (showEmail) {
            WhiteEmailLoginDialog(
                onDismiss = { showEmail = false },
                onSubmit = { email, password, reportError, done ->
                    scope.launch {
                        when (val result = panelApi.login(email, password)) {
                            is ApiCall.Ok -> {
                                done()
                                showEmail = false
                                onLoggedIn(result.value)
                            }
                            is ApiCall.Err -> reportError(result.message)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun WhiteEmailLoginDialog(
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String, reportError: (String) -> Unit, done: () -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .shadow(16.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                .padding(26.dp)
        ) {
            Text(
                stringResource(R.string.login_teacher_title),
                color = EntryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Uses your school account on the server.",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(20.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = EntryText,
                unfocusedTextColor = EntryText,
                focusedBorderColor = BrandOrange,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = BrandOrange
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text(stringResource(R.string.label_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text(stringResource(R.string.label_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = Color(0xFFEF4444), fontSize = 12.sp)
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(enabled = !busy, onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EntryGradient)
                        .clickable(enabled = !busy) {
                            if (!email.contains("@") || password.isBlank()) {
                                error = "Enter a valid email and password."
                            } else {
                                busy = true
                                error = null
                                onSubmit(
                                    email.trim(),
                                    password,
                                    { err -> error = err; busy = false },
                                    { busy = false }
                                )
                            }
                        }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.btn_log_in),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
