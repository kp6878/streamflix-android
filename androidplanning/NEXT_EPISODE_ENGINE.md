# Next Episode Engine — Auto-Play & Stream Matching

Complete specification for the next episode auto-play system,
including the stream source matching algorithm extracted from iOS.

---

## Overview

When playing a TV series, the app can automatically load the next episode using:
1. **Auto-play countdown** — 10-second countdown at the end of an episode
2. **Manual "Next Episode"** — button in the player toolbar
3. **Episode picker** — select any episode from the in-player picker

All three paths share the same stream resolution logic.

---

## Next Episode Resolution Flow

```
┌─────────────────────────────────────────┐
│  1. Get nextEpisodeId from NowPlaying   │
│  2. Stop current playback               │
│  3. Determine addon URL                 │
│     a. If RD key set → Torrentio URL    │
│     b. Else → first enabled stream addon│
│  4. Fetch streams for next episode      │
│  5. Wait for RD cache check (max 3s)    │
│  6. Find best matching stream           │
│  7. Resolve via Real-Debrid             │
│  8. Calculate episode AFTER next        │
│  9. Build NowPlayingItem                │
│  10. Load media in player               │
└─────────────────────────────────────────┘
```

---

## Stream Source Matching Algorithm

The most critical part: choosing the "right" stream for the next episode.
The algorithm tries to match the same release group / source used for the current episode.

### Priority Order

1. **Same source name** — match keywords from current stream's title/name
2. **Any cached stream** — prefer instant playback
3. **First available stream** — fallback

### Implementation

```kotlin
/**
 * Find the best matching stream for next episode based on current stream's characteristics.
 * Prioritizes: same source name > same quality + similar size > cached > first available.
 */
fun findMatchingStream(
    streams: List<Stream>,
    cachedHashes: Set<String>,
    currentStreamName: String?
): Stream? {
    // Separate cached streams
    val cached = streams.filter { stream ->
        cachedHashes.contains(stream.infoHash?.lowercase() ?: "")
    }
    val pool = if (cached.isNotEmpty()) cached else streams
    
    // If we know the current stream's name/title, find a stream from the same source
    if (!currentStreamName.isNullOrEmpty()) {
        // Extract keywords from current stream title
        // Stream titles look like: "Torrentio\n👤 35 💾 1.66 GB ⚙️ YIFY\n1080p"
        // We want to match the release group (e.g., "YIFY", "PSA", "RARBG")
        val currentWords = currentStreamName
            .split(Regex("""\s+"""))
            .map { it.lowercase() }
            .filter { it.length > 2 }
            .toSet()
        
        var bestMatch: Stream? = null
        var bestScore = 0
        
        for (stream in pool) {
            val streamText = (stream.title ?: stream.name ?: "")
            val streamWords = streamText
                .split(Regex("""\s+"""))
                .map { it.lowercase() }
                .filter { it.length > 2 }
                .toSet()
            
            val overlap = currentWords.intersect(streamWords).size
            if (overlap > bestScore) {
                bestScore = overlap
                bestMatch = stream
            }
        }
        
        // Require at least 2 keyword matches to consider it the "same" source
        if (bestScore >= 2 && bestMatch != null) {
            return bestMatch
        }
    }
    
    // Fallback: first cached or first available
    return pool.firstOrNull() ?: streams.firstOrNull()
}
```

### Why This Matters

When a user starts watching a series with a specific release (e.g., "YIFY 1080p"), the next
episode should use the same release group for consistent quality, encoding, and subtitle 
availability. Without this matching, the app might switch to a different release with 
different audio tracks, quality, or subtitles between episodes.

---

## Episode Chain Calculation

After resolving the next episode, calculate the episode AFTER that (for the "Up Next" overlay):

```kotlin
/**
 * Given the current set of all episodes, find the episode that comes after [nextEpisodeId].
 */
fun calculateFutureNextEpisode(
    allEpisodes: List<EpisodeInfo>,
    nextEpisodeId: String
): EpisodeInfo? {
    // Sort episodes: Season 1+ first (in order), Season 0 (specials) last
    val sorted = allEpisodes.sortedWith(
        compareBy<EpisodeInfo> { if (it.season == 0) Int.MAX_VALUE else it.season }
            .thenBy { it.episode }
    )
    
    val currentIndex = sorted.indexOfFirst { it.id == nextEpisodeId }
    if (currentIndex == -1 || currentIndex + 1 >= sorted.size) return null
    
    return sorted[currentIndex + 1]
}
```

### Season 0 Handling

Season 0 episodes (specials) are sorted LAST:
```
Season 1, Episode 1
Season 1, Episode 2
...
Season 2, Episode 1
...
Season 0, Episode 1  ← specials go to the end
Season 0, Episode 2
```

---

## Next Episode Data Flow

### NowPlayingItem Updates

When next episode loads, update `NowPlayingItem` with:

```kotlin
nowPlaying = NowPlayingItem(
    id = current.id,                          // Same series ID
    type = current.type,                      // Still "series"
    title = current.title,                    // Same series title
    streamUrl = resolvedUrl,                  // NEW resolved URL
    posterUrl = current.posterUrl,            // Same poster
    season = nextSeason,                      // NEW season
    episode = nextEpisodeNum,                 // NEW episode number
    episodeName = nextEpName,                 // NEW episode name
    
    // Metadata stays from current series
    year = current.year,
    genres = current.genres,
    imdbRating = current.imdbRating,
    runtime = current.runtime,
    contentDescription = current.contentDescription,
    cast = current.cast,
    logo = current.logo,
    episodeOverview = /* looked up from allEpisodes */,
    
    background = current.background,
    
    // NEW stream info
    streamInfoHash = effectiveInfoHash,
    streamFileIdx = bestStream.fileIdx,
    streamSourceUrl = bestStream.url,
    streamName = bestStream.title ?: bestStream.name,
    
    // FUTURE next episode (the one after this next one)
    nextEpisodeId = futureNextEpisode?.id,
    nextEpisodeSeason = futureNextEpisode?.season,
    nextEpisodeNumber = futureNextEpisode?.episode,
    nextEpisodeName = futureNextEpisode?.title,
    
    // Keep full episode list
    allEpisodes = current.allEpisodes
)
```

