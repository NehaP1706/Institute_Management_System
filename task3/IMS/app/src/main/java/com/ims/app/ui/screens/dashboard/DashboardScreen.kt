package com.ims.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Local colours ─────────────────────────────────────────────────────────────
private val BannerGradientStart = Color(0xFF00A896)
private val BannerGradientEnd   = Color(0xFF006B62)
private val StatCardBg          = Color(0xFF0D2233)
private val MenuItemActiveBg    = Color(0xFF00A896)
private val MenuItemSoonBg      = Color(0xFF0D2233)
private val SubtextColor        = Color(0xFF607D8B)
private val OnBanner            = Color.White
private val GreenAccent         = Color(0xFF00E5B0)
private val RedAccent           = Color(0xFFFF5252)
private val GlobeIcon           = Color(0xFF455A64)
private val ModuleActiveBg      = Color(0xFF00897B)
private val ModuleSoonBg        = Color(0xFF0D2233)
private val DividerColor        = Color(0xFF1E3A4A)

// ── Shared menu data model ────────────────────────────────────────────────────
private data class AdminMenuItem(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val isSoon: Boolean = false,
    val isActive: Boolean = false
)

// ── Main Dashboard Screen ─────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val name      = viewModel.getCurrentUserName()
    val role      = viewModel.getCurrentUserRole()
    val email     = viewModel.getCurrentUserEmail()
    val stats     = viewModel.dashboardStats

    val isAdmin   = viewModel.isAdmin()
    val isFaculty = viewModel.isFaculty()
    val isStudent = !isAdmin && !isFaculty

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DashboardTopBar(onMenuClick = { showMenu = true }, onLogout = onLogout)
        },
        bottomBar = {
            if (isStudent) {
                StudentBottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate)
            } else {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Language, "Language", tint = GlobeIcon, modifier = Modifier.size(26.dp))
                }
            }
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Profile banner — all roles
            item {
                ProfileBanner(name = name, role = role, email = email)
            }

            // ── Student body ────────────────────────────────────────────────
            if (isStudent) {
                item {
                    // Compute attendance across all courses — no hardcoded values
                    val perCoursePercents = viewModel.allCourses
                        .map { viewModel.getAttendancePercent(it.courseId) }
                    val overallAttendance = if (perCoursePercents.isEmpty()) 0f
                    else perCoursePercents.average().toFloat()

                    // Alerts = courses below the 75% threshold
                    val alertCount = viewModel.allCourses
                        .count { viewModel.getAttendancePercent(it.courseId) < 75f }

                    // Credits = sum of enrolled course credits
                    val totalCredits = viewModel.allCourses.sumOf { it.credits }

                    StudentStatRow(
                        attendancePercent = overallAttendance,
                        alertCount        = alertCount,
                        totalCredits      = totalCredits
                    )
                }

                item {
                    Text(
                        "ACADEMIC MODULES",
                        color         = SubtextColor,
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }

                item {
                    StudentModuleGrid(onNavigate = onNavigate)
                }
            }

            // ── Faculty body ────────────────────────────────────────────────
            if (isFaculty) {
                item { FacultyQuickActions(onNavigate = onNavigate) }
            }

            // ── Admin body ──────────────────────────────────────────────────
            if (isAdmin) {
                item {
                    AdminStatCards(
                        students         = "${stats["totalStudents"]    ?: 0}",
                        faculty          = "${stats["totalFaculty"]     ?: 0}",
                        pendingApprovals = "${stats["pendingApprovals"] ?: 0}"
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Role-appropriate slide-out menu
    if (showMenu) {
        when {
            isAdmin   -> AdminMenuOverlay(
                onNavigate = { route -> showMenu = false; onNavigate(route) },
                onDismiss  = { showMenu = false }
            )
            isFaculty -> FacultyMenuOverlay(
                onNavigate = { route -> showMenu = false; onNavigate(route) },
                onDismiss  = { showMenu = false }
            )
            else      -> StudentMenuOverlay(
                onNavigate = { route -> showMenu = false; onNavigate(route) },
                onDismiss  = { showMenu = false }
            )
        }
    }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(onMenuClick: () -> Unit, onLogout: () -> Unit) {
    TopAppBar(
        title = { Text("Dashboard", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu", tint = OnBackground) }
        },
        actions = {
            IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, "Logout", tint = Primary) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

// ── Profile Banner ────────────────────────────────────────────────────────────
@Composable
private fun ProfileBanner(name: String, role: String, email: String) {
    val lastActive = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(BannerGradientStart, BannerGradientEnd)))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Avatar with online dot
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier         = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BannerGradientEnd.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = OnBanner.copy(alpha = 0.85f), modifier = Modifier.size(34.dp))
                }
                // Green dot with background ring so it's visible on any gradient
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(BannerGradientEnd)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(GreenAccent))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + role chip on same row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = OnBanner, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OnBanner.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            role.uppercase(),
                            color         = OnBanner,
                            fontSize      = 9.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(email, color = OnBanner.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = OnBanner.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Last active: Today, $lastActive", color = OnBanner.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

// ── Student Stat Row ──────────────────────────────────────────────────────────
@Composable
private fun StudentStatRow(
    attendancePercent: Float,
    alertCount: Int,
    totalCredits: Int
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = StatCardBg),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            StatCell(
                value      = "${attendancePercent.toInt()}%",
                label      = "ATTENDANCE",
                valueColor = when {
                    attendancePercent >= 75f -> GreenAccent
                    attendancePercent >= 60f -> Color(0xFFFFB300)
                    else                    -> RedAccent
                }
            )

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(DividerColor))

            StatCell(
                value      = "$alertCount",
                label      = "ALERTS",
                valueColor = if (alertCount > 0) RedAccent else GreenAccent
            )

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(DividerColor))

            StatCell(
                value      = "$totalCredits",
                label      = "CREDITS",
                valueColor = OnBackground
            )
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = SubtextColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    }
}

