package com.ims.app.data.model

/**
 * A personal timetable entry created by a student.
 * Unlike [TimetableSlot], this is not tied to a [Course], [Room], or semester —
 * it is a lightweight, free-form block the student adds themselves.
 */
data class PersonalTimetableSlot(
    val id        : String,
    val title     : String,
    val startTime : String,
    val endTime   : String,
    val reason    : String? = null
) {
    fun timeRange(): String = "$startTime – $endTime"
    fun durationMinutes(): Int {
        val toMin = { t: String ->
            val parts = t.split(":")
            parts[0].toInt() * 60 + (parts.getOrNull(1)?.toInt() ?: 0)
        }
        return (toMin(endTime) - toMin(startTime)).coerceAtLeast(0)
    }
}
