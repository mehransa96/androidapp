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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.animation.core.*

@Composable
fun HeaderRow(
    userName: String,
    userAvatar: String,
    onToggleThemeMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAvatarGenerator: () -> Unit,
    onLock: () -> Unit
) {
    val jalaliToday = com.example.utils.JalaliCalendar.gregorianToJalali(java.time.LocalDate.now())
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Interactive Fantasy Avatar Picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { onNavigateToAvatarGenerator() }
            ) {
                FantasyAvatarCanvas(
                    avatarId = userAvatar,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "امروز، ${jalaliToday.formatLong()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "سلام $userName عزیز",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isDarkEffective = ThemeProvider.isDark
            IconButton(onClick = onToggleThemeMode) {
                Icon(
                    imageVector = if (isDarkEffective) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = if (isDarkEffective) "تغییر به حالت روشن" else "تغییر به حالت تاریک",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToReminders) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "یادآورها و داروها",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "تنظیمات",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onLock) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "قفل کردن برنامه",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CycleVisualizer(info: CycleInfo, cycleLength: Int, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    var rotationOffset by remember { mutableStateOf(0f) }

    val interactionScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "interactionScale"
    )

    val localSleekPrimary = SleekPrimary
    val localSleekPrimaryVariant = SleekPrimaryVariant
    val localSleekBorder = SleekBorder
    
    val animatedSweepAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (info.currentDay / cycleLength.toFloat()).coerceAtMost(1f) * 360f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1800, 
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "sweepAngle"
    )
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val ambientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(20000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ambientRotation"
    )
    
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )
  
    Box(
        modifier = modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isPressed = true },
                    onDragEnd = { isPressed = false; rotationOffset = 0f },
                    onDragCancel = { isPressed = false; rotationOffset = 0f },
                    onDrag = { change, dragAmount -> 
                        rotationOffset += dragAmount.x / 3f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val baseRotation = ambientRotation + rotationOffset
        
        // 3D-like glow shadow
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .aspectRatio(1f)
                .scale(interactionScale * 1.05f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            localSleekPrimary.copy(alpha = pulseGlow * 0.4f),
                            SleekSecondary.copy(alpha = pulseGlow * 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .scale(interactionScale)
                .graphicsLayer { rotationZ = baseRotation * 0.4f }
        ) {
            val strokeWidth = 18.dp.toPx()
            val radius = (size.minDimension - strokeWidth * 2) / 2
            
            // Outer 3D faint orbit
            drawCircle(
                color = localSleekPrimaryVariant.copy(alpha = 0.1f),
                radius = radius + (strokeWidth * 1.5f),
                style = Stroke(width = strokeWidth * 0.3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 25f)))
            )
            
            // Background track (multi-layered)
            drawCircle(
                color = localSleekBorder.copy(alpha = 0.4f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius - strokeWidth * 0.1f,
                style = Stroke(width = strokeWidth * 0.8f)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .scale(interactionScale)
                .graphicsLayer { rotationZ = rotationOffset }
        ) {
            val strokeWidth = 18.dp.toPx()
            val radius = (size.minDimension - strokeWidth * 2) / 2
            
            val gradientColors = if (info.isLate || info.daysUntilNextPeriod == 0) {
                listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
            } else if (info.isPms) {
                listOf(Color(0xFFFFB300), Color(0xFFFF8F00))
            } else if (info.isPeriod) {
                listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
            } else {
                listOf(Color(0xFFA78BFA), Color(0xFF818CF8))
            }
            
            // 3D Shadow under the main arc
            drawArc(
                color = Color.Black.copy(alpha = 0.15f),
                startAngle = -90f + 2f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Main progress arc
            drawArc(
                brush = Brush.sweepGradient(colors = gradientColors, center = androidx.compose.ui.geometry.Offset(size.minDimension/2, size.minDimension/2)),
                startAngle = -90f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
            )

            // Sparkle dot overlay
            val angleRad = Math.toRadians((animatedSweepAngle - 90).toDouble())
            val centerOffset = center
            val dotX = centerOffset.x + radius * Math.cos(angleRad).toFloat()
            val dotY = centerOffset.y + radius * Math.sin(angleRad).toFloat()

            drawCircle(
                color = Color.White,
                radius = strokeWidth * 0.6f,
                center = androidx.compose.ui.geometry.Offset(dotX, dotY),
                blendMode = androidx.compose.ui.graphics.BlendMode.Overlay
            )
        }
 
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(interactionScale)
        ) {
            Text(
                text = "روز ${info.currentDay} سیکل",
                style = MaterialTheme.typography.labelMedium,
                color = if (info.isPms) Color(0xFFD97706) else localSleekPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (info.isLate) "${info.daysLate} روز تأخیر" else if (!info.isPeriod && info.daysUntilNextPeriod == 0) "شروع پریود از امروز" else if (info.isPeriod) "پریود" else if (info.isFertile) "پنجره باروری" else if (info.isPms) "پیش‌قاعدگی (PMS)" else "روز معمولی",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = SleekTextMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(
                        color = if (info.isFertile) SleekPrimary else if (info.isPms) Color(0xFFF59E0B) else SleekBorder, 
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (info.isLate) "در صورت ادامه، تست بارداری" else if (info.isFertile) "بارداری: زیاد" else if (info.isPms) "بارداری: کم" else "بارداری: کم / متوسط",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (info.isFertile || info.isPms) Color.White else SleekTextMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DailyInsightCard(info: CycleInfo) {
    val insightData = remember(info) {
        when {
            info.isLate -> "پریود شما ${info.daysLate} روز تأخیر دارد. استرس، تغییر وزن، بیماری یا سفر می‌توانند باعث تأخیر شوند. اگر احتمال بارداری وجود دارد تست بدهید و اگر تأخیر بیش از ۲ هفته شد با پزشک مشورت کنید." to Icons.Filled.Favorite
            info.isPeriod -> "زمان استراحت و بازتوانی است. سعی کنید مایعات گرم بنوشید و از فعالیت‌های سنگین پرهیز کنید." to Icons.Filled.SelfImprovement
            info.isFertile -> "انرژی و تمرکز در اوج است! بهترین زمان برای شروع پروژه‌های جدید و فعالیت‌های اجتماعی است." to Icons.Filled.AutoAwesome
            info.isPms -> "ممکن است کمی خسته یا حساس باشید. به خودتان سخت نگیرید و روی آرامش ذهنی تمرکز کنید." to Icons.Filled.Spa
            info.currentDay in 6..13 -> "انرژی شما در حال افزایش است. زمان خوبی برای ورزش‌های هوازی و برنامه‌ریزی‌های جدید است." to Icons.AutoMirrored.Filled.DirectionsRun
            else -> "سطح هورمون‌ها به ثبات رسیده. یک رژیم غذایی متعادل و خواب کافی به حفظ انرژی شما کمک می‌کند." to Icons.Filled.Favorite
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = insightData.second,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "نکته سلامتی امروز (روز ${info.currentDay} چرخه)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insightData.first,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun AITipsCard(
    info: CycleInfo,
    averageCycleLength: Float,
    aiDailyTip: String?,
    isFetchingAiTip: Boolean,
    onFetchTip: (String, Int, Float) -> Unit,
    aiTipError: String? = null
) {
    val phaseName = when {
        info.isPeriod -> "قاعدگی (Menstrual)"
        info.isFertile -> "تخمک‌گذاری (Ovulation)"
        info.isPms -> "پیش از قاعدگی (PMS)"
        info.currentDay in 6..13 -> "فولیکولی (Follicular)"
        else -> "لوتئال (Luteal)"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI Tips",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مشاور هوشمند هانیتا",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (aiDailyTip != null) {
                Text(
                    text = aiDailyTip,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "با استفاده از هوش مصنوعی، توصیه‌های سلامتی کاملا شخصی‌سازی شده برای فاز $phaseName چرخه خود دریافت کنید.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (aiTipError != null && !isFetchingAiTip) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = aiTipError,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onFetchTip(phaseName, info.currentDay, averageCycleLength) },
                    enabled = !isFetchingAiTip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isFetchingAiTip) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("دریافت توصیه‌های امروز", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionsCard(info: CycleInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("پریود بعدی", style = MaterialTheme.typography.labelSmall, color = SleekTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when {
                        info.isLate -> "${info.daysLate} روز تأخیر"
                        info.daysUntilNextPeriod == 0 -> "امروز"
                        else -> "${info.daysUntilNextPeriod} روز"
                    },
                    fontSize = 13.sp,
                    color = if (info.isLate) Color(0xFFEF4444) else SleekPrimaryDark,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تخمک‌گذاری", style = MaterialTheme.typography.labelSmall, color = SleekTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val cycleLength = info.currentDay + info.daysUntilNextPeriod - 1
                val ovulationDay = com.example.domain.CyclePredictor.ovulationDay(cycleLength)
                val daysToOvulation = if (info.currentDay <= ovulationDay) ovulationDay - info.currentDay
                else (cycleLength - info.currentDay) + ovulationDay
                Text(
                    when {
                        info.isLate || info.daysUntilNextPeriod == 0 -> "پس از پریود"
                        daysToOvulation == 0 -> "امروز (تخمینی)"
                        else -> "حدود $daysToOvulation روز"
                    },
                    fontSize = 13.sp, color = Indigo500, fontWeight = FontWeight.Black
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("دوره PMS", style = MaterialTheme.typography.labelSmall, color = SleekTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                // PMS covers the last 5 days before the expected period (see CyclePredictor).
                val pmsText = when {
                    info.isPms -> "فعال 🌪️"
                    info.isPeriod || info.isLate || info.daysUntilNextPeriod == 0 -> "پایان یافته"
                    info.daysUntilNextPeriod > 5 -> "${info.daysUntilNextPeriod - 5} روز دیگر"
                    else -> "پایان یافته"
                }
                Text(pmsText, fontSize = 13.sp, color = Amber500, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun FeaturesGrid(onNavigateToLog: () -> Unit, onNavigateToCalendar: () -> Unit, onNavigateToHealth: () -> Unit, onNavigateToEducation: () -> Unit, onNavigateToMood: () -> Unit, onNavigateToChat: () -> Unit, onNavigateToMeditation: () -> Unit, onNavigateToReminders: () -> Unit, onNavigateToCharts: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureItem(
                icon = Icons.Filled.Add,
                label = "علائم",
                iconBgColor = MaterialTheme.colorScheme.primary,
                iconColor = Color.White,
                onClick = onNavigateToLog,
                modifier = Modifier.weight(1f)
            )
            FeatureItem(
                icon = Icons.Filled.CalendarMonth,
                label = "تقویم",
                iconBgColor = Indigo50,
                iconColor = Indigo500,
                onClick = onNavigateToCalendar,
                modifier = Modifier.weight(1f)
            )
            FeatureItem(
                icon = Icons.Filled.MonitorWeight,
                label = "سلامتی",
                iconBgColor = Amber50,
                iconColor = Amber500,
                onClick = onNavigateToHealth,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "آموزش",
                iconBgColor = Color(0xFFF3E8FF),
                iconColor = Color(0xFFA855F7), // Purple 500
                onClick = onNavigateToEducation,
                modifier = Modifier.weight(1f)
            )
            FeatureItem(
                icon = Icons.Filled.Favorite,
                label = "حال خوب",
                iconBgColor = Color(0xFFFFE4E6),
                iconColor = Color(0xFFE11D48), // Rose 600
                onClick = onNavigateToMood,
                modifier = Modifier.weight(1f)
            )
            FeatureItem(
                icon = Icons.Filled.AutoAwesome,
                label = "مشاور هوشمند",
                iconBgColor = Color(0xFFEFF6FF),
                iconColor = Color(0xFF3B82F6), // Blue 500
                onClick = onNavigateToChat,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f).bounceClick(onNavigateToMeditation),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFECFDF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = "موسیقی و مدیتیشن",
                            tint = Color(0xFF10B981), // Emerald 500
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "موسیقی و مراقبه 🧘‍♀️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SleekTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "کاهش استرس دوره قاعدگی",
                            fontSize = 8.5.sp,
                            color = SleekTextMedium
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier.weight(1f).bounceClick(onNavigateToReminders),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFEF2F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "یادآورها و هشدار دارویی",
                            tint = Color(0xFFEF4444), // Red 500
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "یادآورها و داروها 🔔",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SleekTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "کنترل مکمل‌ها و دوره بعدی",
                            fontSize = 8.5.sp,
                            color = SleekTextMedium
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f).bounceClick(onNavigateToCharts),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFCE7F3), CircleShape), // Rose 100
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "نمودار هورمون‌ها و علائم",
                            tint = Color(0xFFDB2777), // Rose 600
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "نمودار هورمونی و علائم 📈",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SleekTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تغییرات استروژن، پروژسترون و علائم",
                            fontSize = 8.5.sp,
                            color = SleekTextMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f)) // empty space to keep it half width
        }
    }
}

@Composable
fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.bounceClick(onClick),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = SleekTextDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AlertBanner(info: CycleInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekPrimaryLight),
        border = BorderStroke(1.dp, SleekPrimaryVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = "هشدار",
                tint = SleekPrimaryDark,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when {
                        info.isLate -> "تأخیر در قاعدگی"
                        info.daysUntilNextPeriod == 0 -> "قاعدگی از امروز انتظار می‌رود"
                        else -> "نزدیک شدن قاعدگی"
                    },
                    color = SleekPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = when {
                        info.isLate -> "${info.daysLate} روز از زمان پیش‌بینی‌شده گذشته است. با شروع پریود آن را ثبت کنید."
                        info.daysUntilNextPeriod == 0 -> "با شروع خونریزی آن را ثبت کنید تا پیش‌بینی‌ها دقیق‌تر شوند."
                        else -> "${info.daysUntilNextPeriod} روز زمان باقیست."
                    },
                    color = SleekPrimaryDark.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
