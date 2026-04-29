package com.example.streamfilx_androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val uniqueId: String,     // contentId for movies, contentId:S01E01 for episodes
    val contentId: String,
    val contentType: String,              // "movie" or "series"
    val contentName: String,
    val poster: String?,
    val background: String?,
    val thumbnail: String?,
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,
    val lastPosition: Double,             // seconds
    val duration: Double,
    val progress: Double,                 // 0.0–1.0
    val isCompleted: Boolean,
    val lastWatchedAt: Long,              // epoch ms
    val lastStreamUrl: String?,
    val lastStreamInfoHash: String?,
    val lastStreamFileIdx: Int?,
)
