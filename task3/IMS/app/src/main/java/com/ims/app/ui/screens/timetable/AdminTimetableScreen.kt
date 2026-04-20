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
import com.ims.app.data.model.*
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.BottomNavBar
import com.ims.app.ui.theme.*

private val SlotCardBg    = Color(0xFF00897B)
private val SlotCardDark  = Color(0xFF00695C)
private val ConflictBg    = Color(0xFF1A2C20)
private val ConflictAmber = Color(0xFFFFB300)
private val DayCircleBg   = Color(0xFF1A2C3D)
private val FilterChipBg  = Color(0xFF1A3448)

private fun computeWeeklyLoad(slots: List<TimetableSlot>): Int =
    slots.sumOf { slot ->
        try {
            val sh = slot.start.substringBefore(":").toInt()
            val eh = slot.end.substringBefore(":").toInt()
            (eh - sh).coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }

private const val MAX_WEEKLY_LOAD = 20

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

    var selectedCourseFilter by remember { mutableStateOf<Course?>(null) }
    var showCourseFilter     by remember { mutableStateOf(false) }

    val allSlots  = viewModel.getTimetableForSemester(selectedSem)
    val daySlots  = allSlots.filter { it.day == selectedDay }

    val visibleSlots = daySlots
        .let { slots -> if (selectedRoom == "ROOM") slots else slots.filter { it.room.name == selectedRoom } }
        .let { slots -> if (selectedCourseFilter == null) slots else slots.filter { it.course.courseId == selectedCourseFilter!!.courseId } }

    val weeklyLoad    = computeWeeklyLoad(allSlots)
    val loadPct       = (weeklyLoad.toFloat() / MAX_WEEKLY_LOAD).coerceIn(0f, 1f)
    val loadPctInt    = (loadPct * 100).toInt()
    val conflicts     = detectConflicts(allSlots)
    val conflictCount = conflicts.size

    val roomOptions = listOf("ROOM") + allSlots.map { it.room.name }.distinct().sorted()

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

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showCourseFilter) {
        ModalBottomSheet(
            onDismissRequest = { showCourseFilter = false },
            sheetState       = filterSheetState,
            containerColor   = Surface,
            dragHandle = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Divider)
                    )
                }
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Filter by Course",
                    color      = OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
                HorizontalDivider(color = Divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedCourseFilter == null) Primary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { selectedCourseFilter = null; showCourseFilter = false }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("All Courses", color = OnBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    if (selectedCourseFilter == null)
                        Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                }

                viewModel.allCourses.forEach { course ->
                    val isSelected = selectedCourseFilter?.courseId == course.courseId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedCourseFilter = course; showCourseFilter = false }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                course.title,
                                color      = OnBackground,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 14.sp
                            )
                            Text(
                                course.courseCode,
                                color    = Primary,
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected)
                            Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Divider.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTimetableSlotScreen(
            existingSlot = editingSlot,
            currentSem   = selectedSem,
            allRooms     = viewModel.getAllRooms(),
            existingSlots = viewModel.getTimetableForSemester(selectedSem),
            courses      = viewModel.allCourses,
            onBack       = { showAddDialog = false; editingSlot = null },
            onSave       = { slot ->
                viewModel.addTimetableSlot(slot)
                showAddDialog = false
                editingSlot   = null
            }
        )
        return  
    }

    Scaffold(
        topBar = {
            TimetableTopBar(
                onNavigate          = onNavigate,
                onAddClick          = { editingSlot = null; showAddDialog = true },
                onFilterClick       = { showCourseFilter = true },
                courseFilterActive  = selectedCourseFilter != null
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

            if (allSlots.isNotEmpty()) {
                item {
                    val barColor = when {
                        loadPctInt >= 90 -> Color(0xFFEF5350)  
                        loadPctInt >= 70 -> Color(0xFFFFA726)  
                        else             -> Primary            
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableTopBar(
    onNavigate:         (String) -> Unit,
    onAddClick:         () -> Unit,
    onFilterClick:      () -> Unit,
    courseFilterActive: Boolean
) {
    TopAppBar(
        title = {
            Text(
                "Timetable Management",
                color      = OnBackground,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = { onNavigate("back") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnBackground)
            }
        },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector        = Icons.Default.FilterList,
                    contentDescription = "Filter by Course",
                    tint               = if (courseFilterActive) Primary else OnBackground
                )
            }

            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Slot", tint = Primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
    )
}

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

@Composable
private fun TimetableSlotCard(
    slot: TimetableSlot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val instructorInitials = slot.course.instructorName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2).joinToString("")
        .ifEmpty { "IN" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SlotCardBg)
    ) {
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
                Text(instructorInitials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

@Composable
private fun AddTimetableSlotScreen(
    existingSlot  : TimetableSlot?,
    currentSem    : String,
    allRooms      : List<Room>,
    existingSlots : List<TimetableSlot>,
    courses       : List<Course>,
    onBack        : () -> Unit,
    onSave        : (TimetableSlot) -> Unit
) {
    var selectedCourse by remember { mutableStateOf(existingSlot?.course ?: courses.first()) }
    var selectedDate   by remember { mutableStateOf(existingSlot?.let { formatSlotDate(it) } ?: todayLabel()) }
    var startTime      by remember { mutableStateOf(existingSlot?.start ?: "09:00") }
    var endTime        by remember { mutableStateOf(existingSlot?.end   ?: "10:00") }
    var selectedRoom   by remember { mutableStateOf<Room?>(existingSlot?.room) }
    var roomExpanded   by remember { mutableStateOf(false) }

    val conflictInfo: ConflictInfo = remember(selectedRoom, startTime, endTime, selectedDate) {
        detectSlotConflict(
            newStart     = startTime,
            newEnd       = endTime,
            newRoom      = selectedRoom,
            existingSlots = existingSlots.filter { it.slotId != existingSlot?.slotId }
        )
    }

    Scaffold(
        topBar = {
            AddSlotTopBar(
                isEdit   = existingSlot != null,
                onBack   = onBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick  = {
                        val room = selectedRoom ?: allRooms.firstOrNull()
                        ?: Room("rm_default", "LH-101", 120, RoomType.Room100C, "Lecture Block")
                        onSave(
                            TimetableSlot(
                                slotId   = existingSlot?.slotId ?: "new_${System.currentTimeMillis()}",
                                course   = selectedCourse,
                                day      = parseDayFromDate(selectedDate),
                                start    = startTime,
                                end      = endTime,
                                room     = room,
                                semester = currentSem
                            )
                        )
                    },
                    enabled  = selectedRoom != null && conflictInfo.isAvailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Primary,
                        disabledContainerColor = SurfaceVar
                    )
                ) {
                    Text(
                        "Confirm Schedule Slot",
                        color      = if (selectedRoom != null && conflictInfo.isAvailable) Color.Black else OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint     = if (selectedRoom != null && conflictInfo.isAvailable) Color.Black else OnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        containerColor = Background
    ) { padding ->

        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                SlotSectionLabel("SUBJECT COURSE")
                Spacer(Modifier.height(8.dp))
                CourseSelector(
                    courses        = courses,
                    selectedCourse = selectedCourse,
                    onSelect       = { selectedCourse = it }
                )
            }

            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        SlotSectionLabel("DATE")
                        Spacer(Modifier.height(8.dp))
                        DateSelector(
                            label    = selectedDate,
                            onSelect = { selectedDate = it }
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        SlotSectionLabel("TIME SLOT")
                        Spacer(Modifier.height(8.dp))
                        TimeSlotSelector(
                            start    = startTime,
                            end      = endTime,
                            onStartChange = { startTime = it },
                            onEndChange   = { endTime   = it }
                        )
                    }
                }
            }

            item {
                SlotSectionLabel("VENUE SELECTION")
                Spacer(Modifier.height(8.dp))
                VenueSelector(
                    rooms          = allRooms,
                    selectedRoom   = selectedRoom,
                    expanded       = roomExpanded,
                    onToggle       = { roomExpanded = !roomExpanded },
                    onSelect       = { room ->
                        selectedRoom = room
                        roomExpanded = false
                    },
                    busyRoomIds    = getBusyRoomIds(startTime, endTime, existingSlots, existingSlot?.slotId)
                )
            }

            item {
                ConflictStatusBanner(conflictInfo)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSlotTopBar(isEdit: Boolean, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                if (isEdit) "Edit Timetable Slot" else "Add Timetable Slot",
                color      = OnBackground,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

@Composable
private fun SlotSectionLabel(text: String) {
    Text(
        text          = text,
        color         = OnSurface,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.4.sp
    )
}

@Composable
private fun CourseSelector(
    courses: List<Course>,
    selectedCourse: Course,
    onSelect: (Course) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceVar)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MenuBook, null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "${selectedCourse.title} (${selectedCourse.courseCode})",
                color      = OnBackground,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                modifier   = Modifier.weight(1f)
            )
            Icon(Icons.Default.KeyboardArrowDown, null, tint = OnSurface, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
        ) {
            courses.forEach { course ->
                val isSelected = course.courseId == selectedCourse.courseId
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                course.title,
                                color      = if (isSelected) Primary else OnBackground,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 13.sp
                            )
                            Text(
                                "${course.courseCode} • ${course.semester}",
                                color    = OnSurface,
                                fontSize = 11.sp
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(16.dp))
                    },
                    onClick  = { onSelect(course); expanded = false },
                    modifier = Modifier.background(CardBg)
                )
            }
        }
    }
}

@Composable
private fun DateSelector(label: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options  = nextSevenDayLabels()
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceVar)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
        ) {
            options.forEach { day ->
                DropdownMenuItem(
                    text    = { Text(day, color = OnBackground, fontSize = 13.sp) },
                    onClick = { onSelect(day); expanded = false },
                    modifier = Modifier.background(CardBg)
                )
            }
        }
    }
}

