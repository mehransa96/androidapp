package com.example.tools

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.data.AppDatabase
import com.example.data.model.HealthEntryType
import com.example.data.preferences.UserPreferencesManager
import com.example.data.preferences.WellnessPrefs
import com.example.domain.BloodPressure
import com.example.domain.CycleInsights
import com.example.domain.CyclePredictor
import com.example.utils.JalaliCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * A clean, printable A4 PDF summary for a doctor visit: cycle statistics, recent cycles,
 * frequent symptoms, blood pressure, contraception and medicines. Uses only the Android
 * framework (PdfDocument + StaticLayout, which shapes Persian text right-to-left correctly).
 */
object DoctorReportPdf {
    private const val PAGE_W = 595 // A4 in PostScript points
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    suspend fun write(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val content = collect(context)
        val doc = PdfDocument()
        try {
            val writer = Writer(doc)
            content(writer)
            writer.finish()
            val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IllegalStateException("فایل مقصد باز نشد")
            out.use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private fun jalali(date: LocalDate) = JalaliCalendar.gregorianToJalali(date).formatLong()

    private suspend fun collect(context: Context): (Writer) -> Unit {
        val db = AppDatabase.getDatabase(context)
        val prefs = UserPreferencesManager(context)
        val wellness = WellnessPrefs(context)
        val today = LocalDate.now()
        val bleeding = db.dailyLogDao().getPeriodDaysList().mapNotNull { runCatching { LocalDate.parse(it.dateStr) }.getOrNull() }
        val insights = CycleInsights.analyze(bleeding, today)
        val prediction = CyclePredictor.predict(bleeding, prefs.averageCycleLength.value, today)
        val since = today.minusDays(90).toString()
        val symptoms = db.physicalSymptomDao().getAllSymptomsList().filter { it.dateStr >= since }
        val topSymptoms = symptoms.groupBy { it.symptomName }
            .map { (name, list) -> Triple(name, list.size, list.map { it.severityScore }.average()) }
            .sortedByDescending { it.second }.take(8)
        val entries = db.healthEntryDao().getAllList()
        val bp = entries.filter { it.type == HealthEntryType.BLOOD_PRESSURE && it.num1 != null && it.num2 != null }.take(8)
        val pregnancyTests = entries.filter { it.type == HealthEntryType.PREGNANCY_TEST }.take(3)
        val lastExam = entries.firstOrNull { it.type == HealthEntryType.BREAST_EXAM }
        val meds = prefs.medicationReminders.value.filter { it.isEnabled }
        val name = prefs.userName.value

        return { w ->
            w.text("گزارش سلامت قاعدگی برای پزشک", 20f, bold = true, color = 0xFFDB2777.toInt())
            w.text("تهیه‌شده با اپلیکیشن هانیتا · تاریخ گزارش: ${jalali(today)} (${today})", 10f, color = Color.DKGRAY)
            if (name.isNotBlank()) w.text("نام: $name", 11f)
            w.space(8f)

            w.heading("خلاصه چرخه")
            if (insights.cycles.isEmpty()) {
                w.text("هنوز هیچ دوره قاعدگی ثبت نشده است.", 11f)
            } else {
                w.text("آخرین شروع پریود: ${jalali(insights.cycles.last().start)} (${insights.daysSinceLastStart} روز پیش)", 11f)
                w.text("میانگین طول چرخه: ${insights.averageCycle?.let { "$it روز" } ?: "داده کافی نیست"} · تعداد چرخه‌های کامل: ${insights.completedCycles}", 11f)
                if (insights.shortestCycle != null) {
                    w.text("کوتاه‌ترین/بلندترین چرخه: ${insights.shortestCycle} / ${insights.longestCycle} روز · نوسان: ${insights.variation ?: 0} روز", 11f)
                }
                w.text("میانگین روزهای خونریزی: ${insights.averageBleeding?.let { "$it روز" } ?: "نامشخص"}", 11f)
                w.text("شروع پیش‌بینی‌شده دوره بعد: ${jalali(prediction.predictedNextStart)}${if (prediction.isLate) " (پریود تأخیر دارد)" else ""}", 11f)
            }
            w.space(6f)

            if (insights.flags.isNotEmpty()) {
                w.heading("نکات قابل توجه")
                insights.flags.forEach { f ->
                    val color = when (f.severity) {
                        CycleInsights.Severity.SEE_DOCTOR -> 0xFFB91C1C.toInt()
                        CycleInsights.Severity.WATCH -> 0xFFB45309.toInt()
                        CycleInsights.Severity.INFO -> 0xFF047857.toInt()
                    }
                    w.text("• ${f.title}", 11f, bold = true, color = color)
                    w.text(f.message, 10f, color = Color.DKGRAY)
                }
                w.space(6f)
            }

            if (insights.cycles.isNotEmpty()) {
                w.heading("چرخه‌های اخیر")
                insights.cycles.takeLast(8).reversed().forEach { c ->
                    val len = c.length?.let { "$it روز" } ?: "در جریان"
                    w.text("• شروع ${jalali(c.start)} — طول چرخه: $len — خونریزی: ${c.bleedingDays} روز", 10.5f)
                }
                w.space(6f)
            }

            w.heading("علائم پرتکرار (۹۰ روز اخیر)")
            if (topSymptoms.isEmpty()) w.text("علامتی ثبت نشده است.", 10.5f)
            topSymptoms.forEach { (s, count, sev) ->
                val sevText = when {
                    sev >= 2.5 -> "شدید"
                    sev >= 1.5 -> "متوسط"
                    else -> "خفیف"
                }
                w.text("• $s: $count بار (شدت معمول: $sevText)", 10.5f)
            }
            w.space(6f)

            if (bp.isNotEmpty()) {
                w.heading("فشار خون (آخرین اندازه‌گیری‌ها)")
                bp.forEach { e ->
                    val cat = BloodPressure.classify(e.num1!!, e.num2!!)
                    w.text("• ${jalali(LocalDate.parse(e.dateStr))}: ${e.num1}/${e.num2} mmHg — ${cat.label}", 10.5f)
                }
                w.space(6f)
            }

            w.heading("پیشگیری از بارداری و داروها")
            val start = wellness.pillStart
            if (wellness.pillEnabled && start != null) {
                w.text("قرص ترکیبی خوراکی: ${wellness.pillType.label} — شروع بسته فعلی/اول: ${jalali(start)}", 10.5f)
            } else {
                w.text("قرص ضدبارداری در برنامه ثبت نشده است.", 10.5f)
            }
            if (meds.isEmpty()) w.text("داروی فعالی ثبت نشده است.", 10.5f)
            meds.forEach { m -> w.text("• ${m.name} ${m.dosage} — ساعت ${m.time}", 10.5f) }
            pregnancyTests.forEach { t ->
                val result = if (t.value == "POSITIVE") "مثبت" else if (t.value == "NEGATIVE") "منفی" else "نامشخص"
                w.text("تست بارداری ${jalali(LocalDate.parse(t.dateStr))}: $result", 10.5f)
            }
            lastExam?.let { w.text("آخرین خودآزمایی پستان: ${jalali(LocalDate.parse(it.dateStr))}${if (it.value == "CONCERN") " (مورد نگران‌کننده گزارش شده)" else ""}", 10.5f) }
            w.space(12f)

            w.text(
                "این گزارش از داده‌هایی که کاربر خودش ثبت کرده تهیه شده و جایگزین معاینه، آزمایش یا تشخیص پزشک نیست.",
                9f, color = Color.GRAY
            )
        }
    }

    /** Minimal flowing-text page writer with automatic page breaks. */
    private class Writer(private val doc: PdfDocument) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = MARGIN
        private val width = (PAGE_W - 2 * MARGIN).toInt()

        private fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            y = MARGIN
        }

        fun space(h: Float) {
            y += h
        }

        fun heading(t: String) {
            space(4f)
            text(t, 13f, bold = true, color = 0xFF7C3AED.toInt())
            val p = page ?: return
            val line = Paint().apply { color = 0x337C3AED; strokeWidth = 1f }
            p.canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
            space(4f)
        }

        fun text(t: String, size: Float, bold: Boolean = false, color: Int = Color.BLACK) {
            if (page == null) newPage()
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            val layout = StaticLayout.Builder.obtain(t, 0, t.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(2f, 1f)
                .build()
            if (y + layout.height > PAGE_H - MARGIN) newPage()
            val canvas = page!!.canvas
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 3f
        }

        fun finish() {
            if (page == null) newPage()
            page?.let { doc.finishPage(it) }
            page = null
        }
    }
}
