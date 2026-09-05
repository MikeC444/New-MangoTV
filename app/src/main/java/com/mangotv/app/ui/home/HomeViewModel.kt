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
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val providers = ProviderRegistry.activeProviders()
                val hero = mutableListOf<Content>()
                val sections = mutableListOf<HomeSection>()
                for (provider in providers) {
                    hero += provider.getFeatured()
                    sections += provider.getHomeSections()
                }
                if (hero.isEmpty() && sections.isEmpty()) {
                    _uiState.value = HomeUiState.Error("No content available right now.")
                } else {
                    _uiState.value = HomeUiState.Success(hero, sections)
                }
            } catch (t: Throwable) {
                _uiState.value = HomeUiState.Error(t.message ?: "Something went wrong.")
            }
        }
    }
}
