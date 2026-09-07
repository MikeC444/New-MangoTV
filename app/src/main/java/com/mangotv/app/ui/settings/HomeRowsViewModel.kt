package com.mangotv.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.MangoTvApplication
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.provider.CatalogProvider
import com.mangotv.app.data.provider.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeRowsUiState {
    data object Loading : HomeRowsUiState
    data object NoAddons : HomeRowsUiState
    data class Loaded(val rows: List<HomeSection>) : HomeRowsUiState
}

/**
 * Backs Settings > Home Rows. Re-fetches every installed addon's rows the
 * same way HomeViewModel does (there's no shared cache between them) so the
 * list here always matches what Home would currently show, titles included
 * — this only runs when the user opens this screen, not on every Home load.
 */
class HomeRowsViewModel(application: Application) : AndroidViewModel(application) {

    private val homeRowPreferences = (application as MangoTvApplication).container.homeRowPreferencesRepository

    val hiddenRowIds: StateFlow<Set<String>> = homeRowPreferences.hiddenRowIds

    private val _uiState = MutableStateFlow<HomeRowsUiState>(HomeRowsUiState.Loading)
    val uiState: StateFlow<HomeRowsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ProviderRegistry.providers.collect { providers -> loadRows(providers) }
        }
    }

    private suspend fun loadRows(providers: List<CatalogProvider>) {
        if (providers.isEmpty()) {
            _uiState.value = HomeRowsUiState.NoAddons
            return
        }
        _uiState.value = HomeRowsUiState.Loading
        val rows = mutableListOf<HomeSection>()
        for (provider in providers) {
            runCatching { provider.getHomeSections() }.onSuccess { rows += it }
        }
        _uiState.value = HomeRowsUiState.Loaded(rows)
    }

    fun setRowVisible(rowId: String, visible: Boolean) {
        viewModelScope.launch { homeRowPreferences.setRowHidden(rowId, hidden = !visible) }
    }
}
