# Stream Parsing — Quality, Audio, Size & Language Detection

All stream metadata parsing logic extracted from the iOS source code.
These parsers analyze the `name`, `title`, `description`, and `filename` fields of Stremio stream objects.

---

## Quality Detection

Parses video quality from stream text fields.

### Detection Order

Check fields in order: `title` → `name` → `description` → `behaviorHints.filename`

### Regex Patterns

```kotlin
object StreamQualityParser {
    
    fun detectQuality(stream: Stream): StreamQuality {
        val text = buildSearchText(stream)
        
        return when {
            text.containsAny("4k", "2160p", "uhd") -> StreamQuality.UHD_4K
            text.containsAny("1080p", "fhd") -> StreamQuality.FHD_1080P
            text.containsAny("720p", "hd") -> StreamQuality.HD_720P
            text.containsAny("480p", "sd") -> StreamQuality.SD_480P
            text.containsAny("cam", " ts ", "scr", "screener", "telesync", "telecine") -> StreamQuality.CAM
            else -> StreamQuality.UNKNOWN
        }
    }
    
    private fun buildSearchText(stream: Stream): String {
        return listOfNotNull(
            stream.title,
            stream.name,
            stream.description,
            stream.behaviorHints?.filename
        ).joinToString(" ").lowercase()
    }
    
    private fun String.containsAny(vararg terms: String): Boolean =
        terms.any { this.contains(it, ignoreCase = true) }
}

enum class StreamQuality(val display: String, val sortOrder: Int) {
    UHD_4K("4K", 0),
    FHD_1080P("1080p", 1),
    HD_720P("HD", 2),
    SD_480P("SD", 3),
    CAM("CAM", 4),
    UNKNOWN("Unknown", 5);

    val badgeColor: Long
        get() = when (this) {
            UHD_4K -> 0xFF9C27B0      // Purple
            FHD_1080P -> 0xFF2196F3   // Blue
            HD_720P -> 0xFF4CAF50     // Green
            SD_480P -> 0xFFFF9800     // Orange
            CAM -> 0xFFF44336         // Red
            UNKNOWN -> 0xFF9E9E9E     // Gray
        }
}
```

---

## Audio Codec Detection

Parses audio codec information from stream text.

```kotlin
object AudioCodecParser {
    
    fun detectAudioCodec(stream: Stream): AudioCodec? {
        val text = buildSearchText(stream)
        
        return when {
            // TrueHD + Atmos (must check combo first)
            text.contains("truehd", true) && text.contains("atmos", true) -> AudioCodec.TRUEHD_ATMOS
            // TrueHD alone
            text.contains("truehd", true) -> AudioCodec.TRUEHD
            // DTS-HD MA (various spellings)
            text.containsAny("dts-hd", "dtshd", "dts hd", "dts-hd ma", "dts-hd.ma") -> AudioCodec.DTS_HD_MA
            // Dolby Atmos alone
            text.contains("atmos", true) -> AudioCodec.ATMOS
            // DTS (generic)
            text.contains("dts", true) && !text.contains("dts-hd", true) -> AudioCodec.DTS
            // Dolby Digital Plus / E-AC3
            text.containsAny("dd+", "ddp", "e-ac3", "eac3", "dd plus", "dolby digital plus") -> AudioCodec.DD_PLUS
            // Dolby Digital / AC3
            text.containsAny("ac3", "dd5.1", "dd 5.1", "dolby digital") -> AudioCodec.DD_5_1
            // AAC
            text.contains("aac", true) -> AudioCodec.AAC
            else -> null
        }
    }
    
    private fun buildSearchText(stream: Stream): String {
        return listOfNotNull(
            stream.title,
            stream.name,
            stream.description,
            stream.behaviorHints?.filename
        ).joinToString(" ")
    }
    
    private fun String.containsAny(vararg terms: String): Boolean =
        terms.any { this.contains(it, ignoreCase = true) }
}

enum class AudioCodec(val display: String) {
    TRUEHD_ATMOS("TrueHD Atmos"),
    TRUEHD("TrueHD"),
    DTS_HD_MA("DTS-HD MA"),
    ATMOS("Atmos"),
    DTS("DTS"),
    DD_PLUS("DD+"),
    DD_5_1("DD 5.1"),
    AAC("AAC")
}
```

