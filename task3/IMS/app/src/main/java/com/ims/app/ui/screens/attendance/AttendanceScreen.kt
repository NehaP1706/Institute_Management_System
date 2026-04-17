package com.ims.app.ui.screens.attendance

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.AttendanceRecord
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.data.model.Course
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import com.ims.app.util.exportDailyAttendancePdf
import java.text.SimpleDateFormat
import java.util.*

private enum class ViewMode { DAILY, MONTHLY }

// Course icon helper
private fun iconForCourse(courseCode: String): ImageVector = when {
    courseCode.startsWith("CS1")  -> Icons.Default.Terminal
    courseCode.startsWith("CS2")  -> Icons.Default.Psychology
    courseCode.startsWith("CS3")  -> Icons.Default.Storage
    courseCode.startsWith("CS4")  -> Icons.Default.BarChart
    courseCode.startsWith("MATH") -> Icons.Default.Functions
    courseCode.startsWith("PHIL") -> Icons.Default.Public
    else                          -> Icons.Default.Book
}

/**
 * Student read-only Attendance screen – Daily view.
 *
 * PDF icon → exports a daily attendance report for ALL courses, filtered
 * by the current From/To date range (if set).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    // Filter state
    var selectedCourse        by remember { mutableStateOf(viewModel.allCourses.firstOrNull()) }
    var showCourseSheet       by remember { mutableStateOf(false) }
    var pendingCourse         by remember { mutableStateOf(selectedCourse) }
    var sheetSearch           by remember { mutableStateOf("") }

    var viewMode              by remember { mutableStateOf(ViewMode.DAILY) }
    var viewModeMenuExpanded  by remember { mutableStateOf(false) }

    var fromDateMillis        by remember { mutableStateOf<Long?>(null) }
    var toDateMillis          by remember { mutableStateOf<Long?>(null) }
    var showFromPicker        by remember { mutableStateOf(false) }
    var showToPicker          by remember { mutableStateOf(false) }

    // Export-progress flag (keeps icon from being tapped twice)
    var isExporting by remember { mutableStateOf(false) }

    // Monthly mode – hand off entirely
    if (viewMode == ViewMode.MONTHLY) {
        MonthlyAttendanceScreen(
            viewModel       = viewModel,
            currentRoute    = currentRoute,
            onNavigate      = onNavigate,
            onSwitchToDaily = { viewMode = ViewMode.DAILY }
        )
        return
    }

    //  Derived data (ALL courses, date-filtered)
    val allRecordsForExport by remember(fromDateMillis, toDateMillis) {
        derivedStateOf {
            viewModel.getAttendanceRecords().filter { r ->
                val ms        = r.date.time
                val afterFrom = fromDateMillis?.let { ms >= it } ?: true
                val beforeTo  = toDateMillis?.let  { ms <= it + 86_400_000L } ?: true
                afterFrom && beforeTo
            }
        }
    }

    // Records shown on screen (filtered by selected course too)
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

    //  Date picker states
    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = fromDateMillis ?: System.currentTimeMillis()
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = toDateMillis ?: System.currentTimeMillis()
    )

    //  Course bottom sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showCourseSheet) {
        ModalBottomSheet(
            onDismissRequest  = { showCourseSheet = false },
            sheetState        = sheetState,
            containerColor    = Color(0xFF111E2B),
            dragHandle        = null
        ) {
            CoursePickerSheetContent(
                courses       = viewModel.allCourses.filter {
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

    //  Date picker dialogs
    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    fromDateMillis = fromPickerState.selectedDateMillis
                    showFromPicker = false
                }) { Text("OK", color = Primary) }
            },
            dismissButton    = {
                TextButton(onClick = { showFromPicker = false }) {
                    Text("Cancel", color = OnSurface)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(state = fromPickerState, colors = attendanceDatePickerColors())
        }
    }

    if (showToPicker) {
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    toDateMillis = toPickerState.selectedDateMillis
                    showToPicker = false
                }) { Text("OK", color = Primary) }
            },
            dismissButton    = {
                TextButton(onClick = { showToPicker = false }) {
                    Text("Cancel", color = OnSurface)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(state = toPickerState, colors = attendanceDatePickerColors())
        }
    }

    //  Scaffold
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
                    // PDF export button
                    IconButton(
                        onClick = {
                            if (isExporting) return@IconButton
                            if (allRecordsForExport.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "No attendance records to export.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@IconButton
                            }
                            isExporting = true
                            try {
                                exportDailyAttendancePdf(
                                    context    = context,
                                    records    = allRecordsForExport,
                                    allCourses = viewModel.allCourses,
                                    fromMillis = fromDateMillis,
                                    toMillis   = toDateMillis
                                )
                            } finally {
                                isExporting = false
                            }
                        }
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(20.dp),
                                color     = Primary,
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

            //  Filter row
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    // 1. Course picker
                    FilterChipButton(
                        label    = selectedCourse?.courseCode ?: "Course",
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            pendingCourse = selectedCourse
                            sheetSearch   = ""
                            showCourseSheet = true
                        }
                    )

                    // 2. Daily / Monthly toggle
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
                                    onClick = { viewMode = mode; viewModeMenuExpanded = false },
                                    leadingIcon = {
                                        if (viewMode == mode)
                                            Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }

                    // 3. From date
                    FilterDateButton(
                        label    = fromDateMillis?.let { shortDate(it) } ?: "From",
                        modifier = Modifier.weight(1f),
                        active   = fromDateMillis != null,
                        onClick  = { showFromPicker = true }
                    )

                    // 4. To date
                    FilterDateButton(
                        label    = toDateMillis?.let { shortDate(it) } ?: "To",
                        modifier = Modifier.weight(0.8f),
                        active   = toDateMillis != null,
                        onClick  = { showToPicker = true }
                    )
                }

                // Active date-range indicator + clear
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
                            color = Primary, fontSize = 11.sp
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

            //  Hero summary card
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

            // Section header
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

            // Log rows
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
                items(displayRecords) { record -> AttendanceLogRow(record) }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

//  Figma Course Picker Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoursePickerSheetContent(
    courses:        List<Course>,
    searchQuery:    String,
    onSearchChange: (String) -> Unit,
    pendingCourse:  Course?,
    onSelect:       (Course) -> Unit,
    onBack:         () -> Unit,
    onClose:        () -> Unit,
    onApply:        () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnBackground)
            }
            Text(
                "Select Course",
                color      = OnBackground,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                modifier   = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Box(
                    modifier         = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceVar),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = OnBackground, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Search field
        OutlinedTextField(
            value          = searchQuery,
            onValueChange  = onSearchChange,
            placeholder    = { Text("Search available courses…", color = OnSurface.copy(0.45f)) },
            leadingIcon    = { Icon(Icons.Default.Search, null, tint = OnSurface.copy(0.5f)) },
            modifier       = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape          = RoundedCornerShape(14.dp),
            singleLine     = true,
            colors         = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Color.Transparent,
                unfocusedBorderColor    = Color.Transparent,
                focusedContainerColor   = SurfaceVar,
                unfocusedContainerColor = SurfaceVar,
                focusedTextColor        = OnBackground,
                unfocusedTextColor      = OnBackground,
                cursorColor             = Primary
            )
        )

        Spacer(Modifier.height(12.dp))

        // Course list
        LazyColumn(
            modifier            = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(courses) { course ->
                CoursePickerRow(
                    course     = course,
                    isSelected = pendingCourse?.courseId == course.courseId,
                    onClick    = { onSelect(course) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Apply button
        Button(
            onClick  = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape  = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Apply Filter", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.FilterList, null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

//  Single row inside the sheet
@Composable
private fun CoursePickerRow(course: Course, isSelected: Boolean, onClick: () -> Unit) {
    val rowBg     = if (isSelected) Primary  else SurfaceVar
    val textColor = if (isSelected) Color.Black else OnBackground
    val subColor  = if (isSelected) Color.Black.copy(alpha = 0.65f) else OnSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color.Black.copy(alpha = 0.15f)
                    else PrimaryDark.copy(alpha = 0.35f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconForCourse(course.courseCode),
                contentDescription = null,
                tint     = if (isSelected) Color.Black else Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(course.title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("${course.courseCode} • ${course.semester}", color = subColor, fontSize = 12.sp)
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, "Selected", tint = Color.Black, modifier = Modifier.size(22.dp))
        }
    }
}

//  Hero summary card
@Composable
private fun AttendanceSummaryHeroCard(
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
            Text("$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

// Log row
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(formattedDate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(dayName, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
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

// Shared filter chip components
@Composable
internal fun FilterChipButton(
    label:    String,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit = {}
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
internal fun FilterDateButton(
    label:   String,
    modifier: Modifier  = Modifier,
    active:  Boolean     = false,
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

//  DatePicker colours
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun attendanceDatePickerColors() = DatePickerDefaults.colors(
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

private fun shortDate(millis: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))