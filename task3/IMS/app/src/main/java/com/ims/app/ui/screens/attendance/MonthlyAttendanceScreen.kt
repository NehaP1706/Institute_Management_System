package com.ims.app.ui.screens.attendance

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import com.ims.app.util.exportMonthlyAttendancePdf
import java.text.SimpleDateFormat
import java.util.*

/**
 * Monthly Attendance Screen.
 *
 * PDF icon → exports a monthly report for ALL courses for the month
 * currently shown on screen (calYear / calMonth).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAttendanceScreen(
    viewModel:       IMSViewModel,
    currentRoute:    String,
    onNavigate:      (String) -> Unit,
    onSwitchToDaily: (() -> Unit)? = null
) {
    val context = LocalContext.current

    //  Filter state
    var selectedCourse       by remember { mutableStateOf(viewModel.allCourses.firstOrNull()) }
    var showCourseSheet      by remember { mutableStateOf(false) }
    var pendingCourse        by remember { mutableStateOf(selectedCourse) }
    var sheetSearch          by remember { mutableStateOf("") }
    var viewModeMenuExpanded by remember { mutableStateOf(false) }
    var isExporting          by remember { mutableStateOf(false) }

    // Calendar navigation
    val today    = remember { Calendar.getInstance() }
    var calYear  by remember { mutableStateOf(today.get(Calendar.YEAR))  }
    var calMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }

    // Derived attendance data

    // All records for the viewed month across ALL courses (used for PDF export)
    val allMonthRecords by remember(calYear, calMonth) {
        derivedStateOf {
            viewModel.getAttendanceRecords().filter { r ->
                val cal = Calendar.getInstance().apply { time = r.date }
                cal.get(Calendar.YEAR)  == calYear &&
                        cal.get(Calendar.MONTH) == calMonth
            }
        }
    }

    // Records for the selected course only (hero card + calendar)
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

    // Grid with prev/next month bleed-in
    val gridDays: List<Pair<Int, Boolean>> = remember(calYear, calMonth) {
        val cal         = Calendar.getInstance().apply { set(calYear, calMonth, 1) }
        val firstDow    = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMonthCal = Calendar.getInstance().apply {
            set(calYear, calMonth, 1); add(Calendar.MONTH, -1)
        }
        val prevDaysInMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        buildList {
            for (i in firstDow - 1 downTo 0) add(Pair(prevDaysInMonth - i, false))
            for (d in 1..daysInMonth)         add(Pair(d, true))
            var nextDay = 1
            while (size % 7 != 0)             add(Pair(nextDay++, false))
        }
    }

    val todayDay    = today.get(Calendar.DAY_OF_MONTH)
    val isThisMonth = calYear  == today.get(Calendar.YEAR) &&
            calMonth == today.get(Calendar.MONTH)

    // Course bottom sheet
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

    // Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onNavigate("back") }) {
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
                    // PDF export button
                    IconButton(
                        onClick = {
                            if (isExporting) return@IconButton
                            if (allMonthRecords.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "No attendance records for ${
                                        monthLabel.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    }.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@IconButton
                            }
                            isExporting = true
                            try {
                                exportMonthlyAttendancePdf(
                                    context    = context,
                                    records    = allMonthRecords,
                                    allCourses = viewModel.allCourses,
                                    year       = calYear,
                                    month      = calMonth
                                )
                            } finally {
                                isExporting = false
                            }
                        }
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = OnBackground)
                        }
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

            //  Filter row
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    FilledFilterChipButton(
                        label    = selectedCourse?.title ?: "Course",
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            pendingCourse   = selectedCourse
                            sheetSearch     = ""
                            showCourseSheet = true
                        }
                    )
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
                                onClick = { viewModeMenuExpanded = false; onSwitchToDaily?.invoke() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Monthly", color = Primary, fontWeight = FontWeight.Bold)
                                },
                                onClick     = { viewModeMenuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            //  Month navigator
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (calMonth == 0) { calMonth = 11; calYear-- } else calMonth--
                    }) { Icon(Icons.Default.ChevronLeft, "Previous month", tint = OnBackground) }
                    Text(
                        monthLabel,
                        color         = OnBackground,
                        fontWeight    = FontWeight.SemiBold,
                        fontSize      = 14.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = {
                        if (calMonth == 11) { calMonth = 0; calYear++ } else calMonth++
                    }) { Icon(Icons.Default.ChevronRight, "Next month", tint = OnBackground) }
                }
            }

            // Hero summary card
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

            //  Calendar grid
            item {
                CalendarGrid(
                    gridDays     = gridDays,
                    dayStatusMap = dayStatusMap,
                    todayDay     = if (isThisMonth) todayDay else -1
                )
            }

            //Legend
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


// Filled filter chip (blue bg)
@Composable
private fun FilledFilterChipButton(
    label:    String,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A3A5C))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(label, color = OnBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = OnBackground, modifier = Modifier.size(16.dp))
        }
    }
}

// Hero card
@Composable
private fun MonthlyHeroCard(
    courseTitle:  String,
    percent:      Float,
    presentCount: Int,
    absentCount:  Int
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
                Text(courseTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
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
                    Text("${percent.toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Text("AVG", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor))
            Text("$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

// Calendar grid
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
            Row(Modifier.fillMaxWidth()) {
                dayHeaders.forEach { h ->
                    Text(h, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        color = OnSurface.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            gridDays.chunked(7).forEach { week ->
                Row(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)) {
                    week.forEach { (day, isCurrentMonth) ->
                        Box(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f), contentAlignment = Alignment.Center) {
                            CalendarDayCell(
                                day            = day,
                                status         = if (isCurrentMonth) dayStatusMap[day] else null,
                                isToday        = isCurrentMonth && (day == todayDay),
                                isCurrentMonth = isCurrentMonth
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
    val badgeColor: Color? = when (status) {
        AttendanceStatus.PRESENT        -> Primary
        AttendanceStatus.ABSENT         -> Color(0xFFEF5350)
        AttendanceStatus.APPROVED_LEAVE -> Color(0xFFFFA726)
        else                            -> null
    }
    val badgeLetter: String? = when (status) {
        AttendanceStatus.PRESENT        -> "P"
        AttendanceStatus.ABSENT         -> "A"
        AttendanceStatus.APPROVED_LEAVE -> "L"
        else                            -> null
    }
    // Today ALWAYS solid green – overrides any status colour
    val cellBg: Color = when {
        isToday            -> Primary
        badgeColor != null -> badgeColor.copy(alpha = 0.20f)
        else               -> Color.Transparent
    }
    val numberColor: Color = when {
        isToday            -> Color.White
        badgeColor != null -> badgeColor
        isCurrentMonth     -> OnSurface.copy(alpha = 0.55f)
        else               -> OnSurface.copy(alpha = 0.22f)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(cellBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (badgeLetter != null && (isToday || badgeColor != null)) {
                Text(badgeLetter, color = if (isToday) Color.White else badgeColor!!, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
            }
            Text("$day", color = numberColor, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun LegendSquare(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color))
        Text(label, color = OnSurface, fontSize = 11.sp, letterSpacing = 0.5.sp)
    }
}