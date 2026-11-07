package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.AppLockManager
import kotlinx.coroutines.delay

/**
 * Full-screen lock shown above the whole app. It is a dialog window so that it also covers any
 * dialog that was open when the app went to the background. Back sends the app to the
 * background instead of revealing the screen below.
 */
@Composable
fun AppLockScreen(
    lock: AppLockManager,
    onUnlocked: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var blockedSeconds by remember { mutableIntStateOf((lock.remainingBlockMillis() / 1000).toInt()) }
    val biometric = remember { lock.biometricEnabled && lock.isBiometricAvailable() }
    var showRecovery by remember { mutableStateOf(false) }


    LaunchedEffect(blockedSeconds) {
        if (blockedSeconds > 0) {
            delay(1000)
            blockedSeconds = (lock.remainingBlockMillis() / 1000).toInt()
            if (blockedSeconds <= 0) error = null
        }
    }
    LaunchedEffect(Unit) {
        if (biometric) showBiometricPrompt(context, onSuccess = onUnlocked)
    }

    fun press(digit: Char) {
        if (blockedSeconds > 0 || pin.length >= 6) return
        pin += digit
        error = null
        if (pin.length == lock.pinLength) {
            if (lock.verify(pin)) {
                onUnlocked()
            } else {
                pin = ""
                val left = (lock.remainingBlockMillis() / 1000).toInt()
                if (left > 0) {
                    blockedSeconds = left
                    error = "تلاش‌های ناموفق زیاد بود. لطفاً کمی صبر کنید."
                } else {
                    error = "رمز اشتباه است (${AppLockManager.MAX_ATTEMPTS - lock.failedAttempts()} تلاش دیگر)"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
    if (showRecovery) {
        RecoveryCodeDialog(
            lock = lock,
            onDismiss = { showRecovery = false },
            onReset = {
                showRecovery = false
                onUnlocked()
            }
        )
    }
    // The keypad keeps the familiar phone order (1 2 3 from left to right) even in RTL.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                // Swallow every touch so nothing below the lock can be reached.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("هانیتا قفل است", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (blockedSeconds > 0) "$blockedSeconds ثانیه دیگر دوباره تلاش کنید" else "رمز ${lock.pinLength} رقمی خود را وارد کنید",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.semantics { contentDescription = "${pin.length} رقم وارد شده" }
                ) {
                    repeat(lock.pinLength) { i ->
                        Box(
                            Modifier.size(14.dp).clip(CircleShape).background(
                                if (i < pin.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    error ?: " ",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                val rows = listOf("123", "456", "789")
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { d -> PadKey(label = d.toString(), onClick = { press(d) }) }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (biometric) {
                        PadIcon(Icons.Filled.Fingerprint, "ورود با اثر انگشت") { showBiometricPrompt(context, onSuccess = onUnlocked) }
                    } else {
                        Spacer(Modifier.width(72.dp))
                    }
                    PadKey(label = "0", onClick = { press('0') })
                    PadIcon(Icons.AutoMirrored.Filled.Backspace, "پاک کردن") { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = { showRecovery = true }) {
                    Text("رمز را فراموش کرده‌ام", fontSize = 14.sp)
                }
            }
        }
    }
    }
}

@Composable
private fun PadKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PadIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(72.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
    }
}

/** Platform biometric prompt (Android 10+). Failures simply leave the PIN pad in place. */
fun showBiometricPrompt(context: Context, title: String = "باز کردن هانیتا", onSuccess: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    try {
        val executor = context.mainExecutor
        val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle("برای ورود از اثر انگشت یا چهره استفاده کنید")
            .setNegativeButton("استفاده از رمز", executor) { _, _ -> }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }
            }
        )
    } catch (e: Exception) {
        android.util.Log.w("AppLock", "Biometric prompt unavailable", e)
    }
}

/** Resets a forgotten PIN with the recovery code that was shown when the lock was turned on. */
@Composable
private fun RecoveryCodeDialog(lock: AppLockManager, onDismiss: () -> Unit, onReset: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("بازیابی رمز") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!lock.hasRecoveryCode) {
                        Text(
                            "برای این قفل کد بازیابی ساخته نشده است. اگر رمز را به خاطر نمی‌آورید، تنها راه پاک کردن داده‌های برنامه از تنظیمات اندروید است؛ اگر فایل پشتیبان دارید، بعد از آن می‌توانید اطلاعات را بازیابی کنید.",
                            fontSize = 13.sp
                        )
                    } else {
                        Text("کد بازیابی ۱۲ حرفی که هنگام فعال کردن قفل دریافت کردید و یک رمز جدید وارد کنید.", fontSize = 13.sp)
                        androidx.compose.material3.OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.take(20); error = null },
                            label = { Text("کد بازیابی (مثل ABCD-EFGH-JKMN)") },
                            singleLine = true
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = newPin,
                            onValueChange = { v -> newPin = AppLockManager.normalizeDigits(v).take(6); error = null },
                            label = { Text("رمز جدید ۴ تا ۶ رقمی") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            )
                        )
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = {
                if (lock.hasRecoveryCode) {
                    androidx.compose.material3.TextButton(onClick = {
                        error = when {
                            !AppLockManager.isValidPin(newPin) -> "رمز جدید باید ۴ تا ۶ رقم باشد."
                            lock.remainingBlockMillis() > 0 -> "تلاش زیاد؛ کمی بعد دوباره امتحان کنید."
                            lock.resetPinWithRecoveryCode(code, newPin) -> null
                            else -> "کد بازیابی درست نیست."
                        }
                        if (error == null) onReset()
                    }) { Text("تغییر رمز و ورود") }
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("بستن") } }
        )
    }
}