// ── Student Module Grid ───────────────────────────────────────────────────────
private data class ModuleItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val isSoon: Boolean = false
)

@Composable
private fun StudentModuleGrid(onNavigate: (String) -> Unit) {
    val modules = listOf(
        ModuleItem("Attendance",  Icons.Default.Fingerprint,           Screen.Attendance.route),
        ModuleItem("Timetable",   Icons.Default.Schedule,              Screen.Timetable.route),
        ModuleItem("Exams",       Icons.Default.AssignmentTurnedIn,    Screen.Dashboard.route, isSoon = true),
        ModuleItem("Fee Status",  Icons.Default.AccountBalanceWallet,  Screen.Dashboard.route, isSoon = true),
        ModuleItem("Messages",    Icons.Default.Message,               Screen.Dashboard.route, isSoon = true),
        ModuleItem("Leave",       Icons.Default.EventAvailable,        Screen.Dashboard.route, isSoon = true),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        modules.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { module ->
                    ModuleCard(module = module, modifier = Modifier.weight(1f), onNavigate = onNavigate)
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ModuleCard(module: ModuleItem, modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (module.isSoon) ModuleSoonBg else ModuleActiveBg)
            .clickable(enabled = !module.isSoon) { onNavigate(module.route) }
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column {
            if (module.isSoon) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SubtextColor.copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("SOON", color = SubtextColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(4.dp))
            }
            Icon(
                module.icon, null,
                tint     = if (module.isSoon) SubtextColor else OnBanner,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                module.label,
                color      = if (module.isSoon) SubtextColor else OnBanner,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp
            )
        }
    }
}

// ── Student Bottom Nav Bar ────────────────────────────────────────────────────
@Composable
private fun StudentBottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple(Screen.Dashboard.route,  Icons.Default.Home,        "HOME"),
        Triple(Screen.Attendance.route, Icons.Default.Fingerprint, "ATTENDANCE"),
        Triple(Screen.Timetable.route,  Icons.Default.Schedule,    "TIMETABLE"),
    )
    NavigationBar(containerColor = Background, tonalElevation = 0.dp) {
        items.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(route) },
                icon     = { Icon(icon, label, modifier = Modifier.size(22.dp)) },
                label    = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = OnBackground,
                    unselectedIconColor = SubtextColor,
                    selectedTextColor   = OnBackground,
                    unselectedTextColor = SubtextColor,
                    indicatorColor      = MenuItemActiveBg
                )
            )
        }
    }
}

