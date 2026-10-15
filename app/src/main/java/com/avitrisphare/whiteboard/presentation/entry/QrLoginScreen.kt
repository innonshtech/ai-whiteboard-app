package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.avitrisphare.whiteboard.data.remote.ApiCall
import com.avitrisphare.whiteboard.data.remote.PanelApi
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.data.remote.QrPoll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avitrisphare.whiteboard.presentation.canvas.QRCodeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val steps = listOf(
    "Open the GYOASH Teacher App",
    "Tap 'Scan to Login'",
    "Point your camera at this QR code"
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
    var status by remember { mutableStateOf("Waiting for the school server…") }
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
                    val deadline = System.currentTimeMillis() + 55_000
                    while (isActive && System.currentTimeMillis() < deadline) {
                        delay(2000)
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
                    status = "Refreshing login code"
                }
            }
        }
    }

    val payload = nonce?.let { "gyoash://qr-login?nonce=$it" }
    val qr = remember(payload) { payload?.let { QRCodeHelper.generateQRCodeBitmap(it, 640) } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EntryBg),
        contentAlignment = Alignment.Center
    ) {
        val cardWidth = (maxWidth * 0.78f).coerceIn(720.dp, 1480.dp)
        val qrSize = (maxHeight * 0.46f).coerceIn(280.dp, 520.dp)
        Column(
            modifier = Modifier
                .width(cardWidth)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161C2C))
                .border(1.dp, Color(0xFF2A3348), RoundedCornerShape(28.dp))
                .padding(horizontal = 40.dp, vertical = 36.dp)
        ) {
            GyoashMark(schoolName = schoolName)
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (qr != null) {
                        Image(
                            bitmap = qr,
                            contentDescription = "Panel login QR code",
                            modifier = Modifier
                                .size(qrSize)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(10.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(qrSize)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF101628)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(status, color = EntryMuted, fontSize = 16.sp, modifier = Modifier.padding(24.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, EntryCyan.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("${seconds}s", color = EntryCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        status,
                        color = EntryMuted,
                        fontSize = 12.sp
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = cardWidth * 0.08f)
                ) {
                    Text("Scan to log in", color = EntryText, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Use your phone — no password needed", color = EntryMuted, fontSize = 20.sp)
                    Spacer(Modifier.height(20.dp))
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (index) {
                                            0 -> Color(0xFF2563EB)
                                            1 -> Color(0xFF7C3AED)
                                            else -> Color(0xFF06B6D4)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(step, color = EntryText, fontSize = 22.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF2A3348))
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Or log in with Email & Password",
                        color = Color(0xFF60A5FA),
                        fontSize = 20.sp,
                        modifier = Modifier.clickable { showEmail = true }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Change license key",
                color = EntryMuted,
                fontSize = 16.sp,
                modifier = Modifier.clickable(onClick = onChangeLicense)
            )
        }
    }

    if (showEmail) {
        EmailLoginDialog(
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

@Composable
private fun EmailLoginDialog(
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
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161C2C))
                .padding(24.dp)
                .width(360.dp)
        ) {
            Text("Teacher login", color = EntryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Uses your school account on the server.", color = EntryMuted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = EntryText,
                unfocusedTextColor = EntryText,
                focusedBorderColor = EntryCyan,
                unfocusedBorderColor = Color(0xFF2A3348),
                cursorColor = EntryCyan
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = Color(0xFFF87171), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(999.dp))
                    .background(EntryGradient)
                    .clickable(enabled = !busy) {
                        if (!email.contains("@") || password.isBlank()) {
                            error = "Enter a school email and a password."
                        } else {
                            busy = true
                            error = null
                            onSubmit(
                                email.trim(),
                                password,
                                { message ->
                                    error = message
                                    busy = false
                                },
                                { busy = false }
                            )
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(if (busy) "Checking…" else "Log in", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
