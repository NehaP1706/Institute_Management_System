package com.ims.app.data.model

import java.util.Date

/** UML: Employee extends User */
data class Employee(
    val empId: String,
    val user: User,
    val designation: Designation,
    val departmentId: String,
    val joinDate: Date,
    val exitDate: Date?,
    val payrollConfig: PayrollConfig
) {
    fun getPayslips(year: Int): List<Payslip> = emptyList()
    fun getLeaveBalance(): Int = 20
    fun updatePersonalDetails(details: Map<String, Any>) { /* stub */ }
}

/** UML: Faculty extends Employee */
data class Faculty(
    val facultyId: String,
    val employee: Employee,
    val specialisation: String,
    val officeLocation: String
) {
    fun getCoursesTaught(semester: String): List<Course> = emptyList()
    fun getSupervisedStudents(): List<Student> = emptyList()
}

/** UML: Student extends User */
data class Student(
    val studentId: String,
    val user: User,
    val batchId: String,
    val program: ProgramType,
    val cgpa: Float,
    val advisor: Faculty?,
    val coadvisor: Faculty?,
    val admissionDate: Date,
    val guardians: List<Guardian>,
    val emergencyContact: List<String>,
    val previousEducation: List<String>
) {
    fun getEnrolledCourses(semester: String): List<Course> = emptyList()
    fun registerForCourse(courseId: String): Boolean = true
    fun getAttendanceSummary(courseId: String): List<Any> = emptyList()
    fun getExamResults(semester: String): List<Marks> = emptyList()
    fun viewFeeStatus(): List<FeeRecord> = emptyList()
    fun computeGPA(): Float = cgpa
}
