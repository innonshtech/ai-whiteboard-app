package com.avitrisphare.whiteboard.presentation.homework

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.data.remote.PanelTeacher
import com.avitrisphare.whiteboard.presentation.common.IFPScreenHeader
import com.avitrisphare.whiteboard.presentation.entry.EntryGradient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class HomeworkItem(
    val id: String,
    val title: String,
    val subject: String,
    val classroomName: String,
    val description: String,
    val dueDate: String,
    val totalStudents: Int,
    val submittedCount: Int,
    val pendingReviewCount: Int,
    val hasBoardAttachment: Boolean = false,
    val status: String = "ACTIVE"
)

@Composable
fun HomeworkScreen(
    teacher: PanelTeacher,
    schoolName: String,
    classroomName: String,
    subject: String,
    onBack: () -> Unit
) {
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("ACTIVE") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var viewingHomework by remember { mutableStateOf<HomeworkItem?>(null) }

    val homeworkList = remember {
        mutableStateListOf(
            HomeworkItem(
                id = "hw-1",
                title = "Quadratic Equations Exercise 4.2",
                subject = "Mathematics",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                description = "Solve questions 1 to 15 from Chapter 4 Exercise 4.2. Show all working steps clearly including quadratic formula derivation.",
                dueDate = "Tomorrow, 05:00 PM",
                totalStudents = 38,
                submittedCount = 26,
                pendingReviewCount = 8,
                hasBoardAttachment = true
            ),
            HomeworkItem(
                id = "hw-2",
                title = "Electromagnetic Induction & Faraday's Laws",
                subject = "Physics",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                description = "Complete the observation worksheet for magnetic flux diagrams drawn on today's whiteboard. Write 2-page reflection on Lenz law experiments.",
                dueDate = "Saturday, 11:59 PM",
                totalStudents = 38,
                submittedCount = 14,
                pendingReviewCount = 14,
                hasBoardAttachment = true
            ),
            HomeworkItem(
                id = "hw-3",
                title = "Reflection & Refraction Ray Diagrams",
                subject = "Physics",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                description = "Draw ray diagrams for concave and convex mirrors with object at 6 different standard positions (infinity, C, between C & F, F, between F & P).",
                dueDate = "Completed 2 days ago",
                totalStudents = 38,
                submittedCount = 37,
                pendingReviewCount = 0,
                hasBoardAttachment = false,
                status = "COMPLETED"
            )
        )
    }

    val filteredList = remember(activeTab, homeworkList.toList()) {
        homeworkList.filter {
            if (activeTab == "ACTIVE") it.status == "ACTIVE" else it.status == "COMPLETED"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP HEADER
            IFPScreenHeader(
                title = "Assign Homework",
                subtitle = "Student Assignments & Submissions · $todayDate",
                onBack = onBack,
                teacher = teacher,
                schoolName = schoolName,
                classroomName = classroomName.ifBlank { "Class 10-A" },
                subject = subject.ifBlank { "Physics" },
                actions = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(EntryGradient)
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Assign New Homework",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            // 2. TAB CONTROLS & STATS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeTab == "ACTIVE") Color.White else Color.Transparent)
                            .clickable { activeTab = "ACTIVE" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Active Homework (${homeworkList.count { it.status == "ACTIVE" }})",
                            color = if (activeTab == "ACTIVE") Color(0xFF2563EB) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeTab == "COMPLETED") Color.White else Color.Transparent)
                            .clickable { activeTab = "COMPLETED" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Past & Reviewed (${homeworkList.count { it.status == "COMPLETED" }})",
                            color = if (activeTab == "COMPLETED") Color(0xFF2563EB) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = "Showing tasks for ${classroomName.ifBlank { "Class 10-A" }}",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            // 3. HOMEWORK LIST
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }, contentType = { "homework" }) { item ->
                    HomeworkCard(
                        item = item,
                        onClick = { viewingHomework = item }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        // 4. CREATE HOMEWORK DIALOG
        if (showCreateDialog) {
            var titleInput by remember { mutableStateOf("") }
            var descInput by remember { mutableStateOf("") }
            var attachBoardNotes by remember { mutableStateOf(true) }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = {
                    Text(
                        text = "Assign New Homework",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.width(500.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Homework Title") },
                            placeholder = { Text("e.g. Chapter 4 Exercise Problems 1-10") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = classroomName.ifBlank { "Class 10-A" },
                                onValueChange = {},
                                label = { Text("Target Class") },
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = subject.ifBlank { "Physics" },
                                onValueChange = {},
                                label = { Text("Subject") },
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("Instructions & Guidelines") },
                            placeholder = { Text("Describe tasks, questions to solve, or submission requirements...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attach today's Whiteboard Notes",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF)
                                )
                                Text(
                                    text = "Students receive canvas slides as reference PDF",
                                    fontSize = 11.sp,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            Switch(
                                checked = attachBoardNotes,
                                onCheckedChange = { attachBoardNotes = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (titleInput.isNotBlank()) {
                                homeworkList.add(
                                    0,
                                    HomeworkItem(
                                        id = "hw-${System.currentTimeMillis()}",
                                        title = titleInput.trim(),
                                        subject = subject.ifBlank { "Physics" },
                                        classroomName = classroomName.ifBlank { "Class 10-A" },
                                        description = descInput.trim().ifBlank { "Complete assigned questions." },
                                        dueDate = "In 2 days, 05:00 PM",
                                        totalStudents = 38,
                                        submittedCount = 0,
                                        pendingReviewCount = 0,
                                        hasBoardAttachment = attachBoardNotes
                                    )
                                )
                                showCreateDialog = false
                                scope.launch {
                                    snackbar.showSnackbar("Homework assigned and notified to ${classroomName.ifBlank { "Class 10-A" }}!")
                                }
                            }
                        }
                    ) {
                        Text("Publish to Class", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // 5. VIEW HOMEWORK DETAIL MODAL
        if (viewingHomework != null) {
            val hw = viewingHomework!!
            AlertDialog(
                onDismissRequest = { viewingHomework = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(Modifier.width(8.dp))
                        Text(hw.title, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.width(500.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = hw.description,
                            color = Color(0xFF334155),
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(2.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Class Roster", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("${hw.totalStudents}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Submitted", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("${hw.submittedCount}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF059669))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pending Review", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("${hw.pendingReviewCount}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFD97706))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Due Date", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text(hw.dueDate, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF2563EB))
                            }
                        }

                        if (hw.hasBoardAttachment) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF0FDF4))
                                    .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Whiteboard notes (.PDF) attached to this assignment", color = Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewingHomework = null }) {
                        Text("Close", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun HomeworkCard(
    item: HomeworkItem,
    onClick: () -> Unit
) {
    val progress = if (item.totalStudents > 0) item.submittedCount.toFloat() / item.totalStudents.toFloat() else 0f
    val pct = (progress * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (item.status == "ACTIVE") Color(0xFFECFDF5) else Color(0xFFF1F5F9))
                .border(
                    1.dp,
                    if (item.status == "ACTIVE") Color(0xFFD1FAE5) else Color(0xFFE2E8F0),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.status == "ACTIVE") Icons.Default.MenuBook else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (item.status == "ACTIVE") Color(0xFF059669) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF5F3FF))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(text = item.subject, color = Color(0xFF7C3AED), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                if (item.hasBoardAttachment) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(text = "Board Notes Attached", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = item.description,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Submission Progress Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color(0xFF059669),
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${item.submittedCount}/${item.totalStudents} submitted ($pct%)",
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Due date & Status
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = item.dueDate,
                    color = if (item.status == "ACTIVE") Color(0xFFD97706) else Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            if (item.pendingReviewCount > 0) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${item.pendingReviewCount} pending review",
                    color = Color(0xFF2563EB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}
