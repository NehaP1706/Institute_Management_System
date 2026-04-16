package com.ims.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.*
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*
import java.util.Date

/** Matches "Admin Attend..." and "Manage Attendance" screens in Figma */
@Composable
fun AdminAttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedCourse by remember { mutableStateOf(StubRepository.courses.first()) }
    // Local mutable copy of attendance statuses for this session
    val statusMap = remember {
        mutableStateMapOf<String, AttendanceStatus>().also { map ->
            StubRepository.attendanceRecords.forEach { r ->
                map[r.student.studentId + "_" + r.course.courseId] = r.status
            }
        }
    }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HeaderTopBar(
                title    = "Manage Attendance",
                userName = viewModel.getCurrentUserName(),
                userRole = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin      = true,
                onNavigate   = onNavigate
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Course selector chips
            SectionHeader("Select Course")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StubRepository.courses) { course ->
                    val selected = course.courseId == selectedCourse.courseId
                    FilterChip(
                        selected = selected,
                        onClick  = { selectedCourse = course; saved = false },
                        label    = { Text(course.courseCode, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = Primary,
                            selectedLabelColor        = OnPrimary,
                            containerColor            = SurfaceVar,
                            labelColor                = OnSurface
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Advanced course screen label (matches Figma)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedCourse.title, color = Primary, fontWeight = FontWeight.Bold)
                        Text(selectedCourse.courseCode, color = OnSurface, fontSize = 12.sp)
                    }
                    Text(selectedCourse.semester, color = OnSurface, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionHeader("Mark Attendance")

            // Student list with attendance toggle
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show the sample student (in production, list all enrolled students)
                val students = listOf(StubRepository.sampleStudent)
                items(students) { student ->
                    val key = student.studentId + "_" + selectedCourse.courseId
                    val status = statusMap[key] ?: AttendanceStatus.UNMARKED
                    AttendanceMarkRow(
                        student = student,
                        status  = status,
                        onStatusChange = { newStatus ->
                            statusMap[key] = newStatus
                            saved = false
                            // Calls generateDailyReport via AttendanceRecord
                            val record = AttendanceRecord(
                                attendanceId = key,
                                student      = student,
                                course       = selectedCourse,
                                date         = Date(),
                                status       = newStatus,
                                remarks      = ""
                            )
                            viewModel.markAttendance(record)
                        }
                    )
                }
            }

            // Save Attendance Entry button (matches Figma)
            Button(
                onClick  = { saved = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null, tint = OnPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (saved) "Saved ✓" else "Save Attendance Entry",
                    fontWeight = FontWeight.Bold,
                    color      = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun AttendanceMarkRow(
    student: Student,
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceVar),
        shape    = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(
                    initials = StubRepository.getUserDisplayName(student.user)
                        .split(" ").mapNotNull { it.firstOrNull()?.toString() }
                        .take(2).joinToString(""),
                    size = 36
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        StubRepository.getUserDisplayName(student.user),
                        color      = OnBackground,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp
                    )
                    Text(student.studentId, color = OnSurface, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Status chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AttendanceStatus.values().forEach { s ->
                    val selected = status == s
                    val color = when (s) {
                        AttendanceStatus.PRESENT        -> Primary
                        AttendanceStatus.ABSENT         -> Color(0xFFEF5350)
                        AttendanceStatus.APPROVED_LEAVE -> Color(0xFFFFA726)
                        AttendanceStatus.UNMARKED       -> OnSurface
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) color else SurfaceVar)
                            .clickable { onStatusChange(s) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            s.name.take(3),
                            color      = if (selected) OnPrimary else OnSurface,
                            fontSize   = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
