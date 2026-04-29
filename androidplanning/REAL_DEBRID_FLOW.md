# Real-Debrid Resolution Flow — Complete Technical Specification

Step-by-step stream resolution logic, torrent processing pipeline,
file selection algorithm, and error handling.

---

## Resolution Flow Overview

```
User taps stream
       │
       ▼
┌─────────────────────────┐
│ Has direct URL?         │──Yes──▶ unrestrict(url) → playback URL
│ (stream.url non-empty)  │
└─────────────────────────┘
       │ No
       ▼
┌─────────────────────────┐
│ Has infoHash?           │──Yes──▶ processTorrent(hash, fileIdx) → playback URL
│ (stream.infoHash set)   │
└─────────────────────────┘
       │ No
       ▼
   throw UnsupportedStream
```

---

## Step-by-Step Torrent Processing

### 1. Check Instant Availability

```kotlin
// GET /torrents/instantAvailability/{hash}
val availability = api.checkInstantAvailability(listOf(infoHash))
val isCached = availability[infoHash.lowercase()] ?: false
```

**Response Format (JSON):**
```json
{
  "abc123hash...": {
    "rd": [
      {
        "1": { "filename": "movie.mkv", "filesize": 2147483648 }
      }
    ]
  }
}
```

Parse: hash is cached if `json[hash]["rd"]` is a non-empty array.

### 2. Add Magnet

```kotlin
// POST /torrents/addMagnet
// Body: magnet=magnet:?xt=urn:btih:{infoHash}
val magnet = "magnet:?xt=urn:btih:$infoHash"
val added = api.addMagnet(magnet)
// Returns: { "id": "XXXXX", "uri": "magnet:..." }
```

### 3. Select Files

```kotlin
// POST /torrents/selectFiles/{id}
// Body: files={fileIdx} or files=all
val files = fileIdx?.toString() ?: "all"
api.selectFiles(torrentId = added.id, fileIds = files)
```

### 4. Wait for Torrent Ready

Poll torrent status with 1-second intervals, max 30 seconds:

```kotlin
suspend fun waitForTorrent(id: String, isCached: Boolean, maxWait: Duration = 30.seconds): RDTorrent {
    val startTime = System.currentTimeMillis()
    
    while (System.currentTimeMillis() - startTime < maxWait.inWholeMilliseconds) {
        val torrent = api.getTorrentInfo(id)
        
        // Success: torrent is downloaded
        if (torrent.status == "downloaded") {
            return torrent
        }
        
        // Failure states: clean up and throw
        if (torrent.status in listOf("error", "dead", "virus")) {
            try { api.deleteTorrent(id) } catch (_: Exception) {}
            throw StreamResolutionError.TorrentFailed(torrent.statusDescription)
        }
        
        // If NOT cached and still processing, bail early
        // (uncached torrents would need to actually download)
        if (!isCached && torrent.isProcessing) {
            throw StreamResolutionError.NotCached
        }
        
        delay(1000) // 1 second between polls
    }
    
    throw RealDebridError.TorrentNotReady
}
```

**Torrent Status Values:**
| Status | Description | Action |
|--------|-------------|--------|
| `magnet_error` | Magnet link invalid | Throw error |
| `magnet_conversion` | Converting magnet | Wait (processing) |
| `waiting_files_selection` | Need to select files | Select files |
| `queued` | Queued for download | Wait |
| `downloading` | Active download | Wait (processing) |
| `downloaded` | ✅ Ready | Return torrent |
| `error` | Processing error | Delete + throw |
| `virus` | Virus detected | Delete + throw |
| `compressing` | Being compressed | Wait (processing) |
| `uploading` | Being uploaded | Wait (processing) |
| `dead` | Dead torrent | Delete + throw |

### 5. Select Link from Torrent

```kotlin
fun selectLink(torrent: RDTorrent, fileIdx: Int?): String? {
    if (torrent.links.isEmpty()) return null
    
    // If specific file index requested and valid
    if (fileIdx != null && fileIdx < torrent.links.size) {
        return torrent.links[fileIdx]
    }
    
    // Find largest video file
    val videoExtensions = listOf("mkv", "mp4", "avi", "mov", "webm", "m4v")
    torrent.files?.let { files ->
        val videoFiles = files.filter { file ->
            videoExtensions.any { ext -> file.filename.lowercase().endsWith(".$ext") }
        }
        
        val largest = videoFiles.maxByOrNull { it.bytes }
        if (largest != null) {
            val index = files.indexOfFirst { it.id == largest.id }
            if (index >= 0 && index < torrent.links.size) {
                return torrent.links[index]
            }
        }
    }
    
    // Fallback: first link
    return torrent.links.firstOrNull()
}
```

### 6. Unrestrict Link

```kotlin
// POST /unrestrict/link
// Body: link={torrentLink}
val download = api.unrestrictLink(link)
val playbackUrl = URL(download.download) // ← This is the direct playback URL
```

---

## API Endpoints Reference

