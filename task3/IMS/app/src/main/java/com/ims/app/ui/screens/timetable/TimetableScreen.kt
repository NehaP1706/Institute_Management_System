package com.ims.app.ui.screens.timetable

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.DayEnum
import com.ims.app.data.model.PersonalTimetableSlot
import com.ims.app.data.model.TimetableSlot
import com.ims.app.ui.IMSViewModel

private val BgDeep        = Color(0xFF0A1628)
private val BgCard        = Color(0xFF0F2137)
private val TealPrimary   = Color(0xFF1ABC9C)
private val TealCard      = Color(0xFF0E7C6A)
private val TealCardLight = Color(0xFF15A889)
private val TextWhite     = Color(0xFFFFFFFF)
private val TextMuted     = Color(0xFF7A9BB5)
private val TextSub       = Color(0xFFB0C4D8)
private val AvatarBg      = Color(0xFF1E3A52)
private val OngoingAmber  = Color(0xFFFFB300)
private val ConflictBg    = Color(0xFF3D1A1A)
private val ConflictText  = Color(0xFFFF6B6B)
private val NavBg         = Color(0xFF0D1E30)
private val InactiveNavBg = Color(0xFF0A1628)

@Composable
fun TimetableScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedDay    by remember { mutableStateOf(DayEnum.WEDNESDAY) }
    var showAddScreen   by remember { mutableStateOf(false) }
    var showDragPanel   by remember { mutableStateOf(false) }

    // ── Student drag-and-drop overlay ────────────────────────────────────────
    if (showDragPanel) {
        StudentDragPanel(
            onDismiss  = { showDragPanel = false },
            onDropped  = {
                showDragPanel  = false
                showAddScreen  = true          // opens AddPersonalSlotScreen
            }
        )
        return
    }

    if (showAddScreen) {
        AddPersonalSlotScreen(
            onBack = { showAddScreen = false },
            onSave = { slot ->
                viewModel.addPersonalSlot(slot)
                showAddScreen = false
            }
        )
        return
    }

    val slots = viewModel.getTimetableForSemester(viewModel.selectedSemester)
        .filter { it.day == selectedDay }

    val personalSlots = viewModel.personalSlots

    val hasConflict = slots.zipWithNext().any { (a, b) ->
        a.end > b.start
    }

    Scaffold(
        topBar = {
            TimetableTopBar(onNavigate = onNavigate)
        },
        bottomBar = {
            TimetableBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Drag-and-drop FAB
                FloatingActionButton(
                    onClick        = { showDragPanel = true },
                    containerColor = Color(0xFF0E7C6A),
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.DragIndicator, contentDescription = "Drag & Drop slot")
                }
                // Regular add FAB
                FloatingActionButton(
                    onClick        = { showAddScreen = true },
                    containerColor = TealPrimary,
                    contentColor   = Color.Black,
                    shape          = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add personal slot")
                }
            }
        },
        containerColor = BgDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DaySelectorRow(
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it }
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = "SEMESTER 4 · BATCH 2023",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val slotsWithFlags = slots.mapIndexed { index, slot ->
                    val isOngoing = index == 1
                    val isPast = index == slots.lastIndex && slots.size > 2
                    Triple(slot, isOngoing, isPast)
                }

                slotsWithFlags.forEachIndexed { index, (slot, isOngoing, isPast) ->
                    item(key = slot.hashCode()) {
                        TimetableSlotCard(
                            slot = slot,
                            isOngoing = isOngoing,
                            isPast = isPast
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    if (index == 0 && hasConflict) {
                        item(key = "conflict_$index") {
                            ConflictBanner()
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                if (personalSlots.isNotEmpty()) {
                    item(key = "personal_header") {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text          = "PERSONAL SLOTS",
                            color         = TextMuted,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(personalSlots, key = { it.id }) { pSlot ->
                        PersonalSlotCard(slot = pSlot)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (slots.isNotEmpty() || personalSlots.isNotEmpty()) {
                    item(key = "no_more") {
                        Spacer(Modifier.height(20.dp))
                        NoMoreClassesBlock()
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (slots.isEmpty() && personalSlots.isEmpty()) {
                    item(key = "empty") {
                        NoMoreClassesBlock(emptyDay = true, dayName = selectedDay.name)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableTopBar(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDeep)
            .padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 16.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onNavigate("back") },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextWhite
            )
        }
        Text(
            text = "My Timetable",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun DaySelectorRow(
    selectedDay: DayEnum,
    onDaySelected: (DayEnum) -> Unit
) {
    val days = listOf(
        DayEnum.MONDAY, DayEnum.TUESDAY, DayEnum.WEDNESDAY,
        DayEnum.THURSDAY, DayEnum.FRIDAY, DayEnum.SATURDAY
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(days) { day ->
            val isSelected = day == selectedDay
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) TealPrimary else Color(0xFF132335))
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.take(3).lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color = if (isSelected) TextWhite else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    isOngoing: Boolean = false,
    isPast: Boolean = false
) {
    val cardBackground = if (isPast)
        Color(0xFF132233)
    else
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF0E7C6A), Color(0xFF15A889))
        )

    val courseTextColor   = if (isPast) TextSub   else TextWhite
    val codeTextColor     = if (isPast) TextMuted  else Color(0xFFB2EFE4)
    val timeTextColor     = if (isPast) TextMuted  else TextWhite
    val roomTextColor     = if (isPast) TextMuted  else Color(0xFFB2EFE4)
    val avatarBgColor     = if (isPast) Color(0xFF1C3347) else Color(0xFF0A5446)
    val avatarTextColor   = if (isPast) TextMuted  else TealPrimary

    val initials = slot.course.title
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .take(2)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .then(
                    if (isPast)
                        Modifier.background(Color(0xFF132233))
                    else
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0E7C6A), Color(0xFF15A889))
                            )
                        )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.width(60.dp)
                ) {
                    Text(
                        text = slot.start,
                        color = timeTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "·",
                        color = if (isPast) TextMuted else Color(0xFFB2EFE4),
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = slot.end,
                        color = if (isPast) TextMuted else Color(0xFFB2EFE4),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Room ${slot.room.name}",
                            color = roomTextColor,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = slot.course.title,
                        color = courseTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = slot.course.courseCode,
                        color = codeTextColor,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = avatarTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isOngoing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = (-1).dp)
                    .clip(RoundedCornerShape(bottomStart = 10.dp, topEnd = 20.dp))
                    .background(OngoingAmber)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "ONGOING",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun PersonalSlotCard(slot: PersonalTimetableSlot) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF132233))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // Time column
            Column(
                horizontalAlignment = Alignment.Start,
                modifier            = Modifier.width(60.dp)
            ) {
                Text(slot.startTime, color = TextSub,  fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("·",           color = TextMuted, fontSize = 10.sp)
                Spacer(Modifier.height(2.dp))
                Text(slot.endTime,  color = TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Personal",
                    color    = TextMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    slot.title,
                    color      = TextSub,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!slot.reason.isNullOrBlank()) {
                    Text(slot.reason, color = TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E3A52))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("MY SLOT", color = TealPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun ConflictBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(ConflictBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ConflictText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Schedule Conflict Detected",
                color = ConflictText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NoMoreClassesBlock(
    emptyDay: Boolean = false,
    dayName: String = ""
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF132233)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (emptyDay) "No classes on $dayName"
            else "No more classes scheduled for today.",
            color = TextMuted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TimetableBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBg)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "HOME",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(22.dp),
                        tint = if (currentRoute == "dashboard") TealPrimary else TextMuted
                    )
                },
                isSelected = currentRoute == "dashboard",
                onClick = { onNavigate("dashboard") }
            )

            BottomNavItem(
                label = "ATTENDANCE",
                icon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Attendance",
                        modifier = Modifier.size(22.dp),
                        tint = if (currentRoute == "attendance") TealPrimary else TextMuted
                    )
                },
                isSelected = currentRoute == "attendance",
                onClick = { onNavigate("attendance") }
            )

            val isTimetableActive = currentRoute == "timetable"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isTimetableActive) TealPrimary else Color.Transparent)
                    .clickable { onNavigate("timetable") }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Timetable",
                        modifier = Modifier.size(22.dp),
                        tint = if (isTimetableActive) Color.White else TextMuted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "TIMETABLE",
                        color = if (isTimetableActive) Color.White else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        icon()
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) TealPrimary else TextMuted,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalSlotScreen(
    onBack: () -> Unit,
    onSave: (PersonalTimetableSlot) -> Unit
) {
    var title      by remember { mutableStateOf("") }
    var startTime  by remember { mutableStateOf("09:00") }
    var endTime    by remember { mutableStateOf("10:00") }
    var reason     by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    val sectionLabelColor = TextMuted
    val fieldBg           = Color(0xFF132233)
    val fieldText         = TextWhite
    val fieldHint         = TextMuted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Timetable Slot",
                        color      = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDeep)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgDeep)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                        } else {
                            onSave(
                                PersonalTimetableSlot(
                                    id        = "ps_${System.currentTimeMillis()}",
                                    title     = title.trim(),
                                    startTime = startTime,
                                    endTime   = endTime,
                                    reason    = reason.trim().ifBlank { null }
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(
                        "Confirm Schedule Slot",
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint     = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        containerColor = BgDeep
    ) { padding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                PersonalSlotSectionLabel("TITLE", sectionLabelColor)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    placeholder   = { Text("e.g. Study Group – Algorithms", color = fieldHint) },
                    leadingIcon   = {
                        Icon(Icons.Default.Title, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    },
                    isError       = titleError,
                    supportingText = if (titleError) {{ Text("Title is required", color = Color(0xFFEF5350)) }} else null,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    colors        = personalSlotFieldColors(fieldBg, fieldText)
                )
            }

            item {
                PersonalSlotSectionLabel("TIME SLOT", sectionLabelColor)
                Spacer(Modifier.height(8.dp))
                PersonalTimeSlotRow(
                    start         = startTime,
                    end           = endTime,
                    onStartChange = { startTime = it },
                    onEndChange   = { endTime   = it },
                    fieldBg       = fieldBg,
                    fieldText     = fieldText
                )
            }

            item {
                PersonalSlotSectionLabel("REASON  (OPTIONAL)", sectionLabelColor)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = reason,
                    onValueChange = { reason = it },
                    placeholder   = { Text("e.g. Exam prep, club meeting…", color = fieldHint) },
                    leadingIcon   = {
                        Icon(Icons.Default.Notes, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines  = 4,
                    shape     = RoundedCornerShape(14.dp),
                    colors    = personalSlotFieldColors(fieldBg, fieldText)
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PersonalSlotSectionLabel(text: String, color: Color) {
    Text(
        text          = text,
        color         = color,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.4.sp
    )
}

@Composable
private fun PersonalTimeSlotRow(
    start: String,
    end: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    fieldBg: Color,
    fieldText: Color
) {
    val timeOptions = personalTimeOptions()
    var startExpanded by remember { mutableStateOf(false) }
    var endExpanded   by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("FROM", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fieldBg)
                        .clickable { startExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(start, color = fieldText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded         = startExpanded,
                    onDismissRequest = { startExpanded = false },
                    modifier         = Modifier
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F2137))
                ) {
                    timeOptions.forEach { t ->
                        DropdownMenuItem(
                            text    = { Text(t, color = if (t == start) TealPrimary else TextWhite, fontSize = 13.sp) },
                            onClick = { onStartChange(t); startExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F2137))
                        )
                    }
                }
            }
        }

        Column(Modifier.weight(1f)) {
            Text("TO", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fieldBg)
                        .clickable { endExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(end, color = fieldText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded         = endExpanded,
                    onDismissRequest = { endExpanded = false },
                    modifier         = Modifier
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F2137))
                ) {
                    timeOptions.filter { it > start }.forEach { t ->
                        DropdownMenuItem(
                            text    = { Text(t, color = if (t == end) TealPrimary else TextWhite, fontSize = 13.sp) },
                            onClick = { onEndChange(t); endExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F2137))
                        )
                    }
                }
            }
        }
    }
}

