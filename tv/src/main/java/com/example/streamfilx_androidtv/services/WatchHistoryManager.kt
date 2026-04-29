package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import com.example.streamfilx_androidtv.core.models.WatchHistoryItem
import com.example.streamfilx_androidtv.data.db.WatchHistoryDao
import com.example.streamfilx_androidtv.data.entities.WatchHistoryEntity
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryManager @Inject constructor(
    private val dao: WatchHistoryDao,
    private val supabaseService: SupabaseService,
    private val appPreferences: AppPreferences,
) {

    // ── Observe ───────────────────────────────────────────────────────────────

    fun observeAll(): Flow<List<WatchHistoryItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeContinueWatching(): Flow<List<WatchHistoryItem>> =
        dao.observeAll().map { entities ->
            buildContinueWatching(entities.map { it.toDomain() })
        }

    suspend fun getProgress(
        contentId: String,
        season: Int?,
        episode: Int?,
    ): WatchHistoryItem? {
        val uniqueId = WatchHistoryItem.buildUniqueId(contentId, season, episode)
        return dao.getByUniqueId(uniqueId)?.toDomain()
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    suspend fun saveProgress(nowPlaying: NowPlayingItem, currentTime: Double, duration: Double) {
        if (currentTime < 1.0 || duration < 1.0) return

        val progress = (currentTime / duration).coerceIn(0.0, 1.0)
        val isCompleted = progress >= 0.90
        val uniqueId = WatchHistoryItem.buildUniqueId(nowPlaying.id, nowPlaying.season, nowPlaying.episode)

        val entity = WatchHistoryEntity(
            uniqueId = uniqueId,
            contentId = nowPlaying.id,
            contentType = nowPlaying.type,
            contentName = nowPlaying.title,
            poster = nowPlaying.posterUrl,
            background = nowPlaying.background,
            thumbnail = null,
            season = nowPlaying.season,
            episode = nowPlaying.episode,
            episodeTitle = nowPlaying.episodeName,
            lastPosition = currentTime,
            duration = duration,
            progress = progress,
            isCompleted = isCompleted,
            lastWatchedAt = System.currentTimeMillis(),
            lastStreamUrl = nowPlaying.streamUrl,
            lastStreamInfoHash = nowPlaying.streamInfoHash,
            lastStreamFileIdx = nowPlaying.streamFileIdx,
        )
        dao.upsert(entity)

        // Fire-and-forget remote sync scoped to the active profile
        val profileId = appPreferences.activeProfileId.first()
        if (profileId != null) {
            runCatching { supabaseService.syncWatchHistoryItem(entity, profileId) }
        }
    }

    suspend fun delete(contentId: String, season: Int?, episode: Int?) {
        val uniqueId = WatchHistoryItem.buildUniqueId(contentId, season, episode)
        dao.delete(uniqueId)
    }

    suspend fun deleteAll() = dao.deleteAll()

    // ── Remote fetch (called on profile selection) ────────────────────────────

    suspend fun loadFromRemote(profileId: String) {
        supabaseService.fetchWatchHistory(profileId).getOrNull()?.forEach { entity ->
            // Only merge if remote item is newer than local
            val local = dao.getByUniqueId(entity.uniqueId)
            if (local == null || entity.lastWatchedAt > local.lastWatchedAt) {
                dao.upsert(entity)
            }
        }
    }

    // ── Continue watching builder ──────────────────────────────────────────────

    private fun buildContinueWatching(items: List<WatchHistoryItem>): List<WatchHistoryItem> {
        val resumable = items.filter { it.shouldResume }
        val latestByContent = mutableMapOf<String, WatchHistoryItem>()
        for (item in resumable) {
            val existing = latestByContent[item.contentId]
            if (existing == null || item.lastWatchedAt > existing.lastWatchedAt) {
                latestByContent[item.contentId] = item
            }
        }
        return latestByContent.values
            .sortedByDescending { it.lastWatchedAt }
            .take(20)
    }
}

// ── Entity → Domain ───────────────────────────────────────────────────────────

private fun WatchHistoryEntity.toDomain() = WatchHistoryItem(
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
    progress = progress,
    isCompleted = isCompleted,
    lastWatchedAt = lastWatchedAt,
    lastStreamUrl = lastStreamUrl,
    lastStreamInfoHash = lastStreamInfoHash,
    lastStreamFileIdx = lastStreamFileIdx,
)
