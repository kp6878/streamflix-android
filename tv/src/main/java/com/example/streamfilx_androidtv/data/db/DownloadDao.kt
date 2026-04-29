package com.example.streamfilx_androidtv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.streamfilx_androidtv.data.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE contentId = :contentId")
    suspend fun getByContentId(contentId: String): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, downloaded: Long, total: Long, progress: Float)

    @Query("UPDATE downloads SET status = 'COMPLETED', completedAt = :time WHERE id = :id")
    suspend fun markCompleted(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun deleteAllCompleted()
}
