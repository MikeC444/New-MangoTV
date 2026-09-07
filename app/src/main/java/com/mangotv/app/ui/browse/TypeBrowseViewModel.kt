package com.mangotv.app.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.provider.CatalogProvider
import com.mangotv.app.data.provider.HomeRowPreferences
import com.mangotv.app.data.provider.ProviderRegistry
import com.mangotv.app.data.model.HomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs Movies and TV Shows: loops every installed provider's
 * getSectionsByType(type), same reactive-to-ProviderRegistry pattern as
 * HomeViewModel. Sorted with a throwaway HomeRowPreferences() instance
 * purely for its default "base row first, then curated genre order" sort --
 * these screens deliberately don't read/write the user's actual saved Home
 * Rows order/hidden state, so a hidden Home row still shows up here.
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

        val ordered = HomeRowPreferences().applyOrder(sections)
        _uiState.value = when {
            ordered.isNotEmpty() -> RowsBrowseUiState.Loaded(ordered)
            anyProviderFailed -> RowsBrowseUiState.Error("Couldn't reach your installed addons. Check your connection and try again.")
            else -> RowsBrowseUiState.Loaded(emptyList())
        }
    }
}

class MoviesViewModel(application: Application) : TypeBrowseViewModel(application, ContentType.MOVIE)

class TvShowsViewModel(application: Application) : TypeBrowseViewModel(application, ContentType.TV_SHOW)