@Composable
private fun TimeSlotSelector(
    start: String,
    end: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    var startExpanded by remember { mutableStateOf(false) }
    var endExpanded   by remember { mutableStateOf(false) }
    val timeOptions   = generateTimeOptions()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVar)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AccessTime, null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))

        Box {
            Text(
                start,
                color    = OnBackground,
                fontSize = 13.sp,
                modifier = Modifier.clickable { startExpanded = true }
            )
            DropdownMenu(
                expanded         = startExpanded,
                onDismissRequest = { startExpanded = false },
                modifier         = Modifier
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
            ) {
                timeOptions.forEach { t ->
                    DropdownMenuItem(
                        text    = { Text(t, color = if (t == start) Primary else OnBackground, fontSize = 12.sp) },
                        onClick = { onStartChange(t); startExpanded = false },
                        modifier = Modifier.background(CardBg)
                    )
                }
            }
        }

        Text(" – ", color = OnSurface, fontSize = 13.sp)

        Box {
            Text(
                end,
                color    = OnBackground,
                fontSize = 13.sp,
                modifier = Modifier.clickable { endExpanded = true }
            )
            DropdownMenu(
                expanded         = endExpanded,
                onDismissRequest = { endExpanded = false },
                modifier         = Modifier
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
            ) {
                timeOptions.filter { it > start }.forEach { t ->
                    DropdownMenuItem(
                        text    = { Text(t, color = if (t == end) Primary else OnBackground, fontSize = 12.sp) },
                        onClick = { onEndChange(t); endExpanded = false },
                        modifier = Modifier.background(CardBg)
                    )
                }
            }
        }
    }
}

