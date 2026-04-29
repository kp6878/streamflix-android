# Services & Integrations

All services mirror their iOS counterparts 1-to-1. Same APIs, same logic, new language.

---

## StremioClient (`core/network/StremioClient.kt`)
**iOS:** `StremioClient.swift` (actor)

```kotlin
class StremioClient @Inject constructor(private val apiClient: ApiClient) {
    suspend fun fetchManifest(baseUrl: String): AddonManifest
    suspend fun fetchCatalog(baseUrl: String, type: String, id: String, extra: Map<String, String>? = null): CatalogResponse
    suspend fun searchCatalog(baseUrl: String, type: String, id: String, query: String): CatalogResponse
    suspend fun fetchMeta(baseUrl: String, type: String, id: String): MetaResponse
    suspend fun fetchStreams(baseUrl: String, type: String, id: String): StreamResponse
    suspend fun fetchSubtitles(baseUrl: String, type: String, id: String): SubtitleResponse
}
```

**Well-Known Addons:**
- `WellKnownAddon.CINEMETA = "https://v3-cinemeta.strem.io"`
- `WellKnownAddon.OPENSUBTITLES = "https://opensubtitles-v3.strem.io"`

**Response Cache** (`core/network/ResponseCache.kt`):
- Manifest: 7 days TTL
- Catalog: 24 hours TTL
- Search: 1 hour TTL
- Implemented with `HashMap` + timestamps + `Mutex` for thread safety

---

## ApiClient (`core/network/ApiClient.kt`)
**iOS:** `APIClient.swift`

- OkHttp + Retrofit base
- Retry interceptor: 3 retries on 502, 503, 504, 429, and `IOException`
- Exponential backoff: 1s, 2s, 4s
- JSON via Gson converter

```kotlin
enum class ApiError {
    INVALID_RESPONSE, UNAUTHORIZED, FORBIDDEN, NOT_FOUND,
    RATE_LIMITED, SERVER_ERROR, DECODING_ERROR, NETWORK_ERROR
}
```

---

## SupabaseService (`services/SupabaseService.kt`)
**iOS:** `SupabaseService.swift`

Uses the official Supabase Kotlin SDK.

```kotlin
class SupabaseService @Inject constructor(private val prefs: AppPreferences) {
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun restoreSession(): Boolean           // On app start
    fun isSignedIn(): Flow<Boolean>
    suspend fun syncWatchHistory(item: WatchHistoryItem)  // Fire-and-forget
}
```

**Session storage:** JWT access token, refresh token, expiry in DataStore (mirrors UserDefaults).
**Watch history table:** `public.watch_history` — same schema as iOS, keyed by `user_id = auth.uid()`.

---

## RealDebridService (`services/RealDebridService.kt`)
**iOS:** `RealDebridService.swift`

Same REST API, same flow:

```kotlin
class RealDebridService @Inject constructor(private val api: RealDebridApi, private val prefs: AppPreferences) {
    suspend fun checkInstantAvailability(infoHashes: List<String>): Map<String, Boolean>
    suspend fun resolveStream(stream: Stream, fileIndex: Int?): URL
    // Internal: addMagnet → selectFiles → getTorrentInfo → unrestrictLink
}
```

**RealDebridApi** (Retrofit interface):
```kotlin
interface RealDebridApi {
    @GET("torrents/instantAvailability/{hash}")
    suspend fun checkInstantAvailability(@Path("hash") hash: String): JsonObject

    @POST("torrents/addMagnet")
    suspend fun addMagnet(@Body body: FormBody): RDTorrentAdded

    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(@Path("id") id: String, @Body body: FormBody)

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(@Path("id") id: String): TorrentInfo

    @POST("unrestrict/link")
    suspend fun unrestrictLink(@Body body: FormBody): UnrestrictedLink
}
```

**Base URL:** `https://api.real-debrid.com/rest/1.0/`
**Auth header:** `Authorization: Bearer {apiKey}` injected via OkHttp interceptor.

---

## OpenSubtitlesService (`services/OpenSubtitlesService.kt`)
**iOS:** `OpenSubtitlesService.swift`

```kotlin
class OpenSubtitlesService @Inject constructor(private val prefs: AppPreferences) {
    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun searchSubtitles(imdbId: String, season: Int?, episode: Int?, language: String = "en"): List<OpenSubtitleItem>
    suspend fun downloadSubtitle(fileId: Int): URL
    // Token refresh: 24-hour expiry, auto-refresh on 401
    // Rate limit: 20 downloads per 24 hours (tracked locally)
}
```

---

## WatchHistoryManager (`services/WatchHistoryManager.kt`)
**iOS:** `WatchHistoryManager.swift`

```kotlin
@Singleton
class WatchHistoryManager @Inject constructor(
    private val dao: WatchHistoryDao,
    private val supabase: SupabaseService
) {
    val items: Flow<List<WatchHistoryItem>>          // From Room

    suspend fun updateProgress(uniqueId: String, progress: Double, position: Double, duration: Double)
    suspend fun getProgress(uniqueId: String): WatchHistoryItem?
    suspend fun markAsCompleted(uniqueId: String)
    suspend fun removeItem(uniqueId: String)
    fun switchProfile(profileId: UUID)

    // UniqueId format: "tt1234567" (movie) or "tt1234567:S01E05" (episode)
    companion object {
        fun uniqueId(contentId: String, season: Int?, episode: Int?): String
    }
}
```

