# Data Models — Complete Room Entity & Model Specifications

Every model, entity, DAO query, and data structure needed for the Android app,
mapped 1-to-1 from the iOS Swift models.

---

## Room Entities

### ProfileEntity

```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,          // UUID string
    val name: String,
    val avatarSymbol: String,            // Material icon name (e.g., "person", "star", "favorite")
    val isKidsProfile: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()  // epoch millis
)
```

### FavoriteEntity

```kotlin
@Entity(
    tableName = "favorites",
    primaryKeys = ["profileId", "contentId"]
)
data class FavoriteEntity(
    val profileId: String,               // FK → ProfileEntity.id
    val contentId: String,               // IMDB ID (e.g., "tt1234567")
    val contentType: String,             // "movie" or "series"
    val title: String,
    val posterUrl: String?,
    val year: String?,
    val imdbRating: String?,
    val addedAt: Long = System.currentTimeMillis()
)
```

### WatchHistoryEntity

```kotlin
@Entity(
    tableName = "watch_history",
    primaryKeys = ["profileId", "uniqueId"]
)
data class WatchHistoryEntity(
    val profileId: String,               // FK → ProfileEntity.id
    val uniqueId: String,                // "tt1234567" or "tt1234567:S01E05"
    val contentId: String,               // Base IMDB ID
    val contentType: String,             // "movie" or "series"
    val contentName: String,             // Series name or movie title
    val poster: String?,                 // Poster URL
    val background: String?,             // Background image URL
    val thumbnail: String?,              // Episode thumbnail URL

    // Episode info (null for movies)
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,

    // Progress
    val lastPosition: Double,            // seconds
    val duration: Double,                // seconds
    val progress: Double,                // 0.0–1.0
    val isCompleted: Boolean = false,
    val lastWatchedAt: Long = System.currentTimeMillis(),

    // Stream resume info (for instant resume)
    val lastStreamUrl: String?,          // Resolved stream URL
    val lastStreamInfoHash: String?,     // Torrent info hash
    val lastStreamFileIdx: Int?          // File index in torrent
)
```

### DownloadEntity

```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,          // UUID string
    val profileId: String,               // FK → ProfileEntity.id
    val contentId: String,               // IMDB ID
    val contentType: String,             // "movie" or "series"
    val title: String,                   // Movie title or series name
    val posterUrl: String?,

    // Episode info (null for movies)
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,

    // Download info
    val sourceUrl: String,               // Original stream URL
    val localFilePath: String?,          // Path to downloaded file
    val fileSize: Long = 0,              // Total size in bytes
    val downloadedBytes: Long = 0,       // Downloaded so far
    val progress: Double = 0.0,          // 0.0–1.0
    val status: String = "pending",      // pending, downloading, paused, completed, failed
    val errorMessage: String? = null,

    // Quality metadata
    val quality: String?,                // "4K", "1080p", "720p", etc.
    val audioCodec: String?,             // "TrueHD Atmos", "DTS-HD MA", etc.

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
```

### InstalledAddonEntity (optional — can also use JSON file)

```kotlin
@Entity(tableName = "installed_addons")
data class InstalledAddonEntity(
    @PrimaryKey val id: String,          // UUID string
    val manifestUrl: String,
    val baseUrl: String,
    val manifestJson: String?,           // Cached manifest JSON
    val name: String?,
    val description: String?,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
```

---

## DAO Interfaces

### ProfileDao

```kotlin
@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)

    // Cascade delete profile data
    @Transaction
    suspend fun deleteWithData(id: String) {
        delete(id)
        // Also called: favoritesDao.deleteByProfile(id), watchHistoryDao.deleteByProfile(id)
    }
}
```

### FavoritesDao

```kotlin
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
```

### WatchHistoryDao

