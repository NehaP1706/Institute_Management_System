package com.ims.app.data.repository

import com.ims.app.data.model.*
import java.util.Date

/**
 * In-memory stub repository simulating a backend.
 * Supports role switching (ADMIN / STUDENT / FACULTY) for end-to-end demo.
 */
object StubRepository {

    // ── Roles & Permissions ──────────────────────────────────────────────────

    private val adminRole = Role(
        roleId = "r1", roleName = "Admin",
        permissions = listOf(
            Permission("p1", Module.DASHBOARD, AccessLevel.EDIT, AccessLevel.values().toList()),
            Permission("p2", Module.ATTENDANCE, AccessLevel.EDIT, AccessLevel.values().toList()),
            Permission("p3", Module.TIME_TABLE, AccessLevel.EDIT, AccessLevel.values().toList())
        ),
        moduleAccess = Module.values().associateWith { AccessLevel.EDIT }
    )

    private val studentRole = Role(
        roleId = "r2", roleName = "Student",
        permissions = listOf(
            Permission("p4", Module.ATTENDANCE, AccessLevel.VIEW, listOf(AccessLevel.VIEW)),
            Permission("p5", Module.TIME_TABLE, AccessLevel.VIEW, listOf(AccessLevel.VIEW))
        ),
        moduleAccess = mapOf(
            Module.DASHBOARD to AccessLevel.VIEW,
            Module.ATTENDANCE to AccessLevel.VIEW,
            Module.TIME_TABLE to AccessLevel.VIEW
        )
    )

    private val facultyRole = Role(
        roleId = "r3", roleName = "Faculty",
        permissions = listOf(
            Permission("p6", Module.ATTENDANCE, AccessLevel.EDIT, AccessLevel.values().toList()),
            Permission("p7", Module.TIME_TABLE, AccessLevel.VIEW, listOf(AccessLevel.VIEW))
        ),
        moduleAccess = mapOf(
            Module.DASHBOARD to AccessLevel.VIEW,
            Module.ATTENDANCE to AccessLevel.EDIT,
            Module.TIME_TABLE to AccessLevel.VIEW
        )
    )

    // ── Users ────────────────────────────────────────────────────────────────

    private val adminUser = User(
        userId = "u1", passwordHash = "admin123",
        role = listOf(adminRole), isActive = true,
        lastLogin = Date(), languagePref = "en", timezonePref = "IST"
    )

    private val studentUser = User(
        userId = "u2", passwordHash = "student123",
        role = listOf(studentRole), isActive = true,
        lastLogin = Date(), languagePref = "en", timezonePref = "IST"
    )

    private val facultyUser = User(
        userId = "u3", passwordHash = "faculty123",
        role = listOf(facultyRole), isActive = true,
        lastLogin = Date(), languagePref = "en", timezonePref = "IST"
    )

    // ── Persons & Faculty ────────────────────────────────────────────────────

    private val adminPerson = Person("p1","Academic Admin","admin@ims.edu","9999999999","",Date())
    private val studentPerson = Person("p2","Alex Smith","alex@ims.edu","8888888888","",Date())
    private val facultyPerson = Person("p3","Dr. Ramesh","ramesh@ims.edu","7777777777","",Date())

    private val dummyPayroll = PayrollConfig("pc1",null,50000f,
        mapOf("HRA" to 5000f,"DA" to 2000f),
        mapOf("PF" to 3000f,"Tax" to 4000f), Date())

    private val facultyEmployee = Employee(
        "e1", facultyUser, Designation.PROFESSOR, "d1", Date(), null, dummyPayroll
    )

    val sampleFaculty = Faculty("f1", facultyEmployee, "Algorithms", "Block A, Room 205")

    val sampleStudent = Student(
        studentId = "s1", user = studentUser, batchId = "b1",
        program = ProgramType.CSE, cgpa = 8.5f,
        advisor = sampleFaculty, coadvisor = null,
        admissionDate = Date(), guardians = emptyList(),
        emergencyContact = listOf("9876543210"),
        previousEducation = listOf("Class XII - CBSE")
    )

    // ── Courses ──────────────────────────────────────────────────────────────

