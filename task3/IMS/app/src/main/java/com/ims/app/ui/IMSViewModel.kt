package com.ims.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.ims.app.data.model.*
import com.ims.app.data.model.PersonalTimetableSlot
import com.ims.app.data.repository.StubRepository

// ── Search result model ───────────────────────────────────────────────────────

enum class SearchResultType { PAGE, COURSE, STUDENT, SLOT, SETTING }

data class SearchResult(
    val label:    String,
    val subtitle: String,
    val type:     SearchResultType,
    val route:    String,
    val icon:     ImageVector
)

class IMSViewModel : ViewModel() {

    // ── Auth state ────────────────────────────────────────────────────────────
    var currentUser by mutableStateOf<User?>(null)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    fun login(username: String, password: String) {
        // Strip email domain so "admin@ims.ac.in" → "admin" to match stub credentials
        val localPart = username.substringBefore("@").trim()
        val user = StubRepository.login("$localPart:$password")
        if (user != null) {
            currentUser = user
            loginError = null
        } else {
            loginError = "Invalid credentials"
        }
    }

    fun logout() { currentUser = null }

    fun getCurrentUserName(): String =
        currentUser?.let { StubRepository.getUserDisplayName(it) } ?: ""

    fun getCurrentUserRole(): String =
        currentUser?.let { StubRepository.getUserRole(it) } ?: ""

    fun getCurrentUserEmail(): String = when (currentUser?.userId) {
        "u1" -> "admin@ims.edu"
        "u2" -> "alex@ims.edu"
        else -> ""
    }

    fun isAdmin(): Boolean = getCurrentUserRole() == "Admin"

    // ── Dashboard ─────────────────────────────────────────────────────────────
    val dashboardStats = StubRepository.dashboardStats

    // ── Courses ───────────────────────────────────────────────────────────────
    val allCourses = StubRepository.courses

    var selectedCourseFilter by mutableStateOf<CourseType?>(null)

    fun getFilteredCourses(): List<Course> =
        if (selectedCourseFilter == null) allCourses
        else allCourses.filter { it.type == selectedCourseFilter }

    // ── Timetable ─────────────────────────────────────────────────────────────

    // FIX: Use mutableStateListOf so any add/edit triggers recomposition automatically
    val timetableSlots: MutableList<TimetableSlot> =
        mutableStateListOf<TimetableSlot>().also { it.addAll(StubRepository.timetableSlots) }

    var selectedSemester by mutableStateOf("Sem 5")

    fun getTimetableForSemester(sem: String): List<TimetableSlot> =
        timetableSlots.filter { it.semester == sem }

    // FIX: Actually persists the slot to both the reactive state list and the repository
    fun addTimetableSlot(slot: TimetableSlot) {
        // Replace if editing (same slotId), otherwise append
        val idx = timetableSlots.indexOfFirst { it.slotId == slot.slotId }
        if (idx >= 0) timetableSlots[idx] = slot else timetableSlots.add(slot)
        StubRepository.saveTimetableSlot(slot)
    }

    /** Returns all rooms available for booking, derived from the repository. */
    fun getAllRooms(): List<Room> = StubRepository.allRooms

    // ── Personal timetable slots (student) ────────────────────────────────────

    // FIX: Back personal slots with mutableStateListOf so additions trigger recomposition
    val personalSlots: MutableList<PersonalTimetableSlot> =
        mutableStateListOf<PersonalTimetableSlot>().also { it.addAll(StubRepository.getPersonalSlots()) }

    /**
     * Adds a student-created personal timetable slot.
     * Updates both the reactive [personalSlots] list and the repository.
     */
    fun addPersonalSlot(slot: PersonalTimetableSlot) {
        personalSlots.add(slot)
        StubRepository.addPersonalSlot(slot)
    }

    /** Returns all personal timetable slots for the current student. */
    fun getUserPersonalSlots(): List<PersonalTimetableSlot> = personalSlots.toList()

    // ── Attendance ────────────────────────────────────────────────────────────
    var selectedCourseForAttendance by mutableStateOf<Course?>(null)

    fun getAttendanceRecords(): List<AttendanceRecord> =
        selectedCourseForAttendance?.let {
            StubRepository.getAttendanceForCourse(it.courseId)
        } ?: StubRepository.attendanceRecords.toList()

