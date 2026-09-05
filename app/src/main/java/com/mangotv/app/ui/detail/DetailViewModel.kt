package com.mangotv.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.provider.CatalogProvider
import com.mangotv.app.data.provider.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val content: Content, val similar: List<Content>) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val providerId: String =
        URLDecoder.decode(savedStateHandle.get<String>("providerId").orEmpty(), "UTF-8")
    private val contentType: ContentType =
        if (savedStateHandle.get<String>("type") == ContentType.TV_SHOW.name) {
            ContentType.TV_SHOW
        } else {
            ContentType.MOVIE
        }
    private val contentId: String =
        URLDecoder.decode(savedStateHandle.get<String>("id").orEmpty(), "UTF-8")

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading

            val provider = ProviderRegistry.activeProviders().find { it.id == providerId }
            if (provider == null) {
                _uiState.value = DetailUiState.Error("This addon is no longer installed.")
                return@launch
            }

            val detail = runCatching { provider.getDetails(contentType, contentId) }.getOrNull()
            if (detail == null) {
                _uiState.value = DetailUiState.Error("Couldn't load details for this title.")
                return@launch
            }

            val similar = runCatching { loadSimilar(provider, detail) }.getOrDefault(emptyList())
            _uiState.value = DetailUiState.Success(detail, similar)
        }
    }

    private suspend fun loadSimilar(
        provider: CatalogProvider,
        detail: Content
    ): List<Content> {
        val allItems = provider.getHomeSections()
            .flatMap { it.items }
            .distinctBy { it.id }
            .filterNot { it.id == detail.id }

        val detailGenreIds = detail.genres.map { it.id }.toSet()
        val genreMatches = if (detailGenreIds.isEmpty()) {
            emptyList()
        } else {
            allItems.filter { item -> item.genres.any { it.id in detailGenreIds } }
        }

        return genreMatches.ifEmpty { allItems }.take(15)
    }
}
