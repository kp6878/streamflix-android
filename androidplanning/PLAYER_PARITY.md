# Player Parity — Complete Video Player Specification

The player is the most complex screen in the app. This document covers every overlay,
gesture, state transition, and sub-feature to ensure exact iOS parity.

---

## Player Architecture

### Overlay Pattern

The player is a **full-screen overlay** rendered on top of all other content:

```
┌─────────────────────────────────────────────────────┐
│  Box(modifier = Modifier.fillMaxSize()) {           │
│    NavHost(...)        // Main app navigation        │
│                                                      │
│    if (showPlayer) {                                 │
│      PlayerScreen(...)  // Full-screen, zIndex = 100 │
│    }                                                 │
│  }                                                   │
└─────────────────────────────────────────────────────┘
```

On Android: use `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))` 
or a dedicated `Activity` with `android:screenOrientation="landscape"` and `android:theme="@style/Theme.FullScreen"`.

### Screen Orientation

- Force **landscape** when player is visible
- Restore **portrait/auto** when player closes
- Use `activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE`

### Immersive Mode

When player opens:
```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
WindowInsetsControllerCompat(window, view).apply {
    hide(WindowInsetsCompat.Type.systemBars())
    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
```

---

## Player State Machine

```
IDLE → LOADING → PLAYING ↔ PAUSED
                    ↓          ↓
                BUFFERING   SEEKING
                    ↓
                 PLAYING
                    ↓
                  ERROR
                    ↓
                DISMISSED
```

### State Properties (StateFlows)

```kotlin
data class PlayerState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val currentTime: Double = 0.0,       // seconds
    val duration: Double = 0.0,
    val progress: Double = 0.0,          // 0.0–1.0
    val volume: Int = 100,               // 0–100
    val isMuted: Boolean = false,
    val title: String? = null,
    val error: String? = null,
    
    // Tracks
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedAudioTrackIndex: Int? = null,
    val selectedSubtitleTrackIndex: Int? = null,
    
    // Formatted times
    val currentTimeFormatted: String = "0:00",
    val durationFormatted: String = "0:00",
)
```

---

## Controls Overlay Layout

```
┌────────────────────────────────────────────────────────────────────────┐
│  Title                                              9:42 PM      [X]  │
│  2013 · S02E05 · "The Swedes"  [FHD]               Ends at 10:14 PM  │
│                                                                        │
│                                                                        │
│                            [⏸] center                                  │
│                                                                        │
│                                                                        │
│  23:45 ════════════════════════════░░░░░░░░░░░░░░░░░░░░░░░░ 42:18      │
│                                                                        │
│  [Subtitles] [English ▼] [Episodes] [Sources] [🔊━━]   [Next ▶] [⚙]  │
└────────────────────────────────────────────────────────────────────────┘
```

### Controls Visibility State

```kotlin
var showControls by remember { mutableStateOf(true) }
var controlsHideJob: Job? = null

// Auto-hide after 4 seconds when playing
fun startControlsTimer(delayMs: Long = 4000) {
    controlsHideJob?.cancel()
    controlsHideJob = scope.launch {
        delay(delayMs)
        showControls = false
    }
}

// Tap anywhere → toggle play/pause + show controls
// When playing resumes → restart 4s timer
// When paused → controls stay visible indefinitely
```

### Controls Animation

```kotlin
AnimatedVisibility(
    visible = showControls,
    enter = fadeIn(animationSpec = tween(200)),
    exit = fadeOut(animationSpec = tween(200))
)
```

### Translucent Overlay Background

When controls are visible: `Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.25f)))`

---

## Top Left Info

```kotlin
Column(modifier = Modifier.padding(start = 32.dp, top = topPadding)) {
    // Title
    Text(
        text = nowPlaying.title,
        style = if (isCompact) titleSmall.bold() else titleLarge.bold(),
        color = Color.White,
        maxLines = 1
    )
    
    // Metadata row
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Year
        nowPlaying.year?.let { Text(it, fontWeight = FontWeight.Medium, color = White.copy(0.8f)) }
        
        // Episode info (series only)
        if (nowPlaying.type == "series") {
            nowPlaying.episodeInfo?.let { 
                Text("•", color = White.copy(0.5f))
                Text(it, fontWeight = FontWeight.Bold, color = White)
            }
            nowPlaying.episodeName?.let { name ->
                if (name.isNotEmpty()) {
                    Text("•", color = White.copy(0.5f))
                    Text("\"$name\"", fontWeight = FontWeight.Medium, color = White.copy(0.8f), maxLines = 1)
                }
            }
        }
        
        // Quality badge
        nowPlaying.qualityBadge?.let { QualityBadge(it) }
    }
}
```