private fun personalTimeOptions(): List<String> {
    val list = mutableListOf<String>()
    for (h in 7..21) for (m in listOf(0, 30)) list.add("%02d:%02d".format(h, m))
    return list
}

@Composable
private fun personalSlotFieldColors(bg: Color, text: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = TealPrimary,
        unfocusedBorderColor    = Color.Transparent,
        focusedContainerColor   = bg,
        unfocusedContainerColor = bg,
        focusedTextColor        = text,
        unfocusedTextColor      = text,
        cursorColor             = TealPrimary,
        focusedLabelColor       = TealPrimary
    )

private data class StudentDragState(
    val isDragging: Boolean = false,
    val offsetX: Float      = 0f,   // absolute finger X in root coords
    val offsetY: Float      = 0f    // absolute finger Y in root coords
)

@Composable
fun StudentDragPanel(
    onDismiss: () -> Unit,
    onDropped: () -> Unit
) {
    var dragState by remember { mutableStateOf(StudentDragState()) }
    var cardPos   by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (!dragState.isDragging) onDismiss() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BgCard)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A52))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint     = TealPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Drag to Add a Personal Slot",
                    color      = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }
            Text(
                "Long-press the card below and drag it upward — release to fill in the details",
                color    = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))

            val alpha by animateFloatAsState(
                targetValue   = if (dragState.isDragging) 0.35f else 1f,
                animationSpec = tween(200),
                label         = "card_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .onGloballyPositioned { coords ->
                        cardPos = coords.positionInRoot()
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                // offset is relative to the touch target; add card's
                                // root position to get absolute screen coordinates
                                dragState = StudentDragState(
                                    isDragging = true,
                                    offsetX    = cardPos.x + offset.x,
                                    offsetY    = cardPos.y + offset.y
                                )
                            },
                            onDrag = { _, dragAmount ->
                                // dragAmount is a per-frame delta — accumulate
                                dragState = dragState.copy(
                                    offsetX = dragState.offsetX + dragAmount.x,
                                    offsetY = dragState.offsetY + dragAmount.y
                                )
                            },
                            onDragEnd    = { onDropped() },
                            onDragCancel = { dragState = StudentDragState() }
                        )
                    }
            ) {
                EmptyPersonalSlotCard()
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "— or tap the card to open the form directly —",
                color     = TextMuted,
                fontSize  = 11.sp,
                modifier  = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onDropped() }
            )

            Spacer(Modifier.height(24.dp))
        }

        if (dragState.isDragging) {
            // Ghost card centered on the finger — use absolute root coords
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (dragState.offsetX - 150.dp.toPx()).roundToInt(),
                            (dragState.offsetY - 30.dp.toPx()).roundToInt()
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(TealCard.copy(alpha = 0.92f), TealCardLight.copy(alpha = 0.92f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DragIndicator, null, tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("New Personal Slot", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Drop to add details", color = Color.White.copy(0.65f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPersonalSlotCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(TealCard, TealCardLight)
                )
            )
            .border(
                width = 2.dp,
                color = TealPrimary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment  = Alignment.CenterVertically,
            modifier           = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = null,
                tint     = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "New Personal Slot",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Long-press & drag to schedule",
                    color    = Color.White.copy(0.65f),
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}