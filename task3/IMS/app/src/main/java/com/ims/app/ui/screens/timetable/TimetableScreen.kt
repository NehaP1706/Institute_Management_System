package com.ims.app.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.DayEnum
import com.ims.app.data.model.TimetableSlot
import com.ims.app.ui.IMSViewModel

// ── Design tokens (matching Figma exactly) ────────────────────────────────────
private val BgDeep        = Color(0xFF0A1628)   // darkest navy background
private val BgCard        = Color(0xFF0F2137)   // slightly lighter card bg (inactive)
private val TealPrimary   = Color(0xFF1ABC9C)   // primary teal accent
private val TealCard      = Color(0xFF0E7C6A)   // teal card fill (active classes)
private val TealCardLight = Color(0xFF15A889)   // lighter teal for gradient
private val TextWhite     = Color(0xFFFFFFFF)
private val TextMuted     = Color(0xFF7A9BB5)   // muted blue-grey text
private val TextSub       = Color(0xFFB0C4D8)   // secondary text
private val AvatarBg      = Color(0xFF1E3A52)   // avatar circle bg
private val OngoingAmber  = Color(0xFFFFB300)   // ONGOING badge
private val ConflictBg    = Color(0xFF3D1A1A)   // conflict banner bg
private val ConflictText  = Color(0xFFFF6B6B)   // conflict text/icon
private val NavBg         = Color(0xFF0D1E30)   // bottom nav background
private val InactiveNavBg = Color(0xFF0A1628)

@Composable
fun TimetableScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // Default to Wednesday to match Figma, but allow selection
    var selectedDay by remember { mutableStateOf(DayEnum.WEDNESDAY) }

    val slots = viewModel.getTimetableForSemester(viewModel.selectedSemester)
        .filter { it.day == selectedDay }

    // Detect conflicts: overlapping time slots on same day
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
        containerColor = BgDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Day selector pills ────────────────────────────────────────────
            DaySelectorRow(
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it }
            )

            // ── Semester / Batch subtitle ─────────────────────────────────────
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

            // ── Slot list ─────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val slotsWithFlags = slots.mapIndexed { index, slot ->
                    // Mark a slot as "ongoing" if it's the second slot in Figma (index == 1)
                    // In production, compare against real current time
                    val isOngoing = index == 1
                    // Mark as past if it's the last slot (index == slots.lastIndex)
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

                    // Conflict banner after first card if conflict exists
                    if (index == 0 && hasConflict) {
                        item(key = "conflict_$index") {
                            ConflictBanner()
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                if (slots.isNotEmpty()) {
                    item(key = "no_more") {
                        Spacer(Modifier.height(20.dp))
                        NoMoreClassesBlock()
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (slots.isEmpty()) {
                    item(key = "empty") {
                        NoMoreClassesBlock(emptyDay = true, dayName = selectedDay.name)
                    }
                }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

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
        // Back arrow left-aligned
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
        // Centered title
        Text(
            text = "My Timetable",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// ── Day selector ──────────────────────────────────────────────────────────────

@Composable
private fun DaySelectorRow(
    selectedDay: DayEnum,
    onDaySelected: (DayEnum) -> Unit
) {
    // Figma shows Mon Tue Wed Thu Fri Sat (6 days, no Sunday)
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

// ── Slot card ─────────────────────────────────────────────────────────────────

@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    isOngoing: Boolean = false,
    isPast: Boolean = false
) {
    // Active cards: teal gradient fill; past cards: dark muted fill
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

    // Faculty initials from course (use first two words of title as initials)
    val initials = slot.course.title
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .take(2)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Card with teal or muted background
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
                // Time column
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

                // Course info
                Column(modifier = Modifier.weight(1f)) {
                    // Room line (small, above title – matches Figma layout)
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

                // Avatar circle with initials
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

        // ONGOING badge — top-right corner, overlapping card edge
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

// ── Conflict banner ───────────────────────────────────────────────────────────

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

// ── "No more classes" block ───────────────────────────────────────────────────

@Composable
private fun NoMoreClassesBlock(
    emptyDay: Boolean = false,
    dayName: String = ""
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rounded square icon container (like Figma's calendar-x block)
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

// ── Bottom navigation ─────────────────────────────────────────────────────────

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
            // HOME
            BottomNavItem(
                label = "HOME",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(22.dp),
                        tint = if (currentRoute == "home") TealPrimary else TextMuted
                    )
                },
                isSelected = currentRoute == "home",
                onClick = { onNavigate("home") }
            )

            // ATTENDANCE
            BottomNavItem(
                label = "ATTENDANCE",
                icon = {
                    // Checkmark / attendance icon
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

            // TIMETABLE — active state shows teal pill bg
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