---

## File Size Parsing

Extracts file size from stream metadata.

```kotlin
object FileSizeParser {
    
    private val SIZE_REGEX = Regex("""(\d+\.?\d*)\s*(GB|MB|TB)""", RegexOption.IGNORE_CASE)
    
    /**
     * Returns formatted file size string (e.g., "2.1 GB") or null.
     * Checks in order:
     * 1. behaviorHints.videoSize (bytes) → format to human-readable
     * 2. Regex match in title, name, description, filename
     */
    fun parseFileSize(stream: Stream): String? {
        // 1. Check behaviorHints.videoSize first (exact bytes)
        stream.behaviorHints?.videoSize?.let { bytes ->
            if (bytes > 0) return formatBytes(bytes)
        }
        
        // 2. Regex scan across text fields
        val text = listOfNotNull(
            stream.title,
            stream.name,
            stream.description,
            stream.behaviorHints?.filename
        ).joinToString(" ")
        
        val match = SIZE_REGEX.find(text) ?: return null
        val value = match.groupValues[1]
        val unit = match.groupValues[2].uppercase()
        
        return "$value $unit"
    }
    
    /**
     * Returns size in bytes for sorting/comparison.
     */
    fun parseSizeBytes(stream: Stream): Long? {
        // 1. Exact bytes
        stream.behaviorHints?.videoSize?.let { if (it > 0) return it }
        
        // 2. Parse from text
        val text = listOfNotNull(
            stream.title, stream.name, stream.description,
            stream.behaviorHints?.filename
        ).joinToString(" ")
        
        val match = SIZE_REGEX.find(text) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].uppercase()
        
        return when (unit) {
            "TB" -> (value * 1024 * 1024 * 1024 * 1024).toLong()
            "GB" -> (value * 1024 * 1024 * 1024).toLong()
            "MB" -> (value * 1024 * 1024).toLong()
            else -> null
        }
    }
    
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024 * 1024 * 1024 -> String.format("%.1f TB", bytes.toDouble() / (1024.0 * 1024 * 1024 * 1024))
            bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes.toDouble() / (1024.0 * 1024 * 1024))
            bytes >= 1024L * 1024 -> String.format("%.0f MB", bytes.toDouble() / (1024.0 * 1024))
            else -> String.format("%.0f KB", bytes.toDouble() / 1024.0)
        }
    }
}
```

---

## Language Detection

Detects content languages from stream text.

```kotlin
object LanguageParser {
    
    data class DetectedLanguage(
        val code: String,
        val name: String,
        val flag: String
    )
    
    private val LANGUAGE_MAP = listOf(
        Triple("ENG", "English", "🇬🇧"),
        Triple("SPA", "Spanish", "🇪🇸"),
        Triple("FRE", "French", "🇫🇷"),
        Triple("GER", "German", "🇩🇪"),
        Triple("ITA", "Italian", "🇮🇹"),
        Triple("POR", "Portuguese", "🇧🇷"),
        Triple("RUS", "Russian", "🇷🇺"),
        Triple("JAP", "Japanese", "🇯🇵"),
        Triple("KOR", "Korean", "🇰🇷"),
        Triple("CHI", "Chinese", "🇨🇳"),
        Triple("HIN", "Hindi", "🇮🇳"),
        Triple("ARA", "Arabic", "🇸🇦"),
        Triple("TUR", "Turkish", "🇹🇷"),
        Triple("POL", "Polish", "🇵🇱"),
        Triple("DUT", "Dutch", "🇳🇱"),
    )
    
    /**
     * Returns list of detected languages with flag emojis.
     */
    fun detectLanguages(stream: Stream): List<DetectedLanguage> {
        val text = listOfNotNull(
            stream.title, stream.name, stream.description,
            stream.behaviorHints?.filename
        ).joinToString(" ").uppercase()
        
        // Check for multi-language indicators first
        if (text.containsAny("MULTI", "DUAL")) {
            return listOf(DetectedLanguage("MULTI", "Multiple", "🌐"))
        }
        
        return LANGUAGE_MAP
            .filter { (code, _, _) -> text.contains(code) }
            .map { (code, name, flag) -> DetectedLanguage(code, name, flag) }
    }
    
    private fun String.containsAny(vararg terms: String): Boolean =
        terms.any { this.contains(it, ignoreCase = true) }
}
```

