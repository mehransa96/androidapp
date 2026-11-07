package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PartnerAlerts
import com.example.tools.PartnerPrefs
import com.example.tools.PartnerReminders
import com.example.ui.components.PartnerSyncDialog
import com.example.ui.viewmodel.CycleInfo
import com.example.ui.viewmodel.PredictionInfo
import com.example.utils.JalaliCalendar
import com.example.utils.RescheduleWorker
import java.time.LocalDate

private val Rose = Color(0xFFE11D48)

/**
 * "همراه" tab: partner's name and mobile number, a message ready for today, the next sensitive
 * days and the alert settings. Everything is stored only on this phone; nothing is sent without
 * the user pressing send in the SMS app or messenger.
 */
@Composable
fun PartnerTab(
    userName: String,
    cycleInfo: CycleInfo,
    predictionInfo: PredictionInfo,
    onOpenSettings: () -> Unit,
    onLock: () -> Unit,
    today: LocalDate = LocalDate.now()
) {
    val context = LocalContext.current
    val prefs = remember { PartnerPrefs(context) }
    // Bumped after every change so the UI re-reads the preferences.
    var version by remember { mutableIntStateOf(0) }
    fun changed() {
        version++
        RescheduleWorker.runNow(context) // re-arm the partner alarm with the new settings
    }

    val name = remember(version) { prefs.partnerName }
    val phone = remember(version) { prefs.partnerPhone }
    val alertsOn = remember(version) { prefs.alertsEnabled }
    val events = remember(version) { prefs.events }
    val tone = remember(version) { prefs.tone }
    val daysBefore = remember(version) { prefs.daysBeforePeriod }
    val normalizedPhone = PartnerAlerts.normalizePhone(phone)

    var showEditor by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val todayEvent = PartnerAlerts.eventForToday(
        cycleInfo.isPeriod, cycleInfo.isPms, cycleInfo.isFertile, cycleInfo.daysUntilNextPeriod, daysBefore
    )
    val todayMessage = if (todayEvent == null) {
        PartnerAlerts.everydayMessage(name, tone)
    } else {
        PartnerAlerts.message(
            todayEvent, name, tone,
            if (todayEvent == PartnerAlerts.Event.PERIOD_SOON) cycleInfo.daysUntilNextPeriod else daysBefore
        )
    }
    val upcoming = remember(version, predictionInfo, today) {
        PartnerAlerts.upcoming(
            predictionInfo.lastPeriodStart, predictionInfo.calculatedAverage, today,
            events, daysBefore
        ).take(3)
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabHeader("همراه و همدلی 💞", "اطلاع‌رسانی روزهای حساس به همسر، فقط با تأیید شما", onOpenSettings, onLock)

        // Partner card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("partner_profile_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Rose.copy(alpha = 0.07f)),
            border = BorderStroke(1.dp, Rose.copy(alpha = 0.25f))
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Rose.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(name.take(1).ifBlank { "💞" }, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Rose)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (normalizedPhone == null) "همسرتان را اضافه کنید" else name.ifBlank { "همسر من" },
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (normalizedPhone == null) "نام و شماره موبایل برای پیامک روزهای حساس" else phone,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (normalizedPhone != null) {
                    IconButton(onClick = { PartnerReminders.launch(context, PartnerReminders.dialIntent(normalizedPhone)) }) {
                        Icon(Icons.Filled.Call, contentDescription = "تماس با همسر", tint = Rose)
                    }
                }
                IconButton(onClick = { showEditor = true }, modifier = Modifier.testTag("partner_edit_button")) {
                    Icon(Icons.Filled.Edit, contentDescription = "ویرایش اطلاعات همسر", tint = Rose)
                }
            }
        }

        // Today's message
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    "پیام امروز ${todayEvent?.let { "• ${it.emoji} ${it.title}" } ?: "• 🌱 روز معمولی"}",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Rose
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    todayMessage,
                    modifier = Modifier.weight(1f).verticalScrollIfNeeded(),
                    fontSize = 12.5.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (normalizedPhone == null) showEditor = true
                            else PartnerReminders.launch(context, PartnerReminders.smsIntent(normalizedPhone, todayMessage))
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rose)
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پیامک", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { PartnerReminders.launch(context, PartnerReminders.shareIntent(todayMessage)) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پیام‌رسان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showGuide = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("راهنما", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sensitive-day alerts
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Rose, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text("اطلاع‌رسانی روزهای حساس", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (alertsOn) "در این روزها پیام آماده برایتان اعلان می‌شود" else "خاموش",
                            fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "تنظیم اطلاع‌رسانی", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = alertsOn,
                        onCheckedChange = { on ->
                            if (on && normalizedPhone == null) {
                                showEditor = true
                                Toast.makeText(context, "ابتدا شماره همسر را وارد کنید.", Toast.LENGTH_SHORT).show()
                            } else {
                                prefs.alertsEnabled = on
                                changed()
                            }
                        },
                        modifier = Modifier.testTag("partner_alerts_switch")
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                if (upcoming.isEmpty()) {
                    Text(
                        if (predictionInfo.lastPeriodStart == null) "برای پیش‌بینی روزهای حساس، ابتدا تاریخ پریود را ثبت کنید."
                        else "هیچ روز حساسی انتخاب نشده است.",
                        fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    upcoming.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.event.emoji, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(item.event.title, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                JalaliCalendar.gregorianToJalali(item.date).formatShort() +
                                    if (item.daysFromToday == 0L) " (امروز)" else " (${item.daysFromToday} روز دیگر)",
                                fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Rose
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        PartnerEditorDialog(
            initialName = name,
            initialPhone = phone,
            onDismiss = { showEditor = false },
            onSave = { newName, newPhone ->
                prefs.partnerName = newName
                prefs.partnerPhone = newPhone
                if (!prefs.alertsEnabled && PartnerAlerts.normalizePhone(newPhone) != null) prefs.alertsEnabled = true
                showEditor = false
                changed()
                Toast.makeText(context, "اطلاعات همسر ذخیره شد و اطلاع‌رسانی روزهای حساس فعال است 💞", Toast.LENGTH_LONG).show()
            },
            onRemove = {
                prefs.partnerName = ""
                prefs.partnerPhone = ""
                prefs.alertsEnabled = false
                showEditor = false
                changed()
            }
        )
    }

    if (showSettings) {
        PartnerAlertSettingsDialog(
            initialEvents = events,
            initialTone = tone,
            initialDaysBefore = daysBefore,
            partnerName = name,
            onDismiss = { showSettings = false },
            onSave = { newEvents, newTone, newDays ->
                prefs.events = newEvents
                prefs.tone = newTone
                prefs.daysBeforePeriod = newDays
                showSettings = false
                changed()
            }
        )
    }

    PartnerSyncDialog(
        show = showGuide,
        onDismiss = { showGuide = false },
        userName = userName,
        cycleInfo = cycleInfo
    )
}

/** Long messages scroll inside their box; short ones are unaffected. */
@Composable
private fun Modifier.verticalScrollIfNeeded(): Modifier = this.verticalScroll(rememberScrollState())

@Composable
private fun PartnerEditorDialog(
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    val valid = PartnerAlerts.normalizePhone(phone) != null

    // Picking one phone number grants access to that single contact only (no READ_CONTACTS permission).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    phone = c.getString(0).orEmpty()
                    if (name.isBlank()) name = c.getString(1).orEmpty()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "خواندن مخاطب ممکن نشد؛ شماره را دستی وارد کنید.", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اطلاعات همسر 💞", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("نام یا لقب همسر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("partner_name_field")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(20) },
                    label = { Text("شماره موبایل (مثلاً ۰۹۱۲۳۴۵۶۷۸۹)") },
                    singleLine = true,
                    isError = phone.isNotBlank() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("partner_phone_field")
                )
                TextButton(onClick = {
                    com.example.security.AppLockSession.skipNextLock = true
                    try {
                        picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    } catch (e: Exception) {
                        com.example.security.AppLockSession.skipNextLock = false
                        Toast.makeText(context, "دفترچه مخاطبین در دسترس نیست.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Filled.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("انتخاب از مخاطبین")
                }
                Text(
                    "شماره فقط روی همین گوشی ذخیره می‌شود. هانیتا هرگز بدون تأیید شما پیامی نمی‌فرستد؛ در روزهای حساس پیام آماده را با یک لمس از برنامه پیامک یا پیام‌رسان خودتان ارسال می‌کنید.",
                    fontSize = 11.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), phone.trim()) }, enabled = valid) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (initialPhone.isNotBlank()) {
                    TextButton(onClick = onRemove) { Text("حذف همسر", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
private fun PartnerAlertSettingsDialog(
    initialEvents: Set<PartnerAlerts.Event>,
    initialTone: PartnerAlerts.Tone,
    initialDaysBefore: Int,
    partnerName: String,
    onDismiss: () -> Unit,
    onSave: (Set<PartnerAlerts.Event>, PartnerAlerts.Tone, Int) -> Unit
) {
    var events by remember { mutableStateOf(initialEvents) }
    var tone by remember { mutableStateOf(initialTone) }
    var daysBefore by remember { mutableIntStateOf(initialDaysBefore) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم روزهای حساس", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("در کدام روزها پیام برای همسر آماده شود؟", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                PartnerAlerts.Event.values().forEach { event ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            events = if (event in events) events - event else events + event
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = event in events, onCheckedChange = {
                            events = if (it) events + event else events - event
                        })
                        Text("${event.emoji} ${event.title}", fontSize = 13.sp)
                    }
                }
                if (PartnerAlerts.Event.PERIOD_SOON in events) {
                    Text("چند روز قبل از پریود: $daysBefore روز", fontSize = 12.sp)
                    Slider(
                        value = daysBefore.toFloat(),
                        onValueChange = { daysBefore = it.toInt() },
                        valueRange = PartnerAlerts.MIN_DAYS_BEFORE.toFloat()..PartnerAlerts.MAX_DAYS_BEFORE.toFloat(),
                        steps = PartnerAlerts.MAX_DAYS_BEFORE - PartnerAlerts.MIN_DAYS_BEFORE - 1
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("لحن پیام", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                PartnerAlerts.Tone.values().forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { tone = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = tone == option, onClick = { tone = option })
                        Text(option.title, fontSize = 13.sp)
                    }
                }
                Text(
                    "نمونه: «${PartnerAlerts.message(PartnerAlerts.Event.PMS_START, partnerName, tone, daysBefore)}»",
                    fontSize = 11.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "اعلان در ساعت یادآورهای برنامه (تنظیمات) نمایش داده می‌شود.",
                    fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(events, tone, daysBefore) }) { Text("ذخیره", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
