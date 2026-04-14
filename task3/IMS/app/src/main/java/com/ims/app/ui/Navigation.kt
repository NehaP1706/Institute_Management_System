package com.ims.app.ui

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object SignUp         : Screen("signup")
    object Dashboard      : Screen("dashboard")
    object Attendance     : Screen("attendance")
    object AdminAttendance: Screen("admin_attendance")
    object Timetable      : Screen("timetable")
    object AdminTimetable : Screen("admin_timetable")
    object CourseFilter   : Screen("course_filter")
    object MonthlyAttendance : Screen("monthly_attendance")
}
