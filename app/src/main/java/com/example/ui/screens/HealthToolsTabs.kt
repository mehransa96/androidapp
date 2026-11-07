package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthEntry
import com.example.data.model.HealthEntryType
import com.example.domain.BreastSelfExam
import com.example.domain.CycleInsights
import com.example.domain.PillPack
import com.example.security.AppLockSession
import com.example.ui.components.ProfessionalDatePickerDialog
import com.example.ui.viewmodel.HealthToolsViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------------------------------------
// Contraceptive pill
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PillTab(viewModel: HealthToolsViewModel) {
    val pill by viewModel.pill.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val today = remember { LocalDate.now() }
    var pickStart by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val takenDays = remember(entries) {
        entries.filter { it.type == HealthEntryType.PILL }.mapNotNull { runCatching { LocalDate.parse(it.dateStr) }.getOrNull() }.toSet()
    }

    ToolCard(Icons.Filled.Medication, "قرص ضدبارداری خوراکی") {
        SwitchRow(
            title = "پیگیری بسته قرص و یادآور روزانه",
            subtitle = "هر روز در ساعت انتخابی یادآوری می‌شود؛ از خود اعلان هم می‌توانید «مصرف کردم» را بزنید.",
            checked = pill.enabled,
            onCheckedChange = { on -> viewModel.updatePill(on, pill.start ?: today, pill.type, pill.hour, pill.minute) }
        )
        if (pill.enabled) {
            HorizontalDivider()
            Text("نوع بسته", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            ChoiceChips(PillPack.Type.entries.associate { it.code to it.label }, pill.type.code) { code ->
                if (code != null) viewModel.updatePill(true, pill.start, PillPack.Type.fromCode(code), pill.hour, pill.minute)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) {
                    Text("روز اول بسته: ${pill.start?.let { jalaliShort(it) } ?: "انتخاب"}", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("%02d:%02d".format(pill.hour, pill.minute), fontSize = 12.sp)
                }
            }
            NoteText("روز اول بسته = روزی که اولین قرص همین بسته (یا اولین بسته) را خوردید. بسته‌های بعدی خودکار محاسبه می‌شوند.")
        }
    }

    val start = pill.start
    if (pill.enabled && start != null) {
        val day = PillPack.dayFor(start, pill.type, today)
        ToolCard(Icons.Filled.CheckCircle, "امروز") {
            if (day == null) {
                Text("بسته از ${jalaliLong(start)} شروع می‌شود.", fontSize = 14.sp)
            } else {
                val takenToday = today in takenDays
                Text(
                    "روز ${day.dayInPack} از ${pill.type.packDays} · بسته شماره ${day.packNumber}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
                Text(
                    if (day.isActivePill) "امروز قرص فعال (هورمونی) دارید." else "روزهای استراحت/قرص بی‌اثر. بسته بعدی ${day.daysUntilNextPack} روز دیگر.",
                    fontSize = 13.sp
                )
                if (day.isActivePill || pill.type == PillPack.Type.P24_4) {
                    Button(onClick = { viewModel.setPillTaken(today, !takenToday) }) {
                        Text(if (takenToday) "مصرف شد ✓ (لغو)" else "قرص امروز را خوردم")
                    }
                }
                // Last 7 days strip
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (6 downTo 0).forEach { back ->
                        val d = today.minusDays(back.toLong())
                        val info = PillPack.dayFor(start, pill.type, d)
                        val color = when {
                            info == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            !info.isActivePill -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            d in takenDays -> Color(0xFF10B981)
                            d == today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.error
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(26.dp).background(color, CircleShape))
                            Text(jalaliDay(d), fontSize = 10.sp)
                        }
                    }
                }
                NoteText("سبز: مصرف شده · قرمز: ثبت نشده · خاکستری: روز استراحت")
                val missed = PillPack.missedActiveDays(start, pill.type, takenDays, today.minusDays(7), today.minusDays(1))
                if (missed.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${missed.size} قرص در ۷ روز گذشته ثبت نشده است", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "اگر واقعاً فراموش شده، طبق دستور بروشور همان قرص یا راهنمایی پزشک/داروساز عمل کنید. " +
                                    "ممکن است تا چند روز به روش کمکی (مثل کاندوم) نیاز باشد.",
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { missed.forEach { viewModel.setPillTaken(it, true) } }) {
                                    Text("همه را خورده بودم")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickStart) {
        ProfessionalDatePickerDialog(
            initialDate = pill.start ?: today,
            title = "روز اول بسته قرص",
            minDate = today.minusYears(2),
            maxDate = today.plusDays(30),
            onDismissRequest = { pickStart = false },
            onDateSelected = {
                viewModel.updatePill(true, it, pill.type, pill.hour, pill.minute)
                pickStart = false
            }
        )
    }
    if (pickTime) {
        val state = rememberTimePickerState(initialHour = pill.hour, initialMinute = pill.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("ساعت یادآور قرص") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePill(true, pill.start, pill.type, state.hour, state.minute)
                    pickTime = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("انصراف") } }
        )
    }
}

private fun jalaliDay(date: LocalDate): String = com.example.utils.JalaliCalendar.gregorianToJalali(date).day.toString()

// ---------------------------------------------------------------------------------------------
// Breast self-exam
// ---------------------------------------------------------------------------------------------

@Composable
internal fun BreastExamTab(viewModel: HealthToolsViewModel) {
    val entries by viewModel.entries.collectAsState()
    val cycle by viewModel.cycle.collectAsState()
    val reminder by viewModel.breastReminder.collectAsState()
    val today = remember { LocalDate.now() }
    val last = entries.firstOrNull { it.type == HealthEntryType.BREAST_EXAM }
    val lastDate = last?.let { runCatching { LocalDate.parse(it.dateStr) }.getOrNull() }
    val next = BreastSelfExam.nextDate(cycle?.prediction?.lastPeriodStart, cycle?.prediction?.averageCycleLength ?: 28, today)

    ToolCard(Icons.Filled.Alarm, "خودآزمایی ماهانه پستان", tint = Color(0xFFEC4899)) {
        Text(
            when {
                lastDate == null -> "هنوز خودآزمایی ثبت نشده است."
                else -> "آخرین خودآزمایی: ${jalaliLong(lastDate)} (${ChronoUnit.DAYS.between(lastDate, today)} روز پیش)"
            },
            fontSize = 14.sp
        )
        if (BreastSelfExam.isDue(lastDate, today)) {
            Text("زمان خودآزمایی این ماه است.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text("زمان پیشنهادی بعدی: ${jalaliLong(next)}", fontSize = 13.sp)
        SwitchRow(
            title = "یادآور ماهانه",
            subtitle = "چند روز بعد از پایان پریود (حدود روز ۷ چرخه) که حساسیت پستان کمتر است.",
            checked = reminder,
            onCheckedChange = { viewModel.setBreastReminder(it) }
        )
        HorizontalDivider()
        Text("ثبت نتیجه امروز", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HealthLabels.breastExam.forEach { (code, label) ->
                OutlinedButton(onClick = {
                    viewModel.addEntry(HealthEntry(dateStr = today.toString(), type = HealthEntryType.BREAST_EXAM, value = code))
                }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) }
            }
        }
        if (last?.value == "CONCERN" && lastDate == today) {
            Text(
                "بیشتر توده‌ها و تغییرات پستان خوش‌خیم هستند، اما لازم است در روزهای آینده توسط پزشک معاینه شوید. نگران نباشید و پیگیری را عقب نیندازید.",
                color = MaterialTheme.colorScheme.error, fontSize = 13.sp
            )
        }
    }

    ToolCard(Icons.Filled.Info, "راهنمای گام‌به‌گام") {
        val steps = listOf(
            "۱. جلوی آینه با دست‌های آویزان، سپس دست‌ها روی کمر و بالای سر: شکل، اندازه، پوست و نوک پستان را نگاه کنید.",
            "۲. به پشت دراز بکشید، دست راست زیر سر؛ با بند وسط سه انگشت دست چپ پستان راست را با حرکات دایره‌ای کوچک لمس کنید.",
            "۳. با سه فشار سبک، متوسط و عمیق تمام پستان را از ترقوه تا زیر سینه و از زیربغل تا وسط قفسه سینه بررسی کنید.",
            "۴. زیر بغل را هم لمس کنید و همین کار را برای سمت دیگر تکرار کنید.",
            "۵. می‌توانید زیر دوش با پوست خیس و صابونی هم همین معاینه را انجام دهید."
        )
        steps.forEach { Text(it, fontSize = 13.sp) }
        HorizontalDivider()
        Text("چه زمانی به پزشک مراجعه کنم؟", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            "توده یا سفتی جدید، تغییر شکل یا اندازه، فرورفتگی یا قرمزی پوست (پوست پرتقالی)، ترشح خونی یا ترشح بدون فشار از نوک پستان، فرورفتن تازه نوک پستان، یا درد مداوم یک‌طرفه.",
            fontSize = 13.sp
        )
        NoteText("خودآزمایی جایگزین معاینه بالینی و ماموگرافی/سونوگرافی غربالگری طبق توصیه پزشک نیست.")
    }
}

// ---------------------------------------------------------------------------------------------
// Cycle insights
// ---------------------------------------------------------------------------------------------

@Composable
internal fun CycleInsightsTab(viewModel: HealthToolsViewModel) {
    val cycle by viewModel.cycle.collectAsState()
    val c = cycle
    if (c == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        return
    }
    val r = c.insights
    if (r.cycles.isEmpty()) {
        ToolCard(Icons.Filled.Analytics, "تحلیل سلامت چرخه") {
            Text("برای تحلیل، روزهای پریود را در تقویم یا صفحه علائم ثبت کنید. پس از دو تا سه دوره، تحلیل دقیق‌تر می‌شود.", fontSize = 14.sp)
        }
        return
    }
    ToolCard(Icons.Filled.Analytics, "آمار چرخه شما") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("میانگین چرخه", r.averageCycle?.let { "$it روز" } ?: "—", Modifier.weight(1f))
            StatBox("میانگین خونریزی", r.averageBleeding?.let { "$it روز" } ?: "—", Modifier.weight(1f))
            StatBox("نوسان", r.variation?.let { "$it روز" } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("کوتاه‌ترین", r.shortestCycle?.let { "$it روز" } ?: "—", Modifier.weight(1f))
            StatBox("بلندترین", r.longestCycle?.let { "$it روز" } ?: "—", Modifier.weight(1f))
            StatBox("چرخه‌های کامل", r.completedCycles.toString(), Modifier.weight(1f))
        }
        if (r.completedCycles < 3) NoteText("با ثبت حداقل ۳ چرخه کامل، تحلیل نظم چرخه فعال می‌شود.")
    }

    r.flags.forEach { f ->
        val (bg, fg) = when (f.severity) {
            CycleInsights.Severity.SEE_DOCTOR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            CycleInsights.Severity.WATCH -> Color(0xFFFEF3C7) to Color(0xFF92400E)
            CycleInsights.Severity.INFO -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        }
        Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp)) {
                Icon(
                    if (f.severity == CycleInsights.Severity.INFO) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null, tint = fg
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(f.title, fontWeight = FontWeight.Bold, color = fg, fontSize = 14.sp)
                    Text(f.message, color = fg, fontSize = 13.sp)
                }
            }
        }
    }

    ToolCard(Icons.Filled.Description, "دوره‌های اخیر") {
        r.cycles.takeLast(8).reversed().forEach { cy ->
            Row(Modifier.fillMaxWidth()) {
                Text(jalaliLong(cy.start), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${cy.bleedingDays} روز خونریزی", fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(cy.length?.let { "چرخه $it روز" } ?: "در جریان", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    NoteText("این تحلیل بر اساس داده‌های ثبت‌شده توسط شماست و تشخیص پزشکی نیست. روزهای فراموش‌شده می‌توانند نتیجه را تغییر دهند.")
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------
// Doctor report (PDF)
// ---------------------------------------------------------------------------------------------

@Composable
internal fun DoctorReportTab(viewModel: HealthToolsViewModel) {
    val busy by viewModel.busy.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.exportPdf(uri)
    }
    ToolCard(Icons.Filled.PictureAsPdf, "گزارش PDF برای پزشک", tint = Color(0xFFDC2626)) {
        Text("یک گزارش مرتب و قابل چاپ برای ویزیت پزشک زنان شامل:", fontSize = 14.sp)
        listOf(
            "خلاصه چرخه: میانگین، کوتاه‌ترین/بلندترین، نوسان و روزهای خونریزی",
            "نکات قابل توجه (مثل بی‌نظمی یا خونریزی طولانی)",
            "تاریخچه ۸ دوره اخیر",
            "علائم پرتکرار ۹۰ روز اخیر با شدت",
            "فشار خون، قرص ضدبارداری، داروها، تست بارداری و آخرین خودآزمایی"
        ).forEach { SmallInfoRow(Icons.Filled.CheckCircle, it) }
        NoteText("اطلاعات رابطه جنسی در گزارش قرار نمی‌گیرد.")
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Button(
            enabled = !busy,
            onClick = {
                AppLockSession.skipNextLock = true
                launcher.launch("hanita-report-${LocalDate.now()}.pdf")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ساخت و ذخیره گزارش PDF") }
    }
}
