package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthEntry
import com.example.data.model.HealthEntryType
import com.example.domain.BloodPressure
import com.example.domain.FertilityChance
import com.example.security.AppLockManager
import com.example.ui.components.ProfessionalDatePickerDialog
import com.example.ui.viewmodel.HealthToolsViewModel
import com.example.utils.JalaliCalendar
import java.time.LocalDate

/** Persian labels for the codes stored in [HealthEntry.value]. */
internal object HealthLabels {
    val intimacy = linkedMapOf("PROTECTED" to "با محافظت", "UNPROTECTED" to "بدون محافظت")
    val mucus = linkedMapOf(
        "DRY" to "خشک / بدون ترشح",
        "STICKY" to "چسبنده",
        "CREAMY" to "کرمی",
        "WATERY" to "آبکی",
        "EGG_WHITE" to "شفاف و کشسان (سفیده تخم‌مرغی)"
    )
    val ovulationTest = linkedMapOf("NEGATIVE" to "منفی", "POSITIVE" to "مثبت (اوج LH)")
    val pregnancyTest = linkedMapOf("NEGATIVE" to "منفی", "UNCLEAR" to "کم‌رنگ / نامشخص", "POSITIVE" to "مثبت")
    val breastExam = linkedMapOf("NORMAL" to "مورد غیرعادی ندیدم", "CONCERN" to "مورد نگران‌کننده دیدم")

    fun describe(e: HealthEntry): String = when (e.type) {
        HealthEntryType.INTIMACY -> "رابطه: ${intimacy[e.value] ?: e.value}"
        HealthEntryType.MUCUS -> "ترشحات: ${mucus[e.value] ?: e.value}"
        HealthEntryType.OVULATION_TEST -> "تست تخمک‌گذاری: ${ovulationTest[e.value] ?: e.value}"
        HealthEntryType.PREGNANCY_TEST -> "تست بارداری: ${pregnancyTest[e.value] ?: e.value}"
        HealthEntryType.BLOOD_PRESSURE -> {
            val sys = e.num1 ?: 0
            val dia = e.num2 ?: 0
            val pulse = e.value.toIntOrNull()?.let { " · نبض $it" } ?: ""
            "فشار خون: $sys/$dia$pulse (${BloodPressure.classify(sys, dia).label})"
        }
        HealthEntryType.PILL -> "قرص مصرف شد"
        HealthEntryType.BREAST_EXAM -> "خودآزمایی پستان: ${breastExam[e.value] ?: e.value}"
        HealthEntryType.SLEEP -> "خواب: ${"%.1f".format(java.util.Locale.US, (e.num1 ?: 0) / 60f)} ساعت"
        HealthEntryType.STEPS -> "قدم‌ها: ${e.num1 ?: 0}"
        else -> e.type
    }
}

internal fun jalaliShort(date: LocalDate): String = JalaliCalendar.gregorianToJalali(date).formatShort()
internal fun jalaliLong(date: LocalDate): String = JalaliCalendar.gregorianToJalali(date).formatLong()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthToolsScreen(viewModel: HealthToolsViewModel, onNavigateBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("ثبت روزانه", "قرص", "خودآزمایی", "تحلیل چرخه", "گزارش پزشک")
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("ابزارهای سلامت", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                    }
                )
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                    tabs.forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            when (tab) {
                0 -> DailyHealthTab(viewModel)
                1 -> PillTab(viewModel)
                2 -> BreastExamTab(viewModel)
                3 -> CycleInsightsTab(viewModel)
                else -> DoctorReportTab(viewModel)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun ToolCard(icon: ImageVector, title: String, tint: Color = MaterialTheme.colorScheme.primary, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            content()
        }
    }
}

