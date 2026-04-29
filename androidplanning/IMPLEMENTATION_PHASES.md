# Implementation Phases

Each phase is buildable and testable independently. No iOS files are touched at any point.
All new code lives under `/android/` at the repo root.

---

## Phase 0 — Project Scaffold
**Goal:** Runnable empty Android app with all dependencies wired.

- [ ] Create `/android/` directory with standard Android project structure
- [ ] `build.gradle.kts` (root + app) with all dependencies from `TECH_STACK.md`
- [ ] `AndroidManifest.xml` — permissions: INTERNET, FOREGROUND_SERVICE, WRITE_EXTERNAL_STORAGE
- [ ] `StreamFlixApp.kt` — Application class + Hilt `@HiltAndroidApp`
- [ ] `MainActivity.kt` — single activity, edge-to-edge, dark theme forced
- [ ] `Theme.kt` — dark Material3 theme (Netflix red accent `#E50914`, black background)
- [ ] Hilt modules stubbed out: `AppModule`, `DatabaseModule`, `NetworkModule`, `ServiceModule`
- [ ] Room database created (empty tables)
- [ ] DataStore preferences wired
- [ ] App launches to a blank screen — **build passes**

---

## Phase 1 — Auth
**Goal:** User can sign up, sign in, and the app gates navigation on auth state.

- [ ] `SupabaseService.kt` — signUp, signIn, signOut, restoreSession
- [ ] `AppPreferences.kt` — token storage
- [ ] `AuthViewModel.kt`
- [ ] `LoginScreen.kt` — email/password UI, loading state, error display
- [ ] `AppNavigation.kt` — bootstrap → login → main gate
- [ ] End-to-end test: create account, sign in, see empty home, sign out

---

## Phase 2 — Core Network + Models
**Goal:** Stremio API works, content can be fetched.

- [ ] All data models ported (`MetaModels.kt`, `StreamModels.kt`, `AddonManifest.kt`, `SubtitleModels.kt`)
- [ ] `ApiClient.kt` — OkHttp + Retrofit + retry interceptor
- [ ] `StremioClient.kt` — fetchManifest, fetchCatalog, fetchMeta, fetchStreams, fetchSubtitles
- [ ] `ResponseCache.kt` — in-memory with TTL
- [ ] Unit test: fetch Cinemeta catalog, decode response

---

## Phase 3 — Profiles
**Goal:** Profile picker works, data is profile-scoped.

- [ ] `ProfileEntity.kt` + `ProfileDao.kt`
- [ ] `ProfileManager.kt`
- [ ] `ProfilePickerScreen.kt`
- [ ] `ProfileEditorScreen.kt` (create + edit + delete)
- [ ] Profile selection persisted in DataStore
- [ ] Navigation: login → profile picker → home (empty)

---

## Phase 4 — Home Screen
**Goal:** Real content loads and displays.

- [ ] `HomeViewModel.kt` — loads featured items + catalog rows from Cinemeta
- [ ] `HomeScreen.kt` — hero banner (auto-pager), content rows (LazyRow)
- [ ] `ContentRow.kt` + `ContentCard.kt` — poster image via Coil, rating badge
- [ ] Bottom navigation bar with all 6 tabs (tabs 2–6 show placeholder screens)
- [ ] Pull-to-refresh
- [ ] Tap content card → navigate to ContentDetailScreen (placeholder)

---

## Phase 5 — Browse + Search
**Goal:** User can discover content.

- [ ] `BrowseViewModel.kt` + `BrowseScreen.kt` — genre chips, type toggle, LazyVerticalGrid, pagination
- [ ] `SearchViewModel.kt` + `SearchScreen.kt` — 500ms debounce, results grid, popular searches
- [ ] Shared composable `ContentGrid.kt` (reused by both)

---

## Phase 6 — Content Detail
**Goal:** User can see full metadata for any title.

- [ ] `ContentDetailViewModel.kt` — fetchMeta, manage favorites state
- [ ] `ContentDetailScreen.kt` — background image, metadata, cast, description
- [ ] Episode list + season picker (series)
- [ ] + My List toggle (calls FavoritesManager)
- [ ] `FavoritesManager.kt` + `FavoritesDao.kt` + `FavoriteEntity.kt`
- [ ] `LibraryScreen.kt` — shows favorites with All/Movies/Series filter tabs

---

## Phase 7 — Stream Selection + Real-Debrid
**Goal:** User can pick a stream and resolve it.

- [ ] `RealDebridApi.kt` + `RealDebridService.kt` + `RealDebridModels.kt`
- [ ] `StreamSelectionSheet.kt` — ModalBottomSheet with stream list, quality sort, RD cache badges
- [ ] Stream resolution flow: tap stream → check RD → resolve URL → pass to player
- [ ] RD API key config in SettingsScreen (partial settings screen)

