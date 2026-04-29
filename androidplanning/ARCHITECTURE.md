# Android Architecture

## Pattern: MVVM + Clean-ish Layering

Mirrors the iOS MVVM structure exactly. No over-engineering — same layers the iOS app has.

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/streamflix/
│           │
│           ├── StreamFlixApp.kt              ← Application class (Hilt entry point)
│           ├── MainActivity.kt               ← Single activity, hosts NavHost
│           │
│           ├── core/                         ← Shared across features (= StreamFlixCore)
│           │   ├── models/
│           │   │   ├── MetaModels.kt         ← MetaPreview, MetaItem, Video
│           │   │   ├── StreamModels.kt       ← Stream, StreamQuality, AudioLanguage
│           │   │   ├── AddonManifest.kt      ← AddonManifest, Catalog, ContentType
│           │   │   └── SubtitleModels.kt     ← Subtitle, OpenSubtitles models
│           │   ├── network/
│           │   │   ├── StremioClient.kt      ← Stremio protocol API client
│           │   │   ├── ApiClient.kt          ← Generic OkHttp/Retrofit client
│           │   │   └── ResponseCache.kt      ← In-memory response cache with TTL
│           │   └── di/
│           │       └── CoreModule.kt         ← Hilt module for network/core deps
│           │
│           ├── features/
│           │   ├── auth/
│           │   │   ├── LoginScreen.kt        ← Email/password login + signup
│           │   │   └── AuthViewModel.kt
│           │   │
│           │   ├── home/
│           │   │   ├── HomeScreen.kt         ← Hero banner, continue watching, rows
│           │   │   ├── HomeViewModel.kt
│           │   │   ├── ContentRow.kt         ← Reusable horizontal scroll row
│           │   │   └── ContinueWatchingRow.kt
│           │   │
│           │   ├── browse/
│           │   │   ├── BrowseScreen.kt       ← Genre/type filter, infinite scroll grid
│           │   │   └── BrowseViewModel.kt
│           │   │
│           │   ├── search/
│           │   │   ├── SearchScreen.kt       ← 500ms debounce search, results grid
│           │   │   └── SearchViewModel.kt
│           │   │
│           │   ├── detail/
│           │   │   ├── ContentDetailScreen.kt ← Full metadata, episodes, actions
│           │   │   ├── ContentDetailViewModel.kt
│           │   │   └── StreamSelectionSheet.kt ← Bottom sheet stream picker
│           │   │
│           │   ├── library/
│           │   │   ├── LibraryScreen.kt      ← Favorites (All/Movies/Series tabs)
│           │   │   └── LibraryViewModel.kt
│           │   │
│           │   ├── downloads/
│           │   │   ├── DownloadsScreen.kt    ← Download list with status/progress
│           │   │   └── DownloadsViewModel.kt
│           │   │
│           │   ├── player/
│           │   │   ├── PlayerScreen.kt       ← Fullscreen overlay player UI
│           │   │   ├── PlayerViewModel.kt
│           │   │   └── PlayerControls.kt     ← Extracted control composables
│           │   │
│           │   ├── profile/
│           │   │   ├── ProfilePickerScreen.kt
│           │   │   ├── ProfileEditorScreen.kt
│           │   │   └── ProfileViewModel.kt
│           │   │
│           │   └── settings/
│           │       ├── SettingsScreen.kt     ← Accounts, Playback, Addons, Storage
│           │       └── SettingsViewModel.kt
│           │
│           ├── services/
│           │   ├── SupabaseService.kt        ← Auth + watch history sync
│           │   ├── RealDebridService.kt      ← Torrent → URL resolution
│           │   ├── RealDebridApi.kt          ← Retrofit interface
│           │   ├── OpenSubtitlesService.kt   ← Subtitle search + download
│           │   ├── FavoritesManager.kt       ← Profile-scoped favorites
│           │   ├── WatchHistoryManager.kt    ← Progress tracking, debounced save
│           │   ├── ProfileManager.kt         ← Profile CRUD, max 5
│           │   ├── DownloadWorker.kt         ← WorkManager download task
│           │   └── DownloadManager.kt        ← Download coordination
│           │
│           ├── player/
│           │   ├── VideoPlayerController.kt  ← libVLC wrapper + track management
│           │   ├── VlcPlayerWrapper.kt       ← Raw libVLC calls
│           │   └── VideoPlayer.kt            ← Interface (mirrors PlayerProtocol.swift)
│           │
│           ├── data/
│           │   ├── db/
│           │   │   ├── StreamFlixDatabase.kt ← Room database
│           │   │   ├── ProfileDao.kt
│           │   │   ├── FavoritesDao.kt
│           │   │   ├── WatchHistoryDao.kt
│           │   │   └── DownloadDao.kt
│           │   ├── entities/
│           │   │   ├── ProfileEntity.kt
│           │   │   ├── FavoriteEntity.kt
│           │   │   ├── WatchHistoryEntity.kt
│           │   │   └── DownloadEntity.kt
│           │   └── prefs/
│           │       └── AppPreferences.kt     ← DataStore: API keys, profile ID, settings
│           │
│           ├── navigation/
│           │   ├── AppNavigation.kt          ← NavHost with all routes
│           │   └── Screen.kt                 ← Sealed class of route definitions
│           │
│           └── di/
│               ├── AppModule.kt              ← Hilt app-level bindings
│               ├── DatabaseModule.kt         ← Room + DAOs
│               ├── NetworkModule.kt          ← Retrofit, OkHttp, Stremio client
│               └── ServiceModule.kt          ← Managers, services
│
└── build.gradle.kts                          ← Root build file
```

---

## Data Flow (mirrors iOS)

```
Compose Screen
    ↓ observes StateFlow
ViewModel (+ Hilt injection)
    ↓ calls suspend funs
Service / Repository
    ↓ network or DB
StremioClient / Room / DataStore / Supabase
```

---

## State Management

| iOS Pattern | Android Equivalent |
|-------------|-------------------|
| `@Published var items` | `private val _items = MutableStateFlow(...)` |
| `@StateObject var vm` | `val vm: XViewModel = hiltViewModel()` |
| `@EnvironmentObject appState` | Hilt-provided singleton ViewModel |
| `.task { await load() }` | `LaunchedEffect(Unit) { vm.load() }` |
| `withAnimation` | `AnimatedVisibility` / `animateContentSize` |

---

## AppState Equivalent

A single `AppViewModel` (Hilt singleton scoped to the Application) holds global state:

```kotlin
@HiltViewModel
class AppViewModel @Inject constructor(...) : ViewModel() {
    val currentProfile: StateFlow<Profile?>
    val nowPlaying: StateFlow<NowPlayingItem?>
    val isPlaying: StateFlow<Boolean>
    val installedAddons: StateFlow<List<ConfiguredAddon>>
    // settings fields...

    fun startPlayback(item: NowPlayingItem)
    fun stopPlayback()
    fun selectProfile(profileId: UUID)
    fun switchProfile()
}
```

---

## Threading

- All network and DB work runs on `Dispatchers.IO`
- UI state updates collected on `Dispatchers.Main`
- `viewModelScope` for ViewModel-tied coroutines
- `Mutex` for shared mutable state in services (mirrors Swift actors)
