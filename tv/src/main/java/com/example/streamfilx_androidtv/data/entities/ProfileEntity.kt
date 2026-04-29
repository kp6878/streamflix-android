package com.example.streamfilx_androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarGradient: String,     // color key: "red", "blue", "green", etc.
    val isKidsProfile: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
