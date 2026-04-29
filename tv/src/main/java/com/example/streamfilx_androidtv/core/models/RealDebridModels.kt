package com.example.streamfilx_androidtv.core.models

import com.google.gson.annotations.SerializedName

data class RDUser(
    val id: Int,
    val username: String,
    val email: String,
    val type: String,
    val premium: Int,
    val expiration: String,
    val points: Int? = null,
    val locale: String? = null,
    val avatar: String? = null,
) {
    val isPremium: Boolean get() = type == "premium"

    val premiumDaysRemaining: Int?
        get() {
            if (!isPremium) return null
            return try {
                val expDate = java.time.Instant.parse(expiration)
                val now = java.time.Instant.now()
                java.time.Duration.between(now, expDate).toDays().toInt()
            } catch (e: Exception) { null }
        }

    val statusDisplay: String
        get() = if (isPremium) {
            "$username (Premium${premiumDaysRemaining?.let { " - ${it}d remaining" } ?: ""})"
        } else {
            "$username (Free)"
        }
}

data class RDDownload(
    val id: String,
    val filename: String,
    val mimeType: String? = null,
    val filesize: Long,
    val link: String,
    val host: String,
    val download: String,
    val streamable: Int? = null,
) {
    val isStreamable: Boolean get() = streamable == 1

    val formattedFilesize: String
        get() {
            val gb = filesize.toDouble() / 1_073_741_824
            return if (gb >= 1) String.format("%.2f GB", gb)
            else String.format("%.0f MB", filesize.toDouble() / 1_048_576)
        }
}

data class RDTorrent(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val host: String,
    val status: String,
    val progress: Int,
    val links: List<String>,
    val files: List<RDFile>?,
    val added: String? = null,
    val ended: String? = null,
    val speed: Int? = null,
    val seeders: Int? = null,
    @SerializedName("original_filename") val originalFilename: String? = null,
) {
    val isReady: Boolean get() = status == "downloaded"

    val isProcessing: Boolean
        get() = status in listOf("magnet_conversion", "downloading", "compressing", "uploading", "queued")

    val isError: Boolean
        get() = status in listOf("error", "dead", "virus", "magnet_error")

    val statusDescription: String
        get() = when (status) {
            "magnet_error" -> "Magnet Error"
            "magnet_conversion" -> "Converting Magnet…"
            "waiting_files_selection" -> "Waiting for File Selection"
            "queued" -> "Queued"
            "downloading" -> "Downloading ($progress%)"
            "downloaded" -> "Downloaded"
            "error" -> "Error"
            "virus" -> "Virus Detected"
            "compressing" -> "Compressing…"
            "uploading" -> "Uploading…"
            "dead" -> "Dead Torrent"
            else -> status.replaceFirstChar { it.uppercase() }
        }
}

data class RDFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int,
) {
    val isSelected: Boolean get() = selected == 1
    val filename: String get() = path.substringAfterLast('/')
}

data class RDTorrentAdded(
    val id: String,
    val uri: String,
)

// ── Error types ───────────────────────────────────────────────────────────────

sealed class StreamResolutionError(message: String) : Exception(message) {
    object UnsupportedStream : StreamResolutionError("This stream type is not supported")
    object InvalidURL : StreamResolutionError("Could not generate a valid playback URL")
    object NoLink : StreamResolutionError("No playable link found in torrent")
    object NotCached : StreamResolutionError("This torrent is not cached on Real-Debrid")
    class TorrentFailed(reason: String) : StreamResolutionError("Torrent failed: $reason")
}

sealed class RealDebridError(message: String) : Exception(message) {
    object NoToken : RealDebridError("Real-Debrid API token not configured")
    object InvalidResponse : RealDebridError("Invalid response from Real-Debrid")
    object Unauthorized : RealDebridError("Real-Debrid authentication failed — check your API key")
    object RateLimited : RealDebridError("Real-Debrid rate limit exceeded — try again in a moment")
    object TorrentNotReady : RealDebridError("Torrent did not become ready in time")
    class HttpError(code: Int) : RealDebridError("HTTP error $code from Real-Debrid")
}
