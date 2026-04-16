package com.ims.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.ui.theme.*

// ── HeaderTopBar ─────────────────────────────────────────────────────────────
/** Matches "HeaderTopBar" screen in Figma */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderTopBar(
    title: String,
    userName: String = "",
    userRole: String = "",
    onMenuClick: () -> Unit = {},
    onNotifClick: () -> Unit = {},
    onNavigate: ((String) -> Unit)? = null   // convenience overload used by screens
) {
    TopAppBar(
        title = {
            Column {
                Text(title, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (userName.isNotEmpty())
                    Text(userName, color = Primary, fontSize = 12.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Primary)
            }
        },
        actions = {
            IconButton(onClick = onNotifClick) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Primary)
            }
            if (userRole.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(userRole, color = OnBackground, fontSize = 11.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
    )
}

// ── BottomNavBar ─────────────────────────────────────────────────────────────
/** Matches "BottomNavBar" screen in Figma */
data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun BottomNavBar(
    currentRoute: String,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit
) {
    val items = if (isAdmin) listOf(
        BottomNavItem("Home",       Icons.Default.Home,            "dashboard"),
        BottomNavItem("Attendance", Icons.Default.CheckCircle,     "admin_attendance"),
        BottomNavItem("Timetable",  Icons.Default.CalendarMonth,   "admin_timetable"),
        BottomNavItem("Courses",    Icons.Default.Book,            "course_filter"),
    ) else listOf(
        BottomNavItem("Home",       Icons.Default.Home,            "dashboard"),
        BottomNavItem("Attendance", Icons.Default.CheckCircle,     "attendance"),
        BottomNavItem("Timetable",  Icons.Default.CalendarMonth,   "timetable"),
        BottomNavItem("Courses",    Icons.Default.Book,            "course_filter"),
    )

    NavigationBar(containerColor = Surface) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick  = { onNavigate(item.route) },
                icon     = { Icon(item.icon, contentDescription = item.label) },
                label    = { Text(item.label, fontSize = 10.sp) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Primary,
                    unselectedIconColor = OnSurface.copy(alpha = 0.5f),
                    indicatorColor      = SurfaceVar
                )
            )
        }
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Primary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(label, color = OnSurface, fontSize = 12.sp)
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        color      = OnBackground,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        modifier   = Modifier.padding(vertical = 8.dp)
    )
}

// ── Avatar Circle ─────────────────────────────────────────────────────────────
@Composable
fun AvatarCircle(initials: String, size: Int = 40) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(PrimaryDark),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}

// ── Attendance percent badge ───────────────────────────────────────────────────
@Composable
fun AttendanceBadge(percent: Float) {
    val color = when {
        percent >= 75f -> Primary
        percent >= 50f -> Color(0xFFFFA726)
        else           -> Color(0xFFEF5350)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("${percent.toInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
