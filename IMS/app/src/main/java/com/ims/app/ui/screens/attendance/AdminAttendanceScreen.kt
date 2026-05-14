package com.ims.app.ui.screens.attendance

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ims.app.data.model.*
import com.ims.app.data.repository.StubRepository
import com.ims.app.ui.IMSViewModel
import com.ims.app.ui.components.*
import com.ims.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private val TealChip      = Color(0xFF00A896)
private val ChipBg        = Color(0xFF0D2233)
private val TableHeaderBg = Color(0xFF0A1929)
private val RowBg         = Color(0xFF0D2233)
private val PresentColor  = Color(0xFF00BFA5)
private val AbsentColor   = Color(0xFFEF5350)
private val LeaveColor    = Color(0xFFFFA726)
private val UnmarkedColor = Color(0xFF607D8B)
private val StatCircleBg  = Color(0xFF0D2233)
private val RemarksBg     = Color(0xFF111E2B)
private val PanelBg       = Color(0xFF0A1929)

private fun AttendanceStatus.displayLabel(): String = when (this) {
    AttendanceStatus.PRESENT        -> "PRESENT"
    AttendanceStatus.ABSENT         -> "ABSENT"
    AttendanceStatus.APPROVED_LEAVE -> "LEAVE"
    AttendanceStatus.UNMARKED       -> "UNMARKED"
}

private fun AttendanceStatus.statusColor(): Color = when (this) {
    AttendanceStatus.PRESENT        -> PresentColor
    AttendanceStatus.ABSENT         -> AbsentColor
    AttendanceStatus.APPROVED_LEAVE -> LeaveColor
    AttendanceStatus.UNMARKED       -> UnmarkedColor
}

