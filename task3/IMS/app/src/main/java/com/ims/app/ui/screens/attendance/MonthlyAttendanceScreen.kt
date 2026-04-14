package com.ims.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*

/** Matches "Monthly Atten..." and "Admin Monthl..." screens in Figma */
@Composable
fun MonthlyAttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    var selectedMonth by remember { mutableStateOf("Apr") }
    var selectedTab   by remember { mutableStateOf(0) }   // 0=Attendance, 1=Results, 2=Report

    Scaffold(
        topBar = {
            HeaderTopBar(
                title    = "Attendance – Admin",
                userName = viewModel.getCurrentUserName(),
                userRole = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin      = viewModel.isAdmin(),
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
            // Tab row (Attendance / Results / Report)
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = SurfaceVar,
                    contentColor     = Primary
                ) {
                    listOf("Attendance", "Results", "Report").forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick  = { selectedTab = idx },
                            text     = {
                                Text(
                                    title,
                                    color    = if (selectedTab == idx) Primary else OnSurface,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
            }

            // Month chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(months) { m ->
                        val sel = m == selectedMonth
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (sel) Primary else SurfaceVar)
                                .clickable { selectedMonth = m }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                m,
                                color      = if (sel) OnPrimary else OnSurface,
                                fontSize   = 12.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "APR 2024",
                    color      = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            // Calendar grid
            item { MonthCalendar(selectedMonth) }

            item { SectionHeader("Summary") }

            // Summary legend
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("Present",      Primary,              18)
                    LegendItem("Absent",       Color(0xFFEF5350),    5)
                    LegendItem("Leave",        Color(0xFFFFA726),    2)
                    LegendItem("Holiday",      Divider,              3)
                }
            }

            item { SectionHeader("Course-wise Breakdown") }

            items(viewModel.allCourses) { course ->
                val pct = viewModel.getAttendancePercent(course.courseId)
                CourseAttendanceRow(course.title, course.courseCode, pct)
            }

            // Export button
            item {
                Button(
                    onClick  = { /* generateMonthlyReport stub */ },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, null, tint = OnBackground)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Report", color = OnBackground, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(month: String) {
    val days = listOf("M","T","W","T","F","S","S")
    // Stub: 30 day grid, colour coded
    val attendanceMap = mapOf(
        1 to AttendanceStatus.PRESENT, 2 to AttendanceStatus.PRESENT,
        3 to AttendanceStatus.ABSENT,  4 to AttendanceStatus.PRESENT,
        5 to AttendanceStatus.PRESENT, 7 to AttendanceStatus.APPROVED_LEAVE,
        8 to AttendanceStatus.PRESENT, 9 to AttendanceStatus.PRESENT,
        10 to AttendanceStatus.ABSENT, 11 to AttendanceStatus.PRESENT,
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape  = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Day headers
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                days.forEach { d ->
                    Text(d, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(4.dp))
            // 5 weeks
            for (week in 0 until 5) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (d in 0 until 7) {
                        val dayNum = week * 7 + d + 1
                        val status = attendanceMap[dayNum]
                        val bgColor = when (status) {
                            AttendanceStatus.PRESENT        -> Primary.copy(alpha = 0.8f)
                            AttendanceStatus.ABSENT         -> Color(0xFFEF5350).copy(alpha = 0.8f)
                            AttendanceStatus.APPROVED_LEAVE -> Color(0xFFFFA726).copy(alpha = 0.8f)
                            else -> if (dayNum <= 30) SurfaceVar else Color.Transparent
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum <= 30)
                                Text(
                                    "$dayNum",
                                    color    = if (status != null) OnPrimary else OnSurface,
                                    fontSize = 10.sp
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("$count", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = OnSurface, fontSize = 10.sp)
    }
}

@Composable
private fun CourseAttendanceRow(title: String, code: String, pct: Float) {
    val barColor = when {
        pct >= 75f -> Primary
        pct >= 50f -> Color(0xFFFFA726)
        else       -> Color(0xFFEF5350)
    }
    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceVar),
        shape    = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(code,  color = OnSurface,    fontSize = 11.sp)
                }
                Text("${pct.toInt()}%", color = barColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress   = { pct / 100f },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color      = barColor,
                trackColor = Divider
            )
        }
    }
}

