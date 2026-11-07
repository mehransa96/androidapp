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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: MahitaViewModel,
    onNavigateToHistory: () -> Unit
) {
    if (!show) return

    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsState()
    val daysBefore by viewModel.daysBefore.collectAsState()
    val averageCycleLength by viewModel.averageCycleLength.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userWeight by viewModel.userWeight.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()
    val currentFont by viewModel.appFont.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val lastPeriodDate by viewModel.lastPeriodDate.collectAsState()

    var editWeight by remember { mutableStateOf(userWeight) }
    LaunchedEffect(userWeight) {
        editWeight = userWeight
    }

    var editStartDate by remember { mutableStateOf(lastPeriodDate) }
    var editDuration by remember { mutableStateOf(averageCycleLength.toString()) }
    var showStartDatePickerInSettings by remember { mutableStateOf(false) }

    LaunchedEffect(lastPeriodDate) {
        if (editStartDate.isBlank() && lastPeriodDate.isNotBlank()) {
            editStartDate = lastPeriodDate
        }
    }
    LaunchedEffect(averageCycleLength) {
        editDuration = averageCycleLength.toString()
    }

    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text("تایید", color = SleekPrimary)
            }
        },
        title = {
            Text(
                "تنظیمات",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = SleekTextDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Light / Dark Mode Toggle
                Text(
                    "حالت روشنایی (Theme Mode)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple("LIGHT", "روشن ☀️", Icons.Filled.LightMode),
                        Triple("DARK", "تاریک 🌙", Icons.Filled.DarkMode),
                        Triple("SYSTEM", "سیستم 📱", Icons.Filled.BrightnessAuto)
                    )
                    modes.forEach { (modeCode, modeTitle, modeIcon) ->
                        val isSelected = currentThemeMode == modeCode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SleekPrimary.copy(alpha = 0.18f) else SleekBorder.copy(alpha = 0.25f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) SleekPrimary else SleekBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setThemeMode(modeCode) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = modeTitle,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) SleekPrimary else SleekTextMedium
                                )
                                Text(
                                    text = modeTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SleekPrimary else SleekTextMedium
                                )
                            }
                        }
                    }
                }

                // Section 2: Dynamic Theme Selector
                Text(
                    "پوسته و هماهنگی تم‌ها (شیک و مینیمال)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekPrimary
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple("COSMIC_BLOSSOM", "شکوفه کیهانی ✨", listOf(Color(0xFFE879A0), Color(0xFFB59DFF))),
                        Triple("SWEET_CANDY", "آبنبات شیرین ☀️", listOf(Color(0xFFEC4899), Color(0xFF14B8A6))),
                        Triple("MINT_ORCHID", "ارکیده نعنایی 🌿", listOf(Color(0xFF2DD4BF), Color(0xFFF472B6))),
                        Triple("SOFT_LAVENDER", "اسطوخودوس 🔮", listOf(Color(0xFFA78BFA), Color(0xFF2DD4BF))),
                        Triple("ROSE_GOLD", "رز گلد لوکس 👑", listOf(Color(0xFFBE185D), Color(0xFFD97706))),
                        Triple("EMERALD_ZEN", "زمرد ذن 🍃", listOf(Color(0xFF059669), Color(0xFF10B981))),
                        Triple("MIDNIGHT_AMETHYST", "یاقوت شب 🌙", listOf(Color(0xFFC084FC), Color(0xFFF43F5E))),
                        Triple("SUNSET_PEACH", "هلو و غروب 🌅", listOf(Color(0xFFEA580C), Color(0xFFF59E0B)))
                    )
                    
                    themes.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (code, title, colors) ->
                                val isSelected = currentTheme == code
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SleekPrimary.copy(alpha = 0.15f) else SleekBorder.copy(alpha = 0.3f))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SleekPrimary else SleekBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.setAppTheme(code) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            colors.forEach { swatchColor ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(swatchColor)
                                                )
                                            }
                                        }
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) SleekPrimary else SleekTextMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SleekBorder)

                // Section 1.5: Dynamic Font Selector
                Text(
                    "انتخاب قلم و فونت برنامه (جذاب و فانتزی) 🌸",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekPrimary
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fontOptions = listOf(
                        "CLASSIC" to "شکوفه کلاسیک (خوانا) 🌸",
                        "CURSIVE" to "دست‌نویس جذاب و فانتزی ✨",
                        "SERIF" to "پرنسس لوکس و باکلاس 👑",
                        "SANS_SERIF" to "مینیمال مدرن و ظریف 🌿"
                    )
                    
                    fontOptions.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (code, title) ->
                                val isSelected = currentFont == code
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SleekPrimary.copy(alpha = 0.15f) else SleekBorder.copy(alpha = 0.3f))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SleekPrimary else SleekBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.setAppFont(code) }
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SleekPrimary else SleekTextMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SleekBorder)

                // Section 2: Push Notifications
                Text(
                    "یادآوری چرخه قاعدگی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("فعال‌سازی اعلان یادآور شروع", fontSize = 13.sp, color = SleekTextMedium)
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { viewModel.setNotificationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SleekPrimary,
                            uncheckedThumbColor = SleekTextLight,
                            uncheckedTrackColor = SleekBorder
                        )
                    )
                }

                HorizontalDivider(color = SleekBorder)

                // Profile Component Card for Menstrual Cycle & Personal Info Sync
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تنظیمات اطلاعات چرخه قاعدگی هانیتا",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SleekPrimary
                            )
                            Text(
                                text = "کاربر: $userName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextMedium
                            )
                        }

                        // 1. Weight Form Field
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text("وزن فعلی شما (kg)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorder
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    // Accept Persian/Arabic digits and a decimal comma; keep only realistic values.
                                    val normalized = com.example.security.AppLockManager.normalizeDecimal(editWeight)
                                    val kg = normalized.toFloatOrNull()
                                    if (kg == null || kg !in 25f..250f) {
                                        android.widget.Toast.makeText(context, "لطفاً وزن معتبر بین ۲۵ تا ۲۵۰ کیلوگرم وارد کنید.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.setWeight(normalized)
                                    editWeight = normalized
                                    android.widget.Toast.makeText(context, "وزن شما روی همین گوشی ذخیره شد.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(45.dp)
                            ) {
                                Text("ذخیره وزن", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        HorizontalDivider(color = SleekBorder.copy(alpha = 0.4f))

                        // 2. Menstrual Cycle Form Fields
                        Text(
                            text = "فرم اطلاعات شروع و طول چرخه (سیستم هوشمند)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = SleekTextMedium
                        )

                        // Visual Jalali Date Trigger Card with Calendar Dialog
                        val currentJalaliDisplay = remember(editStartDate) {
                            runCatching {
                                JalaliCalendar.gregorianToJalali(java.time.LocalDate.parse(editStartDate)).formatLong()
                            }.getOrDefault(if (editStartDate.isNotBlank()) editStartDate else "انتخاب نشده")
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showStartDatePickerInSettings = true },
                            colors = CardDefaults.cardColors(containerColor = SleekPrimary.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "انتخاب تاریخ",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "تاریخ شروع آخرین قاعدگی:",
                                            fontSize = 10.5.sp,
                                            color = SleekTextMedium
                                        )
                                        Text(
                                            text = currentJalaliDisplay,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekPrimaryDark
                                        )
                                    }
                                }
                                Surface(
                                    color = SleekPrimary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "تغییر تاریخ 📅",
                                        fontSize = 10.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (showStartDatePickerInSettings) {
                            val initialParsed = runCatching { java.time.LocalDate.parse(editStartDate) }.getOrNull() ?: java.time.LocalDate.now()
                            ProfessionalDatePickerDialog(
                                initialDate = initialParsed,
                                title = "انتخاب تاریخ شروع آخرین پریود 🩸",
                                maxDate = java.time.LocalDate.now(),
                                onDismissRequest = { showStartDatePickerInSettings = false },
                                onDateSelected = { pickedDate ->
                                    editStartDate = pickedDate.toString()
                                    showStartDatePickerInSettings = false
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editDuration,
                                onValueChange = { editDuration = it },
                                label = { Text("طول کل چرخه (روز)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = SleekBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "طول دوره",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            Button(
                                onClick = {
                                    val durationInt = editDuration.toIntOrNull()
                                    if (durationInt == null || durationInt !in 21..45) {
                                        android.widget.Toast.makeText(context, "لطفاً طول چرخه معتبر بین ۲۱ تا ۴۵ روز وارد کنید.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (editStartDate.isNotBlank()) {
                                        val parsedStart = try {
                                            java.time.LocalDate.parse(editStartDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "فرمت تاریخ شروع نامعتبر است (باید YYYY-MM-DD باشد)", android.widget.Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        // Same rule as the calendar: a period cannot start in the future.
                                        if (parsedStart.isAfter(java.time.LocalDate.now())) {
                                            android.widget.Toast.makeText(context, "شروع پریود را نمی‌توان برای روزهای آینده ثبت کرد", android.widget.Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                    viewModel.updateCycleSettings(editStartDate, durationInt)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    android.widget.Toast.makeText(context, "اطلاعات چرخه روی همین گوشی ذخیره شد ✨", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(45.dp).weight(1.3f)
                            ) {
                                Text("ذخیره اطلاعات چرخه", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        
                        Button(
                            onClick = { onNavigateToHistory() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(45.dp)
                        ) {
                            Text("مشاهده تاریخچه چرخه‌ها", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }

                if (isNotificationEnabled) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("زمان یادآوری:", fontSize = 12.sp, color = SleekTextLight)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 1, 2, 3).forEach { day ->
                                val selected = daysBefore == day
                                val label = when (day) {
                                    0 -> "همان روز"
                                    1 -> "۱ روز قبل"
                                    2 -> "۲ روز قبل"
                                    3 -> "۳ روز قبل"
                                    else -> "$day روز قبل"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) SleekPrimary else SleekPrimaryLight)
                                        .clickable { viewModel.setDaysBefore(day) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else SleekPrimaryDark
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SleekBorder)

                // Section 2: where the data lives
                Text(
                    "حساب کاربری و ذخیره‌سازی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekPrimary
                )
                Text(
                    "حساب شما فقط روی همین گوشی است و همه اطلاعات در حافظه خصوصی برنامه ذخیره می‌شود؛ جز درخواست‌هایی که خودتان به دستیار هوشمند می‌فرستید، هیچ داده‌ای از گوشی خارج نمی‌شود. هر روز یک نسخه رمزنگاری‌شده خودکار هم نگه داشته می‌شود. برای انتقال به گوشی جدید یا نگهداری نسخه بیرونی، از بخش «حریم خصوصی و پشتیبان» فایل پشتیبان رمزدار بگیرید.",
                    fontSize = 11.sp,
                    color = SleekTextMedium,
                    lineHeight = 16.sp
                )
                var editName by remember(userName) { mutableStateOf(userName) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.take(com.example.data.preferences.UserPreferencesManager.MAX_NAME_LENGTH) },
                        label = { Text("نام شما", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    val nameContext = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            if (com.example.data.preferences.UserPreferencesManager.sanitizeName(editName).isBlank()) {
                                android.widget.Toast.makeText(nameContext, "نام نمی‌تواند خالی باشد.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.setUserName(editName)
                                android.widget.Toast.makeText(nameContext, "نام شما ذخیره شد.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(45.dp)
                    ) {
                        Text("ذخیره نام", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                HorizontalDivider(color = SleekBorder)
                val aiConsent by viewModel.aiConsentGiven.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("اجازه ارسال سوابق برای تحلیل هوشمند", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekPrimary)
                        Text(
                            "در صورت لغو، هیچ سابقه‌ای برای هوش مصنوعی ارسال نمی‌شود.",
                            fontSize = 11.sp,
                            color = SleekTextMedium
                        )
                    }
                    Switch(
                        checked = aiConsent,
                        onCheckedChange = { checked ->
                            if (checked) viewModel.requireAiConsent { } else viewModel.revokeAiConsent()
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