---

## Top Right Time

```kotlin
Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 32.dp)) {
    // Current system time
    Text(
        text = currentTimeFormatted,  // "9:42 PM" — h:mm a format
        style = if (isCompact) bodyMedium.bold() else titleMedium.bold(),
        fontFamily = FontFamily.Monospace,
        color = Color.White
    )
    
    // "Ends at" time
    if (duration > 0) {
        Text(
            text = "Ends at $endsAtTime",  // calculated from remaining time
            style = if (isCompact) labelSmall else bodyMedium,
            color = White.copy(0.7f)
        )
    }
}
```

### Ends-At Calculation
```kotlin
val endsAtTime: String
    get() {
        val remainingSeconds = duration - currentTime
        val endDate = System.currentTimeMillis() + (remainingSeconds * 1000).toLong()
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endDate))
    }
```

---

## Close Button

```kotlin
IconButton(
    onClick = { closePlayer() },
    modifier = Modifier
        .size(40.dp)
        .background(Color.White.copy(alpha = 0.15f), CircleShape)
) {
    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(24.dp))
}
```

---

## Progress Bar

### Layout

```
[currentTime]  ══════════════════════░░░░░░░░░  [duration]
```

### Specifications

| Property | Normal | Active (dragging/hovering) |
|----------|--------|---------------------------|
| Track height | `5.dp` | `8.dp` |
| Track color | `White.copy(0.3f)` | Same |
| Fill color | `Color.Red` | Same |
| Thumb visible | No | Yes |
| Thumb size | N/A | `16.dp` white circle |
| Thumb shadow | N/A | `Black.copy(0.5f)`, `4.dp` blur |
| Hit area height | `36.dp` | `36.dp` |

### Scrubbing Behavior

1. User starts drag → `isScrubbing = true`, show thumb
2. During drag → update `scrubProgress` (0.0–1.0), show scrub time in red
3. On drag end → `controller.seek(to: duration * progress)`
4. Keep showing scrub position for **500ms** after release (VLC catch-up delay)
5. After 500ms → `isScrubbing = false`

### Time Labels

- **Left:** Current time (or scrub time in RED during scrub)
- **Right:** Total duration
- **Font:** `bodyMedium.bold().monospacedDigit()`
- **Color:** White (red during scrub)

### Time Formatting
```kotlin
fun formatTime(seconds: Double): String {
    val total = seconds.toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
           else String.format("%d:%02d", m, s)
}
```

---

## Bottom Toolbar — Pill Buttons

All buttons use the same `PillButton` composable:

```kotlin
@Composable
fun PillButton(
    icon: ImageVector,
    label: String? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = if (isActive) 0.25f else 0.15f),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (label == null) 14.dp else 18.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = White.copy(if (isActive) 1f else 0.9f))
            label?.let {
                Text(it, color = White.copy(if (isActive) 1f else 0.9f), 
                     fontWeight = FontWeight.Medium)
            }
        }
    }
}
```

### Toolbar Items (left to right)

| Button | Icon | Label | Condition |
|--------|------|-------|-----------|
| Subtitles | `closed_caption` | "On" / "Off" (compact: icon only) | Always |
| Audio | `volume_up` | Current language name (compact: icon only) | Always |
| Episodes | `list` | "Episodes" (compact: icon only) | Series with episodes only |
| Sources | `swap_horiz` | "Sources" (compact: icon only) | Real-Debrid configured only |
| Volume | `volume_up` + Slider | N/A | Android: always (or use system volume) |
| `Spacer(weight)` | | | |
| Next Episode | `skip_next` | Episode info + thumbnail | Series with next episode |
| Settings | `settings` | None | Always |

### Next Episode Pill (Desktop/Tablet)

```
┌──────────────────────────────────────────────┐
│ [Thumbnail]  Next Episode                    │
│  56×36.dp    S02E06 • "USPIS"  [forward ▶]  │
└──────────────────────────────────────────────┘
```

