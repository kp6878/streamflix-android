package com.example.streamfilx_androidtv.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.core.models.WatchHistoryItem
import com.example.streamfilx_androidtv.core.network.StremioClient
import com.example.streamfilx_androidtv.services.WatchHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContentRowState(
    val title: String,
    val type: String,
    val items: List<MetaPreview>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val heroItems: List<MetaPreview> = emptyList(),
    val continueWatching: List<WatchHistoryItem> = emptyList(),
    val contentRows: List<ContentRowState> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stremioClient: StremioClient,
    private val watchHistoryManager: WatchHistoryManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadContent()
        observeContinueWatching()
    }

    private fun observeContinueWatching() {
        watchHistoryManager.observeContinueWatching()
            .onEach { items -> _uiState.update { it.copy(continueWatching = items) } }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            runCatching { fetchCatalogs() }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { fetchCatalogs() }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load content") }
            }
        }
    }

    private suspend fun fetchCatalogs() {
        coroutineScope {
            val topMovies = async { stremioClient.fetchCinemetaCatalog("movie", "top") }
            val topSeries = async { stremioClient.fetchCinemetaCatalog("series", "top") }
            val topMoviesExtra = async {
                stremioClient.fetchCatalog(
                    baseUrl = "https://v3-cinemeta.strem.io",
                    type = "movie",
                    catalogId = "top",
                    extra = mapOf("genre" to "Action"),
                )
            }
            val movies = topMovies.await().metas
            val series = topSeries.await().metas
            val action = runCatching { topMoviesExtra.await().metas }.getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    heroItems = movies.take(5),
                    contentRows = buildList {
                        if (movies.isNotEmpty()) add(ContentRowState("Top Movies", "movie", movies))
                        if (series.isNotEmpty()) add(ContentRowState("Top Series", "series", series))
                        if (action.isNotEmpty()) add(ContentRowState("Action Movies", "movie", action))
                    },
                )
            }
        }
    }
}
