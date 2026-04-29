package com.example.streamfilx_androidtv.core.models

// ── Episode info (used inside player and detail screen) ───────────────────────

data class EpisodeInfo(
    val id: String,             // e.g. "tt1234567:1:1" for Stremio stream ID
    val season: Int,
    val episode: Int,
    val title: String? = null,
    val thumbnail: String? = null,
    val overview: String? = null,
) {
    val episodeString: String get() = String.format("S%02dE%02d", season, episode)
    val displayTitle: String get() = title?.takeIf { it.isNotEmpty() } ?: "Episode $episode"
}

// ── Now-playing item passed to the player overlay ─────────────────────────────

data class NowPlayingItem(
    val id: String,
    val type: String,                   // "movie" or "series"
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeName: String? = null,
    val year: String? = null,
    val genres: List<String>? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val contentDescription: String? = null,
    val cast: List<String>? = null,
    val logo: String? = null,
    val episodeOverview: String? = null,
    val streamQuality: String? = null,
    val background: String? = null,
    var streamInfoHash: String? = null,
    var streamFileIdx: Int? = null,
    var streamSourceUrl: String? = null,
    var streamName: String? = null,
    var nextEpisodeId: String? = null,
    var nextEpisodeSeason: Int? = null,
    var nextEpisodeNumber: Int? = null,
    var nextEpisodeName: String? = null,
    var allEpisodes: List<EpisodeInfo>? = null,
) {
    val episodeInfo: String?
        get() {
            val s = season ?: return null
            val e = episode ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    val playerDisplayTitle: String
        get() = if (type == "series" && episodeInfo != null) {
            buildString {
                append(title)
                append(" • ")
                append(episodeInfo)
                episodeName?.takeIf { it.isNotEmpty() }?.let { append(" • $it") }
            }
        } else title

    val hasNextEpisode: Boolean get() = nextEpisodeId != null

    val nextEpisodeInfo: String?
        get() {
            val s = nextEpisodeSeason ?: return null
            val e = nextEpisodeNumber ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    val qualityBadge: String?
        get() {
            val q = streamQuality?.uppercase() ?: return null
            return when {
                q.contains("2160") || q.contains("4K") || q.contains("UHD") -> "4K"
                q.contains("1080") || q.contains("FHD") -> "FHD"
                q.contains("720") || q.contains("HD") -> "HD"
                q.contains("480") || q.contains("SD") -> "SD"
                else -> q
            }
        }

    val availableSeasons: List<Int>
        get() = allEpisodes
            ?.map { it.season }
            ?.distinct()
            ?.sortedWith(compareBy { if (it == 0) Int.MAX_VALUE else it })
            ?: emptyList()

    fun episodesForSeason(s: Int): List<EpisodeInfo> =
        allEpisodes?.filter { it.season == s }?.sortedBy { it.episode } ?: emptyList()

    fun isCurrentEpisode(ep: EpisodeInfo): Boolean =
        ep.season == season && ep.episode == episode
}

// ── Watch history domain object (wraps WatchHistoryEntity) ───────────────────

data class WatchHistoryItem(
    val uniqueId: String,
    val contentId: String,
    val contentType: String,
    val contentName: String,
    val poster: String?,
    val background: String?,
    val thumbnail: String?,
    val season: Int?,
    val episode: Int?,
    val episodeTitle: String?,
    val lastPosition: Double,
    val duration: Double,
    val progress: Double,
    val isCompleted: Boolean,
    val lastWatchedAt: Long,
    val lastStreamUrl: String?,
    val lastStreamInfoHash: String?,
    val lastStreamFileIdx: Int?,
) {
    val shouldResume: Boolean
        get() = !isCompleted && progress > 0.02 && progress < 0.90

    val timeRemaining: String
        get() {
            val remaining = ((duration - lastPosition) / 60).toInt()
            return if (remaining > 0) "$remaining min left" else ""
        }

    val episodeInfo: String?
        get() {
            val s = season ?: return null
            val e = episode ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    companion object {
        fun buildUniqueId(contentId: String, season: Int?, episode: Int?): String =
            if (season != null && episode != null) {
                "$contentId:S${String.format("%02d", season)}E${String.format("%02d", episode)}"
            } else contentId
    }
}

// ── Minimal context passed when launching playback ────────────────────────────

data class PlaybackContentInfo(
    val contentId: String,
    val contentType: String,
    val contentName: String,
    val poster: String?,
    val background: String?,
    val season: Int?,
    val episode: Int?,
    val episodeId: String?,
    val episodeTitle: String?,
    val thumbnail: String? = null,
)
