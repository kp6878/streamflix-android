# Settings Parity — Complete Settings Screen Specification

Every settings section, field, and interaction from iOS `InAppSettingsView.swift`,
mapped to Android equivalents. The Android planning previously had a partial settings spec;
this document provides the full, exhaustive specification.

---

## Settings Sections (5 total)

| # | Section | Icon | Description |
|---|---------|------|-------------|
| 1 | Accounts | `person` | StreamFlix, Real-Debrid, OpenSubtitles accounts |
| 2 | Playback | `play_circle` | Audio, subtitles, quality, behavior |
| 3 | Storage | `storage` | Cache size, clear cache |
| 4 | Addons | `extension` | Installed addons, add custom |
| 5 | About | `info` | App info, features, factory reset |

### Android Layout

On Android, render as a **scrollable list of NavigationLinks** (like iOS):
- Tap section → navigate to full-screen section detail
- Each section is its own Composable screen

---

## Section 1: Accounts

### StreamFlix Account (Supabase)

```
┌──────────────────────────────────────────────┐
│  👤 StreamFlix Account                        │
│                                               │
│  Your watch history syncs to your StreamFlix  │
│  account so you can pick up where you left    │
│  off on any device.                           │
│                                               │
│  Signed in as                                 │
│  user@email.com              [Sign Out]       │
└──────────────────────────────────────────────┘
```

- Shows current user email from `SupabaseService`
- "Sign Out" button: red tint, calls `supabase.signOut()` → navigates to LoginScreen
- Loading state during sign out

### Real-Debrid

```
┌──────────────────────────────────────────────┐
│  ☁️ Real-Debrid                    (green)    │
│                                               │
│  Real-Debrid provides high-speed cached       │
│  downloads for instant streaming.             │
│                                               │
│  API Key                                      │
│  [••••••••••••••••••••]  [👁]                 │
│                                               │
│  [Validate Key]    ✅ username (Premium - Xd) │
│                                               │
│  🔗 Get your API key from Real-Debrid         │
└──────────────────────────────────────────────┘
┌──────────────────────────────────────────────┐
│  ✅ Real-Debrid Configured                    │  (green accent, only if key set)
│  Torrentio will use Real-Debrid for cached    │
│  torrents                                     │
└──────────────────────────────────────────────┘
```

- **API Key Input:** `SecureField` (password dots) with show/hide toggle
- **Validate Button:** Blue tint, disabled when key empty or validating
- **Validation Result:** Shows username + premium status or "Invalid API key"
- **Help Link:** Opens `https://real-debrid.com/apitoken` in browser

### OpenSubtitles

```
┌──────────────────────────────────────────────┐
│  💬 OpenSubtitles                  (yellow)   │
│                                               │
│  OpenSubtitles provides automatic subtitle    │
│  downloading for movies and TV shows.         │
│                                               │
│  API Key                                      │
│  [••••••••••••••••••••]  [👁]                 │
│                                               │
│  Username (Optional)                          │
│  [________________________]                   │
│                                               │
│  Password (Optional)                          │
│  [••••••••••••••]  [👁]                       │
│  Login for higher download limits (20/day)    │
│                                               │
│  [Validate]    ✅ Logged in as username        │
│                                               │
│  🔗 Get your API key from OpenSubtitles       │
│  👤 Create an OpenSubtitles account            │
└──────────────────────────────────────────────┘
```

- **API Key:** Required. SecureField with toggle.
- **Username/Password:** Optional. Login gives 20 downloads/day vs 5/day.
- **Validate Button:** Yellow tint, validates key or attempts login
- **Help Links:**
  - `https://www.opensubtitles.com/en/consumers` (API key)
  - `https://www.opensubtitles.com/en/users/sign_up` (account creation)

---

## Section 2: Playback

### Video Quality

```
┌──────────────────────────────────────────────┐
│  Video Quality                                │
│                                               │
│  [Auto] [4K] [1080p] [720p]  segmented       │
└──────────────────────────────────────────────┘
```

- Default: "Auto"
- Stored in `AppPreferences.defaultQuality`

### Audio

```
┌──────────────────────────────────────────────┐
│  Audio                                        │
│  Preferred audio track language for videos     │
│  with multiple audio streams                  │
│                                               │
│  Primary Language                             │
│  [🇬🇧 English ▼]  dropdown                   │
│                                               │
│  Secondary Language (Fallback)                │
│  [🇮🇳 Hindi ▼]  dropdown                     │
│  Used when primary language is not available  │
│                                               │
│  🇬🇧 English  →  🇮🇳 Hindi                    │  visual display
└──────────────────────────────────────────────┘
```

- **Primary Language:** Default "en" (English)
- **Secondary Language:** Default "hi" (Hindi), filtered to exclude "Original"
- **Visual Display:** Shows primary → secondary with arrow, both in colored pills
- See `STREAM_PARSING.md` for full language options list

