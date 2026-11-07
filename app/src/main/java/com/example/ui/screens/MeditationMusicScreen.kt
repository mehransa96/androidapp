package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MahitaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.annotation.RawRes
import com.example.R

data class MeditationTrack(
    val id: String,
    val title: String,
    val artist: String,
    val categoryId: String,
    val durationSec: Int,
    @RawRes val rawResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationMusicScreen(
    viewModel: MahitaViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val categories = listOf(
        "meditation" to "🧘 مدیتیشن عمیق",
        "nature" to "🌲 آوای طبیعت",
        "healing" to "✨ تُن‌های آرام‌بخش",
        "lofi" to "🎧 لوفای و آکوستیک"
    )

    val tracks = remember {
        listOf(
            // Meditation (guided by sound: breathing cues, slow tempo, soft timbres)
            MeditationTrack("m1", "تنفس ۴-۷-۸ با نویز صورتی (ضد اضطراب PMS)", "استودیو هانیتا • تنفس هدایت‌شده", "meditation", 76, R.raw.luteal_pink_noise_zen),
            MeditationTrack("m2", "رهاسازی عضلانی با پیانوی آرام (دردهای شکمی)", "استودیو هانیتا • پیانو ۶۰ BPM", "meditation", 64, R.raw.period_piano_calm),
            MeditationTrack("m3", "تجسم آرام با کالیمبا", "استودیو هانیتا • کالیمبا و پد", "meditation", 60, R.raw.follicular_positive_vibes),
            // Nature
            MeditationTrack("n1", "جنگل صبحگاهی، جویبار و پرندگان", "استودیو هانیتا • آوای طبیعت", "nature", 64, R.raw.nature_forest_birds),
            MeditationTrack("n2", "باران شبانه روی برگ‌ها", "استودیو هانیتا • آوای طبیعت", "nature", 64, R.raw.period_rain_nature),
            MeditationTrack("n3", "امواج آرام ساحل", "استودیو هانیتا • آوای طبیعت", "nature", 72, R.raw.nature_ocean_waves),
            // Calm tones
            MeditationTrack("h1", "تُن ۵۲۸ هرتز با ضربان دوگوش ۴ هرتز (با هدفون)", "استودیو هانیتا • تن‌آرامی", "healing", 64, R.raw.period_frequency_528),
            MeditationTrack("h2", "کوک ۴۳۲ هرتز با ضربان دوگوش ۶ هرتز (با هدفون)", "استودیو هانیتا • آرامش ذهن", "healing", 64, R.raw.luteal_frequency_432),
            MeditationTrack("h3", "پد امبینت خواب عمیق و استراحت", "استودیو هانیتا • امبینت", "healing", 64, R.raw.period_ambient_rest),
            // Lo-Fi & acoustic
            MeditationTrack("l1", "بیت‌های لوفای تسکین PMS", "استودیو هانیتا • لوفای جاز", "lofi", 66, R.raw.luteal_lofi_chill),
            MeditationTrack("l2", "آوای گرم ویولنسل و سازهای زهی", "استودیو هانیتا • آکوستیک", "lofi", 64, R.raw.luteal_warm_acoustic),
            MeditationTrack("l3", "گیتار صبحگاهی و آواز پرندگان", "استودیو هانیتا • آکوستیک", "lofi", 60, R.raw.follicular_morning_joy)
        )
    }

    var selectedCategoryId by remember { mutableStateOf("meditation") }
    val filteredTracks = remember(selectedCategoryId) {
        tracks.filter { it.categoryId == selectedCategoryId }
    }

    // Media Player States
    var currentTrack by remember { mutableStateOf<MeditationTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgressSec by remember { mutableStateOf(0) }
    var isLooping by remember { mutableStateOf(false) }

    // Interactive Breath Trainer states
    var breathPacingEnabled by remember { mutableStateOf(false) }
    var breathStateText by remember { mutableStateOf("آماده") }
    var breathPulseScale by remember { mutableStateOf(1.0f) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPlayer = remember { android.media.MediaPlayer() }
    // Pause (and show "paused") when a call or another app takes the sound.
    val playbackFocus = remember {
        com.example.utils.PlaybackFocus(context) {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            } catch (_: Exception) {
            }
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackFocus.abandon()
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch(e: Exception) {}
        }
    }

    fun playTrack(track: MeditationTrack) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(com.example.utils.PlaybackFocus.ATTRIBUTES)
            val afd = context.resources.openRawResourceFd(track.rawResId)
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer.isLooping = isLooping
            mediaPlayer.prepare()
            playbackFocus.request()
            mediaPlayer.start()
            isPlaying = true
            currentProgressSec = 0
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    LaunchedEffect(currentTrack) {
        currentTrack?.let { track ->
            playTrack(track)
        }
    }

    LaunchedEffect(isPlaying) {
        try {
            if (isPlaying) {
                if (!mediaPlayer.isPlaying && currentTrack != null) {
                    playbackFocus.request()
                    mediaPlayer.start()
                }
            } else {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
        } catch (e: Exception) {}
    }

    LaunchedEffect(isLooping) {
        try {
            mediaPlayer.isLooping = isLooping
        } catch (e: Exception) {}
    }

    // Breathing Trainer Engine
    LaunchedEffect(breathPacingEnabled) {
        if (breathPacingEnabled) {
            while (breathPacingEnabled) {
                // Inhale 4 seconds
                breathStateText = "دم عمیق (Inhale) 🌬️"
                animateTo(1.4f) { breathPulseScale = it }
                delay(4000)
                if (!breathPacingEnabled) break

                // Hold 4 seconds
                breathStateText = "حبس نفس (Hold) 🤫"
                delay(4000)
                if (!breathPacingEnabled) break

                // Exhale 4 seconds
                breathStateText = "بازدم آرام (Exhale) 🍃"
                animateTo(1.0f) { breathPulseScale = it }
                delay(4000)
                if (!breathPacingEnabled) break
            }
        } else {
            breathStateText = "شروع تمرین تنفس"
            breathPulseScale = 1.0f
        }
    }

    // Listen to track completion if not looping
    LaunchedEffect(isLooping, filteredTracks, currentTrack) {
        mediaPlayer.isLooping = isLooping
        mediaPlayer.setOnCompletionListener {
            if (!isLooping) {
                val active = currentTrack ?: return@setOnCompletionListener
                val currentIndex = filteredTracks.indexOfFirst { it.id == active.id }
                val next = when {
                    currentIndex != -1 && currentIndex < filteredTracks.lastIndex -> filteredTracks[currentIndex + 1]
                    filteredTracks.isNotEmpty() -> filteredTracks.first()
                    else -> null
                }
                if (next == null) {
                    isPlaying = false
                } else if (next.id == active.id) {
                    // Only one track in the list: setting the same track again does not restart it.
                    playTrack(next)
                } else {
                    currentTrack = next
                }
            }
        }
    }

    // Playback real-time position sync
    LaunchedEffect(isPlaying, currentTrack) {
        while (isPlaying && currentTrack != null) {
            delay(500)
            try {
                if (mediaPlayer.isPlaying) {
                    currentProgressSec = mediaPlayer.currentPosition / 1000
                }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("موزیک و مدیتیشن آرامش 🧖‍♀️", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                // Topic card
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "پرورش آرامش و ثبات ذهنی",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "انتخابی بی‌نظیر از بهترین نواسازهای دنیا برای دوره‌های کاهش خستگی، تسکین دردهای قاعدگی و شادابی روحیه زنانه شما.",
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Breathing interactive exercise widget
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Air,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        "تمرین هوشمند تنفس قاعدگی (۴-۴-۴)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = breathPacingEnabled,
                                    onCheckedChange = { breathPacingEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Breathing Visual Circle
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(breathPulseScale)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f), CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = breathStateText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Category selection LazyRow
                item {
                    Text(
                        "دسته بندی های موسیقی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (id, title) ->
                            val isSelected = selectedCategoryId == id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = id },
                                label = { Text(title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Audio music track list
                items(filteredTracks, key = { it.id }) { track ->
                    val isCurrent = currentTrack == track
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                if (isCurrent) {
                                    isPlaying = !isPlaying
                                } else {
                                    currentTrack = track
                                    currentProgressSec = 0
                                    isPlaying = true
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrent && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d", track.durationSec / 60, track.durationSec % 60),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Beautiful mini-player popping up at the bottom
            AnimatedVisibility(
                visible = currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                currentTrack?.let { track ->
                    // vinyl rotating animation
                    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(14000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = ""
                    )
                    val rotation = if (isPlaying) rotationAngle else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Album CD rotating visualizer
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .rotate(rotation)
                                        .background(
                                            Brush.sweepGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isLooping = !isLooping }) {
                                        Icon(
                                            imageVector = Icons.Filled.Repeat,
                                            tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            contentDescription = "تکرار"
                                        )
                                    }
                                    IconButton(onClick = {
                                        isPlaying = false
                                        currentTrack = null
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ProgressBar Selector
                            Slider(
                                value = currentProgressSec.toFloat().coerceIn(0f, track.durationSec.toFloat()),
                                onValueChange = { currentProgressSec = it.toInt() },
                                onValueChangeFinished = {
                                    try {
                                        mediaPlayer.seekTo(currentProgressSec * 1000)
                                    } catch (e: Exception) {}
                                },
                                valueRange = 0f..track.durationSec.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%02d:%02d", currentProgressSec / 60, currentProgressSec % 60),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%02d:%02d", track.durationSec / 60, track.durationSec % 60),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val index = filteredTracks.indexOf(track)
                                    if (index != -1 && index > 0) {
                                        currentTrack = filteredTracks[index - 1]
                                    } else {
                                        currentTrack = filteredTracks.lastOrNull()
                                    }
                                    currentProgressSec = 0
                                }) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "قبلی", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .bounceClick { isPlaying = !isPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "پخش",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(onClick = {
                                    val index = filteredTracks.indexOf(track)
                                    if (index != -1 && index < filteredTracks.lastIndex) {
                                        currentTrack = filteredTracks[index + 1]
                                    } else {
                                        currentTrack = filteredTracks.firstOrNull()
                                    }
                                    currentProgressSec = 0
                                }) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "بعدی", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

// Simple ease helper for breathing pacing visualization scale
suspend fun animateTo(target: Float, onUpdate: (Float) -> Unit) {
    val durationMs = 4000
    val startValue = if (target == 1.4f) 1.0f else 1.4f
    val steps = 40
    val stepDelay = (durationMs / steps).toLong()
    for (i in 1..steps) {
        val fraction = i.toFloat() / steps
        val value = startValue + (target - startValue) * fraction
        onUpdate(value)
        delay(stepDelay)
    }
}
