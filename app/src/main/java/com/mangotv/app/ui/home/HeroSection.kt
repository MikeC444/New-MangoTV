package com.mangotv.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.ui.components.MangoButton
import com.mangotv.app.ui.components.MangoButtonStyle
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoMotion
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester

private const val HERO_ROTATE_MILLIS = 9000L

@Composable
fun HeroSection(
    items: List<Content>,
    playFocusRequester: FocusRequester,
    onPlay: (Content) -> Unit,
    onAddToList: (Content) -> Unit,
    onMoreInfo: (Content) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    val current = items[index % items.size]

    LaunchedEffect(items) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(HERO_ROTATE_MILLIS)
            index = (index + 1) % items.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MangoDimens.HeroHeight)
    ) {
        Crossfade(
            targetState = current,
            animationSpec = tween(MangoMotion.HeroCrossfadeMillis),
            label = "heroBackdrop"
        ) { item ->
            KenBurnsBackdrop(url = item.backdropUrl)
        }

        // Left-to-right dark gradient so text stays legible over any artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
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

        // Bottom fade so the hero blends into the row content below
        Box(
            modifier = Modifier
                .fillMaxSize()
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = MangoDimens.ScreenPaddingHorizontal,
                    end = MangoDimens.ScreenPaddingHorizontal,
                    bottom = 56.dp
                )
                .widthIn(max = 760.dp)
        ) {
            Text(
                text = current.title,
                color = TextPrimary,
                style = MaterialTheme.typography.displayMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row {
                val metaParts = buildList {
                    current.year?.let { add(it.toString()) }
                    current.ageRating?.let { add(it) }
                    current.runtimeMinutes?.let { add("${it / 60}h ${it % 60}m") }
                    current.rating?.let { add("★ ${"%.1f".format(it)}") }
                }
                Text(
                    text = metaParts.joinToString("   •   "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (current.genres.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = current.genres.joinToString("  ·  ") { it.name },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = current.description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(28.dp))

            Row {
                MangoButton(
                    text = "Play",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { onPlay(current) },
                    style = MangoButtonStyle.FILLED,
                    focusRequester = playFocusRequester
                )
                Spacer(Modifier.width(16.dp))
                MangoButton(
                    text = "My List",
                    icon = Icons.Filled.Add,
                    onClick = { onAddToList(current) },
                    style = MangoButtonStyle.GLASS
                )
                Spacer(Modifier.width(16.dp))
                MangoButton(
                    text = "More Info",
                    icon = Icons.Filled.Info,
                    onClick = { onMoreInfo(current) },
                    style = MangoButtonStyle.GLASS
                )
            }
        }
    }
}

@Composable
private fun KenBurnsBackdrop(url: String?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "kenBurns")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(MangoMotion.HeroKenBurnsMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kenBurnsScale"
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
