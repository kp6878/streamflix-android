# Screens — 1-to-1 iOS Parity

Every screen below maps exactly to its iOS counterpart. Same layout, same data, same interactions.

---

## Auth

### LoginScreen (`features/auth/LoginScreen.kt`)
**iOS:** `LoginView.swift`

- Email + password fields
- Toggle between Sign In / Sign Up
- Calls `AuthViewModel.signIn()` / `AuthViewModel.signUp()`
- On success → navigates to ProfilePickerScreen (first time) or HomeScreen
- Loading spinner during request
- Error message display

---

## Profile

### ProfilePickerScreen (`features/profile/ProfilePickerScreen.kt`)
**iOS:** `ProfilePickerView.swift`

- Centered grid of profile cards (max 5)
- Each card: avatar icon + name
- "+ Add Profile" card if < 5 profiles
- Tap profile → selects and navigates to HomeScreen
- Edit mode → shows pencil overlay on each card → navigates to ProfileEditorScreen
- `maxWidth` constraint centered (same visual weight as iOS 700pt max)

### ProfileEditorScreen (`features/profile/ProfileEditorScreen.kt`)
**iOS:** `ProfileEditorView.swift`

- Name text field
- Avatar picker grid (SF Symbol equivalents → Material icons or custom SVGs)
- Kids Profile toggle
- Save / Delete buttons
- `maxWidth` centered layout (~400dp equivalent)

---

## Main Navigation (BottomBar — replaces iOS TabView)

Tabs (same order as iOS):
1. Home
2. Browse
3. Search
4. Library
5. Downloads
6. Settings

---

## Home

### HomeScreen (`features/home/HomeScreen.kt`)
**iOS:** `HomeView.swift`

- **Hero Banner** — auto-scrolling pager (3-5 featured items)
  - Background image, logo overlay, title, rating, genres
  - Play button, + My List button
- **Continue Watching Row** — horizontal LazyRow
  - Poster + progress bar + "X min left"
  - Tap → plays immediately
- **Content Rows** — multiple horizontal LazyRows by genre/catalog
  - Each row: title + horizontal scroll of ContentCards
- Pull-to-refresh
- Profile avatar top-right → navigates to ProfilePickerScreen

### ContentCard (shared composable)
**iOS:** `ContentCard` in `ContentRow.swift`

- Poster image (Coil async)
- IMDB rating badge overlay
- Title + year on press (long-press or tap-hold)
- Ripple tap → ContentDetailScreen

### WatchHistoryCard (shared composable)
**iOS:** `ContinueWatchingRow.swift` cards

- Poster with dark gradient
- Progress bar at bottom
- Episode label for series (S01E05)
- "X min left" text

---

## Browse

### BrowseScreen (`features/browse/BrowseScreen.kt`)
**iOS:** `BrowseView.swift`

- **Type selector** — Movies / Series chip toggle
- **Genre filter** — horizontal chip row (Action, Comedy, Drama, etc.)
- **Content grid** — 2-column LazyVerticalGrid
- **Infinite scroll** — loads next page when near bottom
- Loading shimmer placeholder
- Each item → ContentDetailScreen on tap

---

## Search

### SearchScreen (`features/search/SearchScreen.kt`)
**iOS:** `SearchView.swift`

- Search TextField with clear button
- **500ms debounce** before firing API call
- **Empty state** — popular searches as chips
- **Results** — 2-column grid (same as Browse)
- "No results" empty state

---

## Content Detail

### ContentDetailScreen (`features/detail/ContentDetailScreen.kt`)
**iOS:** `ContentDetailView.swift`

- Full-width background image with gradient fade
- Logo or title text
- Year · Rating · Runtime · Genres row
- Description (expandable with "more")
- Action buttons row:
  - **Play** (or Resume with progress)
  - **+ My List** (toggle)
  - **Download**
  - **Share**
