package com.example.streamfilx_androidtv.features.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.streamfilx_androidtv.core.models.EpisodeInfo
import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// PlayerScreen — full-screen video player overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerScreen(
    nowPlaying: NowPlayingItem,
    onClose: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    // ── Controls visibility ───────────────────────────────────────────────────
    var showControls by remember { mutableStateOf(true) }
    var controlsHideJob by remember { mutableStateOf<Job?>(null) }

    fun startControlsTimer() {
        controlsHideJob?.cancel()
        controlsHideJob = scope.launch {
            delay(4_000)
            showControls = false
        }
    }

    fun toggleControls() {
        showControls = !showControls
        if (showControls && state.isPlaying) startControlsTimer()
    }

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) startControlsTimer() else controlsHideJob?.cancel()
    }

    // ── Sheet/overlay state ───────────────────────────────────────────────────
    var trackPickerType by remember { mutableStateOf<TrackPickerType?>(null) }
    var showEpisodePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    // ── Skip intro / Up next ──────────────────────────────────────────────────
    var skipIntroCancelled by remember { mutableStateOf(false) }
    var autoPlayCancelled by remember { mutableStateOf(false) }
    var upNextCountdown by remember { mutableIntStateOf(10) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    val showSkipIntro = nowPlaying.type == "series" &&
        state.currentTime in 5.0..90.0 &&
        !skipIntroCancelled

    val showUpNext = nowPlaying.hasNextEpisode &&
        state.duration > 0 &&
        !autoPlayCancelled &&
        (state.remainingTime < 30 || state.progress > 0.95)

    LaunchedEffect(showUpNext) {
        if (showUpNext && !autoPlayCancelled) {
            upNextCountdown = 10
            countdownJob = scope.launch {
                while (upNextCountdown > 0) {
                    delay(1_000)
                    upNextCountdown--
                }
                onNextEpisode?.invoke()
                onClose()
            }
        } else {
            countdownJob?.cancel()
        }
    }

    LaunchedEffect(nowPlaying.streamUrl) {
        skipIntroCancelled = false
        autoPlayCancelled = false
        countdownJob?.cancel()
        viewModel.play(nowPlaying)
    }

    // ── Immersive mode ────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val ctrl = WindowInsetsControllerCompat(window, window.decorView)
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            viewModel.stop()
        }
    }

    // ── Clock ─────────────────────────────────────────────────────────────────
    var clockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            clockMs = System.currentTimeMillis()
        }
    }
    val clockFormatted = remember(clockMs) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(clockMs))
    }

    // ── Gesture state ─────────────────────────────────────────────────────────
    var brightnessIndicator by remember { mutableFloatStateOf(-1f) }
    var volumeIndicator by remember { mutableIntStateOf(-1) }
    var gestureIndicatorJob by remember { mutableStateOf<Job?>(null) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    fun hideGestureIndicators() {
        gestureIndicatorJob?.cancel()
        gestureIndicatorJob = scope.launch {
            delay(1_500)
            brightnessIndicator = -1f
            volumeIndicator = -1
        }
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Video surface
        VideoSurface(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture layer (tap + drag)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { toggleControls() },
                        onDoubleTap = { offset ->
                            showControls = true
                            if (state.isPlaying) startControlsTimer()
                            if (offset.x < size.width / 2) viewModel.skipBackward(10.0)
                            else viewModel.skipForward(10.0)
                        },
                    )
                }
                .pointerInput(Unit) {
                    var dragStartX = 0f
                    var dragStartY = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                            dragStartY = offset.y
                        },
                        onDrag = { change, dragAmount ->
                            val isVertical = kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)
                            if (isVertical) {
                                val isLeftHalf = dragStartX < size.width / 2
                                val delta = -dragAmount.y / size.height
                                if (isLeftHalf) {
                                    // Brightness
                                    val window = activity?.window ?: return@detectDragGestures
                                    val lp = window.attributes
                                    val newBrightness = (lp.screenBrightness + delta).coerceIn(0.01f, 1f)
                                    lp.screenBrightness = newBrightness
                                    window.attributes = lp
                                    brightnessIndicator = newBrightness
                                    hideGestureIndicators()
                                } else {
                                    // Volume
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val step = if (delta > 0) 1 else if (delta < 0) -1 else 0
                                    val newVol = (currentVol + step).coerceIn(0, maxVol)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    volumeIndicator = ((newVol.toFloat() / maxVol) * 100).toInt()
                                    hideGestureIndicators()
                                }
                            }
                        },
                    )
                },
        )

        // Buffering indicator
        if (state.isBuffering && !state.isPlaying) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center),
            )
        }

        // Error overlay
        state.error?.let { err ->
            ErrorOverlay(
                message = err,
                onDismiss = onClose,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Info overlay (paused)
        if (showControls && !state.isPlaying && !state.isBuffering && state.error == null) {
            InfoOverlay(nowPlaying = nowPlaying)
        }

        // Controls overlay
        AnimatedVisibility(
            visible = showControls && state.error == null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    TopLeftInfo(nowPlaying = nowPlaying, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(16.dp))
                    TopRightInfo(
                        clockFormatted = clockFormatted,
                        endsAt = state.endsAtTime,
                        onClose = onClose,
                    )
                }

                // Center play/pause
                if (state.isPlaying || state.isPaused) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .align(Alignment.Center),
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                // Progress + bottom toolbar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlayerProgressBar(
                        state = state,
                        onSeek = { viewModel.seekToPosition(it) },
                    )
                    BottomToolbar(
                        state = state,
                        nowPlaying = nowPlaying,
                        showVolumeSlider = showVolumeSlider,
                        onSubtitlesClick = {
                            trackPickerType = TrackPickerType.SUBTITLE
                            controlsHideJob?.cancel()
                        },
                        onAudioClick = {
                            trackPickerType = TrackPickerType.AUDIO
                            controlsHideJob?.cancel()
                        },
                        onEpisodesClick = {
                            showEpisodePicker = true
                            controlsHideJob?.cancel()
                        },
                        onVolumeClick = { showVolumeSlider = !showVolumeSlider },
                        onVolumeChange = { viewModel.setVolume(it) },
                        onNextEpisodeClick = {
                            onNextEpisode?.invoke()
                            onClose()
                        },
                        onSettingsClick = {
                            showSettings = true
                            controlsHideJob?.cancel()
                        },
                    )
                }
            }
        }

        // Skip intro button (bottom-right, above toolbar)
        if (showSkipIntro) {
            PillButton(
                icon = Icons.Default.SkipNext,
                label = "Skip Intro",
                onClick = {
                    viewModel.skipForward(30.0)
                    skipIntroCancelled = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 100.dp),
            )
        }

        // Up next overlay
        if (showUpNext) {
            UpNextOverlay(
                nowPlaying = nowPlaying,
                countdown = upNextCountdown,
                onCancel = {
                    autoPlayCancelled = true
                    countdownJob?.cancel()
                },
                onPlayNow = {
                    onNextEpisode?.invoke()
                    onClose()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 140.dp),
            )
        }

        // Gesture indicators
        if (brightnessIndicator >= 0f) {
            GestureIndicator(
                label = "Brightness",
                value = (brightnessIndicator * 100).toInt(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 40.dp),
            )
        }
        if (volumeIndicator >= 0) {
            GestureIndicator(
                label = "Volume",
                value = volumeIndicator,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 40.dp),
            )
        }

        // Track picker (audio / subtitle)
        trackPickerType?.let { type ->
            TrackPickerDialog(
                type = type,
                state = state,
                onSelectAudio = { viewModel.selectAudioTrack(it) },
                onSelectSubtitle = { viewModel.selectSubtitleTrack(it) },
                onDismiss = {
                    trackPickerType = null
                    if (state.isPlaying) startControlsTimer()
                },
            )
        }

        // Episode picker
        if (showEpisodePicker && nowPlaying.allEpisodes != null) {
            EpisodePickerDialog(
                nowPlaying = nowPlaying,
                onDismiss = {
                    showEpisodePicker = false
                    if (state.isPlaying) startControlsTimer()
                },
            )
        }

        // Video settings
        if (showSettings) {
            VideoSettingsDialog(
                state = state,
                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                onDismiss = {
                    showSettings = false
                    if (state.isPlaying) startControlsTimer()
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video surface
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoSurface(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val surfaceView = remember { SurfaceView(context) }

    AndroidView(
        factory = { surfaceView },
        modifier = modifier.background(Color.Black),
    )

    DisposableEffect(surfaceView) {
        val callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                viewModel.attachSurface(surfaceView)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                viewModel.detachSurface()
            }
        }
        surfaceView.holder.addCallback(callback)
        if (surfaceView.holder.surface?.isValid == true) {
            viewModel.attachSurface(surfaceView)
        }
        onDispose {
            surfaceView.holder.removeCallback(callback)
            viewModel.detachSurface()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopLeftInfo(nowPlaying: NowPlayingItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = nowPlaying.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            nowPlaying.year?.let {
                Text(it, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.8f))
            }
            if (nowPlaying.type == "series") {
                nowPlaying.episodeInfo?.let { ep ->
                    Text("•", fontSize = 14.sp, color = Color.White.copy(0.5f))
                    Text(ep, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                nowPlaying.episodeName?.takeIf { it.isNotEmpty() }?.let { name ->
                    Text("•", fontSize = 14.sp, color = Color.White.copy(0.5f))
                    Text(
                        text = "\"$name\"",
                        fontSize = 14.sp,
                        color = Color.White.copy(0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            nowPlaying.qualityBadge?.let { QualityBadge(it) }
        }
    }
}

@Composable
private fun TopRightInfo(clockFormatted: String, endsAt: String, onClose: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = clockFormatted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
            )
            if (endsAt.isNotEmpty()) {
                Text(
                    text = "Ends at $endsAt",
                    fontSize = 12.sp,
                    color = Color.White.copy(0.7f),
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape),
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun QualityBadge(quality: String) {
    val color = when (quality) {
        "4K" -> Color(0xFF8B5CF6)
        "FHD" -> Color(0xFF3B82F6)
        "HD" -> Color(0xFF22C55E)
        "SD" -> Color(0xFFF97316)
        else -> Color(0xFF6B7280)
    }
    Text(
        text = quality,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Progress bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerProgressBar(state: PlayerState, onSeek: (Float) -> Unit) {
    val scope = rememberCoroutineScope()
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    var scrubHideJob by remember { mutableStateOf<Job?>(null) }

    val displayProgress = if (isScrubbing) scrubProgress else state.progress.toFloat()
    val displayTime = if (isScrubbing) state.formatTime(scrubProgress * state.duration) else state.currentTimeFormatted

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = displayTime,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isScrubbing) Color.Red else Color.White,
            )
            Text(
                text = state.durationFormatted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            scrubHideJob?.cancel()
                            scrubProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            scrubProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek(scrubProgress)
                            scrubHideJob = scope.launch {
                                delay(500)
                                isScrubbing = false
                            }
                        },
                        onDragCancel = { isScrubbing = false },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pos = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(pos)
                    }
                },
        ) {
            val trackH = if (isScrubbing) 8.dp.toPx() else 5.dp.toPx()
            val cy = size.height / 2f
            val fillW = size.width * displayProgress

            // Background
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, cy - trackH / 2),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(trackH / 2),
            )
            // Fill
            if (fillW > 0f) {
                drawRoundRect(
                    color = Color.Red,
                    topLeft = Offset(0f, cy - trackH / 2),
                    size = Size(fillW, trackH),
                    cornerRadius = CornerRadius(trackH / 2),
                )
            }
            // Thumb
            if (isScrubbing) {
                drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(fillW, cy))
                drawCircle(
                    color = Color.Black.copy(0.5f),
                    radius = 8.dp.toPx(),
                    center = Offset(fillW, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom toolbar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomToolbar(
    state: PlayerState,
    nowPlaying: NowPlayingItem,
    showVolumeSlider: Boolean,
    onSubtitlesClick: () -> Unit,
    onAudioClick: () -> Unit,
    onEpisodesClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onNextEpisodeClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val subtitlesActive = state.selectedSubtitleTrackId != -1
    val audioLabel = state.audioTracks
        .firstOrNull { it.id == state.selectedAudioTrackId }
        ?.displayName?.take(3)
        ?: "EN"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillButton(
            icon = Icons.Default.Subtitles,
            label = if (subtitlesActive) "On" else "Off",
            isActive = subtitlesActive,
            onClick = onSubtitlesClick,
        )
        PillButton(
            icon = Icons.Default.VolumeUp,
            label = audioLabel,
            onClick = onAudioClick,
        )
        if (nowPlaying.type == "series" && !nowPlaying.allEpisodes.isNullOrEmpty()) {
            PillButton(
                icon = Icons.Default.List,
                label = "Episodes",
                onClick = onEpisodesClick,
            )
        }

        // Volume
        Column {
            PillButton(
                icon = Icons.Default.VolumeUp,
                onClick = onVolumeClick,
            )
            if (showVolumeSlider) {
                Slider(
                    value = state.volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 0f..200f,
                    modifier = Modifier.width(120.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(0.3f),
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next episode pill
        if (nowPlaying.hasNextEpisode) {
            NextEpisodePill(nowPlaying = nowPlaying, onClick = onNextEpisodeClick)
        }

        PillButton(
            icon = Icons.Default.Settings,
            onClick = onSettingsClick,
        )
    }
}

@Composable
internal fun PillButton(
    icon: ImageVector,
    label: String? = null,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = if (isActive) 0.25f else 0.15f),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (label == null) 14.dp else 18.dp,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (isActive) 1f else 0.9f),
                modifier = Modifier.size(20.dp),
            )
            label?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = if (isActive) 1f else 0.9f),
                )
            }
        }
    }
}

@Composable
private fun NextEpisodePill(nowPlaying: NowPlayingItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            nowPlaying.nextEpisodeName?.let { thumb ->
                // Thumbnail placeholder
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 36.dp)
                        .background(Color.Black.copy(0.1f), RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(4.dp))
            }
            Column {
                Text("Next Episode", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                nowPlaying.nextEpisodeInfo?.let {
                    Text(it, fontSize = 11.sp, color = Color.Black.copy(0.7f))
                }
            }
            Icon(Icons.Default.SkipNext, null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info overlay (paused)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoOverlay(nowPlaying: NowPlayingItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Center pause indicator
        Box(
            modifier = Modifier
                .background(Color.Black.copy(0.5f), CircleShape)
                .padding(28.dp)
                .align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Pause, null, tint = Color.White, modifier = Modifier.size(56.dp))
                Text(
                    "PAUSED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(0.7f),
                    letterSpacing = 2.sp,
                )
            }
        }

        // Bottom-left info panel
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 120.dp)
                .width(360.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = nowPlaying.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (nowPlaying.type == "series") {
                    nowPlaying.episodeInfo?.let { ep ->
                        val epText = buildString {
                            append(ep)
                            nowPlaying.episodeName?.takeIf { it.isNotEmpty() }?.let { append(" • \"$it\"") }
                        }
                        Text(epText, fontSize = 15.sp, color = Color.White.copy(0.9f), fontWeight = FontWeight.Medium)
                    }
                }
                // Genres + rating
                if (!nowPlaying.genres.isNullOrEmpty() || nowPlaying.imdbRating != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        nowPlaying.genres?.take(3)?.forEach { genre ->
                            Text(
                                text = genre,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                        nowPlaying.imdbRating?.toDoubleOrNull()?.let { rating ->
                            val pct = (rating * 10).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    "$pct%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80),
                                )
                            }
                        }
                    }
                }
                // Synopsis
                val synopsis = (nowPlaying.episodeOverview ?: nowPlaying.contentDescription)
                    ?.takeIf { it.isNotEmpty() }
                synopsis?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = Color.White.copy(0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Cast
                nowPlaying.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
                    Text(
                        text = cast.take(4).joinToString(", "),
                        fontSize = 12.sp,
                        color = Color.White.copy(0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Up Next overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UpNextOverlay(
    nowPlaying: NowPlayingItem,
    countdown: Int,
    onCancel: () -> Unit,
    onPlayNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Black.copy(0.8f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 70.dp)
                        .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Up Next", fontSize = 12.sp, color = Color.White.copy(0.7f))
                    nowPlaying.nextEpisodeInfo?.let {
                        Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    nowPlaying.nextEpisodeName?.let {
                        Text(it, fontSize = 13.sp, color = Color.White.copy(0.8f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onCancel,
                    color = Color.White.copy(0.2f),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        "Cancel",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                Surface(
                    onClick = onPlayNow,
                    color = Color.White,
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = if (countdown > 0) "$countdown Play Now ▶" else "Play Now ▶",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Track picker dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrackPickerDialog(
    type: TrackPickerType,
    state: PlayerState,
    onSelectAudio: (Int) -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color(0xFF1F1F1F),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(420.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (type == TrackPickerType.AUDIO) "Audio" else "Subtitles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    if (type == TrackPickerType.AUDIO) {
                        items(state.audioTracks) { track ->
                            val isSelected = track.id == state.selectedAudioTrackId
                            TrackRow(
                                name = track.displayName,
                                isSelected = isSelected,
                                onClick = { onSelectAudio(track.id); onDismiss() },
                            )
                        }
                    } else {
                        items(state.subtitleTracks) { track ->
                            val isSelected = track.id == state.selectedSubtitleTrackId
                            TrackRow(
                                name = track.displayName,
                                isSelected = isSelected,
                                onClick = { onSelectSubtitle(track.id); onDismiss() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.size(20.dp))
        }
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(0.7f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Episode picker dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EpisodePickerDialog(nowPlaying: NowPlayingItem, onDismiss: () -> Unit) {
    var selectedSeason by remember {
        mutableIntStateOf(
            nowPlaying.season ?: nowPlaying.availableSeasons.firstOrNull() ?: 1
        )
    }
    var showSeasonMenu by remember { mutableStateOf(false) }
    val episodes = nowPlaying.episodesForSeason(selectedSeason)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color(0xFF1F1F1F),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(520.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Episodes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                if (nowPlaying.availableSeasons.size > 1) {
                    Box {
                        Surface(
                            onClick = { showSeasonMenu = true },
                            color = Color.White.copy(0.1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "Season $selectedSeason ▾",
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showSeasonMenu,
                            onDismissRequest = { showSeasonMenu = false },
                        ) {
                            nowPlaying.availableSeasons.forEach { season ->
                                DropdownMenuItem(
                                    text = { Text("Season $season") },
                                    onClick = { selectedSeason = season; showSeasonMenu = false },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(440.dp)) {
                    items(episodes) { ep ->
                        val isCurrent = nowPlaying.isCurrentEpisode(ep)
                        EpisodeRow(episode = ep, isCurrent = isCurrent)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeInfo, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) Color.White.copy(0.1f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isCurrent) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        } else {
            Text(
                text = "${episode.episode}.",
                fontSize = 14.sp,
                color = Color.White.copy(0.5f),
                modifier = Modifier.width(20.dp).padding(top = 2.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = episode.displayTitle,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
            )
            episode.overview?.takeIf { it.isNotEmpty() }?.let {
                Text(it, fontSize = 12.sp, color = Color.White.copy(0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video settings dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoSettingsDialog(
    state: PlayerState,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val speedLabels = listOf("0.5×", "0.75×", "1×", "1.25×", "1.5×", "2×")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color(0xFF1F1F1F),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(360.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Playback Speed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    speeds.forEachIndexed { i, speed ->
                        val isSelected = state.playbackSpeed == speed
                        Surface(
                            onClick = { onSpeedChange(speed); onDismiss() },
                            color = if (isSelected) Color(0xFF3B82F6) else Color.White.copy(0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = speedLabels[i],
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorOverlay(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(0.9f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            Text("Playback Error", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.White.copy(0.7f),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            ) {
                Text("Dismiss")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gesture indicator (brightness / volume)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GestureIndicator(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, fontSize = 12.sp, color = Color.White.copy(0.7f))
            Text("$value%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
