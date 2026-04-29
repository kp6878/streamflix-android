package com.example.streamfilx_androidtv.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.core.network.StremioClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

val POPULAR_SEARCHES = listOf(
    "Batman", "Avengers", "Spider-Man", "Breaking Bad", "Game of Thrones",
    "The Office", "Friends", "Inception", "Interstellar", "The Dark Knight",
    "Stranger Things", "Succession", "Oppenheimer", "Dune", "John Wick",
)

data class SearchUiState(
    val query: String = "",
    val results: List<MetaPreview> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val stremioClient: StremioClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        // 500ms debounce on the query flow — skip the initial empty emission
        _queryFlow
            .drop(1)
            .debounce(500L)
            .distinctUntilChanged()
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)
        _queryFlow.value = query

        // Clear results instantly when query is empty
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.value = SearchUiState(query = "")
        }
    }

    fun clearQuery() = onQueryChange("")

    fun searchPopular(term: String) = onQueryChange(term)

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Search both movies and series, combine results
                val movieResults = runCatching {
                    stremioClient.searchCinemeta("movie", query).metas
                }.getOrDefault(emptyList())

                val seriesResults = runCatching {
                    stremioClient.searchCinemeta("series", query).metas
                }.getOrDefault(emptyList())

                // Interleave: movie, series, movie, series...
                val combined = buildList {
                    val maxLen = maxOf(movieResults.size, seriesResults.size)
                    for (i in 0 until maxLen) {
                        movieResults.getOrNull(i)?.let { add(it) }
                        seriesResults.getOrNull(i)?.let { add(it) }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = combined,
                    hasSearched = true,
                    error = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed",
                )
            }
        }
    }
}
