package com.mangotv.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.provider.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Re-collects (and reloads) automatically whenever an addon is
        // installed, removed, enabled or disabled — Home never needs to be
        // told to refresh explicitly.
        viewModelScope.launch {
            ProviderRegistry.providers.collect {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val providers = ProviderRegistry.activeProviders()
            if (providers.isEmpty()) {
                _uiState.value = HomeUiState.Empty
                return@launch
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

            _uiState.value = when {
                hero.isNotEmpty() || sections.isNotEmpty() -> HomeUiState.Success(hero, sections)
                anyProviderFailed -> HomeUiState.Error("Couldn't reach your installed addons. Check your connection and try again.")
                else -> HomeUiState.Empty
            }
        }
    }
}
