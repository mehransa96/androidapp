package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhysicalSymptomLog
import com.example.data.model.CycleRecord
import com.example.ui.components.CalendarCyclePhase
import com.example.ui.components.InteractivePeriodCalendarWidget
import com.example.ui.components.VisualDailySymptomLogger
import com.example.ui.components.ProfessionalCycleEditDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MahitaViewModel
import com.example.utils.JalaliCalendar
import com.example.utils.JalaliDate
import java.time.LocalDate

enum class CyclePhaseType {
    PERIOD, FOLLICULAR, FERTILE, PMS, REGULAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MahitaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Follows the date change at midnight while the calendar stays open.
    val todayLocalDate by viewModel.currentDate.collectAsState()
    val todayJalali = remember(todayLocalDate) { JalaliCalendar.gregorianToJalali(todayLocalDate) }
    var currentMonth by remember { mutableStateOf(JalaliDate(todayJalali.year, todayJalali.month, 1)) }
    val medReminders by viewModel.medicationReminders.collectAsState()
    
    val logs by viewModel.allLogs.collectAsState()
    val allPhysicalSymptoms by viewModel.allPhysicalSymptoms.collectAsState()
    val cycleRecords by viewModel.allCycleRecords.collectAsState()
    val averageCycleLength by viewModel.averageCycleLength.collectAsState()

    // Symptoms grouped by dateStr from Room local database
    val symptomsByDate = remember(allPhysicalSymptoms) {
        allPhysicalSymptoms.groupBy { it.dateStr }
    }

    // Active tab: 0 = monthly overview & daily notes (default, what people open a calendar for),
    // 1 = touch selection of a period's start and end.
    var selectedTab by remember { mutableIntStateOf(0) }
    var cycleToDelete by remember { mutableStateOf<CycleRecord?>(null) }
    val logsByDate = remember(logs) { logs.associateBy { it.dateStr } }

    // Pre-selected range for the interactive widget
    var interactiveInitialStart by remember { mutableStateOf<LocalDate?>(null) }
    var interactiveInitialEnd by remember { mutableStateOf<LocalDate?>(null) }

    // Professional cycle edit states
    var cycleToEditInCalendar by remember { mutableStateOf<CycleRecord?>(null) }
    var showNewCycleDialog by remember { mutableStateOf(false) }
    
    // Convert logs into a set of period dates for quick lookup
    val periodDates = remember(logs) {
        logs.filter { it.periodFlow != "NONE" }.mapNotNull {
            runCatching { JalaliCalendar.gregorianToJalali(LocalDate.parse(it.dateStr)) }.getOrNull()
        }.toSet()
    }
    
    // Forecast for the period, fertile, PMS and follicular days. It comes from the same
    // CyclePredictor result as the dashboard, so both screens always agree (real average cycle
    // and period length, anchored on the first day of the last period, late periods handled).
    val cyclePrediction by viewModel.cyclePrediction.collectAsState()
    val forecast = remember(cyclePrediction, todayLocalDate) {
        val period = mutableSetOf<JalaliDate>()
        val fertile = mutableSetOf<JalaliDate>()
        val pms = mutableSetOf<JalaliDate>()
        val follicular = mutableSetOf<JalaliDate>()
        val lastStart = cyclePrediction.lastPeriodStart
        if (lastStart != null) {
            val horizon = todayLocalDate.plusDays(6L * cyclePrediction.averageCycleLength)
            var date: LocalDate = lastStart
            while (!date.isAfter(horizon)) {
                val status = com.example.domain.CyclePredictor.dayStatus(
                    date = date,
                    prediction = cyclePrediction,
                    loggedBleedingToday = false,
                    today = todayLocalDate
                )
                // Past days only show what was really logged; the forecast starts today.
                val jalali = JalaliCalendar.gregorianToJalali(date)
                val isForecastDay = !date.isBefore(todayLocalDate)
                when {
                    status.isPeriod -> if (isForecastDay) period.add(jalali)
                    status.isFertile -> fertile.add(jalali)
                    status.isPms -> pms.add(jalali)
                    status.phase == com.example.domain.CyclePredictor.Phase.FOLLICULAR -> follicular.add(jalali)
                }
                date = date.plusDays(1)
            }
        }
        listOf(period, fertile, pms, follicular)
    }
    val predictedPeriodDates: Set<JalaliDate> = forecast[0]
    val fertileDates: Set<JalaliDate> = forecast[1]
    val pmsDates: Set<JalaliDate> = forecast[2]
    val follicularDates: Set<JalaliDate> = forecast[3]

