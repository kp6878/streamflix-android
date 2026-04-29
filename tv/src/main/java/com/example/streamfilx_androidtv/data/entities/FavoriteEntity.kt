package com.example.streamfilx_androidtv.data.entities

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["profileId", "contentId"],
)
data class FavoriteEntity(
    val profileId: String,
    val contentId: String,
    val contentType: String,            // "movie" or "series"
    val title: String,
    val posterUrl: String?,
    val year: String?,
    val imdbRating: String?,
    val addedAt: Long = System.currentTimeMillis(),
)
