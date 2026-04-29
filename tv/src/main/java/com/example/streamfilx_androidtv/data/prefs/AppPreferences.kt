package com.example.streamfilx_androidtv.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("streamflix_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    // Auth
    val authToken: Flow<String?> = store.data.map { it[Keys.AUTH_TOKEN] }
    val activeProfileId: Flow<String?> = store.data.map { it[Keys.ACTIVE_PROFILE_ID] }

    // API keys
    val realDebridApiKey: Flow<String?> = store.data.map { it[Keys.REAL_DEBRID_API_KEY] }
    val openSubtitlesApiKey: Flow<String?> = store.data.map { it[Keys.OPEN_SUBTITLES_API_KEY] }
    val openSubtitlesUsername: Flow<String?> = store.data.map { it[Keys.OPEN_SUBTITLES_USERNAME] }
    val openSubtitlesToken: Flow<String?> = store.data.map { it[Keys.OPEN_SUBTITLES_TOKEN] }

    // Playback settings
    val primaryAudioLanguage: Flow<String> = store.data.map { it[Keys.PRIMARY_AUDIO_LANGUAGE] ?: "en" }
    val secondaryAudioLanguage: Flow<String?> = store.data.map { it[Keys.SECONDARY_AUDIO_LANGUAGE] }
    val subtitlesEnabled: Flow<Boolean> = store.data.map { it[Keys.SUBTITLES_ENABLED] ?: false }
    val subtitleLanguage: Flow<String> = store.data.map { it[Keys.SUBTITLE_LANGUAGE] ?: "en" }
    val subtitleTextSize: Flow<Int> = store.data.map { it[Keys.SUBTITLE_TEXT_SIZE] ?: 3 }
    val autoPlayNextEpisode: Flow<Boolean> = store.data.map { it[Keys.AUTO_PLAY_NEXT_EPISODE] ?: true }

    // Storage settings
    val imageCacheSizeGb: Flow<Int> = store.data.map { it[Keys.IMAGE_CACHE_SIZE_GB] ?: 4 }

    suspend fun setAuthToken(token: String?) = store.edit {
        if (token == null) it.remove(Keys.AUTH_TOKEN) else it[Keys.AUTH_TOKEN] = token
    }

    suspend fun setActiveProfileId(id: String?) = store.edit {
        if (id == null) it.remove(Keys.ACTIVE_PROFILE_ID) else it[Keys.ACTIVE_PROFILE_ID] = id
    }

    suspend fun setRealDebridApiKey(key: String?) = store.edit {
        if (key == null) it.remove(Keys.REAL_DEBRID_API_KEY) else it[Keys.REAL_DEBRID_API_KEY] = key
    }

    suspend fun setOpenSubtitlesCredentials(apiKey: String?, username: String?, token: String?) =
        store.edit {
            if (apiKey == null) it.remove(Keys.OPEN_SUBTITLES_API_KEY) else it[Keys.OPEN_SUBTITLES_API_KEY] = apiKey
            if (username == null) it.remove(Keys.OPEN_SUBTITLES_USERNAME) else it[Keys.OPEN_SUBTITLES_USERNAME] = username
            if (token == null) it.remove(Keys.OPEN_SUBTITLES_TOKEN) else it[Keys.OPEN_SUBTITLES_TOKEN] = token
        }

    suspend fun setPrimaryAudioLanguage(lang: String) = store.edit { it[Keys.PRIMARY_AUDIO_LANGUAGE] = lang }
    suspend fun setSecondaryAudioLanguage(lang: String?) = store.edit {
        if (lang == null) it.remove(Keys.SECONDARY_AUDIO_LANGUAGE) else it[Keys.SECONDARY_AUDIO_LANGUAGE] = lang
    }

    suspend fun setSubtitlesEnabled(enabled: Boolean) = store.edit { it[Keys.SUBTITLES_ENABLED] = enabled }
    suspend fun setSubtitleLanguage(lang: String) = store.edit { it[Keys.SUBTITLE_LANGUAGE] = lang }
    suspend fun setSubtitleTextSize(size: Int) = store.edit { it[Keys.SUBTITLE_TEXT_SIZE] = size }
    suspend fun setAutoPlayNextEpisode(enabled: Boolean) = store.edit { it[Keys.AUTO_PLAY_NEXT_EPISODE] = enabled }
    suspend fun setImageCacheSizeGb(gb: Int) = store.edit { it[Keys.IMAGE_CACHE_SIZE_GB] = gb }

    suspend fun clearAll() = store.edit { it.clear() }

    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val REAL_DEBRID_API_KEY = stringPreferencesKey("real_debrid_api_key")
        val OPEN_SUBTITLES_API_KEY = stringPreferencesKey("open_subtitles_api_key")
        val OPEN_SUBTITLES_USERNAME = stringPreferencesKey("open_subtitles_username")
        val OPEN_SUBTITLES_TOKEN = stringPreferencesKey("open_subtitles_token")
        val PRIMARY_AUDIO_LANGUAGE = stringPreferencesKey("primary_audio_language")
        val SECONDARY_AUDIO_LANGUAGE = stringPreferencesKey("secondary_audio_language")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val SUBTITLE_TEXT_SIZE = intPreferencesKey("subtitle_text_size")
        val AUTO_PLAY_NEXT_EPISODE = booleanPreferencesKey("auto_play_next_episode")
        val IMAGE_CACHE_SIZE_GB = intPreferencesKey("image_cache_size_gb")
    }
}