    fun markAttendance(record: AttendanceRecord) {
        StubRepository.saveAttendance(record)
    }

    fun getAttendancePercent(courseId: String): Float =
        StubRepository.computeAttendancePercent(
            StubRepository.sampleStudent.studentId, courseId
        )

    fun getAttendanceForCourse(courseId: String): List<AttendanceRecord> =
        StubRepository.getAttendanceForCourse(courseId)

    fun saveAttendance(record: AttendanceRecord) =
        StubRepository.saveAttendance(record)

    // ── Registration (Sign Up) ────────────────────────────────────────────────
    fun registerUser(name: String, email: String, password: String): Boolean {
        // Stub: always succeeds
        return true
    }

    // ── Search index ──────────────────────────────────────────────────────────
    /**
     * Builds the full searchable index for the current user's role.
     * Called once per query; cheap since all data is in-memory.
     *
     * Index contents:
     *  - Pages  : always included, role-gated
     *  - Courses: always included
     *  - Slots  : timetable slots for the current semester
     *  - Students: admin only
     *  - Settings: always included (language, logout)
     */
    fun buildSearchIndex(): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // ── Pages ─────────────────────────────────────────────────────────────
        val pages = buildList {
            add(SearchResult("Dashboard",         "Home screen",                    SearchResultType.PAGE, Screen.Dashboard.route,      Icons.Default.Home))
            if (isAdmin()) {
                add(SearchResult("Manage Attendance",  "Mark & review student attendance", SearchResultType.PAGE, Screen.AdminAttendance.route, Icons.Default.Fingerprint))
                add(SearchResult("Timetable Admin",    "Edit lecture schedule",            SearchResultType.PAGE, Screen.AdminTimetable.route,  Icons.Default.CalendarMonth))
            }
            if (!isAdmin()) {
                add(SearchResult("My Attendance",      "View your attendance records",     SearchResultType.PAGE, Screen.Attendance.route,      Icons.Default.BarChart))
                add(SearchResult("Monthly Attendance", "Month-by-month breakdown",         SearchResultType.PAGE, Screen.MonthlyAttendance.route, Icons.Default.DateRange))
                add(SearchResult("Timetable",          "Your class schedule",              SearchResultType.PAGE, Screen.Timetable.route,        Icons.Default.CalendarMonth))
            }
            add(SearchResult("Courses",            "Browse all courses",                SearchResultType.PAGE, Screen.CourseFilter.route,    Icons.Default.MenuBook))
        }
        results.addAll(pages)

        // ── Courses ───────────────────────────────────────────────────────────
        StubRepository.courses.forEach { course ->
            results.add(SearchResult(
                label    = course.title,
                subtitle = "${course.courseCode} · ${course.semester}",
                type     = SearchResultType.COURSE,
                route    = Screen.CourseFilter.route,
                icon     = Icons.Default.School
            ))
        }

        // ── Timetable slots ───────────────────────────────────────────────────
        StubRepository.timetableSlots.forEach { slot ->
            results.add(SearchResult(
                label    = slot.course.title,
                subtitle = "${slot.day.name.lowercase().replaceFirstChar { it.uppercase() }} · ${slot.start}–${slot.end} · ${slot.room.name}",
                type     = SearchResultType.SLOT,
                route    = if (isAdmin()) Screen.AdminTimetable.route else Screen.Timetable.route,
                icon     = Icons.Default.Schedule
            ))
        }

        // ── Students (admin only) ─────────────────────────────────────────────
        if (isAdmin()) {
            listOf(StubRepository.sampleStudent).forEach { student ->
                val displayName = StubRepository.getUserDisplayName(student.user)
                results.add(SearchResult(
                    label    = displayName,
                    subtitle = "Student · ${student.program.name} · ${student.batchId}",
                    type     = SearchResultType.STUDENT,
                    route    = Screen.AdminAttendance.route,
                    icon     = Icons.Default.Person
                ))
            }
        }

        // ── Settings ──────────────────────────────────────────────────────────
        results.add(SearchResult("Language & Region", "Change display language and timezone", SearchResultType.SETTING, "language_prefs", Icons.Default.Language))
        results.add(SearchResult("Logout",            "Sign out of your account",             SearchResultType.SETTING, "logout",         Icons.Default.ExitToApp))

        return results
    }
}