- Background: `Color.White`
- Text color: `Color.Black`
- Corner radius: `22.dp`
- Thumbnail: `56×36.dp`, `6.dp` radius
- On phone: show icon-only pill button instead

---

## Center Play/Pause Button

Visible only when controls are shown AND video is playing:

```kotlin
IconButton(
    onClick = { controller.togglePlayPause() },
    modifier = Modifier
        .size(80.dp)
        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
) {
    Icon(
        Icons.Default.Pause,
        contentDescription = "Pause",
        tint = Color.White,
        modifier = Modifier.size(44.dp)
    )
}
```

---

## Info Overlay (Paused State)

Shows when: `showControls && !isPlaying && !isBuffering`

### Layout

```
┌─────────────────────────────────────────────────┐
│                                            [X]   │  ← Close button stays on top
│                                                   │
│                    ┌──────┐                       │
│                    │ PAUSE│                       │  ← Center: pause icon + "PAUSED"
│                    │ icon │                       │
│                    └──────┘                       │
│                                                   │
│  ┌────────────────────────────┐                   │
│  │  [Logo or Title]           │                   │  ← Bottom-left info panel
│  │  S02E05 • "The Swedes"    │                   │
│  │  [Comedy] [Drama]  85%👍  │                   │
│  │  Episode synopsis text... │                   │
│  │  Starring: Actor1, Actor2 │                   │
│  └────────────────────────────┘                   │
└─────────────────────────────────────────────────┘
```

### Center Pause Indicator

```kotlin
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(Icons.Default.Pause, size = 56.dp, tint = White)
    Text("PAUSED", style = labelSmall.bold, color = White.copy(0.7f), letterSpacing = 2.sp)
}
// Wrapped in Circle background: Black.copy(0.5f), padding = 28.dp
```

### Bottom-Left Info Panel

```kotlin
Surface(
    color = Color.Black.copy(alpha = 0.75f),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.padding(horizontal = 32.dp, bottom = 120.dp)
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Logo image (max 320×80.dp) or Title text (largeTitle.bold)
        // Episode info (headline, White.copy(0.9f))
        // Genre badges + rating percentage
        // Synopsis (body, White.copy(0.85f), 2-line limit, max width 450.dp)
        // Cast (bodyMedium, White.copy(0.6f), 1-line limit)
    }
}
```

### Genre Badge
```kotlin
Text(
    text = genre,
    style = labelSmall.bold,
    color = White,
    modifier = Modifier
        .background(White.copy(0.2f), RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 5.dp)
)
```

### Rating Percentage
```kotlin
// Convert IMDB rating to percentage: "8.5" → "85%"
val percentage = (rating.toDouble() * 10).toInt()
Row {
    Icon(Icons.Default.ThumbUp, tint = Green)
    Text("$percentage%", color = Green, fontWeight = FontWeight.Bold)
}
```

---

## Skip Intro Button

### Trigger Condition
- Content type = series
- Current time between **5 seconds** and **90 seconds**
- Not cancelled by user

### Action
Skip forward **30 seconds** from current position.

### UI
Standard `PillButton` style, positioned bottom-right:
```kotlin
PillButton(
    icon = Icons.Default.SkipNext,
    label = "Skip Intro",
    onClick = { controller.skipForward(30.0) }
)
```

---

## Up Next Overlay

### Trigger Condition
```kotlin
val showUpNextOverlay = showNextEpisodeButton 
    && controller.duration > 0 
    && !autoPlayCancelled
    && (remainingTime < 30 || progress > 0.95)
```

### Layout

```
                                    ┌───────────────────────────────┐
                                    │  [Thumbnail]  Up Next          │
                                    │   120×70.dp   S02E06           │
                                    │               "Episode Name"   │
                                    │                                │
                                    │  [Cancel]  [10 Play Now ▶]    │
                                    └───────────────────────────────┘
```

### Position
- Bottom-right corner
- Padding: `32.dp` trailing, `140.dp` bottom (phone: `16.dp`, `110.dp`)

### Styling
- Background: `Black.copy(0.8f)`, `16.dp` radius
- Padding: `24.dp` (phone: `16.dp`)
- Thumbnail: `120×70.dp`, `8.dp` radius

