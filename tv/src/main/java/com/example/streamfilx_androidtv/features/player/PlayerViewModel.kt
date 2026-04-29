package com.example.streamfilx_androidtv.features.player

import android.content.Context
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import com.example.streamfilx_androidtv.services.OpenSubtitlesService
import com.example.streamfilx_androidtv.services.WatchHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val watchHistoryManager: WatchHistoryManager,
    private val openSubtitlesService: OpenSubtitlesService,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val wrapper = VlcPlayerWrapper(context)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var currentItem: NowPlayingItem? = null
    private var saveJob: Job? = null

    // Map of virtual external subtitle ID → OpenSubtitles file ID
    private val externalSubtitleMap = mutableMapOf<Int, Int>()
    private var externalSubtitleCounter = -100

    init {
        wrapper.onPlaying = {
            _state.update {
                it.copy(isPlaying = true, isPaused = false, isBuffering = false, isLoading = false, error = null)
            }
            refreshTracks()
        }
        wrapper.onPaused = {
            _state.update { it.copy(isPlaying = false, isPaused = true) }
            flushProgressNow()
        }
        wrapper.onStopped = {
            _state.update { it.copy(isPlaying = false, isPaused = false) }
        }
        wrapper.onBuffering = { percent ->
            _state.update { it.copy(isBuffering = percent < 100f) }
        }
        wrapper.onTimeChanged = { timeMs, lengthMs ->
            val currentSec = timeMs / 1000.0
            val durationSec = (lengthMs / 1000.0).coerceAtLeast(0.0)
            val progress = if (durationSec > 0) (currentSec / durationSec).coerceIn(0.0, 1.0) else 0.0
            _state.update { it.copy(currentTime = currentSec, duration = durationSec, progress = progress) }
            scheduleProgressSave(currentSec, durationSec)
        }
        wrapper.onError = { msg ->
            _state.update { it.copy(error = msg, isLoading = false, isPlaying = false, isBuffering = false) }
        }
        wrapper.onEndReached = {
            val s = _state.value
            _state.update { it.copy(isPlaying = false, isPaused = false, progress = 1.0) }
            flushProgress(s.currentTime, s.duration)
        }
        wrapper.onTracksChanged = { refreshTracks() }
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun play(nowPlaying: NowPlayingItem) {
        currentItem = nowPlaying
        externalSubtitleMap.clear()
        externalSubtitleCounter = -100
        _state.update { PlayerState(isLoading = true) }
        saveJob?.cancel()
        wrapper.play(nowPlaying.streamUrl)
        viewModelScope.launch { searchExternalSubtitles(nowPlaying) }
    }

    fun togglePlayPause() = wrapper.togglePlayPause()
    fun pause() = wrapper.pause()
    fun resume() = wrapper.resume()

    fun seekTo(seconds: Double) = wrapper.seekTo(seconds)
    fun seekToPosition(position: Float) = wrapper.seekToPosition(position)
    fun skipForward(seconds: Double) = wrapper.skipForward(seconds)
    fun skipBackward(seconds: Double) = wrapper.skipBackward(seconds)

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        wrapper.setRate(speed)
    }

    fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 200)
        _state.update { it.copy(volume = clamped, isMuted = clamped == 0) }
        wrapper.setVolume(clamped)
    }

    // ── Track selection ───────────────────────────────────────────────────────

    fun selectAudioTrack(id: Int) {
        wrapper.setAudioTrack(id)
        _state.update { it.copy(selectedAudioTrackId = id) }
    }

    fun selectSubtitleTrack(id: Int) {
        if (id <= -100) {
            // External subtitle from OpenSubtitles — download and inject
            val fileId = externalSubtitleMap[id] ?: return
            viewModelScope.launch {
                val url = openSubtitlesService.downloadSubtitle(fileId)
                if (url != null) {
                    wrapper.addExternalSubtitle(url)
                    _state.update { it.copy(selectedSubtitleTrackId = id) }
                    delay(1_200)
                    refreshTracks()
                }
            }
        } else {
            wrapper.setSubtitleTrack(id)
            _state.update { it.copy(selectedSubtitleTrackId = id) }
        }
    }

    private fun refreshTracks() {
        val audio = wrapper.getAudioTracks()
        val embedded = wrapper.getSubtitleTracks()
        val currentAudio = wrapper.getCurrentAudioTrackId()
        val currentSub = wrapper.getCurrentSubtitleTrackId()

        // Preserve external subtitle entries already in state
        val external = _state.value.subtitleTracks.filter { it.isExternal }
        val merged = embedded + external

        _state.update {
            it.copy(
                audioTracks = audio,
                subtitleTracks = merged,
                selectedAudioTrackId = currentAudio,
                selectedSubtitleTrackId = if (currentSub != -1) currentSub else it.selectedSubtitleTrackId,
            )
        }
        autoSelectAudio(audio)
    }

    private fun autoSelectAudio(tracks: List<AudioTrack>) {
        if (tracks.isEmpty()) return
        val preferred = tracks.firstOrNull { it.language.contains("en", ignoreCase = true) }
            ?: tracks.firstOrNull { it.language.contains("hi", ignoreCase = true) }
            ?: return
        wrapper.setAudioTrack(preferred.id)
        _state.update { it.copy(selectedAudioTrackId = preferred.id) }
    }

    // ── External subtitles ────────────────────────────────────────────────────

    private suspend fun searchExternalSubtitles(nowPlaying: NowPlayingItem) {
        val apiKey = appPreferences.openSubtitlesApiKey.first() ?: return
        if (apiKey.isBlank()) return

        val lang = appPreferences.subtitleLanguage.first()
        val results = openSubtitlesService.searchSubtitles(
            imdbId = nowPlaying.id,
            season = nowPlaying.season,
            episode = nowPlaying.episode,
            languages = lang,
        )
        if (results.isEmpty()) return

        val externalTracks = results.mapNotNull { item ->
            val fileId = item.attributes.files.firstOrNull()?.fileId ?: return@mapNotNull null
            val virtualId = externalSubtitleCounter--
            externalSubtitleMap[virtualId] = fileId
            val langLabel = item.attributes.language.uppercase()
            SubtitleTrack(
                id = virtualId,
                name = "$langLabel (OpenSubtitles)",
                language = item.attributes.language,
                isExternal = true,
            )
        }
        if (externalTracks.isEmpty()) return

        _state.update { state ->
            val withoutExternal = state.subtitleTracks.filter { !it.isExternal }
            state.copy(subtitleTracks = withoutExternal + externalTracks)
        }

        // Auto-select if subtitles are enabled and none currently selected
        val subtitlesEnabled = appPreferences.subtitlesEnabled.first()
        if (subtitlesEnabled && _state.value.selectedSubtitleTrackId == -1) {
            externalTracks.firstOrNull()?.let { selectSubtitleTrack(it.id) }
        }
    }

    // ── Surface ───────────────────────────────────────────────────────────────

    fun attachSurface(surfaceView: SurfaceView) = wrapper.attachSurface(surfaceView)
    fun detachSurface() = wrapper.detachSurface()

    fun stop() {
        flushProgressNow()
        wrapper.stop()
    }

    // ── Watch history ─────────────────────────────────────────────────────────

    private fun scheduleProgressSave(currentSec: Double, durationSec: Double) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1_000)
            val item = currentItem ?: return@launch
            watchHistoryManager.saveProgress(item, currentSec, durationSec)
        }
    }

    private fun flushProgressNow() {
        val s = _state.value
        flushProgress(s.currentTime, s.duration)
    }

    private fun flushProgress(currentSec: Double, durationSec: Double) {
        val item = currentItem ?: return
        if (currentSec < 1.0 || durationSec < 1.0) return
        saveJob?.cancel()
        viewModelScope.launch {
            watchHistoryManager.saveProgress(item, currentSec, durationSec)
        }
    }

    override fun onCleared() {
        super.onCleared()
        flushProgressNow()
        wrapper.release()
    }
}
