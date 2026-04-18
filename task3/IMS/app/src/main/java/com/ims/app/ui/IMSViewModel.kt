package com.ims.app.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.ims.app.data.model.*
import com.ims.app.data.model.PersonalTimetableSlot
import com.ims.app.data.repository.StubRepository

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
        "u3" -> "ramesh@ims.edu"
        else -> ""
    }

    fun isAdmin(): Boolean = getCurrentUserRole() == "Admin"
    fun isFaculty(): Boolean = getCurrentUserRole() == "Faculty"

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
}