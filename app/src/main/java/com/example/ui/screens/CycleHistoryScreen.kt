package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CycleRecord
import com.example.ui.components.ProfessionalCycleEditDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MahitaViewModel
import com.example.utils.JalaliCalendar
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleHistoryScreen(
    viewModel: MahitaViewModel,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val records by viewModel.allCycleRecords.collectAsState()
    val avgCycleLength by viewModel.averageCycleLength.collectAsState()

    // Dialog state for editing an existing record or creating a new one
    var recordToEdit by remember { mutableStateOf<CycleRecord?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<CycleRecord?>(null) }

    // Sort records descending by start date
    val sortedRecords = remember(records) {
        records.sortedByDescending { it.startDate }
    }

    // Calculate metrics
    val totalRecords = sortedRecords.size
    val averagePeriodDays = remember(sortedRecords) {
        if (sortedRecords.isEmpty()) 5 else {
            val totalDays = sortedRecords.sumOf { record ->
                val start = runCatching { LocalDate.parse(record.startDate) }.getOrNull()
                val end = runCatching { LocalDate.parse(record.endDate) }.getOrNull() ?: start
                if (start != null && end != null) {
                    (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
                } else 5
            }
            (totalDays / sortedRecords.size).coerceAtLeast(1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تاریخچه و ویرایش دوره‌ها",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "مدیریت و اصلاح اطلاعات دوره‌های قاعدگی گذشته",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCreateDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت دوره جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                text = { Text("ثبت دوره جدید 🩸", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("add_cycle_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Summary Analytics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text("شاخص‌های میانگین چرخه‌های شما", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "$totalRecords دوره ثبت‌شده",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("میانگین طول چرخه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$avgCycleLength روز", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("میانگین خونریزی", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$averagePeriodDays روز", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("وضعیت نظم چرخه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (totalRecords >= 2) "منظم و فعال ✨" else "در حال یادگیری", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sortedRecords.isEmpty()) {
                // Empty state card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "هنوز هیچ دوره قاعدگی ثبت نشده است",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "برای دریافت پیش‌بینی‌های دقیق و توصیه‌های هوشمند، تاریخ شروع و پایان آخرین قاعدگی خود را وارد نمایید.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت اولین دوره قاعدگی", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "لیست تمام دوره‌های ثبت‌شده (قابل ویرایش):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    itemsIndexed(sortedRecords, key = { _, item -> item.id }) { index, record ->
                        // Calculate interval to next recorded cycle
                        val nextCycleInterval = if (index > 0) {
                            val nextStart = runCatching { LocalDate.parse(sortedRecords[index - 1].startDate) }.getOrNull()
                            val thisStart = runCatching { LocalDate.parse(record.startDate) }.getOrNull()
                            if (nextStart != null && thisStart != null) {
                                ChronoUnit.DAYS.between(thisStart, nextStart).toInt()
                            } else null
                        } else null

                        DetailedCycleRecordCard(
                            record = record,
                            cycleNumber = sortedRecords.size - index,
                            cycleIntervalDays = nextCycleInterval,
                            onEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                recordToEdit = record
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // One tap used to delete the cycle and its bleeding days at once.
                                recordToDelete = record
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Edit Dialog for existing cycle
    if (recordToEdit != null) {
        val curr = recordToEdit!!
        val parsedStart = runCatching { LocalDate.parse(curr.startDate) }.getOrNull() ?: LocalDate.now()
        val parsedEnd = runCatching { LocalDate.parse(curr.endDate) }.getOrNull() ?: parsedStart.plusDays(4)

        ProfessionalCycleEditDialog(
            initialStartDate = parsedStart,
            initialEndDate = parsedEnd,
            initialCycleLength = avgCycleLength,
            initialNotes = curr.notes,
            isExistingCycle = true,
            title = "ویرایش دوره قاعدگی 🩸✏️",
            onDismissRequest = { recordToEdit = null },
            onSaveCycle = { newStart, newEnd, length, flow, notes ->
                viewModel.updateFullCycleRecord(
                    oldRecordId = curr.id,
                    oldStartDate = parsedStart,
                    oldEndDate = parsedEnd,
                    newStartDate = newStart,
                    newEndDate = newEnd,
                    cycleLength = length,
                    flow = flow,
                    notes = notes
                )
                recordToEdit = null
            },
            onDeleteCycle = {
                viewModel.deleteFullCycleRecord(curr)
                recordToEdit = null
            }
        )
    }

    recordToDelete?.let { record ->
        val startJ = runCatching {
            com.example.utils.JalaliCalendar.gregorianToJalali(LocalDate.parse(record.startDate)).formatLong()
        }.getOrDefault(record.startDate)
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("حذف این دوره؟", fontWeight = FontWeight.Bold) },
            text = { Text("دوره‌ای که در $startJ شروع شده و روزهای پریود آن حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFullCycleRecord(record)
                    recordToDelete = null
                }) { Text("حذف شود", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { recordToDelete = null }) { Text("انصراف") } }
        )
    }

    // Modal Dialog to Create a New / Past Cycle
    if (showCreateDialog) {
        ProfessionalCycleEditDialog(
            initialStartDate = LocalDate.now().minusDays(5),
            initialEndDate = LocalDate.now(),
            initialCycleLength = avgCycleLength,
            isExistingCycle = false,
            title = "ثبت دوره قاعدگی جدید 🩸",
            onDismissRequest = { showCreateDialog = false },
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
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun DetailedCycleRecordCard(
    record: CycleRecord,
    cycleNumber: Int,
    cycleIntervalDays: Int?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val startLocal = runCatching { LocalDate.parse(record.startDate) }.getOrNull()
    val endLocal = runCatching { LocalDate.parse(record.endDate) }.getOrNull() ?: startLocal

    val startJalali = remember(startLocal) {
        startLocal?.let { JalaliCalendar.gregorianToJalali(it) }
    }
    val endJalali = remember(endLocal) {
        endLocal?.let { JalaliCalendar.gregorianToJalali(it) }
    }

    val durationDays = remember(startLocal, endLocal) {
        if (startLocal != null && endLocal != null) {
            (ChronoUnit.DAYS.between(startLocal, endLocal).toInt() + 1).coerceAtLeast(1)
        } else 5
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Sequence Number, Interval Badge, Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = SleekPrimaryLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "دوره شماره $cycleNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimaryDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (cycleIntervalDays != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "طول چرخه: $cycleIntervalDays روز",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش دوره",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف دوره",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Dates Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (startJalali != null && endJalali != null) {
                            "از ${startJalali.formatLong()} تا ${endJalali.formatLong()}"
                        } else {
                            "شروع: ${record.startDate}"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "معادل میلادی: ${record.startDate} الی ${record.endDate}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = SleekPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$durationDays روز خونریزی 🩸",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (record.notes.isNotBlank() && record.notes != "ثبت شده از طریق تقویم تعاملی هانیتا") {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(
                            text = record.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
