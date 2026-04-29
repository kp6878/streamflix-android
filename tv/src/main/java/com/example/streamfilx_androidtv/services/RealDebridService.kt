package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.core.models.RDTorrent
import com.example.streamfilx_androidtv.core.models.RDUser
import com.example.streamfilx_androidtv.core.models.RealDebridError
import com.example.streamfilx_androidtv.core.models.Stream
import com.example.streamfilx_androidtv.core.models.StreamResolutionError
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealDebridService @Inject constructor(
    private val api: RealDebridApi,
    private val appPreferences: AppPreferences,
) {

    // ── User / validation ─────────────────────────────────────────────────────

    suspend fun validateApiKey(): Result<RDUser> = withContext(Dispatchers.IO) {
        runCatching { api.getUser() }
    }

    suspend fun isConfigured(): Boolean =
        !appPreferences.realDebridApiKey.first().isNullOrBlank()

    // ── Instant availability (batch) ──────────────────────────────────────────

    suspend fun checkAvailability(streams: List<Stream>): Map<String, Boolean> =
        withContext(Dispatchers.IO) {
            val hashes = streams.mapNotNull { it.infoHash?.lowercase() }.distinct()
            if (hashes.isEmpty()) return@withContext emptyMap()

            runCatching {
                val json = api.checkInstantAvailability(hashes.joinToString("/"))
                hashes.associateWith { hash ->
                    val rdArray = json.getAsJsonObject(hash)?.getAsJsonArray("rd")
                    rdArray != null && rdArray.size() > 0
                }
            }.getOrDefault(emptyMap())
        }

    // ── Stream resolution entry point ─────────────────────────────────────────

    suspend fun resolveStream(stream: Stream): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when {
                stream.url != null -> unrestrictDirectUrl(stream.url)
                stream.infoHash != null -> processTorrent(stream.infoHash, stream.fileIdx)
                else -> throw StreamResolutionError.UnsupportedStream
            }
        }
    }

    // ── Unrestrict a direct HTTP link ─────────────────────────────────────────

    private suspend fun unrestrictDirectUrl(url: String): String {
        val download = api.unrestrictLink(url)
        return download.download.ifBlank { throw StreamResolutionError.InvalidURL }
    }

    // ── 6-step torrent pipeline ───────────────────────────────────────────────

    private suspend fun processTorrent(infoHash: String, fileIdx: Int?): String {
        val magnet = "magnet:?xt=urn:btih:$infoHash"

        // Step 1 — check cache
        val isCached = checkIsCached(infoHash)

        // Step 2 — add magnet
        val added = api.addMagnet(magnet)

        try {
            // Step 3 — select files
            val files = fileIdx?.toString() ?: "all"
            api.selectFiles(added.id, files)

            // Step 4 — wait for torrent ready
            val torrent = waitForTorrent(added.id, isCached)

            // Step 5 — pick the right link
            val link = selectLink(torrent, fileIdx)
                ?: throw StreamResolutionError.NoLink

            // Step 6 — unrestrict
            val download = api.unrestrictLink(link)
            return download.download.ifBlank { throw StreamResolutionError.InvalidURL }
        } catch (e: Exception) {
            // Clean up torrent from account on any error
            runCatching { api.deleteTorrent(added.id) }
            throw e
        }
    }

    private suspend fun checkIsCached(infoHash: String): Boolean {
        return runCatching {
            val json = api.checkInstantAvailability(infoHash.lowercase())
            val rdArray = json.getAsJsonObject(infoHash.lowercase())?.getAsJsonArray("rd")
            rdArray != null && rdArray.size() > 0
        }.getOrDefault(false)
    }

    private suspend fun waitForTorrent(
        id: String,
        isCached: Boolean,
        maxWaitMs: Long = 30_000L,
    ): RDTorrent {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < maxWaitMs) {
            val torrent = api.getTorrentInfo(id)

            if (torrent.isReady) return torrent

            if (torrent.isError) {
                throw StreamResolutionError.TorrentFailed(torrent.statusDescription)
            }

            // If not cached and actively downloading, bail — user would wait for full DL
            if (!isCached && torrent.isProcessing) {
                throw StreamResolutionError.NotCached
            }

            delay(1_000L)
        }
        throw RealDebridError.TorrentNotReady
    }

    private fun selectLink(torrent: RDTorrent, fileIdx: Int?): String? {
        if (torrent.links.isEmpty()) return null

        // Specific file index requested
        if (fileIdx != null && fileIdx < torrent.links.size) {
            return torrent.links[fileIdx]
        }

        // Find the largest video file by extension
        val videoExts = setOf("mkv", "mp4", "avi", "mov", "webm", "m4v")
        torrent.files?.let { files ->
            val largest = files
                .filter { f -> videoExts.any { f.filename.lowercase().endsWith(".$it") } }
                .maxByOrNull { it.bytes }

            if (largest != null) {
                val idx = files.indexOfFirst { it.id == largest.id }
                if (idx >= 0 && idx < torrent.links.size) return torrent.links[idx]
            }
        }

        return torrent.links.firstOrNull()
    }
}
