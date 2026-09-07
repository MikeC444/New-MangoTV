package com.mangotv.app.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.provider.CatalogProvider
import com.mangotv.app.data.provider.ProviderRegistry
import com.mangotv.app.data.model.HomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs Movies and TV Shows: loops every installed provider's
 * getSectionsByType(type), same reactive-to-ProviderRegistry pattern as
 * HomeViewModel. Unlike Home, this deliberately shows no genre breakdown --
 * every provider's base + genre rows are flattened into one deduplicated,
 * shuffled row, so genres never touch the user's saved Home Rows state and
 * a hidden Home row still shows up here.
 */
open class TypeBrowseViewModel(application: Application, private val type: ContentType) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<RowsBrowseUiState>(RowsBrowseUiState.Loading)
    val uiState: StateFlow<RowsBrowseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ProviderRegistry.providers.collect { providers -> load(providers) }
        }
    }

    fun load() {
        viewModelScope.launch { load(ProviderRegistry.activeProviders()) }
    }

    private suspend fun load(providers: List<CatalogProvider>) {
        _uiState.value = RowsBrowseUiState.Loading

        if (providers.isEmpty()) {
            _uiState.value = RowsBrowseUiState.Loaded(emptyList())
            return
        }

        val sections = mutableListOf<HomeSection>()
        var anyProviderFailed = false
        for (provider in providers) {
            runCatching { provider.getSectionsByType(type) }
                .onSuccess { sections += it }
                .onFailure { anyProviderFailed = true }
        }

        // Flatten every provider's base + genre rows into one deduplicated,
        // shuffled row -- no genre breakdown here, and a fresh shuffle each
        // time this loads so the order varies on revisit.
        val items = sections.flatMap { it.items }.distinctBy { it.id }.shuffled()
        val flattened = if (items.isNotEmpty()) {
            listOf(HomeSection(id = "flat_$type", title = "", items = items))
        } else {
            emptyList()
        }

        _uiState.value = when {
            flattened.isNotEmpty() -> RowsBrowseUiState.Loaded(flattened)
            anyProviderFailed -> RowsBrowseUiState.Error("Couldn't reach your installed addons. Check your connection and try again.")
            else -> RowsBrowseUiState.Loaded(emptyList())
        }
    }
}

class MoviesViewModel(application: Application) : TypeBrowseViewModel(application, ContentType.MOVIE)

class TvShowsViewModel(application: Application) : TypeBrowseViewModel(application, ContentType.TV_SHOW)
