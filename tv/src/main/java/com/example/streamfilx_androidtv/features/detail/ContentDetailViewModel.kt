package com.example.streamfilx_androidtv.features.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.MetaItem
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.core.models.Video
import com.example.streamfilx_androidtv.core.network.StremioClient
import com.example.streamfilx_androidtv.services.FavoritesManager
import com.example.streamfilx_androidtv.services.download.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContentDetailUiState(
    val meta: MetaItem? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val selectedSeason: Int? = null,
    val moreLikeThis: List<MetaPreview> = emptyList(),
    val error: String? = null,
    val downloadStarted: Boolean = false,
) {
    val episodesForSelectedSeason: List<Video>
        get() {
            val season = selectedSeason ?: return emptyList()
            return meta?.episodesForSeason(season) ?: emptyList()
        }
}

@HiltViewModel
class ContentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stremioClient: StremioClient,
    private val favoritesManager: FavoritesManager,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val type: String = checkNotNull(savedStateHandle["type"])
    val id: String = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(ContentDetailUiState())
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = ContentDetailUiState(isLoading = true)
            runCatching {
                coroutineScope {
                    val metaDeferred = async {
                        stremioClient.fetchMeta(
                            baseUrl = "https://v3-cinemeta.strem.io",
                            type = type,
                            id = id,
                        ).meta
                    }
                    val favoriteDeferred = async { favoritesManager.isFavorite(id) }
                    val moreLikeDeferred = async {
                        runCatching {
                            stremioClient.fetchCinemetaCatalog(type, "top")
                                .metas
                                .filter { it.id != id }
                                .take(20)
                        }.getOrDefault(emptyList())
                    }

                    val meta = metaDeferred.await()
                    val isFavorite = favoriteDeferred.await()
                    val moreLike = moreLikeDeferred.await()
                    val defaultSeason = meta.availableSeasons.firstOrNull()

                    _uiState.value = ContentDetailUiState(
                        meta = meta,
                        isLoading = false,
                        isFavorite = isFavorite,
                        selectedSeason = defaultSeason,
                        moreLikeThis = moreLike,
                    )
                }
            }.onFailure { e ->
                _uiState.value = ContentDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load content",
                )
            }
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun startDownload(url: String, quality: String?, episode: Video?) {
        val meta = _uiState.value.meta ?: return
        viewModelScope.launch {
            downloadManager.startDownload(
                contentId = id,
                contentType = type,
                contentName = meta.name,
                episodeTitle = episode?.title,
                season = episode?.season,
                episode = episode?.episodeNumber,
                poster = meta.poster,
                streamUrl = url,
            )
            _uiState.value = _uiState.value.copy(downloadStarted = true)
        }
    }

    fun clearDownloadStarted() {
        _uiState.value = _uiState.value.copy(downloadStarted = false)
    }

    fun toggleFavorite() {
        val meta = _uiState.value.meta ?: return
        viewModelScope.launch {
            val nowFavorite = favoritesManager.toggleFavorite(meta.toPreview())
            _uiState.value = _uiState.value.copy(isFavorite = nowFavorite)
        }
    }
}