    val courses = listOf(
        Course("c1","CS101","Advanced Algorithms",4, CourseType.H1,
            listOf(ProgramType.CSE),"Graphs, DP, etc.", listOf(sampleFaculty),"Sem 5"),
        Course("c2","CS102","Operating Systems",4, CourseType.H1,
            listOf(ProgramType.CSE),"Process, Memory", listOf(sampleFaculty),"Sem 5"),
        Course("c3","CS103","Database Systems",3, CourseType.H2,
            listOf(ProgramType.CSE),"SQL, NoSQL", listOf(sampleFaculty),"Sem 5"),
        Course("c4","CS104","Computer Networks",3, CourseType.H2,
            listOf(ProgramType.CSE),"TCP/IP", listOf(sampleFaculty),"Sem 5"),
        Course("c5","CS201","Machine Learning",4, CourseType.FULL,
            listOf(ProgramType.CSE, ProgramType.ECE),"ML Fundamentals", listOf(sampleFaculty),"Sem 6"),
    )

    // ── Rooms ────────────────────────────────────────────────────────────────

    private val room101 = Room("rm1","LH-101",120, RoomType.Room100C,"Lecture Block")
    private val room201 = Room("rm2","LH-201",60, RoomType.Room100C,"Lecture Block")
    private val lab1 = Room("rm3","CS-Lab-1",40, RoomType.ComputerLab,"Lab Block")

    // ── Timetable Slots ──────────────────────────────────────────────────────

    val timetableSlots = listOf(
        TimetableSlot("ts1", courses[0], DayEnum.MONDAY,    "09:00","10:00", room101, "Sem 5"),
        TimetableSlot("ts2", courses[1], DayEnum.MONDAY,    "10:00","11:00", room201, "Sem 5"),
        TimetableSlot("ts3", courses[2], DayEnum.TUESDAY,   "09:00","10:00", room101, "Sem 5"),
        TimetableSlot("ts4", courses[3], DayEnum.WEDNESDAY, "11:00","12:00", room101, "Sem 5"),
        TimetableSlot("ts5", courses[0], DayEnum.THURSDAY,  "14:00","15:00", room101, "Sem 5"),
        TimetableSlot("ts6", courses[4], DayEnum.FRIDAY,    "10:00","11:00", lab1,    "Sem 6"),
    )

    // ── Attendance Records ────────────────────────────────────────────────────

    val attendanceRecords: MutableList<AttendanceRecord> = mutableListOf(
        AttendanceRecord("a1", sampleStudent, courses[0], Date(), AttendanceStatus.PRESENT, ""),
        AttendanceRecord("a2", sampleStudent, courses[1], Date(), AttendanceStatus.ABSENT, ""),
        AttendanceRecord("a3", sampleStudent, courses[2], Date(), AttendanceStatus.PRESENT, ""),
        AttendanceRecord("a4", sampleStudent, courses[3], Date(), AttendanceStatus.APPROVED_LEAVE, "Medical"),
        AttendanceRecord("a5", sampleStudent, courses[0], Date(), AttendanceStatus.PRESENT, ""),
    )

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    val dashboardStats = mapOf(
        "totalStudents" to 12482,
        "totalFaculty" to 842,
        "totalCourses" to 28
    )

    // ── Auth ──────────────────────────────────────────────────────────────────

    /** Returns User if credentials match, null otherwise */
    fun login(credentials: String): User? {
        val (username, password) = credentials.split(":").let {
            if (it.size == 2) it[0] to it[1] else return null
        }
        return when {
            username == "admin" && password == "admin123" -> adminUser
            username == "student" && password == "student123" -> studentUser
            username == "faculty" && password == "faculty123" -> facultyUser
            else -> null
        }
    }

    fun getUserDisplayName(user: User): String = when(user.userId) {
        "u1" -> "Academic Admin"
        "u2" -> "Alex Smith"
        "u3" -> "Dr. Ramesh"
        else -> "Unknown"
    }

    fun getUserRole(user: User): String = user.role.firstOrNull()?.roleName ?: "Unknown"

    // ── Attendance helpers ────────────────────────────────────────────────────

    fun getAttendanceForCourse(courseId: String): List<AttendanceRecord> =
        attendanceRecords.filter { it.course.courseId == courseId }

    fun computeAttendancePercent(studentId: String, courseId: String): Float {
        val records = attendanceRecords.filter {
            it.student.studentId == studentId && it.course.courseId == courseId
        }
        if (records.isEmpty()) return 0f
        val present = records.count { it.status == AttendanceStatus.PRESENT }
        return present.toFloat() / records.size * 100
    }

    fun saveAttendance(record: AttendanceRecord) {
        attendanceRecords.removeIf {
            it.student.studentId == record.student.studentId &&
                    it.course.courseId == record.course.courseId &&
                    it.date == record.date
        }
        attendanceRecords.add(record)
    }
}
