package com.ims.app.ui.screens.timetable

import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import com.ims.app.data.model.*
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*

/** Matches "Admin Timeta..." and "Timetable Sl..." (Add Timetable Slot) screens in Figma */
@Composable
fun AdminTimetableScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedDay   by remember { mutableStateOf(DayEnum.MONDAY) }
    var showAddDialog by remember { mutableStateOf(false) }

    val slots = viewModel.getTimetableForSemester(viewModel.selectedSemester)

    Scaffold(
        topBar = {
            HeaderTopBar(
                title    = "Timetable Management",
                userName = viewModel.getCurrentUserName(),
                userRole = viewModel.getCurrentUserRole(),
                onNavigate = onNavigate
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                isAdmin      = true,
                onNavigate   = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { showAddDialog = true },
                containerColor   = Primary,
                contentColor     = OnPrimary
            ) { Icon(Icons.Default.Add, "Add Slot") }
        },
        containerColor = Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Semester switch
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Sem 5","Sem 6").forEach { sem ->
                    val sel = viewModel.selectedSemester == sem
                    FilterChip(
                        selected = sel,
                        onClick  = { viewModel.selectedSemester = sem },
                        label    = { Text(sem) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor     = OnPrimary,
                            containerColor         = SurfaceVar,
                            labelColor             = OnSurface
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Day tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DayEnum.values().toList()) { day ->
                    val sel = day == selectedDay
                    FilterChip(
                        selected = sel,
                        onClick  = { selectedDay = day },
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

            Spacer(Modifier.height(12.dp))
            SectionHeader("${selectedDay.name} – ${viewModel.selectedSemester}")

            val daySlots = slots.filter { it.day == selectedDay }

            if (daySlots.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No slots. Tap + to add.", color = OnSurface)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(daySlots) { slot ->
                        AdminTimetableSlotCard(
                            slot      = slot,
                            onDelete  = { /* stub: removeSlot */ },
                            onEdit    = { showAddDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimetableSlotDialog(
            onDismiss = { showAddDialog = false },
            onSave    = { slot ->
                viewModel.addTimetableSlot(slot)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AdminTimetableSlotCard(
    slot: TimetableSlot,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.width(56.dp)
            ) {
                Text(slot.start, color = Primary,   fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("–",        color = Divider,   fontSize   = 10.sp)
                Text(slot.end,   color = OnSurface, fontSize   = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(slot.course.title,     color = OnBackground, fontWeight = FontWeight.SemiBold)
                Text(slot.course.courseCode,color = OnSurface,    fontSize   = 12.sp)
                Text(slot.room.name,        color = Primary,      fontSize   = 11.sp)
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Default.Edit,   null, tint = Primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

/** Matches "Timetable Sl..." (Add Timetable Slot) screen */
@Composable
private fun AddTimetableSlotDialog(
    onDismiss: () -> Unit,
    onSave:    (TimetableSlot) -> Unit
) {
    var selectedCourse by remember { mutableStateOf(StubRepository.courses.first()) }
    var selectedDay    by remember { mutableStateOf(DayEnum.MONDAY) }
    var startTime      by remember { mutableStateOf("09:00") }
    var endTime        by remember { mutableStateOf("10:00") }
    var conflictResult by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape  = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Timetable Slot", color = Primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                // Course picker
                Text("Select Course", color = OnSurface, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(StubRepository.courses) { course ->
                        val sel = course.courseId == selectedCourse.courseId
                        FilterChip(
                            selected = sel,
                            onClick  = { selectedCourse = course },
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
                            onClick  = { selectedDay = day },
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
                        onValueChange = { startTime = it },
                        label         = { Text("Start") },
                        modifier      = Modifier.weight(1f),
                        colors        = imsFieldColors()
                    )
                    OutlinedTextField(
                        value         = endTime,
                        onValueChange = { endTime = it },
                        label         = { Text("End") },
                        modifier      = Modifier.weight(1f),
                        colors        = imsFieldColors()
                    )
                }

                // Conflict check (calls checkConflict from TimetableSlot UML)
                if (conflictResult != null) {
                    Text(conflictResult!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            val newSlot = TimetableSlot(
                                slotId   = "new_${System.currentTimeMillis()}",
                                course   = selectedCourse,
                                day      = selectedDay,
                                start    = startTime,
                                end      = endTime,
                                room     = Room("rm1","LH-101",120, RoomType.Room100C,"Lecture Block"),
                                semester = "Sem 5"
                            )
                            // checkConflict from UML
                            val hasConflict = newSlot.checkConflict(newSlot)
                            if (hasConflict) {
                                conflictResult = "Conflict detected! Choose another time."
                            } else {
                                onSave(newSlot)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Save Slot", color = OnPrimary) }
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