```kotlin
@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    fun getByProfile(profileId: String): Flow<List<WatchHistoryEntity>>

    @Query("""
        SELECT * FROM watch_history 
        WHERE profileId = :profileId 
        AND isCompleted = 0 
        AND progress > 0.02 AND progress < 0.90
        ORDER BY lastWatchedAt DESC
        LIMIT 20
    """)
    fun getContinueWatching(profileId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId AND uniqueId = :uniqueId")
    suspend fun getItem(profileId: String, uniqueId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId AND contentId = :contentId ORDER BY lastWatchedAt DESC")
    suspend fun getByContentId(profileId: String, contentId: String): List<WatchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchHistoryEntity)

    @Query("UPDATE watch_history SET isCompleted = 1, lastWatchedAt = :now WHERE profileId = :profileId AND uniqueId = :uniqueId")
    suspend fun markAsCompleted(profileId: String, uniqueId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE watch_history SET isCompleted = 0, lastPosition = 0, lastWatchedAt = :now WHERE profileId = :profileId AND uniqueId = :uniqueId")
    suspend fun markAsUnwatched(profileId: String, uniqueId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM watch_history WHERE profileId = :profileId AND uniqueId = :uniqueId")
    suspend fun delete(profileId: String, uniqueId: String)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    // Series helpers
    @Query("""
        SELECT * FROM watch_history 
        WHERE profileId = :profileId AND contentId = :seriesId AND isCompleted = 1
    """)
    suspend fun getWatchedEpisodes(profileId: String, seriesId: String): List<WatchHistoryEntity>

    @Query("""
        SELECT COUNT(*) FROM watch_history 
        WHERE profileId = :profileId AND contentId = :seriesId AND isCompleted = 1
    """)
    suspend fun getWatchedEpisodeCount(profileId: String, seriesId: String): Int
}
```

### DownloadDao

```kotlin
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getByProfile(profileId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE profileId = :profileId AND status = :status ORDER BY createdAt DESC")
    fun getByStatus(profileId: String, status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE profileId = :profileId AND contentId = :contentId AND season IS :season AND episode IS :episode")
    suspend fun findDownload(profileId: String, contentId: String, season: Int?, episode: Int?): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE profileId = :profileId AND contentId = :contentId AND season IS :season AND episode IS :episode AND status = 'completed')")
    suspend fun isDownloaded(profileId: String, contentId: String, season: Int?, episode: Int?): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :bytes, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, bytes: Long, progress: Double)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM downloads WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    @Query("SELECT SUM(fileSize) FROM downloads WHERE profileId = :profileId AND status = 'completed'")
    suspend fun getTotalDownloadSize(profileId: String): Long?
}
```

---

## Domain Models (non-Room)

### WatchHistoryItem (domain model wrapping entity)

```kotlin
data class WatchHistoryItem(
    val uniqueId: String,
    val contentId: String,
    val contentType: String,
    val contentName: String,
    val poster: String?,
    val background: String?,
    val thumbnail: String?,
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,
    val lastPosition: Double,
    val duration: Double,
    val progress: Double,
    val isCompleted: Boolean,
    val lastWatchedAt: Long,
    val lastStreamUrl: String?,
    val lastStreamInfoHash: String?,
    val lastStreamFileIdx: Int?
) {
    /** 2%–90% progress → should show resume */
    val shouldResume: Boolean
        get() = !isCompleted && progress > 0.02 && progress < 0.90

    /** Display-friendly time remaining */
    val timeRemaining: String
        get() {
            val remaining = ((duration - lastPosition) / 60).toInt()
            return if (remaining > 0) "$remaining min left" else ""
        }

    /** Episode display string */
    val episodeInfo: String?
        get() {
            val s = season ?: return null
            val e = episode ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    companion object {
        /** Build unique ID: "tt1234567" for movies, "tt1234567:S01E05" for episodes */
        fun buildUniqueId(contentId: String, season: Int?, episode: Int?): String {
            return if (season != null && episode != null) {
                "$contentId:S${String.format("%02d", season)}E${String.format("%02d", episode)}"
            } else {
                contentId
            }
        }

        fun forMovie(
            contentId: String, name: String, poster: String?, background: String?,
            position: Double, duration: Double,
            streamUrl: String?, streamInfoHash: String?, streamFileIdx: Int?
        ): WatchHistoryItem { /* ... build with uniqueId = contentId */ }

        fun forEpisode(
            seriesId: String, seriesName: String,
            season: Int, episode: Int, episodeId: String, episodeTitle: String?,
            poster: String?, thumbnail: String?,
            position: Double, duration: Double,
            streamUrl: String?, streamInfoHash: String?, streamFileIdx: Int?
        ): WatchHistoryItem { /* ... build with uniqueId = "$seriesId:S{season}E{episode}" */ }
    }
}
```

