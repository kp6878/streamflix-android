package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.data.entities.FavoriteEntity
import com.example.streamfilx_androidtv.data.entities.ProfileEntity
import com.example.streamfilx_androidtv.data.entities.WatchHistoryEntity
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseService @Inject constructor(
    private val client: SupabaseClient,
    private val appPreferences: AppPreferences,
) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Unit
            }
        }

    suspend fun signUp(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Unit
            }
        }

    suspend fun signOut(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signOut()
                appPreferences.setActiveProfileId(null)
                Unit
            }
        }

    // supabase-kt 3.x persists sessions internally
    suspend fun hasActiveSession(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.awaitInitialization()
                client.auth.currentSessionOrNull() != null
            }.getOrDefault(false)
        }

    val currentUserEmail: String?
        get() = client.auth.currentUserOrNull()?.email

    private val currentUserId: String?
        get() = client.auth.currentUserOrNull()?.id

    // ── Watch History ─────────────────────────────────────────────────────────

    suspend fun syncWatchHistoryItem(entity: WatchHistoryEntity, profileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("watch_history").upsert(entity.toRemoteDto(profileId)) {
                    onConflict = "profile_id,unique_id"
                }
                Unit
            }
        }

    suspend fun fetchWatchHistory(profileId: String): Result<List<WatchHistoryEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("watch_history")
                    .select {
                        filter { eq("profile_id", profileId) }
                        order("last_watched_at", Order.DESCENDING)
                        limit(200L)
                    }
                    .decodeList<RemoteWatchHistoryDto>()
                    .map { it.toEntity() }
            }
        }

    // ── Watchlist / Favorites ─────────────────────────────────────────────────

    suspend fun syncFavorite(entity: FavoriteEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId ?: error("Not signed in")
                client.from("watchlist").upsert(entity.toRemoteDto(uid)) {
                    onConflict = "user_id,profile_id,content_id"
                }
                Unit
            }
        }

    suspend fun deleteFavorite(profileId: String, contentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("watchlist").delete {
                    filter {
                        eq("profile_id", profileId)
                        eq("content_id", contentId)
                    }
                }
                Unit
            }
        }

    suspend fun fetchFavorites(profileId: String): Result<List<FavoriteEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("watchlist")
                    .select {
                        filter { eq("profile_id", profileId) }
                        order("added_at", Order.DESCENDING)
                        limit(500L)
                    }
                    .decodeList<RemoteWatchlistDto>()
                    .map { it.toEntity() }
            }
        }

    // ── User Preferences ──────────────────────────────────────────────────────

    suspend fun syncUserPreferences(dto: RemoteUserPreferencesDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId ?: error("Not signed in")
                client.from("user_preferences").upsert(dto.copy(userId = uid)) {
                    onConflict = "user_id"
                }
                Unit
            }
        }

    suspend fun fetchUserPreferences(): Result<RemoteUserPreferencesDto?> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("user_preferences")
                    .select()
                    .decodeList<RemoteUserPreferencesDto>()
                    .firstOrNull()
            }
        }

    // ── Profiles ──────────────────────────────────────────────────────────────

    suspend fun syncProfile(entity: ProfileEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId ?: error("Not signed in")
                client.from("profiles").upsert(entity.toRemoteDto(uid)) {
                    onConflict = "user_id,profile_id"
                }
                Unit
            }
        }

    suspend fun deleteRemoteProfile(profileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("profiles").delete {
                    filter { eq("profile_id", profileId) }
                }
                Unit
            }
        }

    suspend fun fetchProfiles(): Result<List<ProfileEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.from("profiles")
                    .select {
                        order("last_used_at", Order.DESCENDING)
                    }
                    .decodeList<RemoteProfileDto>()
                    .map { it.toEntity() }
            }
        }
}

// ── Timestamp helpers ─────────────────────────────────────────────────────────

internal fun epochMsToIso(epochMs: Long): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMs))

internal fun isoToEpochMs(iso: String): Long =
    runCatching { Instant.parse(iso).toEpochMilli() }.getOrDefault(System.currentTimeMillis())

// ── WatchHistoryEntity ↔ DTO ──────────────────────────────────────────────────

internal fun WatchHistoryEntity.toRemoteDto(profileId: String) = RemoteWatchHistoryDto(
    uniqueId = uniqueId,
    profileId = profileId,
    contentId = contentId,
    contentType = contentType,
    contentName = contentName,
    lastPosition = lastPosition,
    duration = duration,
    isCompleted = isCompleted,
    lastWatchedAt = epochMsToIso(lastWatchedAt),
    season = season,
    episode = episode,
    episodeTitle = episodeTitle,
    poster = poster,
    background = background,
    thumbnail = thumbnail,
    lastStreamUrl = lastStreamUrl,
    lastStreamInfoHash = lastStreamInfoHash,
    lastStreamFileIdx = lastStreamFileIdx,
)

internal fun RemoteWatchHistoryDto.toEntity() = WatchHistoryEntity(
    uniqueId = uniqueId,
    contentId = contentId,
    contentType = contentType,
    contentName = contentName,
    poster = poster,
    background = background,
    thumbnail = thumbnail,
    season = season,
    episode = episode,
    episodeTitle = episodeTitle,
    lastPosition = lastPosition,
    duration = duration,
    progress = if (duration > 0) (lastPosition / duration).coerceIn(0.0, 1.0) else 0.0,
    isCompleted = isCompleted,
    lastWatchedAt = isoToEpochMs(lastWatchedAt),
    lastStreamUrl = lastStreamUrl,
    lastStreamInfoHash = lastStreamInfoHash,
    lastStreamFileIdx = lastStreamFileIdx,
)

// ── FavoriteEntity ↔ DTO ──────────────────────────────────────────────────────

internal fun FavoriteEntity.toRemoteDto(userId: String) = RemoteWatchlistDto(
    userId = userId,
    profileId = profileId,
    contentId = contentId,
    contentType = contentType,
    contentName = title,
    poster = posterUrl,
    year = year,
    imdbRating = imdbRating,
    addedAt = epochMsToIso(addedAt),
)

internal fun RemoteWatchlistDto.toEntity() = FavoriteEntity(
    profileId = profileId,
    contentId = contentId,
    contentType = contentType,
    title = contentName,
    posterUrl = poster,
    year = year,
    imdbRating = imdbRating,
    addedAt = isoToEpochMs(addedAt),
)

// ── ProfileEntity ↔ DTO ───────────────────────────────────────────────────────

internal fun ProfileEntity.toRemoteDto(userId: String) = RemoteProfileDto(
    userId = userId,
    profileId = id,
    name = name,
    avatarName = avatarGradient,
    isKidsProfile = isKidsProfile,
    createdAt = epochMsToIso(createdAt),
)

internal fun RemoteProfileDto.toEntity() = ProfileEntity(
    id = profileId,
    name = name,
    avatarGradient = avatarName,
    isKidsProfile = isKidsProfile,
    createdAt = createdAt?.let { isoToEpochMs(it) } ?: System.currentTimeMillis(),
)