- **Cast & Crew** horizontal scroll
- **Episodes section** (series only):
  - Season picker dropdown
  - Episode list: thumbnail + title + overview + duration
  - Progress indicator on watched episodes
- **More Like This** row
- **Stream Selection** → bottom sheet on Play tap

### StreamSelectionSheet (`features/detail/StreamSelectionSheet.kt`)
**iOS:** `StreamSelectionSheet.swift`

- Bottom sheet (ModalBottomSheet)
- Stream list: name, quality, size, seeders
- RD cache status badge (green = cached, grey = not)
- Sort by quality (4K > FHD > HD > SD)
- Loading state while fetching streams
- Select stream → resolves RD if needed → starts PlayerScreen

---

## Player

### PlayerScreen (`features/player/PlayerScreen.kt`)
**iOS:** `PlayerView.swift`

Full-screen overlay on top of everything (same ZIndex pattern → `Dialog` composable or dedicated Activity).

**Layout matches iOS exactly:**

```
┌──────────────────────────────────────────────────────┐
│  Title                              9:42 PM      [X] │
│  Year · S02E05 · "Episode" [FHD]   Ends at 10:14 PM  │
│                                                       │
│                    [⏸] center                         │
│                                                       │
│  23:45 ══════════════════════░░░░░░░░░░ 42:18         │
│                                                       │
│  [Subtitles] [EN ▼] [🔊━━]    [Next Episode ▶] [⚙]  │
│                                S02E06 · "Next Title"  │
└──────────────────────────────────────────────────────┘
```

**Components:**
- `topLeftInfo` — title, year, episode info, quality badge
- `topRightTime` — system clock + "Ends at" time
- `closeButton` — X to dismiss player, returns to previous screen
- `centerPlayPause` — visible during playback
- `progressBar` — seekable slim red bar with draggable thumb
- `bottomToolbar` — pill buttons (Subtitles, Audio language, Volume, Settings)
- `nextEpisodePill` — bottom-right, shows next episode name
- `infoOverlay` — appears when paused: synopsis, cast, genres, rating

**Controls behavior:**
- Auto-hide after 4 seconds of playback
- Tap anywhere to show/hide controls
- Brightness gesture (vertical swipe left)
- Volume gesture (vertical swipe right)

**Overlays:**
- `skipIntroBanner` — shows 5–90s into series episodes, skips +30s
- `upNextOverlay` — last 30s or 95%+ progress, 10s countdown + cancel

**Quality Badge Colors:**
| Quality | Color |
|---------|-------|
| 4K | Purple |
| FHD | Blue |
| HD | Green |
| SD | Orange |

---

## Library

### LibraryScreen (`features/library/LibraryScreen.kt`)
**iOS:** `LibraryView.swift`

- Filter tabs: All · Movies · Series
- Grid of favorited content (same ContentCard)
- Empty state when no favorites
- Remove from list on long-press → confirmation

---

## Downloads

### DownloadsScreen (`features/downloads/DownloadsScreen.kt`)
**iOS:** `DownloadsView.swift`

- **In Progress** section: progress bar per item, pause/resume/cancel
- **Completed** section: play directly, delete button
- Series grouped by show name
- Total storage used indicator
- Empty state

---

## Settings

### SettingsScreen (`features/settings/SettingsScreen.kt`)
**iOS:** `InAppSettingsView.swift`

Sections (same tabs as iOS, rendered as scrollable sections on Android):

**Accounts**
- Real-Debrid API key input + validate button + user info display
- OpenSubtitles username + password + login button

**Playback**
- Preferred audio language picker
- Auto-play next episode toggle
- Default quality picker (4K / FHD / HD / SD)

**Storage**
- Cache size display
- Clear cache button
- Downloads location display

**Addons**
- Installed addons list (name, version, toggle enable/disable)
- Add addon by URL (text field + Install button)
- Remove addon (swipe or long-press)

**About**
- App version
- GitHub / credits
- Logout button
