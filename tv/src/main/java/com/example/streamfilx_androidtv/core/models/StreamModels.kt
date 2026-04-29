package com.example.streamfilx_androidtv.core.models

data class StreamResponse(val streams: List<Stream> = emptyList())

data class Stream(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val behaviorHints: StreamBehaviorHints? = null,
) {
    val isTorrent: Boolean get() = infoHash != null
    val isHttp: Boolean get() = url != null && infoHash == null
    val displayName: String get() = name ?: title ?: "Unknown"

    val fileSizeBytes: Long? get() = behaviorHints?.videoSize

    val magnetUri: String? get() = infoHash?.let { "magnet:?xt=urn:btih:$it" }
}

data class StreamBehaviorHints(
    val countryWhitelist: List<String>? = null,
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val proxyHeaders: ProxyHeaders? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val videoHash: String? = null,
)

data class ProxyHeaders(
    val request: Map<String, String>? = null,
    val response: Map<String, String>? = null,
)

// ── Quality enum (used by StreamQualityParser) ────────────────────────────────

enum class StreamQuality(val display: String, val sortOrder: Int) {
    UHD_4K("4K", 0),
    FHD_1080P("FHD", 1),
    HD_720P("HD", 2),
    SD_480P("SD", 3),
    CAM("CAM", 4),
    UNKNOWN("?", 5);

    val badgeColor: Long
        get() = when (this) {
            UHD_4K -> 0xFF9C27B0L
            FHD_1080P -> 0xFF2196F3L
            HD_720P -> 0xFF4CAF50L
            SD_480P -> 0xFFFF9800L
            CAM -> 0xFFF44336L
            UNKNOWN -> 0xFF9E9E9EL
        }
}

// ── Audio codec enum (used by AudioCodecParser) ───────────────────────────────

enum class AudioCodec(val display: String) {
    TRUEHD_ATMOS("TrueHD Atmos"),
    TRUEHD("TrueHD"),
    DTS_HD_MA("DTS-HD MA"),
    ATMOS("Atmos"),
    DTS("DTS"),
    DD_PLUS("DD+"),
    DD_5_1("DD 5.1"),
    AAC("AAC"),
}