private fun exportAttendancePdf(
    context: Context,
    courseName: String,
    courseCode: String,
    date: String,
    batch: String,
    students: List<Student>,
    statusMap: Map<String, AttendanceStatus>,
    courseId: String
) {
    val pdfDoc  = PdfDocument()
    val page    = pdfDoc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas: Canvas = page.canvas

    val boldPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
        color    = android.graphics.Color.BLACK
    }
    val subPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        color    = android.graphics.Color.DKGRAY
    }
    val bodyPaint = Paint().apply {
        textSize = 11f
        color    = android.graphics.Color.BLACK
    }
    val linePaint = Paint().apply {
        color       = android.graphics.Color.LTGRAY
        strokeWidth = 1f
    }

    canvas.drawText("Attendance Report",           40f, 50f,  boldPaint)
    canvas.drawText("Course: $courseName ($courseCode)", 40f, 75f,  subPaint)
    canvas.drawText("Date: $date    Batch: $batch", 40f, 95f,  subPaint)
    canvas.drawLine(40f, 105f, 555f, 105f, linePaint)

    canvas.drawText("Student Name", 40f,  125f, subPaint)
    canvas.drawText("ID",           260f, 125f, subPaint)
    canvas.drawText("Status",       390f, 125f, subPaint)
    canvas.drawLine(40f, 130f, 555f, 130f, linePaint)

    var y = 148f
    students.forEach { student ->
        val key    = student.studentId + "_" + courseId
        val status = statusMap[key] ?: AttendanceStatus.UNMARKED
        canvas.drawText(StubRepository.getUserDisplayName(student.user), 40f,  y, bodyPaint)
        canvas.drawText(student.studentId,                               260f, y, bodyPaint)
        canvas.drawText(status.displayLabel(),                           390f, y, bodyPaint)
        y += 22f
        canvas.drawLine(40f, y - 6f, 555f, y - 6f, linePaint)
    }

    val presentCount = students.count { statusMap[it.studentId + "_" + courseId] == AttendanceStatus.PRESENT }
    val absentCount  = students.count { statusMap[it.studentId + "_" + courseId] == AttendanceStatus.ABSENT }
    val leaveCount   = students.count { statusMap[it.studentId + "_" + courseId] == AttendanceStatus.APPROVED_LEAVE }
    y += 16f
    canvas.drawText(
        "Total: ${students.size}   Present: $presentCount   Absent: $absentCount   Leave: $leaveCount",
        40f, y, subPaint
    )

    pdfDoc.finishPage(page)

    val safeDate = date.replace("/", "-")
    val dir      = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    val file     = File(dir, "attendance_${courseCode}_$safeDate.pdf")
    pdfDoc.writeTo(FileOutputStream(file))
    pdfDoc.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Open PDF"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceScreen(
    viewModel: IMSViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    var selectedCourse  by remember { mutableStateOf(StubRepository.courses.first()) }
    var showCourseSheet by remember { mutableStateOf(false) }
    var pendingCourse   by remember { mutableStateOf(selectedCourse) }
    var sheetSearch     by remember { mutableStateOf("") }

    val batchOptions      = listOf("BATCH A", "BATCH B", "BATCH C")
    val freqOptions       = listOf("DAILY", "MONTHLY")
    var selectedBatch     by remember { mutableStateOf(batchOptions.first()) }
    var selectedFreq      by remember { mutableStateOf(freqOptions.first()) }
    var showBatchDropdown by remember { mutableStateOf(false) }
    var showFreqDropdown  by remember { mutableStateOf(false) }

    val todayDayEnum = remember {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        when (dow) {
            Calendar.MONDAY    -> DayEnum.MONDAY
            Calendar.TUESDAY   -> DayEnum.TUESDAY
            Calendar.WEDNESDAY -> DayEnum.WEDNESDAY
            Calendar.THURSDAY  -> DayEnum.THURSDAY
            Calendar.FRIDAY    -> DayEnum.FRIDAY
            Calendar.SATURDAY  -> DayEnum.SATURDAY
            else               -> DayEnum.SUNDAY
        }
    }
    var selectedDay by remember { mutableStateOf(todayDayEnum) }

    var selectedDate   by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter  = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateLabel      = dateFormatter.format(selectedDate.time)
    var showCsvDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val snapshot = selectedDate
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                showDatePicker = false
            },
            snapshot.get(Calendar.YEAR),
            snapshot.get(Calendar.MONTH),
            snapshot.get(Calendar.DAY_OF_MONTH)
        ).also { dlg ->
            dlg.setOnDismissListener { showDatePicker = false }
            dlg.show()
        }
        showDatePicker = false
    }

    val dateKeyFmt = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val statusMap  = remember { mutableStateMapOf<String, AttendanceStatus>() }
    val remarksMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(selectedCourse.courseId, selectedDate.timeInMillis) {
        statusMap.clear()
        remarksMap.clear()
        val selectedDateKey = dateKeyFmt.format(selectedDate.time)
        StubRepository.attendanceRecords
            .filter { r ->
                r.course.courseId == selectedCourse.courseId &&
                        dateKeyFmt.format(r.date) == selectedDateKey
            }
            .forEach { r ->
                val key = r.student.studentId + "_" + r.course.courseId
                statusMap[key] = r.status
                if (r.remarks.isNotBlank()) remarksMap[key] = r.remarks
            }
    }

    var detailStudent: Student? by remember { mutableStateOf(null) }
    var detailRemark            by remember { mutableStateOf("") }
    var saved                   by remember { mutableStateOf(false) }

    if (selectedFreq == "MONTHLY") {
        AdminMonthlyAttendanceScreen(
            viewModel       = viewModel,
            currentRoute    = currentRoute,
            onNavigate      = onNavigate,
            onSwitchToDaily = { selectedFreq = "DAILY" }
        )
        return
    }

    val studentList: List<Student> = remember(selectedCourse, selectedBatch, selectedDate.timeInMillis) {
        val selectedDateKey = dateKeyFmt.format(selectedDate.time)

        val allForDate = StubRepository.attendanceRecords
            .filter { record ->
                record.course.courseId == selectedCourse.courseId &&
                        dateKeyFmt.format(record.date) == selectedDateKey
            }
            .map { it.student }
            .distinctBy { it.studentId }
            .ifEmpty {
                val todayKey = dateKeyFmt.format(Date())
                if (selectedDateKey == todayKey) listOf(StubRepository.sampleStudent)
                else emptyList()
            }

        when (selectedBatch) {
            "BATCH A" -> allForDate
            else      -> emptyList()
        }
    }

    val total   = studentList.size
    val present = studentList.count { s: Student ->
        statusMap[s.studentId + "_" + selectedCourse.courseId] == AttendanceStatus.PRESENT
    }
    val absent = studentList.count { s: Student ->
        statusMap[s.studentId + "_" + selectedCourse.courseId] == AttendanceStatus.ABSENT
    }
    val leave = studentList.count { s: Student ->
        statusMap[s.studentId + "_" + selectedCourse.courseId] == AttendanceStatus.APPROVED_LEAVE
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showCourseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCourseSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF111E2B),
            dragHandle       = null
        ) {
            CoursePickerSheetContent(
                courses = StubRepository.courses.filter {
                    sheetSearch.isBlank() ||
                            it.title.contains(sheetSearch, ignoreCase = true) ||
                            it.courseCode.contains(sheetSearch, ignoreCase = true)
                },
                searchQuery    = sheetSearch,
                onSearchChange = { sheetSearch = it },
                pendingCourse  = pendingCourse,
                onSelect       = { c -> pendingCourse = c },
                onBack         = { showCourseSheet = false },
                onClose        = { showCourseSheet = false; sheetSearch = "" },
                onApply        = {
                    selectedCourse  = pendingCourse
                    saved           = false
                    showCourseSheet = false
                    sheetSearch     = ""
                }
            )
        }
    }

    fun markAndPersist(student: Student, status: AttendanceStatus) {
        val key = student.studentId + "_" + selectedCourse.courseId
        statusMap[key] = status
        viewModel.markAttendance(
            AttendanceRecord(
                attendanceId = key,
                student      = student,
                course       = selectedCourse,
                date         = selectedDate.time,
                status       = status,
                remarks      = remarksMap[key] ?: ""   
            )
        )
    }

    if (showCsvDialog) {
        CsvUploadDialog(
            courses        = StubRepository.courses,
            selectedCourse = selectedCourse,
            onDismiss      = { showCsvDialog = false },
            onConfirm      = { studentName, course ->
                val matchedStudent = StubRepository.sampleStudent.takeIf {
                    StubRepository.getUserDisplayName(it.user)
                        .contains(studentName.trim(), ignoreCase = true)
                } ?: StubRepository.sampleStudent

                val key = matchedStudent.studentId + "_" + course.courseId
                statusMap[key] = AttendanceStatus.PRESENT
                viewModel.markAttendance(
                    AttendanceRecord(
                        attendanceId = key,
                        student      = matchedStudent,
                        course       = course,
                        date         = selectedDate.time,
                        status       = AttendanceStatus.PRESENT,
                        remarks      = "Via CSV upload"
                    )
                )
                showCsvDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            AttendanceTopBar(
                onNavigate = onNavigate,
                onExport   = {
                    exportAttendancePdf(
                        context    = context,
                        courseName = selectedCourse.title,
                        courseCode = selectedCourse.courseCode,
                        date       = dateLabel,
                        batch      = selectedBatch,
                        students   = studentList,
                        statusMap  = statusMap,
                        courseId   = selectedCourse.courseId
                    )
                }
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, isAdmin = true, onNavigate = onNavigate)
        },
        containerColor = Background
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { detailStudent = null }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealChip)
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            pendingCourse   = selectedCourse
                            sheetSearch     = ""
                            showCourseSheet = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "${selectedCourse.title} – ${selectedCourse.courseCode}",
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Box {
                        FilterPill(
                            label   = selectedBatch,
                            icon    = Icons.Default.People,
                            onClick = { showBatchDropdown = true }
                        )
                        DropdownMenu(
                            expanded         = showBatchDropdown,
                            onDismissRequest = { showBatchDropdown = false },
                            modifier         = Modifier.background(ChipBg)
                        ) {
                            batchOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text       = option,
                                            color      = if (option == selectedBatch) TealChip else Color.White,
                                            fontWeight = if (option == selectedBatch) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedBatch     = option
                                        showBatchDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        FilterPill(
                            label   = selectedFreq,
                            icon    = Icons.Default.DateRange,
                            onClick = { showFreqDropdown = true }
                        )
                        DropdownMenu(
                            expanded         = showFreqDropdown,
                            onDismissRequest = { showFreqDropdown = false },
                            modifier         = Modifier.background(ChipBg)
                        ) {
                            freqOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text       = option,
                                            color      = if (option == selectedFreq) TealChip else Color.White,
                                            fontWeight = if (option == selectedFreq) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedFreq     = option   
                                        showFreqDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    FilterPill(
                        label   = dateLabel,
                        icon    = Icons.Default.CalendarToday,
                        onClick = { showDatePicker = true }
                    )

                    FilterPill(
                        label   = "Upload CSV",
                        icon    = Icons.Default.UploadFile,
                        onClick = { showCsvDialog = true }
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCircle(value = "$total",   label = "TOTAL",   icon = Icons.Default.Group,       tint = Color(0xFF90A4AE))
                    StatCircle(value = "$present", label = "PRESENT", icon = Icons.Default.CheckCircle, tint = PresentColor)
                    StatCircle(value = "$absent",  label = "ABSENT",  icon = Icons.Default.Cancel,      tint = AbsentColor)
                    StatCircle(value = "$leave",   label = "LEAVE",   icon = Icons.Default.BeachAccess, tint = LeaveColor)
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(TableHeaderBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STUDENT / ID", color = UnmarkedColor, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
                        modifier = Modifier.weight(1f))
                    Text("STATUS", color = UnmarkedColor, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
                        modifier = Modifier.width(72.dp))
                    Text("ACTION", color = UnmarkedColor, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
                        modifier = Modifier.width(80.dp))
                }

                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(
                        items = studentList,
                        key   = { student: Student -> student.studentId }
                    ) { student: Student ->
                        val key    = student.studentId + "_" + selectedCourse.courseId
                        val status = statusMap[key] ?: AttendanceStatus.UNMARKED
                        val name   = StubRepository.getUserDisplayName(student.user)

                        StudentAttendanceRow(
                            name      = name,
                            id        = student.studentId,
                            status    = status,
                            hasRemark = remarksMap[key]?.isNotBlank() == true,
                            onApprove = {
                                markAndPersist(student, AttendanceStatus.PRESENT)
                                detailStudent = student
                                detailRemark  = remarksMap[key] ?: ""
                                saved         = false
                            },
                            onReject  = {
                                markAndPersist(student, AttendanceStatus.ABSENT)
                                detailStudent = student
                                detailRemark  = remarksMap[key] ?: ""
                                saved         = false
                            },
                            onLeave   = {
                                markAndPersist(student, AttendanceStatus.APPROVED_LEAVE)
                                detailStudent = student
                                detailRemark  = remarksMap[key] ?: ""
                                saved         = false
                            },
                            onClick   = {
                                detailStudent = student
                                detailRemark  = remarksMap[key] ?: ""
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(if (detailStudent != null) 240.dp else 16.dp))
                    }
                }
            }

            val currentDetail: Student? = detailStudent
            if (currentDetail != null) {
                val name     = StubRepository.getUserDisplayName(currentDetail.user)
                val initials = name
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(PanelBg)
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(TealChip),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(name,                             color = Color.White,   fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("ID: ${currentDetail.studentId}", color = UnmarkedColor, fontSize   = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text          = "STUDENT REMARKS",
                        color         = UnmarkedColor,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    TextField(
                        value         = detailRemark,
                        onValueChange = { detailRemark = it },
                        placeholder   = { Text("Add attendance note...", color = UnmarkedColor, fontSize = 13.sp) },
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = RemarksBg,
                            unfocusedContainerColor = RemarksBg,
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape      = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines   = 2
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val key           = currentDetail.studentId + "_" + selectedCourse.courseId
                            val currentStatus = statusMap[key] ?: AttendanceStatus.UNMARKED
                            // Write remark into the session map so re-opening shows it
                            if (detailRemark.isNotBlank()) remarksMap[key] = detailRemark
                            else remarksMap.remove(key)
                            // Persist the remark alongside the current status
                            viewModel.markAttendance(
                                AttendanceRecord(
                                    attendanceId = key,
                                    student      = currentDetail,
                                    course       = selectedCourse,
                                    date         = selectedDate.time,
                                    status       = currentStatus,
                                    remarks      = detailRemark
                                )
                            )
                            saved         = true
                            detailStudent = null
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = TealChip),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Attendance Entry", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceTopBar(onNavigate: (String) -> Unit, onExport: () -> Unit) {
    TopAppBar(
        title = {
            Text("Manage Attendance", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        },
        navigationIcon = {
            IconButton(onClick = { onNavigate("back") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnBackground)
            }
        },
        actions = {
            IconButton(onClick = onExport) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = TealChip)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

@Composable
private fun FilterPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ChipBg)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        Icon(icon, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun StatCircle(value: String, label: String, icon: ImageVector, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(StatCircleBg)
                .border(2.dp, tint.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = UnmarkedColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StudentAttendanceRow(
    name: String,
    id: String,
    status: AttendanceStatus,
    hasRemark: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onLeave: () -> Unit,
    onClick: () -> Unit
) {
    val initials = name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RowBg)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(TealChip.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            if (hasRemark) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(LeaveColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = "Has remark",
                        tint     = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White,   fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(id,   color = UnmarkedColor, fontSize   = 10.sp)
        }

        Box(
            modifier = Modifier
                .width(72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(status.statusColor().copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text          = status.displayLabel(),
                color         = status.statusColor(),
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }

        Spacer(Modifier.width(6.dp))

        Row(
            modifier              = Modifier.width(80.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            ActionIconBtn(icon = Icons.Default.Check,       tint = PresentColor, onClick = onApprove)
            ActionIconBtn(icon = Icons.Default.Close,       tint = AbsentColor,  onClick = onReject)
            ActionIconBtn(icon = Icons.Default.BeachAccess, tint = LeaveColor,   onClick = onLeave)
        }
    }
}

@Composable
private fun ActionIconBtn(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvUploadDialog(
    courses:        List<Course>,
    selectedCourse: Course,
    onDismiss:      () -> Unit,
    onConfirm:      (studentName: String, course: Course) -> Unit
) {
    var studentName     by remember { mutableStateOf("") }
    var chosenCourse    by remember { mutableStateOf(selectedCourse) }
    var showDropdown    by remember { mutableStateOf(false) }
    var confirmationMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF111E2B),
        shape            = RoundedCornerShape(16.dp),
        title            = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    tint     = TealChip,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upload CSV",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealChip.copy(alpha = 0.12f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint     = TealChip,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Enter a row from your CSV file. The student will be marked Present.",
                        color    = Color(0xFF90A4AE),
                        fontSize = 11.sp
                    )
                }

                Text(
                    "STUDENT NAME",
                    color         = UnmarkedColor,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                TextField(
                    value         = studentName,
                    onValueChange = { studentName = it; confirmationMsg = null },
                    placeholder   = { Text("e.g. Alex Smith", color = UnmarkedColor, fontSize = 13.sp) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color(0xFF0A1929),
                        unfocusedContainerColor = Color(0xFF0A1929),
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        focusedIndicatorColor   = TealChip,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    "COURSE",
                    color         = UnmarkedColor,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A1929))
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${chosenCourse.courseCode} – ${chosenCourse.title}",
                            color    = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = UnmarkedColor)
                    }
                    DropdownMenu(
                        expanded         = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier         = Modifier.background(Color(0xFF0D2233))
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${course.courseCode} – ${course.title}",
                                        color      = if (course.courseId == chosenCourse.courseId) TealChip else Color.White,
                                        fontWeight = if (course.courseId == chosenCourse.courseId) FontWeight.Bold else FontWeight.Normal,
                                        fontSize   = 13.sp
                                    )
                                },
                                onClick = {
                                    chosenCourse = course
                                    showDropdown = false
                                    confirmationMsg = null
                                }
                            )
                        }
                    }
                }

                confirmationMsg?.let { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PresentColor.copy(alpha = 0.12f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PresentColor, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(msg, color = PresentColor, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    if (studentName.isNotBlank()) {
                        onConfirm(studentName, chosenCourse)
                    }
                },
                enabled  = studentName.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor  = TealChip,
                    disabledContainerColor = TealChip.copy(alpha = 0.3f)
                ),
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark as Present", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = UnmarkedColor)
            }
        }
    )
}

private fun java.util.Date.toDayEnum(cal: Calendar = Calendar.getInstance()): DayEnum {
    cal.time = this
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY    -> DayEnum.MONDAY
        Calendar.TUESDAY   -> DayEnum.TUESDAY
        Calendar.WEDNESDAY -> DayEnum.WEDNESDAY
        Calendar.THURSDAY  -> DayEnum.THURSDAY
        Calendar.FRIDAY    -> DayEnum.FRIDAY
        Calendar.SATURDAY  -> DayEnum.SATURDAY
        else               -> DayEnum.SUNDAY
    }
}
