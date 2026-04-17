package com.ims.app.ui.screens.dashboard

import androidx.compose.animation.*
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

// ── Local colours ─────────────────────────────────────────────────────────────
private val BannerGradientStart = Color(0xFF00A896)
private val BannerGradientEnd   = Color(0xFF006B62)
private val StatCardBg          = Color(0xFF0D2233)
private val MenuItemActiveBg    = Color(0xFF00A896)
private val MenuItemSoonBg      = Color(0xFF0D2233)
private val SoonLabel           = Color(0xFF607D8B)
private val SubtextColor        = Color(0xFF607D8B)
private val OnBanner            = Color.White
private val GreenAccent         = Color(0xFF00E5B0)
private val RedAccent           = Color(0xFFFF5252)
private val GlobeIcon           = Color(0xFF455A64)

// ── Data models ───────────────────────────────────────────────────────────────
private data class AdminStat(
    val label: String,
    val value: String,
    val sub: String,
    val subColor: Color
)

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
    val name  = viewModel.getCurrentUserName()
    val role  = viewModel.getCurrentUserRole()
    val email = viewModel.getCurrentUserEmail()
    val stats = viewModel.dashboardStats

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AdminTopBar(
                onMenuClick  = { showMenu = true },
                onLogout     = onLogout
            )
        },
        bottomBar = {
            // Globe / language icon row at bottom (as in Figma)
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = "Language",
                    tint     = GlobeIcon,
                    modifier = Modifier.size(26.dp)
                )
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
            item { Spacer(Modifier.height(8.dp)) }

            // Profile banner
            item {
                AdminProfileBanner(
                    name  = name,
                    role  = role,
                    email = email
                )
            }

            // Stat cards
            item {
                AdminStatCards(
                    students         = "${stats["totalStudents"] ?: 12482}",
                    faculty          = "${stats["totalFaculty"]  ?: 842}",
                    pendingApprovals = "${stats["pendingApprovals"] ?: 28}"
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // Slide-in Admin Menu overlay
    if (showMenu) {
        AdminMenuOverlay(
            onNavigate = { route ->
                showMenu = false
                onNavigate(route)
            },
            onDismiss  = { showMenu = false }
        )
    }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopBar(
    onMenuClick: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Dashboard",
                color      = OnBackground,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = OnBackground)
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

// ── Profile Banner ────────────────────────────────────────────────────────────
@Composable
private fun AdminProfileBanner(name: String, role: String, email: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(BannerGradientStart, BannerGradientEnd)
                )
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

            // Avatar with online dot
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier         = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BannerGradientEnd),
                    contentAlignment = Alignment.Center
                ) {
                    // Replace with actual avatar image if available
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint     = OnBanner.copy(alpha = 0.8f),
                        modifier = Modifier.size(44.dp)
                    )
                }
                // Green online indicator
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(GreenAccent)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(name, color = OnBanner, fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Spacer(Modifier.height(8.dp))

            // Role chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OnBanner.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    role.uppercase(),
                    color      = OnBanner,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(email, color = OnBanner.copy(alpha = 0.85f), fontSize = 13.sp)

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = OnBanner.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Last active: 2 mins ago", color = OnBanner.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = OnBanner.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("IIIT Hyderabad", color = OnBanner.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

// ── Admin Stat Cards ──────────────────────────────────────────────────────────
@Composable
private fun AdminStatCards(students: String, faculty: String, pendingApprovals: String) {
    val items = listOf(
        AdminStat("TOTAL STUDENTS",    students,         "↗ +4.2%",       GreenAccent),
        AdminStat("ACTIVE FACULTY",    faculty,          "✓ Stable",       GreenAccent),
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
                    Text(
                        stat.label,
                        color         = SubtextColor,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stat.value,
                        color      = OnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 28.sp
                    )
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
        AdminMenuItem(
            label     = "Attendance Admin",
            subtitle  = "Manage student logs & verification",
            icon      = Icons.Default.Fingerprint,
            route     = Screen.AdminAttendance.route,
            isActive  = true
        ),
        AdminMenuItem(
            label    = "Timetable Admin",
            subtitle = "Optimize lecture halls & schedules",
            icon     = Icons.Default.CalendarMonth,
            route    = Screen.AdminTimetable.route,
            isActive = true
        ),
        AdminMenuItem(
            label    = "Leave Approvals",
            subtitle = "Faculty & Staff requests",
            icon     = Icons.Default.EventAvailable,
            route    = Screen.Dashboard.route,
            isSoon   = true
        ),
        AdminMenuItem(
            label    = "Payroll Config",
            subtitle = "Institutional disbursement tools",
            icon     = Icons.Default.AccountBalanceWallet,
            route    = Screen.Dashboard.route,
            isSoon   = true
        ),
        AdminMenuItem(
            label    = "Fee Defaulters",
            subtitle = "Automated billing reminders",
            icon     = Icons.Default.Build,
            route    = Screen.Dashboard.route,
            isSoon   = true
        ),
        AdminMenuItem(
            label    = "Broadcast Messages",
            subtitle = "Mass notifications & alerts",
            icon     = Icons.Default.Campaign,
            route    = Screen.Dashboard.route,
            isSoon   = true
        ),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
        ) {
            // Menu panel
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .align(Alignment.CenterStart)
                    .background(Background)
                    .clickable(enabled = false) {}  // consume clicks so panel doesn't dismiss
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                // Header
                Text(
                    "Admin Menu",
                    color      = SubtextColor,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Management Modules",
                    color      = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp
                )

                Spacer(Modifier.height(24.dp))

                // Menu items
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(menuItems.size) { idx ->
                        val item = menuItems[idx]
                        AdminMenuItemCard(item = item, onClick = { onNavigate(item.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMenuItemCard(item: AdminMenuItem, onClick: () -> Unit) {
    val bgColor = when {
        item.isActive -> MenuItemActiveBg
        else          -> MenuItemSoonBg
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(enabled = !item.isSoon, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon box
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (item.isActive) OnBanner.copy(alpha = 0.18f)
                        else OnBackground.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon, null,
                    tint     = if (item.isActive) OnBanner else SubtextColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.label,
                    color      = if (item.isActive) OnBanner else OnBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.subtitle,
                    color    = if (item.isActive) OnBanner.copy(alpha = 0.75f) else SubtextColor,
                    fontSize = 12.sp
                )
            }

            if (item.isActive) {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint     = OnBanner,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // "COMING SOON" label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Background)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "COMING SOON",
                        color         = SubtextColor,
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}