### Auto-Play Countdown

```kotlin
var countdown by remember { mutableIntStateOf(10) }
var countdownJob: Job? = null
var autoPlayCancelled by remember { mutableStateOf(false) }

// Start when overlay appears
LaunchedEffect(showUpNextOverlay) {
    if (showUpNextOverlay && !autoPlayCancelled) {
        countdown = 10
        countdownJob = launch {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            // Auto-play next episode
            onNextEpisode()
        }
    }
}
```

### Buttons
- **Cancel:** `White.copy(0.2f)` bg, white text, `22.dp` radius
- **Play Now:** White bg, black text, `22.dp` radius. Shows countdown number when > 0.

### State Reset
When `controller.title` changes (new episode loaded) → reset:
```kotlin
autoPlayCancelled = false
countdown = 0
countdownJob?.cancel()
```

---

## Track Picker Sheet

Modal overlay for audio and subtitle selection.

### Trigger
- Tap "Subtitles" pill → `trackPickerType = SUBTITLE`
- Tap "Audio" pill → `trackPickerType = AUDIO`

### Layout

```
┌────────────────────────────────────┐
│  🔊 Audio                    [X]   │  Header
│────────────────────────────────────│
│  ✓ English (AAC 5.1)              │  Selected = blue checkmark
│    Hindi (AAC Stereo)             │  Unselected = no icon
│    Japanese (AAC Stereo)          │
│    Original                        │
└────────────────────────────────────┘
```

### Container
- Max width: `420.dp`
- Height: `540.dp` (tablet/desktop) / `480.dp` (phone)
- Background: `Color(0xFF1F1F1F)`, `16.dp` radius
- Shadow: `Black.copy(0.7f)`, `40.dp` blur
- Dismiss: tap outside overlay background (`Black.copy(0.75f)`)

### Audio Track Row
```kotlin
Row {
    if (isSelected) Icon(Icons.Default.Check, tint = Blue, size = 20.dp)
    Column {
        Text(track.displayName, color = if (isSelected) White else White.copy(0.7f), 
             fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        Text(track.codecInfo, style = labelSmall, color = Gray)
    }
}
// Background: White.copy(0.1f) if selected, clear otherwise
// Padding: 14.dp h / 12.dp v, radius: 8.dp
```

### Subtitle Track Row
Same as audio but includes:
- "Off" option at top (disables subtitles)
- Embedded tracks filtered to **English only** by default
  - Filter logic: language code matches `en`, `eng`, `en-*`, `eng-*`, or contains "english" (case-insensitive)
- External subtitles (from OpenSubtitles) always shown regardless of language

---

## Episode Picker Sheet

Triggered from "Episodes" pill button.

### Layout

```
┌──────────────────────────────────────┐
│  Episodes                       [X]  │
│  [Season 1 ▼]  dropdown             │
│──────────────────────────────────────│
│  ▶ 1. "Pilot"                        │  ← Currently playing (highlighted)
│       45 min • Episode synopsis...   │
│  2. "Cat's in the Bag"              │
│       47 min • Synopsis text...      │
│  3. "And the Bag's in the River"    │
│       48 min                         │
└──────────────────────────────────────┘
```

### Specifications
- Max width: `520.dp`
- Season picker: dropdown menu
- Currently playing episode: highlighted with play icon, white background accent
- Each row shows: episode number, title, duration, synopsis (2-line limit)
- Tap episode → `onSelectEpisode(episodeInfo)` → loads new stream

### Season Initialization
```kotlin
// Set initial season to current episode's season
selectedSeason = nowPlaying.season ?: nowPlaying.availableSeasons.firstOrNull() ?: 1
```

---

## Source Picker Overlay

Allows switching stream source without leaving the player.

### Trigger
- "Sources" pill button in bottom toolbar
- Pauses playback when opened

### Behavior
1. Opens `StreamSelectionSheet` as an overlay inside the player
2. User selects a new stream → resolves via Real-Debrid
3. New stream URL passed to `onSourceSelected(url, infoHash, fileIdx)`
4. Player loads new media at current position

---

## Video Settings Sheet

Triggered from gear icon in bottom toolbar.

### Content
- Playback speed selector (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x)
- Subtitle text size (matches iOS: Extra Small 0.5x → Maximum 2.0x)
- Video quality info display

