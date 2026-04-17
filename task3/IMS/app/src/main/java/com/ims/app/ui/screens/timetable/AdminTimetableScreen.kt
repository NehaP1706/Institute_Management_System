package com.ims.app.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ims.app.data.model.*
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.Screen
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*

// ── Colour tokens (Figma teal palette) ────────────────────────────────────────
private val SlotCardBg    = Color(0xFF00897B)
private val SlotCardDark  = Color(0xFF00695C)
private val ConflictBg    = Color(0xFF1A2C20)
private val ConflictAmber = Color(0xFFFFB300)
private val DayCircleBg   = Color(0xFF1A2C3D)
private val FilterChipBg  = Color(0xFF1A3448)

// ── Computed workload helpers ─────────────────────────────────────────────────

/**
 * Counts total scheduled hours across all timetable slots for a given semester.
 * Each slot contributes (endHour - startHour) hours.
 */
private fun computeWeeklyLoad(slots: List<TimetableSlot>): Int =
    slots.sumOf { slot ->
        try {
            val sh = slot.start.substringBefore(":").toInt()
            val eh = slot.end.substringBefore(":").toInt()
            (eh - sh).coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }

/** Maximum allowed weekly teaching hours (configurable constant). */
private const val MAX_WEEKLY_LOAD = 20

/**
 * Detects conflicts: two slots on the same day whose time ranges overlap.
 * Returns a list of conflicting slot pairs as human-readable descriptions.
 */
private fun detectConflicts(slots: List<TimetableSlot>): List<String> {
    val conflicts = mutableListOf<String>()
    val byDay = slots.groupBy { it.day }
    byDay.values.forEach { daySlots ->
        for (i in daySlots.indices) {
            for (j in i + 1 until daySlots.size) {
                val a = daySlots[i]
                val b = daySlots[j]
                if (timesOverlap(a.start, a.end, b.start, b.end)) {
                    conflicts.add(
                        "${a.course.courseCode} & ${b.course.courseCode} on ${a.day.name.take(3)} " +
                                "(${a.start}–${a.end} / ${b.start}–${b.end})"
                    )
                }
            }
        }
    }
    return conflicts
}

private fun timesOverlap(s1: String, e1: String, s2: String, e2: String): Boolean {
    return try {
        val start1 = toMinutes(s1); val end1 = toMinutes(e1)
        val start2 = toMinutes(s2); val end2 = toMinutes(e2)
        start1 < end2 && start2 < end1
    } catch (_: Exception) { false }
}

private fun toMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + (parts.getOrNull(1)?.toInt() ?: 0)
}

// ─────────────────────────────────────────────────────────────────────────────

