package com.mangotv.app.ui.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.ui.components.MangoButton
import com.mangotv.app.ui.components.MangoButtonStyle
import com.mangotv.app.ui.components.TvFocusSurface
import com.mangotv.app.ui.theme.MangoAmber
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoMotion
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

/**
 * Detail screen's backdrop hero: title/meta/description/actions bottom-left,
 * an IMDb-style rating badge bottom-right. Mirrors HeroSection's proven
 * layout (heightIn floor + matchParentSize + bottom-aligned content, so a
 * long description never silently overflows above the box) and its focus
 * handling at the nav<->hero seam (imperative scroll-then-focus on UP,
 * non-consuming notify-only on DOWN) so this screen doesn't need to
 * rediscover the same scrolling bugs Home already hit.
 */
@Composable
fun DetailHeroSection(
    content: Content,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onWatchlist: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    navUpFocusRequester: FocusRequester? = null,
    onNavigateUpPastHero: () -> Unit = {},
    onNavigateDownFromHero: () -> Unit = {},
    // Movie detail only, for now: shrinks everything except the title so
    // the whole page (hero + Cast + You May Also Like) fits on one screen
    // without scrolling. TV shows don't pass this and are unaffected.
    compact: Boolean = false
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val heroMinHeight = if (compact) {
        // Reserve just enough fixed space below the hero for the Cast /
        // You May Also Like row (~130dp) plus the bottom spacer (16dp),
        // and give the hero everything else — rather than a fixed
        // fraction of the screen, this scales with the actual screen
        // height so the hero (and therefore where description/buttons/
        // Cast end up) stretches to fill exactly what's left, with Cast /
        // You May Also Like landing right at the bottom instead of
        // sitting high up with dead space beneath it.
        (screenHeightDp - 176.dp).coerceAtLeast(320.dp)
    } else {
        screenHeightDp * 0.82f
    }
    val bottomPadding = if (compact) 24.dp else 56.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = heroMinHeight)
            // Neutral fallback so the backdrop area reads as "still
            // loading" rather than pure black for however long Coil takes
            // to fetch this specific image — a fresh detail page always
            // needs a brand new (likely uncached) backdrop, unlike Home's
            // rows where images have usually already loaded by the time
            // they're scrolled into view.
            .background(MangoSurfaceHigh)
    ) {
        KenBurnsBackdrop(url = content.backdropUrl, modifier = Modifier.matchParentSize())

        // Left-to-right dark gradient so text stays legible over any artwork
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MangoBackground.copy(alpha = 0.95f),
                            MangoBackground.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom fade so the hero blends into the content below
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            MangoBackground.copy(alpha = 0.4f),
                            MangoBackground
                        )
                    )
                )
        )

        content.rating?.let { rating ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = MangoDimens.ScreenPaddingHorizontal, bottom = bottomPadding)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(
                        horizontal = if (compact) 14.dp else 20.dp,
                        vertical = if (compact) 8.dp else 14.dp
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MangoAmber,
                        modifier = Modifier.height(if (compact) 16.dp else 22.dp)
                    )
                    Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
                    Column {
                        Text(
                            text = "%.1f".format(rating),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "IMDb Rating",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .let { if (compact) it.fillMaxSize() else it }
                .padding(
                    start = MangoDimens.ScreenPaddingHorizontal,
                    end = MangoDimens.ScreenPaddingHorizontal,
                    // Compact: title/meta sit at a fixed distance from the
                    // top of the (now much taller) hero, clearing the nav
                    // bar overlay above it, and stay put regardless of how
                    // tall the hero grows. Non-compact: unchanged — the
                    // whole block is still bottom-anchored as one piece.
                    top = if (compact) 130.dp else 0.dp,
                    bottom = bottomPadding
                )
                .widthIn(max = 780.dp),
            verticalArrangement = if (compact) Arrangement.Top else Arrangement.Bottom
        ) {
            Text(
                text = content.title,
                color = TextPrimary,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val metaStyle = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
                val leadingParts = buildList {
                    content.year?.let { add(it.toString()) }
                    content.runtimeMinutes?.let { add("${it / 60}h ${it % 60}m") }
                }
                if (leadingParts.isNotEmpty()) {
                    Text(
                        text = leadingParts.joinToString("   "),
                        color = TextSecondary,
                        style = metaStyle,
                        fontWeight = FontWeight.Medium
                    )
                }
                content.ageRating?.let { rating ->
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, TextTertiary), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = rating, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (content.genres.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = content.genres.joinToString("  •  ") { it.name },
                        color = TextSecondary,
                        style = metaStyle,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (compact) {
                // Flexible gap: absorbs whatever room is left between the
                // meta row and the description/buttons below, so those
                // stay pinned to the bottom of the (now taller) hero
                // instead of trailing right behind the meta row — title
                // and meta above are unaffected since they're above this
                // spacer, not part of what it pushes down.
                Spacer(Modifier.weight(1f))
            }

            if (content.description.isNotBlank()) {
                Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                Text(
                    text = content.description,
                    color = TextSecondary,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    maxLines = if (compact) 2 else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(if (compact) 16.dp else 28.dp))

            // Same imperative UP/DOWN handling as HeroSection: UP is fully
            // consumed (both KeyDown/KeyUp) and drives an explicit
            // scroll-then-focus back to the nav bar; DOWN is left
            // unconsumed (just notifies) so default focus-move handling
            // still carries focus into the content below.
            Row(
                modifier = Modifier.onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.DirectionUp -> {
                            if (event.type == KeyEventType.KeyDown) {
                                onNavigateUpPastHero()
                            }
                            true
                        }
                        event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                            onNavigateDownFromHero()
                            false
                        }
                        else -> false
                    }
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                MangoButton(
                    text = "Play",
                    icon = Icons.Filled.PlayArrow,
                    onClick = onPlay,
                    style = MangoButtonStyle.FILLED,
                    focusRequester = playFocusRequester,
                    focusUp = navUpFocusRequester,
                    bringIntoViewOnFocus = false,
                    compact = compact
                )
                Spacer(Modifier.width(if (compact) 10.dp else 16.dp))
                MangoButton(
                    text = "Watchlist",
                    icon = Icons.Filled.Add,
                    onClick = onWatchlist,
                    style = MangoButtonStyle.GLASS,
                    focusUp = navUpFocusRequester,
                    bringIntoViewOnFocus = false,
                    compact = compact
                )
                Spacer(Modifier.width(if (compact) 10.dp else 16.dp))
                TvFocusSurface(
                    onClick = onMore,
                    shape = CircleShape,
                    backgroundColor = Color.White.copy(alpha = 0.12f),
                    focusUp = navUpFocusRequester,
                    bringIntoViewOnFocus = false
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More options",
                        tint = TextPrimary,
                        modifier = Modifier.padding(if (compact) 10.dp else 14.dp)
                    )
                }
            }
        }
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
                // Without this, the zoomed-in portion of the Ken Burns
                // animation (scale > 1) draws outside the backdrop's own
                // bounds — graphicsLayer scale doesn't clip to the layout
                // size by default — bleeding a sliver of the image past
                // the hero's bottom edge and over whatever's below it.
                clip = true
            }
    )
}
