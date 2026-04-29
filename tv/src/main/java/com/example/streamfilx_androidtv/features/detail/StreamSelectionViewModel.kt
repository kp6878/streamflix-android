package com.example.streamfilx_androidtv.features.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.AudioCodec
import com.example.streamfilx_androidtv.core.models.Stream
import com.example.streamfilx_androidtv.core.models.StreamQuality
import com.example.streamfilx_androidtv.core.network.AudioCodecParser
import com.example.streamfilx_androidtv.core.network.FileSizeParser
import com.example.streamfilx_androidtv.core.network.LanguageParser
import com.example.streamfilx_androidtv.core.network.StreamQualityParser
import com.example.streamfilx_androidtv.core.network.buildTorrentioUrl
import com.example.streamfilx_androidtv.core.network.sortStreams
import com.example.streamfilx_androidtv.core.network.StremioClient
import com.example.streamfilx_androidtv.data.prefs.AppPreferences
import com.example.streamfilx_androidtv.services.RealDebridService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnrichedStream(
    val stream: Stream,
    val quality: StreamQuality,
    val audioCodec: AudioCodec?,
    val fileSize: String?,
    val isCached: Boolean = false,
    val languages: List<LanguageParser.DetectedLanguage> = emptyList(),
)

data class StreamSheetUiState(
    val streams: List<EnrichedStream> = emptyList(),
    val isLoadingStreams: Boolean = true,
    val isResolvingStream: Boolean = false,
    val resolvingStreamId: String? = null,
    val resolvedUrl: String? = null,
    val error: String? = null,
)

@HiltViewModel
class StreamSelectionViewModel @Inject constructor(
    private val stremioClient: StremioClient,
    private val realDebridService: RealDebridService,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamSheetUiState())
    val uiState: StateFlow<StreamSheetUiState> = _uiState.asStateFlow()

    fun loadStreams(type: String, id: String) {
        viewModelScope.launch {
            _uiState.value = StreamSheetUiState(isLoadingStreams = true)

            val rdKey = appPreferences.realDebridApiKey.first()
            val sources = buildList {
                // Torrentio with RD key (if configured)
                if (!rdKey.isNullOrBlank()) {
                    add(buildTorrentioUrl(rdKey))
                } else {
                    // Torrentio without RD — public streams only
                    add("https://torrentio.strem.fun")
                }
            }

            val rawStreams = mutableListOf<Stream>()
            sources.forEach { baseUrl ->
                runCatching {
                    stremioClient.fetchStreams(baseUrl, type, id).streams
                }.onSuccess { rawStreams.addAll(it) }
            }

            // Enrich streams with parsed metadata
            var enriched = rawStreams.map { stream ->
                EnrichedStream(
                    stream = stream,
                    quality = StreamQualityParser.detectQuality(stream),
                    audioCodec = AudioCodecParser.detectAudioCodec(stream),
                    fileSize = FileSizeParser.parseFileSize(stream),
                    languages = LanguageParser.detectLanguages(stream),
                )
            }

            _uiState.value = StreamSheetUiState(
                streams = enriched.sortedBy { it.quality.sortOrder },
                isLoadingStreams = false,
            )

            // Background: check RD cache status and re-sort
            if (!rdKey.isNullOrBlank()) {
                val cachedHashes = realDebridService.checkAvailability(rawStreams)
                val cachedSet = cachedHashes.filter { it.value }.keys

                enriched = enriched.map { es ->
                    es.copy(isCached = cachedSet.contains(es.stream.infoHash?.lowercase()))
                }

                val sortedStreams = sortStreams(rawStreams, cachedSet)
                    .mapNotNull { s -> enriched.firstOrNull { it.stream === s } }

                _uiState.value = _uiState.value.copy(streams = sortedStreams)
            }
        }
    }

    fun resolveStream(enrichedStream: EnrichedStream, onResolved: (String) -> Unit) {
        viewModelScope.launch {
            val streamId = enrichedStream.stream.infoHash ?: enrichedStream.stream.url ?: return@launch
            _uiState.value = _uiState.value.copy(isResolvingStream = true, resolvingStreamId = streamId, error = null)

            realDebridService.resolveStream(enrichedStream.stream)
                .onSuccess { url ->
                    _uiState.value = _uiState.value.copy(
                        isResolvingStream = false,
                        resolvedUrl = url,
                        resolvingStreamId = null,
                    )
                    onResolved(url)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isResolvingStream = false,
                        resolvingStreamId = null,
                        error = e.message ?: "Failed to resolve stream",
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
