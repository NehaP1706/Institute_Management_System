package com.ims.app.ui.screens.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*
import com.ims.app.util.exportMonthlyAttendancePdf
import java.text.SimpleDateFormat
import java.util.*

// ── Colour palette ────────────────────────────────────────────────────────────
private val AmTeal    = Color(0xFF00A896)
private val AmChipBg  = Color(0xFF0D2233)
private val AmSurface = Color(0xFF0D1B2A)
private val AmCard    = Color(0xFF0D2233)
private val AmPresent = Color(0xFF00BFA5)
private val AmAbsent  = Color(0xFFEF5350)
private val AmLeave   = Color(0xFFFFA726)
private val AmMuted   = Color(0xFF607D8B)

/** Per-day attendance counts shown inside each calendar cell. */
private data class DayStat(val present: Int, val absent: Int, val leave: Int) {
    val hasData: Boolean get() = present > 0 || absent > 0 || leave > 0
}

/**
 * Admin Monthly Attendance Screen.
 *
 * Fixes vs original:
 *  - Course chip opens [CoursePickerSheetContent] bottom sheet (same as daily screen).
 *  - Batch chip, Monthly chip, and top-bar filter icon all open a filter
 *    ModalBottomSheet with batch selector + Daily/Monthly view-mode toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonthlyAttendanceScreen(
    viewModel:       IMSViewModel,
    currentRoute:    String,
    onNavigate:      (String) -> Unit,
    onSwitchToDaily: () -> Unit
) {
    val context = LocalContext.current

    // ── Filter state ──────────────────────────────────────────────────────────
    val courseOptions = StubRepository.courses
    val batchOptions  = listOf("BATCH A", "BATCH B", "BATCH C")

    var selectedCourse by remember { mutableStateOf(courseOptions.first()) }
    var selectedBatch  by remember { mutableStateOf(batchOptions.first()) }

    // Course picker sheet
    var showCourseSheet by remember { mutableStateOf(false) }
    var pendingCourse   by remember { mutableStateOf(selectedCourse) }
    var courseSearch    by remember { mutableStateOf("") }

    // Filter sheet (batch + view-mode)
    var showFilterSheet by remember { mutableStateOf(false) }
    var pendingBatch    by remember { mutableStateOf(selectedBatch) }

    // ── Calendar navigation ───────────────────────────────────────────────────
    val today    = remember { Calendar.getInstance() }
    var calYear  by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var calMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }

    val monthLabel = remember(calYear, calMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(Calendar.getInstance().apply { set(calYear, calMonth, 1) }.time)
    }

    // ── Records for the selected course + month ───────────────────────────────
    val monthRecords by remember(selectedCourse, calYear, calMonth) {
        derivedStateOf {
            StubRepository.getAttendanceForCourse(selectedCourse.courseId).filter { r ->
                val c = Calendar.getInstance().apply { time = r.date }
                c.get(Calendar.YEAR) == calYear && c.get(Calendar.MONTH) == calMonth
            }
        }
    }

    // ── Aggregate stats — summed directly from per-day cell data ───────────
    val rawPresent  = monthRecords.count { it.status == AttendanceStatus.PRESENT        }
    val rawAbsent   = monthRecords.count { it.status == AttendanceStatus.ABSENT         }
    val rawLeave    = monthRecords.count { it.status == AttendanceStatus.APPROVED_LEAVE }
    val hasAny      = (rawPresent + rawAbsent + rawLeave) > 0
    val statPresent = if (hasAny) "$rawPresent" else "–"
    val statAbsent  = if (hasAny) "$rawAbsent"  else "–"
    val statLeave   = if (hasAny) "$rawLeave"   else "–"

    // ── Per-day stats map ─────────────────────────────────────────────────────
    val dayStats: Map<Int, DayStat> by remember(monthRecords) {
        derivedStateOf {
            buildMap {
                monthRecords.forEach { r ->
                    val d   = Calendar.getInstance().apply { time = r.date }.get(Calendar.DAY_OF_MONTH)
                    val cur = getOrDefault(d, DayStat(0, 0, 0))
                    put(d, when (r.status) {
                        AttendanceStatus.PRESENT        -> cur.copy(present = cur.present + 1)
                        AttendanceStatus.ABSENT         -> cur.copy(absent  = cur.absent  + 1)
                        AttendanceStatus.APPROVED_LEAVE -> cur.copy(leave   = cur.leave   + 1)
                        else                            -> cur
                    })
                }
            }
        }
    }

    // ── Grid days (Sun-start, bleeds prev/next month padding) ────────────────
    val gridDays: List<Pair<Int, Boolean>> = remember(calYear, calMonth) {
        val cal         = Calendar.getInstance().apply { set(calYear, calMonth, 1) }
        val firstDow    = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevDays    = Calendar.getInstance()
            .apply { set(calYear, calMonth, 1); add(Calendar.MONTH, -1) }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
        buildList {
            for (i in firstDow - 1 downTo 0) add(prevDays - i to false)
            for (d in 1..daysInMonth)         add(d to true)
            var nd = 1; while (size % 7 != 0) add(nd++ to false)
        }
    }

    val todayDay    = today.get(Calendar.DAY_OF_MONTH)
    val isThisMonth = calYear == today.get(Calendar.YEAR) && calMonth == today.get(Calendar.MONTH)

    // ── PDF export ────────────────────────────────────────────────────────────
    var isExporting by remember { mutableStateOf(false) }
    val allMonthRecords by remember(calYear, calMonth) {
        derivedStateOf {
            StubRepository.attendanceRecords.filter { r ->
                val c = Calendar.getInstance().apply { time = r.date }
                c.get(Calendar.YEAR) == calYear && c.get(Calendar.MONTH) == calMonth
            }
        }
    }

    // ── Course picker bottom sheet ────────────────────────────────────────────
    val courseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showCourseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCourseSheet = false },
            sheetState       = courseSheetState,
            containerColor   = Color(0xFF111E2B),
            dragHandle       = null
        ) {
            CoursePickerSheetContent(
                courses = courseOptions.filter {
                    courseSearch.isBlank() ||
                            it.title.contains(courseSearch, ignoreCase = true) ||
                            it.courseCode.contains(courseSearch, ignoreCase = true)
                },
                searchQuery    = courseSearch,
                onSearchChange = { courseSearch = it },
                pendingCourse  = pendingCourse,
                onSelect       = { pendingCourse = it },
                onBack         = { showCourseSheet = false },
                onClose        = { showCourseSheet = false; courseSearch = "" },
                onApply        = {
                    selectedCourse  = pendingCourse
                    showCourseSheet = false
                    courseSearch    = ""
                }
            )
        }
    }

    // ── Filter bottom sheet (batch + view-mode) ───────────────────────────────
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState       = filterSheetState,
            containerColor   = Color(0xFF111E2B),
            dragHandle = {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(AmMuted.copy(alpha = 0.4f))
                    )
                }
            }
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filters", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Text("BATCH", color = AmMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    batchOptions.forEach { batch ->
                        val selected = batch == pendingBatch
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AmTeal else AmChipBg)
                                .clickable { pendingBatch = batch }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(batch, color = if (selected) Color.White else AmMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text("VIEW MODE", color = AmMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Monthly", "Daily").forEach { mode ->
                        val active = mode == "Monthly"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AmTeal else AmChipBg)
                                .clickable { showFilterSheet = false; if (mode == "Daily") onSwitchToDaily() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(mode, color = if (active) Color.White else AmMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick  = { selectedBatch = pendingBatch; showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = AmTeal),
                    shape    = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.FilterList, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Filter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onNavigate("back") }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = OnBackground)
                    }
                },
                title = {
                    Text("Attendance — Admin", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                actions = {
                    // FIX: was a non-clickable Icon; now an IconButton opening the filter sheet
                    IconButton(onClick = { pendingBatch = selectedBatch; showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, "Filters", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, isAdmin = true, onNavigate = onNavigate)
        },
        containerColor = Background
    ) { padding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── ① Course chip + filter pills ──────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))

                // FIX: opens CoursePickerSheetContent bottom sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmTeal)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            pendingCourse   = selectedCourse
                            courseSearch    = ""
                            showCourseSheet = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${selectedCourse.title} – ${selectedCourse.courseCode}",
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // FIX: Batch + Monthly chips both open the filter sheet
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmPill(label = selectedBatch, onClick = { pendingBatch = selectedBatch; showFilterSheet = true })
                    AmPill(label = "Monthly",     onClick = { pendingBatch = selectedBatch; showFilterSheet = true })
                }
            }

            // ── ② Month navigator ─────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { if (calMonth == 0) { calMonth = 11; calYear-- } else calMonth-- }) {
                        Icon(Icons.Default.ChevronLeft, "Prev", tint = OnBackground)
                    }
                    Text(monthLabel, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    IconButton(onClick = { if (calMonth == 11) { calMonth = 0; calYear++ } else calMonth++ }) {
                        Icon(Icons.Default.ChevronRight, "Next", tint = OnBackground)
                    }
                }
            }

            // ── ③ Aggregate stat bubbles ──────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    AmStatBubble("TOTAL\nSTUDENTS", "50",        AmTeal)
                    AmStatBubble("TOTAL\nPRESENT",  statPresent, AmPresent)
                    AmStatBubble("TOTAL\nABSENT",   statAbsent,  AmAbsent)
                    AmStatBubble("TOTAL\nLEAVE",    statLeave,   AmLeave)
                }
            }

            // ── ④ Calendar grid ───────────────────────────────────────────────
            item {
                AmCalendarGrid(
                    gridDays = gridDays,
                    dayStats = dayStats,
                    todayDay = if (isThisMonth) todayDay else -1
                )
            }

            // ── ⑤ Export Full Report ──────────────────────────────────────────
            item {
                Button(
                    onClick = {
                        if (isExporting) return@Button
                        isExporting = true
                        try {
                            exportMonthlyAttendancePdf(
                                context    = context,
                                records    = allMonthRecords,
                                allCourses = StubRepository.courses,
                                year       = calYear,
                                month      = calMonth
                            )
                        } finally { isExporting = false }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = AmChipBg),
                    shape    = RoundedCornerShape(28.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AmTeal, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.FileDownload, null, tint = OnBackground)
                        Spacer(Modifier.width(10.dp))
                        Text("Export Full Report", color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Filter pill chip ──────────────────────────────────────────────────────────
@Composable
private fun AmPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AmChipBg)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFF90A4AE), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(14.dp))
        }
    }
}

// ── Stat bubble ───────────────────────────────────────────────────────────────
@Composable
private fun AmStatBubble(label: String, value: String, color: Color) {
    Box(
        modifier         = Modifier.size(72.dp).clip(RoundedCornerShape(50)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 7.sp, textAlign = TextAlign.Center, lineHeight = 9.sp)
        }
    }
}

// ── Calendar grid ─────────────────────────────────────────────────────────────
@Composable
private fun AmCalendarGrid(
    gridDays: List<Pair<Int, Boolean>>,
    dayStats: Map<Int, DayStat>,
    todayDay: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmCard)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { h ->
                Text(h, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    color = AmMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        gridDays.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { (day, isCurrent) ->
                    Box(
                        modifier         = Modifier.weight(1f).aspectRatio(0.75f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        AmCalDayCell(
                            day       = day,
                            stat      = if (isCurrent) dayStats[day] else null,
                            isToday   = isCurrent && day == todayDay,
                            isCurrent = isCurrent
                        )
                    }
                }
            }
        }
    }
}

// ── Single day cell ───────────────────────────────────────────────────────────
@Composable
private fun AmCalDayCell(day: Int, stat: DayStat?, isToday: Boolean, isCurrent: Boolean) {
    val hasData = stat?.hasData == true
    val bg: Color = when {
        isToday   -> AmTeal
        hasData   -> AmSurface
        isCurrent -> AmSurface.copy(alpha = 0.45f)
        else      -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 1.dp, vertical = 3.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text       = "$day",
                color      = when {
                    !isCurrent -> AmMuted.copy(alpha = 0.25f)
                    isToday    -> Color.White
                    else       -> Color.White.copy(alpha = 0.9f)
                },
                fontSize   = 9.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign  = TextAlign.Center
            )
            if (isCurrent && hasData && stat != null) {
                Text("P:${stat.present}", color = AmPresent, fontSize = 7.sp, lineHeight = 8.sp, textAlign = TextAlign.Center)
                Text("A:${stat.absent}",  color = AmAbsent,  fontSize = 7.sp, lineHeight = 8.sp, textAlign = TextAlign.Center)
                Text("L:${stat.leave}",   color = AmLeave,   fontSize = 7.sp, lineHeight = 8.sp, textAlign = TextAlign.Center)
            }
        }
    }
}