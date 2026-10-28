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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.R
import com.avitrisphare.whiteboard.core.localization.AppLanguage
import com.avitrisphare.whiteboard.data.remote.ClassBriefing
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.domain.model.Note
import com.avitrisphare.whiteboard.presentation.common.LanguageSelectionDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ClassHomeScreen(
    teacher: PanelTeacher,
    schoolName: String,
    classroomName: String,
    briefing: ClassBriefing,
    boards: List<Note>,
    onOpenWhiteboard: () -> Unit,
    onOpenBoard: (String) -> Unit,
    onNewBoard: () -> Unit,
    onOpenBoards: () -> Unit,
    onLogout: () -> Unit,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val subject = briefing.subject?.takeIf { it.isNotBlank() } ?: teacher.subject
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val comingSoon: () -> Unit = {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("Coming soon")
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Soft top cyan-blue glow gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.40f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE0F2FE).copy(alpha = 0.55f),
                            Color(0xFFF0FDF4).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        val contentWidth = (maxWidth * 0.74f).coerceIn(720.dp, 1240.dp)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(contentWidth)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. TOP APP BAR / HEADER CARD
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color(0x10000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GyoashMark(markSize = 34.dp, labelSize = 16.sp)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .height(28.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schoolName.ifBlank { stringResource(R.string.portal_title) },
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (classroomName.isNotBlank()) "$classroomName · AvitriSphare IFP Series" else "Interactive Classroom Display",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Language Selector Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                        .clickable { showLanguageDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = currentLanguage.nativeName,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Teacher Avatar & Name Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacherInitials(teacher.name),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = teacher.name.ifBlank { "Teacher" },
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Logout Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(999.dp))
                        .clickable { showLogoutConfirm = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.log_out),
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // ==========================================
            // 2. GREETING & OVERVIEW HERO CARD
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color(0x10000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${classGreeting(hour, teacher.name)} 👋",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (classroomName.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = classroomName,
                                        color = Color(0xFF1D4ED8),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            if (subject.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF5F3FF))
                                        .border(1.dp, Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = subject,
                                        color = Color(0xFF6D28D9),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Text(
                                text = "•   $todayDate",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }

                        val lesson = briefing.lastLesson
                        if (lesson.isNotBlank() && lesson != "Checking…" && lesson != "No lesson yet") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Last lesson: $lesson",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. PRIMARY HERO CTA: OPEN WHITEBOARD
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color(0x332563EB),
                        ambientColor = Color(0x152563EB)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0284C7),
                                Color(0xFF2563EB),
                                Color(0xFF7C3AED)
                            )
                        )
                    )
                    .clickable(onClick = onOpenWhiteboard)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.40f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.open_whiteboard),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.24f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Interactive",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.continue_today_board)} · $classroomName $subject",
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // Launch Board Action Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(999.dp),
                                spotColor = Color(0x2A000000)
                            )
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Launch Board",
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 4. CLASSROOM MANAGEMENT GRID (2x2)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionTile(
                    icon = Icons.Default.FactCheck,
                    iconColor = Color(0xFFEA580C),
                    containerBg = Color(0xFFFFF7ED),
                    containerBorder = Color(0xFFFFEDD5),
                    title = stringResource(R.string.mark_attendance),
                    subtitle = "Student presence for current period",
                    statusText = briefing.attendance.ifBlank { "Not marked" },
                    statusBg = if (briefing.attendance.contains("Marked", ignoreCase = true)) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    statusTextColor = if (briefing.attendance.contains("Marked", ignoreCase = true)) Color(0xFF15803D) else Color(0xFFB45309),
                    onClick = comingSoon,
                    modifier = Modifier.weight(1f)
                )

                ActionTile(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = Color(0xFF7C3AED),
                    containerBg = Color(0xFFF5F3FF),
                    containerBorder = Color(0xFFEDE9FE),
                    title = stringResource(R.string.weekly_timetable),
                    subtitle = "Today's scheduled periods",
                    statusText = "Class schedule",
                    statusBg = Color(0xFFF5F3FF),
                    statusTextColor = Color(0xFF7C3AED),
                    onClick = comingSoon,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionTile(
                    icon = Icons.Default.MenuBook,
                    iconColor = Color(0xFF059669),
                    containerBg = Color(0xFFECFDF5),
                    containerBorder = Color(0xFFD1FAE5),
                    title = stringResource(R.string.assign_homework),
                    subtitle = "Class assignments & tasks",
                    statusText = briefing.homework.ifBlank { "Nothing to review" },
                    statusBg = Color(0xFFECFDF5),
                    statusTextColor = Color(0xFF059669),
                    onClick = comingSoon,
                    modifier = Modifier.weight(1f)
                )

                ActionTile(
                    icon = Icons.Default.ChatBubbleOutline,
                    iconColor = Color(0xFF0284C7),
                    containerBg = Color(0xFFF0F9FF),
                    containerBorder = Color(0xFFE0F2FE),
                    title = stringResource(R.string.student_doubts),
                    subtitle = "Questions raised by students",
                    statusText = "Class queries",
                    statusBg = Color(0xFFF0F9FF),
                    statusTextColor = Color(0xFF0284C7),
                    onClick = comingSoon,
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // 5. CLASS TESTS & GRADING (Full width)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color(0x0C000000)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                    .clickable(onClick = comingSoon)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF))
                        .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tests_grading),
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Assessment scores, question papers & academic analytics",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                )
            }

            // ==========================================
            // 6. PAST WHITEBOARDS / RECENT LESSONS
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.past_whiteboards),
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${boards.size} boards",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.weight(1f))

                // + New Board Action Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(EntryGradient)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(999.dp),
                            spotColor = Color(0x3300C6FF)
                        )
                        .clickable(onClick = onNewBoard)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.new_board),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(10.dp))

                // View All Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenBoards)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.view_all),
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (boards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = Color(0x0A000000)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.no_boards_yet),
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ New Board' to create your first lesson canvas",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    boards.take(6).forEach { note ->
                        BoardRow(note = note, onClick = { onOpenBoard(note.id) })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { newLang ->
                    onLanguageChange(newLang)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = {
                    Text(
                        text = "End Session?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Text(
                        text = "You will be logged out of this classroom panel. Saved whiteboards will remain intact.",
                        color = Color(0xFF475569)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirm = false
                            onLogout()
                        }
                    ) {
                        Text(
                            text = "Log out",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text(text = "Cancel", color = Color(0xFF64748B))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    iconColor: Color,
    containerBg: Color,
    containerBorder: Color,
    title: String,
    subtitle: String,
    statusText: String? = null,
    statusBg: Color = Color(0xFFF1F5F9),
    statusTextColor: Color = Color(0xFF64748B),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x0C000000)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerBg)
                .border(1.dp, containerBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (statusText != null && statusText.isNotBlank() && statusText != "Checking…") {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusBg)
                    .border(1.dp, statusTextColor.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BoardRow(note: Note, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x0C000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEFF6FF))
                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Draw,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "Untitled Board" },
                color = Color(0xFF0F172A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = boardAge(note.updatedAt),
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "•",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Interactive Board",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // Open chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFEFF6FF))
                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Open",
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

internal fun classGreeting(hour: Int, teacherName: String): String {
    val hello = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val parts = teacherName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val honorifics = setOf("dr", "dr.", "mr", "mr.", "mrs", "mrs.", "ms", "ms.", "prof", "prof.")
    val greetingName = when {
        parts.isEmpty() -> "Teacher"
        parts.size >= 2 && parts[0].lowercase() in honorifics -> "${parts[0]} ${parts[1]}"
        else -> parts[0]
    }
    return "$hello, $greetingName"
}

internal fun teacherInitials(teacherName: String): String {
    val clean = teacherName.replace(Regex("^(?i)(dr\\.?|mr\\.?|mrs\\.?|ms\\.?|prof\\.?)\\s*"), "").trim()
    val parts = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "T"
    }
}

internal fun boardAge(updatedAt: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - updatedAt) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "${minutes}m ago"
        minutes < 60L * 24L -> "${minutes / 60L}h ago"
        else -> "${minutes / (60L * 24L)}d ago"
    }
}
