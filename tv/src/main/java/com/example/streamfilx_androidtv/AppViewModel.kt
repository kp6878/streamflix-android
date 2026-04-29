package com.example.streamfilx_androidtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import com.example.streamfilx_androidtv.services.FavoritesManager
import com.example.streamfilx_androidtv.services.Profile
import com.example.streamfilx_androidtv.services.ProfileManager
import com.example.streamfilx_androidtv.services.SupabaseService
import com.example.streamfilx_androidtv.services.WatchHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val supabaseService: SupabaseService,
    private val appPreferences: AppPreferences,
    private val profileManager: ProfileManager,
    private val watchHistoryManager: WatchHistoryManager,
    private val favoritesManager: FavoritesManager,
) : ViewModel() {

    private val _isBootstrapping = MutableStateFlow(true)
    val isBootstrapping: StateFlow<Boolean> = _isBootstrapping.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    // ── Now-playing state (player overlay) ───────────────────────────────────

    private val _nowPlaying = MutableStateFlow<NowPlayingItem?>(null)
    val nowPlaying: StateFlow<NowPlayingItem?> = _nowPlaying.asStateFlow()

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        if (supabaseService.hasActiveSession()) {
            _isSignedIn.value = true
            val savedId = appPreferences.activeProfileId.first()
            if (!savedId.isNullOrBlank()) {
                _selectedProfile.value = profileManager.getById(savedId)
            }
            // Restore remote data in the background without blocking bootstrap
            viewModelScope.launch { syncRemoteDataOnSignIn() }
        }
        _isBootstrapping.value = false
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    fun onSignedIn() {
        _isSignedIn.value = true
        viewModelScope.launch { syncRemoteDataOnSignIn() }
    }

    fun onSignedOut() {
        viewModelScope.launch {
            supabaseService.signOut()
            profileManager.clearSelection()
            _selectedProfile.value = null
            _nowPlaying.value = null
            _isSignedIn.value = false
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    fun onProfileSelected(profile: Profile) {
        viewModelScope.launch {
            profileManager.selectProfile(profile.id)
            _selectedProfile.value = profile
            // Fetch this profile's remote data in the background
            launch { watchHistoryManager.loadFromRemote(profile.id) }
            launch { favoritesManager.loadFromRemote(profile.id) }
        }
    }

    fun switchProfile() {
        viewModelScope.launch {
            profileManager.clearSelection()
            _selectedProfile.value = null
            _nowPlaying.value = null
        }
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun startPlayback(item: NowPlayingItem) {
        _nowPlaying.value = item
    }

    fun stopPlayback() {
        _nowPlaying.value = null
    }

    // ── Remote sync helpers ───────────────────────────────────────────────────

    private suspend fun syncRemoteDataOnSignIn() {
        // Fetch profiles and merge into local DB
        profileManager.loadFromRemote()

        // Fetch user preferences and apply to local DataStore
        supabaseService.fetchUserPreferences().getOrNull()?.let { dto ->
            dto.realDebridApiKey?.let { appPreferences.setRealDebridApiKey(it) }
            dto.preferredAudioLanguage?.let { appPreferences.setPrimaryAudioLanguage(it) }
            dto.secondaryAudioLanguage?.let { appPreferences.setSecondaryAudioLanguage(it) }
            dto.subtitlesEnabled?.let { appPreferences.setSubtitlesEnabled(it) }
            dto.preferredSubtitleLanguage?.let { appPreferences.setSubtitleLanguage(it) }
            if (dto.opensubtitlesApiKey != null || dto.opensubtitlesUsername != null) {
                appPreferences.setOpenSubtitlesCredentials(
                    apiKey = dto.opensubtitlesApiKey,
                    username = dto.opensubtitlesUsername,
                    token = null,
                )
            }
            dto.maxCacheSizeGb?.let { appPreferences.setImageCacheSizeGb(it) }
        }
    }
}
