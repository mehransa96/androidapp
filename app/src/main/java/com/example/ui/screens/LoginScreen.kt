package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.JalaliCalendar
import java.time.LocalDate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.preferences.UserPreferencesManager
import com.example.security.AppLockManager
import com.example.security.AppLockSession
import com.example.ui.components.BackupPasswordDialog
import com.example.ui.components.RecoveryCodeShownDialog
import com.example.ui.viewmodel.HealthToolsViewModel

/**
 * First-launch sign-up. The account is created only on this phone: no Google account, no server,
 * no internet needed. Everything the user enters afterwards stays in the app's private storage.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    lockManager: AppLockManager,
    restoreViewModel: HealthToolsViewModel,
    onRestored: () -> Unit,
    /** name, average cycle length, first day of the last period (null = "I don't remember"). */
    onRegistered: (String, Int, String?) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var periodDateUnknown by rememberSaveable { mutableStateOf(false) }
    
    val todayJalali = remember { JalaliCalendar.gregorianToJalali(LocalDate.now()) }
    var selectedYear by rememberSaveable { mutableIntStateOf(todayJalali.year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(todayJalali.month) }
    var selectedDay by rememberSaveable { mutableIntStateOf(todayJalali.day) }
    var averageCycleLength by rememberSaveable { mutableFloatStateOf(28f) }

    fun finish() {
        val lastPeriod = if (periodDateUnknown) null
        else JalaliCalendar.jalaliToGregorian(selectedYear, selectedMonth, selectedDay).toString()
        onRegistered(UserPreferencesManager.sanitizeName(name), averageCycleLength.toInt(), lastPeriod)
    }
    
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alphaAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )

    val isDark = ThemeProvider.isDark
    val themeController = ThemeProvider.controller

    val bgGradientColors = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            Color(0xFF2C2554)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.background
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = bgGradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.08f)
        val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.12f else 0.06f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryGlow,
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                color = secondaryGlow,
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
        }

        // Quick theme toggle button in top corner
        IconButton(
            onClick = { themeController.toggleMode() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (isDark) "تغییر به حالت روشن" else "تغییر به حالت تاریک",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .alpha(alphaAnimation)
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 16.dp else 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Application Logo / Header
                Text(
                    text = "هانیتا",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "همراه سلامتی و تندرستی تو",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Active Step indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val active = step == i
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                    },
                    label = "step"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> StepWelcome(
                            name = name,
                            onNameChange = { name = it.take(UserPreferencesManager.MAX_NAME_LENGTH) },
                            restoreViewModel = restoreViewModel,
                            onRestored = onRestored,
                            onNext = { step = 2 }
                        )
                        2 -> StepPeriodDate(
                            selectedYear = selectedYear,
                            selectedMonth = selectedMonth,
                            selectedDay = selectedDay,
                            onYearChange = { selectedYear = it },
                            onMonthChange = { selectedMonth = it },
                            onDayChange = { selectedDay = it },
                            onBack = { step = 1 },
                            onUnknown = {
                                periodDateUnknown = true
                                step = 3
                            },
                            onNext = {
                                periodDateUnknown = false
                                step = 3
                            }
                        )
                        3 -> StepCycleLength(
                            averageCycleLength = averageCycleLength,
                            onCycleLengthChange = { averageCycleLength = it },
                            onBack = { step = 2 },
                            onSubmit = { step = 4 }
                        )
                        4 -> StepSecurity(
                            lockManager = lockManager,
                            onBack = { step = 3 },
                            onFinish = { finish() }
                        )
                    }
                }
            }
        }
}
}

