package com.example.streamfilx_androidtv.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.streamfilx_androidtv.data.entities.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun observeAll(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE contentId = :contentId ORDER BY lastWatchedAt DESC")
    suspend fun getByContentId(contentId: String): List<WatchHistoryEntity>

    @Upsert
    suspend fun upsert(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE uniqueId = :uniqueId")
    suspend fun delete(uniqueId: String)

    @Query("DELETE FROM watch_history")
    suspend fun deleteAll()
}