// ── Faculty Quick Actions ─────────────────────────────────────────────────────
@Composable
private fun FacultyQuickActions(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("QUICK ACTIONS", color = SubtextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FacultyActionCard("Mark Attendance", Icons.Default.Fingerprint, Modifier.weight(1f)) { onNavigate(Screen.AdminAttendance.route) }
            FacultyActionCard("Timetable", Icons.Default.Schedule, Modifier.weight(1f)) { onNavigate(Screen.Timetable.route) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FacultyActionCard("Courses", Icons.Default.MenuBook, Modifier.weight(1f)) { onNavigate(Screen.CourseFilter.route) }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FacultyActionCard(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ModuleActiveBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column {
            Icon(icon, null, tint = OnBanner, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(10.dp))
            Text(label, color = OnBanner, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ── Admin Stat Cards ──────────────────────────────────────────────────────────
private data class AdminStat(val label: String, val value: String, val sub: String, val subColor: Color)

@Composable
private fun AdminStatCards(students: String, faculty: String, pendingApprovals: String) {
    val items = listOf(
        AdminStat("TOTAL STUDENTS",    students,         "↗ +4.2%",        GreenAccent),
        AdminStat("ACTIVE FACULTY",    faculty,          "✓ Stable",        GreenAccent),
        AdminStat("PENDING APPROVALS", pendingApprovals, "! High Priority", RedAccent),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { stat ->
            Card(
                colors   = CardDefaults.cardColors(containerColor = StatCardBg),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(stat.label, color = SubtextColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(stat.value, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(stat.sub, color = stat.subColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Admin Menu Overlay ────────────────────────────────────────────────────────
@Composable
fun AdminMenuOverlay(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    val menuItems = listOf(
        AdminMenuItem("Attendance Admin",   "Manage student logs & verification",  Icons.Default.Fingerprint,          Screen.AdminAttendance.route, isActive = true),
        AdminMenuItem("Timetable Admin",    "Optimize lecture halls & schedules",  Icons.Default.CalendarMonth,        Screen.AdminTimetable.route,  isActive = true),
        AdminMenuItem("Leave Approvals",    "Faculty & Staff requests",            Icons.Default.EventAvailable,       Screen.Dashboard.route,       isSoon   = true),
        AdminMenuItem("Payroll Config",     "Institutional disbursement tools",    Icons.Default.AccountBalanceWallet, Screen.Dashboard.route,       isSoon   = true),
        AdminMenuItem("Fee Defaulters",     "Automated billing reminders",         Icons.Default.Build,                Screen.Dashboard.route,       isSoon   = true),
        AdminMenuItem("Broadcast Messages", "Mass notifications & alerts",         Icons.Default.Campaign,             Screen.Dashboard.route,       isSoon   = true),
    )
    MenuOverlayScaffold("Admin Menu", "Management Modules", menuItems, onNavigate, onDismiss)
}

// ── Faculty Menu Overlay ──────────────────────────────────────────────────────
@Composable
fun FacultyMenuOverlay(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    val menuItems = listOf(
        AdminMenuItem("Mark Attendance", "Record student attendance for your courses", Icons.Default.Fingerprint,   Screen.AdminAttendance.route, isActive = true),
        AdminMenuItem("Timetable",       "View your lecture schedule",                 Icons.Default.CalendarMonth, Screen.Timetable.route,       isActive = true),
        AdminMenuItem("Courses",         "Browse assigned courses",                    Icons.Default.MenuBook,      Screen.CourseFilter.route,    isActive = true),
    )
    MenuOverlayScaffold("Faculty Menu", "Teaching Modules", menuItems, onNavigate, onDismiss)
}

// ── Student Menu Overlay ──────────────────────────────────────────────────────
@Composable
fun StudentMenuOverlay(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    val menuItems = listOf(
        AdminMenuItem("My Attendance",      "View your attendance records",        Icons.Default.BarChart,      Screen.Attendance.route,        isActive = true),
        AdminMenuItem("Timetable",          "Your personal class schedule",        Icons.Default.CalendarMonth, Screen.Timetable.route,         isActive = true),
        AdminMenuItem("Courses",            "Browse enrolled courses",             Icons.Default.MenuBook,      Screen.CourseFilter.route,      isActive = true),
        AdminMenuItem("Monthly Attendance", "Month-by-month attendance breakdown", Icons.Default.DateRange,     Screen.MonthlyAttendance.route, isActive = true),
    )
    MenuOverlayScaffold("Student Menu", "Academic Modules", menuItems, onNavigate, onDismiss)
}

// ── Shared Menu Overlay Scaffold ──────────────────────────────────────────────
@Composable
private fun MenuOverlayScaffold(
    title: String,
    subtitle: String,
    menuItems: List<AdminMenuItem>,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .align(Alignment.CenterStart)
                    .background(Background)
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Text(title,    color = SubtextColor, fontSize = 12.sp,  fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = OnBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(menuItems.size) { idx ->
                        val item = menuItems[idx]
                        MenuItemCard(item = item, onClick = { onNavigate(item.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(item: AdminMenuItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (item.isActive) MenuItemActiveBg else MenuItemSoonBg)
            .clickable(enabled = !item.isSoon, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (item.isActive) OnBanner.copy(alpha = 0.18f) else OnBackground.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = if (item.isActive) OnBanner else SubtextColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.label,    color = if (item.isActive) OnBanner else OnBackground.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(3.dp))
                Text(item.subtitle, color = if (item.isActive) OnBanner.copy(alpha = 0.75f) else SubtextColor, fontSize = 12.sp)
            }
            if (item.isActive) {
                Icon(Icons.Default.ChevronRight, null, tint = OnBanner, modifier = Modifier.size(20.dp))
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Background)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("COMING SOON", color = SubtextColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}