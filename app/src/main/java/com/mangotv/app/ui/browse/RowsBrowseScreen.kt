package com.mangotv.app.ui.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.navigation.routeForNavLabel
import com.mangotv.app.ui.components.ContentRow
import com.mangotv.app.ui.components.FullScreenErrorState
import com.mangotv.app.ui.components.RowsLoadingSkeleton
import com.mangotv.app.ui.home.MangoNavItems
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoMotion
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

sealed interface RowsBrowseUiState {
    data object Loading : RowsBrowseUiState
    data class Loaded(val sections: List<HomeSection>) : RowsBrowseUiState
    data class Error(val message: String) : RowsBrowseUiState
}

/**
 * Shared shell for any "stack of ContentRows under the nav bar, no hero"
 * screen — currently Movies, TV Shows, and Genre Results. Structurally
 * HomeScreen's own HomeContent minus the hero item: same nav<->content
 * focus-seam mechanism (a navRegionFocused lock instead of Home's
 * heroRegionFocused, since there's no intermediate hero region here — the
 * nav bar borders row content directly) and the same explicit
 * animateScrollBy row-centering effect, both copied deliberately rather
 * than re-derived, since this app fought several rounds of real stutter/
 * shake bugs to arrive at them on Home.
 *
 * Not wrapped in SettingsScaffold: ContentRow supplies its own
 * ScreenPaddingHorizontal via its LazyRow's contentPadding, and
 * SettingsScaffold's content slot applies that same padding again --
 * stacking both would double the left/right margin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowsBrowseContent(
    screenTitle: String,
    navLabel: String,
    uiState: RowsBrowseUiState,
    onNavigate: (String) -> Unit,
    onRetry: () -> Unit,
    emptyMessage: String = "Nothing to show here right now."
) {
    Box(Modifier.fillMaxSize().background(MangoBackground)) {
        when (uiState) {
            is RowsBrowseUiState.Loading -> RowsLoadingSkeleton()
            is RowsBrowseUiState.Error -> FullScreenErrorState(message = uiState.message, onRetry = onRetry)
            is RowsBrowseUiState.Loaded -> RowsBrowseLoadedContent(screenTitle, navLabel, uiState.sections, onNavigate, emptyMessage)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowsBrowseLoadedContent(
    screenTitle: String,
    navLabel: String,
    sections: List<HomeSection>,
    onNavigate: (String) -> Unit,
    emptyMessage: String
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val navFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    // Mirrors HomeContent's heroRegionFocused, minus the intermediate hero
    // region this screen doesn't have: true while focus is in the nav bar
    // (list stays pinned at the top), false once focus has moved into row
    // content (normal centering/scrolling takes over).
    var navRegionFocused by remember { mutableStateOf(true) }
    var focusedRowIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(sections) {
        if (!hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            runCatching { navFocusRequester.requestFocus() }
        }
    }

    val navScrollLock = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (navRegionFocused) available else Offset.Zero
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (navRegionFocused && (index != 0 || offset != 0)) {
                    listState.scrollToItem(0, 0)
                }
            }
    }

    LaunchedEffect(focusedRowIndex, navRegionFocused) {
        val rowIndex = focusedRowIndex ?: return@LaunchedEffect
        if (navRegionFocused) return@LaunchedEffect
        val lazyIndex = rowIndex + 1 // offset for the title item at index 0
        val info = listState.layoutInfo.visibleItemsInfo.find { it.index == lazyIndex }
        if (info != null) {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val itemCenter = info.offset + info.size / 2f
            val delta = itemCenter - viewportHeight / 2f
            listState.animateScrollBy(delta)
        } else {
            listState.animateScrollToItem(lazyIndex)
        }
    }

    fun navigateToContent(target: Content) {
        val providerId = target.providerId ?: return
        onNavigate(MangoRoutes.detail(providerId, target.type, target.id))
    }

    Box(Modifier.fillMaxSize()) {
        if (sections.isEmpty()) {
            Text(
                text = emptyMessage,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = MangoDimens.ScreenPaddingHorizontal)
            )
        } else {
            CompositionLocalProvider(LocalBringIntoViewSpec provides MangoMotion.DisabledBringIntoViewSpec) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .nestedScroll(navScrollLock)
                        .fillMaxSize()
                        .padding(top = MangoDimens.NavBarHeight + 24.dp)
                ) {
                    item(key = "title") {
                        Text(
                            text = screenTitle,
                            color = TextPrimary,
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(
                                horizontal = MangoDimens.ScreenPaddingHorizontal,
                                vertical = 4.dp
                            )
                        )
                    }
                    itemsIndexed(sections, key = { _, section -> section.id }) { index, section ->
                        ContentRow(
                            section = section,
                            onItemClick = ::navigateToContent,
                            modifier = Modifier.padding(bottom = MangoDimens.RowSpacing),
                            posterScale = 0.75f,
                            onFocusChanged = { hasFocus -> if (hasFocus) focusedRowIndex = index },
                            firstItemFocusRequester = if (index == 0) firstCardFocusRequester else null,
                            onNavigateUpPastRow = if (index == 0) {
                                {
                                    navRegionFocused = true
                                    coroutineScope.launch {
                                        listState.scrollToItem(0, 0)
                                        runCatching { navFocusRequester.requestFocus() }
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
            }
        }

        TopNavBar(
            transparentBackground = false,
            modifier = Modifier.align(Alignment.TopCenter),
            selectedIndex = MangoNavItems.indexOf(navLabel),
            selectedItemFocusRequester = navFocusRequester,
            contentFocusRequester = if (sections.isNotEmpty()) firstCardFocusRequester else null,
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) },
            onNavigateDown = if (sections.isNotEmpty()) {
                {
                    navRegionFocused = false
                    coroutineScope.launch {
                        listState.scrollToItem(0, 0)
                        runCatching { firstCardFocusRequester.requestFocus() }
                    }
                }
            } else {
                null
            }
        )
    }
}

@Composable
fun MoviesScreen(onNavigate: (String) -> Unit, viewModel: MoviesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RowsBrowseContent(
        screenTitle = "Movies",
        navLabel = "Movies",
        uiState = uiState,
        onNavigate = onNavigate,
        onRetry = viewModel::load
    )
}

@Composable
fun TvShowsScreen(onNavigate: (String) -> Unit, viewModel: TvShowsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RowsBrowseContent(
        screenTitle = "TV Shows",
        navLabel = "TV Shows",
        uiState = uiState,
        onNavigate = onNavigate,
        onRetry = viewModel::load
    )
}