---

## Audio Language Options

For settings pickers (primary and secondary audio language):

```kotlin
val AUDIO_LANGUAGE_OPTIONS = listOf(
    AudioLanguageOption("en", "English", "🇬🇧"),
    AudioLanguageOption("es", "Spanish", "🇪🇸"),
    AudioLanguageOption("fr", "French", "🇫🇷"),
    AudioLanguageOption("de", "German", "🇩🇪"),
    AudioLanguageOption("it", "Italian", "🇮🇹"),
    AudioLanguageOption("pt", "Portuguese", "🇧🇷"),
    AudioLanguageOption("ru", "Russian", "🇷🇺"),
    AudioLanguageOption("ja", "Japanese", "🇯🇵"),
    AudioLanguageOption("ko", "Korean", "🇰🇷"),
    AudioLanguageOption("zh", "Chinese", "🇨🇳"),
    AudioLanguageOption("hi", "Hindi", "🇮🇳"),
    AudioLanguageOption("ar", "Arabic", "🇸🇦"),
    AudioLanguageOption("tr", "Turkish", "🇹🇷"),
    AudioLanguageOption("pl", "Polish", "🇵🇱"),
    AudioLanguageOption("nl", "Dutch", "🇳🇱"),
    AudioLanguageOption("original", "Original", "🎬"),
)

data class AudioLanguageOption(
    val code: String,
    val name: String,
    val flag: String
)
```

---

## Stream Sorting

Streams are sorted by quality (best first), then by cache status:

```kotlin
fun sortStreams(
    streams: List<Stream>,
    cachedHashes: Set<String>
): List<Stream> {
    return streams.sortedWith(
        compareBy<Stream> { stream ->
            // Cached streams first
            val isCached = cachedHashes.contains(stream.infoHash?.lowercase())
            if (isCached) 0 else 1
        }.thenBy { stream ->
            // Then by quality (best first)
            StreamQualityParser.detectQuality(stream).sortOrder
        }.thenByDescending { stream ->
            // Then by file size (largest first — usually better quality)
            FileSizeParser.parseSizeBytes(stream) ?: 0L
        }
    )
}
```

---

## Torrentio URL Builder

Constructs the Torrentio addon URL with Real-Debrid config:

```kotlin
object WellKnownAddon {
    val CINEMETA = "https://v3-cinemeta.strem.io"
    val OPENSUBTITLES = "https://opensubtitles-v3.strem.io"
    
    /**
     * Build Torrentio URL with Real-Debrid key and filters.
     * 
     * Example output:
     * https://torrentio.strem.fun/realdebrid=API_KEY|qualityfilter=scr,cam|sort=qualitysize/manifest.json
     */
    fun torrentioUrl(
        realDebridKey: String,
        qualityFilter: List<String> = listOf("scr", "cam"),
        sorting: String = "qualitysize"
    ): String {
        val config = buildString {
            append("realdebrid=$realDebridKey")
            if (qualityFilter.isNotEmpty()) {
                append("|qualityfilter=${qualityFilter.joinToString(",")}")
            }
            append("|sort=$sorting")
        }
        return "https://torrentio.strem.fun/$config"
    }
    
    /**
     * Build TorrentsDB URL with Real-Debrid key.
     */
    fun torrentsDbUrl(realDebridKey: String): String {
        return "https://torrentsdb.strem.fun/$realDebridKey"
    }
}
```

---

## InfoHash Extraction

Extract torrent info hash from Torrentio resolve URLs:

```kotlin
fun extractInfoHash(url: String?): String? {
    if (url == null) return null
    return url.split("/").firstOrNull { component ->
        component.length == 40 && component.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }
}
```
