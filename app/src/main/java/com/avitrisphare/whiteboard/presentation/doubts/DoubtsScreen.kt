package com.avitrisphare.whiteboard.presentation.doubts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
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
data class StudentDoubt(
    val id: String,
    val studentName: String,
    val studentRoll: String,
    val classroomName: String,
    val chapterTag: String,
    val question: String,
    val hasAttachment: Boolean,
    val attachmentName: String? = null,
    val timeAgo: String,
    val isResolved: Boolean = false,
    val isOverdue: Boolean = false,
    val teacherReply: String? = null
)

@Composable
fun DoubtsScreen(
    teacher: PanelTeacher,
    schoolName: String,
    classroomName: String,
    subject: String,
    onBack: () -> Unit,
    onSolveOnWhiteboard: (title: String, question: String) -> Unit
) {
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val doubts = remember {
        mutableStateListOf(
            StudentDoubt(
                id = "doubt-1",
                studentName = "Aarav Sharma",
                studentRoll = "10A-01",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                chapterTag = "Chapter 9: Laws of Motion",
                question = "Sir, in problem 4 from page 128, when two masses are connected by a light inextensible string over a frictionless pulley, why is the tension considered constant throughout? Attached my calculation photo.",
                hasAttachment = true,
                attachmentName = "pulley_calculation_steps.jpg",
                timeAgo = "2h ago",
                isResolved = false,
                isOverdue = false
            ),
            StudentDoubt(
                id = "doubt-2",
                studentName = "Priya Patel",
                studentRoll = "10A-18",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                chapterTag = "Chapter 10: Light & Reflection",
                question = "How do we determine if the image formed by a concave mirror is virtual vs real when object is positioned between Focus (F) and Pole (P)? Does magnification formula sign convention change?",
                hasAttachment = false,
                timeAgo = "5h ago",
                isResolved = false,
                isOverdue = false
            ),
            StudentDoubt(
                id = "doubt-3",
                studentName = "Dev Patel",
                studentRoll = "10A-03",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                chapterTag = "Chapter 12: Electricity",
                question = "In parallel circuits, why does total resistance decrease as more resistors are added? Can we solve an example with 3 parallel resistors on the whiteboard?",
                hasAttachment = false,
                timeAgo = "1d ago",
                isResolved = true,
                teacherReply = "Because each parallel branch offers an independent path for current! Total conductance G = 1/R1 + 1/R2 + 1/R3 increases, so overall resistance drops."
            ),
            StudentDoubt(
                id = "doubt-4",
                studentName = "Rhea Sengupta",
                studentRoll = "10A-12",
                classroomName = classroomName.ifBlank { "Class 10-A" },
                chapterTag = "Chapter 13: Magnetic Effects",
                question = "Could you please explain Fleming's Left-Hand Rule vs Right-Hand Rule again? I am confusing motor effect with generator induction during practice questions.",
                hasAttachment = true,
                attachmentName = "hand_rule_diagram_notes.png",
                timeAgo = "2d ago",
                isResolved = false,
                isOverdue = true
            )
        )
    }

    var selectedDoubtId by remember { mutableStateOf(doubts.firstOrNull()?.id) }
    val currentDoubt = remember(selectedDoubtId, doubts.toList()) { doubts.find { it.id == selectedDoubtId } }
    var replyText by remember { mutableStateOf("") }

    val filteredDoubts = remember(doubts.toList(), searchQuery, selectedFilter) {
        doubts.filter { d ->
            val matchesSearch = searchQuery.isBlank() ||
                d.studentName.contains(searchQuery, ignoreCase = true) ||
                d.chapterTag.contains(searchQuery, ignoreCase = true) ||
                d.question.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "OPEN" -> !d.isResolved
                "RESOLVED" -> d.isResolved
                else -> true
            }
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
            // 1. TOP HEADER
            IFPScreenHeader(
                title = "Student Doubts",
                subtitle = "In-Class & Remote Queries · $todayDate",
                onBack = onBack,
                teacher = teacher,
                schoolName = schoolName,
                classroomName = classroomName.ifBlank { "Class 10-A" },
                subject = subject.ifBlank { "Physics" }
            )

            // 2. MASTER-DETAIL SPLIT LAYOUT (35% List, 65% Interactive Detail & Solving)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // LEFT PANE: DOUBTS FEED
                Column(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search & Filter Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Search field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) Text("Filter doubts…", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        inner()
                                    }
                                )
                            }

                            // Filter Pills
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(label = "All", count = doubts.size, isSelected = selectedFilter == "ALL") { selectedFilter = "ALL" }
                                FilterChip(label = "Open", count = doubts.count { !it.isResolved }, isSelected = selectedFilter == "OPEN", color = Color(0xFFEA580C)) { selectedFilter = "OPEN" }
                                FilterChip(label = "Resolved", count = doubts.count { it.isResolved }, isSelected = selectedFilter == "RESOLVED", color = Color(0xFF16A34A)) { selectedFilter = "RESOLVED" }
                            }
                        }
                    }

                    // Doubts List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDoubts, key = { it.id }, contentType = { "doubt" }) { doubt ->
                            val isSelected = doubt.id == selectedDoubtId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedDoubtId = doubt.id }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (doubt.isResolved) Color(0xFFDCFCE7) else Color(0xFFFFF7ED)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (doubt.isResolved) Icons.Default.CheckCircle else Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (doubt.isResolved) Color(0xFF15803D) else Color(0xFFEA580C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = doubt.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = doubt.timeAgo,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        text = doubt.chapterTag,
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = doubt.question,
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT PANE: DETAIL & INTERACTION
                if (currentDoubt != null) {
                    Column(
                        modifier = Modifier
                            .weight(0.64f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header of Doubt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentDoubt.studentName.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentDoubt.studentName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("• Roll: ${currentDoubt.studentRoll}", color = Color(0xFF64748B), fontSize = 12.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("• ${currentDoubt.classroomName}", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = currentDoubt.chapterTag,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }

                            // Status Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (currentDoubt.isResolved) Color(0xFFDCFCE7) else Color(0xFFFFF7ED))
                                    .border(1.dp, if (currentDoubt.isResolved) Color(0xFFBBF7D0) else Color(0xFFFFEDD5), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (currentDoubt.isResolved) "Resolved" else "Needs Answer",
                                    color = if (currentDoubt.isResolved) Color(0xFF15803D) else Color(0xFFEA580C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Question Body Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = currentDoubt.question,
                                    color = Color(0xFF1E293B),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )

                                if (currentDoubt.hasAttachment) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Attachment,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = currentDoubt.attachmentName ?: "student_notebook_photo.jpg",
                                            color = Color(0xFF2563EB),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "(Attached photo)",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // KILLER IFP FEATURE: "SOLVE LIVE ON WHITEBOARD"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFEFF6FF), Color(0xFFF5F3FF))
                                    )
                                )
                                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Explain this Doubt on Whiteboard",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E40AF)
                                )
                                Text(
                                    text = "Opens this query directly on canvas for live in-class solving",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(EntryGradient)
                                    .clickable {
                                        onSolveOnWhiteboard(currentDoubt.chapterTag, currentDoubt.question)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Send to Board",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Teacher Reply Section
                        if (currentDoubt.teacherReply != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDF4))
                                    .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Answered by ${teacher.name}", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = currentDoubt.teacherReply,
                                        color = Color(0xFF166534),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            // Quick Reply Input
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    label = { Text("Teacher's Explanation / Reply") },
                                    placeholder = { Text("Type written answer, hint, or formula explanation...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFF2563EB))
                                            .clickable {
                                                if (replyText.isNotBlank()) {
                                                    val idx = doubts.indexOfFirst { it.id == currentDoubt.id }
                                                    if (idx != -1) {
                                                        doubts[idx] = doubts[idx].copy(
                                                            teacherReply = replyText.trim(),
                                                            isResolved = true
                                                        )
                                                    }
                                                    replyText = ""
                                                    scope.launch {
                                                        snackbar.showSnackbar("Doubt answered and synced with ${currentDoubt.studentName}!")
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Send Answer & Resolve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
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
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (isSelected) color else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = if (isSelected) color else Color(0xFF475569),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "$count",
                color = if (isSelected) color else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
