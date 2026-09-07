package com.mangotv.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.MangoTvApplication
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.provider.CatalogProvider
import com.mangotv.app.data.provider.HomeRowPreferences
import com.mangotv.app.data.provider.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(
        val heroItems: List<Content>,
        val sections: List<HomeSection>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val homeRowPreferences = (application as MangoTvApplication).container.homeRowPreferencesRepository

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Re-collects (and reloads) automatically whenever an addon is
        // installed, removed, enabled or disabled, or the user hides/shows a
        // row from Settings > Home Rows — Home never needs to be told to
        // refresh explicitly.
        viewModelScope.launch {
            combine(ProviderRegistry.providers, homeRowPreferences.preferences) { providers, prefs -> providers to prefs }
                .collect { (providers, prefs) -> load(providers, prefs) }
        }
    }

    fun load() {
        viewModelScope.launch {
            load(ProviderRegistry.activeProviders(), homeRowPreferences.preferences.value)
        }
    }

    private suspend fun load(providers: List<CatalogProvider>, rowPreferences: HomeRowPreferences) {
        _uiState.value = HomeUiState.Loading

        if (providers.isEmpty()) {
            _uiState.value = HomeUiState.Empty
            return
        }

        val hero = mutableListOf<Content>()
        val sections = mutableListOf<HomeSection>()
        var anyProviderFailed = false

        for (provider in providers) {
            runCatching { provider.getFeatured() }
                .onSuccess { hero += it }
                .onFailure { anyProviderFailed = true }
            runCatching { provider.getHomeSections() }
                .onSuccess { sections += it }
                .onFailure { anyProviderFailed = true }
        }

        val visibleSections = rowPreferences.applyOrder(sections).filterNot { it.id in rowPreferences.hiddenRowIds }

        _uiState.value = when {
            hero.isNotEmpty() || visibleSections.isNotEmpty() -> HomeUiState.Success(hero, visibleSections)
            anyProviderFailed -> HomeUiState.Error("Couldn't reach your installed addons. Check your connection and try again.")
            else -> HomeUiState.Empty
        }
    }
}
