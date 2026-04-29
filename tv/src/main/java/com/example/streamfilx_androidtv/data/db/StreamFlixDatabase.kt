package com.example.streamfilx_androidtv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.streamfilx_androidtv.data.entities.DownloadEntity
import com.example.streamfilx_androidtv.data.entities.FavoriteEntity
import com.example.streamfilx_androidtv.data.entities.ProfileEntity
import com.example.streamfilx_androidtv.data.entities.WatchHistoryEntity

@Database(
    entities = [ProfileEntity::class, FavoriteEntity::class, WatchHistoryEntity::class, DownloadEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class StreamFlixDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun downloadDao(): DownloadDao
}
