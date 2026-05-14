package com.ims.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.ims.app.data.model.AttendanceRecord
import com.ims.app.data.model.AttendanceStatus
import com.ims.app.data.model.Course
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Colour palette (mirrors the app's dark-teal theme)
// ─────────────────────────────────────────────────────────────────────────────
private object PdfColors {
    val BACKGROUND   = Color.parseColor("#0D1B2A")   // deep navy
    val SURFACE      = Color.parseColor("#112233")   // card surface
    val HEADER_BG    = Color.parseColor("#0D7A6E")   // teal header
    val PRIMARY      = Color.parseColor("#1DC9A4")   // mint-green accent
    val ABSENT_RED   = Color.parseColor("#EF5350")
    val LEAVE_ORANGE = Color.parseColor("#FFA726")
    val WHITE        = Color.WHITE
    val WHITE_60     = Color.argb(153, 255, 255, 255)
    val WHITE_80     = Color.argb(204, 255, 255, 255)
    val DIVIDER      = Color.argb(40,  255, 255, 255)
    val ROW_ALT      = Color.argb(20,  255, 255, 255)
}

// ─────────────────────────────────────────────────────────────────────────────
// Public entry-points
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Build and share a "Daily" attendance PDF.
 *
 * @param context       Android context (Activity or Application).
 * @param records       All attendance records to include (already filtered by
 *                      course / date-range by the caller).
 * @param allCourses    Full course list (used to group records by course).
 * @param fromMillis    Start of the date-range filter (null = no lower bound).
 * @param toMillis      End of the date-range filter   (null = no upper bound).
 */
fun exportDailyAttendancePdf(
    context:    Context,
    records:    List<AttendanceRecord>,
    allCourses: List<Course>,
    fromMillis: Long?,
    toMillis:   Long?
) {
    val dateRange = buildDateRangeLabel(fromMillis, toMillis)
    val title     = "Daily Attendance Report"
    val subtitle  = if (dateRange.isNotBlank()) "Period: $dateRange" else "All dates"

    val file = buildPdf(context, "attendance_daily_${timestamp()}.pdf") { doc ->
        renderReport(
            doc        = doc,
            context    = context,
            title      = title,
            subtitle   = subtitle,
            records    = records,
            allCourses = allCourses,
            mode       = ReportMode.DAILY
        )
    }
    sharePdf(context, file)
}

/**
 * Build and share a "Monthly" attendance PDF.
 *
 * @param context    Android context.
 * @param records    All attendance records for the given month (already
 *                   filtered by the caller).
 * @param allCourses Full course list.
 * @param year       Calendar year being shown.
 * @param month      0-based month index (Calendar.JANUARY = 0).
 */
fun exportMonthlyAttendancePdf(
    context:    Context,
    records:    List<AttendanceRecord>,
    allCourses: List<Course>,
    year:       Int,
    month:      Int
) {
    val cal      = Calendar.getInstance().apply { set(year, month, 1) }
    val label    = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    val title    = "Monthly Attendance Report"
    val subtitle = label

    val file = buildPdf(context, "attendance_${label.replace(" ", "_").lowercase()}.pdf") { doc ->
        renderReport(
            doc        = doc,
            context    = context,
            title      = title,
            subtitle   = subtitle,
            records    = records,
            allCourses = allCourses,
            mode       = ReportMode.MONTHLY
        )
    }
    sharePdf(context, file)
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

private enum class ReportMode { DAILY, MONTHLY }

/** A4 page dimensions in points (72 pt/inch). */
private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 36f

/** Generates the PDF and returns the saved [File]. */
private fun buildPdf(
    context:  Context,
    filename: String,
    block:    (PdfDocument) -> Unit
): File {
    val doc  = PdfDocument()
    block(doc)

    val dir  = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ?: context.filesDir
    dir.mkdirs()
    val file = File(dir, filename)
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return file
}

/**
 * Core renderer: draws the cover header then one section per course.
 * Pages are created dynamically as content overflows.
 */
private fun renderReport(
    doc:        PdfDocument,
    context:    Context,
    title:      String,
    subtitle:   String,
    records:    List<AttendanceRecord>,
    allCourses: List<Course>,
    mode:       ReportMode
) {
    // Group records by course, preserving allCourses order
    val byCourse: Map<String, List<AttendanceRecord>> = buildMap {
        allCourses.forEach { course ->
            val courseRecords = records
                .filter { it.course.courseId == course.courseId }
                .sortedBy { it.date }
            if (courseRecords.isNotEmpty()) put(course.courseId, courseRecords)
        }
    }

    val state = PageState(doc)

    // ── Cover header (first page) ─────────────────────────────────────────
    state.newPage()
    drawCoverHeader(state.canvas!!, title, subtitle)
    state.cursorY = MARGIN + 130f   // below the header band

    // ── Generation timestamp ──────────────────────────────────────────────
    val genLabel = "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}"
    val metaPaint = textPaint(10f, PdfColors.WHITE_60)
    state.canvas!!.drawText(genLabel, MARGIN, state.cursorY, metaPaint)
    state.cursorY += 20f

    // ── Overall summary bar ───────────────────────────────────────────────
    val totalPresent = records.count { it.status == AttendanceStatus.PRESENT }
    val totalAbsent  = records.count { it.status == AttendanceStatus.ABSENT  }
    val totalLeave   = records.count { it.status == AttendanceStatus.APPROVED_LEAVE }
    val totalMarked  = records.count { it.status != AttendanceStatus.UNMARKED }
    val overallPct   = if (totalMarked > 0) totalPresent * 100 / totalMarked else 0

    state.ensureSpace(52f)
    drawSummaryBar(
        canvas  = state.canvas!!,
        y       = state.cursorY,
        present = totalPresent,
        absent  = totalAbsent,
        leave   = totalLeave,
        pct     = overallPct,
        label   = "Overall – all courses"
    )
    state.cursorY += 52f

    // ── Per-course sections ───────────────────────────────────────────────
    allCourses.forEach { course ->
        val courseRecords = byCourse[course.courseId] ?: return@forEach

        val present = courseRecords.count { it.status == AttendanceStatus.PRESENT }
        val absent  = courseRecords.count { it.status == AttendanceStatus.ABSENT  }
        val leave   = courseRecords.count { it.status == AttendanceStatus.APPROVED_LEAVE }
        val marked  = courseRecords.count { it.status != AttendanceStatus.UNMARKED }
        val pct     = if (marked > 0) present * 100 / marked else 0

        // Section heading
        state.ensureSpace(70f, doc)
        drawSectionHeading(state.canvas!!, state.cursorY, course)
        state.cursorY += 30f

        // Mini-summary
        state.ensureSpace(52f, doc)
        drawSummaryBar(
            canvas  = state.canvas!!,
            y       = state.cursorY,
            present = present,
            absent  = absent,
            leave   = leave,
            pct     = pct,
            label   = "${course.courseCode} – ${course.semester}"
        )
        state.cursorY += 52f

        // Table header
        state.ensureSpace(26f, doc)
        drawTableHeader(state.canvas!!, state.cursorY, mode)
        state.cursorY += 26f

        // Rows
        courseRecords.forEachIndexed { idx, record ->
            state.ensureSpace(28f, doc)
            drawTableRow(state.canvas!!, state.cursorY, idx, record, mode)
            state.cursorY += 28f
        }

        state.cursorY += 12f  // gap between courses
    }

    // ── Footer on every page ──────────────────────────────────────────────
    state.finishAllPages()
}

// ─────────────────────────────────────────────────────────────────────────────
// Draw helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun drawCoverHeader(canvas: Canvas, title: String, subtitle: String) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Teal header band
    paint.color = PdfColors.HEADER_BG
    canvas.drawRect(0f, 0f, PAGE_W.toFloat(), MARGIN + 110f, paint)

    // App label
    paint.color     = PdfColors.WHITE_60
    paint.textSize  = 9f
    paint.typeface  = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("IMS – Integrated Management System", MARGIN, MARGIN + 18f, paint)

    // Title
    paint.color    = PdfColors.WHITE
    paint.textSize = 22f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(title, MARGIN, MARGIN + 48f, paint)

    // Subtitle (period / month)
    paint.color    = PdfColors.WHITE_80
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(subtitle, MARGIN, MARGIN + 68f, paint)

    // Accent line
    paint.color       = PdfColors.PRIMARY
    paint.strokeWidth = 3f
    canvas.drawLine(MARGIN, MARGIN + 80f, PAGE_W - MARGIN, MARGIN + 80f, paint)
}

private fun drawSummaryBar(
    canvas:  Canvas,
    y:       Float,
    present: Int,
    absent:  Int,
    leave:   Int,
    pct:     Int,
    label:   String
) {
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val left   = MARGIN
    val right  = PAGE_W - MARGIN
    val width  = right - left
    val height = 44f
    val radius = 8f

    // Card background
    paint.color = PdfColors.SURFACE
    canvas.drawRoundRect(RectF(left, y, right, y + height), radius, radius, paint)

    // Label
    paint.color    = PdfColors.WHITE_60
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(label, left + 12f, y + 14f, paint)

    // Pills: Present | Absent | Leave | pct%
    val pillY   = y + 30f
    var pillX   = left + 12f

    data class Pill(val count: Int, val lbl: String, val color: Int)
    listOf(
        Pill(present, "PRESENT", PdfColors.PRIMARY),
        Pill(absent,  "ABSENT",  PdfColors.ABSENT_RED),
        Pill(leave,   "LEAVE",   PdfColors.LEAVE_ORANGE)
    ).forEach { pill ->
        paint.color    = pill.color
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val countW = paint.measureText("${pill.count}")
        paint.color    = PdfColors.WHITE_60
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val lblW   = paint.measureText(" ${pill.lbl}  ")
        val totalW = countW + lblW + 16f

        // pill bg
        paint.color = Color.argb(40, 255, 255, 255)
        canvas.drawRoundRect(RectF(pillX, pillY - 10f, pillX + totalW, pillY + 4f), 6f, 6f, paint)

        paint.color    = pill.color
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${pill.count}", pillX + 8f, pillY, paint)
        paint.color    = PdfColors.WHITE_80
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(" ${pill.lbl}", pillX + 8f + countW, pillY, paint)

        pillX += totalW + 6f
    }

    // pct badge (right-aligned)
    val pctStr   = "$pct%"
    paint.color  = PdfColors.PRIMARY
    paint.textSize  = 14f
    paint.typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val pctW     = paint.measureText(pctStr)
    canvas.drawText(pctStr, right - pctW - 12f, y + 30f, paint)
}

private fun drawSectionHeading(canvas: Canvas, y: Float, course: Course) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Left accent bar
    paint.color = PdfColors.PRIMARY
    canvas.drawRect(MARGIN, y, MARGIN + 3f, y + 24f, paint)

    // Course title
    paint.color    = PdfColors.WHITE
    paint.textSize = 13f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(course.title, MARGIN + 10f, y + 16f, paint)

    // Course code chip
    val code    = course.courseCode
    paint.textSize = 9f
    val codeW   = paint.measureText(code) + 14f
    paint.color = Color.argb(60, 29, 201, 164)
    canvas.drawRoundRect(
        RectF(PAGE_W - MARGIN - codeW, y + 4f, PAGE_W - MARGIN.toFloat(), y + 20f),
        6f, 6f, paint
    )
    paint.color    = PdfColors.PRIMARY
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(code, PAGE_W - MARGIN - codeW + 7f, y + 15f, paint)
}

