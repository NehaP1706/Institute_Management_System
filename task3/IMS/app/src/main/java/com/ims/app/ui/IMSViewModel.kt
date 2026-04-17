package com.ims.app.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.ims.app.data.model.*
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
    val timetableSlots = StubRepository.timetableSlots

    var selectedSemester by mutableStateOf("Sem 5")

    fun getTimetableForSemester(sem: String): List<TimetableSlot> =
        timetableSlots.filter { it.semester == sem }

    fun addTimetableSlot(slot: TimetableSlot) {
        // stub – in production, persist to DB
    }

    /** Returns all rooms available for booking, derived from the repository. */
    fun getAllRooms(): List<Room> = StubRepository.allRooms

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