---

## Audio Auto-Selection

When video starts playing with multiple audio tracks:

```kotlin
fun autoSelectAudioTrack(tracks: List<AudioTrack>, primaryLang: String, secondaryLang: String) {
    // 1. Try primary language
    val primary = tracks.firstOrNull { it.language.contains(primaryLang, ignoreCase = true) }
    if (primary != null) { selectAudioTrack(primary.id); return }
    
    // 2. Try secondary language (fallback)
    val secondary = tracks.firstOrNull { it.language.contains(secondaryLang, ignoreCase = true) }
    if (secondary != null) { selectAudioTrack(secondary.id); return }
    
    // 3. Keep default (first track)
}
```

Default configuration:
- Primary: "en" (English)
- Secondary: "hi" (Hindi)

---

## Subtitle Auto-Selection

When subtitles are enabled in settings:

```kotlin
fun autoSelectSubtitleTrack(tracks: List<SubtitleTrack>, preferredLang: String) {
    if (!subtitlesEnabled) {
        disableSubtitles()
        return
    }
    
    // 1. Try preferred language
    val preferred = tracks.firstOrNull { it.language.contains(preferredLang, ignoreCase = true) }
    if (preferred != null) { selectSubtitleTrack(preferred.id); return }
    
    // 2. Select first available
    tracks.firstOrNull()?.let { selectSubtitleTrack(it.id) }
}
```

---

## Subtitle Text Size

Configurable in Settings → Playback → Subtitles → Text Size.

| Label | Scale | libVLC fontsize value |
|-------|-------|-----------------------|
| Extra Small | 0.5x | 32 |
| Small | 0.75x | 21 |
| Medium (default) | 1.0x | 16 |
| Large | 1.25x | 13 |
| Extra Large | 1.5x | 11 |
| Maximum | 2.0x | 8 |

### libVLC Implementation
```kotlin
// Scale formula: fontsize = (16 / scale).coerceIn(8, 32)
val fontsizeValue = (16.0 / scale).toInt().coerceIn(8, 32)
mediaPlayer.media?.addOption(":freetype-rel-fontsize=$fontsizeValue")
```

**Important:** VLCKit/libVLC uses inverse relationship — smaller fontsize value = larger text.
Size must be set BEFORE media starts playing.

---

## Gesture Controls (Android-Specific)

### Brightness (Left Side Vertical Swipe)
```kotlin
// Left half of screen: vertical swipe adjusts screen brightness
val brightness = window.attributes.screenBrightness
// Swipe up → increase, swipe down → decrease
// Range: 0.0f to 1.0f
// Show brightness overlay indicator
```

### Volume (Right Side Vertical Swipe)
```kotlin
// Right half of screen: vertical swipe adjusts volume
val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
// Swipe up → increase, swipe down → decrease
// Show volume overlay indicator
```

### Horizontal Swipe
```kotlin
// Horizontal swipe across entire screen: seek
// Sensitivity: ~1 second per 2dp of movement
// Show seek preview overlay with time
```

### Double Tap
```kotlin
// Double tap left third → skip backward 10s
// Double tap right third → skip forward 10s
// Show skip animation (ripple + ±10s text)
```

---

## Buffering Indicator

Center of screen, visible when `isBuffering && !isPlaying`:

```kotlin
CircularProgressIndicator(
    color = Color.White,
    modifier = Modifier.size(48.dp)  // 1.5x default scale
)
```

---

## Error Overlay

```kotlin
Column(
    modifier = Modifier
        .background(Color.Black.copy(0.9f), RoundedCornerShape(16.dp))
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    Icon(Icons.Default.Warning, tint = Color.Red, modifier = Modifier.size(48.dp))
    Text("Playback Error", style = titleMedium, color = White)
    Text(errorMessage, style = bodyMedium, color = White.copy(0.7f), textAlign = TextAlign.Center)
    Button(onClick = { closePlayer() }, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
        Text("Dismiss")
    }
}
```

---

## Clock Timer

System time display updates every 60 seconds:

```kotlin
var currentClockTime by remember { mutableStateOf(System.currentTimeMillis()) }

LaunchedEffect(Unit) {
    while (true) {
        delay(60_000)
        currentClockTime = System.currentTimeMillis()
    }
}

val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(currentClockTime))
```
