package com.avitrisphare.whiteboard.presentation.entry

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.data.remote.ClassBriefing
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.domain.model.Note
import java.util.Calendar
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import com.avitrisphare.whiteboard.core.localization.AppLanguage
import com.avitrisphare.whiteboard.presentation.common.LanguageSelectionDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val CardLine = Color(0xFF2A3348)

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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val comingSoon: () -> Unit = {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("Coming soon")
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EntryBg)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width((maxWidth * 0.62f).coerceIn(640.dp, 980.dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EntryCard)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GyoashMark(markSize = 32.dp, showLabel = false)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.portal_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        schoolName,
                        color = EntryMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = currentLanguage.nativeName,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { showLanguageDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.log_out),
                    color = EntryCyan,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onLogout)
                )
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EntryCard)
                        .border(1.dp, CardLine, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "${classGreeting(hour, teacher.name)} 👋",
                        color = EntryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$classroomName • $subject",
                        color = EntryCyan,
                        fontSize = 14.sp
                    )
                    val lesson = briefing.lastLesson
                    if (lesson.isNotBlank() && lesson != "Checking…" && lesson != "No lesson yet") {
                        Text(
                            "Last lesson · $lesson",
                            color = EntryMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                ActionTile(
                    icon = Icons.Default.Draw,
                    iconTint = Color(0xFF2563EB),
                    title = stringResource(R.string.open_whiteboard),
                    subtitle = stringResource(R.string.continue_today_board),
                    onClick = onOpenWhiteboard,
                    emphasized = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(
                        icon = Icons.Default.FactCheck,
                        iconTint = Color(0xFFEA580C),
                        title = stringResource(R.string.mark_attendance),
                        subtitle = briefing.attendance,
                        onClick = comingSoon,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        icon = Icons.Default.CalendarMonth,
                        iconTint = Color(0xFF7C3AED),
                        title = stringResource(R.string.weekly_timetable),
                        subtitle = "Today's periods",
                        onClick = comingSoon,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(
                        icon = Icons.Default.MenuBook,
                        iconTint = Color(0xFF059669),
                        title = stringResource(R.string.assign_homework),
                        subtitle = briefing.homework,
                        onClick = comingSoon,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        icon = Icons.Default.ChatBubbleOutline,
                        iconTint = Color(0xFF0284C7),
                        title = stringResource(R.string.student_doubts),
                        subtitle = "Questions from class",
                        onClick = comingSoon,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EntryCard)
                        .border(1.dp, CardLine, RoundedCornerShape(10.dp))
                        .clickable(onClick = comingSoon)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = EntryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.tests_grading),
                        color = EntryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = EntryMuted
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.past_whiteboards), color = EntryText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.new_board),
                        color = EntryCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(onClick = onNewBoard)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Text(
                        stringResource(R.string.view_all),
                        color = EntryMuted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(onClick = onOpenBoards)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                if (boards.isEmpty()) {
                    Text(
                        stringResource(R.string.no_boards_yet),
                        color = EntryMuted,
                        fontSize = 14.sp
                    )
                } else {
                    boards.take(6).forEach { note ->
                        BoardRow(note = note, onClick = { onOpenBoard(note.id) })
                    }
                }
            }
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
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (emphasized) Color(0xFF152033) else EntryCard)
            .border(1.dp, if (emphasized) EntryCyan.copy(alpha = 0.45f) else CardLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = EntryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank() && subtitle != "Checking…") {
                Text(
                    subtitle,
                    color = EntryMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
            .clip(RoundedCornerShape(10.dp))
            .background(EntryCard)
            .border(1.dp, CardLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.title.ifBlank { "Untitled" },
                color = EntryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(boardAge(note.updatedAt), color = EntryMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = EntryMuted)
    }
}

internal fun classGreeting(hour: Int, teacherName: String): String {
    val first = teacherName.substringBefore(" ").trim().ifBlank { "Teacher" }
    val hello = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return "$hello, $first"
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
