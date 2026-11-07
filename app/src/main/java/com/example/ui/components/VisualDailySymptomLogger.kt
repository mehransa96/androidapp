package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.DailyLog
import com.example.data.model.MedicineReminder
import com.example.data.model.PhysicalSymptomLog
import com.example.ui.theme.*
import com.example.utils.JalaliDate
import java.time.LocalDate

data class SymptomPresetItem(
    val id: String,
    val name: String,
    val category: String,
    val iconEmoji: String
)

val SYMPTOM_PRESETS = listOf(
    // شکم و لگن
    SymptomPresetItem("cramps", "درد و گرفتگی شکم", "شکم و لگن", "⚡"),
    SymptomPresetItem("backache", "کمردرد", "شکم و لگن", "🦴"),
    SymptomPresetItem("bloating", "نفخ و احساس سنگینی", "شکم و لگن", "🎈"),
    SymptomPresetItem("pelvic_pain", "درد تیرکشنده لگن", "شکم و لگن", "🎯"),
    SymptomPresetItem("thigh_pain", "کوفتگی عضلات ران", "شکم و لگن", "🦵"),

    // سر و اعصاب
    SymptomPresetItem("headache", "سردرد و میگرن", "سر و اعصاب", "🤕"),
    SymptomPresetItem("fatigue", "خستگی و بی‌حالی", "سر و اعصاب", "🔋"),
    SymptomPresetItem("dizziness", "سرگیجه و منگی", "سر و اعصاب", "💫"),
    SymptomPresetItem("insomnia", "بی‌خوابی و اختلال خواب", "سر و اعصاب", "🌙"),
    SymptomPresetItem("brain_fog", "مه‌مغزی و عدم تمرکز", "سر و اعصاب", "🌫️"),

    // پوست و بدن
    SymptomPresetItem("breast_tender", "حساسیت و تورم سینه‌ها", "پوست و بدن", "🌸"),
    SymptomPresetItem("acne", "جوش و آکنه هورمونی", "پوست و بدن", "✨"),
    SymptomPresetItem("hot_flash", "گرگرفتگی و تعریق", "پوست و بدن", "🔥"),
    SymptomPresetItem("water_retention", "ورم و احتباس آب", "پوست و بدن", "💧"),

    // گوارش و اشتها
    SymptomPresetItem("nausea", "حالت تهوع", "گوارش و اشتها", "🤢"),
    SymptomPresetItem("cravings", "هوس شیرینی و خوراکی", "گوارش و اشتها", "🍫"),
    SymptomPresetItem("heavy_appetite", "اشتهای زیاد و پرخوری", "گوارش و اشتها", "🍽️"),
    SymptomPresetItem("low_appetite", "بی‌اشتهایی", "گوارش و اشتها", "🥗"),
    SymptomPresetItem("constipation", "یبوست یا اختلال گوارش", "گوارش و اشتها", "🌿")
)

val MOOD_EMOJIS = listOf(
    "😊" to "شاد",
    "⚡" to "پرانرژی",
    "🌿" to "آرام",
    "🌪️" to "نوسان خلق",
    "🥺" to "حساس",
    "😤" to "عصبی",
    "😰" to "مضطرب",
    "🥱" to "خسته"
)

enum class CalendarCyclePhase {
    MENSTRUAL,
    FOLLICULAR,
    OVULATION,
    LUTEAL,
    REGULAR
}

