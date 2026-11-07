package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tools.BackupCrypto

/**
 * Shows the PIN recovery code once. The user has to confirm that it was written down, because it is
 * the only way back in if the PIN is forgotten (nothing is stored on a server).
 */
@Composable
fun RecoveryCodeShownDialog(code: String, onDone: () -> Unit) {
    var saved by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { /* must be confirmed explicitly */ },
        title = { Text("کد بازیابی رمز") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "اگر رمز را فراموش کنید، فقط با این کد می‌توانید دوباره وارد برنامه شوید. آن را روی کاغذ بنویسید یا در جای امنی نگه دارید. این کد دیگر نمایش داده نمی‌شود.",
                    fontSize = 13.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(code))
                    android.widget.Toast.makeText(context, "کد کپی شد؛ آن را در جای امنی ذخیره کنید.", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("کپی کد") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saved, onCheckedChange = { saved = it })
                    Text("کد را یادداشت کردم", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = saved, onClick = onDone) { Text("ادامه") }
        }
    )
}

/**
 * Asks for an optional password before exporting a backup ([forExport] = true, with a repeat field),
 * or for the password of an encrypted backup before restoring it.
 */
@Composable
fun BackupPasswordDialog(
    forExport: Boolean,
    errorText: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (password: String?) -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(errorText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (forExport) "رمز فایل پشتیبان" else "این فایل رمزدار است") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (forExport) "پیشنهاد می‌کنیم برای فایل پشتیبان رمز بگذارید تا اگر فایل به دست کسی افتاد، اطلاعات سلامت شما خوانده نشود. رمز را فراموش نکنید؛ بدون آن فایل باز نمی‌شود."
                    else "رمزی را که هنگام تهیه پشتیبان انتخاب کردید وارد کنید.",
                    fontSize = 13.sp
                )
                PasswordField(first, if (forExport) "رمز (حداقل ${BackupCrypto.MIN_PASSWORD_LENGTH} نویسه)" else "رمز فایل") { first = it; error = null }
                if (forExport) PasswordField(second, "تکرار رمز") { second = it; error = null }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    first.length < (if (forExport) BackupCrypto.MIN_PASSWORD_LENGTH else 1) ->
                        if (forExport) "رمز باید حداقل ${BackupCrypto.MIN_PASSWORD_LENGTH} نویسه باشد." else "رمز را وارد کنید."
                    forExport && first != second -> "دو رمز یکسان نیستند."
                    else -> null
                }
                if (error == null) onConfirm(first)
            }) { Text(if (forExport) "ذخیره با رمز" else "بازیابی") }
        },
        dismissButton = {
            if (forExport) {
                TextButton(onClick = { onConfirm(null) }) { Text("بدون رمز") }
            } else {
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
private fun PasswordField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(64)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