private fun drawTableHeader(canvas: Canvas, y: Float, mode: ReportMode) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Header strip
    paint.color = Color.argb(80, 13, 122, 110)
    canvas.drawRect(MARGIN, y, PAGE_W - MARGIN.toFloat(), y + 22f, paint)

    paint.color    = PdfColors.WHITE_60
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    val cols = tableColumns(mode)
    cols.forEach { col ->
        canvas.drawText(col.header.uppercase(), col.x, y + 15f, paint)
    }
}

private fun drawTableRow(
    canvas: Canvas,
    y:      Float,
    idx:    Int,
    record: AttendanceRecord,
    mode:   ReportMode
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Alternating row background
    if (idx % 2 == 0) {
        paint.color = PdfColors.ROW_ALT
        canvas.drawRect(MARGIN, y, PAGE_W - MARGIN.toFloat(), y + 26f, paint)
    }

    // Divider
    paint.color       = PdfColors.DIVIDER
    paint.strokeWidth = 0.5f
    canvas.drawLine(MARGIN, y + 26f, PAGE_W - MARGIN.toFloat(), y + 26f, paint)

    val dateFmt  = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dayFmt   = SimpleDateFormat("EEE",         Locale.getDefault())
    val timeFmt  = SimpleDateFormat("HH:mm",       Locale.getDefault())

    val statusColor = when (record.status) {
        AttendanceStatus.PRESENT        -> PdfColors.PRIMARY
        AttendanceStatus.ABSENT         -> PdfColors.ABSENT_RED
        AttendanceStatus.APPROVED_LEAVE -> PdfColors.LEAVE_ORANGE
        else                            -> PdfColors.WHITE_60
    }
    val statusText = when (record.status) {
        AttendanceStatus.PRESENT        -> "Present"
        AttendanceStatus.ABSENT         -> "Absent"
        AttendanceStatus.APPROVED_LEAVE -> "Leave"
        else                            -> "–"
    }

    val cols = tableColumns(mode)
    val textY = y + 17f

    cols.forEach { col ->
        val text = when (col.key) {
            "date"    -> dateFmt.format(record.date)
            "day"     -> dayFmt.format(record.date)
            "course"  -> record.course.courseCode
            "status"  -> statusText
            "remarks" -> record.remarks.take(30).ifBlank { "–" }
            "time"    -> timeFmt.format(record.date)
            else      -> "–"
        }

        paint.color    = if (col.key == "status") statusColor else PdfColors.WHITE_80
        paint.textSize = if (col.key == "status") 9f else 9f
        paint.typeface = if (col.key == "status")
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        else
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText(text, col.x, textY, paint)
    }
}

