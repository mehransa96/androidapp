package com.example.ui.screens

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CycleMusic
import com.example.data.model.CyclePhaseType
import com.example.data.repository.CycleMusicRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.MahitaViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    viewModel: MahitaViewModel,
    onNavigateBack: () -> Unit
) {
    val cycleInfo by viewModel.cycleInfo.collectAsState()

    // Determine current user cycle phase based on cycle tracking
    // Works for any cycle length (the old fixed day ranges assumed a 28-day cycle).
    val currentCyclePhase = when {
        cycleInfo.isPeriod -> CyclePhaseType.PERIOD
        cycleInfo.isFertile -> CyclePhaseType.OVULATION
        else -> when (cycleInfo.phase) {
            com.example.domain.CyclePredictor.Phase.MENSTRUAL -> CyclePhaseType.PERIOD
            com.example.domain.CyclePredictor.Phase.FOLLICULAR -> CyclePhaseType.FOLLICULAR
            com.example.domain.CyclePredictor.Phase.OVULATION -> CyclePhaseType.OVULATION
            com.example.domain.CyclePredictor.Phase.LUTEAL -> CyclePhaseType.LUTEAL
        }
    }

    // Phase filter selection: null means "All phases", otherwise specific phase
    var selectedPhaseFilter by remember { mutableStateOf<CyclePhaseType?>(currentCyclePhase) }

    val displayedTracks = remember(selectedPhaseFilter) {
        if (selectedPhaseFilter == null) {
            CycleMusicRepository.allTracks
        } else {
            CycleMusicRepository.getTracksForPhase(selectedPhaseFilter!!)
        }
    }

    // Media Player and interactive playback states
    var currentPlayingMusic by remember { mutableStateOf<CycleMusic?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionSeconds by remember { mutableStateOf(0) }
    var trackDurationSeconds by remember { mutableStateOf(24) }
    var isLooping by remember { mutableStateOf(true) }
    var isFavorited by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Playback logic with bundled offline raw audio resources
    fun startTrackPlayback(track: CycleMusic) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(com.example.utils.PlaybackFocus.ATTRIBUTES)
            val afd: AssetFileDescriptor = context.resources.openRawResourceFd(track.resId)
            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer.isLooping = isLooping
            mediaPlayer.prepare()
            playbackFocus.request()
            mediaPlayer.start()
            isPlaying = true
            currentPositionSeconds = 0
            trackDurationSeconds = (mediaPlayer.duration / 1000).coerceAtLeast(track.durationSeconds)
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    val togglePlayPause: (CycleMusic) -> Unit = { music ->
        if (currentPlayingMusic?.id == music.id) {
            if (isPlaying) {
                try {
                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                } catch (e: Exception) {}
                isPlaying = false
            } else {
                try {
                    playbackFocus.request()
                    mediaPlayer.start()
                    isPlaying = true
                } catch (e: Exception) {
                    startTrackPlayback(music)
                }
            }
        } else {
            currentPlayingMusic = music
            startTrackPlayback(music)
        }
    }

    // Listen to track completion if not looping
    LaunchedEffect(isLooping, displayedTracks, currentPlayingMusic) {
        mediaPlayer.isLooping = isLooping
        mediaPlayer.setOnCompletionListener {
            if (!isLooping) {
                val currentIndex = displayedTracks.indexOfFirst { it.id == currentPlayingMusic?.id }
                if (currentIndex != -1 && currentIndex < displayedTracks.lastIndex) {
                    val nextTrack = displayedTracks[currentIndex + 1]
                    currentPlayingMusic = nextTrack
                    startTrackPlayback(nextTrack)
                } else if (displayedTracks.isNotEmpty()) {
                    val firstTrack = displayedTracks.first()
                    currentPlayingMusic = firstTrack
                    startTrackPlayback(firstTrack)
                }
            }
        }
    }

    // Real-time position polling ticker
    LaunchedEffect(isPlaying, currentPlayingMusic) {
        while (isPlaying && currentPlayingMusic != null) {
            delay(500)
            try {
                if (mediaPlayer.isPlaying) {
                    currentPositionSeconds = mediaPlayer.currentPosition / 1000
                    val dur = mediaPlayer.duration / 1000
                    if (dur > 0) trackDurationSeconds = dur
                }
            } catch (e: Exception) {
                currentPositionSeconds += 1
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("موزیک و حال خوب چرخه 🌸", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "آهنگ‌های علمی متناسب با هر دوره قاعدگی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("mood_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Welcome / Current Phase Banner
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(currentCyclePhase.emoji, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "فاز کنونی شما: ${currentCyclePhase.persianTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentCyclePhase.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Phase selection filter tabs
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "انتخاب دوره و سبک موسیقی:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal scrollable phase chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Current Phase recommendation quick chip
                            FilterChip(
                                selected = selectedPhaseFilter == currentCyclePhase,
                                onClick = { selectedPhaseFilter = currentCyclePhase },
                                label = { Text("دوره من (${currentCyclePhase.emoji})", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (selectedPhaseFilter == currentCyclePhase) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.testTag("filter_my_phase")
                            )

                            CyclePhaseType.values().forEach { phase ->
                                if (phase != currentCyclePhase) {
                                    FilterChip(
                                        selected = selectedPhaseFilter == phase,
                                        onClick = { selectedPhaseFilter = phase },
                                        label = { Text("${phase.emoji} ${phase.persianTitle}", fontSize = 12.sp) },
                                        leadingIcon = {
                                            if (selectedPhaseFilter == phase) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        modifier = Modifier.testTag("filter_phase_${phase.key}")
                                    )
                                }
                            }

                            FilterChip(
                                selected = selectedPhaseFilter == null,
                                onClick = { selectedPhaseFilter = null },
                                label = { Text("همه ۱۶ قطعه 🎶", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (selectedPhaseFilter == null) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.testTag("filter_all_tracks")
                            )
                        }
                    }
                }

                // Phase guide banner if a specific phase is chosen
                if (selectedPhaseFilter != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedPhaseFilter!!.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = selectedPhaseFilter!!.shortSubtitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "۴ قطعه اختصاصی، تست‌شده و با بالاترین کیفیت صوتی",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Track list
                items(displayedTracks, key = { it.id }) { track ->
                    val isThisPlaying = currentPlayingMusic?.id == track.id
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isThisPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isThisPlaying) 1.5.dp else 1.dp,
                            color = if (isThisPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        elevation = CardDefaults.cardElevation(if (isThisPlaying) 4.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { togglePlayPause(track) }
                            .testTag("music_track_${track.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play / Pause circular button
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = if (isThisPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "پخش ${track.title}",
                                        tint = if (isThisPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = track.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.phase.emoji,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = track.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Dynamic soundwave animation when playing
                                if (isThisPlaying && isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
                                            val h1 by infiniteTransition.animateFloat(
                                                initialValue = 4f, targetValue = 14f,
                                                animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                                label = "h1"
                                            )
                                            val h2 by infiniteTransition.animateFloat(
                                                initialValue = 14f, targetValue = 5f,
                                                animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                                label = "h2"
                                            )
                                            val h3 by infiniteTransition.animateFloat(
                                                initialValue = 6f, targetValue = 16f,
                                                animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                                label = "h3"
                                            )
                                            Box(modifier = Modifier.size(3.dp, h1.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                            Box(modifier = Modifier.size(3.dp, h2.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                            Box(modifier = Modifier.size(3.dp, h3.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                        }
                                    }
                                } else {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%02d:%02d", track.durationSeconds / 60, track.durationSeconds % 60),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Benefit description and tags
                            Text(
                                text = track.benefitDescription,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(track.categoryBadge, fontSize = 10.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(track.frequencyLabel, fontSize = 10.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                        labelColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(150.dp)) // Cushion for bottom player
                }
            }

            // Interactive Bottom Music Player
            AnimatedVisibility(
                visible = currentPlayingMusic != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                currentPlayingMusic?.let { music ->
                    // Rotating vinyl animation
                    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rot")
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(10000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "disk"
                    )
                    val currentRotation = if (isPlaying) rotationAngle else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .testTag("mood_bottom_player"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
                        ),
                        elevation = CardDefaults.cardElevation(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Minimal Drag Handle
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rotating Vinyl Graphic
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .rotate(currentRotation)
                                        .background(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                    MaterialTheme.colorScheme.tertiary,
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = music.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(music.phase.emoji, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${music.subtitle} • ${music.categoryBadge}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Favorite Toggle
                                IconButton(
                                    onClick = { isFavorited = !isFavorited },
                                    modifier = Modifier.testTag("player_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        tint = if (isFavorited) Color(0xFFFF4C4C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        contentDescription = "علاقه‌مندی"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Playback Timeline Slider with Seeking Support
                            val formattedCurrent = String.format(java.util.Locale.US, "%02d:%02d", currentPositionSeconds / 60, currentPositionSeconds % 60)
                            val formattedTotal = String.format(java.util.Locale.US, "%02d:%02d", trackDurationSeconds / 60, trackDurationSeconds % 60)

                            Slider(
                                value = currentPositionSeconds.toFloat().coerceIn(0f, trackDurationSeconds.toFloat()),
                                onValueChange = { newValue ->
                                    currentPositionSeconds = newValue.toInt()
                                },
                                onValueChangeFinished = {
                                    try {
                                        mediaPlayer.seekTo(currentPositionSeconds * 1000)
                                    } catch (e: Exception) {}
                                },
                                valueRange = 0f..trackDurationSeconds.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .testTag("player_timeline_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formattedCurrent,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formattedTotal,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Playback Control Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Repeat / Loop Toggle
                                IconButton(
                                    onClick = {
                                        isLooping = !isLooping
                                        try {
                                            mediaPlayer.isLooping = isLooping
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier.testTag("player_loop_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Repeat,
                                        contentDescription = if (isLooping) "تکرار فعال" else "پخش پیوسته",
                                        tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }

                                // Skip Previous
                                IconButton(
                                    onClick = {
                                        val currentIndex = displayedTracks.indexOfFirst { it.id == music.id }
                                        if (currentIndex > 0) {
                                            val prev = displayedTracks[currentIndex - 1]
                                            currentPlayingMusic = prev
                                            startTrackPlayback(prev)
                                        } else if (displayedTracks.isNotEmpty()) {
                                            val last = displayedTracks.last()
                                            currentPlayingMusic = last
                                            startTrackPlayback(last)
                                        }
                                    },
                                    modifier = Modifier.testTag("player_skip_prev")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "قبلی",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Primary Play / Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            if (isPlaying) {
                                                try {
                                                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                                                } catch (e: Exception) {}
                                                isPlaying = false
                                            } else {
                                                try {
                                                    mediaPlayer.start()
                                                    isPlaying = true
                                                } catch (e: Exception) {
                                                    startTrackPlayback(music)
                                                }
                                            }
                                        }
                                        .testTag("player_play_pause"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "توقف" else "پخش",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Skip Next
                                IconButton(
                                    onClick = {
                                        val currentIndex = displayedTracks.indexOfFirst { it.id == music.id }
                                        if (currentIndex != -1 && currentIndex < displayedTracks.lastIndex) {
                                            val next = displayedTracks[currentIndex + 1]
                                            currentPlayingMusic = next
                                            startTrackPlayback(next)
                                        } else if (displayedTracks.isNotEmpty()) {
                                            val first = displayedTracks.first()
                                            currentPlayingMusic = first
                                            startTrackPlayback(first)
                                        }
                                    },
                                    modifier = Modifier.testTag("player_skip_next")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "بعدی",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Close Player
                                IconButton(
                                    onClick = {
                                        isPlaying = false
                                        try {
                                            if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                        } catch (e: Exception) {}
                                        currentPlayingMusic = null
                                    },
                                    modifier = Modifier.testTag("player_close")
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "بستن پلیر",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
