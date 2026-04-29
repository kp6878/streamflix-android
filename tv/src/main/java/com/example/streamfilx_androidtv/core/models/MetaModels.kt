package com.example.streamfilx_androidtv.core.models

import com.google.gson.annotations.SerializedName

// ── Catalog response ──────────────────────────────────────────────────────────

data class CatalogResponse(val metas: List<MetaPreview> = emptyList())

// ── Meta response ─────────────────────────────────────────────────────────────

data class MetaResponse(val meta: MetaItem)

// ── Lightweight card model (from catalog) ─────────────────────────────────────

data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val posterShape: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null,
) {
    val year: String? get() = releaseInfo?.take(4)

    val ratingDisplay: String? get() = imdbRating?.let { "★ $it" }
}

// ── Full metadata (from meta endpoint) ───────────────────────────────────────

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String>? = null,
    val cast: List<String>? = null,
    val director: List<String>? = null,
    val videos: List<Video>? = null,
    val links: List<MetaLink>? = null,
) {
    val year: String? get() = releaseInfo?.take(4)

    val isSeries: Boolean get() = type == "series"

    val sortedVideos: List<Video>
        get() = videos?.sortedWith(
            compareBy({ it.season ?: Int.MAX_VALUE }, { it.episodeNumber ?: Int.MAX_VALUE })
        ) ?: emptyList()

    val availableSeasons: List<Int>
        get() = videos
            ?.mapNotNull { it.season }
            ?.distinct()
            ?.sortedWith(compareBy { if (it == 0) Int.MAX_VALUE else it })
            ?: emptyList()

    fun episodesForSeason(season: Int): List<Video> =
        videos?.filter { it.season == season }
            ?.sortedBy { it.episodeNumber }
            ?: emptyList()

    fun toPreview(): MetaPreview = MetaPreview(
        id = id,
        type = type,
        name = name,
        poster = poster,
        background = background,
        logo = logo,
        description = description,
        releaseInfo = releaseInfo,
        imdbRating = imdbRating,
        genres = genres,
    )
}

// ── Episode video entry ───────────────────────────────────────────────────────

data class Video(
    val id: String,
    val title: String? = null,
    val season: Int? = null,
    @SerializedName("episode") val episodeNumber: Int? = null,
    val released: String? = null,
    val overview: String? = null,
    val thumbnail: String? = null,
) {
    val episodeString: String?
        get() {
            val s = season ?: return null
            val e = episodeNumber ?: return null
            return String.format("S%02dE%02d", s, e)
        }

    val displayTitle: String
        get() = title?.takeIf { it.isNotEmpty() }
            ?: episodeNumber?.let { "Episode $it" }
            ?: id
}

// ── Metadata link ─────────────────────────────────────────────────────────────

data class MetaLink(
    val name: String? = null,
    val category: String? = null,
    val url: String? = null,
)