**Debounce:** 1-second debounce on `updateProgress` (using `Flow.debounce(1000)`).
**Resume threshold:** 2%–90% progress → shows "Resume" button.
**Supabase sync:** fire-and-forget on `viewModelScope`, no blocking.

---

## FavoritesManager (`services/FavoritesManager.kt`)
**iOS:** `FavoritesManager.swift`

```kotlin
@Singleton
class FavoritesManager @Inject constructor(private val dao: FavoritesDao) {
    val favorites: Flow<List<FavoriteItem>>

    suspend fun addFavorite(item: MetaPreview)
    suspend fun removeFavorite(itemId: String)
    suspend fun isFavorite(itemId: String): Boolean
    fun switchProfile(profileId: UUID)
}
```

---

## ProfileManager (`services/ProfileManager.kt`)
**iOS:** `ProfileManager.swift`

```kotlin
@Singleton
class ProfileManager @Inject constructor(
    private val dao: ProfileDao,
    private val prefs: AppPreferences
) {
    val profiles: Flow<List<Profile>>
    val selectedProfileId: Flow<UUID?>
    val selectedProfile: Flow<Profile?>

    val canAddMoreProfiles: Boolean  // max 5

    suspend fun addProfile(name: String, avatarName: String, isKidsProfile: Boolean)
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(id: UUID)
    suspend fun selectProfile(id: UUID)
}
```

---

## DownloadManager (`services/DownloadManager.kt`)
**iOS:** `DownloadManager.swift`

Uses Android's `WorkManager` for background reliability.

```kotlin
@Singleton
class DownloadManager @Inject constructor(
    private val workManager: WorkManager,
    private val dao: DownloadDao
) {
    val downloads: Flow<List<DownloadItem>>

    fun startDownload(url: String, filename: String, contentId: String, metadata: DownloadMetadata?)
    suspend fun pauseDownload(id: UUID)
    suspend fun resumeDownload(id: UUID)
    suspend fun cancelDownload(id: UUID)
    suspend fun deleteDownload(id: UUID)     // Deletes file + DB record

    fun getDownloadedFileUrl(contentId: String): Uri?
    fun isDownloaded(contentId: String): Boolean
}
```

**DownloadWorker** (`services/DownloadWorker.kt`):
- `CoroutineWorker` with `Dispatchers.IO`
- Streams response body to file in `getExternalFilesDir()`
- Updates `DownloadDao` progress every 512KB
- Handles pause via `WorkManager` cancellation + resume data saved

---

## VideoPlayerController (`player/VideoPlayerController.kt`)
**iOS:** `VideoPlayerController.swift`

```kotlin
class VideoPlayerController @Inject constructor() {
    // State
    val isPlaying: StateFlow<Boolean>
    val currentTime: StateFlow<Double>       // seconds
    val duration: StateFlow<Double>
    val progress: StateFlow<Double>          // 0.0 to 1.0
    val isBuffering: StateFlow<Boolean>
    val audioTracks: StateFlow<List<AudioTrack>>
    val subtitleTracks: StateFlow<List<SubtitleTrack>>
    val currentAudioTrackIndex: StateFlow<Int>
    val currentSubtitleTrackIndex: StateFlow<Int>

    // Delegate
    var progressDelegate: WatchProgressDelegate?

    // Control
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(seconds: Double)
    fun seekToProgress(progress: Double)    // 0.0 to 1.0
    fun skipForward(seconds: Double = 10.0)
    fun skipBackward(seconds: Double = 10.0)
    fun stop()

    // Tracks
    fun setAudioTrack(index: Int)
    fun setSubtitleTrack(index: Int)
    fun addExternalSubtitle(uri: Uri)

    // Setup
    fun setMedia(url: String)
    fun attachToView(surfaceView: SurfaceView)
    fun release()
}
```

---

## ImageCacheManager (`services/ImageCacheManager.kt`)
**iOS:** `ImageCacheManager.swift`

Not needed as a custom class — **Coil** handles memory + disk caching automatically.
Configure globally in `StreamFlixApp.kt`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
    .diskCache { DiskCache.Builder().directory(context.cacheDir.resolve("image_cache")).maxSizeBytes(150L * 1024 * 1024).build() }
    .build()
Coil.setImageLoader(imageLoader)
```

---

## AppPreferences (`data/prefs/AppPreferences.kt`)
**iOS:** UserDefaults keys

DataStore-backed preferences:

```kotlin
class AppPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {
    // Keys
    val realDebridApiKey: Flow<String?>
    val openSubtitlesUsername: Flow<String?>
    val openSubtitlesPassword: Flow<String?>
    val selectedProfileId: Flow<String?>
    val preferredAudioLanguage: Flow<String>
    val autoPlayNext: Flow<Boolean>
    val defaultQuality: Flow<String>
    val supabaseAccessToken: Flow<String?>
    val supabaseRefreshToken: Flow<String?>
    val supabaseExpiresAt: Flow<Long?>

    // Setters
    suspend fun setRealDebridApiKey(key: String?)
    suspend fun setSelectedProfileId(id: String?)
    // ... etc
}
```

---

## Room Database (`data/db/StreamFlixDatabase.kt`)

```kotlin
@Database(
    entities = [ProfileEntity::class, FavoriteEntity::class, WatchHistoryEntity::class, DownloadEntity::class],
    version = 1
)
abstract class StreamFlixDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun downloadDao(): DownloadDao
}
```

Profile-scoping: all tables have a `profileId: String` column. DAOs filter by current `profileId`.
