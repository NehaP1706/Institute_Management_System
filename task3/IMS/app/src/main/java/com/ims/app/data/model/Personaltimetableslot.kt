package com.ims.app.data.model

/**
 * A personal timetable entry created by a student.
 * Unlike [TimetableSlot], this is not tied to a [Course], [Room], or semester —
 * it is a lightweight, free-form block the student adds themselves.
 *
 * @param id        Unique identifier (generated client-side until a real backend exists).
 * @param title     Display title for the slot (e.g. "Study Group – Algorithms").
 * @param startTime Start time as "HH:mm".
 * @param endTime   End time as "HH:mm". Must be after [startTime].
 * @param reason    Optional note explaining why the slot was added (e.g. "Exam prep").
 */
data class PersonalTimetableSlot(
    val id        : String,
    val title     : String,
    val startTime : String,
    val endTime   : String,
    val reason    : String? = null
) {
    /** Human-readable time range, e.g. "09:00 – 10:30". */
    fun timeRange(): String = "$startTime – $endTime"

    /** Duration in minutes, computed from [startTime] and [endTime]. */
    fun durationMinutes(): Int {
        val toMin = { t: String ->
            val parts = t.split(":")
            parts[0].toInt() * 60 + (parts.getOrNull(1)?.toInt() ?: 0)
        }
        return (toMin(endTime) - toMin(startTime)).coerceAtLeast(0)
    }
}