### Watch History Update

```kotlin
val playbackInfo = PlaybackContentInfo(
    contentId = current.id,           // Series IMDB ID
    contentType = "series",
    contentName = current.title,      // Series name
    poster = current.posterUrl,
    background = current.background,
    season = nextSeason,              // Next episode's season
    episode = nextEpisodeNum,         // Next episode's number
    episodeId = nextEpisodeId,        // Next episode's stream ID
    episodeTitle = nextEpName
)
```

---

## Double-Call Guard

Prevent duplicate `playNextEpisode()` calls:

```kotlin
private var isLoadingNextEpisode = false

fun playNextEpisode() {
    if (isLoadingNextEpisode) {
        Log.d("NextEpisode", "Already loading next episode, ignoring duplicate call")
        return
    }
    isLoadingNextEpisode = true
    
    viewModelScope.launch {
        try {
            // ... resolution logic ...
        } finally {
            isLoadingNextEpisode = false
        }
    }
}
```

---

## Cache Check Wait

After fetching streams, wait for Real-Debrid cache check (up to 3 seconds):

```kotlin
// Wait for cache check (up to 3 seconds)
var waitTime = 0
while (streamViewModel.isCheckingAvailability && waitTime < 30) {
    delay(100) // 100ms intervals
    waitTime++
}
// Total max wait: 30 × 100ms = 3 seconds
```

---

## InfoHash Extraction from URLs

When the stream doesn't have an explicit `infoHash` field, extract it from the URL:

```kotlin
/**
 * Extract torrent infoHash from a Torrentio resolve URL if present.
 * URLs look like: https://torrentio.strem.fun/.../stream/abc123...40chars.../...
 */
fun extractInfoHash(url: String?): String? {
    if (url == null) return null
    return url.split("/").firstOrNull { component ->
        component.length == 40 && component.all { c ->
            c.isDigit() || c in 'a'..'f' || c in 'A'..'F'
        }
    }
}
```

---

## Episode Picker Integration

When a user selects an episode from the in-player episode picker:

```kotlin
// PlayerScreen passes callback:
onSelectEpisode = { episodeInfo ->
    // Same flow as next episode, but with the selected episode
    viewModelScope.launch {
        val streamViewModel = StreamSelectionViewModel()
        await streamViewModel.loadStreams(
            type = "series",
            id = episodeInfo.id,      // Selected episode's stream ID
            addonBaseUrl = addonUrl,
            realDebridKey = realDebridKey
        )
        // ... same resolution + NowPlaying update logic ...
    }
}
```

---

## Continue Watching Logic

### Grouping Rules

The "Continue Watching" row groups by `contentId`:
- **Movies:** One entry per movie
- **Series:** One entry per series (shows the most recently watched episode)

```kotlin
fun buildContinueWatching(history: List<WatchHistoryItem>): List<WatchHistoryItem> {
    // 1. Filter resumable items (2%–90% progress, not completed)
    val resumable = history.filter { it.shouldResume }
    
    // 2. Group by contentId, keep most recent per group
    val latestByContent = mutableMapOf<String, WatchHistoryItem>()
    for (item in resumable) {
        val existing = latestByContent[item.contentId]
        if (existing == null || item.lastWatchedAt > existing.lastWatchedAt) {
            latestByContent[item.contentId] = item
        }
    }
    
    // 3. Sort by last watched date, limit to 20
    return latestByContent.values
        .sortedByDescending { it.lastWatchedAt }
        .take(20)
}
```

### Next Unwatched Episode (for Resume button on ContentDetail)

```kotlin
/**
 * Find the next unwatched episode for a series.
 * Returns the first in-progress episode, or the first unwatched episode.
 */
fun getNextUnwatchedEpisode(
    seriesId: String,
    allEpisodes: List<Triple<Int, Int, String>>,  // (season, episode, episodeId)
    history: List<WatchHistoryItem>
): Pair<String, Double?>? {  // (episodeId, resumePosition?)
    
    // 1. Check for in-progress episode first
    val inProgress = history.firstOrNull { item ->
        item.contentId == seriesId && item.shouldResume
    }
    
    if (inProgress != null) {
        val match = allEpisodes.firstOrNull { (s, e, _) ->
            s == inProgress.season && e == inProgress.episode
        }
        if (match != null) {
            return Pair(match.third, inProgress.lastPosition)
        }
    }
    
    // 2. Find watched episodes
    val watched = history
        .filter { it.contentId == seriesId && it.isCompleted }
        .mapNotNull { item ->
            val s = item.season ?: return@mapNotNull null
            val e = item.episode ?: return@mapNotNull null
            "$s:$e"
        }
        .toSet()
    
    // 3. Sort episodes (Season 0 last)
    val sorted = allEpisodes.sortedWith(
        compareBy<Triple<Int, Int, String>> { if (it.first == 0) Int.MAX_VALUE else it.first }
            .thenBy { it.second }
    )
    
    // 4. Find first unwatched
    val nextEpisode = sorted.firstOrNull { (s, e, _) ->
        !watched.contains("$s:$e")
    }
    
    return nextEpisode?.let { Pair(it.third, null) }
}
```
