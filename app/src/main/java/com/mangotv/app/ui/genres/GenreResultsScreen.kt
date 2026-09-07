package com.mangotv.app.ui.genres

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.ui.browse.RowsBrowseContent

@Composable
fun GenreResultsScreen(
    onNavigate: (String) -> Unit,
    viewModel: GenreResultsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RowsBrowseContent(
        screenTitle = viewModel.genre,
        navLabel = "Genres",
        uiState = uiState,
        onNavigate = onNavigate,
        onRetry = viewModel::load
    )
}
