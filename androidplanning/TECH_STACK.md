# Tech Stack — iOS → Android Equivalents

## Language & UI

| iOS | Android |
|-----|---------|
| Swift 5.9+ | Kotlin 1.9+ |
| SwiftUI | Jetpack Compose |
| async/await | Kotlin Coroutines + suspend |
| Actor | Coroutines + Mutex |
| @Published / ObservableObject | StateFlow / ViewModel |
| @StateObject / @EnvironmentObject | Hilt-injected ViewModel |

---

## Networking

| iOS | Android |
|-----|---------|
| URLSession (APIClient.swift) | OkHttp + Retrofit |
| Codable (JSONDecoder) | Gson / Moshi (with Retrofit) |
| Retry logic (exponential backoff) | OkHttp Interceptor |
| ResponseCache (in-memory + TTL) | OkHttp Cache + custom in-memory cache |

---

## Video Playback

| iOS | Android |
|-----|---------|
| VLCKitSPM (VLCPlayerWrapper) | libVLC for Android (AAR via JitPack) |
| VideoPlayerController | AndroidVideoPlayerController (Kotlin wrapper) |
| PlayerProtocol | VideoPlayer interface (Kotlin) |
| External subtitle injection | libVLC `addSlave()` |
| Audio/subtitle track selection | libVLC `AudioTrack` / `SpuTrack` |

> **Why libVLC over ExoPlayer:** VLC handles the same broad format support as VLCKit on iOS. ExoPlayer is HLS/DASH/MP4 only — torrents and unusual codecs from Stremio addons require VLC.

---

## Image Loading

| iOS | Android |
|-----|---------|
| CachedAsyncImage + ImageCacheManager | Coil (AsyncImage in Compose) |
| NSCache (memory, 150MB) | Coil memory cache (default) |
| Disk cache (file-based) | Coil disk cache |

---

## Local Persistence

| iOS | Android |
|-----|---------|
| JSON files (PersistenceManager) | Room Database (SQLite) |
| UserDefaults | Jetpack DataStore (Preferences) |
| Profile-scoped JSON files | Room with profileId foreign key |

---

## Authentication

| iOS | Android |
|-----|---------|
| SupabaseService (custom URLSession) | Supabase Android SDK (`io.github.jan-tennert.supabase`) |
| JWT stored in UserDefaults | JWT stored in DataStore |
| Auto-refresh logic | Supabase SDK handles token refresh |

---

## Dependency Injection

| iOS | Android |
|-----|---------|
| Manual singletons / @EnvironmentObject | Hilt (compile-time DI) |

---

## Background Work

| iOS | Android |
|-----|---------|
| URLSessionDownloadDelegate | WorkManager + DownloadManager (system) |
| Background download sessions | WorkManager `DownloadWorker` |

---

## Navigation

| iOS | Android |
|-----|---------|
| TabView (iOS) | Scaffold + BottomNavigationBar |
| NavigationSplitView (macOS) | (macOS not targeted) |
| NavigationStack / .sheet | Navigation Compose (NavHost) |

---

## Gradle Dependencies (planned)

```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-android-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Image Loading
implementation("io.coil-kt:coil-compose:2.5.0")

// Room (local DB)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Supabase
implementation(platform("io.github.jan-tennert.supabase:bom:2.1.4"))
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")

// libVLC
implementation("org.videolan.android:libvlc-all:3.6.0")

// WorkManager (downloads)
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.1.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```
