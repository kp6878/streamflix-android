package com.example.streamfilx_androidtv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.streamfilx_androidtv.data.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun getByProfile(profileId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE profileId = :profileId AND contentType = :type ORDER BY addedAt DESC")
    fun getByProfileAndType(profileId: String, type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND contentId = :contentId)")
    suspend fun isFavorite(profileId: String, contentId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND contentId = :contentId")
    suspend fun delete(profileId: String, contentId: String)

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
