package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.core.models.OpenSubtitleItem
import com.example.streamfilx_androidtv.core.models.OpenSubtitleSearchResponse
import com.example.streamfilx_androidtv.core.models.OsDownloadRequest
import com.example.streamfilx_androidtv.core.models.OsLoginRequest
import com.example.streamfilx_androidtv.core.models.OsLoginResponse
import com.example.streamfilx_androidtv.core.models.SubtitleDownloadResponse
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
private const val USER_AGENT = "StreamFlix Android v1.0"
private const val DOWNLOAD_LIMIT = 20

@Singleton
class OpenSubtitlesService @Inject constructor(
    private val appPreferences: AppPreferences,
    private val okHttpClient: OkHttpClient,
) {
    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()

    // In-memory rate limit counter (resets on app restart; server enforces hard limit)
    private var downloadsUsedThisSession = 0

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = appPreferences.openSubtitlesApiKey.first() ?: ""
            val body = gson.toJson(OsLoginRequest(username, password))
                .toRequestBody(json)
            val request = Request.Builder()
                .url("$BASE_URL/login")
                .post(body)
                .addHeader("Api-Key", apiKey)
                .addHeader("User-Agent", USER_AGENT)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: error("Empty login response")
                if (!response.isSuccessful) error("Login failed: HTTP ${response.code}")
                val parsed = gson.fromJson(responseBody, OsLoginResponse::class.java)
                val token = parsed.token ?: error("No token in login response")
                appPreferences.setOpenSubtitlesCredentials(
                    apiKey = apiKey,
                    username = username,
                    token = token,
                )
                Unit
            }
        }
    }

    suspend fun logout() {
        appPreferences.setOpenSubtitlesCredentials(apiKey = null, username = null, token = null)
    }

    val isLoggedIn: Boolean
        get() = runCatching {
            kotlinx.coroutines.runBlocking { appPreferences.openSubtitlesToken.first() }
        }.getOrNull()?.isNotBlank() == true

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchSubtitles(
        imdbId: String,
        season: Int? = null,
        episode: Int? = null,
        languages: String = "en",
    ): List<OpenSubtitleItem> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = appPreferences.openSubtitlesApiKey.first() ?: return@runCatching emptyList()
            if (apiKey.isBlank()) return@runCatching emptyList()

            val baseImdbId = extractBaseImdbId(imdbId)
            val urlBuilder = StringBuilder("$BASE_URL/subtitles?imdb_id=$baseImdbId&languages=$languages")
            if (season != null) urlBuilder.append("&season_number=$season")
            if (episode != null) urlBuilder.append("&episode_number=$episode")
            if (season == null && episode == null) urlBuilder.append("&type=movie")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .addHeader("Api-Key", apiKey)
                .addHeader("User-Agent", USER_AGENT)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                val body = response.body?.string() ?: return@runCatching emptyList()
                val parsed = gson.fromJson(body, OpenSubtitleSearchResponse::class.java)
                // Filter out hearing-impaired and foreign-only, prefer highest download count
                parsed.data
                    .filter { !it.attributes.hearingImpaired && !it.attributes.foreignPartsOnly }
                    .filter { it.attributes.files.isNotEmpty() }
                    .sortedByDescending { it.attributes.downloadCount }
                    .take(10)
            }
        }.getOrDefault(emptyList())
    }

    // ── Download ──────────────────────────────────────────────────────────────

    suspend fun downloadSubtitle(fileId: Int): String? = withContext(Dispatchers.IO) {
        if (downloadsUsedThisSession >= DOWNLOAD_LIMIT) return@withContext null
        runCatching {
            val apiKey = appPreferences.openSubtitlesApiKey.first() ?: return@runCatching null
            val token = appPreferences.openSubtitlesToken.first() ?: return@runCatching null
            if (apiKey.isBlank() || token.isBlank()) return@runCatching null

            val body = gson.toJson(OsDownloadRequest(fileId)).toRequestBody(json)
            val request = Request.Builder()
                .url("$BASE_URL/download")
                .post(body)
                .addHeader("Api-Key", apiKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("User-Agent", USER_AGENT)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    // Token expired — clear it so next call re-prompts login
                    appPreferences.setOpenSubtitlesCredentials(
                        apiKey = apiKey,
                        username = appPreferences.openSubtitlesUsername.first(),
                        token = null,
                    )
                    return@runCatching null
                }
                if (!response.isSuccessful) return@runCatching null
                val responseBody = response.body?.string() ?: return@runCatching null
                val parsed = gson.fromJson(responseBody, SubtitleDownloadResponse::class.java)
                downloadsUsedThisSession++
                parsed.link.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun extractBaseImdbId(stremioId: String): String =
        stremioId.split(":").firstOrNull { it.startsWith("tt") } ?: stremioId
}