    var selectedDate by remember { mutableStateOf<JalaliDate?>(todayJalali) }

    val selectedDatePhaseType = remember(selectedDate, periodDates, predictedPeriodDates, fertileDates, pmsDates, follicularDates) {
        derivedStateOf {
            val date = selectedDate
            if (date == null) null
            else {
                val isPeriod = periodDates.contains(date) || predictedPeriodDates.contains(date)
                val isFertile = fertileDates.contains(date) && !isPeriod
                val isPms = pmsDates.contains(date) && !isPeriod && !isFertile
                val isFollicular = follicularDates.contains(date) && !isPeriod && !isFertile && !isPms
                
                when {
                    isPeriod -> CyclePhaseType.PERIOD
                    isFollicular -> CyclePhaseType.FOLLICULAR
                    isFertile -> CyclePhaseType.FERTILE
                    isPms -> CyclePhaseType.PMS
                    else -> CyclePhaseType.REGULAR
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تقویم هوشمند قاعدگی هانیتا",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showNewCycleDialog = true }) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = "ثبت یا ویرایش دوره جدید",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Primary Screen Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to "نمای ماهانه و یادداشت‌ها 📅",
                    1 to "ثبت لمسی شروع و پایان 🩸"
                ).forEach { (tabIndex, tabTitle) ->
                    val isTabSelected = selectedTab == tabIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isTabSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedTab = tabIndex }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 12.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTabSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 1) {
                // ==========================================
                // TAB 1: Interactive Touch Period Selection Widget
                // ==========================================
                // Keyed on the preset range so "select range" from the monthly tab always applies.
                key(interactiveInitialStart, interactiveInitialEnd) {
                InteractivePeriodCalendarWidget(
                    recordedLogs = logs,
                    initialStartDate = interactiveInitialStart,
                    initialEndDate = interactiveInitialEnd,
                    onSavePeriod = { start, end, flow, notes ->
                        viewModel.savePeriodRange(start, end, flow, notes)
                        val startJ = JalaliCalendar.gregorianToJalali(start).formatShort()
                        val endJ = JalaliCalendar.gregorianToJalali(end).formatShort()
                        Toast.makeText(
                            context,
                            "دوره قاعدگی از $startJ تا $endJ با موفقیت ثبت شد ✨",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onClearPeriod = { start, end ->
                        viewModel.clearPeriodRange(start, end)
                        Toast.makeText(context, "اطلاعات دوره انتخابی پاک شد", Toast.LENGTH_SHORT).show()
                    }
                )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Recent Logged Cycles List
                if (cycleRecords.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "دوره‌های ثبت‌شده اخیر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextDark
                                )
                                Text(
                                    text = "${cycleRecords.size} دوره",
                                    fontSize = 11.sp,
                                    color = SleekTextLight
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            cycleRecords.take(5).forEach { record ->
                                val startJ = runCatching {
                                    JalaliCalendar.gregorianToJalali(LocalDate.parse(record.startDate)).formatShort()
                                }.getOrDefault(record.startDate)
                                val endJ = if (record.endDate.isNotBlank()) {
                                    runCatching {
                                        JalaliCalendar.gregorianToJalali(LocalDate.parse(record.endDate)).formatShort()
                                    }.getOrDefault(record.endDate)
                                } else null

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Column {
                                            Text(
                                                text = if (endJ != null) "از $startJ تا $endJ" else "شروع: $startJ",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SleekTextDark
                                            )
                                            if (record.notes.isNotBlank()) {
                                                Text(
                                                    text = record.notes,
                                                    fontSize = 10.5.sp,
                                                    color = SleekTextLight,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { cycleToEditInCalendar = record },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "ویرایش دوره",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { cycleToDelete = record },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // ==========================================
                // TAB 0: Overview Calendar & Daily Notes
                // ==========================================

                // Month Navigation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                var m = currentMonth.month - 1
                                var y = currentMonth.year
                                if (m < 1) {
                                    m = 12; y--
                                }
                                currentMonth = JalaliDate(y, m, 1)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = SleekBorder.copy(alpha = 0.3f))
                        ) {
                            // AutoMirrored: points right (outward, toward the past) in the RTL layout.
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه قبل", tint = SleekPrimary)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentMonth.monthName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = SleekPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${currentMonth.year}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekTextLight
                                )
                                if (currentMonth.year != todayJalali.year || currentMonth.month != todayJalali.month) {
                                    Surface(
                                        color = SleekPrimaryLight,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable {
                                            currentMonth = JalaliDate(todayJalali.year, todayJalali.month, 1)
                                            selectedDate = todayJalali
                                        }
                                    ) {
                                        Text(
                                            text = "امروز",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                var m = currentMonth.month + 1
                                var y = currentMonth.year
                                if (m > 12) {
                                    m = 1; y++
                                }
                                currentMonth = JalaliDate(y, m, 1)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = SleekBorder.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه بعد", tint = SleekPrimary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Days of week header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekSurface, shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp)
                ) {
                    val daysOfWeek = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                    // Same equal-width columns as the day grid so each letter sits above its column.
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEachIndexed { index, day ->
                            Text(
                                text = day,
                                fontWeight = FontWeight.Black,
                                color = if (index == 6) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else SleekTextLight,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                val daysInMonth = currentMonth.lengthOfMonth
                val firstDayOfWeek = currentMonth.firstDayOfWeek()
                val offset = firstDayOfWeek
                val totalSlots = offset + daysInMonth
                val weeksCount = (totalSlots + 6) / 7

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (weekIndex in 0 until weeksCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayOfWeek in 0 until 7) {
                                val slotIndex = weekIndex * 7 + dayOfWeek
                                if (slotIndex < offset || slotIndex >= totalSlots) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val dayIndex = slotIndex - offset
                                    val date = JalaliDate(currentMonth.year, currentMonth.month, dayIndex + 1)
                                    val localDateStr = date.toLocalDate().toString()
                                    val dayLog = logsByDate[localDateStr]
                                    val daySymptoms = symptomsByDate[localDateStr] ?: emptyList()
                                    
                                    val hasRecordedPeriodFlow = dayLog?.periodFlow != null && dayLog.periodFlow != "NONE"
                                    val hasSymptomLogs = daySymptoms.isNotEmpty() || (dayLog != null && dayLog.symptoms.isNotBlank())

                                    val isPeriod = hasRecordedPeriodFlow || periodDates.contains(date) || predictedPeriodDates.contains(date)
                                    val isFertile = fertileDates.contains(date) && !isPeriod
                                    val isPms = pmsDates.contains(date) && !isPeriod && !isFertile
                                    val isFollicular = follicularDates.contains(date) && !isPeriod && !isFertile && !isPms
                                    val isToday = date == todayJalali
                                    val isSelected = date == selectedDate
                                    
                                    val bgColor = when {
                                        isSelected -> SleekPrimary
                                        isPeriod -> SleekPrimaryLight
                                        isFollicular -> Color(0xFFE8F5E9)
                                        isFertile -> Indigo50
                                        isPms -> Amber50
                                        isToday -> SleekBorder.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }
                                    val textColor = when {
                                        isSelected -> Color.White
                                        isPeriod -> SleekPrimaryDark
                                        isFollicular -> Color(0xFF2E7D32)
                                        isFertile -> Indigo500
                                        isPms -> Amber500
                                        isToday -> SleekTextDark
                                        else -> SleekTextMedium
                                    }

                                    val animatedBgColor by animateColorAsState(
                                        targetValue = bgColor,
                                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                        label = "animatedBgColor"
                                    )
                                    val animatedTextColor by animateColorAsState(
                                        targetValue = textColor,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                        label = "animatedTextColor"
                                    )

                                    val itemScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.12f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "itemScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .scale(itemScale)
                                            .clip(CircleShape)
                                            .background(animatedBgColor)
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${dayIndex + 1}",
                                                color = animatedTextColor,
                                                fontWeight = if (isPeriod || isFertile || isPms || isFollicular || isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            
                                            // Badges and Indicators
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                // Flow droplet indicator
                                                if (hasRecordedPeriodFlow) {
                                                    Text("💧", fontSize = 7.sp)
                                                }
                                                // Symptoms badge indicator
                                                if (hasSymptomLogs) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (isSelected) 5.dp else 4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else Color(0xFF7C3AED))
                                                    )
                                                }
                                                // Phase indicator dot (if not selected and no flow/symptoms)
                                                if (!isSelected && !hasRecordedPeriodFlow && !hasSymptomLogs) {
                                                    if (isPeriod) {
                                                        Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(SleekPrimary))
                                                    } else if (isFollicular) {
                                                        Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                                    } else if (isFertile) {
                                                        Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(Indigo500))
                                                    } else if (isPms) {
                                                        Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(Amber500))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Summary Metrics Strip from Room local database
                val totalPeriodDaysInMonth = remember(logs, currentMonth) {
                    logs.count { log ->
                        val date = runCatching { LocalDate.parse(log.dateStr) }.getOrNull()
                        if (date != null) {
                            val jDate = JalaliCalendar.gregorianToJalali(date)
                            jDate.year == currentMonth.year && jDate.month == currentMonth.month && log.periodFlow != "NONE"
                        } else false
                    }
                }
                val totalSymptomDaysInMonth = remember(allPhysicalSymptoms, logs, currentMonth) {
                    val datesWithPhysical = allPhysicalSymptoms.mapNotNull { s ->
                        val date = runCatching { LocalDate.parse(s.dateStr) }.getOrNull()
                        if (date != null) {
                            val jDate = JalaliCalendar.gregorianToJalali(date)
                            if (jDate.year == currentMonth.year && jDate.month == currentMonth.month) s.dateStr else null
                        } else null
                    }
                    val datesWithDaily = logs.filter { it.symptoms.isNotBlank() }.mapNotNull { log ->
                        val date = runCatching { LocalDate.parse(log.dateStr) }.getOrNull()
                        if (date != null) {
                            val jDate = JalaliCalendar.gregorianToJalali(date)
                            if (jDate.year == currentMonth.year && jDate.month == currentMonth.month) log.dateStr else null
                        } else null
                    }
                    (datesWithPhysical + datesWithDaily).distinct().size
                }

                // The third metric used to count every symptom ever logged, under a "this month" strip.
                val totalSymptomsInMonth = remember(allPhysicalSymptoms, currentMonth) {
                    allPhysicalSymptoms.count { s ->
                        val date = runCatching { LocalDate.parse(s.dateStr) }.getOrNull()
                        date != null && JalaliCalendar.gregorianToJalali(date).let {
                            it.year == currentMonth.year && it.month == currentMonth.month
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🩸 پریود این ماه", fontSize = 11.sp, color = SleekTextMedium)
                            Text(text = "$totalPeriodDaysInMonth روز", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                        }
                        Box(modifier = Modifier.height(24.dp).width(1.dp).background(SleekBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📝 روزهای دارای علائم", fontSize = 11.sp, color = SleekTextMedium)
                            Text(text = "$totalSymptomDaysInMonth روز", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                        }
                        Box(modifier = Modifier.height(24.dp).width(1.dp).background(SleekBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🟣 علائم این ماه", fontSize = 11.sp, color = SleekTextMedium)
                            Text(text = "$totalSymptomsInMonth مورد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions for selected date
                if (selectedDate != null && !selectedDate!!.toLocalDate().isAfter(todayLocalDate)) {
                    val localDate = selectedDate!!.toLocalDate()
                    val selectedJalaliText = selectedDate!!.formatLong()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                interactiveInitialStart = localDate
                                // Never pre-select days after today.
                                interactiveInitialEnd = com.example.ui.components.earlierOf(localDate.plusDays(4), todayLocalDate)
                                selectedTab = 1
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انتخاب بازه شروع و پایان 🩸", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateCycleSettings(localDate.toString(), averageCycleLength)
                                Toast.makeText(context, "شروع پریود در $selectedJalaliText ثبت شد 🩸", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("شروع پریود", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                
                // Dynamic Phase Active Info Banner
                val phaseType = selectedDatePhaseType.value
                if (phaseType != null) {
                    val title = when (phaseType) {
                        CyclePhaseType.PERIOD -> "دوره قاعدگی (شروع چرخه جدید) 🩸"
                        CyclePhaseType.FOLLICULAR -> "فاز فولیکولار (بهار بدن و انرژی نو) 🌱"
                        CyclePhaseType.FERTILE -> "پنجره باروری و زمان تخمک‌گذاری 🥚✨"
                        CyclePhaseType.PMS -> "روزهای سندرم پیش از قاعدگی (PMS) 🌪️💛"
                        CyclePhaseType.REGULAR -> "فاز فعال معمول بدنی 🌸"
                    }
                    val description = when (phaseType) {
                        CyclePhaseType.PERIOD -> "رحم در حال پاکسازی و بازسازی دیواره خود است. سطح عمومی هورمون‌ها پایین است و استراحت کافی اهمیت بالایی دارد."
                        CyclePhaseType.FOLLICULAR -> "فولیکول‌ها در تخمدان در حال رشد هستند و استروژن به تدریج بالا می‌رود. تمرکز ذهنی و انرژی رو به صعود است."
                        CyclePhaseType.FERTILE -> "روزهای اوج شادابی! استروژن در بالاترین حد بیولوژیکی قرار دارد که منجر به افزایش فوق‌العاده سطح انرژی و انگیزه می‌شود."
                        CyclePhaseType.PMS -> "پروژسترون کاهش یافته و ممکن است علائم فیزیکی مانند خستگی خفیف یا نوسانات احساسی جزئی پیش آید."
                        CyclePhaseType.REGULAR -> "سطح عمومی هورمون‌ها در منظم‌ترین حالت ممکن قرار دارد. تمرکز فکری شما بالاست."
                    }
                    val recommendation = when (phaseType) {
                        CyclePhaseType.PERIOD -> "استراحت کافی، مصرف مکمل آهن، گرم نگه داشتن شکم و نوشیدن دمنوش بابونه یا زنجبیل پیشنهاد می‌شود."
                        CyclePhaseType.FOLLICULAR -> "تغذیه سرشار از پروتئین و آنتی‌اکسیدان، برنامه‌ریزی پروژه‌های جدید و فعالیت‌های ورزشی با انگیزه."
                        CyclePhaseType.FERTILE -> "زمان مناسب برای تمرینات پرنشاط، فعالیت‌های اجتماعی و تصمیم‌گیری‌های مهم کاری است."
                        CyclePhaseType.PMS -> "نوشیدن آب مداوم، کاهش مصرف نمک و شکر، یوگای سبک و مراقبه‌های ذهن‌آگاهی هانیتا توصیه می‌شود."
                        CyclePhaseType.REGULAR -> "تغذیه متعادل و فعالیت‌های بدنی منظم را برای حفظ این ثبات ادامه دهید."
                    }
                    val mainColor = when (phaseType) {
                        CyclePhaseType.PERIOD -> SleekPrimary
                        CyclePhaseType.FOLLICULAR -> Color(0xFF10B981)
                        CyclePhaseType.FERTILE -> Indigo500
                        CyclePhaseType.PMS -> Amber500
                        CyclePhaseType.REGULAR -> SleekSecondary
                    }
                    val containerColor = when (phaseType) {
                        CyclePhaseType.PERIOD -> SleekPrimaryLight
                        CyclePhaseType.FOLLICULAR -> Color(0xFFE8F5E9)
                        CyclePhaseType.FERTILE -> Indigo50
                        CyclePhaseType.PMS -> Amber50
                        CyclePhaseType.REGULAR -> SleekBorder.copy(alpha = 0.5f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = BorderStroke(1.5.dp, mainColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(mainColor)
                                )
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mainColor
                                )
                            }
                            
                            HorizontalDivider(color = mainColor.copy(alpha = 0.2f), thickness = 1.dp)
                            
                            Text(
                                text = description,
                                fontSize = 11.5.sp,
                                color = SleekTextMedium,
                                lineHeight = 19.sp
                            )
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface.copy(alpha = 0.6f)),
                                border = BorderStroke(0.5.dp, mainColor.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "💡 پیشنهاد هانیتا:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mainColor
                                    )
                                    Text(
                                        text = recommendation,
                                        fontSize = 10.5.sp,
                                        color = SleekTextLight,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Visual Daily Symptom & Phase Logger (Connected with Room Persistence)
                if (selectedDate != null) {
                    val selDate = selectedDate!!
                    val localDateStr = selDate.toLocalDate().toString()
                    val initialDailyLog = logsByDate[localDateStr]
                    val selJalaliText = selDate.formatLong()
                    val initialSymptoms = symptomsByDate[localDateStr] ?: emptyList()

                    val mappedCalendarPhase = when (selectedDatePhaseType.value) {
                        CyclePhaseType.PERIOD -> CalendarCyclePhase.MENSTRUAL
                        CyclePhaseType.FOLLICULAR -> CalendarCyclePhase.FOLLICULAR
                        CyclePhaseType.FERTILE -> CalendarCyclePhase.OVULATION
                        CyclePhaseType.PMS -> CalendarCyclePhase.LUTEAL
                        else -> CalendarCyclePhase.REGULAR
                    }

                    VisualDailySymptomLogger(
                        selectedDate = selDate,
                        cyclePhase = mappedCalendarPhase,
                        initialDailyLog = initialDailyLog,
                        initialSymptoms = initialSymptoms,
                        medReminders = medReminders,
                        onSave = { flow, symptoms, moods, notes, emoji ->
                            viewModel.savePhysicalSymptomForm(
                                dateStr = localDateStr,
                                symptoms = symptoms,
                                flow = flow,
                                // This form has no mood editor (it sends only the emoji), so keep the
                                // moods logged elsewhere; notes and emoji follow what is on screen.
                                moods = initialDailyLog?.moods?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
                                    ?: moods,
                                overallNotes = notes,
                                emoji = emoji,
                                replaceDayState = true
                            )
                            Toast.makeText(context, "اطلاعات روز $selJalaliText ذخیره شد ✨", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            viewModel.savePhysicalSymptomForm(
                                dateStr = localDateStr,
                                symptoms = emptyList(),
                                flow = "NONE",
                                moods = emptyList(),
                                overallNotes = "",
                                emoji = "",
                                replaceDayState = true
                            )
                            Toast.makeText(context, "اطلاعات روز $selJalaliText پاک شد", Toast.LENGTH_SHORT).show()
                        },
                        onToggleMedication = { medId, taken ->
                            viewModel.toggleMedicationTaken(localDateStr, medId, taken)
                        },
                        onSetPeriodStart = {
                            if (selDate.toLocalDate().isAfter(todayLocalDate)) {
                                Toast.makeText(context, "شروع پریود را نمی‌توان برای روزهای آینده ثبت کرد", Toast.LENGTH_SHORT).show()
                                return@VisualDailySymptomLogger
                            }
                            viewModel.updateCycleSettings(localDateStr, averageCycleLength)
                            Toast.makeText(context, "شروع چرخه قاعدگی برای $selJalaliText ثبت شد 🩸", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Color Legend Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "راهنمای فازها و نشانگرهای تقویم هانیتا",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = SleekTextDark,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LegendItem(color = SleekPrimaryLight, textColor = SleekPrimaryDark, dotColor = SleekPrimary, label = "روزهای دوره قاعدگی (پریود) (🩸)")
                        Spacer(modifier = Modifier.height(8.dp))
                        LegendItem(color = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32), dotColor = Color(0xFF10B981), label = "فاز فولیکولار و بهار بدن (🌱)")
                        Spacer(modifier = Modifier.height(8.dp))
                        LegendItem(color = Indigo50, textColor = Indigo500, dotColor = Indigo500, label = "پنجره باروری و تخمک‌گذاری (🥚)")
                        Spacer(modifier = Modifier.height(8.dp))
                        LegendItem(color = Amber50, textColor = Amber500, dotColor = Amber500, label = "سندرم پیش از قاعدگی (PMS) (🌪️)")
                        Spacer(modifier = Modifier.height(8.dp))
                        LegendItem(color = Color(0xFFF3E8FF), textColor = Color(0xFF7C3AED), dotColor = Color(0xFF7C3AED), label = "روزهای دارای علائم ثبت‌شده (🟣)")
                        Spacer(modifier = Modifier.height(8.dp))
                        LegendItem(color = SleekBorder, textColor = SleekTextDark, dotColor = SleekTextDark, label = "امروز")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (cycleToEditInCalendar != null) {
        val record = cycleToEditInCalendar!!
        val parsedStart = runCatching { LocalDate.parse(record.startDate) }.getOrNull() ?: LocalDate.now()
        val parsedEnd = if (record.endDate.isNotBlank()) {
            runCatching { LocalDate.parse(record.endDate) }.getOrNull()
        } else null

        ProfessionalCycleEditDialog(
            initialStartDate = parsedStart,
            initialEndDate = parsedEnd,
            initialCycleLength = averageCycleLength,
            // Use the flow really logged on the first day instead of always "MEDIUM".
            initialFlow = logsByDate[record.startDate]?.periodFlow?.takeIf { it != "NONE" } ?: "MEDIUM",
            initialNotes = record.notes,
            isExistingCycle = true,
            title = "ویرایش حرفه‌ای دوره قاعدگی 🩸",
            onDismissRequest = { cycleToEditInCalendar = null },
            onSaveCycle = { newStart, newEnd, length, flow, notes ->
                viewModel.updateFullCycleRecord(
                    oldRecordId = record.id,
                    oldStartDate = parsedStart,
                    oldEndDate = parsedEnd,
                    newStartDate = newStart,
                    newEndDate = newEnd,
                    cycleLength = length,
                    flow = flow,
                    notes = notes
                )
                cycleToEditInCalendar = null
                Toast.makeText(context, "تغییرات دوره با موفقیت در سیستم ثبت شد ✨", Toast.LENGTH_SHORT).show()
            },
            onDeleteCycle = {
                viewModel.deleteFullCycleRecord(record)
                cycleToEditInCalendar = null
                Toast.makeText(context, "دوره انتخابی حذف گردید", Toast.LENGTH_SHORT).show()
            }
        )
    }

    cycleToDelete?.let { record ->
        ConfirmDeleteCycleDialog(
            record = record,
            onConfirm = {
                viewModel.deleteFullCycleRecord(record)
                cycleToDelete = null
                Toast.makeText(context, "دوره انتخابی حذف شد", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { cycleToDelete = null }
        )
    }

    if (showNewCycleDialog) {
        // A *new* period starts today by default; pre-filling the last period's date made
        // "save" silently rewrite the previous cycle.
        val newStart = LocalDate.now()
        ProfessionalCycleEditDialog(
            initialStartDate = newStart,
            initialEndDate = newStart,
            initialCycleLength = averageCycleLength,
            title = "ثبت دوره جدید قاعدگی 🩸",
            onDismissRequest = { showNewCycleDialog = false },
            onSaveCycle = { newStart, newEnd, length, flow, notes ->
                viewModel.updateFullCycleRecord(
                    oldRecordId = null,
                    oldStartDate = null,
                    oldEndDate = null,
                    newStartDate = newStart,
                    newEndDate = newEnd,
                    cycleLength = length,
                    flow = flow,
                    notes = notes
                )
                showNewCycleDialog = false
                val startJ = JalaliCalendar.gregorianToJalali(newStart).formatShort()
                Toast.makeText(context, "دوره جدید با شروع $startJ با موفقیت ثبت شد ✨", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
private fun ConfirmDeleteCycleDialog(record: CycleRecord, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val startJ = runCatching { JalaliCalendar.gregorianToJalali(LocalDate.parse(record.startDate)).formatLong() }
        .getOrDefault(record.startDate)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف این دوره؟", fontWeight = FontWeight.Bold) },
        text = { Text("دوره‌ای که در $startJ شروع شده و روزهای پریود آن حذف می‌شود و پیش‌بینی‌ها دوباره محاسبه می‌شوند.", lineHeight = 22.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("حذف شود", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun LegendItem(color: Color, textColor: Color, dotColor: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, color = SleekTextMedium, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}
