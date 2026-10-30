package com.avitrisphare.whiteboard.presentation.grading

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Save
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

@Immutable
data class AssessmentItem(
    val id: String,
    val name: String,
    val subject: String,
    val date: String,
    val totalMarks: Int,
    val passingMarks: Int,
    val type: String,
    val status: String
)

@Immutable
data class StudentMarkRow(
    val studentId: String,
    val rollNumber: String,
    val name: String,
    val marksObtained: Double?,
    val isAbsent: Boolean = false
)

@Composable
fun TestsGradingScreen(
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

    var showCreateDialog by remember { mutableStateOf(false) }

    val assessments = remember {
        mutableStateListOf(
            AssessmentItem("test-1", "Unit Test 1 - Laws of Motion", "Physics", "28 Sep 2026", 50, 20, "UNIT_TEST", "PUBLISHED"),
            AssessmentItem("test-2", "Chapter 10 Light & Reflection Quiz", "Physics", "15 Oct 2026", 25, 10, "QUIZ", "PUBLISHED"),
            AssessmentItem("test-3", "Mid-Term Physics Assessment", "Physics", "5 Nov 2026", 100, 40, "MID_TERM", "SCHEDULED")
        )
    }

    var selectedAssessmentId by remember { mutableStateOf(assessments.firstOrNull()?.id) }
    val currentAssessment = remember(selectedAssessmentId, assessments.toList()) {
        assessments.find { it.id == selectedAssessmentId } ?: assessments.first()
    }

    val studentMarks = remember {
        mutableStateListOf(
            StudentMarkRow("std-01", "10A-01", "Aarav Sharma", 46.0),
            StudentMarkRow("std-02", "10A-02", "Ananya Verma", 48.0),
            StudentMarkRow("std-03", "10A-03", "Dev Patel", null, isAbsent = true),
            StudentMarkRow("std-04", "10A-04", "Diya Rao", 39.5),
            StudentMarkRow("std-05", "10A-05", "Ishaan Gupta", 44.0),
            StudentMarkRow("std-06", "10A-06", "Kavya Singh", 47.0),
            StudentMarkRow("std-07", "10A-07", "Manish Kulkarni", 35.0),
            StudentMarkRow("std-08", "10A-08", "Meera Nair", 42.5),
            StudentMarkRow("std-09", "10A-09", "Nikhil Joshi", 38.0),
            StudentMarkRow("std-10", "10A-10", "Pooja Reddy", 45.0),
            StudentMarkRow("std-11", "10A-11", "Pranav Deshmukh", 32.0),
            StudentMarkRow("std-12", "10A-12", "Rhea Sengupta", 41.0),
            StudentMarkRow("std-13", "10A-13", "Rohan Mehta", 49.0),
            StudentMarkRow("std-14", "10A-14", "Sanya Kapoor", 43.0),
            StudentMarkRow("std-15", "10A-15", "Siddharth Jain", 37.0)
        )
    }

    val stats = remember(studentMarks.toList(), currentAssessment) {
        val entered = studentMarks.filter { !it.isAbsent && it.marksObtained != null }.map { it.marksObtained!! }
        val avg = if (entered.isNotEmpty()) entered.average() else 0.0
        val avgPercent = if (currentAssessment.totalMarks > 0) ((avg / currentAssessment.totalMarks) * 100).toInt() else 0
        val high = entered.maxOrNull() ?: 0.0
        val passed = studentMarks.count { !it.isAbsent && (it.marksObtained ?: 0.0) >= currentAssessment.passingMarks }
        val rate = if (entered.isNotEmpty()) (passed * 100 / entered.size) else 0
        object {
            val averageMarks = avg
            val averagePct = avgPercent
            val highest = high
            val passRatePct = rate
            val passStudents = passed
            val totalEntered = entered.size
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
                title = "Class Tests & Grading",
                subtitle = "Assessment Scores, Marks Entry & Academic Analytics · $todayDate",
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
                            text = "New Assessment",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            // 2. ACADEMIC OVERVIEW METRICS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradingMetricCard(
                    icon = Icons.Default.Percent,
                    label = "Class Average",
                    value = "${stats.averagePct}%",
                    sub = String.format("%.1f / %d marks", stats.averageMarks, currentAssessment.totalMarks),
                    color = Color(0xFF2563EB),
                    bg = Color(0xFFEFF6FF),
                    border = Color(0xFFDBEAFE),
                    modifier = Modifier.weight(1f)
                )
                GradingMetricCard(
                    icon = Icons.Default.EmojiEvents,
                    label = "Highest Marks",
                    value = "${stats.highest}",
                    sub = "Topper: Rohan Mehta (49/50)",
                    color = Color(0xFF16A34A),
                    bg = Color(0xFFDCFCE7),
                    border = Color(0xFFBBF7D0),
                    modifier = Modifier.weight(1f)
                )
                GradingMetricCard(
                    icon = Icons.Default.CheckCircle,
                    label = "Pass Rate",
                    value = "${stats.passRatePct}%",
                    sub = "${stats.passStudents} / ${stats.totalEntered} Passed",
                    color = Color(0xFF7C3AED),
                    bg = Color(0xFFF5F3FF),
                    border = Color(0xFFEDE9FE),
                    modifier = Modifier.weight(1f)
                )
                GradingMetricCard(
                    icon = Icons.Default.Assignment,
                    label = "Assessments",
                    value = "${assessments.size}",
                    sub = "Conducted this term",
                    color = Color(0xFFEA580C),
                    bg = Color(0xFFFFF7ED),
                    border = Color(0xFFFFEDD5),
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. ASSESSMENT SELECTOR CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Active Test:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                assessments.forEach { test ->
                    val isSelected = test.id == selectedAssessmentId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                            .clickable { selectedAssessmentId = test.id }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = test.name,
                            color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF334155),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "(${test.totalMarks}M)",
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 4. MARKS ENTRY TABLE
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Roll No", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 12.sp, modifier = Modifier.width(75.dp))
                    Text("Student Name", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Marks Obtained", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 12.sp, modifier = Modifier.width(150.dp))
                    Text("Percentage", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Text("Grade", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 12.sp, modifier = Modifier.width(75.dp))
                    Text("Attendance", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 12.sp, modifier = Modifier.width(105.dp))
                }

                // Table Rows
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(studentMarks, key = { it.studentId }, contentType = { "student_mark" }) { row ->
                        val pct = if (row.marksObtained != null && currentAssessment.totalMarks > 0) {
                            ((row.marksObtained / currentAssessment.totalMarks) * 100).toInt()
                        } else null

                        val grade = when {
                            row.isAbsent -> "AB"
                            pct == null -> "—"
                            pct >= 90 -> "A+"
                            pct >= 80 -> "A"
                            pct >= 70 -> "B+"
                            pct >= 60 -> "B"
                            pct >= 50 -> "C"
                            pct >= 40 -> "D"
                            else -> "F"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, Color(0xFFF1F5F9))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(row.rollNumber, color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.width(75.dp))

                            Text(row.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), fontSize = 13.sp, modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier.width(150.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (row.isAbsent) {
                                    Text("ABSENT", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable {
                                                val curr = row.marksObtained ?: 0.0
                                                if (curr > 0) {
                                                    val idx = studentMarks.indexOfFirst { it.studentId == row.studentId }
                                                    if (idx != -1) studentMarks[idx] = studentMarks[idx].copy(marksObtained = (curr - 0.5).coerceAtLeast(0.0))
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    }

                                    Text(
                                        text = "${row.marksObtained ?: 0.0} / ${currentAssessment.totalMarks}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable {
                                                val curr = row.marksObtained ?: 0.0
                                                if (curr < currentAssessment.totalMarks) {
                                                    val idx = studentMarks.indexOfFirst { it.studentId == row.studentId }
                                                    if (idx != -1) studentMarks[idx] = studentMarks[idx].copy(marksObtained = (curr + 0.5).coerceAtMost(currentAssessment.totalMarks.toDouble()))
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    }
                                }
                            }

                            Text(
                                text = if (row.isAbsent) "—" else "${pct ?: 0}%",
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                modifier = Modifier.width(90.dp)
                            )

                            Box(modifier = Modifier.width(75.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            when (grade) {
                                                "A+", "A" -> Color(0xFFDCFCE7)
                                                "B+", "B" -> Color(0xFFEFF6FF)
                                                "C", "D" -> Color(0xFFFEF3C7)
                                                "AB" -> Color(0xFFF1F5F9)
                                                else -> Color(0xFFFEE2E2)
                                            }
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = grade,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = when (grade) {
                                            "A+", "A" -> Color(0xFF15803D)
                                            "B+", "B" -> Color(0xFF1D4ED8)
                                            "C", "D" -> Color(0xFFB45309)
                                            "AB" -> Color(0xFF64748B)
                                            else -> Color(0xFFDC2626)
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .width(105.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (row.isAbsent) Color(0xFFFEE2E2) else Color(0xFFF1F5F9))
                                    .clickable {
                                        val idx = studentMarks.indexOfFirst { it.studentId == row.studentId }
                                        if (idx != -1) {
                                            val newAbsent = !row.isAbsent
                                            studentMarks[idx] = studentMarks[idx].copy(
                                                isAbsent = newAbsent,
                                                marksObtained = if (newAbsent) null else (currentAssessment.totalMarks * 0.7)
                                            )
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (row.isAbsent) "Mark Present" else "Mark Absent",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (row.isAbsent) Color(0xFFDC2626) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Table Footer with Save Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: ${currentAssessment.totalMarks} Marks · Instant grade computation",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(EntryGradient)
                            .clickable {
                                scope.launch {
                                    snackbar.showSnackbar("Scores saved and published for ${currentAssessment.name}!")
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Save & Publish Scores", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        // CREATE ASSESSMENT DIALOG
        if (showCreateDialog) {
            var testTitle by remember { mutableStateOf("") }
            var marksStr by remember { mutableStateOf("50") }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = {
                    Text("Schedule New Assessment", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                },
                text = {
                    Column(
                        modifier = Modifier.width(460.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = testTitle,
                            onValueChange = { testTitle = it },
                            label = { Text("Assessment Title") },
                            placeholder = { Text("e.g. Unit Test 2 - Electricity & Circuits") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = marksStr,
                                onValueChange = { marksStr = it },
                                label = { Text("Total Marks") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = classroomName.ifBlank { "Class 10-A" },
                                onValueChange = {},
                                label = { Text("Class") },
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (testTitle.isNotBlank()) {
                                val tm = marksStr.toIntOrNull() ?: 50
                                assessments.add(
                                    0,
                                    AssessmentItem(
                                        id = "test-${System.currentTimeMillis()}",
                                        name = testTitle.trim(),
                                        subject = subject.ifBlank { "Physics" },
                                        date = todayDate,
                                        totalMarks = tm,
                                        passingMarks = (tm * 0.4).toInt(),
                                        type = "UNIT_TEST",
                                        status = "SCHEDULED"
                                    )
                                )
                                showCreateDialog = false
                                scope.launch {
                                    snackbar.showSnackbar("Assessment scheduled and added to class record!")
                                }
                            }
                        }
                    ) {
                        Text("Create Test", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
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
    }
}

@Composable
private fun GradingMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
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
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(text = label, color = Color(0xFF64748B), fontSize = 11.sp)
            Text(text = value, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(1.dp))
            Text(text = sub, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