### Behavior

```
┌──────────────────────────────────────────────┐
│  Behavior                                     │
│                                               │
│  Auto-play next episode  [toggle]             │
└──────────────────────────────────────────────┘
```

- Default: ON
- Stored in `@AppStorage("autoPlayNext")`

### Subtitles

```
┌──────────────────────────────────────────────┐
│  Subtitles                                    │
│                                               │
│  Enable Subtitles  [toggle]                   │
│  Subtitles will auto-select when available    │
│                                               │
│  Preferred Language (shown when enabled)      │
│  [🇬🇧 English ▼]  dropdown                   │
│                                               │
│  💬 🇬🇧 English subtitles enabled              │
│                                               │
│  Text Size                                    │
│  [Extra Small] [Small] [Medium] [Large]       │
│  [Extra Large] [Maximum]  selector/slider     │
└──────────────────────────────────────────────┘
```

- **Enable Subtitles:** Default ON. When OFF, all subtitles disabled.
- **Preferred Language:** Default "en". Only shown when subtitles enabled.
- **Text Size:** 6 options (see `PLAYER_PARITY.md` for libVLC fontsize mapping)
- **Status Display:** Shows preferred language with icon when enabled

### Subtitle Text Size Options

| Label | Scale | Stored Value |
|-------|-------|-------------|
| Extra Small | 0.5x | `"extra_small"` |
| Small | 0.75x | `"small"` |
| Medium (default) | 1.0x | `"medium"` |
| Large | 1.25x | `"large"` |
| Extra Large | 1.5x | `"extra_large"` |
| Maximum | 2.0x | `"maximum"` |

---

## Section 3: Storage

### Image Cache Size

```
┌──────────────────────────────────────────────┐
│  💾 Image Cache Size                          │
│                                               │
│  Maximum disk space used for caching poster   │
│  images, thumbnails, and backgrounds.         │
│                                               │
│  [2 GB] [4 GB] [6 GB] [8 GB]  segmented     │
│                                               │
│  📊 Current Usage                             │
│  156.2 MB                    of 2 GB          │
└──────────────────────────────────────────────┘
```

- Default: 2 GB
- Updates Coil disk cache size on change
- Shows current disk usage on appear

### Clear Cache

```
┌──────────────────────────────────────────────┐
│  🗑 Clear Cache                    (orange)    │
│                                               │
│  Remove all cached images and API responses.  │
│  Content will be re-downloaded as needed.     │
│                                               │
│  [Clear All Caches]                           │
└──────────────────────────────────────────────┘
```

- **Button:** Orange tint
- **Action:** Clears Coil memory + disk cache, clears ResponseCache
- Shows "Clearing..." with spinner during operation
- Updates disk usage display after clear

---

## Section 4: Addons

### Installed Addons

