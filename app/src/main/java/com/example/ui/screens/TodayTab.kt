package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.CycleInfo
import com.example.ui.viewmodel.PredictionInfo
import com.example.utils.JalaliCalendar

/**
 * First tab: what is used every day, all on one screen without scrolling — cycle ring,
 * predictions, logging the period / symptoms, quick date edit, calendar, water and mood.
 */
@Composable
fun TodayTab(
    userName: String,
    userAvatar: String,
    cycleInfo: CycleInfo,
    predictionInfo: PredictionInfo,
    waterGlasses: Int,
    todaySymptomCount: Int,
    lastPeriodDate: String,
    averageCycleLength: Int,
    onToggleThemeMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAvatarGenerator: () -> Unit,
    onLock: () -> Unit,
    onLogPeriod: () -> Unit,
    onEditCycle: () -> Unit,
    onLogSymptoms: () -> Unit,
    onOpenCalendar: () -> Unit,
    onIncrementWater: () -> Unit,
    onDecrementWater: () -> Unit,
    onLogMood: (String) -> Unit
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        HeaderRow(
            userName = userName,
            userAvatar = userAvatar,
            onToggleThemeMode = onToggleThemeMode,
            onOpenSettings = onOpenSettings,
            onNavigateToReminders = onNavigateToReminders,
            onNavigateToAvatarGenerator = onNavigateToAvatarGenerator,
            onLock = onLock
        )
        AnimatedVisibility(visible = !cycleInfo.isPeriod && (cycleInfo.isLate || cycleInfo.daysUntilNextPeriod in 0..3)) {
            Column {
                Spacer(Modifier.height(8.dp))
                AlertBanner(cycleInfo)
            }
        }

        // The ring takes whatever height is left, so the tab always fits the screen.
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val ring = min(min(maxWidth, maxHeight), 250.dp)
            CycleVisualizer(
                info = cycleInfo,
                cycleLength = predictionInfo.calculatedAverage,
                modifier = Modifier.size(ring)
            )
        }

        PredictionsCard(cycleInfo)
        Spacer(Modifier.height(10.dp))

        val lastJalali = runCatching {
            JalaliCalendar.gregorianToJalali(java.time.LocalDate.parse(lastPeriodDate)).formatShort()
        }.getOrDefault("ثبت نشده")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("ثبت پریود", "شروع و پایان", Icons.Filled.WaterDrop, MaterialTheme.colorScheme.primary,
                "open_interactive_period_card", onLogPeriod, Modifier.weight(1f))
            QuickAction("علائم امروز", if (todaySymptomCount == 0) "ثبت علائم" else "$todaySymptomCount مورد",
                Icons.Filled.Healing, Color(0xFFE11D48), "today_log_symptoms", onLogSymptoms, Modifier.weight(1f))
            QuickAction("ویرایش تاریخ", lastJalali, Icons.Filled.EditCalendar, MaterialTheme.colorScheme.secondary,
                "quick_edit_cycle_button", onEditCycle, Modifier.weight(1f))
            QuickAction("تقویم", "${predictionInfo.calculatedAverage} روزه", Icons.Filled.CalendarMonth, Color(0xFF6366F1),
                "today_open_calendar", onOpenCalendar, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        WaterAndMoodRow(waterGlasses, onIncrementWater, onDecrementWater, onLogMood)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick).testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(34.dp).background(tint.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WaterAndMoodRow(
    waterGlasses: Int,
    onIncrementWater: () -> Unit,
    onDecrementWater: () -> Unit,
    onLogMood: (String) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrementWater, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "کم کردن یک لیوان آب", tint = Color(0xFF0EA5E9))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💧 $waterGlasses/8", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0284C7))
                Text("${waterGlasses * 250} ml", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = {
                    onIncrementWater()
                    Toast.makeText(context, "یک لیوان آب ثبت شد (+۲۵۰ml) 💧", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp).testTag("today_add_water")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن یک لیوان آب", tint = Color(0xFF0EA5E9))
            }
            Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.outlineVariant))
            // Same four moods as the full daily card.
            val moods = listOf("پرانرژی ✨" to "✨", "آرام و متمرکز 🧘" to "🧘", "خواب‌آلود و خسته 😴" to "😴", "حساس و احساساتی 🌧️" to "🌧️")
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                moods.forEach { (mood, emoji) ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                            .clickable {
                                onLogMood(mood)
                                Toast.makeText(context, "حالت «$mood» برای امروز ثبت شد 🌸", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 18.sp) }
                }
            }
        }
    }
}

/** One-line summary at the top of the cycle tab. */
@Composable
fun CycleSummaryStrip(cycleInfo: CycleInfo, predictionInfo: PredictionInfo) {
    val next = predictionInfo.predictedNextStart ?: cycleInfo.predictedPeriodStart
    val nextJalali = runCatching { JalaliCalendar.gregorianToJalali(next).formatShort() }.getOrDefault("-")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryChip("روز چرخه", "${cycleInfo.currentDay}", Modifier.weight(1f))
        SummaryChip("میانگین", "${predictionInfo.calculatedAverage} روز", Modifier.weight(1f))
        SummaryChip("پریود بعدی", nextJalali, Modifier.weight(1f))
        SummaryChip("نظم", if (predictionInfo.isIrregular) "نامنظم" else if (predictionInfo.totalCyclesCalculated < 3) "داده کم" else "منظم", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
