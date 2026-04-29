package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.data.db.ProfileDao
import com.example.streamfilx_androidtv.data.entities.ProfileEntity
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class Profile(
    val id: String,
    val name: String,
    val avatarGradient: String,
    val isKidsProfile: Boolean,
    val createdAt: Long,
) {
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "?"
}

fun ProfileEntity.toProfile() = Profile(
    id = id,
    name = name,
    avatarGradient = avatarGradient,
    isKidsProfile = isKidsProfile,
    createdAt = createdAt,
)

@Singleton
class ProfileManager @Inject constructor(
    private val dao: ProfileDao,
    private val prefs: AppPreferences,
    private val supabaseService: SupabaseService,
) {
    val profiles: Flow<List<Profile>> = dao.getAll().map { list -> list.map { it.toProfile() } }

    val selectedProfileId: Flow<String?> = prefs.activeProfileId

    val selectedProfile: Flow<Profile?> = profiles.map { list ->
        val id = prefs.activeProfileId.let { flow ->
            // Use latest value — resolved at collection time by callers
            null
        }
        list.firstOrNull { it.id == id }
    }

    val canAddMoreProfiles: Flow<Boolean> = profiles.map { it.size < MAX_PROFILES }

    suspend fun addProfile(
        name: String,
        avatarGradient: String,
        isKidsProfile: Boolean,
    ): Profile {
        val entity = ProfileEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            avatarGradient = avatarGradient,
            isKidsProfile = isKidsProfile,
        )
        dao.insert(entity)
        runCatching { supabaseService.syncProfile(entity) }
        return entity.toProfile()
    }

    suspend fun updateProfile(profile: Profile) {
        val entity = ProfileEntity(
            id = profile.id,
            name = profile.name.trim(),
            avatarGradient = profile.avatarGradient,
            isKidsProfile = profile.isKidsProfile,
            createdAt = profile.createdAt,
        )
        dao.update(entity)
        runCatching { supabaseService.syncProfile(entity) }
    }

    suspend fun deleteProfile(id: String) {
        dao.delete(id)
        runCatching { supabaseService.deleteRemoteProfile(id) }
        if (prefs.activeProfileId.let { true }) {
            prefs.setActiveProfileId(null)
        }
    }

    suspend fun getById(id: String): Profile? = dao.getById(id)?.toProfile()

    suspend fun selectProfile(id: String) {
        prefs.setActiveProfileId(id)
    }

    suspend fun clearSelection() {
        prefs.setActiveProfileId(null)
    }

    // ── Remote fetch (called on sign-in) ──────────────────────────────────────

    suspend fun loadFromRemote() {
        supabaseService.fetchProfiles().getOrNull()?.forEach { entity ->
            dao.insert(entity)
        }
    }

    companion object {
        const val MAX_PROFILES = 5
    }
}

// ── Avatar gradient definitions ───────────────────────────────────────────────

object AvatarGradients {
    val options = listOf("red", "blue", "green", "purple", "orange", "pink", "teal", "amber")
    val default = "red"

    data class GradientColors(val start: Long, val end: Long)

    fun colors(key: String): GradientColors = when (key) {
        "red"    -> GradientColors(0xFFE50914L, 0xFF8B0000L)
        "blue"   -> GradientColors(0xFF2196F3L, 0xFF0D47A1L)
        "green"  -> GradientColors(0xFF4CAF50L, 0xFF1B5E20L)
        "purple" -> GradientColors(0xFF9C27B0L, 0xFF4A148CL)
        "orange" -> GradientColors(0xFFFF9800L, 0xFFE65100L)
        "pink"   -> GradientColors(0xFFE91E63L, 0xFF880E4FL)
        "teal"   -> GradientColors(0xFF009688L, 0xFF004D40L)
        "amber"  -> GradientColors(0xFFFFC107L, 0xFFFF6F00L)
        else     -> GradientColors(0xFF616161L, 0xFF212121L)
    }
}
