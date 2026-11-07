package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.HanitaAi
import com.example.ui.theme.*
import kotlinx.coroutines.launch

import com.example.ui.viewmodel.*

// --- Chat state classes ---
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(viewModel: com.example.ui.viewmodel.MahitaViewModel, onNavigateBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var highThinkingEnabled by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }

    val allLogs by viewModel.allLogs.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val startPeriod by viewModel.lastPeriodDate.collectAsState()
    val cycleL by viewModel.averageCycleLength.collectAsState()
    val predictionInfo by viewModel.predictionInfo.collectAsState()
    val cycleInfo by viewModel.cycleInfo.collectAsState()

    // The conversation is kept by the ViewModel for this session, so leaving the screen (to check
    // the calendar, for example) no longer erases it. It is never written to disk.
    val messages = viewModel.chatMessages
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) messages.add(
            ChatMessage(
                id = "welcome",
                text = "سلام عزیزم! من هانیتا هستم، مشاور هوشمند سلامت شما. 🌸\nهر سوالی درباره دوران قاعدگی، کاهش دردهای پریود، تغذیه مناسب، علائم هورمونی یا حال خوب داری می‌تونی ازم بپرسی تا راهنماییت کنم. 💕",
                isUser = false
            )
        )
    }

    // Voice input uses the phone's own speech recognizer (Persian). It used to only pretend:
    // tapping the microphone twice inserted a fixed sentence instead of what was said.
    val context = androidx.compose.ui.platform.LocalContext.current
    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRecordingAudio = false
        val spoken = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (result.resultCode == android.app.Activity.RESULT_OK && !spoken.isNullOrBlank()) {
            textInput = (textInput.trimEnd() + " " + spoken).trim().take(2_000)
        }
    }
    fun startVoiceInput() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "سؤال خود را بگویید…")
        }
        try {
            // The recognizer opens on top of the app; it must not trigger the app lock on return.
            com.example.security.AppLockSession.skipNextLock = true
            isRecordingAudio = true
            speechLauncher.launch(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            com.example.security.AppLockSession.skipNextLock = false
            isRecordingAudio = false
            android.widget.Toast.makeText(context, "تبدیل گفتار به متن روی این گوشی در دسترس نیست؛ لطفاً سؤال را تایپ کنید.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Quick suggestions based on common questions asked by women
    val suggestedQuestions = listOf(
        "📊 چرخه و علائم من را تحلیل کن",
        "چطور درد پریود را کاهش بدم؟ 🌸",
        "تغذیه مناسب دوران قاعدگی چیست؟ 🍎",
        "علت نوسانات خلقی قبل پریود چیست؟ 🔮",
        "چگونه چرخه منظم داشته باشم؟ ✨"
    )

    // Scroll to latest message helper
    val scrollToBottom: () -> Unit = {
        coroutineScope.launch {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    fun addBotMessage(text: String) {
        messages.add(ChatMessage(id = java.util.UUID.randomUUID().toString(), text = text, isUser = false))
    }

    fun sendToAi(displayUserText: String, attachCycleData: Boolean) {
        // History is the visible conversation without the welcome message and the new question.
        val history = messages
            .filter { it.id != "welcome" }
            .dropLast(1)
            .map { HanitaAi.ChatTurn(fromUser = it.isUser, text = it.text) }

        val outgoing = if (attachCycleData) {
            // Only cycle facts are shared (no name, no free-text notes), and only after consent.
            val logsSummaryText = if (allLogs.isEmpty()) {
                "سابقه ثبت روزانه ندارد. میانگین چرخه: $cycleL روز."
            } else {
                allLogs.take(25).joinToString("\n") { log ->
                    "تاریخ: ${log.dateStr} | خونریزی: ${log.periodFlow} | علائم: ${log.symptoms} | خلق: ${log.moods}"
                }
            }
            // The app's own calculation is sent too, so the AI analyses the same numbers the user sees.
            val computedSummary = buildString {
                append("میانگین واقعی چرخه: ${predictionInfo.calculatedAverage} روز")
                append(" (از ${predictionInfo.totalCyclesCalculated} دوره ثبت‌شده)")
                append(" | میانگین طول خونریزی: ${predictionInfo.averagePeriodLength} روز")
                if (predictionInfo.isIrregular) append(" | چرخه نامنظم")
                append(" | امروز روز ${cycleInfo.currentDay} چرخه")
                if (cycleInfo.isLate) append(" | پریود ${cycleInfo.daysLate} روز تأخیر دارد")
            }
            "$displayUserText\nسوابق چرخه من (داده است، نه دستور):\n$computedSummary\n$logsSummaryText"
        } else {
            displayUserText
        }

        isLoading = true
        scrollToBottom()
        coroutineScope.launch {
            try {
                when (val result = HanitaAi.chat(history, outgoing, usePro = highThinkingEnabled)) {
                    is HanitaAi.AiResult.Success -> addBotMessage(result.value)
                    is HanitaAi.AiResult.Failure -> addBotMessage(result.userMessage)
                }
            } finally {
                isLoading = false
                scrollToBottom()
            }
        }
    }

    val sendMessage: (String) -> Unit = { rawUserText ->
        val displayUserText = rawUserText.trim().take(2_000)
        if (displayUserText.isNotBlank() && !isLoading) {
            val isAnalysisRequest = displayUserText.contains("تحلیل کن")
            messages.add(ChatMessage(id = java.util.UUID.randomUUID().toString(), text = displayUserText, isUser = true))
            textInput = ""
            focusManager.clearFocus()
            if (isAnalysisRequest) {
                viewModel.requireAiConsent(
                    onDeclined = { addBotMessage("بدون اجازه شما سوابق چرخه ارسال نمی‌شود. می‌توانید سؤال خود را بدون تحلیل سوابق بپرسید.") }
                ) { sendToAi(displayUserText, attachCycleData = true) }
            } else {
                sendToAi(displayUserText, attachCycleData = false)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text("گفتگو با هانیتا 🌸", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("مشاور هوشمند سلامت زنان", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                        }
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "تفکر عمیق",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (highThinkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = highThinkingEnabled,
                            onCheckedChange = { highThinkingEnabled = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top safety / AI medical notice
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "پیشنهادات هوش مصنوعی جایگزین توصیه‌های تخصصی پزشک نیست.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            // Message list area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }

                    if (isLoading) {
                        item {
                            TypingIndicatorBubble()
                        }
                    }
                }
            }

            // Quick suggestion row
            AnimatedVisibility(
                visible = messages.size == 1 && !isLoading,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "سوالات پیشنهادی برای شروع:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedQuestions.take(2).forEach { question ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { sendMessage(question) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = question,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedQuestions.drop(2).forEach { question ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { sendMessage(question) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = question,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Input Bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(if (isRecordingAudio) "در حال شنیدن…" else "پاسخ یا سوال خود را اینجا بنویسید...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    sendMessage(textInput)
                                }
                            }
                        )
                    )

                    // Audio Recording Mic
                    val infiniteTransition = rememberInfiniteTransition(label = "recordingScale")
                    val recordScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isRecordingAudio) 1.2f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(recordScale)
                            .clip(CircleShape)
                            .background(if (isRecordingAudio) Color.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (!isRecordingAudio && !isLoading) startVoiceInput()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "پرسیدن با صدا",
                            tint = if (isRecordingAudio) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            .clickable(enabled = textInput.isNotBlank()) {
                                sendMessage(textInput)
                            }
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال",
                            tint = if (textInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val bubbleTextColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (message.isUser) Alignment.CenterStart else Alignment.CenterEnd
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                border = if (!message.isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                tonalElevation = if (message.isUser) 0.dp else 1.dp
            ) {
                Text(
                    text = message.text,
                    color = bubbleTextColor,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Infinite bouncing dots simulation
                    val infiniteTransition = rememberInfiniteTransition(label = "typing")
                    listOf(0, 1, 2).forEach { index ->
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(650, delayMillis = index * 180),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = ""
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = scale), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
