package com.mangotv.app.ui.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.navigation.routeForNavLabel
import com.mangotv.app.ui.components.ContentRow
import com.mangotv.app.ui.components.FullScreenErrorState
import com.mangotv.app.ui.components.HomeLoadingSkeleton
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoMotion
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangoBackground)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> HomeLoadingSkeleton()
            is DetailUiState.Error -> FullScreenErrorState(
                message = state.message,
                onRetry = viewModel::load
            )
            is DetailUiState.Success -> DetailContent(
                content = state.content,
                similar = state.similar,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
private fun DetailContent(
    content: Content,
    similar: List<Content>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val playFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    // Same "keep the nav bar / hero region completely static, only scroll
    // when focus genuinely moves into the content below" setup already
    // proven on Home — see HomeScreen.kt for the full rationale on why both
    // the NestedScrollConnection block and the snapshotFlow watchdog exist.
    var heroRegionFocused by remember { mutableStateOf(true) }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
        }
    }

    LaunchedEffect(content.id) {
        if (!hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            runCatching { playFocusRequester.requestFocus() }
        }
    }

    val heroScrollLock = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (heroRegionFocused) available else Offset.Zero
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (heroRegionFocused && (index != 0 || offset != 0)) {
                    listState.scrollToItem(0, 0)
                }
            }
    }

    fun returnToHero() {
        heroRegionFocused = true
        coroutineScope.launch {
            listState.scrollToItem(0, 0)
            runCatching { playFocusRequester.requestFocus() }
        }
    }

    fun navigateToContent(target: Content) {
        val providerId = target.providerId ?: return
        onNavigate(MangoRoutes.detail(providerId, target.type, target.id))
    }

    // Movies only, for now: shrink the whole page so it fits on one screen
    // without scrolling. TV shows (whether they have season data or not)
    // keep the existing, larger layout untouched.
    val compact = content.type == ContentType.MOVIE

    // Crossfades the fixed backdrop from sharp to blurred once focus
    // leaves the hero/nav region — reusing the same signal that already
    // drives the scroll pinning above — matching how Nuvio recedes/blurs
    // its page background once you've scrolled down into Cast/Seasons
    // rather than leaving it sharp the whole way down.
    val blurAlpha by animateFloatAsState(
        targetValue = if (heroRegionFocused) 0f else 1f,
        animationSpec = tween(400),
        label = "detailBackdropBlurAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Fixed, full-screen backdrop that stays put behind the scrolling
        // content instead of scrolling away with the hero item — the page
        // background the way Nuvio treats it, rather than an image
        // confined to a "hero" region.
        KenBurnsBackdrop(
            url = content.backdropUrl,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MangoSurfaceHigh)
                .alpha(1f - blurAlpha)
        )

        // The "blurred" state: the same image loaded at a tiny target size
        // and stretched to fill the screen. RenderEffect-based
        // Modifier.blur() only works on Android 12+ (API 31) and is a
        // silent no-op below that — real Fire TV hardware runs a wide
        // spread of older Android/FireOS versions, and on-device testing
        // confirmed blur() wasn't visibly doing anything there. Stretching
        // a heavily downsampled bitmap back up produces a genuine blurred
        // look through ordinary bitmap scaling, so it works on every API
        // level this app supports (minSdk 23), not just the newest
        // hardware. Crossfaded with the sharp version above via alpha
        // rather than swapped outright, for a smooth transition.
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(content.backdropUrl)
                .size(64, 36)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MangoSurfaceHigh)
                .alpha(blurAlpha)
        )

        // Left-to-right gradient so hero text stays legible. Cast/Seasons
        // further down sit on their own opaque card backgrounds rather
        // than directly on the image, so unlike before this doesn't also
        // need a bottom fade — the blur above now handles de-emphasizing
        // the backdrop once scrolled that far.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MangoBackground.copy(alpha = 0.55f),
                            MangoBackground.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .nestedScroll(heroScrollLock)
                .fillMaxSize()
        ) {
            item(key = "hero") {
                DetailHeroSection(
                    content = content,
                    playFocusRequester = playFocusRequester,
                    onPlay = {},
                    onWatchlist = {},
                    onMore = {},
                    navUpFocusRequester = navFocusRequester,
                    onNavigateUpPastHero = { returnToHero() },
                    onNavigateDownFromHero = { heroRegionFocused = false },
                    compact = compact
                )
            }
            item(key = "seasons_or_cast_and_similar") {
                // TV shows with real season/episode data get a season
                // picker + episode list instead — cast and "similar" don't
                // apply the same way once there's something more useful
                // (and more central to actually watching the show) to show.
                if (content.type == ContentType.TV_SHOW && content.seasons.isNotEmpty()) {
                    SeasonsSection(
                        seasons = content.seasons,
                        modifier = Modifier.fillMaxWidth(),
                        onNavigateUpPastRow = { returnToHero() }
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CastRow(
                            cast = content.cast,
                            modifier = Modifier.weight(1f),
                            onNavigateUpPastRow = { returnToHero() },
                            compact = compact
                        )
                        if (similar.isNotEmpty()) {
                            // Movies get the smaller, landscape-card
                            // treatment; the TV show fallback (a show with
                            // no season data) keeps the original ContentRow
                            // layout, since compact is only ever true for
                            // movies.
                            if (compact) {
                                SimilarRow(
                                    title = "You May Also Like",
                                    items = similar,
                                    onItemClick = ::navigateToContent,
                                    modifier = Modifier.weight(2f),
                                    onNavigateUpPastRow = { returnToHero() }
                                )
                            } else {
                                ContentRow(
                                    section = HomeSection(
                                        id = "similar",
                                        title = "You May Also Like",
                                        items = similar
                                    ),
                                    onItemClick = ::navigateToContent,
                                    modifier = Modifier.weight(2f),
                                    onNavigateUpPastRow = { returnToHero() }
                                )
                            }
                        }
                    }
                }
            }
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(if (compact) 16.dp else 48.dp))
            }
        }

        TopNavBar(
            transparentBackground = !isScrolled,
            modifier = Modifier.align(Alignment.TopCenter),
            selectedIndex = 0,
            selectedItemFocusRequester = navFocusRequester,
            contentFocusRequester = playFocusRequester,
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) },
            onNavigateDown = { returnToHero() }
        )
    }
}

@Composable
private fun KenBurnsBackdrop(url: String?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "detailKenBurns")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(MangoMotion.HeroKenBurnsMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "detailKenBurnsScale"
    )

    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}
