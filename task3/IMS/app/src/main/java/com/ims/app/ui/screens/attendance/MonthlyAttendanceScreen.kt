package com.ims.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Monthly Attendance Screen – updated to match Image 2.
 *
 * Changes vs Image 1:
 *  - Filter chips: filled blue background, full course name visible
 *  - Calendar day cells: rounded-square letter badge (P / A / L) with coloured bg
 *  - Today's cell: solid green background + white text (no border-only ring)
 *  - Calendar bleeds prev/next month days (greyed out) to fill the grid
 *  - Legend: rounded-square indicators instead of circles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSwitchToDaily: (() -> Unit)? = null
) {
    // ── Filter state ──────────────────────────────────────────────────────────
    var selectedCourse       by remember { mutableStateOf(viewModel.allCourses.firstOrNull()) }
    var showCourseSheet      by remember { mutableStateOf(false) }
    var pendingCourse        by remember { mutableStateOf(selectedCourse) }
    var sheetSearch          by remember { mutableStateOf("") }
    var viewModeMenuExpanded by remember { mutableStateOf(false) }

    // ── Calendar navigation ───────────────────────────────────────────────────
    val today    = remember { Calendar.getInstance() }
    var calYear  by remember { mutableStateOf(today.get(Calendar.YEAR))  }
    var calMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) } // 0-based

    // ── Derived attendance data ───────────────────────────────────────────────
    val allRecords by remember(selectedCourse) {
        derivedStateOf {
            selectedCourse
                ?.let { viewModel.getAttendanceForCourse(it.courseId) }
                ?: viewModel.getAttendanceRecords()
        }
    }

    val monthRecords by remember(allRecords, calYear, calMonth) {
        derivedStateOf {
            allRecords.filter { r ->
                val cal = Calendar.getInstance().apply { time = r.date }
                cal.get(Calendar.YEAR)  == calYear &&
                        cal.get(Calendar.MONTH) == calMonth
            }
        }
    }

    val presentCount = monthRecords.count { it.status == AttendanceStatus.PRESENT }
    val absentCount  = monthRecords.count { it.status == AttendanceStatus.ABSENT  }
    val totalMarked  = monthRecords.count { it.status != AttendanceStatus.UNMARKED }
    val percent      = if (totalMarked > 0) presentCount * 100f / totalMarked else 0f

    // day-of-month → status map
    val dayStatusMap: Map<Int, AttendanceStatus> by remember(monthRecords) {
        derivedStateOf {
            buildMap {
                monthRecords.forEach { r ->
                    val cal = Calendar.getInstance().apply { time = r.date }
                    put(cal.get(Calendar.DAY_OF_MONTH), r.status)
                }
            }
        }
    }

    val monthLabel = remember(calYear, calMonth) {
        val cal = Calendar.getInstance().apply { set(calYear, calMonth, 1) }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time).uppercase()
    }

    // ── Grid: include trailing/leading days from adjacent months ─────────────
    // Each cell is (day, isCurrentMonth)
    val gridDays: List<Pair<Int, Boolean>> = remember(calYear, calMonth) {
        val cal         = Calendar.getInstance().apply { set(calYear, calMonth, 1) }
        val firstDow    = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0 = Sunday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Days from previous month
        val prevMonthCal = Calendar.getInstance().apply {
            set(calYear, calMonth, 1)
            add(Calendar.MONTH, -1)
        }
        val prevDaysInMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        buildList {
            // Leading days from previous month
            for (i in firstDow - 1 downTo 0) {
                add(Pair(prevDaysInMonth - i, false))
            }
            // Current month
            for (d in 1..daysInMonth) {
                add(Pair(d, true))
            }
            // Trailing days from next month
            var nextDay = 1
            while (size % 7 != 0) {
                add(Pair(nextDay++, false))
            }
        }
    }

    val todayDay    = today.get(Calendar.DAY_OF_MONTH)
    val isThisMonth = calYear  == today.get(Calendar.YEAR) &&
            calMonth == today.get(Calendar.MONTH)

    // ── Course bottom sheet ───────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showCourseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCourseSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF111E2B),
            dragHandle       = null
        ) {
            CoursePickerSheetContent(
                courses        = viewModel.allCourses.filter {
                    sheetSearch.isBlank() ||
                            it.title.contains(sheetSearch, ignoreCase = true) ||
                            it.courseCode.contains(sheetSearch, ignoreCase = true)
                },
                searchQuery    = sheetSearch,
                onSearchChange = { sheetSearch = it },
                pendingCourse  = pendingCourse,
                onSelect       = { pendingCourse = it },
                onBack         = { showCourseSheet = false },
                onClose        = { showCourseSheet = false; sheetSearch = "" },
                onApply        = {
                    selectedCourse  = pendingCourse
                    showCourseSheet = false
                    sheetSearch     = ""
                }
            )
        }
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onNavigate("dashboard") }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = OnBackground)
                    }
                },
                title = {
                    Text(
                        "Attendance",
                        color      = OnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        modifier   = Modifier.fillMaxWidth(),
                        textAlign  = TextAlign.Center
                    )
                },
                actions = {
                    IconButton(onClick = { /* PDF export stub */ }) {
                        Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
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
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── 1. Filter row ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    // Course picker chip – filled blue, shows full course name
                    FilledFilterChipButton(
                        label    = selectedCourse?.title ?: "Course",
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            pendingCourse   = selectedCourse
                            sheetSearch     = ""
                            showCourseSheet = true
                        }
                    )

                    // Daily / Monthly toggle chip – filled blue
                    Box(modifier = Modifier.weight(0.6f)) {
                        FilledFilterChipButton(
                            label    = "Monthly",
                            modifier = Modifier.fillMaxWidth(),
                            onClick  = { viewModeMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded         = viewModeMenuExpanded,
                            onDismissRequest = { viewModeMenuExpanded = false },
                            modifier         = Modifier.background(SurfaceVar)
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Daily", color = OnBackground) },
                                onClick = {
                                    viewModeMenuExpanded = false
                                    onSwitchToDaily?.invoke()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Monthly",
                                        color      = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick     = { viewModeMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Check, null,
                                        tint     = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── 2. Month navigator: < APRIL 2026 > ───────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (calMonth == 0) { calMonth = 11; calYear-- } else calMonth--
                    }) {
                        Icon(Icons.Default.ChevronLeft, "Previous month", tint = OnBackground)
                    }
                    Text(
                        monthLabel,
                        color         = OnBackground,
                        fontWeight    = FontWeight.SemiBold,
                        fontSize      = 14.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = {
                        if (calMonth == 11) { calMonth = 0; calYear++ } else calMonth++
                    }) {
                        Icon(Icons.Default.ChevronRight, "Next month", tint = OnBackground)
                    }
                }
            }

            // ── 3. Hero summary card ──────────────────────────────────────────
            item {
                selectedCourse?.let { course ->
                    MonthlyHeroCard(
                        courseTitle  = course.title,
                        percent      = percent,
                        presentCount = presentCount,
                        absentCount  = absentCount
                    )
                }
            }

            // ── 4. Calendar grid ──────────────────────────────────────────────
            item {
                CalendarGrid(
                    gridDays     = gridDays,
                    dayStatusMap = dayStatusMap,
                    todayDay     = if (isThisMonth) todayDay else -1
                )
            }

            // ── 5. Legend – square indicators ─────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    LegendSquare(color = Primary,           label = "PRESENT")
                    Spacer(Modifier.width(20.dp))
                    LegendSquare(color = Color(0xFFEF5350), label = "ABSENT")
                    Spacer(Modifier.width(20.dp))
                    LegendSquare(color = Color(0xFFFFA726), label = "LEAVE")
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filled filter chip button (blue background, replaces outline-only chip)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FilledFilterChipButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Blue-tinted surface, matching Image 2 filter chip colour
    val chipBg = Color(0xFF1A3A5C)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                color      = OnBackground,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint     = OnBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MonthlyHeroCard(
    courseTitle: String,
    percent: Float,
    presentCount: Int,
    absentCount: Int
) {
    val ringColor = when {
        percent >= 75f -> Primary
        percent >= 50f -> Color(0xFFFFA726)
        else           -> Color(0xFFEF5350)
    }
    Card(
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0D7A6E)),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "CURRENT COURSE",
            color         = Color.White.copy(alpha = 0.6f),
            fontSize      = 10.sp,
            letterSpacing = 1.2.sp,
            fontWeight    = FontWeight.SemiBold,
            modifier      = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 2.dp)
        )
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    courseTitle,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroPill(count = presentCount, label = "PRESENT", dotColor = Primary)
                    HeroPill(count = absentCount,  label = "ABSENT",  dotColor = Color(0xFFEF5350))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                CircularProgressIndicator(
                    progress    = { percent / 100f },
                    modifier    = Modifier.fillMaxSize(),
                    color       = ringColor,
                    trackColor  = Color.White.copy(alpha = 0.25f),
                    strokeWidth = 5.dp,
                    strokeCap   = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${percent.toInt()}%",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        "AVG",
                        color    = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPill(count: Int, label: String, dotColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text("$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label,    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calendar grid
// Each cell is Pair(dayNumber, isCurrentMonth)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarGrid(
    gridDays:     List<Pair<Int, Boolean>>,
    dayStatusMap: Map<Int, AttendanceStatus>,
    todayDay:     Int
) {
    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")

    Card(
        colors   = CardDefaults.cardColors(containerColor = Surface),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {

            // Day-of-week header row
            Row(Modifier.fillMaxWidth()) {
                dayHeaders.forEach { h ->
                    Text(
                        h,
                        modifier      = Modifier.weight(1f),
                        textAlign     = TextAlign.Center,
                        color         = OnSurface.copy(alpha = 0.5f),
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Week rows
            gridDays.chunked(7).forEach { week ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    week.forEach { (day, isCurrentMonth) ->
                        Box(
                            modifier         = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CalendarDayCell(
                                day              = day,
                                status           = if (isCurrentMonth) dayStatusMap[day] else null,
                                isToday          = isCurrentMonth && (day == todayDay),
                                isCurrentMonth   = isCurrentMonth
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day:            Int,
    status:         AttendanceStatus?,
    isToday:        Boolean,
    isCurrentMonth: Boolean
) {
    // Colours for the filled badge
    val badgeColor: Color? = when (status) {
        AttendanceStatus.PRESENT        -> Primary               // teal/green
        AttendanceStatus.ABSENT         -> Color(0xFFEF5350)     // red
        AttendanceStatus.APPROVED_LEAVE -> Color(0xFFFFA726)     // orange
        else                            -> null
    }

    // Letter shown inside the badge
    val badgeLetter: String? = when (status) {
        AttendanceStatus.PRESENT        -> "P"
        AttendanceStatus.ABSENT         -> "A"
        AttendanceStatus.APPROVED_LEAVE -> "L"
        else                            -> null
    }

    // Today ALWAYS gets solid Primary (green) bg, regardless of attendance status
    val cellBg: Color = when {
        isToday            -> Primary
        badgeColor != null -> badgeColor.copy(alpha = 0.20f)
        else               -> Color.Transparent
    }

    val numberColor: Color = when {
        isToday            -> Color.White
        badgeColor != null -> badgeColor
        isCurrentMonth     -> OnSurface.copy(alpha = 0.55f)
        else               -> OnSurface.copy(alpha = 0.22f)  // adjacent-month greyed out
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))   // rounded square, not circle
            .background(cellBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show letter badge on top for status days (similar to Image 2)
            if (badgeLetter != null && (isToday || badgeColor != null)) {
                Text(
                    badgeLetter,
                    color      = if (isToday) Color.White else badgeColor!!,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
            Text(
                "$day",
                color      = numberColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 11.sp,
                lineHeight = 13.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legend with rounded-square indicators (matching Image 2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LegendSquare(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))   // small rounded square
                .background(color)
        )
        Text(label, color = OnSurface, fontSize = 11.sp, letterSpacing = 0.5.sp)
    }
}