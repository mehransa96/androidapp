package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyLog
import com.example.ui.theme.*
import com.example.utils.JalaliCalendar
import com.example.utils.JalaliDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CalendarSelectionTarget {
    SMART_RANGE, // First tap sets Start, second tap sets End
    START_DATE,  // Explicitly selecting start date
    END_DATE     // Explicitly selecting end date
}

/**
 * Interactive Period Calendar Widget allowing users to select period start and end dates
 * through direct date tapping and range visualization on the Jalali calendar.
 */
@Composable
fun InteractivePeriodCalendarWidget(
    recordedLogs: List<DailyLog>,
    initialStartDate: LocalDate? = null,
    initialEndDate: LocalDate? = null,
    onSavePeriod: (startDate: LocalDate, endDate: LocalDate, flow: String, notes: String) -> Unit,
    onClearPeriod: ((startDate: LocalDate, endDate: LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val todayLocalDate = remember { LocalDate.now() }
    val todayJalali = remember(todayLocalDate) { JalaliCalendar.gregorianToJalali(todayLocalDate) }

    // Navigation Month State
    var currentMonth by remember {
        val initial = initialStartDate ?: todayLocalDate
        val jalali = JalaliCalendar.gregorianToJalali(initial)
        mutableStateOf(JalaliDate(jalali.year, jalali.month, 1))
    }

    // Selected Start and End Dates (Jalali)
    var selectedStartJalali by remember {
        mutableStateOf(initialStartDate?.let { JalaliCalendar.gregorianToJalali(it) })
    }
    var selectedEndJalali by remember {
        mutableStateOf(initialEndDate?.let { JalaliCalendar.gregorianToJalali(it) })
    }

    // Selection target mode
    var selectionTarget by remember { mutableStateOf(CalendarSelectionTarget.SMART_RANGE) }

    // Period flow and notes for the selected cycle
    var selectedFlow by remember { mutableStateOf("MEDIUM") }
    var cycleNotes by remember { mutableStateOf("") }
    var showNotesField by remember { mutableStateOf(false) }
    var futureTapHint by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // Past recorded period dates set for display
    val recordedPeriodDates = remember(recordedLogs) {
        recordedLogs.filter { it.periodFlow != "NONE" }.mapNotNull {
            runCatching { JalaliCalendar.gregorianToJalali(LocalDate.parse(it.dateStr)) }.getOrNull()
        }.toSet()
    }

    // Calculated range duration
    val selectedDurationDays: Int? = remember(selectedStartJalali, selectedEndJalali) {
        val start = selectedStartJalali
        val end = selectedEndJalali
        if (start != null && end != null) {
            val startLocal = start.toLocalDate()
            val endLocal = end.toLocalDate()
            (ChronoUnit.DAYS.between(startLocal, endLocal).toInt() + 1).coerceAtLeast(1)
        } else if (start != null) {
            1
        } else {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_period_calendar_widget"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Widget Title & Subtitle Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🩸", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "انتخاب لمسی دوره قاعدگی",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "برای شروع و پایان، تاریخ‌ها را لمس کنید",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Reset / Clear button
                if (selectedStartJalali != null || selectedEndJalali != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedStartJalali = null
                            selectedEndJalali = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "پاک کردن انتخاب",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selection Mode Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    CalendarSelectionTarget.SMART_RANGE to "انتخاب متوالی 🔄",
                    CalendarSelectionTarget.START_DATE to "روز شروع 🩸",
                    CalendarSelectionTarget.END_DATE to "روز پایان 🏁"
                )
                modes.forEach { (target, label) ->
                    val isSelected = selectionTarget == target
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectionTarget = target
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Month Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous month: first child, so it sits on the right in the RTL layout.
                IconButton(
                    onClick = {
                        var m = currentMonth.month - 1
                        var y = currentMonth.year
                        if (m < 1) {
                            m = 12
                            y--
                        }
                        currentMonth = JalaliDate(y, m, 1)
                    }
                ) {
                    // AutoMirrored: in the RTL layout this arrow points outward (right), toward the past.
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "ماه قبل",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentMonth.monthName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${currentMonth.year}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Button to jump back to today's month
                    if (currentMonth.year != todayJalali.year || currentMonth.month != todayJalali.month) {
                        SuggestionChip(
                            onClick = {
                                currentMonth = JalaliDate(todayJalali.year, todayJalali.month, 1)
                            },
                            label = { Text("برو به امروز", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        var m = currentMonth.month + 1
                        var y = currentMonth.year
                        if (m > 12) {
                            m = 1
                            y++
                        }
                        currentMonth = JalaliDate(y, m, 1)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "ماه بعد",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Headers (شنبه تا جمعه)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val daysOfWeek = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                daysOfWeek.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        fontWeight = FontWeight.Bold,
                        // Friday (the weekend) in the accent color, as in Persian wall calendars.
                        color = if (index == 6) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Grid Calculation
            val daysInMonth = currentMonth.lengthOfMonth
            val firstDayOfWeek = currentMonth.firstDayOfWeek() // 0 = Sat, 6 = Fri
            val offset = firstDayOfWeek
            val totalSlots = offset + daysInMonth
            val weeksCount = (totalSlots + 6) / 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (weekIndex in 0 until weeksCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            val slotIndex = weekIndex * 7 + dayOfWeek
                            if (slotIndex < offset || slotIndex >= totalSlots) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val dayNum = slotIndex - offset + 1
                                val date = JalaliDate(currentMonth.year, currentMonth.month, dayNum)

                                val isToday = date == todayJalali
                                val isStart = date == selectedStartJalali
                                val isEnd = date == selectedEndJalali
                                val isPastRecorded = recordedPeriodDates.contains(date)

                                val isInSelectedRange = remember(date, selectedStartJalali, selectedEndJalali) {
                                    val start = selectedStartJalali
                                    val end = selectedEndJalali
                                    if (start != null && end != null) {
                                        date >= start && date <= end
                                    } else {
                                        false
                                    }
                                }

                                val isFuture = date > todayJalali
                                DayCellItem(
                                    modifier = Modifier.weight(1f),
                                    date = date,
                                    dayNum = dayNum,
                                    isFuture = isFuture,
                                    isToday = isToday,
                                    isStart = isStart,
                                    isEnd = isEnd,
                                    isInRange = isInSelectedRange,
                                    isPastRecorded = isPastRecorded,
                                    onDateClick = {
                                        if (isFuture) {
                                            futureTapHint = true
                                            return@DayCellItem
                                        }
                                        futureTapHint = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (selectionTarget) {
                                            CalendarSelectionTarget.START_DATE -> {
                                                selectedStartJalali = date
                                                if (selectedEndJalali != null && date > selectedEndJalali!!) {
                                                    selectedEndJalali = date
                                                }
                                            }
                                            CalendarSelectionTarget.END_DATE -> {
                                                if (selectedStartJalali == null || date < selectedStartJalali!!) {
                                                    selectedStartJalali = date
                                                    selectedEndJalali = date
                                                } else {
                                                    selectedEndJalali = date
                                                }
                                            }
                                            CalendarSelectionTarget.SMART_RANGE -> {
                                                if (selectedStartJalali == null || (selectedStartJalali != null && selectedEndJalali != null)) {
                                                    selectedStartJalali = date
                                                    selectedEndJalali = null
                                                } else {
                                                    // Start date was set, now setting end date
                                                    if (date >= selectedStartJalali!!) {
                                                        selectedEndJalali = date
                                                    } else {
                                                        // Tapped earlier date, swap as new start
                                                        selectedEndJalali = selectedStartJalali
                                                        selectedStartJalali = date
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (futureTapHint) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "روزهای آینده را نمی‌توان به‌عنوان پریود ثبت کرد؛ پیش‌بینی‌ها در نمای ماهانه نشان داده می‌شوند.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Duration Preset Chips (when start date is selected)
            if (selectedStartJalali != null) {
                Text(
                    text = "تعیین سریع مدت دوره از روز شروع:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 4, 5, 6, 7).forEach { days ->
                        val isPresetActive = selectedDurationDays == days
                        val presetEnd = selectedStartJalali!!.toLocalDate().plusDays((days - 1).toLong())
                        val presetEnabled = !presetEnd.isAfter(todayLocalDate)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isPresetActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = if (presetEnabled) 0.08f else 0.03f)
                                )
                                .clickable(enabled = presetEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedEndJalali = JalaliCalendar.gregorianToJalali(presetEnd)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$days روز",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isPresetActive -> Color.White
                                    presetEnabled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Summary Card and Action Controls
            AnimatedVisibility(
                visible = selectedStartJalali != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Selected period dates summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "روز شروع قاعدگی:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedStartJalali?.formatShort() ?: "-",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            // Start is on the right in RTL, so the arrow must point left toward the end.
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )

                        Column {
                            Text(
                                text = "روز پایان قاعدگی:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (selectedEndJalali ?: selectedStartJalali)?.formatShort() ?: "-",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${selectedDurationDays ?: 1} روز",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Flow Intensity Selector
                    Column {
                        Text(
                            text = "شدت خونریزی این دوره:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("LIGHT", "خفیف 💧", MaterialTheme.colorScheme.secondary),
                                Triple("MEDIUM", "متوسط 🩸", MaterialTheme.colorScheme.primary),
                                Triple("HEAVY", "شدید 🔴", MaterialTheme.colorScheme.error)
                            ).forEach { (flowKey, flowLabel, flowColor) ->
                                val isSelected = selectedFlow == flowKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) flowColor
                                            else flowColor.copy(alpha = 0.1f)
                                        )
                                        .clickable { selectedFlow = flowKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = flowLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else flowColor
                                    )
                                }
                            }
                        }
                    }

                    // Optional Notes Toggle
                    if (showNotesField) {
                        OutlinedTextField(
                            value = cycleNotes,
                            onValueChange = { cycleNotes = it },
                            placeholder = { Text("یادداشت یا توضیحات این دوره (اختیاری)...", fontSize = 11.5.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        TextButton(
                            onClick = { showNotesField = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ افزودن یادداشت برای این چرخه", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val startJ = selectedStartJalali ?: return@Button
                                val endJ = selectedEndJalali ?: startJ
                                val startLocal = startJ.toLocalDate()
                                val endLocal = endJ.toLocalDate()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSavePeriod(startLocal, endLocal, selectedFlow, cycleNotes)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_period_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ثبت دوره قاعدگی 🌸",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (onClearPeriod != null) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    confirmClear = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف بازه", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear && onClearPeriod != null) {
        val startJ = selectedStartJalali
        val endJ = selectedEndJalali ?: startJ
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("پاک کردن روزهای پریود؟", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "علامت پریود از ${startJ?.formatShort() ?: "-"} تا ${endJ?.formatShort() ?: "-"} برداشته می‌شود. علائم و یادداشت‌های این روزها حذف نمی‌شوند.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    if (startJ != null && endJ != null) {
                        onClearPeriod(startJ.toLocalDate(), endJ.toLocalDate())
                        selectedStartJalali = null
                        selectedEndJalali = null
                    }
                }) { Text("پاک شود", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("انصراف") } }
        )
    }
}

/**
 * Single day cell representation in the interactive period calendar.
 */
@Composable
private fun DayCellItem(
    modifier: Modifier = Modifier,
    date: JalaliDate,
    dayNum: Int,
    isFuture: Boolean,
    isToday: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    isPastRecorded: Boolean,
    onDateClick: () -> Unit
) {
    val isEndpoint = isStart || isEnd

    val cellBgColor = when {
        isEndpoint -> MaterialTheme.colorScheme.primary
        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        isPastRecorded -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    val cellTextColor = when {
        isEndpoint -> Color.White
        isInRange -> MaterialTheme.colorScheme.primary
        isPastRecorded -> MaterialTheme.colorScheme.secondary
        isToday -> MaterialTheme.colorScheme.primary
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val scale by animateFloatAsState(
        targetValue = if (isEndpoint) 1.12f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dayScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .scale(scale)
            .clip(
                if (isEndpoint || (!isInRange && !isPastRecorded)) CircleShape
                else RoundedCornerShape(10.dp)
            )
            .background(cellBgColor)
            .clickable(onClick = onDateClick)
            .testTag("day_cell_${date.year}_${date.month}_${date.day}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNum",
                fontSize = 13.5.sp,
                fontWeight = if (isEndpoint || isToday || isPastRecorded || isInRange) FontWeight.Bold else FontWeight.Normal,
                color = cellTextColor
            )

            // Micro Indicators below day number
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isStart) {
                    Text("شروع", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                } else if (isEnd) {
                    Text("پایان", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                } else if (isPastRecorded) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                } else if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
