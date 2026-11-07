package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.components.ProfessionalDatePickerDialog
import com.example.ui.components.ProfessionalCycleEditDialog
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import com.example.ui.theme.ThemeProvider
import com.example.ui.theme.ThemeMode
import com.example.ui.components.MedicalReportDialog
import com.example.ui.components.PartnerSyncDialog
import com.example.ui.components.CyclePhaseGuideDialog
import com.example.ui.components.PregnancyTrackerDialog
import com.example.ui.components.InteractivePeriodCalendarWidget
import com.example.ui.components.HormoneLevelsChartCard
import com.example.ui.components.SmartInsightsCard
import com.example.utils.JalaliCalendar
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.CycleInfo
import com.example.ui.viewmodel.MahitaViewModel
import com.example.ui.viewmodel.PredictionInfo

import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.animation.core.*

/**
 * Lengths of the user's last completed cycles, measured from one logged period start to the next.
 * (This card used to draw invented values around the average with fixed month names.)
 */
@Composable
fun CycleHistoryChart(
    cycles: List<com.example.domain.CyclePredictor.CompletedCycle>,
    averageCycleLength: Int,
    modifier: Modifier = Modifier
) {
    val pColor = SleekPrimary
    val warnColor = Color(0xFFF59E0B)
    val bColor = SleekBorder

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, bColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "طول چرخه‌های اخیر شما",
                        style = MaterialTheme.typography.titleSmall,
                        color = SleekTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "از روز اول هر پریود تا روز اول پریود بعدی",
                        fontSize = 10.sp,
                        color = SleekTextMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekPrimaryLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "میانگین: $averageCycleLength روز",
                        fontSize = 10.sp,
                        color = SleekPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cycles.isEmpty()) {
                Text(
                    text = "برای رسم این نمودار دست‌کم دو پریود ثبت‌شده لازم است. با ثبت شروع هر پریود در تقویم، طول چرخه‌های شما اینجا نمایش داده می‌شود.",
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = SleekTextMedium
                )
                return@Column
            }

            val maxLen = maxOf(cycles.maxOf { it.lengthDays }, averageCycleLength, 35)
            Row(
                modifier = Modifier.fillMaxWidth().height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                cycles.forEach { cycle ->
                    val barColor = if (cycle.isTypical) pColor else warnColor
                    val month = com.example.utils.JalaliCalendar.gregorianToJalali(cycle.start).monthName
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("${cycle.lengthDays}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = barColor)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight((cycle.lengthDays.toFloat() / maxLen).coerceIn(0.05f, 0.82f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(barColor.copy(alpha = 0.85f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(month, fontSize = 9.sp, color = SleekTextMedium, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            val typical = cycles.filter { it.isTypical }
            val spread = if (typical.size >= 2) typical.maxOf { it.lengthDays } - typical.minOf { it.lengthDays } else 0
            Text(
                text = when {
                    typical.size < 2 -> "با ثبت چرخه‌های بیشتر، نظم چرخه شما هم ارزیابی می‌شود."
                    spread <= 7 -> "اختلاف کوتاه‌ترین و بلندترین چرخه $spread روز است؛ چرخه شما منظم به نظر می‌رسد."
                    else -> "اختلاف چرخه‌ها $spread روز است؛ چرخه کمی نامنظم است. اگر این نوسان ادامه داشت با پزشک مشورت کنید."
                },
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = SleekTextMedium
            )
            if (cycles.any { !it.isTypical }) {
                Text(
                    text = "ستون نارنجی یعنی فاصله کمتر از ۲۱ یا بیشتر از ۴۵ روز؛ اگر پریودی را ثبت نکرده‌اید، آن را در تقویم اضافه کنید تا میانگین دقیق‌تر شود.",
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp,
                    color = warnColor
                )
            }
        }
    }
}

@Composable
fun PredictionNotificationCard(predictionInfo: PredictionInfo) {
    val predictedDate = predictionInfo.predictedNextStart
    val jalaliPrediction = predictedDate?.let { com.example.utils.JalaliCalendar.gregorianToJalali(it) }
    val jalaliPredictionStr = jalaliPrediction?.formatLong() ?: "نامشخص"
    
    val daysRemaining = predictionInfo.daysRemaining
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "پیش‌بینی هوشمند",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پیش‌بینی هوشمند سیکل بعدی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = if (predictionInfo.hasEnoughData) SleekPrimary else Color.Gray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (predictionInfo.hasEnoughData) "طول واقعی" else "طول فرضی",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = if (predictionInfo.hasEnoughData) {
                    "بر اساس ${predictionInfo.totalCyclesCalculated} دوره ثبت شده قبلی شما، طول متوسط چرخه قاعدگی شما ${predictionInfo.calculatedAverage} روز برآورد شد."
                } else {
                    "به دلیل محدودیت داده‌های چرخه‌ شما، زمان‌بندی فعلی بر اساس مقدار تنظیم‌شده در تنظیمات (${predictionInfo.calculatedAverage} روز) است."
                },
                fontSize = 11.5.sp,
                color = SleekTextMedium,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(0.5.dp, SleekBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تاریخ تخمینی شروع بعدی:",
                        fontSize = 10.sp,
                        color = SleekTextLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = jalaliPredictionStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextDark
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                daysRemaining < 0 -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                daysRemaining in 0..3 -> SleekPrimary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            daysRemaining < 0 -> "${-daysRemaining} روز تاخیر"
                            daysRemaining == 0 -> "شروع از امروز"
                            else -> "$daysRemaining روز دیگر"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            daysRemaining < 0 -> Color(0xFFEF4444)
                            daysRemaining in 0..3 -> SleekPrimaryDark
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}
