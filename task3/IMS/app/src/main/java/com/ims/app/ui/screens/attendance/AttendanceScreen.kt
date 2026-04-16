package com.ims.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.AttendanceRecord
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
private enum class ViewMode { DAILY, MONTHLY }

/**
 * Student read-only Attendance screen.
 *
 * Filters:
 *  1. Course dropdown  – selecting "Browse All Courses" navigates to CourseFilter screen.
 *                        Selecting a course entry filters the log to that course.
 *  2. Daily / Monthly  – toggles between this daily log view and MonthlyAttendanceScreen.
 *  3. From / To        – opens Material3 DatePickerDialog; filters log by date range.
 *                        An active-range chip appears below with a "Clear" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // ── Filter state ──────────────────────────────────────────────────────────
    var selectedCourse       by remember { mutableStateOf(viewModel.allCourses.firstOrNull()) }
    var courseMenuExpanded   by remember { mutableStateOf(false) }
    var viewMode             by remember { mutableStateOf(ViewMode.DAILY) }
    var viewModeMenuExpanded by remember { mutableStateOf(false) }
    var fromDateMillis       by remember { mutableStateOf<Long?>(null) }
    var toDateMillis         by remember { mutableStateOf<Long?>(null) }
    var showFromPicker       by remember { mutableStateOf(false) }
    var showToPicker         by remember { mutableStateOf(false) }

    // ── Monthly mode – hand off entirely ─────────────────────────────────────
    if (viewMode == ViewMode.MONTHLY) {
        MonthlyAttendanceScreen(
            viewModel       = viewModel,
            currentRoute    = currentRoute,
            onNavigate      = onNavigate,
            onSwitchToDaily = { viewMode = ViewMode.DAILY }
        )
        return
    }

    // ── Derived data ──────────────────────────────────────────────────────────
    val baseRecords by remember(selectedCourse) {
        derivedStateOf {
            selectedCourse?.let { viewModel.getAttendanceForCourse(it.courseId) }
                ?: viewModel.getAttendanceRecords()
        }
    }
    val displayRecords by remember(baseRecords, fromDateMillis, toDateMillis) {
        derivedStateOf {
            baseRecords.filter { r ->
                val ms        = r.date.time
                val afterFrom = fromDateMillis?.let { ms >= it } ?: true
                val beforeTo  = toDateMillis?.let  { ms <= it + 86_400_000L } ?: true
                afterFrom && beforeTo
            }
        }
    }

    val presentCount = displayRecords.count { it.status == AttendanceStatus.PRESENT }
    val absentCount  = displayRecords.count { it.status == AttendanceStatus.ABSENT  }
    val totalMarked  = displayRecords.count { it.status != AttendanceStatus.UNMARKED }
    val percent      = if (totalMarked > 0) presentCount * 100f / totalMarked else 0f

    // ── Date picker dialogs ───────────────────────────────────────────────────
    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = fromDateMillis ?: System.currentTimeMillis()
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = toDateMillis ?: System.currentTimeMillis()
    )

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromDateMillis = fromPickerState.selectedDateMillis
                    showFromPicker = false
                }) { Text("OK", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) {
                    Text("Cancel", color = OnSurface)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(
                state  = fromPickerState,
                colors = datePickerColors()
            )
        }
    }

    if (showToPicker) {
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    toDateMillis = toPickerState.selectedDateMillis
                    showToPicker = false
                }) { Text("OK", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) {
                    Text("Cancel", color = OnSurface)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(
                state  = toPickerState,
                colors = datePickerColors()
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
                    IconButton(onClick = { /* PDF stub */ }) {
                        Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, isAdmin = false, onNavigate = onNavigate)
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

            // ── Filter row ────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    // 1. Course dropdown ──────────────────────────────────────
                    Box(modifier = Modifier.weight(1f)) {
                        FilterChipButton(
                            label    = selectedCourse?.courseCode ?: "Course",
                            modifier = Modifier.fillMaxWidth(),
                            onClick  = { courseMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded         = courseMenuExpanded,
                            onDismissRequest = { courseMenuExpanded = false },
                            modifier         = Modifier.background(SurfaceVar)
                        ) {
                            // "Browse All Courses" → navigate to CourseFilterScreen
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint     = Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "Browse All Courses",
                                            color      = Primary,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                onClick = {
                                    courseMenuExpanded = false
                                    onNavigate(Screen.CourseFilter.route)
                                }
                            )
                            HorizontalDivider(color = Divider)
                            // Per-course entries
                            viewModel.allCourses.forEach { course ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                course.courseCode,
                                                color      = OnBackground,
                                                fontSize   = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                course.title,
                                                color    = OnSurface,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCourse     = course
                                        courseMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (course.courseId == selectedCourse?.courseId)
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint     = Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                    }
                                )
                            }
                        }
                    }

                    // 2. Daily / Monthly toggle ───────────────────────────────
                    Box(modifier = Modifier.weight(0.9f)) {
                        FilterChipButton(
                            label    = if (viewMode == ViewMode.DAILY) "Daily" else "Monthly",
                            modifier = Modifier.fillMaxWidth(),
                            onClick  = { viewModeMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded         = viewModeMenuExpanded,
                            onDismissRequest = { viewModeMenuExpanded = false },
                            modifier         = Modifier.background(SurfaceVar)
                        ) {
                            ViewMode.values().forEach { mode ->
                                val label = if (mode == ViewMode.DAILY) "Daily" else "Monthly"
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            color      = if (viewMode == mode) Primary else OnBackground,
                                            fontWeight = if (viewMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewMode             = mode
                                        viewModeMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (viewMode == mode)
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint     = Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                    }
                                )
                            }
                        }
                    }

                    // 3. From date ────────────────────────────────────────────
                    FilterDateButton(
                        label    = fromDateMillis?.let { shortDate(it) } ?: "From",
                        modifier = Modifier.weight(1f),
                        active   = fromDateMillis != null,
                        onClick  = { showFromPicker = true }
                    )

                    // 4. To date ──────────────────────────────────────────────
                    FilterDateButton(
                        label    = toDateMillis?.let { shortDate(it) } ?: "To",
                        modifier = Modifier.weight(0.8f),
                        active   = toDateMillis != null,
                        onClick  = { showToPicker = true }
                    )
                }

                // Active range indicator + clear button
                if (fromDateMillis != null || toDateMillis != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.FilterAlt, null, tint = Primary, modifier = Modifier.size(13.dp))
                        Text(
                            buildString {
                                fromDateMillis?.let { append("From ${shortDate(it)}  ") }
                                toDateMillis?.let   { append("→  ${shortDate(it)}")     }
                            },
                            color    = Primary,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVar)
                                .clickable { fromDateMillis = null; toDateMillis = null }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) { Text("Clear", color = OnSurface, fontSize = 11.sp) }
                    }
                }
            }

            // ── Hero summary card ─────────────────────────────────────────────
            item {
                selectedCourse?.let { course ->
                    AttendanceSummaryHeroCard(
                        courseTitle  = course.title,
                        percent      = percent,
                        presentCount = presentCount,
                        absentCount  = absentCount
                    )
                }
            }

            // ── Section header ────────────────────────────────────────────────
            item {
                Text(
                    "ATTENDANCE LOG",
                    color         = OnSurface.copy(alpha = 0.7f),
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier      = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // ── Log rows ──────────────────────────────────────────────────────
            if (displayRecords.isEmpty()) {
                item {
                    Text(
                        "No records match the selected filters.",
                        color    = OnSurface,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(displayRecords) { record ->
                    AttendanceLogRow(record)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Hero summary card ─────────────────────────────────────────────────────────
@Composable
private fun AttendanceSummaryHeroCard(
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
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(courseTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountPill(presentCount, "PRESENT")
                    CountPill(absentCount,  "ABSENT")
                }
            }
            Spacer(Modifier.width(12.dp))
            CircularPercentBadge(percent, ringColor)
        }
    }
}

@Composable
private fun CountPill(count: Int, label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$count", color = Color.White,                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label,    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CircularPercentBadge(percent: Float, ringColor: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        CircularProgressIndicator(
            progress    = { percent / 100f },
            modifier    = Modifier.fillMaxSize(),
            color       = ringColor,
            trackColor  = Color.White.copy(alpha = 0.25f),
            strokeWidth = 5.dp,
            strokeCap   = StrokeCap.Round
        )
        Text(
            "${percent.toInt()}%",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp,
            textAlign  = TextAlign.Center
        )
    }
}

// ── Log row ───────────────────────────────────────────────────────────────────
@Composable
private fun AttendanceLogRow(record: AttendanceRecord) {
    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(record.date)
    val dayName       = SimpleDateFormat("EEEE",         Locale.getDefault()).format(record.date)

    data class PillStyle(val bg: Color, val text: String, val fg: Color, val border: Boolean)
    val pill = when (record.status) {
        AttendanceStatus.PRESENT        -> PillStyle(Color.Transparent, "Present",  Primary,          true)
        AttendanceStatus.ABSENT         -> PillStyle(Color(0xFFD32F2F), "Absent",   Color.White,       false)
        AttendanceStatus.APPROVED_LEAVE -> PillStyle(Color(0xFFFFA726), "Leave",    Color(0xFF1A1A1A), false)
        AttendanceStatus.UNMARKED       -> PillStyle(SurfaceVar,        "Unmarked", OnSurface,         false)
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0D7A6E)),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(formattedDate, color = Color.White,                       fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(dayName,       color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
                Text(
                    record.course.courseCode,
                    color     = Color.White.copy(alpha = 0.85f),
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(0.7f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(pill.bg)
                        .then(
                            if (pill.border) Modifier.border(1.dp, Primary, RoundedCornerShape(50.dp))
                            else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(pill.text, color = pill.fg, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            if (record.remarks.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(record.remarks, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

// ── Filter chip components ────────────────────────────────────────────────────
@Composable
private fun FilterChipButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceVar)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = OnSurface, fontSize = 12.sp, maxLines = 1)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = OnSurface, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun FilterDateButton(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean    = false,
    onClick: () -> Unit = {}
) {
    val bg   = if (active) PrimaryDark else SurfaceVar
    val tint = if (active) Primary     else OnSurface
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = tint, fontSize = 11.sp, maxLines = 1)
            Icon(Icons.Default.CalendarToday, null, tint = tint, modifier = Modifier.size(13.dp))
        }
    }
}

// ── DatePicker colour helper ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun datePickerColors() = DatePickerDefaults.colors(
    containerColor            = Surface,
    titleContentColor         = OnBackground,
    headlineContentColor      = Primary,
    weekdayContentColor       = OnSurface,
    dayContentColor           = OnBackground,
    selectedDayContainerColor = Primary,
    selectedDayContentColor   = OnPrimary,
    todayContentColor         = Primary,
    todayDateBorderColor      = Primary
)

// ── Utility ───────────────────────────────────────────────────────────────────
private fun shortDate(millis: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))