package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.CycleInfo
import com.example.ui.viewmodel.PredictionInfo
import com.example.utils.JalaliCalendar
import java.time.LocalDate

@Composable
fun MedicalReportDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    userName: String,
    userWeight: String,
    averageCycleLength: Int,
    cycleInfo: CycleInfo,
    predictionInfo: PredictionInfo,
    logs: List<DailyLog>
) {
    if (!show) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val todayJalali = remember { JalaliCalendar.gregorianToJalali(LocalDate.now()) }
    val todayJalaliStr = todayJalali.formatLong()

    // Analysis calculations
    val periodLogs = remember(logs) { logs.filter { it.periodFlow != "NONE" } }
    val loggedDaysCount = logs.size
    val totalPeriodDays = periodLogs.size

    val allSymptoms: List<String> = remember(logs) {
        logs.flatMap { log ->
            if (log.symptoms.isBlank()) emptyList()
            else log.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    val symptomFrequency: List<Pair<String, Int>> = remember(allSymptoms) {
        allSymptoms.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
    }

    val allMoods: List<String> = remember(logs) {
        logs.flatMap { log ->
            if (log.moods.isBlank()) emptyList()
            else log.moods.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    val moodFrequency: List<Pair<String, Int>> = remember(allMoods) {
        allMoods.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
    }

    val avgBbt = remember(logs) {
        val bbtLogs = logs.filter { it.bbt != null && it.bbt > 0f }
        if (bbtLogs.isNotEmpty()) bbtLogs.map { it.bbt!! }.average().toFloat() else null
    }
    val avgWater = remember(logs) {
        val waterLogs = logs.filter { it.waterIntake > 0 }
        if (waterLogs.isNotEmpty()) waterLogs.map { it.waterIntake }.average().toFloat() else null
    }

    // Every figure below comes from the logged data. The report used to state a fixed
    // "regular (96%)" and "5 to 6 days" and the cycle-length setting instead of the real average.
    val realCycleLength = if (predictionInfo.hasEnoughData) predictionInfo.calculatedAverage else averageCycleLength
    val cycleLengthText = if (predictionInfo.hasEnoughData) {
        "$realCycleLength روز (از ${predictionInfo.totalCyclesCalculated} چرخه ثبت‌شده)"
    } else {
        "$realCycleLength روز (تخمینی؛ هنوز دو پریود ثبت نشده)"
    }
    val regularityStatus = when {
        predictionInfo.totalCyclesCalculated < 3 -> "برای ارزیابی دست‌کم ۳ چرخه ثبت‌شده لازم است"
        predictionInfo.isIrregular -> "نامنظم (اختلاف چرخه‌ها بیش از ۷ روز)"
        else -> "منظم (اختلاف چرخه‌ها ۷ روز یا کمتر)"
    }
    val avgPeriodDuration = if (periodLogs.isNotEmpty()) "${predictionInfo.averagePeriodLength} روز" else "ثبت نشده"
    val nextPeriodJalali = JalaliCalendar.gregorianToJalali(cycleInfo.predictedPeriodStart).formatLong()

    // Clinical text builder for sharing with doctor
    val clinicalSummaryText = buildString {
        appendLine("📋 **خلاصه گزارش پزشکی سلامت زنان و چرخه قاعدگی**")
        appendLine("برنامه هوشمند هانیتا (Hanita)")
        appendLine("----------------------------------")
        appendLine("👤 نام مراجع: $userName")
        appendLine("📅 تاریخ گزارش: $todayJalaliStr")
        if (userWeight.isNotBlank()) appendLine("⚖️ وزن فعلی ثبت شده: $userWeight کیلوگرم")
        if (avgBbt != null) appendLine("🌡 میانگین دمای پایه بدن (BBT): ${String.format(java.util.Locale.US, "%.1f", avgBbt)} درجه")
        if (avgWater != null) appendLine("💧 میانگین مصرف آب: ${String.format(java.util.Locale.US, "%.1f", avgWater)} لیوان در روز")
        appendLine("⏱ میانگین طول چرخه: $cycleLengthText")
        appendLine("🩸 میانگین طول دوره خونریزی: $avgPeriodDuration")
        appendLine("📊 وضعیت نظم چرخه: $regularityStatus")
        appendLine("🔄 شروع تقریبی پریود بعدی: $nextPeriodJalali")
        if (symptomFrequency.isNotEmpty()) {
            appendLine("----------------------------------")
            appendLine("🩺 شایع‌ترین علائم ثبت شده:")
            for (item in symptomFrequency.take(4)) {
                appendLine(" • ${item.first} (${item.second} مرتبه)")
            }
        }
        if (moodFrequency.isNotEmpty()) {
            appendLine("----------------------------------")
            appendLine("🧠 وضعیت‌های خلقی مکرر:")
            for (item in moodFrequency.take(3)) {
                appendLine(" • ${item.first} (${item.second} مرتبه)")
            }
        }
        appendLine("----------------------------------")
        appendLine("تعداد روزهای پایش شده: $loggedDaysCount روز")
        appendLine("تولید شده توسط دستیار سلامت هانیتا")
    }

    val shareReport = {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, clinicalSummaryText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "ارسال پرونده سلامت به پزشک یا همراه")
        context.startActivity(shareIntent)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "پرونده سلامت و گزارش پزشک",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تحلیل بالینی و آماده اشتراک با متخصص زنان",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "نام بیمار: $userName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "تاریخ تنظیم گزارش: $todayJalaliStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (userWeight.isNotBlank()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "$userWeight kg",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Key Clinical Metrics Grid
                    Text("شاخص‌های بالینی چرخه", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClinicalMetricCard(
                            title = "میانگین طول چرخه",
                            value = "$realCycleLength روز",
                            subtitle = "محدوده نرمال: ۲۱-۳۵",
                            modifier = Modifier.weight(1f),
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        ClinicalMetricCard(
                            title = "طول خونریزی",
                            value = avgPeriodDuration,
                            subtitle = "محدوده نرمال: ۳-۷",
                            modifier = Modifier.weight(1f),
                            accentColor = Color(0xFFEF4444)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClinicalMetricCard(
                            title = "شاخص نظم چرخه",
                            value = regularityStatus,
                            subtitle = "بر پایه دوره‌های قبلی",
                            modifier = Modifier.weight(1f),
                            accentColor = Color(0xFF10B981)
                        )
                        ClinicalMetricCard(
                            title = "چرخه بعدی",
                            value = when {
                                cycleInfo.isLate -> "${cycleInfo.daysLate} روز تأخیر"
                                cycleInfo.daysUntilNextPeriod == 0 -> "امروز"
                                else -> "${cycleInfo.daysUntilNextPeriod} روز دیگر"
                            },
                            subtitle = JalaliCalendar.gregorianToJalali(cycleInfo.predictedPeriodStart).formatLong(),
                            modifier = Modifier.weight(1f),
                            accentColor = Color(0xFFA855F7)
                        )
                    }

                    // Symptoms Analysis Section
                    Text("تحلیل علائم شایع ثبت شده", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (symptomFrequency.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "هنوز علائم خاصی ثبت نشده است. با ثبت روزانه در بخش لاگ، الگوی علائم اینجا تحلیل می‌شود.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (item in symptomFrequency.take(5)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "• ${item.first}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${item.second} بار گزارش شده",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Mood Analysis Section
                    if (moodFrequency.isNotEmpty()) {
                        Text("الگوهای خلقی و هیجانی", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (item in moodFrequency.take(4)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "• ${item.first}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${item.second} روز",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }
                        }
                    }

                    // Doctor Note
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "این گزارش به عنوان ابزار کمکی جهت پایش و خودآگاهی طراحی شده و جایگزین تشخیص قطعی پزشکی نیست. ارائه این خلاصه به پزشک متخصص، به بررسی دقیق‌تر پرونده کمک شایانی خواهد نمود.",
                                fontSize = 10.5.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(clinicalSummaryText))
                            Toast.makeText(context, "متن گزارش پزشکی در حافظه کپی شد.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کپی گزارش", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = shareReport,
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اشتراک با پزشک", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ClinicalMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}