@Composable
private fun VenueSelector(
    rooms       : List<Room>,
    selectedRoom: Room?,
    expanded    : Boolean,
    onToggle    : () -> Unit,
    onSelect    : (Room) -> Unit,
    busyRoomIds : Set<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVar)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MeetingRoom, null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                selectedRoom?.name ?: "Select Room",
                color      = if (selectedRoom != null) OnBackground else OnSurface,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                modifier   = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint     = OnSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        if (expanded) {
            HorizontalDivider(color = Divider.copy(alpha = 0.5f))
            rooms.forEach { room ->
                val isBusy     = room.roomId in busyRoomIds
                val isSelected = room.roomId == selectedRoom?.roomId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isBusy) { onSelect(room) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            room.name,
                            color      = if (isBusy) OnSurface.copy(0.45f) else OnBackground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        )
                        Text(
                            "CAPACITY: ${room.capacity} STUDENTS",
                            color    = OnSurface.copy(if (isBusy) 0.3f else 0.6f),
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                    when {
                        isSelected -> Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(22.dp))
                        isBusy     -> Text("IN USE", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (room != rooms.last()) HorizontalDivider(color = Divider.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun ConflictStatusBanner(info: ConflictInfo) {
    val bgColor   = if (info.isAvailable) Color(0xFF1B3A2A) else ConflictBg
    val iconColor = if (info.isAvailable) Primary else ConflictAmber
    val icon      = if (info.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning
    val title     = if (info.isAvailable) "No Conflict Detected" else "${info.conflictCount} Conflict${if (info.conflictCount != 1) "s" else ""} Detected"
    val subtitle  = if (info.isAvailable)
        "The selected venue and time slot are available for this curriculum node."
    else
        info.details.take(2).joinToString("\n")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title,    color = OnBackground, fontWeight = FontWeight.Bold,  fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = OnBackground.copy(0.75f), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

data class ConflictInfo(
    val isAvailable  : Boolean,
    val conflictCount: Int,
    val details      : List<String>
)

private fun detectSlotConflict(
    newStart     : String,
    newEnd       : String,
    newRoom      : Room?,
    existingSlots: List<TimetableSlot>
): ConflictInfo {
    if (newRoom == null) return ConflictInfo(isAvailable = false, conflictCount = 0,
        details = listOf("Please select a room."))
    val conflicts = existingSlots.filter { slot ->
        slot.room.roomId == newRoom.roomId &&
                timesOverlap(newStart, newEnd, slot.start, slot.end)
    }.map { slot ->
        "${slot.room.name} occupied by ${slot.course.courseCode} (${slot.start}–${slot.end})"
    }
    return ConflictInfo(
        isAvailable   = conflicts.isEmpty(),
        conflictCount = conflicts.size,
        details       = conflicts
    )
}

private fun getBusyRoomIds(
    newStart     : String,
    newEnd       : String,
    existingSlots: List<TimetableSlot>,
    editingSlotId: String?
): Set<String> = existingSlots
    .filter { it.slotId != editingSlotId && timesOverlap(newStart, newEnd, it.start, it.end) }
    .map { it.room.roomId }
    .toSet()

private fun todayLabel(): String {
    val cal = java.util.Calendar.getInstance()
    return "%s %d, %d".format(
        listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[cal.get(java.util.Calendar.MONTH)],
        cal.get(java.util.Calendar.DAY_OF_MONTH),
        cal.get(java.util.Calendar.YEAR)
    )
}

private fun nextSevenDayLabels(): List<String> {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val cal    = java.util.Calendar.getInstance()
    return (0..6).map {
        val c = cal.clone() as java.util.Calendar
        c.add(java.util.Calendar.DAY_OF_YEAR, it)
        "%s %d, %d".format(months[c.get(java.util.Calendar.MONTH)],
            c.get(java.util.Calendar.DAY_OF_MONTH), c.get(java.util.Calendar.YEAR))
    }
}

private fun formatSlotDate(slot: TimetableSlot): String = todayLabel()

private fun parseDayFromDate(label: String): DayEnum {
    val days   = nextSevenDayLabels()
    val idx    = days.indexOf(label)
    val cal    = java.util.Calendar.getInstance()
    if (idx >= 0) cal.add(java.util.Calendar.DAY_OF_YEAR, idx)
    return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY    -> DayEnum.MONDAY
        java.util.Calendar.TUESDAY   -> DayEnum.TUESDAY
        java.util.Calendar.WEDNESDAY -> DayEnum.WEDNESDAY
        java.util.Calendar.THURSDAY  -> DayEnum.THURSDAY
        java.util.Calendar.FRIDAY    -> DayEnum.FRIDAY
        java.util.Calendar.SATURDAY  -> DayEnum.SATURDAY
        else                         -> DayEnum.SUNDAY
    }
}

private fun generateTimeOptions(): List<String> {
    val options = mutableListOf<String>()
    for (h in 7..21) {
        for (m in listOf(0, 30)) {
            options.add("%02d:%02d".format(h, m))
        }
    }
    return options
}
