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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.saveable.rememberSaveable

/** The five sections of the app, in bottom-bar order (rightmost first in RTL). */
enum class HomeTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY("امروز", Icons.Filled.Home),
    CYCLE("چرخه و تحلیل", Icons.Filled.Insights),
    HEALTH("سلامت", Icons.Filled.HealthAndSafety),
    WELLBEING("حال خوب", Icons.Filled.Spa),
    PARTNER("همراه", Icons.Filled.Favorite)
}

@Composable
fun DashboardScreen(
    viewModel: MahitaViewModel,
    onNavigateToLog: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToEducation: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToMeditation: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAvatarGenerator: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCharts: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onLock: () -> Unit,
    startTab: Int = 0
) {
    val cycleInfo by viewModel.cycleInfo.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val averageCycleLength by viewModel.averageCycleLength.collectAsState()
    val predictionInfo by viewModel.predictionInfo.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val userWeight by viewModel.userWeight.collectAsState()
    val waterGlasses by viewModel.waterGlasses.collectAsState()
    val lastPeriodDate by viewModel.lastPeriodDate.collectAsState()
    val allPhysicalSymptoms by viewModel.allPhysicalSymptoms.collectAsState()
    val cycleRecords by viewModel.allCycleRecords.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val todayDateStr = currentDate.toString()
    val todayPhysicalSymptoms = remember(allPhysicalSymptoms, todayDateStr) {
        allPhysicalSymptoms.filter { it.dateStr == todayDateStr }
    }
    val aiDailyTip by viewModel.aiDailyTip.collectAsState()
    val isFetchingAiTip by viewModel.isFetchingAiTip.collectAsState()
    val aiTipError by viewModel.aiTipError.collectAsState()
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(startTab.coerceIn(0, HomeTab.values().lastIndex)) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMedicalReportDialog by remember { mutableStateOf(false) }
    var showPartnerSyncDialog by remember { mutableStateOf(false) }
    var showPhaseGuideDialog by remember { mutableStateOf(false) }
    var showPregnancyDialog by remember { mutableStateOf(false) }
    var showInteractivePeriodDialog by remember { mutableStateOf(false) }
    var showQuickCycleEditDialog by remember { mutableStateOf(false) }
    // Which dashboard card is open in a full-screen sheet (null = none).
    var openSheet by remember { mutableStateOf<String?>(null) }

    // Back from another tab returns to "today" instead of leaving the app.
    BackHandler(enabled = selectedTab != HomeTab.TODAY.ordinal) { selectedTab = HomeTab.TODAY.ordinal }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                HomeTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.ordinal,
                        onClick = { selectedTab = tab.ordinal },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, fontSize = 10.5.sp, maxLines = 1) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) togetherWith
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
            },
            label = "homeTab",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) { tabIndex ->
            when (HomeTab.values()[tabIndex]) {
                HomeTab.TODAY -> TodayTab(
                    userName = userName,
                    userAvatar = userAvatar,
                    cycleInfo = cycleInfo,
                    predictionInfo = predictionInfo,
                    waterGlasses = waterGlasses,
                    todaySymptomCount = todayPhysicalSymptoms.size,
                    lastPeriodDate = lastPeriodDate,
                    averageCycleLength = averageCycleLength,
                    onToggleThemeMode = { viewModel.toggleThemeMode() },
                    onOpenSettings = { showSettingsDialog = true },
                    onNavigateToReminders = onNavigateToReminders,
                    onNavigateToAvatarGenerator = onNavigateToAvatarGenerator,
                    onLock = onLock,
                    onLogPeriod = { showInteractivePeriodDialog = true },
                    onEditCycle = { showQuickCycleEditDialog = true },
                    onLogSymptoms = onNavigateToLog,
                    onOpenCalendar = onNavigateToCalendar,
                    onIncrementWater = { viewModel.incrementWater() },
                    onDecrementWater = { viewModel.decrementWater() },
                    onLogMood = { viewModel.logQuickMood(it) }
                )
                HomeTab.CYCLE -> Column(Modifier.fillMaxSize()) {
                    TabHeader("چرخه و تحلیل 📊", "تقویم، تاریخچه، پیش‌بینی‌ها و نمودارهای چرخه شما", { showSettingsDialog = true }, onLock)
                    CycleSummaryStrip(cycleInfo, predictionInfo)
                    Spacer(Modifier.height(10.dp))
                    HubGrid(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp),
                        items = listOf(
                            HubItem("تقویم قاعدگی", "ماه به ماه، ثبت و ویرایش روزها", Icons.Filled.CalendarMonth, Color(0xFF6366F1), "hub_calendar", onNavigateToCalendar),
                            HubItem("تاریخچه چرخه‌ها", "همه دوره‌های ثبت‌شده و ویرایش آن‌ها", Icons.Filled.History, Color(0xFFDB2777), "hub_history", onNavigateToHistory),
                            HubItem("پیش‌بینی هوشمند", "پریود بعدی، دقت و نظم چرخه", Icons.Filled.QueryStats, Color(0xFF0EA5E9), "hub_prediction") { openSheet = "prediction" },
                            HubItem("نمودار طول چرخه", "مقایسه طول چرخه‌های اخیر", Icons.Filled.Timeline, Color(0xFFF59E0B), "hub_cycle_chart") { openSheet = "cycle_chart" },
                            HubItem("منحنی هورمون‌ها", "استروژن، پروژسترون و LH در چرخه", Icons.Filled.BarChart, Color(0xFFA855F7), "hub_hormones") { openSheet = "hormones" },
                            HubItem("نمودار علائم", "روند علائم و هورمون‌ها در طول زمان", Icons.Filled.Insights, Color(0xFFE11D48), "hub_charts", onNavigateToCharts),
                            HubItem("تحلیل هوشمند چرخه", "بینش‌های شخصی از داده‌های شما", Icons.Filled.Psychology, Color(0xFF10B981), "hub_insights") { openSheet = "insights" },
                            HubItem("راهنمای فازهای چرخه", "هر فاز چه تغییری در بدن می‌دهد", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF64748B), "hub_phase_guide") { showPhaseGuideDialog = true }
                        )
                    )
                }
                HomeTab.HEALTH -> Column(Modifier.fillMaxSize()) {
                    TabHeader("سلامت و مراقبت 🩺", "ابزارهای سلامت، یادآورها، گزارش پزشک و بارداری", { showSettingsDialog = true }, onLock)
                    Spacer(Modifier.height(6.dp))
                    HubGrid(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp),
                        items = listOf(
                            HubItem("علائم جسمی امروز", if (todayPhysicalSymptoms.isEmpty()) "هنوز علامتی ثبت نشده" else "${todayPhysicalSymptoms.size} علامت ثبت شده", Icons.Filled.Healing, Color(0xFFE11D48), "hub_today_symptoms") { openSheet = "symptoms" },
                            HubItem("ثبت سریع حال روزانه", "خلق، انرژی، خواب و درد در یک نگاه", Icons.Filled.EventNote, Color(0xFF8B5CF6), "hub_quick_log") { openSheet = "quick_log" },
                            HubItem("ابزارهای سلامت", "قرص، تست‌ها، فشار خون، خودآزمایی، PDF پزشک", Icons.Filled.HealthAndSafety, Color(0xFFDB2777), "hub_tools", onNavigateToTools),
                            HubItem("سلامت جسمی و وزن", "وزن، BMI و شاخص‌های بدن", Icons.Filled.MonitorWeight, Color(0xFFF59E0B), "hub_health", onNavigateToHealth),
                            HubItem("یادآورها و داروها", "داروها، مکمل‌ها و هشدار دوره بعد", Icons.Filled.NotificationsActive, Color(0xFFEF4444), "hub_reminders", onNavigateToReminders),
                            HubItem("گزارش پزشکی", "خلاصه آماده برای ویزیت پزشک", Icons.Filled.Description, Color(0xFF0EA5E9), "hub_medical_report") { showMedicalReportDialog = true },
                            HubItem("ردیاب بارداری", "هفته بارداری و تاریخ تقریبی زایمان", Icons.Filled.PregnantWoman, Color(0xFFEC4899), "hub_pregnancy") { showPregnancyDialog = true },
                            HubItem("حریم خصوصی و پشتیبان", "قفل برنامه، پشتیبان‌گیری و بازیابی", Icons.Filled.Lock, Color(0xFF7C3AED), "hub_privacy", onNavigateToPrivacy)
                        )
                    )
                }
                HomeTab.WELLBEING -> Column(Modifier.fillMaxSize()) {
                    TabHeader("حال خوب 🌸", "موسیقی متناسب با دوره، مدیتیشن، مشاور و آموزش", { showSettingsDialog = true }, onLock)
                    Spacer(Modifier.height(6.dp))
                    HubGrid(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp),
                        items = listOf(
                            HubItem("موسیقی متناسب با دوره", "۱۶ قطعه اختصاصی برای ۴ فاز چرخه", Icons.Filled.MusicNote, Color(0xFFE11D48), "hub_cycle_music", onNavigateToMood),
                            HubItem("مدیتیشن و آوای طبیعت", "تنفس هدایت‌شده، باران، جنگل و دریا", Icons.Filled.SelfImprovement, Color(0xFF10B981), "hub_meditation", onNavigateToMeditation),
                            HubItem("بینش امروز", "پیشنهاد متناسب با فاز فعلی شما", Icons.Filled.Eco, Color(0xFF22C55E), "hub_daily_insight") { openSheet = "daily_insight" },
                            HubItem("نکته هوشمند روز", "پیشنهاد شخصی با هوش مصنوعی", Icons.Filled.TipsAndUpdates, Color(0xFFF59E0B), "hub_ai_tip") { openSheet = "ai_tip" },
                            HubItem("مشاور هوشمند", "پرسش و پاسخ درباره سلامت زنان", Icons.Filled.AutoAwesome, Color(0xFF3B82F6), "hub_chat", onNavigateToChat),
                            HubItem("آموزش و دانستنی‌ها", "مقاله‌های کوتاه و قابل اعتماد", Icons.AutoMirrored.Filled.MenuBook, Color(0xFFA855F7), "hub_education", onNavigateToEducation),
                            HubItem("آواتار من", "ساخت و تغییر تصویر پروفایل", Icons.Filled.Face, Color(0xFFEC4899), "hub_avatar", onNavigateToAvatarGenerator),
                            HubItem("همدلی با همسر", "راهنمای حمایت در هر فاز", Icons.Filled.VolunteerActivism, Color(0xFFF43F5E), "hub_partner_guide") { showPartnerSyncDialog = true }
                        )
                    )
                }
                HomeTab.PARTNER -> Column(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                    PartnerTab(
                        userName = userName,
                        cycleInfo = cycleInfo,
                        predictionInfo = predictionInfo,
                        onOpenSettings = { showSettingsDialog = true },
                        onLock = onLock,
                        today = currentDate
                    )
                }
            }
        }
    }

    when (openSheet) {
        "prediction" -> CardSheet("پیش‌بینی هوشمند", { openSheet = null }) {
            PredictionsCard(cycleInfo)
            PredictionNotificationCard(predictionInfo)
        }
        "cycle_chart" -> CardSheet("نمودار طول چرخه", { openSheet = null }) {
            val recentCycles by viewModel.recentCycles.collectAsState()
            CycleHistoryChart(cycles = recentCycles, averageCycleLength = predictionInfo.calculatedAverage, modifier = Modifier.fillMaxWidth())
        }
        "hormones" -> CardSheet("منحنی هورمون‌ها", { openSheet = null }) {
            HormoneLevelsChartCard(
                cycleInfo = cycleInfo,
                averageCycleLength = predictionInfo.calculatedAverage,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "insights" -> CardSheet("تحلیل هوشمند چرخه", { openSheet = null }) {
            SmartInsightsCard(cycleInfo = cycleInfo, modifier = Modifier.fillMaxWidth())
        }
        "symptoms" -> CardSheet("علائم جسمی امروز", { openSheet = null }) {
            DailyPhysicalSymptomsCard(
                todaySymptoms = todayPhysicalSymptoms,
                onNavigateToLog = {
                    openSheet = null
                    onNavigateToLog()
                }
            )
        }
        "quick_log" -> CardSheet("ثبت سریع حال روزانه", { openSheet = null }) {
            com.example.ui.components.QuickSymptomWidget(
                onSave = { mood, energy, sleep, pain ->
                    viewModel.saveQuickLog(currentDate.toString(), mood, energy, sleep, pain)
                },
                modifier = Modifier.fillMaxWidth()
            )
            QuickHydrationAndMoodCard(
                waterGlasses = waterGlasses,
                onIncrementWater = { viewModel.incrementWater() },
                onDecrementWater = { viewModel.decrementWater() },
                onLogMood = { mood -> viewModel.logQuickMood(mood) }
            )
        }
        "daily_insight" -> CardSheet("بینش امروز", { openSheet = null }) {
            DailyInsightCard(cycleInfo)
        }
        "ai_tip" -> CardSheet("نکته هوشمند روز", { openSheet = null }) {
            AITipsCard(
                info = cycleInfo,
                averageCycleLength = predictionInfo.calculatedAverage.toFloat(),
                aiDailyTip = aiDailyTip,
                isFetchingAiTip = isFetchingAiTip,
                onFetchTip = { phase, day, avg -> viewModel.fetchAiDailyTip(phase, day, avg) },
                aiTipError = aiTipError
            )
        }
    }

    SettingsDialog(
        show = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
        viewModel = viewModel,
        onNavigateToHistory = {
            showSettingsDialog = false
            onNavigateToHistory()
        }
    )

    MedicalReportDialog(
        show = showMedicalReportDialog,
        onDismiss = { showMedicalReportDialog = false },
        userName = userName,
        userWeight = userWeight,
        averageCycleLength = averageCycleLength,
        cycleInfo = cycleInfo,
        predictionInfo = predictionInfo,
        logs = logs
    )

    PartnerSyncDialog(
        show = showPartnerSyncDialog,
        onDismiss = { showPartnerSyncDialog = false },
        userName = userName,
        cycleInfo = cycleInfo
    )

    CyclePhaseGuideDialog(
        show = showPhaseGuideDialog,
        onDismiss = { showPhaseGuideDialog = false }
    )

    PregnancyTrackerDialog(
        show = showPregnancyDialog,
        onDismiss = { showPregnancyDialog = false },
        lastPeriodDateStr = lastPeriodDate,
        cycleLength = predictionInfo.calculatedAverage
    )

    if (showInteractivePeriodDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showInteractivePeriodDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showInteractivePeriodDialog = false }) {
                    Text("بستن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    InteractivePeriodCalendarWidget(
                        recordedLogs = logs,
                        onSavePeriod = { start, end, flow, notes ->
                            viewModel.savePeriodRange(start, end, flow, notes)
                            showInteractivePeriodDialog = false
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
                            Toast.makeText(context, "بازه انتخابی پاک شد", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showQuickCycleEditDialog) {
        // Edit the period the forecast is really based on (first day of the last logged bleed),
        // with its real end date, instead of a possibly stale setting and a guessed 5-day end.
        val parsedLastPeriod = predictionInfo.lastPeriodStart
            ?: runCatching { java.time.LocalDate.parse(lastPeriodDate) }.getOrNull()
            ?: java.time.LocalDate.now()
        val lastRecord = cycleRecords.firstOrNull { it.startDate == parsedLastPeriod.toString() }
        val lastRecordEnd = lastRecord?.endDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        val lastFlow = logs.firstOrNull { it.dateStr == parsedLastPeriod.toString() }
            ?.periodFlow?.takeIf { it != "NONE" } ?: "MEDIUM"
        ProfessionalCycleEditDialog(
            initialStartDate = parsedLastPeriod,
            initialEndDate = lastRecordEnd ?: com.example.ui.components.earlierOf(parsedLastPeriod.plusDays(4), java.time.LocalDate.now()),
            initialFlow = lastFlow,
            initialCycleLength = averageCycleLength,
            title = "ویرایش حرفه‌ای دوره قاعدگی و تنظیم چرخه 🩸",
            onDismissRequest = { showQuickCycleEditDialog = false },
            onSaveCycle = { newStart, newEnd, length, flow, notes ->
                viewModel.updateFullCycleRecord(
                    oldRecordId = lastRecord?.id,
                    oldStartDate = parsedLastPeriod,
                    oldEndDate = lastRecordEnd,
                    newStartDate = newStart,
                    newEndDate = newEnd,
                    cycleLength = length,
                    flow = flow,
                    notes = notes
                )
                showQuickCycleEditDialog = false
                val startJalali = JalaliCalendar.gregorianToJalali(newStart).formatShort()
                Toast.makeText(context, "اطلاعات چرخه با شروع $startJalali با موفقیت ثبت گردید ✨", Toast.LENGTH_LONG).show()
            }
        )
    }
}
