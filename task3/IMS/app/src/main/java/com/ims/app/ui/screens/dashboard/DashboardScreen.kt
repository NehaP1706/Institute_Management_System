package com.ims.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*

// ── Extra local colours not in Theme.kt ──────────────────────────────────────
private val BannerBg   = Color(0xFF006B62)  // rich teal for profile banner
private val SoonCardBg = Color(0xFF162E3E)  // muted navy for "soon" module cards

/** Figma "Dashboard" screen – renders correctly for Student AND Admin roles */
@Composable
fun DashboardScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val name  = viewModel.getCurrentUserName()
    val role  = viewModel.getCurrentUserRole()
    val stats = viewModel.dashboardStats

    val attendancePct = if (!viewModel.isAdmin()) {
        val firstId = viewModel.allCourses.firstOrNull()?.courseId ?: ""
        "${viewModel.getAttendancePercent(firstId).toInt()}%"
    } else "—"

    Scaffold(
        topBar = {
            DashboardTopBar(title = "Dashboard", userName = name, userRole = role, onNavigate = onNavigate)
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, isAdmin = viewModel.isAdmin(), onNavigate = onNavigate)
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Profile banner
            item {
                ProfileBanner(
                    name     = name,
                    role     = role,
                    email    = viewModel.getCurrentUserEmail(),
                    onLogout = onLogout
                )
            }

            // Quick-stat pills
            item {
                if (viewModel.isAdmin()) {
                    StatRow(listOf(
                        StatData("${stats["totalStudents"]}", "STUDENTS"),
                        StatData("${stats["totalFaculty"]}",  "FACULTY"),
                        StatData("${stats["totalCourses"]}",  "COURSES")
                    ))
                } else {
                    StatRow(listOf(
                        StatData(attendancePct, "ATTENDANCE"),
                        StatData("2",           "ALERTS"),
                        StatData("18",          "CREDITS")
                    ))
                }
            }

            // Section heading
            item {
                Text(
                    text          = if (viewModel.isAdmin()) "MANAGEMENT MODULES" else "ACADEMIC MODULES",
                    color         = OnSurface,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp
                )
            }

            // Module grid
            item {
                if (viewModel.isAdmin()) AdminModuleGrid(onNavigate)
                else StudentModuleGrid(onNavigate)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    title: String,
    userName: String,
    userRole: String,
    onNavigate: (String) -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(title,    color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(userName, color = Primary,      fontSize   = 12.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = { onNavigate(Screen.Dashboard.route) }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = OnBackground)
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = OnBackground)
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(userRole, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
    )
}

// ── Profile Banner ────────────────────────────────────────────────────────────
@Composable
private fun ProfileBanner(
    name: String,
    role: String,
    email: String,
    onLogout: () -> Unit
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2).joinToString("")

    Card(
        colors   = CardDefaults.cardColors(containerColor = BannerBg),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(name, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Spacer(Modifier.height(4.dp))

                // Role chip
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(role.uppercase(), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(4.dp))
                Text(email, color = OnBackground.copy(alpha = 0.75f), fontSize = 12.sp)

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime, null,
                        tint     = OnBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Last active: Today, 9:41 AM", color = OnBackground.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }

            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Primary)
            }
        }
    }
}

// ── Stat Row ──────────────────────────────────────────────────────────────────
private data class StatData(val value: String, val label: String)

@Composable
private fun StatRow(stats: List<StatData>) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { stat ->
            Card(
                colors   = CardDefaults.cardColors(containerColor = CardBg),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier            = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stat.value, color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(stat.label, color = OnSurface, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Student Module Grid ───────────────────────────────────────────────────────
private data class StudentModule(
    val label: String, val icon: ImageVector, val route: String, val isSoon: Boolean = false
)

@Composable
private fun StudentModuleGrid(onNavigate: (String) -> Unit) {
    val modules = listOf(
        StudentModule("Attendance", Icons.Default.CheckCircle,   Screen.Attendance.route),
        StudentModule("Timetable",  Icons.Default.CalendarMonth, Screen.Timetable.route),
        StudentModule("Exams",      Icons.Default.Assignment,    Screen.Dashboard.route, isSoon = true),
        StudentModule("Fee Status", Icons.Default.CreditCard,    Screen.Dashboard.route, isSoon = true),
        StudentModule("Messages",   Icons.Default.Message,       Screen.Dashboard.route, isSoon = true),
        StudentModule("Leave",      Icons.Default.ExitToApp,     Screen.Dashboard.route, isSoon = true),
    )
    ModuleGrid(
        entries    = modules.map { ModuleEntry(it.label, it.icon, it.route, it.isSoon) },
        onNavigate = onNavigate
    )
}

// ── Admin Module Grid ─────────────────────────────────────────────────────────
/** Kept public so other screens can reference it if needed */
data class ModuleItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun AdminModuleGrid(onNavigate: (String) -> Unit) {
    val modules = listOf(
        ModuleEntry("Attendance",  Icons.Default.CheckCircle,   Screen.AdminAttendance.route),
        ModuleEntry("Timetable",   Icons.Default.CalendarMonth, Screen.AdminTimetable.route),
        ModuleEntry("Courses",     Icons.Default.Book,          Screen.CourseFilter.route),
        ModuleEntry("Monthly Att", Icons.Default.BarChart,      Screen.MonthlyAttendance.route),
        ModuleEntry("Messages",    Icons.Default.Message,       Screen.Dashboard.route),
        ModuleEntry("Finance",     Icons.Default.AttachMoney,   Screen.Dashboard.route),
        ModuleEntry("HR",          Icons.Default.People,        Screen.Dashboard.route),
        ModuleEntry("News",        Icons.Default.Newspaper,     Screen.Dashboard.route),
    )
    ModuleGrid(entries = modules, onNavigate = onNavigate)
}

// ── Shared grid + card ────────────────────────────────────────────────────────
private data class ModuleEntry(
    val label: String, val icon: ImageVector, val route: String, val isSoon: Boolean = false
)

@Composable
private fun ModuleGrid(entries: List<ModuleEntry>, onNavigate: (String) -> Unit) {
    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.heightIn(max = 600.dp)
    ) {
        items(entries) { entry ->
            ModuleCard(
                label   = entry.label,
                icon    = entry.icon,
                isSoon  = entry.isSoon,
                onClick = { if (!entry.isSoon) onNavigate(entry.route) }
            )
        }
    }
}

@Composable
private fun ModuleCard(label: String, icon: ImageVector, isSoon: Boolean, onClick: () -> Unit) {
    val bg = if (isSoon) SoonCardBg else SurfaceVar

    Card(
        colors   = CardDefaults.cardColors(containerColor = bg),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clickable(enabled = !isSoon, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {

            if (isSoon) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("SOON", color = OnSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSoon) Primary.copy(alpha = 0.12f) else Primary.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint     = if (isSoon) OnSurface else Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    label,
                    color      = if (isSoon) OnSurface else OnBackground,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}