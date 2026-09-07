package com.mangotv.app.ui.genres

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.data.provider.CatalogProvider
import com.mangotv.app.data.provider.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GenresUiState {
    data object Loading : GenresUiState
    data object NoAddons : GenresUiState
    data class Loaded(val genres: List<String>) : GenresUiState
}

/** Backs the Genres picker: unions getAvailableGenres() across every installed provider. */
class GenresViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GenresUiState>(GenresUiState.Loading)
    val uiState: StateFlow<GenresUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ProviderRegistry.providers.collect { providers -> load(providers) }
        }
    }

    private suspend fun load(providers: List<CatalogProvider>) {
        if (providers.isEmpty()) {
            _uiState.value = GenresUiState.NoAddons
            return
        }
        _uiState.value = GenresUiState.Loading
        val genres = mutableSetOf<String>()
        for (provider in providers) {
            runCatching { provider.getAvailableGenres() }.onSuccess { genres += it }
        }
        _uiState.value = GenresUiState.Loaded(genres.toList().sorted())
    }
}
