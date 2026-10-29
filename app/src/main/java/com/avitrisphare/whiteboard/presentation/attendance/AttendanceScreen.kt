package com.avitrisphare.whiteboard.presentation.attendance

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class StudentPresence {
    PRESENT,
    ABSENT,
    LATE
}

@Immutable
data class AttendanceStudent(
    val id: String,
    val rollNumber: String,
    val name: String,
    val presence: StudentPresence,
    val remarks: String? = null
)

@Composable
fun AttendanceScreen(
    teacher: PanelTeacher,
    schoolName: String,
    classroomName: String,
    subject: String,
    onBack: () -> Unit,
    onAttendanceSubmitted: (presentCount: Int, totalCount: Int) -> Unit = { _, _ -> }
) {
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val roster = remember {
        mutableStateListOf(
            AttendanceStudent("std-01", "10A-01", "Aarav Sharma", StudentPresence.PRESENT),
            AttendanceStudent("std-02", "10A-02", "Ananya Verma", StudentPresence.PRESENT),
            AttendanceStudent("std-03", "10A-03", "Dev Patel", StudentPresence.ABSENT, "Parent informed fever"),
            AttendanceStudent("std-04", "10A-04", "Diya Rao", StudentPresence.LATE, "School bus delay (+15m)"),
            AttendanceStudent("std-05", "10A-05", "Ishaan Gupta", StudentPresence.PRESENT),
            AttendanceStudent("std-06", "10A-06", "Kavya Singh", StudentPresence.PRESENT),
            AttendanceStudent("std-07", "10A-07", "Manish Kulkarni", StudentPresence.PRESENT),
            AttendanceStudent("std-08", "10A-08", "Meera Nair", StudentPresence.PRESENT),
            AttendanceStudent("std-09", "10A-09", "Nikhil Joshi", StudentPresence.PRESENT),
            AttendanceStudent("std-10", "10A-10", "Pooja Reddy", StudentPresence.PRESENT),
            AttendanceStudent("std-11", "10A-11", "Pranav Deshmukh", StudentPresence.PRESENT),
            AttendanceStudent("std-12", "10A-12", "Rhea Sengupta", StudentPresence.ABSENT, "Medical appointment"),
            AttendanceStudent("std-13", "10A-13", "Rohan Mehta", StudentPresence.PRESENT),
            AttendanceStudent("std-14", "10A-14", "Sanya Kapoor", StudentPresence.PRESENT),
            AttendanceStudent("std-15", "10A-15", "Siddharth Jain", StudentPresence.PRESENT),
            AttendanceStudent("std-16", "10A-16", "Sneha Iyer", StudentPresence.PRESENT),
            AttendanceStudent("std-17", "10A-17", "Tanvi Patil", StudentPresence.PRESENT),
            AttendanceStudent("std-18", "10A-18", "Varun Chopra", StudentPresence.PRESENT),
            AttendanceStudent("std-19", "10A-19", "Yashwant Shinde", StudentPresence.PRESENT),
            AttendanceStudent("std-20", "10A-20", "Zoya Khan", StudentPresence.PRESENT)
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var filterPresence by remember { mutableStateOf<StudentPresence?>(null) }
    var editingRemarksStudent by remember { mutableStateOf<AttendanceStudent?>(null) }
    var remarksText by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }

    val totalCount = roster.size
    val presentCount = roster.count { it.presence == StudentPresence.PRESENT }
    val absentCount = roster.count { it.presence == StudentPresence.ABSENT }
    val lateCount = roster.count { it.presence == StudentPresence.LATE }
    val attendancePct = if (totalCount > 0) ((presentCount + lateCount * 0.5) / totalCount * 100).toInt() else 0

    val filteredList = remember(roster.toList(), searchQuery, filterPresence) {
        roster.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                s.name.contains(searchQuery, ignoreCase = true) ||
                s.rollNumber.contains(searchQuery, ignoreCase = true)
            val matchesFilter = filterPresence == null || s.presence == filterPresence
            matchesSearch && matchesFilter
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
            // 1. TOP HEADER (Zero hardware shadows for 60fps)
            IFPScreenHeader(
                title = "Mark Attendance",
                subtitle = "Period 1 · $todayDate",
                onBack = onBack,
                teacher = teacher,
                schoolName = schoolName,
                classroomName = classroomName.ifBlank { "Class 10-A" },
                subject = subject.ifBlank { "Physics" }
            )

            // 2. METRICS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.People,
                    label = "Total Students",
                    value = "$totalCount",
                    color = Color(0xFF2563EB),
                    bg = Color(0xFFEFF6FF),
                    border = Color(0xFFDBEAFE),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.Person,
                    label = "Present",
                    value = "$presentCount",
                    color = Color(0xFF16A34A),
                    bg = Color(0xFFDCFCE7),
                    border = Color(0xFFBBF7D0),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.PersonOff,
                    label = "Absent",
                    value = "$absentCount",
                    color = Color(0xFFDC2626),
                    bg = Color(0xFFFEE2E2),
                    border = Color(0xFFFECACA),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.Timer,
                    label = "Late Arrival",
                    value = "$lateCount",
                    color = Color(0xFFD97706),
                    bg = Color(0xFFFEF3C7),
                    border = Color(0xFFFDE68A),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.DoneAll,
                    label = "Attendance %",
                    value = "$attendancePct%",
                    color = Color(0xFF7C3AED),
                    bg = Color(0xFFF5F3FF),
                    border = Color(0xFFEDE9FE),
                    modifier = Modifier.weight(1.1f)
                )
            }

            // 3. CONTROLS BAR (Search, Filters, Batch Actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search student name or roll...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                // Filter Chips
                FilterPill(
                    label = "All",
                    count = totalCount,
                    isSelected = filterPresence == null,
                    onClick = { filterPresence = null }
                )
                FilterPill(
                    label = "Present",
                    count = presentCount,
                    isSelected = filterPresence == StudentPresence.PRESENT,
                    color = Color(0xFF16A34A),
                    onClick = { filterPresence = StudentPresence.PRESENT }
                )
                FilterPill(
                    label = "Absent",
                    count = absentCount,
                    isSelected = filterPresence == StudentPresence.ABSENT,
                    color = Color(0xFFDC2626),
                    onClick = { filterPresence = StudentPresence.ABSENT }
                )
                FilterPill(
                    label = "Late",
                    count = lateCount,
                    isSelected = filterPresence == StudentPresence.LATE,
                    color = Color(0xFFD97706),
                    onClick = { filterPresence = StudentPresence.LATE }
                )

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Batch Actions
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDCFCE7))
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                        .clickable {
                            roster.indices.forEach { i ->
                                roster[i] = roster[i].copy(presence = StudentPresence.PRESENT)
                            }
                            hasChanges = true
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Mark All Present",
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .clickable {
                            roster.indices.forEach { i ->
                                roster[i] = roster[i].copy(presence = StudentPresence.ABSENT)
                            }
                            hasChanges = true
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            // 4. ROSTER GRID (2 Columns for wide IFP screens)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }, contentType = { "student" }) { student ->
                    StudentAttendanceCard(
                        student = student,
                        onPresenceChange = { newPresence ->
                            val idx = roster.indexOfFirst { it.id == student.id }
                            if (idx != -1) {
                                roster[idx] = roster[idx].copy(presence = newPresence)
                                hasChanges = true
                            }
                        },
                        onEditRemarks = {
                            editingRemarksStudent = student
                            remarksText = student.remarks ?: ""
                        }
                    )
                }
            }

            // 5. STICKY FOOTER ACTION BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$presentCount of $totalCount students marked present ($attendancePct%)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (hasChanges) "Unsaved attendance records on this panel" else "Records synced for today's session",
                        color = if (hasChanges) Color(0xFFD97706) else Color(0xFF16A34A),
                        fontSize = 12.sp
                    )
                }

                // Submit Attendance Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(EntryGradient)
                        .clickable {
                            hasChanges = false
                            onAttendanceSubmitted(presentCount, totalCount)
                            scope.launch {
                                snackbar.showSnackbar("Attendance submitted successfully! ($presentCount/$totalCount Present)")
                            }
                        }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Submit Attendance",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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

        // Remarks Edit Dialog
        if (editingRemarksStudent != null) {
            val target = editingRemarksStudent!!
            AlertDialog(
                onDismissRequest = { editingRemarksStudent = null },
                title = {
                    Text(
                        text = "Remarks for ${target.name}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Add optional note for parent notification or attendance record:",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = remarksText,
                            onValueChange = { remarksText = it },
                            placeholder = { Text("e.g. Medical leave, Bus delayed, Sports duty...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val idx = roster.indexOfFirst { it.id == target.id }
                            if (idx != -1) {
                                roster[idx] = roster[idx].copy(remarks = remarksText.trim().ifBlank { null })
                                hasChanges = true
                            }
                            editingRemarksStudent = null
                        }
                    ) {
                        Text("Save Remark", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingRemarksStudent = null }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun StudentAttendanceCard(
    student: AttendanceStudent,
    onPresenceChange: (StudentPresence) -> Unit,
    onEditRemarks: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Roll & Avatar Pill
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    when (student.presence) {
                        StudentPresence.PRESENT -> Color(0xFFDCFCE7)
                        StudentPresence.ABSENT -> Color(0xFFFEE2E2)
                        StudentPresence.LATE -> Color(0xFFFEF3C7)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = student.rollNumber.takeLast(2),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = when (student.presence) {
                    StudentPresence.PRESENT -> Color(0xFF15803D)
                    StudentPresence.ABSENT -> Color(0xFFDC2626)
                    StudentPresence.LATE -> Color(0xFFB45309)
                }
            )
        }

        Spacer(Modifier.width(10.dp))

        // Name & Remarks
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Roll: ${student.rollNumber}",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                if (!student.remarks.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "• ${student.remarks}",
                        color = Color(0xFFD97706),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Remarks Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (!student.remarks.isNullOrBlank()) Color(0xFFFEF3C7) else Color(0xFFF1F5F9))
                .clickable(onClick = onEditRemarks),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Comment,
                contentDescription = "Remarks",
                tint = if (!student.remarks.isNullOrBlank()) Color(0xFFD97706) else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // P / A / L Segmented Controls (High-contrast, large touch targets, zero lag)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5F9))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            PresenceToggleChip(
                label = "P",
                isSelected = student.presence == StudentPresence.PRESENT,
                activeBg = Color(0xFF16A34A),
                onClick = { onPresenceChange(StudentPresence.PRESENT) }
            )
            PresenceToggleChip(
                label = "A",
                isSelected = student.presence == StudentPresence.ABSENT,
                activeBg = Color(0xFFDC2626),
                onClick = { onPresenceChange(StudentPresence.ABSENT) }
            )
            PresenceToggleChip(
                label = "L",
                isSelected = student.presence == StudentPresence.LATE,
                activeBg = Color(0xFFD97706),
                onClick = { onPresenceChange(StudentPresence.LATE) }
            )
        }
    }
}

@Composable
private fun PresenceToggleChip(
    label: String,
    isSelected: Boolean,
    activeBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (isSelected) activeBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF475569),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = label, color = Color(0xFF64748B), fontSize = 11.sp)
            Text(text = value, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) color else Color(0xFFE2E8F0),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isSelected) color else Color(0xFF475569),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
        Spacer(Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isSelected) color else Color(0xFFF1F5F9))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$count",
                color = if (isSelected) Color.White else Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
