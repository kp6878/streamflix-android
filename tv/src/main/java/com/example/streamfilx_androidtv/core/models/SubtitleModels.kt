package com.example.streamfilx_androidtv.core.models

import com.google.gson.annotations.SerializedName

// ── OpenSubtitles auth models ─────────────────────────────────────────────────

data class OsLoginRequest(val username: String, val password: String)

data class OsLoginResponse(
    val token: String? = null,
    val status: Int = 0,
    val user: OsUser? = null,
)

data class OsUser(val allowed_downloads: Int = 20)

data class OsDownloadRequest(@SerializedName("file_id") val fileId: Int)

// ── Stremio subtitle response ─────────────────────────────────────────────────

data class SubtitleResponse(val subtitles: List<SubtitleTrack> = emptyList())

data class SubtitleTrack(
    val id: String,
    val url: String,
    val lang: String,
)

// ── OpenSubtitles API models ──────────────────────────────────────────────────

data class OpenSubtitleSearchResponse(
    val data: List<OpenSubtitleItem> = emptyList(),
)

data class OpenSubtitleItem(
    val id: String,
    val attributes: SubtitleAttributes,
)

data class SubtitleAttributes(
    val language: String,
    @SerializedName("download_count") val downloadCount: Int = 0,
    @SerializedName("hearing_impaired") val hearingImpaired: Boolean = false,
    @SerializedName("foreign_parts_only") val foreignPartsOnly: Boolean = false,
    val files: List<SubtitleFile> = emptyList(),
)

data class SubtitleFile(
    @SerializedName("file_id") val fileId: Int,
    @SerializedName("file_name") val fileName: String,
)

data class SubtitleDownloadResponse(
    val link: String,
    val remaining: Int,
    @SerializedName("reset_time_utc") val resetTimeUtc: String? = null,
)

data class SubtitleLanguage(
    @SerializedName("language_code") val languageCode: String,
    @SerializedName("language_name") val languageName: String,
)
