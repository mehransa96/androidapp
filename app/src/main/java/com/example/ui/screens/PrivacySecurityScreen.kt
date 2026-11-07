package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppLockManager
import com.example.security.AppLockSession
import com.example.tools.BackupManager
import com.example.ui.viewmodel.HealthToolsViewModel

/** App lock, local backup/restore and "delete everything". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    viewModel: HealthToolsViewModel,
    onNavigateBack: () -> Unit,
    onSecureScreenChanged: () -> Unit,
    onRestartApp: () -> Unit
) {
    val context = LocalContext.current
    val lock = remember { AppLockManager(context) }
    var lockEnabled by remember { mutableStateOf(lock.isEnabled) }
    var biometricOn by remember { mutableStateOf(lock.biometricEnabled) }
    var secureOn by remember { mutableStateOf(lock.secureScreen) }
    val biometricAvailable = remember { lock.isBiometricAvailable() }

    var showSetPin by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }
    /** Changing the PIN or making a new recovery code needs the current PIN first. */
    var verifyForChange by remember { mutableStateOf(false) }
    var verifyForRecovery by remember { mutableStateOf(false) }
    var shownRecoveryCode by remember { mutableStateOf<String?>(null) }
    var askExportPassword by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var confirmSnapshot by remember { mutableStateOf<com.example.tools.LocalSnapshots.Snapshot?>(null) }
    val snapshots by viewModel.snapshots.collectAsState()
    val passwordRequest by viewModel.passwordRequest.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshSnapshots() }
    var showRestoreConfirm by remember { mutableStateOf<android.net.Uri?>(null) }
    var showWipe by remember { mutableStateOf(false) }

    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportBackup(uri, pendingExportPassword)
        pendingExportPassword = null
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) showRestoreConfirm = uri
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("حریم خصوصی و پشتیبان", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                "همه اطلاعات شما فقط روی همین گوشی و در حافظه خصوصی برنامه ذخیره می‌شود و به هیچ سروری ارسال نمی‌شود (مگر وقتی خودتان از دستیار هوشمند استفاده کنید و اجازه بدهید). با به‌روزرسانی برنامه هم چیزی پاک نمی‌شود.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            SectionCard(Icons.Filled.Lock, "قفل برنامه") {
                SwitchRow(
                    title = "قفل با رمز عددی",
                    subtitle = "هنگام باز کردن برنامه و برگشت از پس‌زمینه، رمز خواسته می‌شود. ویجت هم اطلاعاتی نشان نمی‌دهد.",
                    checked = lockEnabled,
                    onCheckedChange = { on -> if (on) showSetPin = true else showDisablePin = true }
                )
                if (lockEnabled) {
                    HorizontalDivider()
                    if (biometricAvailable) {
                        SwitchRow(
                            title = "ورود با اثر انگشت یا چهره",
                            subtitle = "رمز عددی همچنان به‌عنوان جایگزین فعال است.",
                            checked = biometricOn,
                            onCheckedChange = { on ->
                                if (on) {
                                    showBiometricPrompt(context, title = "تأیید اثر انگشت") {
                                        lock.biometricEnabled = true
                                        biometricOn = true
                                    }
                                } else {
                                    lock.biometricEnabled = false
                                    biometricOn = false
                                }
                            }
                        )
                    } else {
                        Text("ورود بیومتریک روی این گوشی در دسترس نیست یا اثر انگشتی ثبت نشده است.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Row {
                        TextButton(onClick = { verifyForChange = true }) { Text("تغییر رمز") }
                        TextButton(onClick = { verifyForRecovery = true }) {
                            Text(if (lock.hasRecoveryCode) "کد بازیابی جدید" else "ساخت کد بازیابی")
                        }
                    }
                    if (!lock.hasRecoveryCode) {
                        Text(
                            "برای این قفل کد بازیابی ندارید. اگر رمز را فراموش کنید راهی برای ورود نمی‌ماند؛ همین حالا یک کد بازیابی بسازید.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                HorizontalDivider()
                SwitchRow(
                    title = "پنهان کردن در برنامه‌های اخیر",
                    subtitle = "پیش‌نمایش برنامه در فهرست برنامه‌های اخیر نمایش داده نمی‌شود و گرفتن اسکرین‌شات ممکن نیست.",
                    checked = secureOn,
                    onCheckedChange = { on ->
                        lock.secureScreen = on
                        secureOn = on
                        onSecureScreenChanged()
                    }
                )
            }

            SectionCard(Icons.Filled.Backup, "پشتیبان‌گیری و بازیابی") {
                Text(
                    "یک فایل پشتیبان از همه سوابق (روزها، علائم، چرخه‌ها، ابزارهای سلامت و تنظیمات) بسازید و در حافظه گوشی، فلش یا Google Drive نگه دارید. برای رفتن به گوشی جدید هم از همین فایل استفاده کنید. می‌توانید فایل را با رمز قفل کنید.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            askExportPassword = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("تهیه پشتیبان") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            AppLockSession.skipNextLock = true
                            restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("بازیابی")
                    }
                }
            }

            SectionCard(Icons.Filled.Restore, "نسخه‌های امن خودکار") {
                Text(
                    "هانیتا هر روز یک نسخه رمزنگاری‌شده از همه اطلاعات شما در حافظه خصوصی گوشی نگه می‌دارد (۷ روز آخر). کلید رمز در سخت‌افزار امن گوشی است و از آن خارج نمی‌شود. اگر اطلاعاتی گم شد، از اینجا برگردانید.",
                    fontSize = 13.sp
                )
                if (snapshots.isEmpty()) {
                    Text("هنوز نسخه خودکاری ساخته نشده است.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    snapshots.forEach { snap ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(com.example.tools.LocalSnapshots.formatDate(snap), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            TextButton(enabled = !busy, onClick = { confirmSnapshot = snap }) { Text("بازیابی") }
                        }
                    }
                }
                OutlinedButton(enabled = !busy, onClick = { viewModel.takeSnapshotNow() }, modifier = Modifier.fillMaxWidth()) {
                    Text("ذخیره نسخه امن همین حالا")
                }
            }

            SectionCard(Icons.Filled.DeleteForever, "حذف همه داده‌ها", danger = true) {
                Text(
                    "همه سوابق، تنظیمات، نسخه‌های خودکار و حساب کاربری روی این گوشی برای همیشه پاک می‌شود و برنامه از اول شروع می‌شود.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !busy,
                    onClick = { showWipe = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("حذف همه داده‌های این گوشی") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onConfirm = { pin ->
                val firstTime = !lock.isEnabled || !lock.hasRecoveryCode
                lock.setPin(pin)
                lockEnabled = true
                showSetPin = false
                if (firstTime) shownRecoveryCode = lock.createRecoveryCode()
            }
        )
    }
    if (verifyForChange || verifyForRecovery) {
        VerifyPinDialog(
            lock = lock,
            title = if (verifyForChange) "تأیید رمز فعلی" else "ساخت کد بازیابی",
            onDismiss = { verifyForChange = false; verifyForRecovery = false },
            onVerified = {
                if (verifyForChange) showSetPin = true else shownRecoveryCode = lock.createRecoveryCode()
                verifyForChange = false
                verifyForRecovery = false
            }
        )
    }
    shownRecoveryCode?.let { code ->
        com.example.ui.components.RecoveryCodeShownDialog(code = code, onDone = { shownRecoveryCode = null })
    }
    if (askExportPassword) {
        com.example.ui.components.BackupPasswordDialog(
            forExport = true,
            onDismiss = { askExportPassword = false },
            onConfirm = { password ->
                askExportPassword = false
                pendingExportPassword = password
                AppLockSession.skipNextLock = true
                exportLauncher.launch(BackupManager.suggestedFileName(encrypted = password != null))
            }
        )
    }
    passwordRequest?.let { req ->
        com.example.ui.components.BackupPasswordDialog(
            forExport = false,
            errorText = if (req.wrongPassword) "رمز اشتباه است یا فایل آسیب دیده است." else null,
            onDismiss = { viewModel.cancelPasswordRequest() },
            onConfirm = { password -> viewModel.restoreBackup(req.uri, password, onRestartApp) }
        )
    }
    confirmSnapshot?.let { snap ->
        AlertDialog(
            onDismissRequest = { confirmSnapshot = null },
            title = { Text("بازیابی نسخه خودکار؟") },
            text = { Text("اطلاعات نسخه «${com.example.tools.LocalSnapshots.formatDate(snap)}» به برنامه برگردانده می‌شود؛ روزهایی که در هر دو هست با نسخه پشتیبان جایگزین می‌شوند و بقیه اطلاعات فعلی می‌ماند. سپس برنامه دوباره باز می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSnapshot = null
                    viewModel.restoreSnapshot(snap, onRestartApp)
                }) { Text("بازیابی") }
            },
            dismissButton = { TextButton(onClick = { confirmSnapshot = null }) { Text("انصراف") } }
        )
    }
    if (showDisablePin) {
        VerifyPinDialog(
            lock = lock,
            onDismiss = { showDisablePin = false },
            onVerified = {
                lock.disable()
                lockEnabled = false
                biometricOn = false
                showDisablePin = false
            }
        )
    }
    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text("بازیابی از فایل پشتیبان؟") },
            text = { Text("سوابق فایل به داده‌های فعلی اضافه می‌شوند و روزهای مشترک با نسخه فایل جایگزین می‌شوند. سپس برنامه دوباره باز می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = null
                    viewModel.restoreBackup(uri, null, onRestartApp)
                }) { Text("بازیابی") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = null }) { Text("انصراف") } }
        )
    }
    if (showWipe) {
        var typed by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWipe = false },
            title = { Text("حذف دائمی همه داده‌ها") },
            text = {
                Column {
                    Text("این کار برگشت‌پذیر نیست. اگر پشتیبان ندارید، اول پشتیبان بگیرید. برای تأیید، کلمه «حذف» را بنویسید.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = typed, onValueChange = { typed = it }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim() == "حذف",
                    onClick = {
                        showWipe = false
                        viewModel.wipeAll(onRestartApp)
                    }
                ) { Text("حذف کن", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showWipe = false }) { Text("انصراف") } }
        )
    }
}

@Composable
internal fun SectionCard(
    icon: ImageVector,
    title: String,
    danger: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (danger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            content()
        }
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعیین رمز قفل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("رمز ۴ تا ۶ رقمی انتخاب کنید. بعد از ذخیره، یک کد بازیابی به شما داده می‌شود تا اگر رمز را فراموش کردید، بتوانید دوباره وارد شوید.", fontSize = 13.sp)
                PinField(first, "رمز جدید") { first = it; error = null }
                PinField(second, "تکرار رمز") { second = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    !AppLockManager.isValidPin(first) -> "رمز باید ۴ تا ۶ رقم باشد."
                    first != second -> "دو رمز یکسان نیستند."
                    else -> null
                }
                if (error == null) onConfirm(first)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun VerifyPinDialog(lock: AppLockManager, title: String = "خاموش کردن قفل", onDismiss: () -> Unit, onVerified: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(pin, "رمز فعلی") { pin = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (lock.verify(pin)) onVerified()
                else error = if (lock.remainingBlockMillis() > 0) "تلاش زیاد؛ کمی بعد دوباره امتحان کنید." else "رمز اشتباه است."
            }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun PinField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(AppLockManager.normalizeDigits(v).take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}
