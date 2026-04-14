package com.ims.app.data.model

import java.util.Date

/** UML: AttendanceRecord */
data class AttendanceRecord(
    val attendanceId: String,
    val student: Student,
    val course: Course,
    val date: Date,
    val status: AttendanceStatus,
    val remarks: String
) {
    fun markPresent() { /* stub */ }
    fun markAbsent() { /* stub */ }
    fun markLeave(reason: String) { /* stub */ }
    fun generateDailyReport(courseId: String, date: Date): List<AttendanceRecord> = emptyList()
    fun generateMonthlyReport(courseId: String, month: String): List<AttendanceRecord> = emptyList()
}

/** UML: Marks */
data class Marks(
    val marksId: String,
    val exam: Exam,
    val student: Student,
    val marks: Float,
    val grade: String
) {
    fun computeGrade(): String = grade
    fun updateMarks(lowestMarks: Float) { /* stub */ }
}

/** UML: GradingSystem */
data class GradingSystem(
    val systemId: String,
    val method: GradingMethod,
    val gradeBoundaries: List<Int>,
    val passMark: Float
) {
    fun assignGrades(marks: Float, max: Float): String = "A"
    fun computeGPA(marksList: List<Float>): Float = 0f
    fun isPass(marks: Float): Boolean = marks >= passMark
}

/** UML: Exam */
data class Exam(
    val examId: String,
    val course: Course,
    val room: Room,
    val type: ExamType,
    val date: Date,
    val start: String,
    val duration: Int,
    val maxMarks: Float,
    val gradingSystem: GradingSystem,
    val batch: List<Batch>,
    val questionAnswers: List<Pair<String, String>>
) {
    fun scheduleExam(date: Date) { /* stub */ }
    fun enterMarks(studentId: String, marks: Marks) { /* stub */ }
    fun computeGrades(): List<Marks> = emptyList()
    fun generateMarksheet(batchId: String): Map<String, Float> = emptyMap()
    fun getStatistics(): List<Any> = emptyList()
    fun inputQAns(question: String, answer: String) { /* stub */ }
    fun getAns(): Pair<String, String> = Pair("", "")
}

/** UML: FeeRecord */
data class FeeRecord(
    val feeId: String,
    val student: Student,
    val category: FeeCategory,
    val amount: Float,
    val dueDate: Date,
    val paidDate: Date?,
    val status: PaymentStatus,
    val receiptNumber: String
) {
    fun processPayment(amount: Float): String = "receipt_stub"
    fun issueRefund(reason: String) { /* stub */ }
    fun flagAsDefaulter() { /* stub */ }
    fun generateReceipt(): String = receiptNumber
}

/** UML: Transaction */
data class Transaction(
    val txnId: String,
    val type: TxnType,
    val amount: Float,
    val date: Date,
    val category: String,
    val user: User,
    val initiatedBy: User,
    val referenceId: String,
    val mode: TxnMode
) {
    fun record() { /* stub */ }
    fun categorise(category: String) { /* stub */ }
}

/** UML: PayrollConfig */
data class PayrollConfig(
    val configId: String,
    val emp: Employee?,
    val basic: Float,
    val allowances: Map<String, Float>,
    val deductions: Map<String, Float>,
    val effectiveFrom: Date
) {
    fun computeGrossSalary(): Float = basic + allowances.values.sum()
    fun computeNetSalary(): Float = computeGrossSalary() - deductions.values.sum()
    fun updateAllowance(type: String, amount: Float) { /* stub */ }
    fun updateDeduction(type: String, amount: Float) { /* stub */ }
}

/** UML: Payslip */
data class Payslip(
    val payslipId: String,
    val emp: Employee,
    val month: String,
    val gross: Float,
    val deductions: Float,
    val netPay: Float,
    val status: PayslipStatus,
    val approvedBy: User?
) {
    fun generatePayslip() { /* stub */ }
    fun approve(approverId: String) { /* stub */ }
    fun reject(reason: String) { /* stub */ }
    fun triggerFinanceTransaction(): Transaction? = null
    fun downloadAsPDF(): Any? = null
}

/** UML: LeaveApplication */
data class LeaveApplication(
    val leaveId: String,
    val applicant: User,
    val leaveType: LeaveType,
    val fromDate: Date,
    val toDate: Date
) {
    fun submit() { /* stub */ }
    fun cancel() { /* stub */ }
    fun getDuration(): Int = 1
    fun deductFromBalance(): Int = 0
}

/** UML: Recruitment */
data class Recruitment(
    val interviewId: String,
    val candidate: Person,
    val mode: InterviewMode,
    val panelists: List<User>,
    val scheduledDate: Date,
    val outcome: InterviewOutcome?
) {
    fun scheduleInterview(date: Date, mode: InterviewMode) { /* stub */ }
    fun assignPanel(facultyIds: List<String>) { /* stub */ }
    fun recordOutcome(result: InterviewOutcome) { /* stub */ }
    fun sendInvitation() { /* stub */ }
}

/** UML: Message */
data class Message(
    val messageId: String,
    val sender: User,
    val recipients: List<User>,
    val subject: String,
    val body: String,
    val sentAt: Date,
    val readAt: Date?,
    val attachments: List<Any>
) {
    fun send() { /* stub */ }
    fun markAsRead(userId: String) { /* stub */ }
    fun reply(body: String): Message = this.copy(messageId = "reply_stub")
    fun forward(userId: String): Message = this.copy(messageId = "fwd_stub")
}

/** UML: NewsItem */
data class NewsItem(
    val newsId: String,
    val title: String,
    val author: User,
    val publishedAt: Date,
    val tag: List<String>,
    val isPublished: Boolean
) {
    fun publish() { /* stub */ }
    fun unpublish() { /* stub */ }
    fun editContent(title: String, body: String) { /* stub */ }
    fun searchByTag(tag: String): List<NewsItem> = emptyList()
    fun getComments(): List<NewsComment> = emptyList()
}

/** UML: NewsComment */
data class NewsComment(
    val commentId: String,
    val news: NewsItem,
    val user: User,
    val text: String,
    val postedAt: Date,
    val isDeleted: Boolean
) {
    fun post() { /* stub */ }
    fun moderateAndDelete() { /* stub */ }
    fun flag(reason: String) { /* stub */ }
}
