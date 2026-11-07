package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import com.example.data.model.PhysicalSymptomLog
import com.example.ui.components.ProfessionalDatePickerDialog
import com.example.ui.components.ProfessionalCycleEditDialog
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Edit
import com.example.ui.theme.*
import com.example.ui.viewmodel.MahitaViewModel
import com.example.utils.JalaliCalendar
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: MahitaViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Date management
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val selectedDateStr = selectedDate.toString()
    val jalaliSelected = remember(selectedDate) { JalaliCalendar.gregorianToJalali(selectedDate) }

    // Collect saved Room data for selected date
    // remember: a new Flow on every recomposition restarted the collection (and re-emitted empty).
    val savedSymptomsFlow = remember(selectedDateStr) { viewModel.getPhysicalSymptomsForDate(selectedDateStr) }
    val savedRoomSymptoms by savedSymptomsFlow.collectAsState(initial = emptyList())
    val allLogs by viewModel.allLogs.collectAsState()
    val existingDailyLog = remember(allLogs, selectedDateStr) { allLogs.find { it.dateStr == selectedDateStr } }

    // Form inputs
    val activeSymptoms = remember { mutableStateMapOf<String, String>() } // Symptom name -> Severity ("MILD", "MODERATE", "SEVERE")
    val symptomCategories = remember { mutableStateMapOf<String, String>() } // Symptom name -> Category
    var overallPainScore by remember { mutableStateOf(1f) }
    var selectedFlow by remember { mutableStateOf("NONE") }
    val moods = remember { mutableStateListOf<String>() }
    var notes by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("") }
    var waterIntake by remember { mutableIntStateOf(0) }
    var bbtInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    // Custom symptoms from preferences
    val customPhysical by viewModel.customPhysicalSymptoms.collectAsState()
    val customMood by viewModel.customMoodSymptoms.collectAsState()
    var showAddPhysicalDialog by remember { mutableStateOf(false) }
    var showAddMoodDialog by remember { mutableStateOf(false) }
    var showCycleEditDialogInLog by remember { mutableStateOf(false) }

    // Synchronize UI form when date or saved room data changes
    // The form is filled from the database itself once per date. It used to fill from flows whose
    // first value is an empty list, so the saved symptoms of the day never appeared and pressing
    // "save" erased them.
    LaunchedEffect(selectedDateStr) {
        val dbSymptoms = viewModel.getPhysicalSymptomsListForDate(selectedDateStr)
        val dbLog = viewModel.getLogForDate(selectedDateStr)
        run {
            activeSymptoms.clear()
            symptomCategories.clear()

            if (dbSymptoms.isNotEmpty()) {
                dbSymptoms.forEach { s ->
                    activeSymptoms[s.symptomName] = s.severity
                    symptomCategories[s.symptomName] = s.category
                }
            } else if (dbLog != null && dbLog.symptoms.isNotBlank()) {
                // Fallback to symptoms string from DailyLog if any
                dbLog.symptoms.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { sym ->
                    activeSymptoms[sym] = "MODERATE"
                    symptomCategories[sym] = "عمومی"
                }
            }

            // Sync flow, moods, notes, emoji
            selectedFlow = dbLog?.periodFlow ?: "NONE"
            notes = dbLog?.notes ?: ""
            selectedEmoji = dbLog?.emoji ?: ""
            waterIntake = dbLog?.waterIntake ?: 0
            bbtInput = dbLog?.bbt?.let { if (it > 0f) it.toString() else "" } ?: ""
            weightInput = dbLog?.weight?.let { if (it > 0f) it.toString() else "" } ?: ""
            moods.clear()
            dbLog?.moods?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.let {
                moods.addAll(it)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ثبت روزانه علائم جسمانی",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${jalaliSelected.formatLong()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Room Database Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Healing,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ثبت علائم جسمی",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (savedRoomSymptoms.isEmpty()) "هیچ علامتی برای این تاریخ ثبت نشده" else "${savedRoomSymptoms.size} علامت ذخیره شده",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Date Switcher
            DateSelectorRow(
                selectedDate = selectedDate,
                onSelectDate = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedDate = it
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Highlighted Core Symptoms: سردرد و خستگی
            Text(
                text = "علائم اصلی روزانه",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "برای تغییر شدت هر علامت، روی درجه خفیف، متوسط یا شدید ضربه بزنید.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Headache Card (سردرد)
            FeaturedSymptomCard(
                symptomName = "سردرد",
                iconEmoji = "🤕",
                subtitle = "سردردهای ضربان‌دار، میگرنی یا تنشی",
                category = "سر و اعصاب",
                isSelected = activeSymptoms.containsKey("سردرد"),
                severity = activeSymptoms["سردرد"] ?: "MODERATE",
                onToggle = { isSelected ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelected) {
                        activeSymptoms["سردرد"] = "MODERATE"
                        symptomCategories["سردرد"] = "سر و اعصاب"
                    } else {
                        activeSymptoms.remove("سردرد")
                        symptomCategories.remove("سردرد")
                    }
                },
                onSeverityChange = { newSev ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSymptoms["سردرد"] = newSev
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Fatigue Card (خستگی)
            FeaturedSymptomCard(
                symptomName = "خستگی",
                iconEmoji = "🥱",
                subtitle = "احساس بی‌حالی، کاهش شدید انرژی یا خواب‌آلودگی",
                category = "سر و اعصاب",
                isSelected = activeSymptoms.containsKey("خستگی"),
                severity = activeSymptoms["خستگی"] ?: "MODERATE",
                onToggle = { isSelected ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelected) {
                        activeSymptoms["خستگی"] = "MODERATE"
                        symptomCategories["خستگی"] = "سر و اعصاب"
                    } else {
                        activeSymptoms.remove("خستگی")
                        symptomCategories.remove("خستگی")
                    }
                },
                onSeverityChange = { newSev ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSymptoms["خستگی"] = newSev
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Categorized Physical Symptoms
            Text(
                text = "سایر علائم جسمانی بر اساس دسته‌بندی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Category 1: سر و اعصاب
            SymptomCategorySection(
                categoryTitle = "سر، اعصاب و تمرکز",
                categoryName = "سر و اعصاب",
                items = listOf("سرگیجه", "بی‌خوابی", "افت تمرکز", "تاری دید"),
                activeSymptoms = activeSymptoms,
                symptomCategories = symptomCategories,
                onToggle = { sym, cat, isSel ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSel) {
                        activeSymptoms[sym] = "MODERATE"
                        symptomCategories[sym] = cat
                    } else {
                        activeSymptoms.remove(sym)
                        symptomCategories.remove(sym)
                    }
                },
                onSeverityChange = { sym, sev ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSymptoms[sym] = sev
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category 2: لگن، شکم و عضلات
            SymptomCategorySection(
                categoryTitle = "شکم، لگن و عضلات",
                categoryName = "شکم و لگن",
                items = listOf("درد شکم", "کمردرد", "نفخ", "حساسیت سینه", "گرفتگی عضلات"),
                activeSymptoms = activeSymptoms,
                symptomCategories = symptomCategories,
                onToggle = { sym, cat, isSel ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSel) {
                        activeSymptoms[sym] = "MODERATE"
                        symptomCategories[sym] = cat
                    } else {
                        activeSymptoms.remove(sym)
                        symptomCategories.remove(sym)
                    }
                },
                onSeverityChange = { sym, sev ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSymptoms[sym] = sev
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category 3: گوارش و فیزیک عمومی
            val combinedGeneral = (listOf("حالت تهوع", "هوس غذایی", "بی‌اشتهایی", "جوش صورت", "گرگرفتگی") + customPhysical).distinct()
            SymptomCategorySection(
                categoryTitle = "گوارش، متابولیسم و پوست",
                categoryName = "گوارش و متابولیسم",
                items = combinedGeneral,
                activeSymptoms = activeSymptoms,
                symptomCategories = symptomCategories,
                onAddCustom = { showAddPhysicalDialog = true },
                onToggle = { sym, cat, isSel ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSel) {
                        activeSymptoms[sym] = "MODERATE"
                        symptomCategories[sym] = cat
                    } else {
                        activeSymptoms.remove(sym)
                        symptomCategories.remove(sym)
                    }
                },
                onSeverityChange = { sym, sev ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeSymptoms[sym] = sev
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Overall Discomfort Rating (1 to 10)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "میزان کلی درد و ناراحتی جسمانی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${overallPainScore.toInt()} از ۱۰",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = when {
                                overallPainScore <= 3 -> Color(0xFF10B981)
                                overallPainScore <= 6 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = overallPainScore,
                        onValueChange = {
                            overallPainScore = it
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("آرام و بی‌درد (۱)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("متوسط (۵)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("شدید (۱۰)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Blood Flow Section
            SectionTitle("شدت خونریزی")
            FlowSelector(selectedFlow) { selectedFlow = it }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showCycleEditDialogInLog = true },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ویرایش تاریخ و بازه کامل پریود برای این ماه 🩸", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mood & Mood Emojis
            SectionTitle("وضعیت روحی")
            val combinedMood = (listOf("آرامش", "اضطراب", "افسردگی", "استرس", "پرانرژی", "حساس و تحریک‌پذیر") + customMood).distinct()
            ChipGroup(
                items = combinedMood,
                selectedItems = moods,
                onAddClick = { showAddMoodDialog = true }
            ) { item, isSelected ->
                if (isSelected) moods.add(item) else moods.remove(item)
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("ایموجی احوال امروز")
            val moodEmojis = listOf("😊", "🥰", "😐", "🥱", "😔", "😭", "😡", "🤕")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodEmojis.forEach { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedEmoji = emoji
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Water Intake Tracker
            SectionTitle("مصرف آب (تعداد لیوان)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { if (waterIntake > 0) waterIntake-- },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Opacity, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(32.dp))
                    Text(
                        text = "$waterIntake لیوان",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                IconButton(
                    onClick = { waterIntake++ },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weight & BBT Tracker
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle("وزن (کیلوگرم)")
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("مثلاً 65.5", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle("دمای پایه بدن (°C)")
                    OutlinedTextField(
                        value = bbtInput,
                        onValueChange = { bbtInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("مثلاً 36.5", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notes Section
            SectionTitle("یادداشت روزانه و شرح حال")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                placeholder = {
                    Text(
                        "توضیحات بیشتر در مورد شدت سردرد، خستگی، داروهای مصرف شده یا احساس بدن...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save to Room Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Convert active symptoms map to PhysicalSymptomLog entities
                    val symptomsToSave = activeSymptoms.map { (symName, sev) ->
                        val cat = symptomCategories[symName] ?: "عمومی"
                        val score = when (sev) {
                            "SEVERE" -> 3
                            "MODERATE" -> 2
                            else -> 1
                        }
                        PhysicalSymptomLog(
                            dateStr = selectedDateStr,
                            symptomName = symName,
                            severity = sev,
                            severityScore = score,
                            category = cat,
                            notes = notes
                        )
                    }

                    // Save in Room database via ViewModel
                    viewModel.savePhysicalSymptomForm(
                        dateStr = selectedDateStr,
                        symptoms = symptomsToSave,
                        flow = selectedFlow,
                        moods = moods,
                        overallNotes = notes,
                        emoji = selectedEmoji,
                        waterIntake = waterIntake,
                        bbt = bbtInput.toFloatOrNull(),
                        weight = weightInput.toFloatOrNull(),
                        // The form shows every field of the day, so clearing a mood or note sticks.
                        replaceDayState = true,
                        measurementsFromForm = true
                    )

                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "علائم جسمانی با موفقیت ذخیره شد ✨"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ذخیره علائم",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Records in Room for Selected Date
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "علائم ثبت‌شده برای این تاریخ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${savedRoomSymptoms.size} مورد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (savedRoomSymptoms.isEmpty()) {
                        Text(
                            text = "هنوز علامتی برای این تاریخ در پایگاه داده ذخیره نشده است.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedRoomSymptoms.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.symptomName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SeverityBadge(severity = item.severity)
                                    }
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.deletePhysicalSymptomById(item.id, selectedDateStr)
                                            activeSymptoms.remove(item.symptomName)
                                            symptomCategories.remove(item.symptomName)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "حذف علامت",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dialogs for custom entries
        if (showAddPhysicalDialog) {
            CustomSymptomDialog(
                title = "افزودن علامت جسمی جدید",
                onDismiss = { showAddPhysicalDialog = false },
                onAdd = { newSymptom ->
                    viewModel.addCustomPhysicalSymptom(newSymptom)
                    activeSymptoms[newSymptom] = "MODERATE"
                    symptomCategories[newSymptom] = "علائم سفارشی"
                    showAddPhysicalDialog = false
                }
            )
        }

        if (showAddMoodDialog) {
            CustomSymptomDialog(
                title = "افزودن وضعیت روحی جدید",
                onDismiss = { showAddMoodDialog = false },
                onAdd = { newMood ->
                    viewModel.addCustomMoodSymptom(newMood)
                    moods.add(newMood)
                    showAddMoodDialog = false
                }
            )
        }

        if (showCycleEditDialogInLog) {
            ProfessionalCycleEditDialog(
                initialStartDate = selectedDate,
                initialEndDate = selectedDate.plusDays(4),
                title = "ویرایش و ثبت دوره قاعدگی 🩸",
                onDismissRequest = { showCycleEditDialogInLog = false },
                onSaveCycle = { newStart, newEnd, length, flow, notes ->
                    viewModel.updateFullCycleRecord(
                        oldRecordId = null,
                        oldStartDate = null,
                        oldEndDate = null,
                        newStartDate = newStart,
                        newEndDate = newEnd,
                        cycleLength = length,
                        flow = flow,
                        notes = notes
                    )
                    showCycleEditDialogInLog = false
                    selectedDate = newStart
                    selectedFlow = flow
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("دوره قاعدگی با موفقیت در دیتابیس ثبت و ذخیره گردید ✨")
                    }
                }
            )
        }
    }
}

@Composable
fun DateSelectorRow(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val twoDaysAgo = today.minusDays(2)
    var showDatePicker by remember { mutableStateOf(false) }

    val isOtherDate = selectedDate != today && selectedDate != yesterday && selectedDate != twoDaysAgo
    val otherLabel = if (isOtherDate) {
        val j = JalaliCalendar.gregorianToJalali(selectedDate)
        "${j.day} ${j.monthName}"
    } else {
        "تقویم 📅"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DateFilterChip(
            label = "امروز",
            isSelected = selectedDate == today,
            onClick = { onSelectDate(today) },
            modifier = Modifier.weight(1f)
        )
        DateFilterChip(
            label = "دیروز",
            isSelected = selectedDate == yesterday,
            onClick = { onSelectDate(yesterday) },
            modifier = Modifier.weight(1f)
        )
        DateFilterChip(
            label = "۲ روز قبل",
            isSelected = selectedDate == twoDaysAgo,
            onClick = { onSelectDate(twoDaysAgo) },
            modifier = Modifier.weight(1f)
        )
        DateFilterChip(
            label = otherLabel,
            isSelected = isOtherDate,
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1.15f)
        )
    }

    if (showDatePicker) {
        ProfessionalDatePickerDialog(
            initialDate = selectedDate,
            title = "انتخاب تاریخ ثبت علائم و پریود 🗓️",
            maxDate = today,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { picked ->
                onSelectDate(picked)
                showDatePicker = false
            }
        )
    }
}

@Composable
fun DateFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.height(42.dp)
    )
}

@Composable
fun FeaturedSymptomCard(
    symptomName: String,
    iconEmoji: String,
    subtitle: String,
    category: String,
    isSelected: Boolean,
    severity: String,
    onToggle: (Boolean) -> Unit,
    onSeverityChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = symptomName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle(it) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }

            AnimatedVisibility(visible = isSelected) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "شدت $symptomName:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SeveritySelectorChip(
                                label = "خفیف",
                                isSelected = severity == "MILD",
                                color = Color(0xFF10B981),
                                onClick = { onSeverityChange("MILD") }
                            )
                            SeveritySelectorChip(
                                label = "متوسط",
                                isSelected = severity == "MODERATE",
                                color = Color(0xFFF59E0B),
                                onClick = { onSeverityChange("MODERATE") }
                            )
                            SeveritySelectorChip(
                                label = "شدید",
                                isSelected = severity == "SEVERE",
                                color = Color(0xFFEF4444),
                                onClick = { onSeverityChange("SEVERE") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeveritySelectorChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) color else color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else color
        )
    }
}

@Composable
fun SeverityBadge(severity: String) {
    val (label, bg, fg) = when (severity) {
        "SEVERE" -> Triple("شدید", Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444))
        "MODERATE" -> Triple("متوسط", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFD97706))
        else -> Triple("خفیف", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomCategorySection(
    categoryTitle: String,
    categoryName: String,
    items: List<String>,
    activeSymptoms: Map<String, String>,
    symptomCategories: Map<String, String>,
    onAddCustom: (() -> Unit)? = null,
    onToggle: (String, String, Boolean) -> Unit,
    onSeverityChange: (String, String) -> Unit
) {
    Column {
        Text(
            text = categoryTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val isSelected = activeSymptoms.containsKey(item)
                val currentSev = activeSymptoms[item] ?: "MODERATE"

                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(item, categoryName, !isSelected) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item, fontSize = 13.sp)
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (currentSev) {
                                        "SEVERE" -> "• شدید"
                                        "MODERATE" -> "• متوسط"
                                        else -> "• خفیف"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(44.dp)
                )
            }
            if (onAddCustom != null) {
                FilterChip(
                    selected = false,
                    onClick = onAddCustom,
                    label = { Text("+ افزودن", fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(44.dp)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun FlowSelector(selected: String, onSelect: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val options: List<Pair<String, Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>> = listOf(
        "NONE" to Pair("هیچ", Icons.Filled.Opacity),
        "LIGHT" to Pair("کم", Icons.Filled.Opacity),
        "MEDIUM" to Pair("متوسط", Icons.Filled.Opacity),
        "HEAVY" to Pair("زیاد", Icons.Filled.Opacity)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { data ->
            val key = data.first
            val label = data.second.first
            val isSelected = selected == key
            FilterChip(
                selected = isSelected,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(key) 
                },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f).height(46.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    items: List<String>,
    selectedItems: List<String>,
    onAddClick: (() -> Unit)? = null,
    onSelectionChange: (String, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = selectedItems.contains(item)
            FilterChip(
                selected = isSelected,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectionChange(item, !isSelected) 
                },
                label = { Text(item, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(44.dp)
            )
        }
        if (onAddClick != null) {
            FilterChip(
                selected = false,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddClick() 
                },
                label = { Text("+ افزودن", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.height(44.dp)
            )
        }
    }
}

@Composable
fun CustomSymptomDialog(
    title: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("نام علامت") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