private fun drawPageFooter(canvas: Canvas, pageNum: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color       = PdfColors.DIVIDER
    paint.strokeWidth = 1f
    canvas.drawLine(MARGIN, PAGE_H - 28f, PAGE_W - MARGIN.toFloat(), PAGE_H - 28f, paint)

    paint.color    = PdfColors.WHITE_60
    paint.textSize = 8f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("IMS Attendance Report", MARGIN, PAGE_H - 14f, paint)

    val pgStr = "Page $pageNum"
    val pgW   = paint.measureText(pgStr)
    canvas.drawText(pgStr, PAGE_W - MARGIN - pgW, PAGE_H - 14f, paint)
}

// ─────────────────────────────────────────────────────────────────────────────
// Column layout
// ─────────────────────────────────────────────────────────────────────────────

private data class Col(val key: String, val header: String, val x: Float)

private fun tableColumns(mode: ReportMode): List<Col> {
    // For daily mode show date + day; for monthly show date only (course col is
    // inside the section so we skip it; we add a "Time" column instead).
    return if (mode == ReportMode.DAILY) {
        listOf(
            Col("date",    "Date",    MARGIN + 4f),
            Col("day",     "Day",     MARGIN + 90f),
            Col("course",  "Course",  MARGIN + 130f),
            Col("status",  "Status",  MARGIN + 200f),
            Col("remarks", "Remarks", MARGIN + 260f)
        )
    } else {
        listOf(
            Col("date",    "Date",    MARGIN + 4f),
            Col("day",     "Day",     MARGIN + 90f),
            Col("status",  "Status",  MARGIN + 145f),
            Col("remarks", "Remarks", MARGIN + 215f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page-state helper (handles pagination)
// ─────────────────────────────────────────────────────────────────────────────

private class PageState(private val doc: PdfDocument) {
    var canvas:  Canvas? = null
    var cursorY: Float   = 0f
    var pageNum: Int     = 0

    private var currentPage: PdfDocument.Page? = null

    fun newPage() {
        finishCurrent()
        pageNum++
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
        val page = doc.startPage(info)
        currentPage = page
        canvas = page.canvas

        // Page background
        val bg = Paint().apply { color = PdfColors.BACKGROUND }
        canvas!!.drawRect(0f, 0f, PAGE_W.toFloat(), PAGE_H.toFloat(), bg)

        cursorY = MARGIN + 12f
    }

    /** Ensure at least [needed] pts remain; if not, start a new page. */
    fun ensureSpace(needed: Float, doc: PdfDocument? = null) {
        if (cursorY + needed > PAGE_H - 40f) newPage()
    }

    private fun finishCurrent() {
        if (currentPage != null) {
            drawPageFooter(canvas!!, pageNum)
            doc.finishPage(currentPage)
            currentPage = null
            canvas = null
        }
    }

    fun finishAllPages() {
        finishCurrent()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Share via FileProvider
// ─────────────────────────────────────────────────────────────────────────────

private fun sharePdf(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, "Open attendance report").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

// ─────────────────────────────────────────────────────────────────────────────
// Small utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun textPaint(size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color    = color
    this.textSize = size
    this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
}

private fun buildDateRangeLabel(fromMillis: Long?, toMillis: Long?): String {
    val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return when {
        fromMillis != null && toMillis != null ->
            "${fmt.format(Date(fromMillis))} → ${fmt.format(Date(toMillis))}"
        fromMillis != null -> "From ${fmt.format(Date(fromMillis))}"
        toMillis   != null -> "Up to ${fmt.format(Date(toMillis))}"
        else               -> ""
    }
}

private fun timestamp() =
    SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())