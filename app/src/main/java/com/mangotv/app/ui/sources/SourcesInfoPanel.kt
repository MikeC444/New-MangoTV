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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.ui.components.HeroIconButton
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
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
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MangoBackground.copy(alpha = 0.82f))
        )

        Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
            HeroIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )

            Spacer(Modifier.height(24.dp))

            AsyncImage(
                model = rememberOpaqueImageRequest(content.posterUrl),
                contentDescription = content.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 110.dp, height = 165.dp)
                    .background(MangoSurfaceHigh, RoundedCornerShape(MangoDimens.CardCornerRadius))
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                content.year?.let {
                    Text(
                        text = it.toString(),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                content.ageRating?.let { rating ->
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, TextTertiary), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = rating, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (content.logoUrl != null) {
                AsyncImage(
                    model = content.logoUrl,
                    contentDescription = content.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 260.dp, height = 70.dp)
                )
            } else {
                Text(
                    text = content.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            val genresAndRuntime = buildList {
                if (content.genres.isNotEmpty()) add(content.genres.joinToString("  •  ") { it.name })
                content.runtimeMinutes?.let { add("${it / 60}h ${it % 60}m") }
            }
            if (genresAndRuntime.isNotEmpty()) {
                Text(
                    text = genresAndRuntime.joinToString("   "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
            }

            if (content.description.isNotBlank()) {
                Text(
                    text = content.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MangoSurfaceHigh, RoundedCornerShape(MangoDimens.CardCornerRadius))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.width(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "What are sources?",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Sources are different streams or files available online. Choose the one that works best for you.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}
