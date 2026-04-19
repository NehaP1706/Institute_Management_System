package com.ims.app.data.model

import java.util.Date

/** UML: Department */
data class Department(
    val departmentId: String,
    val name: String,
    val code: String
) {
    fun getMembers(): List<User> = emptyList()
    fun getStudents(): List<Student> = emptyList()
}

/** UML: Course */
data class Course(
    val courseId: String,
    val courseCode: String,
    val title: String,
    val credits: Int,
    val type: CourseType,
    val program: List<ProgramType>,
    val syllabus: String,
    val instructorName: String,
    val semester: String
) {
    fun getEnrolledStudents(): List<Student> = emptyList()
    fun getAttendanceRecords(): List<AttendanceRecord> = emptyList()
    fun getExams(): List<Exam> = emptyList()
    fun getTimetableSlots(): List<TimetableSlot> = emptyList()
    fun computeAttendancePercentage(studentId: String): Float = 0f
}

/** UML: Batch */
data class Batch(
    val batchId: String,
    val admissionYear: Int,
    val students: List<Student>,
    val allowedElectives: List<Course>
) {
    @JvmName("fetchStudents")
    fun getStudents(): List<Student> = students
    fun addStudent(id: String) { /* stub */ }
    fun removeStudent(id: String) { /* stub */ }
    fun transferStudent(id: String, batchId: String) { /* stub */ }
    fun getCourses(semester: String): List<Course> = emptyList()
}

/** UML: Enrollment */
data class Enrollment(
    val enrollmentId: String,
    val student: Student,
    val course: Course,
    val credits: Int,
    val finalGrade: String
) {
    fun enroll(): Enrollment = this
    fun drop() { /* stub */ }
    fun complete(): Enrollment = this
    fun getFinalGrade(enrollmentId: String, studentId: String): String = finalGrade
}

/** UML: Room */
data class Room(
    val roomId: String,
    val name: String,
    val capacity: Int,
    val type: RoomType,
    val building: String
) {
    fun checkAvailability(slot: TimetableSlot): Boolean = true
    fun book(sid: String, details: Any): TimetableSlot? = null
    fun getTimetable(): List<TimetableSlot> = emptyList()
}

/** UML: TimetableSlot */
data class TimetableSlot(
    val slotId: String,
    val course: Course,
    val day: DayEnum,
    val start: String,   // Time as HH:mm
    val end: String,
    val room: Room,
    val semester: String
) {
    fun checkConflict(slot: TimetableSlot): Boolean = false
    fun reschedule(newDay: DayEnum, newTime: String) { /* stub */ }
    fun getWeeklyLoad(id: String): Int = 0
    fun alertOnLimitBreach(type: String) { /* stub */ }
}

/** UML: Broadcast */
data class Broadcast(
    val broadcastId: String,
    val sender: User,
    val audience: AudienceType,
    val message: String,
    val sentAt: Date
) {
    fun sendToAll() { /* stub */ }
    fun sendToBatch(batchId: String) { /* stub */ }
    fun sendToDepartment(deptId: String) { /* stub */ }
    fun schedule(dateTime: Date) { /* stub */ }
}