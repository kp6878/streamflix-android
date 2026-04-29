package com.example.streamfilx_androidtv.core.network

import com.example.streamfilx_androidtv.core.models.AudioCodec
import com.example.streamfilx_androidtv.core.models.Stream
import com.example.streamfilx_androidtv.core.models.StreamQuality

// ── Quality detection ─────────────────────────────────────────────────────────

object StreamQualityParser {

    fun detectQuality(stream: Stream): StreamQuality {
        val text = searchText(stream)
        return when {
            text.containsAny("4k", "2160p", "uhd") -> StreamQuality.UHD_4K
            text.containsAny("1080p", "fhd") -> StreamQuality.FHD_1080P
            text.containsAny("720p", "hd") -> StreamQuality.HD_720P
            text.containsAny("480p", "sd") -> StreamQuality.SD_480P
            text.containsAny("cam", " ts ", "scr", "screener", "telesync", "telecine") -> StreamQuality.CAM
            else -> StreamQuality.UNKNOWN
        }
    }

    private fun searchText(stream: Stream) = listOfNotNull(
        stream.title, stream.name, stream.description, stream.behaviorHints?.filename
    ).joinToString(" ").lowercase()

    private fun String.containsAny(vararg terms: String) =
        terms.any { contains(it, ignoreCase = true) }
}

// ── Audio codec detection ─────────────────────────────────────────────────────

object AudioCodecParser {

    fun detectAudioCodec(stream: Stream): AudioCodec? {
        val text = searchText(stream)
        return when {
            text.contains("truehd", true) && text.contains("atmos", true) -> AudioCodec.TRUEHD_ATMOS
            text.contains("truehd", true) -> AudioCodec.TRUEHD
            text.containsAny("dts-hd", "dtshd", "dts hd", "dts-hd ma", "dts-hd.ma") -> AudioCodec.DTS_HD_MA
            text.contains("atmos", true) -> AudioCodec.ATMOS
            text.contains("dts", true) && !text.contains("dts-hd", true) -> AudioCodec.DTS
            text.containsAny("dd+", "ddp", "e-ac3", "eac3", "dd plus", "dolby digital plus") -> AudioCodec.DD_PLUS
            text.containsAny("ac3", "dd5.1", "dd 5.1", "dolby digital") -> AudioCodec.DD_5_1
            text.contains("aac", true) -> AudioCodec.AAC
            else -> null
        }
    }

    private fun searchText(stream: Stream) = listOfNotNull(
        stream.title, stream.name, stream.description, stream.behaviorHints?.filename
    ).joinToString(" ")

    private fun String.containsAny(vararg terms: String) =
        terms.any { contains(it, ignoreCase = true) }
}

// ── File size parsing ─────────────────────────────────────────────────────────

object FileSizeParser {

    private val SIZE_REGEX = Regex("""(\d+\.?\d*)\s*(GB|MB|TB)""", RegexOption.IGNORE_CASE)

    fun parseFileSize(stream: Stream): String? {
        stream.behaviorHints?.videoSize?.let { bytes ->
            if (bytes > 0) return formatBytes(bytes)
        }
        val match = SIZE_REGEX.find(searchText(stream)) ?: return null
        return "${match.groupValues[1]} ${match.groupValues[2].uppercase()}"
    }

    fun parseSizeBytes(stream: Stream): Long? {
        stream.behaviorHints?.videoSize?.let { if (it > 0) return it }
        val match = SIZE_REGEX.find(searchText(stream)) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        return when (match.groupValues[2].uppercase()) {
            "TB" -> (value * 1024 * 1024 * 1024 * 1024).toLong()
            "GB" -> (value * 1024 * 1024 * 1024).toLong()
            "MB" -> (value * 1024 * 1024).toLong()
            else -> null
        }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 * 1024 ->
            String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024))
        bytes >= 1024L * 1024 * 1024 ->
            String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 ->
            String.format("%.0f MB", bytes / (1024.0 * 1024))
        else ->
            String.format("%.0f KB", bytes / 1024.0)
    }

    private fun searchText(stream: Stream) = listOfNotNull(
        stream.title, stream.name, stream.description, stream.behaviorHints?.filename
    ).joinToString(" ")
}

// ── Language detection ────────────────────────────────────────────────────────

object LanguageParser {

    data class DetectedLanguage(val code: String, val name: String, val flag: String)

    private val LANGUAGE_MAP = listOf(
        Triple("ENG", "English", "🇬🇧"), Triple("SPA", "Spanish", "🇪🇸"),
        Triple("FRE", "French", "🇫🇷"),  Triple("GER", "German", "🇩🇪"),
        Triple("ITA", "Italian", "🇮🇹"), Triple("POR", "Portuguese", "🇧🇷"),
        Triple("RUS", "Russian", "🇷🇺"), Triple("JAP", "Japanese", "🇯🇵"),
        Triple("KOR", "Korean", "🇰🇷"),  Triple("CHI", "Chinese", "🇨🇳"),
        Triple("HIN", "Hindi", "🇮🇳"),   Triple("ARA", "Arabic", "🇸🇦"),
        Triple("TUR", "Turkish", "🇹🇷"), Triple("POL", "Polish", "🇵🇱"),
        Triple("DUT", "Dutch", "🇳🇱"),
    )

    fun detectLanguages(stream: Stream): List<DetectedLanguage> {
        val text = listOfNotNull(
            stream.title, stream.name, stream.description, stream.behaviorHints?.filename
        ).joinToString(" ").uppercase()

        if (text.contains("MULTI") || text.contains("DUAL")) {
            return listOf(DetectedLanguage("MULTI", "Multiple", "🌐"))
        }
        return LANGUAGE_MAP
            .filter { (code, _, _) -> text.contains(code) }
            .map { (code, name, flag) -> DetectedLanguage(code, name, flag) }
    }
}

// ── Stream sorting ────────────────────────────────────────────────────────────

fun sortStreams(streams: List<Stream>, cachedHashes: Set<String>): List<Stream> =
    streams.sortedWith(
        compareBy<Stream> { if (cachedHashes.contains(it.infoHash?.lowercase())) 0 else 1 }
            .thenBy { StreamQualityParser.detectQuality(it).sortOrder }
            .thenByDescending { FileSizeParser.parseSizeBytes(it) ?: 0L }
    )

// ── Torrentio URL builder ─────────────────────────────────────────────────────

fun buildTorrentioUrl(
    realDebridKey: String,
    qualityFilter: List<String> = listOf("scr", "cam"),
    sorting: String = "qualitysize",
): String {
    val config = buildString {
        append("realdebrid=$realDebridKey")
        if (qualityFilter.isNotEmpty()) append("|qualityfilter=${qualityFilter.joinToString(",")}")
        append("|sort=$sorting")
    }
    return "https://torrentio.strem.fun/$config"
}

// ── InfoHash extraction ───────────────────────────────────────────────────────

fun extractInfoHash(url: String?): String? = url?.split("/")?.firstOrNull { segment ->
    segment.length == 40 && segment.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
}
