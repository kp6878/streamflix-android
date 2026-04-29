# Content Detail Parity — Complete Detail Screen Specification

The ContentDetailView is one of the most complex screens in the app, handling
movies and series with downloads, resume logic, and stream selection integration.

---

## Screen Layout

```
┌────────────────────────────────────────────┐
│  [background image]                   [X]  │  Hero (600dp desktop / 420dp phone)
│  ╔═══════════════════════╗                 │
│  ║  Logo/Title           ║                 │
│  ╚═══════════════════════╝                 │
├────────────────────────────────────────────┤
│  [▶ Play/Resume S1E03]  [♡]  [↗]          │  Action buttons + progress bar
│  ═══════░░░░░░░░░░░░ (progress if resuming)│
├────────────────────────────────────────────┤
│  ★ 8.5  •  2025  •  55 min  •  Drama      │  Metadata
├────────────────────────────────────────────┤
│  Description text...                       │
│  Cast: Actor1, Actor2, Actor3              │
├────────────────────────────────────────────┤
│  Season 1 ▼                                │  (series only)
│  ┌──────────────────────────────────┐      │
│  │ [thumb] 1. "Pilot"    45min [⬇] │      │
│  │ ═══░░  Episode overview...       │      │
│  ├──────────────────────────────────┤      │
│  │ [thumb] 2. "Cat's"   47min [⬇]  │      │
│  │ ✓  Episode overview...           │      │
│  └──────────────────────────────────┘      │
└────────────────────────────────────────────┘
```

---

## Hero Header Specifications

| Property | Phone | Tablet/Desktop |
|----------|-------|----------------|
| Height | `420.dp` | `600.dp` |
| Logo max width | `240.dp` | `300.dp` |
| Logo max height | `80.dp` | `100.dp` |
| Gradient | 50% clear top, 50% fade to black bottom |
| Background image | Fill width, `contentScale = Crop`, alignment = top |
| Close button top padding | `12.dp` phone / `16.dp` desktop |

---

## Play/Resume Button Logic

### For Movies

```kotlin
val historyItem = watchHistoryManager.getItem(contentId)
val hasProgress = historyItem?.shouldResume == true  // 2%-90%
val progress = historyItem?.progress ?: 0.0

// Button text: "Resume" if hasProgress, "Play" otherwise
// Shows red progress bar at bottom of button if hasProgress && progress > 0.02
```

### For Series

The button shows the **next episode to play**:

```kotlin
fun getNextEpisodeInfo(content: MetaItem): EpisodeButtonInfo? {
    // 1. Check continue watching for in-progress episode
    val inProgress = continueWatching.firstOrNull { it.contentId == content.id }
    if (inProgress != null) {
        return EpisodeButtonInfo(
            label = "S${inProgress.season} E${inProgress.episode}",
            isResume = true,
            progress = inProgress.progress
        )
    }
    
    // 2. Find next unwatched episode
    val nextEp = watchHistoryManager.getNextUnwatchedEpisode(content.id, allEpisodes)
    if (nextEp != null) {
        return EpisodeButtonInfo(
            label = "S${nextEp.season} E${nextEp.episode}",
            isResume = nextEp.resumePosition != null,
            progress = null
        )
    }
    
    // 3. Default to first episode
    return EpisodeButtonInfo(label = "S1 E1", isResume = false, progress = null)
}
```

### Downloaded Content Priority

If the next episode (or movie) is **downloaded**, show a different button:
- Icon: green download checkmark instead of play triangle
- Text: "Resume Downloaded" or "Play Downloaded" 
- Subtext: "(Downloaded)" in green
- Taps play from local file instead of streaming

### Button Styling

| Property | Value |
|----------|-------|
| Style | `borderedProminent`, white tint, black text |
| Font | `headline` |
| Progress bar height | `3.dp` |
| Progress track | `Black.copy(0.3f)` |
| Progress fill | `Color.Red` |

---

## Favorite Button (My List)