@Composable
fun StepWelcome(
    name: String,
    onNameChange: (String) -> Unit,
    restoreViewModel: HealthToolsViewModel,
    onRestored: () -> Unit,
    onNext: () -> Unit
) {
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    val busy by restoreViewModel.busy.collectAsState()
    val message by restoreViewModel.message.collectAsState()
    val passwordRequest by restoreViewModel.passwordRequest.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            restoreViewModel.clearMessage()
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Step 1 Welcome Illustration using elegant canvas petals style
        Canvas(modifier = Modifier.size(120.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            // Beautiful modern flower blossom
            drawCircle(
                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                radius = size.width * 0.45f,
                center = center
            )
            drawCircle(
                color = Color(0xFFE879A0).copy(alpha = 0.2f),
                radius = size.width * 0.32f,
                center = center
            )
            for (i in 0 until 6) {
                val angle = (i * 60) * (Math.PI / 180)
                val distance = size.width * 0.22f
                val leafCenter = Offset(
                    (center.x + distance * Math.cos(angle)).toFloat(),
                    (center.y + distance * Math.sin(angle)).toFloat()
                )
                drawCircle(
                    color = Color(0xFFE879A0).copy(alpha = 0.6f),
                    radius = size.width * 0.14f,
                    center = leafCenter
                )
            }
            drawCircle(
                color = Color.White,
                radius = size.width * 0.12f,
                center = center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "خوش آمدی! ثبت‌نام کن",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "حساب کاربری تو فقط روی همین گوشی ساخته می‌شود. نیازی به ایمیل، شماره تلفن یا اینترنت نیست و همه اطلاعاتت فقط روی گوشی خودت می‌ماند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("نام یا نام مستعار شما", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("next_step_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            ),
            enabled = UserPreferencesManager.sanitizeName(name).isNotBlank() && !busy
        ) {
            Text("مرحله بعدی", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            enabled = !busy,
            onClick = {
                AppLockSession.skipNextLock = true
                restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
            }
        ) {
            Text("قبلاً از هانیتا استفاده می‌کردم: بازیابی از فایل پشتیبان", fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("بازیابی از فایل پشتیبان") },
            text = { Text("همه سوابق و تنظیمات فایل پشتیبان روی این گوشی بازیابی می‌شود و برنامه دوباره باز می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    restoreViewModel.restoreBackup(uri, null, onRestored)
                }) { Text("بازیابی") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("انصراف") } }
        )
    }
    passwordRequest?.let { req ->
        BackupPasswordDialog(
            forExport = false,
            errorText = if (req.wrongPassword) "رمز اشتباه است یا فایل آسیب دیده است." else null,
            onDismiss = { restoreViewModel.cancelPasswordRequest() },
            onConfirm = { password -> restoreViewModel.restoreBackup(req.uri, password, onRestored) }
        )
    }
}

/**
 * Last sign-up step: an optional PIN that locks the app. A recovery code is shown right away so a
 * forgotten PIN never means losing the data.
 */
