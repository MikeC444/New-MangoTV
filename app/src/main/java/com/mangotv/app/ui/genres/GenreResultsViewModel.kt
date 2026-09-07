package com.mangotv.app.ui.genres

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.provider.ProviderRegistry
import com.mangotv.app.ui.browse.RowsBrowseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder

/**
 * Backs the Genre Results screen (reuses RowsBrowseContent). Fetches
 * getGenreSection(genre) from every installed provider and merges them into
 * one HomeSection -- each provider's own section is already mixed-type
 * (movies+series) via StremioAddonProvider's own merge, this just combines
 * across providers on top of that.
 */
class GenreResultsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val genre: String = URLDecoder.decode(savedStateHandle.get<String>("genre").orEmpty(), "UTF-8")

    private val _uiState = MutableStateFlow<RowsBrowseUiState>(RowsBrowseUiState.Loading)
    val uiState: StateFlow<RowsBrowseUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RowsBrowseUiState.Loading

            val providers = ProviderRegistry.activeProviders()
            if (providers.isEmpty()) {
                _uiState.value = RowsBrowseUiState.Loaded(emptyList())
                return@launch
            }

            val perProviderItems = mutableListOf<List<Content>>()
            var anyProviderFailed = false
            for (provider in providers) {
                runCatching { provider.getGenreSection(genre) }
                    .onSuccess { section -> section?.let { perProviderItems += it.items } }
                    .onFailure { anyProviderFailed = true }
            }

            val merged = interleave(perProviderItems).distinctBy { it.id }
            _uiState.value = when {
                merged.isNotEmpty() -> RowsBrowseUiState.Loaded(listOf(HomeSection(id = "genre_$genre", title = genre, items = merged)))
                anyProviderFailed -> RowsBrowseUiState.Error("Couldn't reach your installed addons. Check your connection and try again.")
                else -> RowsBrowseUiState.Loaded(emptyList())
            }
        }
    }
}

// Small local copy of the same interleave pattern StremioAddonProvider uses
// internally -- not worth extracting that private helper into shared code
// for this one extra caller (cross-PROVIDER merging here, vs. cross-catalog
// merging there).
private fun <T> interleave(lists: List<List<T>>): List<T> {
    if (lists.size == 1) return lists[0]
    val result = mutableListOf<T>()
    val maxSize = lists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until maxSize) {
        for (list in lists) {
            if (i < list.size) result += list[i]
        }
    }
    return result
}