/** Admin Timetable Management — Figma-accurate UI with computed stats */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimetableScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedDay   by remember { mutableStateOf(DayEnum.MONDAY) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSlot   by remember { mutableStateOf<TimetableSlot?>(null) }
    var selectedDept  by remember { mutableStateOf("DEPT") }
    var selectedSem   by remember { mutableStateOf(viewModel.selectedSemester) }
    var selectedRoom  by remember { mutableStateOf("ROOM") }
    var showConflictDetail by remember { mutableStateOf(false) }

    // ── Computed stats (reactive to selectedSem) ──────────────────────────────
    val allSlots  = viewModel.getTimetableForSemester(selectedSem)
    val daySlots  = allSlots.filter { it.day == selectedDay }

    // Room filter applied on top
    val visibleSlots = if (selectedRoom == "ROOM") daySlots
    else daySlots.filter { it.room.name == selectedRoom }

    val weeklyLoad    = computeWeeklyLoad(allSlots)
    val loadPct       = (weeklyLoad.toFloat() / MAX_WEEKLY_LOAD).coerceIn(0f, 1f)
    val loadPctInt    = (loadPct * 100).toInt()
    val conflicts     = detectConflicts(allSlots)
    val conflictCount = conflicts.size

    // Room options derived from actual data
    val roomOptions = listOf("ROOM") + allSlots.map { it.room.name }.distinct().sorted()

    // ── Conflict detail sheet ─────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showConflictDetail && conflicts.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showConflictDetail = false },
            sheetState       = sheetState,
            containerColor   = Surface,
            dragHandle       = { Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Divider))
            }}
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = ConflictAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$conflictCount Conflict${if (conflictCount != 1) "s" else ""} Detected",
                        color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
                HorizontalDivider(color = Divider)
                conflicts.forEach { desc ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ConflictBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ConflictAmber,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(desc, color = OnBackground, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────
    if (showAddDialog) {
        AddTimetableSlotDialog(
            existingSlot = editingSlot,
            currentSem   = selectedSem,
            onDismiss    = { showAddDialog = false; editingSlot = null },
            onSave       = { slot ->
                viewModel.addTimetableSlot(slot)
                showAddDialog = false
                editingSlot   = null
            }
        )
    }

    Scaffold(
        topBar = {
            TimetableTopBar(
                userName   = viewModel.getCurrentUserName(),
                userRole   = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate,
                onAddClick = { editingSlot = null; showAddDialog = true }
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
            item { Spacer(Modifier.height(4.dp)) }

            // ── DEPT / SEM / ROOM filter row ──────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownFilterChip(
                        label    = selectedDept,
                        options  = listOf("DEPT", "CSE", "ECE", "CSD"),
                        onSelect = { selectedDept = it }
                    )
                    DropdownFilterChip(
                        label    = selectedSem,
                        options  = listOf("Sem 5", "Sem 6"),
                        onSelect = { selectedSem = it; viewModel.selectedSemester = it }
                    )
                    DropdownFilterChip(
                        label    = selectedRoom,
                        options  = roomOptions,
                        onSelect = { selectedRoom = it }
                    )
                }
            }

            // ── Day circle tabs ───────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayEnum.values().forEach { day ->
                        val isSelected   = day == selectedDay
                        val hasSlotsToday = allSlots.any { it.day == day }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else DayCircleBg)
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = if (isSelected) Color.Transparent
                                    else if (hasSlotsToday) Primary.copy(alpha = 0.4f)
                                    else Color.White.copy(0.1f),
                                    shape = CircleShape
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.name.first().toString(),
                                color      = if (isSelected) Color.Black else
                                    if (hasSlotsToday) Primary else OnSurface.copy(alpha = 0.5f),
                                fontWeight = if (isSelected || hasSlotsToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 15.sp
                            )
                        }
                    }
                }
            }

            // ── Instructor Weekly Load ────────────────────────────────────────
            // Only rendered when there are actual slots to measure
            if (allSlots.isNotEmpty()) {
                item {
                    val barColor = when {
                        loadPctInt >= 90 -> Color(0xFFEF5350)  // over-loaded → red
                        loadPctInt >= 70 -> Color(0xFFFFA726)  // near limit  → amber
                        else             -> Primary            // healthy      → teal
                    }
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = CardBg),
                        shape    = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Instructor Weekly Load: $weeklyLoad / $MAX_WEEKLY_LOAD hrs",
                                    color    = OnSurface,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "$loadPctInt%",
                                    color      = barColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 15.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress   = { loadPct },
                                modifier   = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color      = barColor,
                                trackColor = SurfaceVar
                            )
                        }
                    }
                }
            }

            // ── Conflicts banner — only shown when conflicts > 0 ──────────────
            if (conflictCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ConflictBg)
                            .border(
                                width = 1.dp,
                                color = ConflictAmber.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = ConflictAmber,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "$conflictCount Conflict${if (conflictCount != 1) "s" else ""} Detected",
                                color      = OnBackground,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 13.sp
                            )
                        }
                        Text(
                            "REVIEW",
                            color      = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp,
                            modifier   = Modifier.clickable { showConflictDetail = true }
                        )
                    }
                }
            }

            // ── Day header ────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        selectedDay.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        color      = OnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Text(
                        "${visibleSlots.size} slot${if (visibleSlots.size != 1) "s" else ""}",
                        color    = OnSurface,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Slot cards ────────────────────────────────────────────────────
            if (visibleSlots.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint     = OnSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No slots for ${selectedDay.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }}",
                                color    = OnSurface,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap + to add a new slot",
                                color    = Primary,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { showAddDialog = true }
                            )
                        }
                    }
                }
            } else {
                items(visibleSlots) { slot ->
                    TimetableSlotCard(
                        slot     = slot,
                        onEdit   = { editingSlot = slot; showAddDialog = true },
                        onDelete = { /* stub: removeSlot(slot) */ }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableTopBar(
    userName: String,
    userRole: String,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Timetable Management",
                    color      = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp
                )
                Text(userName, color = Primary, fontSize = 12.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = { onNavigate(Screen.Dashboard.route) }) {
                Icon(Icons.Default.Menu, "Menu", tint = OnBackground)
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, "Notifications", tint = OnBackground)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(userRole, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, "Add Slot", tint = Primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
    )
}

// ── Dropdown filter chip ──────────────────────────────────────────────────────
@Composable
private fun DropdownFilterChip(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(FilterChipBg)
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Primary, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt, color = OnBackground, fontSize = 13.sp) },
                    onClick = { onSelect(opt); expanded = false },
                    modifier = Modifier.background(CardBg)
                )
            }
        }
    }
}

