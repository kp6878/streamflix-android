package com.example.streamfilx_androidtv.features.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.core.network.StremioClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val GENRES = listOf(
    "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime",
    "Documentary", "Drama", "Family", "Fantasy", "History", "Horror",
    "Music", "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller", "Western",
)

private const val PAGE_SIZE = 100

data class BrowseUiState(
    val selectedType: String = "movie",
    val selectedGenre: String? = null,
    val items: List<MetaPreview> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentSkip: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val stremioClient: StremioClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadContent(reset = true)
    }

    fun selectType(type: String) {
        if (_uiState.value.selectedType == type) return
        _uiState.value = _uiState.value.copy(selectedType = type, selectedGenre = null)
        loadContent(reset = true)
    }

    fun selectGenre(genre: String?) {
        if (_uiState.value.selectedGenre == genre) return
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
        loadContent(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        loadContent(reset = false)
    }

    fun retry() = loadContent(reset = true)

    private fun loadContent(reset: Boolean) {
        loadJob?.cancel()
        val state = _uiState.value
        val skip = if (reset) 0 else state.currentSkip

        if (reset) {
            _uiState.value = state.copy(isLoading = true, items = emptyList(), error = null, currentSkip = 0, hasMore = true)
        } else {
            _uiState.value = state.copy(isLoadingMore = true)
        }

        loadJob = viewModelScope.launch {
            runCatching {
                val current = _uiState.value
                val extra = buildMap<String, String> {
                    if (skip > 0) put("skip", skip.toString())
                    current.selectedGenre?.let { put("genre", it) }
                }

                stremioClient.fetchCatalog(
                    baseUrl = "https://v3-cinemeta.strem.io",
                    type = current.selectedType,
                    catalogId = "top",
                    extra = extra.ifEmpty { null },
                )
            }.onSuccess { response ->
                val newItems = response.metas
                val existingItems = if (reset) emptyList() else _uiState.value.items
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    items = existingItems + newItems,
                    currentSkip = skip + newItems.size,
                    hasMore = newItems.size >= PAGE_SIZE,
                    error = null,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load content",
                )
            }
        }
    }
}
