package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.utils.JalaliCalendar
import com.example.utils.JalaliDate
import java.time.LocalDate

/**
 * Professional, highly-polished Persian (Jalali) & Gregorian Date Picker Dialog.
 * Provides quick shortcuts, intuitive month/year navigation, and calendar grid selection.
 */
@Composable
fun ProfessionalDatePickerDialog(
    initialDate: LocalDate = LocalDate.now(),
    title: String = "انتخاب تاریخ",
    subtitle: String = "تقویم هوشمند هانیتا",
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now() }
    val todayJalali = remember(today) { JalaliCalendar.gregorianToJalali(today) }

    // minDate / maxDate bound the choice (for example no future period or symptom entries).
    fun isAllowed(date: LocalDate): Boolean =
        (minDate == null || !date.isBefore(minDate)) && (maxDate == null || !date.isAfter(maxDate))

    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedJalali by remember {
        mutableStateOf(JalaliCalendar.gregorianToJalali(initialDate))
    }

    var viewingYear by remember { mutableIntStateOf(selectedJalali.year) }
    var viewingMonth by remember { mutableIntStateOf(selectedJalali.month) }
    var isYearPickerOpen by remember { mutableStateOf(false) }

    val monthNames = remember {
        listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
    }

    val availableYears = remember(todayJalali.year) {
        ((todayJalali.year - 4)..(todayJalali.year + 2)).toList().reversed()
    }

    // Days in current viewing month
    val daysInViewingMonth = remember(viewingYear, viewingMonth) {
        val dummy = JalaliDate(viewingYear, viewingMonth, 1)
        dummy.lengthOfMonth
    }

    // First day of week for the current viewing month (Saturday = 0 .. Friday = 6)
    val firstDayOffset = remember(viewingYear, viewingMonth) {
        val dummy = JalaliDate(viewingYear, viewingMonth, 1)
        dummy.firstDayOfWeek()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("professional_date_picker_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Selection Preview Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "تاریخ انتخابی:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedJalali.day} ${selectedJalali.monthName} ${selectedJalali.year}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "میلادی: $selectedDate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips
                Text(
                    text = "انتخاب سریع:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "امروز" to today,
                        "دیروز" to today.minusDays(1),
                        "۳ روز پیش" to today.minusDays(3),
                        "۱ هفته پیش" to today.minusWeeks(1),
                        "۲ هفته پیش" to today.minusWeeks(2),
                        "ماه گذشته" to today.minusMonths(1)
                    )

                    items(presets.filter { isAllowed(it.second) }) { (label, presetDate) ->
                        val isSelected = selectedDate == presetDate
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedDate = presetDate
                                    val j = JalaliCalendar.gregorianToJalali(presetDate)
                                    selectedJalali = j
                                    viewingYear = j.year
                                    viewingMonth = j.month
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month / Year Navigation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Month (Goes forward in Gregorian, earlier in calendar time)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (viewingMonth > 1) {
                                viewingMonth -= 1
                            } else {
                                viewingMonth = 12
                                viewingYear -= 1
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            // AutoMirrored ArrowBack points right in RTL: outward, toward the past.
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ماه قبل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Month & Year Selector Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isYearPickerOpen = !isYearPickerOpen }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${monthNames[viewingMonth - 1]} $viewingYear",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (isYearPickerOpen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Next Month
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (viewingMonth < 12) {
                                viewingMonth += 1
                            } else {
                                viewingMonth = 1
                                viewingYear += 1
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "ماه بعد",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Year selector expansion
                AnimatedVisibility(visible = isYearPickerOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "انتخاب سال شمسی:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableYears) { yr ->
                                val isCur = yr == viewingYear
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewingYear = yr
                                            isYearPickerOpen = false
                                        }
                                ) {
                                    Text(
                                        text = yr.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCur) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days of week header (Saturday to Friday)
                val dayHeaders = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayHeaders.forEach { header ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = header,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (header == "ج") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Grid Days
                val totalSlots = firstDayOffset + daysInViewingMonth
                val totalRows = (totalSlots + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val slotIndex = row * 7 + col
                                val dayNumber = slotIndex - firstDayOffset + 1

                                if (dayNumber in 1..daysInViewingMonth) {
                                    val currentSlotJalali = JalaliDate(viewingYear, viewingMonth, dayNumber)
                                    val currentSlotLocal = currentSlotJalali.toLocalDate()

                                    val isSelected = selectedJalali.year == viewingYear &&
                                            selectedJalali.month == viewingMonth &&
                                            selectedJalali.day == dayNumber

                                    val isToday = todayJalali.year == viewingYear &&
                                            todayJalali.month == viewingMonth &&
                                            todayJalali.day == dayNumber

                                    val isFriday = col == 6
                                    val isEnabled = isAllowed(currentSlotLocal)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = isEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedJalali = currentSlotJalali
                                                selectedDate = currentSlotLocal
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                isToday -> MaterialTheme.colorScheme.primary
                                                isFriday -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("انصراف", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDateSelected(selectedDate)
                        },
                        enabled = isAllowed(selectedDate),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تایید و انتخاب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