**Base URL:** `https://api.real-debrid.com/rest/1.0`
**Auth Header:** `Authorization: Bearer {apiKey}`
**Content-Type (POST):** `application/x-www-form-urlencoded`
**Rate Limit:** 250 requests/minute

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/user` | Get user info (username, premium status, expiration) |
| POST | `/unrestrict/link` | Convert hoster link to direct download URL |
| GET | `/torrents/instantAvailability/{hash1}/{hash2}/...` | Check if torrents are cached |
| POST | `/torrents/addMagnet` | Add magnet link to user's torrents |
| POST | `/torrents/selectFiles/{id}` | Select files from torrent |
| GET | `/torrents/info/{id}` | Get torrent status and file list |
| DELETE | `/torrents/delete/{id}` | Delete torrent from account |
| GET | `/torrents` | List user's torrents |
| GET | `/downloads` | List user's downloads |
| DELETE | `/downloads/delete/{id}` | Delete download |

---

## Instant Availability Batch Check

For the stream list, check all torrent streams' cache status at once:

```kotlin
suspend fun checkAvailability(streams: List<Stream>): Map<String, Boolean> {
    val hashes = streams.mapNotNull { it.infoHash }.distinct()
    if (hashes.isEmpty()) return emptyMap()
    
    // Batch request: all hashes in single URL path
    // GET /torrents/instantAvailability/{hash1}/{hash2}/{hash3}
    return try {
        api.checkInstantAvailability(hashes)
    } catch (e: Exception) {
        Log.e("RealDebrid", "Availability check failed: ${e.message}")
        emptyMap()
    }
}
```

**Important:** This runs in the background AFTER streams are displayed:
1. Streams appear immediately with quality badges
2. Cache check runs asynchronously
3. Green "Cached" badges appear once check completes
4. Cached streams are re-sorted to top

---

## Error Types

```kotlin
sealed class StreamResolutionError(message: String) : Exception(message) {
    object UnsupportedStream : StreamResolutionError("This stream type is not supported")
    object InvalidURL : StreamResolutionError("Could not generate valid playback URL")
    object NoLink : StreamResolutionError("No playable link found in torrent")
    object NotCached : StreamResolutionError(
        "This torrent is not cached. It would need to download first."
    )
    class TorrentFailed(reason: String) : StreamResolutionError("Torrent failed: $reason")
}

sealed class RealDebridError(message: String) : Exception(message) {
    object NoToken : RealDebridError("Real-Debrid API token not configured")
    object InvalidResponse : RealDebridError("Invalid response from Real-Debrid")
    object Unauthorized : RealDebridError("Real-Debrid authentication failed")
    object Forbidden : RealDebridError("Access forbidden")
    object RateLimited : RealDebridError("Rate limit exceeded. Please wait a moment.")
    class HttpError(code: Int) : RealDebridError("HTTP error: $code")
    object TorrentNotReady : RealDebridError("Torrent is not ready for streaming")
}
```

### RD API Error Codes

| Code | Meaning |
|------|---------|
| 1 | Missing parameter |
| 2 | Bad parameter value |
| 3 | Unknown method |
| 5 | Slow down (rate limit) |
| 8 | Bad token |
| 9 | Permission denied |
| 16 | Unsupported hoster |
| 21 | Too many active downloads |
| 22 | IP address not allowed |
| 23 | Traffic exhausted |
| 33 | Torrent already active |
| 34 | Too many requests |
| 35 | Infringing file |
| 36 | Fair usage limit |

---

## Retrofit Interface

```kotlin
interface RealDebridApi {
    @GET("user")
    suspend fun getUser(): RDUser

    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(
        @Field("link") link: String,
        @Field("password") password: String? = null,
        @Field("remote") remote: String? = null
    ): RDDownload

    @GET("torrents/instantAvailability/{hashes}")
    suspend fun checkInstantAvailability(
        @Path("hashes", encoded = true) hashes: String
    ): JsonObject  // Needs manual parsing due to dynamic keys

    @FormUrlEncoded
    @POST("torrents/addMagnet")
    suspend fun addMagnet(@Field("magnet") magnet: String): RDTorrentAdded

    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(
        @Path("id") id: String,
        @Field("files") files: String
    ): ResponseBody

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(@Path("id") id: String): RDTorrent

    @DELETE("torrents/delete/{id}")
    suspend fun deleteTorrent(@Path("id") id: String): ResponseBody

    @GET("torrents")
    suspend fun getTorrents(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null
    ): List<RDTorrent>

    @GET("downloads")
    suspend fun getDownloads(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null
    ): List<RDDownload>
}
```

### OkHttp Auth Interceptor

```kotlin
class RealDebridAuthInterceptor(
    private val prefs: AppPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = runBlocking { prefs.realDebridApiKey.first() }
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${apiKey ?: ""}")
            .build()
        return chain.proceed(request)
    }
}
```

---

## RD User Validation Flow (Settings)

```kotlin
// User taps "Validate Key"
suspend fun validateApiKey(key: String): Result<RDUser> {
    return try {
        // Temporarily set token for this request
        val user = api.getUser()  // GET /user
        
        // Show result:
        // "username (Premium - X days remaining)"
        // or "username (Free)"
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)  // Show "Invalid API key" error
    }
}
```

### Premium Status Display

```kotlin
val statusText = if (user.isPremium) {
    "${user.username} (Premium${user.premiumDaysRemaining?.let { " - ${it}d remaining" } ?: ""})"
} else {
    "${user.username} (Free)"
}
```
