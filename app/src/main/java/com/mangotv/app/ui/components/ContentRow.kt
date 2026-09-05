package com.mangotv.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary

@Composable
fun ContentRow(
    section: HomeSection,
    onItemClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
    // Only the first row needs this: its hero buttons no longer
    // auto-scroll into view on focus (see HeroSection), so returning to
    // them from here needs to be handled explicitly instead.
    onNavigateUpPastRow: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = section.title,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(
                horizontal = MangoDimens.ScreenPaddingHorizontal,
                vertical = 12.dp
            )
        )
        LazyRow(
            modifier = if (onNavigateUpPastRow != null) {
                Modifier.onPreviewKeyEvent { event ->
                    // Consume both KeyDown and KeyUp for this key — an
                    // unconsumed KeyUp can fall through to Compose's default
                    // focus-move handling and trigger its own scroll-into-
                    // view, independent of bringIntoViewOnFocus.
                    if (event.key == Key.DirectionUp) {
                        if (event.type == KeyEventType.KeyDown) {
                            onNavigateUpPastRow()
                        }
                        true
                    } else {
                        false
                    }
                }
            } else {
                Modifier
            },
            contentPadding = PaddingValues(horizontal = MangoDimens.ScreenPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(MangoDimens.CardSpacing)
        ) {
            items(section.items, key = { it.id }) { content ->
                ContentCard(
                    content = content,
                    style = section.style,
                    onClick = { onItemClick(content) }
                )
            }
        }
    }
}