### NowPlayingItem

```kotlin
data class NowPlayingItem(
    val id: String,                      // IMDB ID
    val type: String,                    // "movie" or "series"
    val title: String,
    val streamUrl: String,               // Resolved playback URL
    val posterUrl: String?,
    val season: Int?,
    val episode: Int?,
    val episodeName: String?,

    // Rich metadata
    val year: String?,
    val genres: List<String>?,
    val imdbRating: String?,
    val runtime: String?,
    val contentDescription: String?,
    val cast: List<String>?,
    val logo: String?,
    val episodeOverview: String?,
    val streamQuality: String?,
    val background: String?,

    // Stream source info (for re-resolution)
    var streamInfoHash: String? = null,
    var streamFileIdx: Int? = null,
    var streamSourceUrl: String? = null,
    var streamName: String? = null,

    // Next episode info
    var nextEpisodeId: String? = null,
    var nextEpisodeSeason: Int? = null,
    var nextEpisodeNumber: Int? = null,
    var nextEpisodeName: String? = null,

    // All episodes for episode picker
    var allEpisodes: List<EpisodeInfo>? = null
) {
    val episodeInfo: String?
        get() {
            val s = season ?: return null
            val e = episode ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    val playerDisplayTitle: String
        get() = if (type == "series" && episodeInfo != null) {
            val parts = mutableListOf(title, episodeInfo!!)
            episodeName?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
            parts.joinToString(" • ")
        } else title

    val hasNextEpisode: Boolean get() = nextEpisodeId != null

    val nextEpisodeInfo: String?
        get() {
            val s = nextEpisodeSeason ?: return null
            val e = nextEpisodeNumber ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    val qualityBadge: String?
        get() {
            val q = streamQuality?.uppercase() ?: return null
            return when {
                q.contains("2160") || q.contains("4K") || q.contains("UHD") -> "4K"
                q.contains("1080") || q.contains("FHD") -> "FHD"
                q.contains("720") || q.contains("HD") -> "HD"
                q.contains("480") || q.contains("SD") -> "SD"
                else -> q
            }
        }

    val ratingPercentage: Int?
        get() = imdbRating?.toDoubleOrNull()?.let { (it * 10).toInt() }

    val availableSeasons: List<Int>
        get() = allEpisodes
            ?.map { it.season }
            ?.distinct()
            ?.sortedWith(compareBy { if (it == 0) Int.MAX_VALUE else it })
            ?: emptyList()

    fun episodesForSeason(season: Int): List<EpisodeInfo> =
        allEpisodes?.filter { it.season == season }?.sortedBy { it.episode } ?: emptyList()

    fun isCurrentEpisode(ep: EpisodeInfo): Boolean =
        ep.season == season && ep.episode == episode
}
```

### EpisodeInfo

```kotlin
data class EpisodeInfo(
    val id: String,           // Episode ID for stream fetching (e.g., "tt1234567:1:1")
    val season: Int,
    val episode: Int,
    val title: String?,
    val thumbnail: String?,
    val overview: String?
) {
    val episodeString: String get() = String.format("S%02dE%02d", season, episode)
    val displayTitle: String get() = title?.takeIf { it.isNotEmpty() } ?: "Episode $episode"
}
```

### PlaybackContentInfo

