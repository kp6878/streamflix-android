# StreamFlix Android — Planning Index

Exact feature parity with the iOS/macOS app. Zero changes to any existing Swift/Xcode files.
The Android app lives entirely in a new `/android/` directory at the repo root.

---

## Planning Documents

### Core Architecture
| File | What It Covers |
|------|---------------|
| [TECH_STACK.md](TECH_STACK.md) | Kotlin, Compose, libraries — direct iOS→Android equivalents |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Folder structure, MVVM pattern, DI, data flow |
| [NAVIGATION.md](NAVIGATION.md) | Bottom nav, backstack, deep links |
| [DATA_MODELS.md](DATA_MODELS.md) | Room entities, DAOs, domain models, API response models, Supabase schema |

### Feature Specifications
| File | What It Covers |
|------|---------------|
| [SCREENS.md](SCREENS.md) | Every screen, 1-to-1 with iOS counterparts |
| [SERVICES.md](SERVICES.md) | All services and API integrations |
| [PLAYER_PARITY.md](PLAYER_PARITY.md) | Complete video player: overlays, gestures, states, track pickers, source picker |
| [NEXT_EPISODE_ENGINE.md](NEXT_EPISODE_ENGINE.md) | Auto-play, stream matching algorithm, episode chain, continue watching |
| [STREAM_PARSING.md](STREAM_PARSING.md) | Quality/audio/size/language detection, Torrentio URL builder |
| [SETTINGS_PARITY.md](SETTINGS_PARITY.md) | Complete settings screen: all 5 sections, every field, factory reset |
| [REAL_DEBRID_FLOW.md](REAL_DEBRID_FLOW.md) | 6-step torrent resolution pipeline, file selection, API reference, error codes |
| [CONTENT_DETAIL_PARITY.md](CONTENT_DETAIL_PARITY.md) | Detail screen: hero, play/resume logic, episodes, downloads, player launch |

### Design & Visual
| File | What It Covers |
|------|---------------|
| [UI_PARITY.md](UI_PARITY.md) | Design tokens, color palette, typography, spacing, animations, component specs |

### Implementation
| File | What It Covers |
|------|---------------|
| [IMPLEMENTATION_PHASES.md](IMPLEMENTATION_PHASES.md) | 14-phase build order |

---

## Goal

Reproduce StreamFlix on Android with:
- Same UI layout and feel (Jetpack Compose, dark theme, `#E50914` Netflix red accent)
- Same content sources (Stremio addons: Cinemeta, Torrentio, TorrentsDB)
- Same debrid service (Real-Debrid API integration)
- Same auth (Supabase email/password with session persistence)
- Same subtitle support (OpenSubtitles API + embedded track selection)
- Same video playback (libVLC for Android — TrueHD, DTS-HD, Atmos codec support)
- Same profile system (up to 5 profiles, kids flag, avatar gradients)
- Same watch history + favorites (local-first with Room, Supabase sync, 1s debounced saves)
- Same download manager (WorkManager background downloads, pause/resume/cancel)
- Same player features (auto-play next episode, stream source matching, episode picker, skip intro, Up Next overlay)
- Same settings (dual audio language, subtitle text size, cache management, factory reset)

---

## Feature Parity Checklist

### Core
- [ ] Stremio addon protocol implementation
- [ ] Cinemeta integration (metadata)
- [ ] Torrentio integration (streams)
- [ ] TorrentsDB integration (streams)
- [ ] Real-Debrid API integration
- [ ] libVLC video player

### Content
- [ ] Home screen with hero banner + catalog rows
- [ ] Browse by type/genre
- [ ] Search with 500ms debounce
- [ ] Movie detail view
- [ ] Series detail with episodes + season picker

### Playback
- [ ] Video playback with libVLC
- [ ] Audio track selection
- [ ] Subtitle support (embedded + external)
- [ ] OpenSubtitles auto-fetch
- [ ] Resume from last position
- [ ] Primary + secondary audio language preference
- [ ] Next episode button + auto-play
- [ ] Subtitle text size adjustment (6 levels)
- [ ] English-only subtitle filter
- [ ] Episode picker in player
- [ ] Source picker in player
- [ ] Skip intro button (series, 5-90s)
- [ ] Up Next overlay (last 30s, 10s countdown)
- [ ] Paused info overlay (synopsis, cast, genres)
- [ ] Brightness/volume swipe gestures

### Stream Selection
- [ ] Quality badges (4K, 1080p, 720p, SD, CAM)
- [ ] File size display
- [ ] Audio codec badges (Atmos, TrueHD, DTS-HD, DD+, AAC)
- [ ] Language flags
- [ ] Cached/instant play detection
- [ ] Download button per stream
- [ ] Already downloaded warning banner
- [ ] Currently downloading banner
- [ ] Download paused banner

### Downloads
- [ ] WorkManager background downloads
- [ ] Download progress tracking
- [ ] Pause/Resume/Cancel
- [ ] Category tabs (All, Movies, TV Shows)
- [ ] Status filter (All, In Progress, Completed)
- [ ] Series grouping with collapsible cards
- [ ] Offline playback
- [ ] Storage size display

### User Features
- [ ] Multiple user profiles (max 5)
- [ ] Profile picker on launch
- [ ] Per-profile favorites
- [ ] Per-profile watch history
- [ ] Continue watching row
- [ ] Watchlist/Library tab

### Settings
- [ ] StreamFlix account display + sign out
- [ ] Real-Debrid configuration + validation
- [ ] OpenSubtitles configuration (API key + login)
- [ ] Primary audio language preference
- [ ] Secondary audio language (fallback)
- [ ] Subtitles toggle (always on/off)
- [ ] Subtitle language preference
- [ ] Subtitle text size adjustment
- [ ] Auto-play next episode toggle
- [ ] Image cache size limit (2/4/6/8 GB)
- [ ] Clear cache button
- [ ] Addon management
- [ ] Factory reset with confirmation

---

## Source Reference

The iOS/macOS source code is the ground truth:
- `StreamFlix/StreamFlixApp/` — All Swift source files
- `StreamFlix/COMPLETE_DOCUMENTATION.md` — Feature documentation
- `StreamFlix/PLANNING.md` — Original iOS development plan
- `docs/UI.md` — Complete UI reference with pixel-level specifications
- `docs/Planning.md` — Comprehensive development plan
- `CLAUDE.md` — Architecture and feature reference
