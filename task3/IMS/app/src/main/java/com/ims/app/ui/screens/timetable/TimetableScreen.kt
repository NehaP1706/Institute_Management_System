package com.ims.app.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.app.data.model.DayEnum
import com.ims.app.data.model.TimetableSlot
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*

/** Matches "My Timetable" screen in Figma */
@Composable
fun TimetableScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedDay by remember { mutableStateOf(DayEnum.MONDAY) }
    val slots = viewModel.getTimetableForSemester(viewModel.selectedSemester)
        .filter { it.day == selectedDay }

    Scaffold(
        topBar = {
            HeaderTopBar(
                title    = "My Timetable",
                userName = viewModel.getCurrentUserName(),
                userRole = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin      = false,
                onNavigate   = onNavigate
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Semester selector
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Semester", color = OnSurface, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Sem 5","Sem 6").forEach { sem ->
                        val sel = viewModel.selectedSemester == sem
                        FilterChip(
                            selected = sel,
                            onClick  = { viewModel.selectedSemester = sem },
                            label    = { Text(sem, fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor     = OnPrimary,
                                containerColor         = SurfaceVar,
                                labelColor             = OnSurface
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Day selector row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DayEnum.values().toList()) { day ->
                    val sel = day == selectedDay
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) Primary else SurfaceVar)
                            .clickable { selectedDay = day }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            day.name.take(3),
                            color      = if (sel) OnPrimary else OnSurface,
                            fontSize   = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (slots.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No classes on ${selectedDay.name}", color = OnSurface)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(slots) { slot -> TimetableSlotCard(slot) }
                }
            }
        }
    }
}

@Composable
fun TimetableSlotCard(slot: TimetableSlot) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.width(56.dp)
            ) {
                Text(slot.start, color = Primary,   fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("|",        color = Divider,   fontSize   = 10.sp)
                Text(slot.end,   color = OnSurface, fontSize   = 11.sp)
            }

            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .width(2.dp)
                    .height(50.dp)
                    .background(Primary.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(1.dp))
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(slot.course.title, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(slot.course.courseCode, color = OnSurface, fontSize = 12.sp)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Room, null, tint = Primary, modifier = Modifier.size(12.dp))
                    Text(slot.room.name, color = OnSurface, fontSize = 11.sp)
                }
            }

            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryDark.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(slot.course.type.name, color = Primary, fontSize = 10.sp)
            }
        }
    }
}