```kotlin
IconButton(
    onClick = { toggleFavorite(content) }
) {
    Icon(
        if (isFavorite) Icons.Default.Check else Icons.Default.Add,
        contentDescription = "My List",
        modifier = Modifier.size(50.dp)
    )
}
// Tint: green.copy(0.3f) if favorite, gray if not
// Animation: easeInOut 200ms on toggle
```

---

## Share Button

```kotlin
IconButton(onClick = { /* Share via Android Intent */ }) {
    Icon(Icons.Default.Share, modifier = Modifier.size(50.dp))
}
// Tint: gray
// Action: Android share intent with content title + IMDB link
```

---

## Metadata Row

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    // Star rating (yellow)
    content.imdbRating?.let { rating ->
        Row {
            Icon(Icons.Default.Star, tint = Color.Yellow, modifier = Modifier.size(16.dp))
            Text(rating, color = Color.Yellow)
        }
    }
    
    // Year
    content.releaseInfo?.let { Text(it, color = Color.Gray) }
    
    // Runtime
    content.runtime?.let { Text(it, color = Color.Gray) }
    
    // Genres (max 2)
    content.genres?.take(2)?.joinToString(", ")?.let { Text(it, color = Color.Gray) }
}
// Font: bodyMedium
```

---

## Episodes Section

### Season Picker

```kotlin
// Dropdown menu when > 1 season
// Seasons sorted: 1, 2, 3, ... N, 0 (specials last)
val seasons = videos.mapNotNull { it.season }.distinct().sortedWith(
    compareBy { if (it == 0) Int.MAX_VALUE else it }
)

// Initialize selectedSeason from content load
// Default: first non-zero season, or 1
```

### Episode Row

```
┌─────────────────────────────────────────────────────────┐
│  [Thumbnail]   1. "Pilot"                    45 min     │
│   80×48.dp     ════░░░░░░ (progress bar)      [⬇] [▶]  │
│                Episode overview text...                  │
│                Episode overview text max 3 lines...      │
└─────────────────────────────────────────────────────────┘
```

| Property | Value |
|----------|-------|
| Thumbnail | `80×48.dp`, `8.dp` radius, aspect fill |
| Title font | `bodyMedium.bold`, white |
| Episode number | Prefix to title |
| Duration | `labelSmall`, gray, right-aligned |
| Overview | `labelSmall`, `White.copy(0.7f)`, 3-line limit |
| Progress bar | `3.dp` height, same style as hero |
| Watched checkmark | Green check icon for completed episodes |
| Download button | Download arrow icon (green) |
| Play button | Play circle icon (blue) |
| Background | `Gray.copy(0.1f)` if playing, clear otherwise |
| Row padding | `12.dp` |
| Row radius | `8.dp` |

### Episode Download Status

Each episode shows its download state:
- **Not downloaded:** Show download button (⬇)
- **Downloading:** Show progress percentage
- **Downloaded:** Show green checkmark + "Play Downloaded" action
- **Failed:** Show retry button

### Episode Tap Action

1. Set `selectedEpisode = episode`
2. Set `selectedVideoId = episode.id`
3. Calculate `resumePosition` from watch history
4. Show `StreamSelectionSheet`

---

## Stream Selection Sheet Integration

When user taps Play or an episode row:

```kotlin
StreamSelectionSheet(
    contentType = contentType,
    contentId = selectedVideoId ?: contentId,  // Episode ID for series
    onStreamSelected = { url, infoHash, fileIdx, streamName ->
        dismissSheet()
        playStream(url, infoHash, fileIdx, streamName)
    },
    contentName = content.name,
    seasonNumber = selectedEpisode?.season,
    episodeNumber = selectedEpisode?.episode,
    episodeName = selectedEpisode?.title,
    poster = content.poster,
    seriesId = if (contentType == "series") contentId else null,
    activeStreamInfoHash = nowPlaying?.streamInfoHash,
    activeStreamURL = nowPlaying?.streamSourceUrl
)
```

---

## NowPlayingItem Construction

When a stream is selected and playback starts:

```kotlin
val nowPlaying = NowPlayingItem(
    id = contentId,
    type = contentType,
    title = content.name,
    streamUrl = resolvedUrl,
    posterUrl = content.poster,
    season = selectedEpisode?.season,
    episode = selectedEpisode?.episode,
    episodeName = selectedEpisode?.title,
    
    // Rich metadata
    year = content.releaseInfo,
    genres = content.genres,
    imdbRating = content.imdbRating,
    runtime = content.runtime,
    contentDescription = content.description,
    cast = content.cast,
    logo = content.logo,
    episodeOverview = selectedEpisode?.overview,
    streamQuality = qualityBadge,
    background = content.background,
    
    // Stream source info
    streamInfoHash = effectiveInfoHash,
    streamFileIdx = stream.fileIdx,
    streamSourceUrl = stream.url,
    streamName = stream.title ?: stream.name,
    
    // Next episode
    nextEpisodeId = nextEp?.id,
    nextEpisodeSeason = nextEp?.season,
    nextEpisodeNumber = nextEp?.episode,
    nextEpisodeName = nextEp?.title,
    
    // All episodes for episode picker
    allEpisodes = allEpisodes
)
```

---

## Player Launch Sequence

```kotlin
// 1. Set player preferences
playerController.preferredAudioLanguage = prefs.primaryAudioLanguage
playerController.secondaryAudioLanguage = prefs.secondaryAudioLanguage
playerController.subtitlesEnabled = prefs.subtitlesEnabled
playerController.preferredSubtitleLanguage = prefs.preferredSubtitleLanguage

