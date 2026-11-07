package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.utils.JalaliCalendar
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * High-craft, comprehensive Period & Cycle Editing Dialog.
 * Allows users to edit start date, end date/duration, cycle length, flow, and notes
 * with Persian Jalali Date Pickers and live physiological preview.
 */
@Composable
fun ProfessionalCycleEditDialog(
    initialStartDate: LocalDate = LocalDate.now(),
    initialEndDate: LocalDate? = null,
    initialCycleLength: Int = 28,
    initialFlow: String = "MEDIUM",
    initialNotes: String = "",
    isExistingCycle: Boolean = false,
    title: String = if (isExistingCycle) "ویرایش دوره قاعدگی 🩸✏️" else "ثبت یا ویرایش دوره جدید 🩸",
    onDismissRequest: () -> Unit,
    onSaveCycle: (startDate: LocalDate, endDate: LocalDate, cycleLength: Int, flow: String, notes: String) -> Unit,
    onDeleteCycle: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    // Bleeding cannot be logged ahead of time: a default end such as "start + 4 days" used to
    // write future days that later counted as real bleeding.
    var startDate by remember { mutableStateOf(earlierOf(initialStartDate, LocalDate.now())) }
    var endDate by remember {
        mutableStateOf(laterOf(earlierOf(initialEndDate ?: initialStartDate.plusDays(4), LocalDate.now()), earlierOf(initialStartDate, LocalDate.now())))
    }
    var cycleLength by remember { mutableIntStateOf(initialCycleLength.coerceIn(21, 45)) }
    var flow by remember { mutableStateOf(initialFlow) }
    var notes by remember { mutableStateOf(initialNotes) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Ensure start is not after end
    val periodDurationDays = remember(startDate, endDate) {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        days.coerceAtLeast(1)
    }

    val startJalali = remember(startDate) {
        JalaliCalendar.gregorianToJalali(startDate)
    }
    val endJalali = remember(endDate) {
        JalaliCalendar.gregorianToJalali(endDate)
    }

    // Physiological calculations
    val estimatedNextPeriod = remember(startDate, cycleLength) {
        startDate.plusDays(cycleLength.toLong())
    }
    val nextPeriodJalali = remember(estimatedNextPeriod) {
        JalaliCalendar.gregorianToJalali(estimatedNextPeriod)
    }
    val estimatedOvulation = remember(estimatedNextPeriod) {
        estimatedNextPeriod.minusDays(14)
    }
    val ovulationJalali = remember(estimatedOvulation) {
        JalaliCalendar.gregorianToJalali(estimatedOvulation)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("professional_cycle_edit_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SleekPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تنظیم و اصلاح تاریخ‌های قاعدگی و طول چرخه",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Live Physiological Summary Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "خلاصه محاسبات این دوره 📊",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("طول خونریزی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$periodDurationDays روز", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("طول چرخه", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$cycleLength روز", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تخمک‌گذاری بعدی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(ovulationJalali.formatShort(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "پیش‌بینی شروع دوره بعد: ${nextPeriodJalali.formatLong()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Start Date Picker Section
                    Column {
                        Text(
                            text = "تاریخ شروع قاعدگی (پریود):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showStartDatePicker = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = startJalali.formatLong(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "معادل میلادی: $startDate",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SleekPrimary.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(13.dp))
                                        Text("ویرایش تاریخ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // 3. End Date Picker Section
                    Column {
                        Text(
                            text = "تاریخ پایان قاعدگی (پاکی):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showEndDatePicker = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = endJalali.formatLong(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "معادل میلادی: $endDate ($periodDurationDays روز پریود)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                        Text("ویرایش تاریخ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }

                    // 4. Quick Duration Stepper for Period (تعداد روزهای پریود)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تنظیم سریع طول پریود:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3, 4, 5, 6, 7).forEach { days ->
                                val isSelected = periodDurationDays == days
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            endDate = startDate.plusDays((days - 1).toLong())
                                        }
                                ) {
                                    Text(
                                        text = "$days روز",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 5. Total Cycle Length Stepper (طول کل چرخه قاعدگی)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "طول کل چرخه قاعدگی (روز):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$cycleLength روز",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (cycleLength > 21) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        cycleLength -= 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "کاهش")
                            }

                            Slider(
                                value = cycleLength.toFloat(),
                                onValueChange = {
                                    cycleLength = it.toInt()
                                },
                                valueRange = 21f..45f,
                                steps = 23,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (cycleLength < 45) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        cycleLength += 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "افزایش")
                            }
                        }

                        // Presets for cycle length
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(26, 28, 30, 32).forEach { preset ->
                                val isSelected = cycleLength == preset
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        cycleLength = preset
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("$preset روز", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // 6. Menstrual Flow Intensity
                    Column {
                        Text(
                            text = "شدت عمومی خونریزی در این دوره:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val flowOptions = listOf(
                                "SPOT" to "لکه بینی 💧",
                                "LIGHT" to "خفیف 🌸",
                                "MEDIUM" to "متوسط 🩸",
                                "HEAVY" to "شدید 🩸🩸"
                            )
                            flowOptions.forEach { (key, label) ->
                                val isSelected = flow == key
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            flow = key
                                        },
                                    color = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) SleekPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 7. Notes Text Field
                    Column {
                        Text(
                            text = "یادداشت‌های اختصاصی این دوره (اختیاری):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("مثلاً: علائم PMS ملایم بود، شروع با لکه‌بینی، مصرف قرص آهن", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isExistingCycle && onDeleteCycle != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف دوره",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("لغو", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val today = LocalDate.now()
                            val safeStart = earlierOf(startDate, today)
                            val validEnd = laterOf(
                                safeStart,
                                if (endDate.isBefore(safeStart)) earlierOf(safeStart.plusDays(4), today) else earlierOf(endDate, today)
                            )
                            startDate = safeStart
                            onSaveCycle(safeStart, validEnd, cycleLength, flow, notes)
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره تغییرات چرخه", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Date Pickers
    if (showStartDatePicker) {
        ProfessionalDatePickerDialog(
            initialDate = startDate,
            title = "انتخاب تاریخ شروع قاعدگی 🩸",
            subtitle = "شروع پریود خود را مشخص نمایید",
            maxDate = LocalDate.now(),
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { picked ->
                startDate = picked
                if (endDate.isBefore(picked)) {
                    endDate = laterOf(earlierOf(picked.plusDays(4), LocalDate.now()), picked)
                }
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        ProfessionalDatePickerDialog(
            initialDate = endDate,
            title = "انتخاب تاریخ پایان قاعدگی (پاکی) ✨",
            subtitle = "پایان خونریزی این دوره را مشخص نمایید",
            minDate = startDate,
            // A period cannot end in the future.
            maxDate = laterOf(startDate, LocalDate.now()),
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { picked ->
                if (picked.isBefore(startDate)) {
                    startDate = picked
                    endDate = laterOf(earlierOf(picked.plusDays(4), LocalDate.now()), picked)
                } else {
                    endDate = picked
                }
                showEndDatePicker = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && onDeleteCycle != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("حذف این دوره قاعدگی؟", fontWeight = FontWeight.Bold) },
            text = {
                Text("آیا مطمئن هستید که می‌خواهید این دوره ثبت‌شده از تاریخچه پاک شود؟ تمام روزهای مربوطه به حالت عادی باز خواهند گشت.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteCycle()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

/** LocalDate is Comparable<ChronoLocalDate>, so the stdlib minOf/maxOf do not apply to it. */
internal fun earlierOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) b else a
internal fun laterOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isBefore(b)) b else a