/**
 * Visual interface for logging daily menstrual cycle symptoms and cycle phases,
 * using Room for local persistence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualDailySymptomLogger(
    selectedDate: JalaliDate,
    cyclePhase: CalendarCyclePhase,
    initialDailyLog: DailyLog?,
    initialSymptoms: List<PhysicalSymptomLog>,
    medReminders: List<MedicineReminder>,
    onSave: (
        flow: String,
        symptoms: List<PhysicalSymptomLog>,
        moods: List<String>,
        notes: String,
        emoji: String
    ) -> Unit,
    onDelete: () -> Unit,
    onToggleMedication: (medId: String, taken: Boolean) -> Unit,
    onSetPeriodStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val localDateStr = remember(selectedDate) { selectedDate.toLocalDate().toString() }

    // State for flow
    var selectedFlow by remember(selectedDate, initialDailyLog) {
        mutableStateOf(initialDailyLog?.periodFlow ?: "NONE")
    }

    // State for symptoms map: SymptomName -> Severity ("MILD", "MODERATE", "SEVERE")
    val activeSymptoms = remember(selectedDate, initialSymptoms, initialDailyLog) {
        val map = mutableStateMapOf<String, String>()
        if (initialSymptoms.isNotEmpty()) {
            initialSymptoms.forEach { map[it.symptomName] = it.severity }
        } else if (initialDailyLog != null && initialDailyLog.symptoms.isNotBlank()) {
            initialDailyLog.symptoms.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { map[it] = "MODERATE" }
        }
        map
    }

    // Selected symptom category tab
    val categories = listOf("همه", "شکم و لگن", "سر و اعصاب", "پوست و بدن", "گوارش و اشتها")
    var selectedCategory by remember { mutableStateOf("همه") }

    // State for emoji and notes
    var selectedEmoji by remember(selectedDate, initialDailyLog) {
        mutableStateOf(initialDailyLog?.emoji ?: "")
    }
    var notesText by remember(selectedDate, initialDailyLog) {
        mutableStateOf(initialDailyLog?.notes ?: "")
    }

    // Has saved data in Room indicator
    val hasRoomData = (initialSymptoms.isNotEmpty() || (initialDailyLog != null && (initialDailyLog.symptoms.isNotBlank() || initialDailyLog.periodFlow != "NONE" || initialDailyLog.notes.isNotBlank())))

    // Save confirmation trigger
    var showSavedNotification by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_daily_symptom_logger")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Date & Room Local Persistence Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ثبت علائم روز ${selectedDate.day} ${selectedDate.monthName} ${selectedDate.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "معادل میلادی: $localDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (hasRoomData) SleekPrimaryLight else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "وضعیت ذخیره",
                            tint = if (hasRoomData) SleekPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (hasRoomData) "ذخیره شده ✓" else "ثبت نشده",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasRoomData) SleekPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==============================================================
            // Section 1: Cycle Phase & Flow Intensity
            // ==============================================================
            val phaseTitle = when (cyclePhase) {
                CalendarCyclePhase.MENSTRUAL -> "فاز قاعدگی (پریود) 🩸"
                CalendarCyclePhase.FOLLICULAR -> "فاز فولیکولار (بهار بدن) 🌱"
                CalendarCyclePhase.OVULATION -> "فاز تخمک‌گذاری و باروری 🌸"
                CalendarCyclePhase.LUTEAL -> "فاز لوتئال و PMS 🌙"
                CalendarCyclePhase.REGULAR -> "فاز عادی چرخه ✨"
            }
            val phaseColor = when (cyclePhase) {
                CalendarCyclePhase.MENSTRUAL -> SleekPrimary
                CalendarCyclePhase.FOLLICULAR -> Color(0xFF10B981)
                CalendarCyclePhase.OVULATION -> Indigo500
                CalendarCyclePhase.LUTEAL -> Amber500
                CalendarCyclePhase.REGULAR -> MaterialTheme.colorScheme.secondary
            }
            val phaseBg = when (cyclePhase) {
                CalendarCyclePhase.MENSTRUAL -> SleekPrimaryLight
                CalendarCyclePhase.FOLLICULAR -> Color(0xFFE8F5E9)
                CalendarCyclePhase.OVULATION -> Indigo50
                CalendarCyclePhase.LUTEAL -> Amber50
                CalendarCyclePhase.REGULAR -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }

            Surface(
                color = phaseBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(phaseColor)
                        )
                        Text(
                            text = phaseTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                    }

                    // Quick action to mark period start
                    if (selectedFlow == "NONE") {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedFlow = "MEDIUM"
                                onSetPeriodStart()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("🩸 شروع پریود", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFlow = "NONE"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("✨ پاکی", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Menstrual Flow Selector
            Text(
                text = "شدت خونریزی قاعدگی 🩸",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val flowOptions = listOf(
                "NONE" to "بدون خونریزی",
                "SPOTTING" to "لکه‌بینی 💧",
                "LIGHT" to "کم 🩸",
                "MEDIUM" to "متوسط 🩸🩸",
                "HEAVY" to "زیاد 🩸🩸🩸"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                flowOptions.forEach { (flowKey, label) ->
                    val isSelected = selectedFlow == flowKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFlow = flowKey
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekPrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.testTag("flow_chip_$flowKey")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==============================================================
            // Section 2: Physical Menstrual Symptoms with 3-Level Severity
            // ==============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "علائم فیزیکی چرخه قاعدگی 🌿",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activeSymptoms.size} مورد ثبت شده",
                    fontSize = 11.sp,
                    color = if (activeSymptoms.isNotEmpty()) SleekPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    SuggestionChip(
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isCatSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            labelColor = if (isCatSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filtered symptoms list
            val filteredSymptoms = remember(selectedCategory) {
                if (selectedCategory == "همه") SYMPTOM_PRESETS
                else SYMPTOM_PRESETS.filter { it.category == selectedCategory }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredSymptoms.forEach { symptom ->
                    val isChecked = activeSymptoms.containsKey(symptom.name)
                    val severity = activeSymptoms[symptom.name] ?: "MODERATE"

                    Surface(
                        color = if (isChecked) {
                            when (severity) {
                                "MILD" -> Color(0xFFE8F5E9)
                                "MODERATE" -> Amber50
                                "SEVERE" -> SleekPrimaryLight
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) {
                                when (severity) {
                                    "MILD" -> Color(0xFF4CAF50)
                                    "MODERATE" -> Amber500
                                    "SEVERE" -> SleekPrimary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .testTag("symptom_item_${symptom.id}")
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isChecked) {
                                    activeSymptoms.remove(symptom.name)
                                } else {
                                    activeSymptoms[symptom.name] = "MODERATE"
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(symptom.iconEmoji, fontSize = 14.sp)
                                Text(
                                    text = symptom.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "انتخاب شده",
                                        tint = when (severity) {
                                            "MILD" -> Color(0xFF2E7D32)
                                            "MODERATE" -> Color(0xFFB45309)
                                            "SEVERE" -> SleekPrimary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // 3-Level Severity Selector when active
                            if (isChecked) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        "MILD" to "خفیف",
                                        "MODERATE" to "متوسط",
                                        "SEVERE" to "شدید"
                                    ).forEach { (sevKey, sevLabel) ->
                                        val isSevSelected = severity == sevKey
                                        val activeColor = when (sevKey) {
                                            "MILD" -> Color(0xFF2E7D32)
                                            "MODERATE" -> Color(0xFFB45309)
                                            "SEVERE" -> SleekPrimary
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSevSelected) activeColor else Color.Transparent)
                                                .border(
                                                    0.8.dp,
                                                    if (isSevSelected) activeColor else MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    activeSymptoms[symptom.name] = sevKey
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = sevLabel,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSevSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSevSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==============================================================
            // Section 3: Mood & Emotional State
            // ==============================================================
            Text(
                text = "احساس و خلق‌وخوی روزانه 💭",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MOOD_EMOJIS.forEach { (emoji, label) ->
                    val isSelected = selectedEmoji == emoji
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedEmoji = if (isSelected) "" else emoji
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==============================================================
            // Section 4: Daily Note
            // ==============================================================
            Text(
                text = "یادداشت روزانه ✍️",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("daily_note_input"),
                placeholder = {
                    Text("یادداشت یا تجربه بدنی امروز (مثلاً: دمنوش بابونه، کاهش استرس...)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Medication reminders if any
            if (medReminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "مکمل‌ها و داروهای این روز 💊",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val takenMedsList = remember(initialDailyLog) {
                    initialDailyLog?.takenMeds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                }

                medReminders.forEach { med ->
                    val isTaken = takenMedsList.contains(med.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                onToggleMedication(med.id, !isTaken)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTaken,
                            onCheckedChange = { checked ->
                                onToggleMedication(med.id, checked)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = SleekPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${med.name} (${med.dosage})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated feedback banner on save
            AnimatedVisibility(
                visible = showSavedNotification,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Text(
                            text = "علائم و وضعیت روز با موفقیت ذخیره شد ✨",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // ==============================================================
            // Section 5: Action Buttons (Room Local Persistence)
            // ==============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Save Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        // Convert active symptoms map to PhysicalSymptomLog entities
                        val physicalSymptomLogs = activeSymptoms.map { (name, sev) ->
                            val preset = SYMPTOM_PRESETS.find { it.name == name }
                            val category = preset?.category ?: "شکم و لگن"
                            val score = when (sev) {
                                "MILD" -> 1
                                "MODERATE" -> 2
                                "SEVERE" -> 3
                                else -> 2
                            }
                            PhysicalSymptomLog(
                                dateStr = localDateStr,
                                symptomName = name,
                                severity = sev,
                                severityScore = score,
                                category = category,
                                notes = notesText,
                                timestamp = System.currentTimeMillis()
                            )
                        }

                        val moods = if (selectedEmoji.isNotBlank()) listOf(selectedEmoji) else emptyList()

                        onSave(
                            selectedFlow,
                            physicalSymptomLogs,
                            moods,
                            notesText,
                            selectedEmoji
                        )

                        showSavedNotification = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_symptoms_room_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ذخیره اطلاعات این روز 💾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Delete / Clear Button
                if (hasRoomData) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeSymptoms.clear()
                            selectedFlow = "NONE"
                            selectedEmoji = ""
                            notesText = ""
                            onDelete()
                            showSavedNotification = false
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_day_data_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "پاک‌سازی روز", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