@Composable
internal fun NoteText(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceChips(options: Map<String, String>, selected: String?, onSelect: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (code, label) ->
            FilterChip(
                selected = selected == code,
                // Tapping the selected chip again clears the day's value.
                onClick = { onSelect(if (selected == code) null else code) },
                label = { Text(label, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun DailyHealthTab(viewModel: HealthToolsViewModel) {
    val entries by viewModel.entries.collectAsState()
    val cycle by viewModel.cycle.collectAsState()
    val today = remember { LocalDate.now() }
    var date by remember { mutableStateOf(today) }
    var pickDate by remember { mutableStateOf(false) }
    val dateStr = date.toString()
    val dayEntries = entries.filter { it.dateStr == dateStr }
    fun valueOf(type: String) = dayEntries.firstOrNull { it.type == type }?.value

    // Date selector
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("ثبت برای ${if (date == today) "امروز" else jalaliLong(date)}", fontWeight = FontWeight.Bold)
                NoteText("برای روزهای قبل، تاریخ را تغییر دهید.")
            }
            OutlinedButton(onClick = { pickDate = true }) { Text("تغییر تاریخ") }
        }
    }
    if (pickDate) {
        ProfessionalDatePickerDialog(
            initialDate = date,
            title = "روز موردنظر",
            maxDate = today,
            onDismissRequest = { pickDate = false },
            onDateSelected = { date = it; pickDate = false }
        )
    }

    // Chance of pregnancy (calendar estimate) for today
    cycle?.let { c ->
        if (date == today && c.prediction.lastPeriodStart != null) {
            val level = FertilityChance.level(c.today.cycleDay, c.prediction.averageCycleLength, c.today.isPeriod)
            val color = when (level) {
                FertilityChance.Level.HIGH -> Color(0xFFE11D48)
                FertilityChance.Level.MEDIUM -> Color(0xFFF59E0B)
                FertilityChance.Level.LOW -> Color(0xFF10B981)
            }
            ToolCard(Icons.Filled.Favorite, "احتمال بارداری امروز: ${level.label}", tint = color) {
                Text("روز ${c.today.cycleDay} چرخه · میانگین چرخه ${c.prediction.averageCycleLength} روز", fontSize = 13.sp)
                if (c.today.isLate) {
                    NoteText("پریود شما ${c.today.daysLate} روز تأخیر دارد؛ در این حالت تقویم قابل اتکا نیست. اگر احتمال بارداری هست، تست بارداری بدهید.")
                }
                NoteText(
                    "این تخمین فقط بر اساس تقویم است و روش پیشگیری از بارداری نیست. در چرخه‌های نامنظم دقت آن کم است؛ " +
                        "ثبت ترشحات و تست تخمک‌گذاری دقت را بیشتر می‌کند."
                )
            }
        }
    }

    ToolCard(Icons.Filled.Favorite, "رابطه جنسی", tint = Color(0xFFE11D48)) {
        val intimacy = dayEntries.filter { it.type == HealthEntryType.INTIMACY }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HealthLabels.intimacy.forEach { (code, label) ->
                OutlinedButton(onClick = {
                    viewModel.addEntry(HealthEntry(dateStr = dateStr, type = HealthEntryType.INTIMACY, value = code))
                }) { Text("+ $label", fontSize = 13.sp) }
            }
        }
        if (intimacy.isNotEmpty()) NoteText("ثبت‌شده در این روز: ${intimacy.size} مورد")
        NoteText("این اطلاعات فقط روی گوشی شما می‌ماند و در هیچ گزارشی بدون اجازه شما نمایش داده نمی‌شود.")
    }

    ToolCard(Icons.Filled.WaterDrop, "ترشحات دهانه رحم", tint = Color(0xFF0EA5E9)) {
        ChoiceChips(HealthLabels.mucus, valueOf(HealthEntryType.MUCUS)) { code ->
            if (code == null) viewModel.clearDay(date, HealthEntryType.MUCUS)
            else viewModel.addEntry(HealthEntry(dateStr = dateStr, type = HealthEntryType.MUCUS, value = code))
        }
        NoteText("ترشحات شفاف و کشسان معمولاً نزدیک تخمک‌گذاری دیده می‌شود. ترشح بدبو، سبز/زرد یا همراه خارش را با پزشک بررسی کنید.")
    }

    ToolCard(Icons.Filled.Science, "تست‌ها", tint = Color(0xFF8B5CF6)) {
        Text("تست تخمک‌گذاری (LH)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        ChoiceChips(HealthLabels.ovulationTest, valueOf(HealthEntryType.OVULATION_TEST)) { code ->
            if (code == null) viewModel.clearDay(date, HealthEntryType.OVULATION_TEST)
            else viewModel.addEntry(HealthEntry(dateStr = dateStr, type = HealthEntryType.OVULATION_TEST, value = code))
        }
        NoteText("تست مثبت یعنی تخمک‌گذاری احتمالاً طی ۱۲ تا ۳۶ ساعت آینده رخ می‌دهد.")
        Spacer(Modifier.height(4.dp))
        Text("تست بارداری", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        val pregnancy = valueOf(HealthEntryType.PREGNANCY_TEST)
        ChoiceChips(HealthLabels.pregnancyTest, pregnancy) { code ->
            if (code == null) viewModel.clearDay(date, HealthEntryType.PREGNANCY_TEST)
            else viewModel.addEntry(HealthEntry(dateStr = dateStr, type = HealthEntryType.PREGNANCY_TEST, value = code))
        }
        when (pregnancy) {
            "POSITIVE" -> NoteText("برای تأیید (آزمایش خون بتا) و شروع مراقبت‌های بارداری به پزشک یا ماما مراجعه کنید. می‌توانید حالت بارداری را از داشبورد فعال کنید.")
            "UNCLEAR", "NEGATIVE" -> NoteText("اگر پریود هنوز نیامده، ۲ تا ۳ روز بعد با ادرار صبحگاهی دوباره تست بدهید.")
            else -> {}
        }
    }

    BloodPressureCard(date, dateStr, viewModel)

    if (dayEntries.isNotEmpty()) {
        ToolCard(Icons.Filled.CalendarMonth, "موارد ثبت‌شده در ${jalaliShort(date)}") {
            dayEntries.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(HealthLabels.describe(e), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.deleteEntry(e.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun BloodPressureCard(date: LocalDate, dateStr: String, viewModel: HealthToolsViewModel) {
    var sys by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<BloodPressure.Category?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val entries by viewModel.entries.collectAsState()
    val recent = remember(entries) { entries.filter { it.type == HealthEntryType.BLOOD_PRESSURE }.take(3) }

    ToolCard(Icons.Filled.MonitorHeart, "فشار خون", tint = Color(0xFFDC2626)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(sys, "سیستولیک", Modifier.weight(1f)) { sys = it; error = null }
            NumberField(dia, "دیاستولیک", Modifier.weight(1f)) { dia = it; error = null }
            NumberField(pulse, "نبض (اختیاری)", Modifier.weight(1f)) { pulse = it }
        }
        Button(onClick = {
            val s = sys.toIntOrNull()
            val d = dia.toIntOrNull()
            if (s == null || d == null || !BloodPressure.isValid(s, d)) {
                error = "عددها را بررسی کنید (مثلاً ۱۲۰ روی ۸۰)."
                result = null
            } else {
                val p = pulse.toIntOrNull()?.takeIf { it in 30..220 }
                viewModel.addEntry(
                    HealthEntry(dateStr = dateStr, type = HealthEntryType.BLOOD_PRESSURE, value = p?.toString() ?: "", num1 = s, num2 = d)
                )
                result = BloodPressure.classify(s, d)
                sys = ""; dia = ""; pulse = ""
            }
        }) { Text("ثبت فشار خون${if (date != LocalDate.now()) " برای ${jalaliShort(date)}" else ""}") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        result?.let { cat ->
            val urgent = cat == BloodPressure.Category.CRISIS || cat == BloodPressure.Category.STAGE_2
            Text(
                "نتیجه: ${cat.label}",
                fontWeight = FontWeight.Bold,
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(cat.advice, fontSize = 13.sp)
        }
        if (recent.isNotEmpty()) {
            NoteText("آخرین ثبت‌ها: " + recent.joinToString(" · ") { "${it.num1}/${it.num2} (${jalaliShort(LocalDate.parse(it.dateStr))})" })
        }
        NoteText("در حالت نشسته و پس از ۵ دقیقه استراحت اندازه بگیرید. در بارداری، فشار ۱۴۰/۹۰ یا بالاتر را فوراً به پزشک اطلاع دهید.")
    }
}

@Composable
internal fun NumberField(value: String, label: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(AppLockManager.normalizeDigits(it).take(3)) },
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
internal fun SmallInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp)
    }
}
