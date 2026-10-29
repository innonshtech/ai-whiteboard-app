package com.avitrisphare.whiteboard.presentation.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class TimetablePeriod(
    val id: String,
    val day: String,
    val periodNumber: Int,
    val startTime: String,
    val endTime: String,
    val subject: String,
    val className: String,
    val roomNumber: String,
    val isSubstitute: Boolean = false,
    val substituteNotes: String? = null,
    val isCurrent: Boolean = false
)

@Immutable
data class AcademicEvent(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val type: String,
    val color: Color
)

@Composable
fun TimetableScreen(
    teacher: PanelTeacher,
    schoolName: String,
    classroomName: String,
    subject: String,
    onBack: () -> Unit,
    onLaunchClassBoard: (className: String, subject: String) -> Unit
) {
    val todayDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val currentDayName = remember {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    var selectedView by remember { mutableStateOf("TODAY") }
    var selectedDayTab by remember {
        mutableStateOf(if (currentDayName in listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")) currentDayName else "Monday")
    }

    val daysOfWeek = remember { listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday") }

    val allPeriods = remember {
        listOf(
            TimetablePeriod("p-mon-1", "Monday", 1, "08:30 AM", "09:15 AM", "Physics Theory", "Class 10-A", "Room 204"),
            TimetablePeriod("p-mon-2", "Monday", 2, "09:20 AM", "10:05 AM", "Physics Lab", "Class 10-A", "Physics Lab 1"),
            TimetablePeriod("p-mon-3", "Monday", 3, "10:20 AM", "11:05 AM", "General Science", "Class 8-B", "Room 108", isSubstitute = true, substituteNotes = "Covering for Mr. R. Sharma"),
            TimetablePeriod("p-mon-4", "Monday", 4, "11:10 AM", "11:55 AM", "Physics Problem Solving", "Class 10-B", "Room 206"),
            TimetablePeriod("p-mon-5", "Monday", 5, "12:40 PM", "01:25 PM", "Practical Demonstration", "Class 9-A", "Room 102"),

            TimetablePeriod("p-tue-1", "Tuesday", 1, "08:30 AM", "09:15 AM", "Physics", "Class 10-B", "Room 206"),
            TimetablePeriod("p-tue-2", "Tuesday", 2, "09:20 AM", "10:05 AM", "Physics", "Class 9-A", "Room 102"),
            TimetablePeriod("p-tue-3", "Tuesday", 3, "10:20 AM", "11:05 AM", "Science Olympiad Prep", "Class 10-A", "AV Room 2"),
            TimetablePeriod("p-tue-4", "Tuesday", 4, "11:10 AM", "11:55 AM", "Physics Lab", "Class 10-B", "Physics Lab 1"),

            TimetablePeriod("p-wed-1", "Wednesday", 1, "08:30 AM", "09:15 AM", "Physics Theory", "Class 10-A", "Room 204"),
            TimetablePeriod("p-wed-2", "Wednesday", 2, "09:20 AM", "10:05 AM", "Electricity & Magnetism", "Class 10-A", "Room 204"),
            TimetablePeriod("p-wed-3", "Wednesday", 3, "10:20 AM", "11:05 AM", "Foundation Physics", "Class 9-B", "Room 105"),
            TimetablePeriod("p-wed-4", "Wednesday", 4, "11:10 AM", "11:55 AM", "Remedial Coaching", "Class 10-B", "Room 206"),

            TimetablePeriod("p-thu-1", "Thursday", 1, "08:30 AM", "09:15 AM", "Physics Theory", "Class 10-A", "Room 204", isCurrent = true),
            TimetablePeriod("p-thu-2", "Thursday", 2, "09:20 AM", "10:05 AM", "Physics", "Class 10-B", "Room 206"),
            TimetablePeriod("p-thu-3", "Thursday", 3, "10:20 AM", "11:05 AM", "Science Olympiad Guidance", "Class 10-A", "AV Room 2"),
            TimetablePeriod("p-thu-4", "Thursday", 4, "11:10 AM", "11:55 AM", "Chemistry Practical", "Class 9-A", "Chemistry Lab", isSubstitute = true, substituteNotes = "Acid and Base titration demo"),

            TimetablePeriod("p-fri-1", "Friday", 1, "08:30 AM", "09:15 AM", "Physics Weekly Revision", "Class 10-A", "Room 204"),
            TimetablePeriod("p-fri-2", "Friday", 2, "09:20 AM", "10:05 AM", "Physics Weekly Revision", "Class 10-B", "Room 206"),
            TimetablePeriod("p-fri-3", "Friday", 3, "10:20 AM", "11:05 AM", "Conceptual Test", "Class 9-A", "Room 102"),
            TimetablePeriod("p-fri-4", "Friday", 4, "11:10 AM", "11:55 AM", "Interactive Simulations", "Class 10-A", "Physics Lab 1")
        )
    }

    val upcomingEvents = remember {
        listOf(
            AcademicEvent("ev-1", "Parent-Teacher Meeting (PTM)", "Saturday, 31 Oct", "09:00 AM - 01:00 PM", "PTM", Color(0xFF7C3AED)),
            AcademicEvent("ev-2", "Physics Lab Practical Assessment", "Tuesday, 3 Nov", "10:00 AM - 12:00 PM", "EXAM", Color(0xFFEA580C)),
            AcademicEvent("ev-3", "Faculty Curriculum Review", "Friday, 6 Nov", "03:00 PM - 04:00 PM", "MEETING", Color(0xFF2563EB))
        )
    }

    val dayPeriods = remember(selectedDayTab, allPeriods) {
        allPeriods.filter { it.day.equals(selectedDayTab, ignoreCase = true) }
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
                title = "Weekly Timetable",
                subtitle = todayDate,
                onBack = onBack,
                teacher = teacher,
                schoolName = schoolName,
                classroomName = classroomName.ifBlank { "Class 10-A" },
                subject = subject.ifBlank { "Physics" }
            )

            // 2. VIEW CONTROLS & DAY TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Selector (Today vs Weekly)
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
                            .background(if (selectedView == "TODAY") Color.White else Color.Transparent)
                            .clickable { selectedView = "TODAY" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Daily Schedule",
                            color = if (selectedView == "TODAY") Color(0xFF2563EB) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedView == "WEEKLY") Color.White else Color.Transparent)
                            .clickable { selectedView = "WEEKLY" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Weekly Matrix",
                            color = if (selectedView == "WEEKLY") Color(0xFF2563EB) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.width(18.dp))

                // Day selector chips (for Daily View)
                if (selectedView == "TODAY") {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysOfWeek.forEach { day ->
                            val isSelected = selectedDayTab == day
                            val isToday = currentDayName.equals(day, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isSelected) Color(0xFFEFF6FF) else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { selectedDayTab = day }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day,
                                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF475569),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                                if (isToday) {
                                    Spacer(Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "5-Day Master Matrix · All Teaching Periods",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. MAIN CONTENT
            if (selectedView == "TODAY") {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Periods Column (70%)
                    Column(
                        modifier = Modifier
                            .weight(0.70f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        dayPeriods.forEach { period ->
                            DailyPeriodCard(
                                period = period,
                                onLaunchBoard = {
                                    onLaunchClassBoard(period.className, period.subject)
                                }
                            )
                        }
                    }

                    // Side Panel: Today's Summary & Upcoming (30%)
                    Column(
                        modifier = Modifier
                            .weight(0.30f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Day Stats
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "$selectedDayTab Summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Periods", color = Color(0xFF64748B), fontSize = 11.sp)
                                        Text("${dayPeriods.size}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF2563EB))
                                    }
                                    Column {
                                        Text("Teaching Time", color = Color(0xFF64748B), fontSize = 11.sp)
                                        Text("${dayPeriods.size * 45} mins", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF059669))
                                    }
                                    Column {
                                        Text("Classes", color = Color(0xFF64748B), fontSize = 11.sp)
                                        Text("${dayPeriods.map { it.className }.distinct().size}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF7C3AED))
                                    }
                                }
                            }
                        }

                        // Upcoming Events Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Important Academic Dates",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    upcomingEvents.forEach { ev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(ev.color)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ev.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(Modifier.height(1.dp))
                                                Text(
                                                    text = "${ev.date} · ${ev.time}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Weekly Grid View (5-day matrix)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    daysOfWeek.forEach { day ->
                        val gridPeriods = remember(day, allPeriods) {
                            allPeriods.filter { it.day.equals(day, ignoreCase = true) }
                        }
                        val isToday = currentDayName.equals(day, ignoreCase = true)

                        Column(
                            modifier = Modifier
                                .width(240.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isToday) Color(0xFFF0FDF4) else Color.White)
                                .border(
                                    1.dp,
                                    if (isToday) Color(0xFF86EFAC) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isToday) Color(0xFF15803D) else Color(0xFF0F172A)
                                )
                                if (isToday) {
                                    Spacer(Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Today", color = Color(0xFF15803D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "${gridPeriods.size} periods",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }

                            // Period Cards inside Column
                            gridPeriods.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .clickable { onLaunchClassBoard(p.className, p.subject) }
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "P${p.periodNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF2563EB)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "${p.startTime} - ${p.endTime}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = p.subject,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = p.className,
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("•", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = p.roomNumber,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyPeriodCard(
    period: TimetablePeriod,
    onLaunchBoard: () -> Unit
) {
    val isLive = period.isCurrent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                if (isLive) 1.5.dp else 1.dp,
                if (isLive) Color(0xFF3B82F6) else Color(0xFFE2E8F0),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Period Number Pill
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isLive) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                .border(
                    1.dp,
                    if (isLive) Color(0xFFDBEAFE) else Color(0xFFE2E8F0),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "P${period.periodNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isLive) Color(0xFF2563EB) else Color(0xFF334155)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = period.subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                if (isLive) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "● LIVE NOW",
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                if (period.isSubstitute) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Substitute",
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${period.startTime} - ${period.endTime}",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = period.className,
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = period.roomNumber,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            if (!period.substituteNotes.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Note: ${period.substituteNotes}",
                    color = Color(0xFFD97706),
                    fontSize = 11.sp
                )
            }
        }

        // Action: Launch Whiteboard
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (isLive) EntryGradient else Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFEFF6FF))))
                .border(1.dp, if (isLive) Color.Transparent else Color(0xFFDBEAFE), RoundedCornerShape(999.dp))
                .clickable(onClick = onLaunchBoard)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Draw,
                contentDescription = null,
                tint = if (isLive) Color.White else Color(0xFF2563EB),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (isLive) "Launch Board Now" else "Start Board",
                color = if (isLive) Color.White else Color(0xFF2563EB),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
