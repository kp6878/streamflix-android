package com.example.streamfilx_androidtv.services

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteWatchHistoryDto(
    @SerialName("unique_id") val uniqueId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("content_id") val contentId: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("content_name") val contentName: String,
    @SerialName("last_position") val lastPosition: Double,
    val duration: Double,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("last_watched_at") val lastWatchedAt: String,
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("episode_title") val episodeTitle: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val thumbnail: String? = null,
    @SerialName("last_stream_url") val lastStreamUrl: String? = null,
    @SerialName("last_stream_info_hash") val lastStreamInfoHash: String? = null,
    @SerialName("last_stream_file_idx") val lastStreamFileIdx: Int? = null,
)

@Serializable
data class RemoteWatchlistDto(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("content_id") val contentId: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("content_name") val contentName: String,
    val poster: String? = null,
    val background: String? = null,
    val year: String? = null,
    @SerialName("imdb_rating") val imdbRating: String? = null,
    val genres: List<String>? = null,
    @SerialName("added_at") val addedAt: String,
)

@Serializable
data class RemoteUserPreferencesDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("real_debrid_api_key") val realDebridApiKey: String? = null,
    @SerialName("preferred_audio_language") val preferredAudioLanguage: String? = null,
    @SerialName("secondary_audio_language") val secondaryAudioLanguage: String? = null,
    @SerialName("subtitles_enabled") val subtitlesEnabled: Boolean? = null,
    @SerialName("preferred_subtitle_language") val preferredSubtitleLanguage: String? = null,
    @SerialName("opensubtitles_api_key") val opensubtitlesApiKey: String? = null,
    @SerialName("opensubtitles_username") val opensubtitlesUsername: String? = null,
    @SerialName("opensubtitles_password") val opensubtitlesPassword: String? = null,
    @SerialName("max_cache_size_gb") val maxCacheSizeGb: Int? = null,
)

@Serializable
data class RemoteProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_id") val profileId: String,
    val name: String,
    @SerialName("avatar_name") val avatarName: String,
    @SerialName("is_kids_profile") val isKidsProfile: Boolean,
    @SerialName("preferred_audio_language") val preferredAudioLanguage: String? = null,
    @SerialName("preferred_subtitle_language") val preferredSubtitleLanguage: String? = null,
    @SerialName("auto_play_next_episode") val autoPlayNextEpisode: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)
