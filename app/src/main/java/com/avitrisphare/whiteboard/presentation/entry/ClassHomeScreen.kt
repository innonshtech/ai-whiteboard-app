package com.avitrisphare.whiteboard.presentation.entry

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
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
    onOpenAttendance: () -> Unit = {},
    onOpenTimetable: () -> Unit = {},
    onOpenHomework: () -> Unit = {},
    onOpenDoubts: () -> Unit = {},
    onOpenTestsGrading: () -> Unit = {},
    onUpdateBoard: (noteId: String, newTitle: String, newSubtitle: String, newTag: String) -> Unit = { _, _, _, _ -> },
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val subject = briefing.subject?.takeIf { it.isNotBlank() } ?: teacher.subject
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val snackbar = remember { SnackbarHostState() }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var boardSearchQuery by remember { mutableStateOf("") }
    var editingBoard by remember { mutableStateOf<Note?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. TOP APP BAR / HEADER CARD
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    tint = Color(0xFFF26A1B),
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
                            .background(Color(0xFF2E2466)),
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
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                        .clickable { showLogoutConfirm = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.log_out),
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // ==========================================
            // 2. GREETING & OVERVIEW HERO
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${classGreeting(hour, teacher.name)} 👋",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = classroomName.ifBlank { "asdfg" },
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = subject.ifBlank { "Class" },
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "· $todayDate",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            // ==========================================
            // 3. PRIMARY HERO CTA: OPEN WHITEBOARD
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF26A1B),
                                Color(0xFF2E2466)
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
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Launch Board",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
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
                    onClick = onOpenAttendance,
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
                    onClick = onOpenTimetable,
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
                    onClick = onOpenHomework,
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
                    onClick = onOpenDoubts,
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // 5. CLASS TESTS & GRADING (Full width)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenTestsGrading)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF7ED))
                        .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
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
            val filteredBoards = remember(boards, boardSearchQuery) {
                if (boardSearchQuery.isBlank()) {
                    boards
                } else {
                    boards.filter {
                        it.title.contains(boardSearchQuery, ignoreCase = true) ||
                        it.subtitle.contains(boardSearchQuery, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(boardSearchQuery, ignoreCase = true) }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent boards",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "· ${boards.size} saved",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )

                // Search Bar with actual interactive input
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = boardSearchQuery,
                        onValueChange = { boardSearchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (boardSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search boards by title, class, subject...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (boardSearchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { boardSearchQuery = "" }
                        )
                    }
                }

                // View All Link
                Text(
                    text = "View all ›",
                    color = Color(0xFFEA580C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onOpenBoards)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Spacer(Modifier.width(12.dp))

                // + New Board Action Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF26A1B))
                        .clickable(onClick = onNewBoard)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "New board",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cards row (Dynamic boards with Edit option, no hardcoded classes!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredBoards.isEmpty()) {
                    NewBoardCard(
                        onClick = onNewBoard,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    val displayBoards = filteredBoards.take(4)
                    displayBoards.forEach { note ->
                        val tag = note.tags.firstOrNull() ?: subject.ifBlank { "Board" }
                        BoardCard(
                            title = note.title.ifBlank { "Untitled Board" },
                            subtitle = note.subtitle.ifBlank { "Interactive whiteboard" },
                            tag = tag,
                            tagColor = boardTagColor(tag),
                            pageCount = 1,
                            timeAgo = boardAge(note.updatedAt),
                            onClick = { onOpenBoard(note.id) },
                            onEdit = { editingBoard = note },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (displayBoards.size < 4) {
                        NewBoardCard(
                            onClick = onNewBoard,
                            modifier = Modifier.weight(1f)
                        )
                        for (i in (displayBoards.size + 1) until 4) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
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

        if (editingBoard != null) {
            val b = editingBoard!!
            var titleDraft by remember(b.id) { mutableStateOf(b.title) }
            var subtitleDraft by remember(b.id) { mutableStateOf(b.subtitle) }
            var tagDraft by remember(b.id) { mutableStateOf(b.tags.firstOrNull() ?: subject.ifBlank { "Whiteboard" }) }

            AlertDialog(
                onDismissRequest = { editingBoard = null },
                title = {
                    Text(
                        text = "Edit Board Details",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column {
                            Text(
                                text = "Board Title (e.g., Grade 11-C · Physics)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = titleDraft,
                                onValueChange = { titleDraft = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF26A1B),
                                    cursorColor = Color(0xFFF26A1B)
                                ),
                                placeholder = { Text("e.g. Grade 11-C · Physics", fontSize = 13.sp) }
                            )
                        }

                        Column {
                            Text(
                                text = "Subtitle / Topic (e.g., Projectile motion)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = subtitleDraft,
                                onValueChange = { subtitleDraft = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF26A1B),
                                    cursorColor = Color(0xFFF26A1B)
                                ),
                                placeholder = { Text("e.g. Projectile motion", fontSize = 13.sp) }
                            )
                        }

                        Column {
                            Text(
                                text = "Subject / Tag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = tagDraft,
                                onValueChange = { tagDraft = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF26A1B),
                                    cursorColor = Color(0xFFF26A1B)
                                ),
                                placeholder = { Text("e.g. Physics", fontSize = 13.sp) }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalTitle = titleDraft.ifBlank { "Untitled Board" }
                            onUpdateBoard(b.id, finalTitle, subtitleDraft, tagDraft)
                            editingBoard = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF26A1B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingBoard = null }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
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
                tint = Color(0xFFEA580C),
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

private fun boardTagColor(tag: String): Color {
    val lower = tag.lowercase()
    return when {
        lower.contains("physics") -> Color(0xFF10B981)
        lower.contains("math") -> Color(0xFFF26A1B)
        lower.contains("science") -> Color(0xFF8B5CF6)
        lower.contains("chem") -> Color(0xFFEC4899)
        lower.contains("bio") -> Color(0xFF059669)
        lower.contains("hist") -> Color(0xFFD97706)
        else -> Color(0xFFF26A1B)
    }
}

@Composable
private fun NewBoardCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF7ED))
            .border(1.5.dp, Color(0xFFFFEDD5), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF26A1B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Board",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "New Board",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Start interactive board",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun BoardCard(
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    pageCount: Int,
    timeAgo: String,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            // Canvas Preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFEEF2F6), RoundedCornerShape(10.dp))
            ) {
                // Top subject colored bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(tagColor)
                )
                // Tag pill top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(tagColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(tag, color = tagColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Edit pencil button on preview
                if (onEdit != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.90f))
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit board",
                            tint = Color(0xFFF26A1B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Sketch illustration lines/circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = tagColor.copy(alpha = 0.7f),
                        start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.42f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.42f),
                        strokeWidth = 3f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.58f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.58f),
                        strokeWidth = 2.5f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawCircle(
                        color = tagColor.copy(alpha = 0.7f),
                        radius = h * 0.20f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.58f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onEdit != null) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit board",
                        tint = Color(0xFFF26A1B),
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onEdit() }
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$pageCount pages · $timeAgo",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }
        }
    }
}
