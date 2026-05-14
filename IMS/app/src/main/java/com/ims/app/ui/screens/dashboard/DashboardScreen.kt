package com.ims.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.ims.app.ui.SearchResult
import com.ims.app.ui.SearchResultType
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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

private data class AdminMenuItem(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val isSoon: Boolean = false,
    val isActive: Boolean = false
)

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
    val isStudent = !isAdmin

    var showMenu           by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var searchQuery        by remember { mutableStateOf("") }
    var searchActive       by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DashboardTopBar(
                onMenuClick  = { showMenu = true },
                onLogout     = onLogout,
                showGlobe    = isStudent,
                onGlobeClick = { showLanguageDialog = true }
            )
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

            item {
                DashboardSearchBar(
                    query        = searchQuery,
                    active       = searchActive,
                    onQueryChange = { searchQuery = it; searchActive = it.isNotBlank() },
                    onClear      = { searchQuery = ""; searchActive = false },
                    results      = if (searchActive)
                        viewModel.buildSearchIndex().filter {
                            it.label.contains(searchQuery, ignoreCase = true) ||
                                    it.subtitle.contains(searchQuery, ignoreCase = true)
                        }
                    else emptyList(),
                    onResultClick = { result ->
                        searchQuery  = ""
                        searchActive = false
                        when (result.route) {
                            "language_prefs" -> showLanguageDialog = true
                            "logout"         -> onLogout()
                            else             -> onNavigate(result.route)
                        }
                    }
                )
            }

            item {
                if (isAdmin) {
                    AdminProfileBanner(name = name, role = role, email = email)
                } else {
                    ProfileBanner(name = name, role = role, email = email)
                }
            }

            if (isStudent) {
                item {
                    val perCoursePercents = viewModel.allCourses
                        .map { viewModel.getAttendancePercent(it.courseId) }
                    val overallAttendance = if (perCoursePercents.isEmpty()) 0f
                    else perCoursePercents.average().toFloat()

                    val alertCount = viewModel.allCourses
                        .count { viewModel.getAttendancePercent(it.courseId) < 75f }

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

            if (isAdmin) {
                item {
                    AdminStatCards(
                        students         = "${stats["totalStudents"]    ?: 0}",
                        pendingApprovals = "${stats["pendingApprovals"] ?: 0}"
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showMenu) {
        when {
            isAdmin   -> AdminMenuOverlay(
                onNavigate = { route -> showMenu = false; onNavigate(route) },
                onDismiss  = { showMenu = false }
            )
            else      -> StudentMenuOverlay(
                onNavigate = { route -> showMenu = false; onNavigate(route) },
                onDismiss  = { showMenu = false }
            )
        }
    }

    if (showLanguageDialog) {
        LanguagePreferencesDialog(onDismiss = { showLanguageDialog = false })
    }
}

private val SearchBg      = Color(0xFF0D2233)
private val SearchDivider = Color(0xFF1E3A4A)

private val SearchResultType.typeLabel: String get() = when (this) {
    SearchResultType.PAGE    -> "Page"
    SearchResultType.COURSE  -> "Course"
    SearchResultType.STUDENT -> "Student"
    SearchResultType.SLOT    -> "Slot"
    SearchResultType.SETTING -> "Setting"
}

private val SearchResultType.typeColor: Color get() = when (this) {
    SearchResultType.PAGE    -> Color(0xFF00BFA5)
    SearchResultType.COURSE  -> Color(0xFF5C6BC0)
    SearchResultType.STUDENT -> Color(0xFF26A69A)
    SearchResultType.SLOT    -> Color(0xFFFF8F00)
    SearchResultType.SETTING -> Color(0xFF607D8B)
}

@Composable
private fun DashboardSearchBar(
    query:         String,
    active:        Boolean,
    onQueryChange: (String) -> Unit,
    onClear:       () -> Unit,
    results:       List<SearchResult>,
    onResultClick: (SearchResult) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (active && results.isNotEmpty()) 14.dp else 14.dp))
                .background(SearchBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint     = if (active) MenuItemActiveBg else SubtextColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value         = query,
                onValueChange = onQueryChange,
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                textStyle     = androidx.compose.ui.text.TextStyle(
                    color    = OnBackground,
                    fontSize = 14.sp
                ),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search pages, courses, timetable…",
                            color    = SubtextColor,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
            if (active) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(SubtextColor.copy(alpha = 0.25f))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint     = SubtextColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        if (active && results.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SearchBg)
            ) {
                results.forEachIndexed { idx, result ->
                    if (idx > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SearchDivider)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(result) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(result.type.typeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                result.icon,
                                contentDescription = null,
                                tint     = result.type.typeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                result.label,
                                color      = OnBackground,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 13.sp
                            )
                            Text(
                                result.subtitle,
                                color    = SubtextColor,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(result.type.typeColor.copy(alpha = 0.13f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                result.type.typeLabel.uppercase(),
                                color         = result.type.typeColor,
                                fontSize      = 8.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                if (results.isEmpty()) {
                    Text(
                        "No results for \"$query\"",
                        color    = SubtextColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    )
                }
            }
        }

        if (active && results.isEmpty() && query.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SearchBg)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No results for \"$query\"",
                    color    = SubtextColor,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onMenuClick:  () -> Unit,
    onLogout:     () -> Unit,
    showGlobe:    Boolean = false,
    onGlobeClick: () -> Unit = {}
) {
    TopAppBar(
        title = { Text("Dashboard", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu", tint = OnBackground) }
        },
        actions = {
            if (showGlobe) {
                IconButton(onClick = onGlobeClick) {
                    Icon(Icons.Default.Language, "Language Preferences", tint = Primary)
                }
            }
            IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, "Logout", tint = Primary) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

@Composable
private fun AdminProfileBanner(name: String, role: String, email: String) {
    val lastActive = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(BannerGradientStart, BannerGradientEnd)))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier         = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BannerGradientEnd.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint     = OnBanner.copy(alpha = 0.9f),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(BannerGradientEnd)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(GreenAccent)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                name,
                color      = OnBanner,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp
            )

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OnBanner.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    role.uppercase(),
                    color         = OnBanner,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                email,
                color    = OnBanner.copy(alpha = 0.85f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    null,
                    tint     = OnBanner.copy(alpha = 0.65f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Last active: Today, $lastActive",
                    color    = OnBanner.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint     = OnBanner.copy(alpha = 0.65f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "IIIT Hyderabad",
                    color    = OnBanner.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

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

private data class AdminStat(
    val label:    String,
    val value:    String,
    val sub:      String,
    val subColor: Color,
    val subIcon:  ImageVector
)

@Composable
private fun AdminStatCards(students: String, pendingApprovals: String) {
    val items = listOf(
        AdminStat("TOTAL STUDENTS",    students,         "+4.2%",         GreenAccent, Icons.Default.TrendingUp),
        AdminStat("PENDING APPROVALS", pendingApprovals, "High Priority", RedAccent,   Icons.Default.Warning),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { stat ->
            Card(
                colors   = CardDefaults.cardColors(containerColor = StatCardBg),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            stat.subIcon,
                            null,
                            tint     = stat.subColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stat.sub,
                            color      = stat.subColor,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private data class LanguageOption(val code: String, val label: String, val region: String)

@Composable
fun LanguagePreferencesDialog(onDismiss: () -> Unit) {
    val languages = listOf(
        LanguageOption("en", "English",    "Global"),
        LanguageOption("hi", "Hindi",      "India"),
        LanguageOption("mr", "Marathi",    "Maharashtra"),
        LanguageOption("gu", "Gujarati",   "Gujarat"),
        LanguageOption("ta", "Tamil",      "Tamil Nadu"),
        LanguageOption("te", "Telugu",     "Andhra Pradesh"),
    )
    var selected by remember { mutableStateOf("en") }

    val timeZones = listOf("Asia/Kolkata (IST)", "UTC", "America/New_York (EST)")
    var selectedTz by remember { mutableStateOf(timeZones[0]) }

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
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f)
                    .align(Alignment.CenterStart)
                    .background(Background)
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Text(
                    "PREFERENCES",
                    color         = SubtextColor,
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language,
                        null,
                        tint     = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Language & Region",
                        color      = OnBackground,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(24.dp))

                Text(
                    "DISPLAY LANGUAGE",
                    color         = SubtextColor,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier            = Modifier.weight(1f)
                ) {
                    items(languages.size) { idx ->
                        val lang = languages[idx]
                        val isSelected = selected == lang.code
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MenuItemActiveBg else MenuItemSoonBg)
                                .clickable { selected = lang.code }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (isSelected) OnBanner.copy(alpha = 0.18f)
                                            else OnBackground.copy(alpha = 0.07f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        lang.code.uppercase(),
                                        color      = if (isSelected) OnBanner else SubtextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        lang.label,
                                        color      = if (isSelected) OnBanner else OnBackground.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 15.sp
                                    )
                                    Text(
                                        lang.region,
                                        color    = if (isSelected) OnBanner.copy(alpha = 0.75f) else SubtextColor,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint     = OnBanner,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "TIME ZONE",
                            color         = SubtextColor,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        timeZones.forEach { tz ->
                            val isTzSelected = selectedTz == tz
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTzSelected) MenuItemActiveBg else MenuItemSoonBg)
                                    .clickable { selectedTz = tz }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        null,
                                        tint     = if (isTzSelected) OnBanner else SubtextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        tz,
                                        color      = if (isTzSelected) OnBanner else OnBackground.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium,
                                        fontSize   = 14.sp,
                                        modifier   = Modifier.weight(1f)
                                    )
                                    if (isTzSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint     = OnBanner,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MenuItemActiveBg)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save Preferences",
                        color      = OnBanner,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdminMenuOverlay(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    val menuItems = listOf(
        AdminMenuItem("Attendance Admin",   "Manage student logs & verification",  Icons.Default.Fingerprint,          Screen.AdminAttendance.route, isActive = true),
        AdminMenuItem("Timetable Admin",    "Optimize lecture halls & schedules",  Icons.Default.CalendarMonth,        Screen.AdminTimetable.route,  isActive = true),
        AdminMenuItem("Leave Approvals",    "Staff requests",                              Icons.Default.EventAvailable,       Screen.Dashboard.route,       isSoon   = true),
        AdminMenuItem("Payroll Config",     "Institutional disbursement tools",    Icons.Default.AccountBalanceWallet, Screen.Dashboard.route,       isSoon   = true),
        AdminMenuItem("Fee Defaulters",     "Automated billing reminders",         Icons.Default.Build,                Screen.Dashboard.route,       isSoon   = true),
        AdminMenuItem("Broadcast Messages", "Mass notifications & alerts",         Icons.Default.Campaign,             Screen.Dashboard.route,       isSoon   = true),
    )
    MenuOverlayScaffold("Admin Menu", "Management Modules", menuItems, onNavigate, onDismiss)
}

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
