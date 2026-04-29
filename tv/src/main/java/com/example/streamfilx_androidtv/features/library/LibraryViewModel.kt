package com.example.streamfilx_androidtv.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamfilx_androidtv.core.models.MetaPreview
import com.example.streamfilx_androidtv.services.FavoritesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter { ALL, MOVIES, SERIES }

data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter.ALL,
    val items: List<MetaPreview> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoritesManager: FavoritesManager,
) : ViewModel() {

    private val _filter = MutableStateFlow(LibraryFilter.ALL)

    val uiState: StateFlow<LibraryUiState> = combine(
        _filter,
        favoritesManager.favorites,
        favoritesManager.movieFavorites,
        favoritesManager.seriesFavorites,
    ) { filter, all, movies, series ->
        val items = when (filter) {
            LibraryFilter.ALL -> all
            LibraryFilter.MOVIES -> movies
            LibraryFilter.SERIES -> series
        }.map { it.toMetaPreview() }

        LibraryUiState(filter = filter, items = items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setFilter(filter: LibraryFilter) {
        _filter.value = filter
    }

    fun removeFromList(contentId: String) {
        viewModelScope.launch {
            favoritesManager.removeFavorite(contentId)
        }
    }
}
