package com.mangotv.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    val scrollTopAnchorFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
        }
    }

    LaunchedEffect(state) {
        if (!hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            runCatching { playFocusRequester.requestFocus() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // A zero-footprint focus waypoint between the nav bar and the
            // hero. If the hero's own auto-scroll-into-view ever leaves the
            // list somewhere other than true index 0 (e.g. after an addon
            // install remounts this screen), pressing UP from the hero
            // buttons lands here and forces the list back to the real top —
            // a deterministic recovery path that doesn't depend on guessing
            // exactly why the scroll drifted.
            item(key = "scroll_top_anchor") {
                ScrollToTopAnchor(
                    focusRequester = scrollTopAnchorFocusRequester,
                    focusUp = homeNavFocusRequester,
                    focusDown = playFocusRequester,
                    onFocused = { coroutineScope.launch { listState.scrollToItem(0, 0) } }
                )
            }
            item(key = "hero") {
                HeroSection(
                    items = state.heroItems,
                    playFocusRequester = playFocusRequester,
                    onPlay = {},
                    onAddToList = {},
                    onMoreInfo = {},
                    navUpFocusRequester = scrollTopAnchorFocusRequester
                )
            }
            items(state.sections, key = { it.id }) { section ->
                ContentRow(
                    section = section,
                    onItemClick = {},
                    modifier = Modifier.padding(bottom = MangoDimens.RowSpacing)
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
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) }
        )
    }
}

/**
 * Invisible (no size beyond a hairline, no focus visuals) focusable item
 * sitting between the nav bar and the hero in the list. Its only job is to
 * force the list back to true index 0 the moment it's focused — see the
 * comment at its call site for why this exists.
 */
@Composable
private fun ScrollToTopAnchor(
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    focusUp: FocusRequester? = null,
    focusDown: FocusRequester? = null
) {
    var anchorModifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .focusRequester(focusRequester)
        .onFocusChanged { state -> if (state.isFocused) onFocused() }
    if (focusUp != null || focusDown != null) {
        anchorModifier = anchorModifier.focusProperties {
            focusUp?.let { up = it }
            focusDown?.let { down = it }
        }
    }
    Box(modifier = anchorModifier.focusable())
}
