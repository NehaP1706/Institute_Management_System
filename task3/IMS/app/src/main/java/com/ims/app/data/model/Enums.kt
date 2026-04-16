package com.ims.app.data.model

enum class ProgramType { CSE, ECE, CSD, ECD, CLD, CHD, CGD, CND, PG, MTech }
enum class CourseType { H1, H2, H, FULL }
enum class AttendanceStatus { PRESENT, ABSENT, APPROVED_LEAVE, UNMARKED }
enum class ExamType { QUIZ1, MIDSEM, QUIZ2, ENDSEM }
enum class GradingMethod { ABSOLUTE, RELATIVE }
enum class RoomType { ScienceLab, ComputerLab, Room100C, Room300C }
enum class PaymentStatus { PAID, PENDING, CANCELLED }
enum class PayslipStatus { APPROVED, PENDING, SCHEDULED }
enum class LeaveType { MEDICAL, ACADEMIC, EMERGENCY, VACATION }
enum class TxnType { TUITION_FEE, FINANCIAL_AIDS, REFUNDS }
enum class TxnMode { UPI, RTGS, NET_BANKING, BANK_LOAN, DEMAND_DRAFT, CHEQUE, CARD }
enum class FeeCategory { HOSTEL, MESS, TUITION }
enum class ApprovalStatus { APPROVED, PENDING, REJECTED }
enum class InterviewMode { ONLINE, OFFLINE }
enum class InterviewOutcome { RECRUITED, NOT_RECRUITED }
enum class AccessLevel { VIEW, EDIT, DELETE }
enum class AudienceType { BATCH, DEPARTMENT, ALL, HOSTEL1, HOSTEL2, FACULTY }
enum class Designation {
    PROFESSOR, ASSOCIATE_PROFESSOR, DEAN_OF_ACADEMICS,
    LAB_TECHNICIANS, REGISTRAR, MAINTENANCE_WORKERS, SECURITY_PERSONS
}
enum class DayEnum { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
enum class Module {
    DASHBOARD, ADMISSION, STUDENT_DETAILS, EXAMINATIONS,
    MANAGE_USERS, HR, ATTENDANCE, FINANCE, MESSAGES, TIME_TABLE, MANAGE_NEWS
}
