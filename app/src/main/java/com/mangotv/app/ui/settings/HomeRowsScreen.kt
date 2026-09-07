package com.mangotv.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.ui.components.TvFocusSurface
import com.mangotv.app.ui.theme.MangoAmber
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

@Composable
fun HomeRowsScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeRowsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hiddenRowIds by viewModel.hiddenRowIds.collectAsStateWithLifecycle()
    val navFocusRequester = remember { FocusRequester() }
    val firstRowFocusRequester = remember { FocusRequester() }

    SettingsScaffold(
        title = "Home Rows",
        onNavigate = onNavigate,
        navFocusRequester = navFocusRequester,
        firstContentFocusRequester = firstRowFocusRequester
    ) {
        Text(
            text = "Choose which rows show up on Home. Addons with lots of genres can add a lot of rows — turn off the ones you don't want.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        when (val state = uiState) {
            is HomeRowsUiState.Loading -> CircularProgressIndicator(color = MangoAmber)
            is HomeRowsUiState.NoAddons -> Text(
                text = "Install an addon first — its rows will show up here once it's added.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            is HomeRowsUiState.Loaded -> {
                if (state.rows.isEmpty()) {
                    Text(
                        text = "Your installed addons aren't reporting any rows right now.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
                            val visible = row.id !in hiddenRowIds
                            HomeRowToggleRow(
                                row = row,
                                visible = visible,
                                onToggle = { viewModel.setRowVisible(row.id, !visible) },
                                focusRequester = if (index == 0) firstRowFocusRequester else null,
                                focusUp = if (index == 0) navFocusRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRowToggleRow(
    row: HomeSection,
    visible: Boolean,
    onToggle: () -> Unit,
    focusRequester: FocusRequester? = null,
    focusUp: FocusRequester? = null
) {
    TvFocusSurface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MangoDimens.CardCornerRadius),
        backgroundColor = MangoSurface,
        focusRequester = focusRequester,
        focusUp = focusUp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = visible,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = MangoAmber)
            )
        }
    }
}
