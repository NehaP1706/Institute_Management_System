package com.ims.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*

/** Matches "Attendance De..." (student attendance detail) screen in Figma */
@Composable
fun AttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val records = viewModel.getAttendanceRecords()

    Scaffold(
        topBar = {
            HeaderTopBar(
                title    = "My Attendance",
                userName = viewModel.getCurrentUserName(),
                userRole = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin      = false,
                onNavigate   = onNavigate
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary cards per course
            item {
                SectionHeader("Course Summary")
            }
            items(viewModel.allCourses) { course ->
                val pct = viewModel.getAttendancePercent(course.courseId)
                AttendanceSummaryCard(
                    courseTitle = course.title,
                    courseCode  = course.courseCode,
                    percent     = pct
                )
            }

            item { SectionHeader("Recent Records") }

            items(records) { record ->
                AttendanceRecordRow(record)
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(
    courseTitle: String,
    courseCode: String,
    percent: Float
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(courseTitle, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(courseCode,  color = OnSurface,    fontSize = 12.sp)
            }
            AttendanceBadge(percent)
        }
        // Progress bar
        val color = when {
            percent >= 75f -> Primary
            percent >= 50f -> Color(0xFFFFA726)
            else           -> Color(0xFFEF5350)
        }
        LinearProgressIndicator(
            progress    = { percent / 100f },
            modifier    = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 10.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color       = color,
            trackColor  = Divider
        )
    }
}

@Composable
private fun AttendanceRecordRow(record: com.ims.app.data.model.AttendanceRecord) {
    val (statusColor, statusIcon) = when (record.status) {
        AttendanceStatus.PRESENT        -> Primary to Icons.Default.CheckCircle
        AttendanceStatus.ABSENT         -> Color(0xFFEF5350) to Icons.Default.Cancel
        AttendanceStatus.APPROVED_LEAVE -> Color(0xFFFFA726) to Icons.Default.EventAvailable
        AttendanceStatus.UNMARKED       -> OnSurface to Icons.Default.HelpOutline
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceVar),
        shape    = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(record.course.title, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(record.date.toString().take(10), color = OnSurface, fontSize = 11.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(record.status.name, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
