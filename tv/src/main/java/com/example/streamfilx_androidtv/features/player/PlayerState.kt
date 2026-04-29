package com.example.streamfilx_androidtv.features.player

data class AudioTrack(
    val id: Int,
    val name: String,
    val language: String,
) {
    val displayName: String get() = name.takeIf { it.isNotEmpty() } ?: "Track $id"
}

data class SubtitleTrack(
    val id: Int,
    val name: String,
    val language: String,
    val isExternal: Boolean = false,
) {
    val displayName: String
        get() = when {
            id == -1 -> "Off"
            name.isNotEmpty() -> name
            else -> "Track $id"
        }

    val isEnglish: Boolean
        get() {
            val l = language.lowercase()
            return l.startsWith("en") || l.contains("english")
        }
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoading: Boolean = true,
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val progress: Double = 0.0,
    val volume: Int = 100,
    val isMuted: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null,
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedAudioTrackId: Int = -1,
    val selectedSubtitleTrackId: Int = -1,
) {
    val currentTimeFormatted: String get() = formatTime(currentTime)
    val durationFormatted: String get() = formatTime(duration)
    val remainingTime: Double get() = (duration - currentTime).coerceAtLeast(0.0)

    fun formatTime(seconds: Double): String {
        if (seconds <= 0) return "0:00"
        val total = seconds.toInt()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    val endsAtTime: String
        get() {
            if (duration <= 0) return ""
            val remainingMs = (remainingTime * 1000).toLong()
            val endMs = System.currentTimeMillis() + remainingMs
            return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                .format(java.util.Date(endMs))
        }
}

enum class TrackPickerType { AUDIO, SUBTITLE }