@Composable
fun StepSecurity(
    lockManager: AppLockManager,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pinRepeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var recoveryCode by remember { mutableStateOf<String?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "امنیت اطلاعات تو",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "اطلاعاتت فقط در حافظه خصوصی برنامه روی همین گوشی ذخیره می‌شود و هر روز یک نسخه رمزنگاری‌شده خودکار هم از آن نگه داشته می‌شود. با به‌روزرسانی برنامه چیزی پاک نمی‌شود.\n\nبرای اینکه کسی بدون اجازه وارد برنامه نشود، یک رمز ۴ تا ۶ رقمی بگذار (اختیاری).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = AppLockManager.normalizeDigits(it).take(6); error = null },
            label = { Text("رمز ورود (اختیاری)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pinRepeat,
            onValueChange = { pinRepeat = AppLockManager.normalizeDigits(it).take(6); error = null },
            label = { Text("تکرار رمز") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("بازگشت", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (pin.isEmpty() && pinRepeat.isEmpty()) {
                        onFinish()
                        return@Button
                    }
                    error = when {
                        !AppLockManager.isValidPin(pin) -> "رمز باید ۴ تا ۶ رقم باشد."
                        pin != pinRepeat -> "دو رمز یکسان نیستند."
                        else -> null
                    }
                    if (error == null) {
                        lockManager.setPin(pin)
                        recoveryCode = lockManager.createRecoveryCode()
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp)
                    .testTag("onboarding_finish_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (pin.isEmpty() && pinRepeat.isEmpty()) "شروع بدون رمز" else "ذخیره رمز و شروع",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    recoveryCode?.let { code ->
        RecoveryCodeShownDialog(code = code, onDone = {
            recoveryCode = null
            onFinish()
        })
    }
}

@Composable
fun StepPeriodDate(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onBack: () -> Unit,
    onUnknown: () -> Unit,
    onNext: () -> Unit
) {
    val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    // Calculate maximum days in current solar hijri selection
    // Same leap-year rule as the rest of the app (the old local formula disagreed in some years).
    val maxDays = remember(selectedYear, selectedMonth) {
        when {
            selectedMonth <= 6 -> 31
            selectedMonth <= 11 -> 30
            // Esfand: count the real days between 1 Esfand and 1 Farvardin of the next year.
            else -> runCatching {
                java.time.temporal.ChronoUnit.DAYS.between(
                    JalaliCalendar.jalaliToGregorian(selectedYear, 12, 1),
                    JalaliCalendar.jalaliToGregorian(selectedYear + 1, 1, 1)
                ).toInt().coerceIn(29, 30)
            }.getOrDefault(29)
        }
    }

    val maxYear = remember { JalaliCalendar.gregorianToJalali(LocalDate.now()).year }

    // The first day of the last period cannot be in the future (it would never count as data).
    val isFutureDate = remember(selectedYear, selectedMonth, selectedDay) {
        runCatching {
            JalaliCalendar.jalaliToGregorian(selectedYear, selectedMonth, selectedDay).isAfter(LocalDate.now())
        }.getOrDefault(false)
    }

    // Adjust selected day if it exceeds the max days of the new month
    LaunchedEffect(selectedMonth, selectedYear) {
        if (selectedDay > maxDays) {
            onDayChange(maxDays)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Step 2 Calendar sheet illustration using elegant Canvas drawings
        Canvas(modifier = Modifier.size(110.dp)) {
            val w = size.width
            val h = size.height
            // Draw a stylish wireframe mini calendar
            drawRoundRect(
                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
            )
            // Header bar
            drawRoundRect(
                color = Color(0xFFE879A0),
                size = androidx.compose.ui.geometry.Size(w, h * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            // Binder spiral rings
            for (i in 1..4) {
                val rX = w * 0.2f * i
                drawCircle(color = Color(0xFF0F0C20), radius = 6f, center = Offset(rX, h * 0.28f))
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(rX, h * 0.1f),
                    end = Offset(rX, h * 0.35f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            // Calendar active grid points
            for (col in 0..2) {
                for (row in 0..2) {
                    val pX = w * 0.25f + col * w * 0.25f
                    val pY = h * 0.45f + row * h * 0.18f
                    val isFirst = col == 1 && row == 1
                    drawCircle(
                        color = if (isFirst) Color(0xFFE879A0) else Color.White.copy(alpha = 0.3f),
                        radius = if (isFirst) 10f else 6f,
                        center = Offset(pX, pY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "آخرین شروع قاعدگی",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "اولین روز آخرین پریود خودت رو ثبت کن:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Customizable Inline Persian Roller Pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day selector Column
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("روز", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                RollerControls(
                    value = selectedDay,
                    onIncrement = { if (selectedDay < maxDays) onDayChange(selectedDay + 1) else onDayChange(1) },
                    onDecrement = { if (selectedDay > 1) onDayChange(selectedDay - 1) else onDayChange(maxDays) }
                )
            }

            // Month selector Column
            Column(modifier = Modifier.weight(1.8f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ماه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                RollerTextControls(
                    valueText = months[selectedMonth - 1],
                    onIncrement = { if (selectedMonth < 12) onMonthChange(selectedMonth + 1) else onMonthChange(1) },
                    onDecrement = { if (selectedMonth > 1) onMonthChange(selectedMonth - 1) else onMonthChange(12) }
                )
            }

            // Year selector Column
            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("سال", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                RollerControls(
                    value = selectedYear,
                    onIncrement = { if (selectedYear < maxYear) onYearChange(selectedYear + 1) },
                    onDecrement = { if (selectedYear > maxYear - 2) onYearChange(selectedYear - 1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isFutureDate) "تاریخ شروع آخرین پریود نمی‌تواند در آینده باشد."
                else "تاریخ انتخابی شما: $selectedDay ${months[selectedMonth - 1]} $selectedYear",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFutureDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("بازگشت", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNext,
                enabled = !isFutureDate,
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("مرحله بعدی", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onUnknown) {
            Text("تاریخ دقیقش یادم نیست؛ بعداً در تقویم ثبت می‌کنم", fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StepCycleLength(
    averageCycleLength: Float,
    onCycleLengthChange: (Float) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Step 3 Slider Arc Speedometer illustration using elegant Canvas drawings
        val trackBgColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        val trackActiveColor = MaterialTheme.colorScheme.primary
        val trackCenterColor = MaterialTheme.colorScheme.secondary
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                drawArc(
                    color = trackBgColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 10f, cap = StrokeCap.Round),
                    size = size
                )
                val fraction = (averageCycleLength - 21f) / (45f - 21f)
                drawArc(
                    color = trackActiveColor,
                    startAngle = 180f,
                    sweepAngle = 180f * fraction,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round),
                    size = size
                )
                // Center node
                drawCircle(color = trackCenterColor, radius = size.width * 0.15f, center = center)
            }
            Text(
                averageCycleLength.toInt().toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "طول چرخه قاعدگی",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "فاصله متوسط اول هر پریود تا پریود بعدی شما چند روز است؟ (فاصله طبیعی بین ۲۱ تا ۴۵ روز است)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "${averageCycleLength.toInt()} روز",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )

        Slider(
            value = averageCycleLength,
            onValueChange = onCycleLengthChange,
            valueRange = 21f..45f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.padding(horizontal = 16.dp).testTag("cycle_length_slider")
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("بازگشت", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp)
                    .testTag("cycle_length_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("مرحله بعدی", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun RollerControls(
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "کاهش", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = value.toString(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp)
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "افزایش", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun RollerTextControls(
    valueText: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "کاهش", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = valueText,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "افزایش", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