```kotlin
data class PlaybackContentInfo(
    val contentId: String,
    val contentType: String,
    val contentName: String,
    val poster: String?,
    val background: String?,
    val season: Int?,
    val episode: Int?,
    val episodeId: String?,
    val episodeTitle: String?,
    val thumbnail: String? = null
)
```

### ConfiguredAddon

```kotlin
data class ConfiguredAddon(
    val id: String = UUID.randomUUID().toString(),
    val manifestUrl: String,
    val baseUrl: String,
    val manifest: AddonManifest? = null,
    val isEnabled: Boolean = true
) {
    fun supports(resource: String): Boolean =
        manifest?.resources?.any { it.equals(resource, ignoreCase = true) } == true
}
```

---

## Stremio API Models

These models decode JSON responses from the Stremio addon protocol.

### AddonManifest

```kotlin
data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val resources: List<String>,       // ["catalog", "meta", "stream", "subtitles"]
    val types: List<String>,           // ["movie", "series"]
    val catalogs: List<Catalog>? = null,
    val idPrefixes: List<String>? = null,
    val behaviorHints: BehaviorHints? = null
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<ExtraDefinition>? = null
)

data class ExtraDefinition(
    val name: String,
    val isRequired: Boolean? = null,
    val options: List<String>? = null
)

data class BehaviorHints(
    val adult: Boolean? = null,
    val p2p: Boolean? = null,
    val configurable: Boolean? = null,
    val configurationRequired: Boolean? = null
)
```

### MetaModels

```kotlin
data class CatalogResponse(val metas: List<MetaPreview>)
data class MetaResponse(val meta: MetaItem)

data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val posterShape: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null
)

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String>? = null,
    val cast: List<String>? = null,
    val director: List<String>? = null,
    val videos: List<Video>? = null,
    val links: List<MetaLink>? = null
)

data class Video(
    val id: String,
    val title: String? = null,
    val season: Int? = null,
    @SerializedName("episode") val episodeNumber: Int? = null,
    val released: String? = null,
    val overview: String? = null,
    val thumbnail: String? = null
)

data class MetaLink(
    val name: String? = null,
    val category: String? = null,
    val url: String? = null
)
```

### StreamModels

```kotlin
data class StreamResponse(val streams: List<Stream>)

data class Stream(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val behaviorHints: StreamBehaviorHints? = null
)

data class StreamBehaviorHints(
    val countryWhitelist: List<String>? = null,
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val proxyHeaders: ProxyHeaders? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val videoHash: String? = null
)

data class ProxyHeaders(
    val request: Map<String, String>? = null,
    val response: Map<String, String>? = null
)
```

### SubtitleModels

```kotlin
data class SubtitleResponse(val subtitles: List<SubtitleTrack>)

data class SubtitleTrack(
    val id: String,
    val url: String,
    val lang: String
)
```

---

## Real-Debrid Models