// 2. Set progress delegate
playerController.progressDelegate = watchProgressDelegate

// 3. Build PlaybackContentInfo
val contentInfo = PlaybackContentInfo(
    contentId = contentId,
    contentType = contentType,
    contentName = content.name,
    poster = content.poster,
    background = content.background,
    season = episode?.season,
    episode = episode?.episode,
    episodeId = episode?.id,
    episodeTitle = episode?.title,
    thumbnail = episode?.thumbnail
)

// 4. Load media
playerController.loadMedia(
    url = resolvedUrl,
    title = content.name,
    posterUrl = content.poster?.let { URL(it) },
    startPosition = resumePosition,
    contentInfo = contentInfo
)

// 5. Show player
appViewModel.showPlayer()

// 6. Fetch OpenSubtitles in background (if configured)
if (prefs.subtitlesEnabled && prefs.openSubtitlesApiKey.isNotEmpty()) {
    launch {
        fetchAndLoadOpenSubtitles(
            imdbId = contentId,
            season = episode?.season,
            episode = episode?.episode
        )
    }
}
```

---

## Auto-Play Episode on Load

When navigating to detail with `autoPlayVideoId` set (e.g., from continue watching):

```kotlin
LaunchedEffect(content) {
    if (content != null && autoPlayVideoId != null && !hasAutoPlayed) {
        val video = content.videos?.firstOrNull { it.id == autoPlayVideoId }
        if (video != null) {
            hasAutoPlayed = true
            selectedEpisode = video
            selectedVideoId = video.id
            resumePosition = getEpisodeResumePosition(video)
            autoPlayEpisode(video)  // Opens stream sheet immediately
        }
    }
}
```

---

## Loading / Error States

### Loading View
```kotlin
Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(48.dp))
    Text("Loading...", color = Color.Gray, modifier = Modifier.padding(top = 64.dp))
}
```

### Error View
```kotlin
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(Icons.Default.Warning, tint = Color.Red, modifier = Modifier.size(48.dp))
    Text("Failed to load content", style = titleMedium, color = White)
    Text(errorMessage, style = bodyMedium, color = Gray)
    Button(onClick = { viewModel.retry() }) {
        Text("Retry")
    }
}
```