// ── Timetable slot card ───────────────────────────────────────────────────────
@Composable
private fun TimetableSlotCard(
    slot: TimetableSlot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val facultyInitials = slot.course.instructor.firstOrNull()
        ?.employee?.user
        ?.let { StubRepository.getUserDisplayName(it) }
        ?.split(" ")
        ?.mapNotNull { it.firstOrNull()?.toString() }
        ?.take(2)?.joinToString("") ?: "DR"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SlotCardBg)
    ) {
        // Left column: time + room
        Column(
            modifier = Modifier
                .width(78.dp)
                .fillMaxHeight()
                .background(SlotCardDark)
                .padding(vertical = 18.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(slot.start, color = Color.White,              fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("–",        color = Color.White.copy(0.55f),  fontSize   = 10.sp)
            Text(slot.end,   color = Color.White.copy(0.80f),  fontSize   = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                slot.room.name,
                color      = Color.White.copy(0.70f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Center: course title + code
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                slot.course.title,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(slot.course.courseCode, color = Color.White.copy(0.70f), fontSize = 12.sp)
        }

        // Right: faculty avatar + overflow menu
        Column(
            modifier            = Modifier.padding(end = 10.dp, top = 14.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SlotCardDark),
                contentAlignment = Alignment.Center
            ) {
                Text(facultyInitials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint     = Color.White.copy(0.80f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier         = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBg)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit,   null, tint = Primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Edit", color = OnBackground)
                            }
                        },
                        onClick  = { menuExpanded = false; onEdit() },
                        modifier = Modifier.background(CardBg)
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Delete", color = Color(0xFFEF5350))
                            }
                        },
                        onClick  = { menuExpanded = false; onDelete() },
                        modifier = Modifier.background(CardBg)
                    )
                }
            }
        }
    }
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────
@Composable
private fun AddTimetableSlotDialog(
    existingSlot: TimetableSlot? = null,
    currentSem: String,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit
) {
    var selectedCourse by remember { mutableStateOf(existingSlot?.course ?: StubRepository.courses.first()) }
    var selectedDay    by remember { mutableStateOf(existingSlot?.day    ?: DayEnum.MONDAY) }
    var startTime      by remember { mutableStateOf(existingSlot?.start  ?: "09:00") }
    var endTime        by remember { mutableStateOf(existingSlot?.end    ?: "10:00") }
    var conflictMsg    by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape  = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (existingSlot == null) "Add Timetable Slot" else "Edit Timetable Slot",
                    color = Primary, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )

                // Course picker
                Text("Select Course", color = OnSurface, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(StubRepository.courses) { course ->
                        val sel = course.courseId == selectedCourse.courseId
                        FilterChip(
                            selected = sel,
                            onClick  = { selectedCourse = course; conflictMsg = null },
                            label    = { Text(course.courseCode, fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor     = OnPrimary,
                                containerColor         = SurfaceVar,
                                labelColor             = OnSurface
                            )
                        )
                    }
                }

                // Day picker
                Text("Day", color = OnSurface, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DayEnum.values().toList()) { day ->
                        val sel = day == selectedDay
                        FilterChip(
                            selected = sel,
                            onClick  = { selectedDay = day; conflictMsg = null },
                            label    = { Text(day.name.take(3), fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor     = OnPrimary,
                                containerColor         = SurfaceVar,
                                labelColor             = OnSurface
                            )
                        )
                    }
                }

                // Time fields
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value         = startTime,
                        onValueChange = { startTime = it; conflictMsg = null },
                        label         = { Text("Start (HH:mm)") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        colors        = imsFieldColors()
                    )
                    OutlinedTextField(
                        value         = endTime,
                        onValueChange = { endTime = it; conflictMsg = null },
                        label         = { Text("End (HH:mm)") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        colors        = imsFieldColors()
                    )
                }

                // Inline conflict message
                if (conflictMsg != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ConflictBg)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = ConflictAmber, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(conflictMsg!!, color = OnBackground, fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            // Real-time conflict check against existing slots
                            val existingSlots = StubRepository.timetableSlots.filter {
                                it.day == selectedDay && it.slotId != existingSlot?.slotId
                            }
                            val hasConflict = existingSlots.any { other ->
                                timesOverlap(startTime, endTime, other.start, other.end)
                            }
                            if (hasConflict) {
                                conflictMsg = "Time overlaps with an existing slot. Choose another time."
                            } else {
                                val newSlot = TimetableSlot(
                                    slotId   = existingSlot?.slotId ?: "new_${System.currentTimeMillis()}",
                                    course   = selectedCourse,
                                    day      = selectedDay,
                                    start    = startTime,
                                    end      = endTime,
                                    room     = existingSlot?.room
                                        ?: Room("rm1", "LH-101", 120, RoomType.Room100C, "Lecture Block"),
                                    semester = currentSem
                                )
                                onSave(newSlot)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(
                            if (existingSlot == null) "Save Slot" else "Update Slot",
                            color = OnPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun imsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Primary,
    unfocusedBorderColor = Divider,
    focusedLabelColor    = Primary,
    cursorColor          = Primary,
    focusedTextColor     = OnBackground,
    unfocusedTextColor   = OnBackground
)