```kotlin
data class RDUser(
    val id: Int,
    val username: String,
    val email: String,
    val type: String,         // "premium" or "free"
    val premium: Int,         // Premium time left in seconds
    val expiration: String,   // ISO date string
    val points: Int? = null,
    val locale: String? = null,
    val avatar: String? = null
) {
    val isPremium: Boolean get() = type == "premium"

    /** Parse expiration ISO date and compute days remaining */
    val premiumDaysRemaining: Int?
        get() {
            if (!isPremium) return null
            return try {
                val expDate = java.time.Instant.parse(expiration)
                val now = java.time.Instant.now()
                java.time.Duration.between(now, expDate).toDays().toInt()
            } catch (e: Exception) { null }
        }
}

data class RDDownload(
    val id: String,
    val filename: String,
    val mimeType: String? = null,
    val filesize: Long,
    val link: String,
    val host: String,
    val download: String,     // ✅ Direct download URL — use this for playback
    val streamable: Int? = null,
    val chunks: Int? = null,
    val crc: Int? = null,
    val type: String? = null
) {
    val isStreamable: Boolean get() = streamable == 1

    val formattedFilesize: String
        get() {
            val gb = filesize.toDouble() / 1_073_741_824
            return if (gb >= 1) String.format("%.2f GB", gb)
                   else String.format("%.0f MB", filesize.toDouble() / 1_048_576)
        }
}

data class RDTorrent(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val host: String,
    val status: String,
    val progress: Int,
    val links: List<String>,
    val files: List<RDFile>?,
    val added: String? = null,
    val ended: String? = null,
    val speed: Int? = null,
    val seeders: Int? = null,
    @SerializedName("original_filename") val originalFilename: String? = null
) {
    val isReady: Boolean get() = status == "downloaded"

    val isProcessing: Boolean
        get() = status in listOf("magnet_conversion", "downloading", "compressing", "uploading")

    val statusDescription: String
        get() = when (status) {
            "magnet_error" -> "Magnet Error"
            "magnet_conversion" -> "Converting Magnet..."
            "waiting_files_selection" -> "Waiting for File Selection"
            "queued" -> "Queued"
            "downloading" -> "Downloading ($progress%)"
            "downloaded" -> "Downloaded"
            "error" -> "Error"
            "virus" -> "Virus Detected"
            "compressing" -> "Compressing..."
            "uploading" -> "Uploading..."
            "dead" -> "Dead Torrent"
            else -> status.replaceFirstChar { it.uppercase() }
        }

    val formattedSize: String
        get() {
            val gb = bytes.toDouble() / 1_073_741_824
            return if (gb >= 1) String.format("%.2f GB", gb)
                   else String.format("%.0f MB", bytes.toDouble() / 1_048_576)
        }
}

data class RDFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int
) {
    val isSelected: Boolean get() = selected == 1
    val filename: String get() = path.substringAfterLast('/')
}

data class RDTorrentAdded(
    val id: String,
    val uri: String
)
```

---

## OpenSubtitles Models

```kotlin
data class OpenSubtitleSearchResponse(
    val data: List<OpenSubtitleItem>
)

data class OpenSubtitleItem(
    val id: String,
    val attributes: SubtitleAttributes
)

data class SubtitleAttributes(
    val language: String,
    @SerializedName("download_count") val downloadCount: Int,
    @SerializedName("hearing_impaired") val hearingImpaired: Boolean,
    @SerializedName("foreign_parts_only") val foreignPartsOnly: Boolean,
    val files: List<SubtitleFile>
)

data class SubtitleFile(
    @SerializedName("file_id") val fileId: Int,
    @SerializedName("file_name") val fileName: String
)

data class SubtitleDownloadResponse(
    val link: String,
    val remaining: Int,
    @SerializedName("reset_time_utc") val resetTimeUtc: String?
)

data class SubtitleLanguage(
    @SerializedName("language_code") val languageCode: String,
    @SerializedName("language_name") val languageName: String
)
```

---

## Supabase Watch History Schema

Table: `public.watch_history`

| Column | Type | Notes |
|--------|------|-------|
| id | uuid | PK, auto-generated |
| user_id | uuid | FK → auth.users, RLS policy |
| profile_id | text | Profile UUID string |
| unique_id | text | "tt1234567" or "tt1234567:S01E05" |
| content_id | text | Base IMDB ID |
| content_type | text | "movie" or "series" |
| content_name | text | Title |
| poster | text | nullable |
| background | text | nullable |
| thumbnail | text | nullable |
| season | int | nullable |
| episode | int | nullable |
| episode_title | text | nullable |
| last_position | float8 | seconds |
| duration | float8 | seconds |
| progress | float8 | 0.0–1.0 |
| is_completed | boolean | |
| last_watched_at | timestamptz | |
| last_stream_url | text | nullable |
| last_stream_info_hash | text | nullable |
| last_stream_file_idx | int | nullable |

**RLS Policy:** `user_id = auth.uid()` for all operations.
**Unique constraint:** `(user_id, profile_id, unique_id)` — upsert on conflict.
