package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HomeTab
import com.example.ui.screens.LogScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoodScreen
import com.example.ui.screens.ChatBotScreen
import com.example.ui.screens.MeditationMusicScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.AvatarGeneratorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MahitaViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        private val DEEP_LINK_ROUTES = setOf("log", "calendar", "reminders", "tools")
    }

    private lateinit var lockManager: com.example.security.AppLockManager
    /** True while the app-lock screen covers the app (set again every time the app goes to background). */
    private var isLocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockManager = com.example.security.AppLockManager(this)
        isLocked = lockManager.isEnabled
        applySecureFlag()
        
        enableEdgeToEdge()
        setContent {
            val crashPrefs = remember { getSharedPreferences(HanitaApplication.CRASH_PREFS, android.content.Context.MODE_PRIVATE) }
            var crashStackTrace by remember { mutableStateOf(crashPrefs.getString(HanitaApplication.KEY_LAST_CRASH, null)) }
            var confirmReset by remember { mutableStateOf(false) }

            if (crashStackTrace != null) {
                // Beautiful Recovery and Crash Diagnosis Screen in Farsi
                MyApplicationTheme(themeCode = "COSMIC_BLOSSOM", themeMode = "DARK", fontCode = "CLASSIC") {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "سامانه ایمن بازیابی خطا 🌸",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "پوزش می‌طلبیم؛ برنامه با یک خطای فنی متوقف شد. برای بازیابی می‌توانید حافظه موقت را بازنشانی کرده یا سیستم را دوباره اجرا کنید.",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                                        Text(
                                            text = "گزارش خطا (جهت ارسال به پشتیبان):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = crashStackTrace ?: "No Trace",
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                if (confirmReset) {
                                    AlertDialog(
                                        onDismissRequest = { confirmReset = false },
                                        title = { Text("حذف همه اطلاعات؟") },
                                        text = { Text("با این کار همه سوابق چرخه، علائم و تنظیمات روی این گوشی برای همیشه پاک می‌شود. پیشنهاد می‌کنیم ابتدا «تلاش مجدد» را امتحان کنید.") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                confirmReset = false
                                                resetAllLocalData()
                                            }) { Text("بله، پاک شود", color = MaterialTheme.colorScheme.error) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { confirmReset = false }) { Text("انصراف") }
                                        }
                                    )
                                }
                                Button(
                                    onClick = { confirmReset = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("بازنشانی کامل و حذف اطلاعات 🔄", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                OutlinedButton(
                                    onClick = {
                                        crashPrefs.edit().remove(HanitaApplication.KEY_LAST_CRASH).apply()
                                        crashStackTrace = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تلاش مجدد (بدون حذف داده‌ها) 🚀")
                                }
                            }
                        }
                    }
                }
            } else {
                val viewModel: MahitaViewModel = viewModel()
            val appTheme by viewModel.appTheme.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val appFont by viewModel.appFont.collectAsState()

            MyApplicationTheme(
                themeCode = appTheme,
                themeMode = themeMode,
                fontCode = appFont,
                onThemeChanged = { viewModel.setAppTheme(it) },
                onThemeModeChanged = { viewModel.setThemeMode(it) }
            ) {
                // Ensure layout is Right-to-Left for Persian language
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                  Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                        com.example.ui.components.AiConsentDialog(viewModel)
                        DataRecoveryDialog(viewModel)
                        NotificationPermissionRequest(isLoggedIn)

                        // Notification shortcuts ("record today's symptoms", "open calendar").
                        // Only known screens are accepted, and only for an onboarded user.
                        val deepLink = remember { intent?.getStringExtra(EXTRA_NAVIGATE_TO) }
                        LaunchedEffect(deepLink, isLoggedIn) {
                            if (isLoggedIn && deepLink in DEEP_LINK_ROUTES) {
                                intent?.removeExtra(EXTRA_NAVIGATE_TO)
                                navController.navigate(deepLink!!) { launchSingleTop = true }
                            }
                        }

                        NavHost(
                            navController = navController, 
                            startDestination = if (isLoggedIn) "dashboard" else "login",
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(300))
                            }
                        ) {
                            composable("login") {
                                val healthVm: com.example.ui.viewmodel.HealthToolsViewModel = viewModel()
                                LoginScreen(
                                    lockManager = lockManager,
                                    restoreViewModel = healthVm,
                                    onRestored = { restartApp() },
                                    onRegistered = { name, cycleLength, lastPeriodDateStr ->
                                        viewModel.register(name, cycleLength, lastPeriodDateStr)
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("dashboard") {
                                DashboardScreen(
                                    // The partner-alert notification opens the "همراه" tab directly.
                                    startTab = if (deepLink == "partner") HomeTab.PARTNER.ordinal else 0,
                                    viewModel = viewModel,
                                    onNavigateToLog = { navController.navigateOnce("log") },
                                    onNavigateToCalendar = { navController.navigateOnce("calendar") },
                                    onNavigateToHealth = { navController.navigateOnce("health") },
                                    onNavigateToEducation = { navController.navigateOnce("education") },
                                    onNavigateToMood = { navController.navigateOnce("mood") },
                                    onNavigateToChat = { navController.navigateOnce("chatbot") },
                                    onNavigateToMeditation = { navController.navigateOnce("meditation") },
                                    onNavigateToReminders = { navController.navigateOnce("reminders") },
                                    onNavigateToAvatarGenerator = { navController.navigateOnce("avatargenerator") },
                                    onNavigateToHistory = { navController.navigateOnce("history") },
                                    onNavigateToCharts = { navController.navigateOnce("charts") },
                                    onNavigateToTools = { navController.navigateOnce("tools") },
                                    onNavigateToPrivacy = { navController.navigateOnce("privacy") },
                                    onLock = {
                                        if (lockManager.isEnabled) {
                                            isLocked = true
                                        } else {
                                            android.widget.Toast.makeText(
                                                this@MainActivity,
                                                "برای قفل کردن برنامه، ابتدا رمز قفل را فعال کنید.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            navController.navigateOnce("privacy")
                                        }
                                    }
                                )
                            }
                            composable("avatargenerator") {
                                AvatarGeneratorScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("reminders") {
                                RemindersScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("meditation") {
                                MeditationMusicScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("health") {
                                com.example.ui.screens.HealthScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("education") {
                                com.example.ui.screens.EducationScreen(
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("mood") {
                                MoodScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("chatbot") {
                                ChatBotScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("log") {
                                LogScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.safePopBackStack() }
                                )
                            }
                            composable(
                                route = "history",
                                enterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(500, easing = LinearOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(500))
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(500, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(500))
                                },
                                popEnterTransition = {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(500, easing = LinearOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(500))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(500, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(500))
                                }
                            ) {
                                com.example.ui.screens.CycleHistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable(
                                route = "charts",
                                enterTransition = {
                                    scaleIn(
                                        initialScale = 0.9f,
                                        animationSpec = tween(400, easing = LinearOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(400))
                                },
                                exitTransition = {
                                    scaleOut(
                                        targetScale = 0.9f,
                                        animationSpec = tween(400, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(400))
                                },
                                popEnterTransition = {
                                    scaleIn(
                                        initialScale = 0.9f,
                                        animationSpec = tween(400, easing = LinearOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(400))
                                },
                                popExitTransition = {
                                    scaleOut(
                                        targetScale = 0.9f,
                                        animationSpec = tween(400, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(400))
                                }
                            ) {
                                com.example.ui.screens.SymptomChartsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("calendar") {
                                CalendarScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("tools") {
                                val healthVm: com.example.ui.viewmodel.HealthToolsViewModel = viewModel()
                                com.example.ui.screens.HealthToolsScreen(
                                    viewModel = healthVm,
                                    onNavigateBack = { navController.safePopBackStack() }
                                )
                            }
                            composable("privacy") {
                                val healthVm: com.example.ui.viewmodel.HealthToolsViewModel = viewModel()
                                com.example.ui.screens.PrivacySecurityScreen(
                                    viewModel = healthVm,
                                    onNavigateBack = { navController.safePopBackStack() },
                                    onSecureScreenChanged = { applySecureFlag() },
                                    onRestartApp = { restartApp() }
                                )
                            }
                        }
                    }
                    if (isLocked) {
                        com.example.ui.screens.AppLockScreen(
                            lock = lockManager,
                            onUnlocked = { isLocked = false },
                            onExit = { moveTaskToBack(true) }
                        )
                    }
                  }
                }
            }
        }
    }
}

    override fun onStop() {
        super.onStop()
        if (com.example.security.AppLockSession.skipNextLock) {
            // A file picker opened by the app itself; do not lock behind the user's back.
            com.example.security.AppLockSession.skipNextLock = false
        } else if (!isChangingConfigurations && lockManager.isEnabled) {
            isLocked = true
        }
        // Keep the home-screen widget in step with what was just logged.
        com.example.tools.CycleWidgetProvider.requestUpdate(this)
        // Refresh the encrypted automatic safety copy (at most every few hours).
        if (com.example.data.preferences.UserPreferencesManager(this).isLoggedIn.value) {
            com.example.tools.LocalSnapshots.takeInBackground(applicationContext)
        }
    }

    /** FLAG_SECURE hides the app preview in "recent apps" and blocks screenshots. */
    fun applySecureFlag() {
        if (lockManager.secureScreen) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /** Starts the app again from scratch (after restoring a backup or deleting all data). */
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
    }

    /** Deletes all local data (only after explicit confirmation on the recovery screen). */
    @android.annotation.SuppressLint("ApplySharedPref") // must be on disk before the process restarts
    private fun resetAllLocalData() {
        // A last safety copy before the reset (if the database can still be read).
        try {
            kotlinx.coroutines.runBlocking { com.example.tools.LocalSnapshots.takeIfDue(applicationContext, force = true) }
        } catch (_: Throwable) {
        }
        getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(HanitaApplication.CRASH_PREFS, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(com.example.data.preferences.WellnessPrefs.NAME, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(com.example.security.AppLockManager.NAME, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        getSharedPreferences(com.example.tools.PartnerPrefs.NAME, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        try {
            com.example.data.AppDatabase.closeAndForget()
            deleteDatabase("hanita_database")
        } catch (_: Exception) {
        }
        com.example.data.preferences.UserPreferencesManager(this).reload()
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
    }
}

/**
 * Period and phase reminders are the core of the app, but on Android 13+ they are silently
 * dropped until the user grants POST_NOTIFICATIONS. Ask once, right after onboarding, instead of
 * only when the user happens to open the reminders screen.
 */
@Composable
private fun NotificationPermissionRequest(isLoggedIn: Boolean) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("hanita_permissions", android.content.Context.MODE_PRIVATE) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* Reminders are rescheduled anyway; the user can change this later in settings. */ }
    LaunchedEffect(isLoggedIn) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (isLoggedIn && !granted && !prefs.getBoolean("notifications_asked", false)) {
            prefs.edit().putBoolean("notifications_asked", true).apply()
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Pops only when there is a screen to go back to. Tapping a back arrow twice quickly used to pop
 * the dashboard too and leave an empty white screen.
 */
private fun androidx.navigation.NavController.safePopBackStack() {
    if (previousBackStackEntry != null) popBackStack()
}

/** Ignores a second tap while the same screen is already on top (no duplicate screens). */
private fun androidx.navigation.NavController.navigateOnce(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true }
}

/**
 * Shown when a registered user's database is found empty while an automatic encrypted copy still
 * holds their data (for example after storage corruption). Nothing is restored without consent.
 */
@Composable
private fun DataRecoveryDialog(viewModel: MahitaViewModel) {
    val offer by viewModel.recoveryOffer.collectAsState()
    val current = offer ?: return
    AlertDialog(
        onDismissRequest = { },
        title = { Text("بازیابی اطلاعات شما") },
        text = {
            Text(
                "به نظر می‌رسد اطلاعات ثبت‌شده شما روی این گوشی پیدا نمی‌شود، اما یک نسخه امن خودکار از تاریخ ${current.label} با ${current.records} مورد ثبت‌شده موجود است. آن را بازیابی کنیم؟"
            )
        },
        confirmButton = { TextButton(onClick = { viewModel.acceptRecovery() }) { Text("بازیابی کن") } },
        dismissButton = { TextButton(onClick = { viewModel.dismissRecovery() }) { Text("فعلاً نه") } }
    )
}
