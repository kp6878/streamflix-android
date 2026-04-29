package com.example.streamfilx_androidtv.features.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

class VlcPlayerWrapper(context: Context) {

    private val libVLC: LibVLC = LibVLC(
        context,
        arrayListOf(
            "--network-caching=1500",
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--aout=opensles",
        ),
    )
    private val mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

    // ── Callbacks ─────────────────────────────────────────────────────────────

    var onPlaying: (() -> Unit)? = null
    var onPaused: (() -> Unit)? = null
    var onStopped: (() -> Unit)? = null
    var onBuffering: ((Float) -> Unit)? = null
    var onTimeChanged: ((timeMs: Long, lengthMs: Long) -> Unit)? = null
    var onPositionChanged: ((Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onEndReached: (() -> Unit)? = null
    var onTracksChanged: (() -> Unit)? = null

    init {
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> onPlaying?.invoke()
                MediaPlayer.Event.Paused -> onPaused?.invoke()
                MediaPlayer.Event.Stopped -> onStopped?.invoke()
                MediaPlayer.Event.Buffering -> onBuffering?.invoke(event.buffering)
                MediaPlayer.Event.TimeChanged ->
                    onTimeChanged?.invoke(event.timeChanged, mediaPlayer.length)
                MediaPlayer.Event.PositionChanged -> onPositionChanged?.invoke(event.positionChanged)
                MediaPlayer.Event.EncounteredError -> onError?.invoke("Playback error occurred")
                MediaPlayer.Event.EndReached -> onEndReached?.invoke()
                MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted -> onTracksChanged?.invoke()
                else -> {}
            }
        }
    }

    // ── Surface ───────────────────────────────────────────────────────────────

    fun attachSurface(surfaceView: SurfaceView) {
        mediaPlayer.vlcVout.apply {
            setVideoView(surfaceView)
            attachViews()
        }
    }

    fun detachSurface() {
        try {
            if (mediaPlayer.vlcVout.areViewsAttached()) {
                mediaPlayer.vlcVout.detachViews()
            }
        } catch (_: Exception) {}
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun play(url: String) {
        val media = Media(libVLC, Uri.parse(url))
        media.setHWDecoderEnabled(true, false)
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    fun pause() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    fun resume() {
        if (!mediaPlayer.isPlaying) mediaPlayer.play()
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
    }

    fun stop() {
        try { mediaPlayer.stop() } catch (_: Exception) {}
    }

    // ── Seeking ───────────────────────────────────────────────────────────────

    fun seekTo(seconds: Double) {
        mediaPlayer.time = (seconds * 1000).toLong().coerceAtLeast(0)
    }

    fun seekToPosition(position: Float) {
        mediaPlayer.position = position.coerceIn(0f, 1f)
    }

    fun skipForward(seconds: Double) {
        val newTime = (mediaPlayer.time + (seconds * 1000).toLong())
            .coerceAtMost(mediaPlayer.length)
        mediaPlayer.time = newTime
    }

    fun skipBackward(seconds: Double) {
        val newTime = (mediaPlayer.time - (seconds * 1000).toLong()).coerceAtLeast(0)
        mediaPlayer.time = newTime
    }

    // ── Audio/Volume ──────────────────────────────────────────────────────────

    fun setVolume(volume: Int) {
        mediaPlayer.volume = volume.coerceIn(0, 200)
    }

    fun setRate(rate: Float) {
        mediaPlayer.rate = rate
    }

    // ── Track selection ───────────────────────────────────────────────────────

    fun getAudioTracks(): List<AudioTrack> {
        return mediaPlayer.audioTracks
            ?.filter { it.id != -1 }
            ?.map { AudioTrack(id = it.id, name = it.name ?: "", language = it.name ?: "") }
            ?: emptyList()
    }

    fun getSubtitleTracks(): List<SubtitleTrack> {
        val embedded = mediaPlayer.spuTracks
            ?.map { SubtitleTrack(id = it.id, name = it.name ?: "", language = it.name ?: "") }
            ?: emptyList()
        return listOf(SubtitleTrack(id = -1, name = "Off", language = "")) + embedded
    }

    fun setAudioTrack(id: Int): Boolean = mediaPlayer.setAudioTrack(id)
    fun setSubtitleTrack(id: Int): Boolean = mediaPlayer.setSpuTrack(id)
    fun getCurrentAudioTrackId(): Int = mediaPlayer.audioTrack
    fun getCurrentSubtitleTrackId(): Int = mediaPlayer.spuTrack

    fun addExternalSubtitle(url: String) {
        mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, url, true)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun release() {
        try { mediaPlayer.stop() } catch (_: Exception) {}
        detachSurface()
        mediaPlayer.release()
        libVLC.release()
    }
}