```
┌──────────────────────────────────────────────┐
│  Installed Addons                             │
│                                               │
│  ┌─────────────────────────────────────────┐ │
│  │ 🎬 Cinemeta          [Built-in]   🟢   │ │
│  │    Movie & TV metadata from IMDB/TMDB   │ │
│  ├─────────────────────────────────────────┤ │
│  │ ⬇ Torrentio          [Built-in]   🟢   │ │
│  │    Torrent streams with Real-Debrid     │ │
│  ├─────────────────────────────────────────┤ │
│  │ 💾 TorrentsDB         [Built-in]   🟢   │ │
│  │    Alternative torrent streams w/ RD    │ │
│  ├─────────────────────────────────────────┤ │
│  │ 🧩 Custom Addon               🗑   🟢   │ │
│  │    Custom addon description             │ │
│  └─────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

- **Cinemeta:** Always enabled, no remove button, "Built-in" badge
- **Torrentio/TorrentsDB:** Enabled when Real-Debrid key is set, "Built-in" badge
- **Custom addons:** Show name, description, remove button (🗑), enable status dot (🟢/⚫)
- **Status dot:** Green = enabled, gray = disabled

### Add Custom Addon

```
┌──────────────────────────────────────────────┐
│  Add Custom Addon                             │
│                                               │
│  [Addon manifest URL_____________]  [Add]    │
│                                               │
│  Enter the full URL to an addon's manifest   │
│                                               │
│  ❌ Failed to load addon: error message       │  (error)
│  ✅ Added "Addon Name"                        │  (success, auto-dismiss 3s)
└──────────────────────────────────────────────┘
```

- **URL normalization:** Auto-append `/manifest.json` if missing
- **Duplicate check:** Error if addon already installed
- **Manifest fetch:** Uses `StremioClient.fetchManifest()`
- **Success:** Shows addon name, clears input, auto-dismiss after 3s

---

## Section 5: About

### App Info

```
┌──────────────────────────────────────────────┐
│  ▶️ StreamFlix                                │
│  Version 1.0.0                                │
│                                               │
│  A native streaming app with Stremio addon    │
│  support and Real-Debrid integration.         │
└──────────────────────────────────────────────┘
```

### Features List

```
┌──────────────────────────────────────────────┐
│  Features                                     │
│                                               │
│  🎬 Browse movies & TV shows from Cinemeta    │
│  ⚡ Instant playback via Real-Debrid           │
│  🔊 TrueHD & DTS-HD audio support             │
│  💬 Subtitle support                           │
└──────────────────────────────────────────────┘
```

### Danger Zone — Factory Reset

```
┌──────────────────────────────────────────────┐
│  Danger Zone                       (red bg)   │
│                                               │
│  These actions cannot be undone.              │
│                                               │
│  [        Factory Reset        ]  red button  │
│                                               │
│  Removes all profiles, watch history,         │
│  favorites, API keys, and settings.           │
└──────────────────────────────────────────────┘
```

### Factory Reset Action

When confirmed via AlertDialog:

```kotlin
fun performFactoryReset() {
    // 1. Clear DataStore preferences
    dataStore.edit { it.clear() }
    
    // 2. Clear Room database (all tables)
    database.clearAllTables()
    
    // 3. Clear Coil image cache
    imageLoader.memoryCache?.clear()
    imageLoader.diskCache?.clear()
    
    // 4. Clear ResponseCache
    responseCache.removeAll()
    
    // 5. Clear downloaded files
    context.getExternalFilesDir(null)?.deleteRecursively()
    
    // 6. Clear EncryptedSharedPreferences (API keys stored securely)
    // encryptedPrefs.edit { clear() }
    
    // 7. Reset AppState
    appState.resetToDefaults()
    
    // 8. Navigate to LoginScreen
    navController.navigate("login") {
        popUpTo(0) { inclusive = true }
    }
}
```

### Confirmation Dialog

```
Title: "Factory Reset"
Message: "This will delete all your data including profiles, watch history, 
          favorites, downloaded content, API keys, and all settings. 
          This action cannot be undone."
Buttons: [Cancel] [Reset Everything] (destructive)
```

---

## Complete DataStore Preferences Keys

```kotlin
object PreferenceKeys {
    // Auth
    val SUPABASE_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
    val SUPABASE_REFRESH_TOKEN = stringPreferencesKey("supabase_refresh_token")
    val SUPABASE_EXPIRES_AT = longPreferencesKey("supabase_expires_at")
    
    // Profile
    val SELECTED_PROFILE_ID = stringPreferencesKey("selected_profile_id")
    
    // Real-Debrid
    val REAL_DEBRID_API_KEY = stringPreferencesKey("real_debrid_api_key")
    
    // OpenSubtitles
    val OPEN_SUBTITLES_API_KEY = stringPreferencesKey("open_subtitles_api_key")
    val OPEN_SUBTITLES_USERNAME = stringPreferencesKey("open_subtitles_username")
    val OPEN_SUBTITLES_PASSWORD = stringPreferencesKey("open_subtitles_password")
    
    // Playback
    val PREFERRED_AUDIO_LANGUAGE = stringPreferencesKey("preferred_audio_language")       // default: "en"
    val SECONDARY_AUDIO_LANGUAGE = stringPreferencesKey("secondary_audio_language")       // default: "hi"
    val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")                   // default: true
    val PREFERRED_SUBTITLE_LANGUAGE = stringPreferencesKey("preferred_subtitle_language") // default: "en"
    val SUBTITLE_TEXT_SCALE = stringPreferencesKey("subtitle_text_scale")                 // default: "medium"
    val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")                         // default: true
    val DEFAULT_QUALITY = stringPreferencesKey("default_quality")                         // default: "auto"
    
    // Storage
    val MAX_CACHE_SIZE_GB = intPreferencesKey("max_cache_size_gb")                       // default: 2
}
```

---

## Missing from Original Android Planning

The following settings were present in iOS but missing from the original `SCREENS.md`:

1. ✅ **Secondary audio language** — fallback language picker
2. ✅ **Subtitle text size** — 6-step slider/selector
3. ✅ **Subtitles enabled toggle** — global on/off switch
4. ✅ **Subtitle language preference** — language picker when enabled
5. ✅ **Cache size limit** — 2/4/6/8 GB segmented picker
6. ✅ **Current cache usage display** — shows disk bytes used
7. ✅ **Clear cache button** — with spinner during operation
8. ✅ **StreamFlix account section** — email display + sign out
9. ✅ **OpenSubtitles API key field** — separate from username/password
10. ✅ **Factory reset** — with confirmation dialog
11. ✅ **TorrentsDB addon** — second built-in stream addon
12. ✅ **Audio language visual display** — primary → secondary arrow
