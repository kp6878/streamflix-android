package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.data.db.FavoritesDao
import com.example.streamfilx_androidtv.data.entities.FavoriteEntity
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class FavoriteItem(
    val profileId: String,
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val year: String?,
    val imdbRating: String?,
    val addedAt: Long,
) {
    fun toMetaPreview() = MetaPreview(
        id = contentId,
        type = contentType,
        name = title,
        poster = posterUrl,
        releaseInfo = year,
        imdbRating = imdbRating,
    )
}

fun FavoriteEntity.toItem() = FavoriteItem(
    profileId = profileId,
    contentId = contentId,
    contentType = contentType,
    title = title,
    posterUrl = posterUrl,
    year = year,
    imdbRating = imdbRating,
    addedAt = addedAt,
)

@Singleton
class FavoritesManager @Inject constructor(
    private val dao: FavoritesDao,
    private val appPreferences: AppPreferences,
    private val supabaseService: SupabaseService,
) {
    // Auto-switches when active profile changes
    val favorites: Flow<List<FavoriteItem>> = appPreferences.activeProfileId
        .flatMapLatest { pid ->
            if (pid != null) dao.getByProfile(pid).map { list -> list.map { it.toItem() } }
            else flowOf(emptyList())
        }

    val movieFavorites: Flow<List<FavoriteItem>> = appPreferences.activeProfileId
        .flatMapLatest { pid ->
            if (pid != null) dao.getByProfileAndType(pid, "movie").map { list -> list.map { it.toItem() } }
            else flowOf(emptyList())
        }

    val seriesFavorites: Flow<List<FavoriteItem>> = appPreferences.activeProfileId
        .flatMapLatest { pid ->
            if (pid != null) dao.getByProfileAndType(pid, "series").map { list -> list.map { it.toItem() } }
            else flowOf(emptyList())
        }

    suspend fun addFavorite(meta: MetaPreview) {
        val pid = appPreferences.activeProfileId.first() ?: return
        val entity = FavoriteEntity(
            profileId = pid,
            contentId = meta.id,
            contentType = meta.type,
            title = meta.name,
            posterUrl = meta.poster,
            year = meta.year,
            imdbRating = meta.imdbRating,
        )
        dao.insert(entity)
        runCatching { supabaseService.syncFavorite(entity) }
    }

    suspend fun removeFavorite(contentId: String) {
        val pid = appPreferences.activeProfileId.first() ?: return
        dao.delete(pid, contentId)
        runCatching { supabaseService.deleteFavorite(pid, contentId) }
    }

    suspend fun isFavorite(contentId: String): Boolean {
        val pid = appPreferences.activeProfileId.first() ?: return false
        return dao.isFavorite(pid, contentId)
    }

    suspend fun toggleFavorite(meta: MetaPreview): Boolean {
        return if (isFavorite(meta.id)) {
            removeFavorite(meta.id)
            false
        } else {
            addFavorite(meta)
            true
        }
    }

    // ── Remote fetch (called on profile selection) ────────────────────────────

    suspend fun loadFromRemote(profileId: String) {
        supabaseService.fetchFavorites(profileId).getOrNull()?.forEach { entity ->
            dao.insert(entity)
        }
    }
}