---

## Phase 8 — Video Player
**Goal:** User can watch content.

- [ ] `VlcPlayerWrapper.kt` — libVLC init, SurfaceView attach, play/pause/seek, track enumeration
- [ ] `VideoPlayerController.kt` — StateFlow-based state, progressDelegate
- [ ] `PlayerScreen.kt` — exact iOS layout (see SCREENS.md player section)
  - Top-left info (title, episode, quality badge)
  - Top-right clock + ends-at time
  - Center play/pause
  - Seekable progress bar
  - Bottom toolbar pills (subtitles, audio, volume, settings)
  - Next episode pill
- [ ] Auto-hide controls after 4 seconds
- [ ] Info overlay when paused (synopsis, cast, genres)
- [ ] Skip intro button (series, 5–90s window)
- [ ] Up Next overlay (last 30s or 95%+ progress, 10s countdown)
- [ ] AppViewModel overlay pattern (Box wrapping NavHost)

---

## Phase 9 — Watch History
**Goal:** Progress saves and Continue Watching works.

- [ ] `WatchHistoryEntity.kt` + `WatchHistoryDao.kt`
- [ ] `WatchHistoryManager.kt` — 1s debounced saves, resume threshold, completion tracking
- [ ] `WatchProgressDelegate` implementation in `PlayerViewModel`
- [ ] `ContinueWatchingRow.kt` in HomeScreen — `WatchHistoryCard` with progress bar
- [ ] Supabase sync: `SupabaseService.syncWatchHistory()` wired in `WatchHistoryManager`

---

## Phase 10 — Subtitles
**Goal:** User can load and switch subtitles.

- [ ] `OpenSubtitlesService.kt` — login, search, download, token management, rate limit
- [ ] Subtitle track selection in player bottom toolbar
- [ ] External subtitle injection via `VlcPlayerWrapper.addExternalSubtitle()`
- [ ] OpenSubtitles credentials in SettingsScreen

---

## Phase 11 — Downloads
**Goal:** User can download content for offline viewing.

- [ ] `DownloadEntity.kt` + `DownloadDao.kt`
- [ ] `DownloadWorker.kt` — WorkManager coroutine worker, streams to file, updates progress
- [ ] `DownloadManager.kt` — start/pause/resume/cancel/delete
- [ ] `DownloadsScreen.kt` — progress UI, grouped series, completed section
- [ ] Play downloaded content: `VideoPlayerController.setMedia(localUri)`
- [ ] Download button wired in `ContentDetailScreen`

---

## Phase 12 — Full Settings
**Goal:** All settings functional.

- [ ] `SettingsScreen.kt` — all sections complete (Accounts, Playback, Storage, Addons, About)
- [ ] Addon management: install by URL, list installed, enable/disable, remove
- [ ] Addon persistence: `installedAddons.json` in internal storage (or Room table)
- [ ] Clear cache button
- [ ] Preferred audio language picker
- [ ] Auto-play next episode toggle
- [ ] Logout wired to `SupabaseService.signOut()` → navigates to LoginScreen

---

## Phase 13 — Polish + Testing
**Goal:** App feels complete and stable.

- [ ] Loading shimmer placeholders on all grids/rows
- [ ] Error states with retry buttons
- [ ] Empty states (no favorites, no downloads, no search results)
- [ ] Back gesture handling (Android back button/gesture)
- [ ] Landscape player layout locked (force landscape when player opens)
- [ ] Keyboard handling in search and settings
- [ ] Dark theme throughout (no light mode)
- [ ] Test on Android 8+ (API 26 minimum)
- [ ] Test on tablets (responsive grid columns)

---

## File Creation Order Summary

```
Phase 0:  Scaffold (build files, app class, theme, DI skeleton)
Phase 1:  Auth (Supabase, LoginScreen)
Phase 2:  Models + Network (StremioClient, ApiClient)
Phase 3:  Profiles (ProfileManager, ProfilePicker/Editor screens)
Phase 4:  Home (HomeViewModel, HomeScreen, ContentCard, BottomNav)
Phase 5:  Browse + Search
Phase 6:  Detail + Favorites + Library
Phase 7:  Streams + Real-Debrid + StreamSelectionSheet
Phase 8:  Player (libVLC + PlayerScreen UI)
Phase 9:  Watch History + Continue Watching
Phase 10: Subtitles (OpenSubtitles)
Phase 11: Downloads (WorkManager)
Phase 12: Full Settings
Phase 13: Polish
```

---

## What Is NOT Targeted

- macOS (not applicable on Android)
- Sidebar navigation (Android uses bottom nav)
- Keyboard shortcuts (macOS-only feature)
- `@objc` bridging, Objective-C, or any Swift-specific constructs
