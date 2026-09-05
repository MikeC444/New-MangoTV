package com.mangotv.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.navigation.routeForNavLabel
import com.mangotv.app.ui.components.ContentRow
import com.mangotv.app.ui.components.FullScreenErrorState
import com.mangotv.app.ui.components.HomeEmptyState
import com.mangotv.app.ui.components.HomeLoadingSkeleton
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangoBackground)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> HomeLoadingSkeleton()
            is HomeUiState.Error -> FullScreenErrorState(
                message = state.message,
                onRetry = viewModel::load
            )
            is HomeUiState.Empty -> HomeEmptyScreen(onNavigate = onNavigate)
            is HomeUiState.Success -> HomeContent(state = state, onNavigate = onNavigate)
        }
    }
}

@Composable
private fun HomeEmptyScreen(onNavigate: (String) -> Unit) {
    val navFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { buttonFocusRequester.requestFocus() }
    }

    Column(Modifier.fillMaxSize()) {
        TopNavBar(
            transparentBackground = false,
            selectedIndex = 0,
            selectedItemFocusRequester = navFocusRequester,
            contentFocusRequester = buttonFocusRequester,
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) }
        )
        HomeEmptyState(
            onBrowseAddons = { onNavigate(MangoRoutes.SETTINGS_ADDONS) },
            modifier = Modifier.weight(1f),
            buttonFocusRequester = buttonFocusRequester,
            buttonFocusUp = navFocusRequester
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val playFocusRequester = remember { FocusRequester() }
    val homeNavFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
        }
    }

    LaunchedEffect(state) {
        if (!hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            // Land on the "Home" nav tab by default, not the Play button —
            // homeNavFocusRequester targets the nav bar overlay, which is
            // always composed (unlike anything inside the LazyColumn below),
            // so this is safe even before the list has laid out.
            runCatching { homeNavFocusRequester.requestFocus() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "hero") {
                HeroSection(
                    items = state.heroItems,
                    playFocusRequester = playFocusRequester,
                    onPlay = {},
                    onAddToList = {},
                    onMoreInfo = {},
                    navUpFocusRequester = homeNavFocusRequester,
                    onNavigateUpPastHero = {
                        // Imperative, not focusProperties-driven: pressing UP
                        // from any hero button forces the list back to true
                        // top and moves focus straight to the (always
                        // composed, never-virtualized) nav bar, rather than
                        // routing through a FocusRequester on a lazily
                        // composed list item — see HeroSection for why that
                        // approach crashed.
                        coroutineScope.launch {
                            listState.scrollToItem(0, 0)
                            runCatching { homeNavFocusRequester.requestFocus() }
                        }
                    }
                )
            }
            itemsIndexed(state.sections, key = { _, section -> section.id }) { index, section ->
                ContentRow(
                    section = section,
                    onItemClick = {},
                    modifier = Modifier.padding(bottom = MangoDimens.RowSpacing),
                    onNavigateUpPastRow = if (index == 0) {
                        {
                            // Hero buttons no longer auto-scroll into view
                            // (see HeroSection) — returning to them from the
                            // first content row needs to be explicit too.
                            coroutineScope.launch {
                                listState.scrollToItem(0, 0)
                                runCatching { playFocusRequester.requestFocus() }
                            }
                        }
                    } else {
                        null
                    }
                )
            }
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(48.dp))
            }
        }

        TopNavBar(
            transparentBackground = !isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter),
            selectedIndex = 0,
            selectedItemFocusRequester = homeNavFocusRequester,
            contentFocusRequester = playFocusRequester,
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) },
            onNavigateDown = {
                // Scroll first, then focus — guarantees the Play button's
                // hero item is actually composed before we try to focus it.
                coroutineScope.launch {
                    listState.scrollToItem(0, 0)
                    runCatching { playFocusRequester.requestFocus() }
                }
            }
        )
    }
}
