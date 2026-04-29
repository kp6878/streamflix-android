package com.example.streamfilx_androidtv.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.example.streamfilx_androidtv.core.models.AddonManifest
import com.example.streamfilx_androidtv.core.network.ResponseCache
import com.example.streamfilx_androidtv.core.network.StremioClient
import com.example.streamfilx_androidtv.data.db.StreamFlixDatabase
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import com.example.streamfilx_androidtv.services.OpenSubtitlesService
import com.example.streamfilx_androidtv.services.RealDebridService
import com.example.streamfilx_androidtv.services.RemoteUserPreferencesDto
import com.example.streamfilx_androidtv.services.SupabaseService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class InstalledAddon(
    val baseUrl: String,
    val manifest: AddonManifest,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
)

data class SettingsUiState(
    // Accounts
    val supabaseEmail: String? = null,
    val isSigningOut: Boolean = false,
    val rdApiKey: String = "",
    val rdKeyVisible: Boolean = false,
    val rdValidating: Boolean = false,
    val rdUser: com.example.streamfilx_androidtv.core.models.RDUser? = null,
    val rdError: String? = null,
    val osApiKey: String = "",
    val osApiKeyVisible: Boolean = false,
    val osUsername: String = "",
    val osPassword: String = "",
    val osPasswordVisible: Boolean = false,
    val osValidating: Boolean = false,
    val osLoggedIn: Boolean = false,
    val osError: String? = null,
    // Playback
    val primaryAudioLang: String = "en",
    val secondaryAudioLang: String = "hi",
    val subtitlesEnabled: Boolean = true,
    val subtitleLang: String = "en",
    val subtitleTextSize: Int = 3,           // 1-6 index
    val autoPlayNext: Boolean = true,
    val defaultQuality: String = "auto",
    // Storage
    val imageCacheGb: Int = 2,
    val cacheDiskUsedMb: Long = 0,
    val isClearingCache: Boolean = false,
    // Addons
    val installedAddons: List<InstalledAddon> = emptyList(),
    val addonUrlInput: String = "",
    val isAddingAddon: Boolean = false,
    val addonAddResult: String? = null,      // success/error message
    // About
    val showFactoryResetDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val supabaseService: SupabaseService,
    private val realDebridService: RealDebridService,
    private val openSubtitlesService: OpenSubtitlesService,
    private val stremioClient: StremioClient,
    private val responseCache: ResponseCache,
    private val database: StreamFlixDatabase,
    private val imageLoader: ImageLoader,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val gson = Gson()
    private val addonsFile = File(context.filesDir, "installed_addons.json")

    init {
        viewModelScope.launch { loadAll() }
    }

    private suspend fun loadAll() {
        val rdKey = appPreferences.realDebridApiKey.first() ?: ""
        val osKey = appPreferences.openSubtitlesApiKey.first() ?: ""
        val osUser = appPreferences.openSubtitlesUsername.first() ?: ""
        val osToken = appPreferences.openSubtitlesToken.first()
        _state.update {
            it.copy(
                supabaseEmail = supabaseService.currentUserEmail,
                rdApiKey = rdKey,
                osApiKey = osKey,
                osUsername = osUser,
                osLoggedIn = !osToken.isNullOrBlank(),
                primaryAudioLang = appPreferences.primaryAudioLanguage.first(),
                secondaryAudioLang = appPreferences.secondaryAudioLanguage.first() ?: "hi",
                subtitlesEnabled = appPreferences.subtitlesEnabled.first(),
                subtitleLang = appPreferences.subtitleLanguage.first(),
                subtitleTextSize = appPreferences.subtitleTextSize.first(),
                autoPlayNext = appPreferences.autoPlayNextEpisode.first(),
                imageCacheGb = appPreferences.imageCacheSizeGb.first(),
                cacheDiskUsedMb = diskUsedMb(),
                installedAddons = loadAddons(rdKey),
            )
        }
    }

    // ── Accounts ──────────────────────────────────────────────────────────────

    fun signOut(onSignedOut: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(isSigningOut = true) }
        supabaseService.signOut()
        _state.update { it.copy(isSigningOut = false) }
        onSignedOut()
    }

    fun setRdApiKey(key: String) {
        _state.update { it.copy(rdApiKey = key, rdError = null, rdUser = null) }
    }

    fun toggleRdKeyVisible() = _state.update { it.copy(rdKeyVisible = !it.rdKeyVisible) }

    fun validateRdKey() = viewModelScope.launch {
        val key = _state.value.rdApiKey.trim()
        if (key.isEmpty()) return@launch
        _state.update { it.copy(rdValidating = true, rdError = null) }
        appPreferences.setRealDebridApiKey(key)
        syncPreferences()
        realDebridService.validateApiKey().fold(
            onSuccess = { user ->
                _state.update { it.copy(rdValidating = false, rdUser = user, installedAddons = loadAddons(key)) }
            },
            onFailure = { e ->
                _state.update { it.copy(rdValidating = false, rdError = e.message ?: "Invalid API key") }
            },
        )
    }

    fun setOsApiKey(key: String) = _state.update { it.copy(osApiKey = key, osError = null) }
    fun setOsUsername(u: String) = _state.update { it.copy(osUsername = u) }
    fun setOsPassword(p: String) = _state.update { it.copy(osPassword = p) }
    fun toggleOsApiKeyVisible() = _state.update { it.copy(osApiKeyVisible = !it.osApiKeyVisible) }
    fun toggleOsPasswordVisible() = _state.update { it.copy(osPasswordVisible = !it.osPasswordVisible) }

    fun validateOs() = viewModelScope.launch {
        val key = _state.value.osApiKey.trim()
        val user = _state.value.osUsername.trim()
        val pass = _state.value.osPassword
        if (key.isEmpty()) return@launch
        _state.update { it.copy(osValidating = true, osError = null) }
        appPreferences.setOpenSubtitlesCredentials(apiKey = key, username = user.ifEmpty { null }, token = null)
        if (user.isNotEmpty() && pass.isNotEmpty()) {
            openSubtitlesService.login(user, pass).fold(
                onSuccess = { _state.update { s -> s.copy(osValidating = false, osLoggedIn = true) } },
                onFailure = { e -> _state.update { s -> s.copy(osValidating = false, osError = e.message) } },
            )
        } else {
            _state.update { it.copy(osValidating = false, osLoggedIn = false) }
        }
        syncPreferences()
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun setPrimaryAudioLang(lang: String) = viewModelScope.launch {
        appPreferences.setPrimaryAudioLanguage(lang)
        _state.update { it.copy(primaryAudioLang = lang) }
        syncPreferences()
    }

    fun setSecondaryAudioLang(lang: String) = viewModelScope.launch {
        appPreferences.setSecondaryAudioLanguage(lang)
        _state.update { it.copy(secondaryAudioLang = lang) }
        syncPreferences()
    }

    fun setSubtitlesEnabled(enabled: Boolean) = viewModelScope.launch {
        appPreferences.setSubtitlesEnabled(enabled)
        _state.update { it.copy(subtitlesEnabled = enabled) }
        syncPreferences()
    }

    fun setSubtitleLang(lang: String) = viewModelScope.launch {
        appPreferences.setSubtitleLanguage(lang)
        _state.update { it.copy(subtitleLang = lang) }
        syncPreferences()
    }

    fun setSubtitleTextSize(size: Int) = viewModelScope.launch {
        appPreferences.setSubtitleTextSize(size)
        _state.update { it.copy(subtitleTextSize = size) }
        syncPreferences()
    }

    fun setAutoPlayNext(enabled: Boolean) = viewModelScope.launch {
        appPreferences.setAutoPlayNextEpisode(enabled)
        _state.update { it.copy(autoPlayNext = enabled) }
        syncPreferences()
    }

    fun setDefaultQuality(quality: String) = _state.update { it.copy(defaultQuality = quality) }

    // ── Storage ───────────────────────────────────────────────────────────────

    fun setImageCacheGb(gb: Int) = viewModelScope.launch {
        appPreferences.setImageCacheSizeGb(gb)
        _state.update { it.copy(imageCacheGb = gb) }
        syncPreferences()
    }

    fun clearCaches() = viewModelScope.launch {
        _state.update { it.copy(isClearingCache = true) }
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        responseCache.clear()
        _state.update { it.copy(isClearingCache = false, cacheDiskUsedMb = diskUsedMb()) }
    }

    // ── Supabase preferences sync ─────────────────────────────────────────────

    private fun syncPreferences() = viewModelScope.launch {
        val dto = RemoteUserPreferencesDto(
            realDebridApiKey = appPreferences.realDebridApiKey.first(),
            preferredAudioLanguage = appPreferences.primaryAudioLanguage.first(),
            secondaryAudioLanguage = appPreferences.secondaryAudioLanguage.first(),
            subtitlesEnabled = appPreferences.subtitlesEnabled.first(),
            preferredSubtitleLanguage = appPreferences.subtitleLanguage.first(),
            opensubtitlesApiKey = appPreferences.openSubtitlesApiKey.first(),
            opensubtitlesUsername = appPreferences.openSubtitlesUsername.first(),
            maxCacheSizeGb = appPreferences.imageCacheSizeGb.first(),
        )
        supabaseService.syncUserPreferences(dto)
    }

    suspend fun applyRemotePreferences(dto: RemoteUserPreferencesDto) {
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
        loadAll()
    }

    private fun diskUsedMb(): Long {
        val coilDir = context.cacheDir.resolve("image_cache")
        return coilDir.walkTopDown().sumOf { it.length() } / (1024 * 1024)
    }

    // ── Addons ────────────────────────────────────────────────────────────────

    fun setAddonUrlInput(url: String) = _state.update { it.copy(addonUrlInput = url, addonAddResult = null) }

    fun addAddon() = viewModelScope.launch {
        val raw = _state.value.addonUrlInput.trim()
        if (raw.isEmpty()) return@launch
        val url = normalizeAddonUrl(raw)
        _state.update { it.copy(isAddingAddon = true, addonAddResult = null) }
        runCatching { stremioClient.fetchManifest(url) }.fold(
            onSuccess = { manifest ->
                val current = _state.value.installedAddons
                if (current.any { it.baseUrl == url }) {
                    _state.update { it.copy(isAddingAddon = false, addonAddResult = "❌ Addon already installed") }
                    return@launch
                }
                val updated = current + InstalledAddon(baseUrl = url, manifest = manifest)
                saveAddons(updated)
                _state.update {
                    it.copy(
                        isAddingAddon = false,
                        addonUrlInput = "",
                        addonAddResult = "✅ Added \"${manifest.name}\"",
                        installedAddons = updated,
                    )
                }
            },
            onFailure = { e ->
                _state.update {
                    it.copy(isAddingAddon = false, addonAddResult = "❌ ${e.message ?: "Failed to load addon"}")
                }
            },
        )
    }

    fun toggleAddon(baseUrl: String) {
        val updated = _state.value.installedAddons.map {
            if (it.baseUrl == baseUrl) it.copy(enabled = !it.enabled) else it
        }
        saveAddons(updated)
        _state.update { it.copy(installedAddons = updated) }
    }

    fun removeAddon(baseUrl: String) {
        val updated = _state.value.installedAddons.filter { it.baseUrl != baseUrl }
        saveAddons(updated)
        _state.update { it.copy(installedAddons = updated) }
    }

    fun clearAddonResult() = _state.update { it.copy(addonAddResult = null) }

    private fun normalizeAddonUrl(url: String): String {
        val base = url.trimEnd('/')
        return if (base.endsWith("manifest.json")) base.removeSuffix("/manifest.json")
        else base
    }

    private fun loadAddons(rdKey: String): List<InstalledAddon> {
        val custom = runCatching {
            val json = addonsFile.readText()
            val type = object : TypeToken<List<InstalledAddon>>() {}.type
            gson.fromJson<List<InstalledAddon>>(json, type)
        }.getOrDefault(emptyList())

        val rdConfigured = rdKey.isNotBlank()
        return buildList {
            add(InstalledAddon(
                baseUrl = "https://v3-cinemeta.strem.io",
                manifest = AddonManifest(
                    id = "com.linvo.cinemeta",
                    version = "3.0.14",
                    name = "Cinemeta",
                    description = "Movie & TV metadata from IMDB/TMDB",
                    resources = listOf("catalog", "meta"),
                    types = listOf("movie", "series"),
                ),
                enabled = true,
                isBuiltIn = true,
            ))
            if (rdConfigured) {
                add(InstalledAddon(
                    baseUrl = "https://torrentio.strem.fun/realdebrid=${rdKey}",
                    manifest = AddonManifest(
                        id = "com.torrentio",
                        version = "1.0.0",
                        name = "Torrentio",
                        description = "Torrent streams with Real-Debrid",
                        resources = listOf("stream"),
                        types = listOf("movie", "series"),
                    ),
                    enabled = true,
                    isBuiltIn = true,
                ))
                add(InstalledAddon(
                    baseUrl = "https://torrentio.strem.fun/providers=yts,eztv,rarbg,1337x,thepiratebay,kickasstorrents,torrentgalaxy,magnetdl,horriblesubs,nyaasi,tokyotosho,anidex|realdebrid=${rdKey}",
                    manifest = AddonManifest(
                        id = "com.torrentsdb",
                        version = "1.0.0",
                        name = "TorrentsDB",
                        description = "Alternative torrent streams with Real-Debrid",
                        resources = listOf("stream"),
                        types = listOf("movie", "series"),
                    ),
                    enabled = true,
                    isBuiltIn = true,
                ))
            }
            addAll(custom)
        }
    }

    private fun saveAddons(addons: List<InstalledAddon>) {
        val custom = addons.filter { !it.isBuiltIn }
        addonsFile.writeText(gson.toJson(custom))
    }

    // ── About / Factory reset ─────────────────────────────────────────────────

    fun showFactoryResetDialog() = _state.update { it.copy(showFactoryResetDialog = true) }
    fun dismissFactoryResetDialog() = _state.update { it.copy(showFactoryResetDialog = false) }

    fun performFactoryReset(onComplete: () -> Unit) = viewModelScope.launch {
        appPreferences.clearAll()
        database.clearAllTables()
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        responseCache.clear()
        addonsFile.delete()
        File(context.filesDir, "downloads").deleteRecursively()
        _state.update { it.copy(showFactoryResetDialog = false) }
        onComplete()
    }
}

// ── Common language options ───────────────────────────────────────────────────

val AUDIO_LANGUAGES = listOf(
    "en" to "English",
    "hi" to "Hindi",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "pt" to "Portuguese",
    "it" to "Italian",
    "ru" to "Russian",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese",
    "ar" to "Arabic",
    "tr" to "Turkish",
    "pl" to "Polish",
    "nl" to "Dutch",
)

val SUBTITLE_TEXT_SIZES = listOf(
    1 to "XS",
    2 to "Small",
    3 to "Medium",
    4 to "Large",
    5 to "XL",
    6 to "Max",
)

val QUALITY_OPTIONS = listOf("auto" to "Auto", "2160p" to "4K", "1080p" to "FHD", "720p" to "HD", "480p" to "SD")
