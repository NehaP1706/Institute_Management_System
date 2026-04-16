package com.ims.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*

/** Matches "Admin Dashboard" screen */
@Composable
fun DashboardScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val name = viewModel.getCurrentUserName()
    val role = viewModel.getCurrentUserRole()
    val stats = viewModel.dashboardStats

    Scaffold(
        topBar = {
            HeaderTopBar(
                title    = "Dashboard",
                userName = name,
                userRole = role,
                onMenuClick  = { onNavigate(Screen.Dashboard.route) },
                onNotifClick = {}
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Profile card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape  = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(
                            initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                                .take(2).joinToString(""),
                            size = 56
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryDark)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(role, color = OnBackground, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, "Logout", tint = Primary)
                        }
                    }
                }
            }

            item { SectionHeader("Overview") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Students", "${stats["totalStudents"]}", Modifier.weight(1f))
                    StatCard("Faculty",  "${stats["totalFaculty"]}",  Modifier.weight(1f))
                    StatCard("Courses",  "${stats["totalCourses"]}",  Modifier.weight(1f))
                }
            }

            if (viewModel.isAdmin()) {
                item { SectionHeader("Management Modules") }
                item { AdminModuleGrid(onNavigate) }
            } else {
                item { SectionHeader("My Courses") }
                viewModel.allCourses.forEach { course ->
                    item {
                        CourseCard(course.title, course.courseCode, viewModel.getAttendancePercent(course.courseId))
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(title: String, code: String, attendance: Float) {
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
                Text(title, color = OnBackground, fontWeight = FontWeight.SemiBold)
                Text(code,  color = OnSurface, fontSize = 12.sp)
            }
            AttendanceBadge(attendance)
        }
    }
}

// ── Admin Module Grid ─────────────────────────────────────────────────────────
/** Matches "Admin Menu" screen */
data class ModuleItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun AdminModuleGrid(onNavigate: (String) -> Unit) {
    val modules = listOf(
        ModuleItem("Attendance",  Icons.Default.CheckCircle,   Screen.AdminAttendance.route),
        ModuleItem("Timetable",   Icons.Default.CalendarMonth, Screen.AdminTimetable.route),
        ModuleItem("Courses",     Icons.Default.Book,          Screen.CourseFilter.route),
        ModuleItem("Monthly Att", Icons.Default.BarChart,      Screen.MonthlyAttendance.route),
        ModuleItem("Messages",    Icons.Default.Message,       Screen.Dashboard.route),
        ModuleItem("Finance",     Icons.Default.AttachMoney,   Screen.Dashboard.route),
        ModuleItem("HR",          Icons.Default.People,        Screen.Dashboard.route),
        ModuleItem("News",        Icons.Default.Newspaper,     Screen.Dashboard.route),
    )
    LazyVerticalGrid(
        columns           = GridCells.Fixed(2),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(max = 600.dp)
    ) {
        items(modules) { mod ->
            Card(
                colors   = CardDefaults.cardColors(containerColor = SurfaceVar),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(mod.route) }
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(mod.icon, null, tint = Primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(mod.label, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


