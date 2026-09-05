package com.mangotv.app.ui.sources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.ui.components.GlowPlayBadge
import com.mangotv.app.ui.components.HeroIconButton
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.theme.DividerSubtle
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

@Composable
fun SourcesInfoPanel(
    content: Content,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxHeight()) {
        AsyncImage(
            model = rememberOpaqueImageRequest(content.backdropUrl ?: content.posterUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        )
        // Two stacked gradients (mirrors HeroSection's own pattern) rather
        // than a flat scrim, so the backdrop reads as integrated into the
        // dark UI instead of a pasted rectangle. Both reach fully opaque
        // MangoBackground at the far edge so there's no seam against the
        // right column's flat black background — lightened near the near
        // edge (top-left) so the photo itself reads clearly instead of
        // being mostly obscured.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MangoBackground.copy(alpha = 0.2f),
                        0.5f to MangoBackground.copy(alpha = 0.45f),
                        1f to MangoBackground
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MangoBackground.copy(alpha = 0.1f),
                        0.6f to MangoBackground.copy(alpha = 0.5f),
                        1f to MangoBackground
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
            HeroIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )

            Spacer(Modifier.height(16.dp))

            val posterShape = RoundedCornerShape(MangoDimens.CardCornerRadius)
            AsyncImage(
                model = rememberOpaqueImageRequest(content.posterUrl),
                contentDescription = content.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 84.dp, height = 126.dp)
                    // shadow (unclipped) -> clip -> background -> border:
                    // the same ordering TvFocusSurface already established,
                    // since clipping before applying elevation would cut
                    // the shadow away entirely.
                    .shadow(elevation = 10.dp, shape = posterShape, clip = false)
                    .clip(posterShape)
                    .background(MangoSurfaceHigh, posterShape)
                    .border(BorderStroke(1.dp, DividerSubtle), posterShape)
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                content.year?.let {
                    Text(
                        text = it.toString(),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                content.ageRating?.let { rating ->
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, TextTertiary), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(text = rating, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (content.logoUrl != null) {
                AsyncImage(
                    model = content.logoUrl,
                    contentDescription = content.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 200.dp, height = 54.dp)
                )
            } else {
                Text(
                    text = content.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            val genresAndRuntime = buildList {
                if (content.genres.isNotEmpty()) add(content.genres.joinToString("  •  ") { it.name })
                content.runtimeMinutes?.let { add("${it / 60}h ${it % 60}m") }
            }
            if (genresAndRuntime.isNotEmpty()) {
                Text(
                    text = genresAndRuntime.joinToString("   "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(10.dp))
            }

            if (content.description.isNotBlank()) {
                Text(
                    text = content.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MangoSurfaceHigh, RoundedCornerShape(MangoDimens.CardCornerRadius))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.width(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "What are sources?",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sources are different streams or files available online. Choose the one that works best for you.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(10.dp))
                GlowPlayBadge(size = 30.dp, glowSize = 46.dp, iconSize = 15.dp)
            }
        }
    }
}
