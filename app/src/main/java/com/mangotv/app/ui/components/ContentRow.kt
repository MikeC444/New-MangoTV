package com.mangotv.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary

@OptIn(ExperimentalFoundationApi::class)
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
    var rowSize by remember { mutableStateOf(IntSize.Zero) }

    // Moving focus between cards within this row still changes which
    // element is focused, and the framework's own automatic "keep the
    // focused thing in view" behavior reports that CARD's rect up to the
    // enclosing (vertical) LazyColumn — which shifts slightly as each card
    // scale-animates on focus (TvFocusSurface's focusedScale), reading as
    // "slightly outside the viewport" and nudging the whole page on every
    // horizontal move. Reporting this row's own fixed bounds instead of the
    // focused card's shifting sub-rect makes what the outer list sees
    // invariant to horizontal navigation — it only reacts when the focused
    // ROW itself actually changes.
    val rowBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                return Rect(0f, 0f, rowSize.width.toFloat(), rowSize.height.toFloat())
            }

            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // No-op: this wrapper isn't itself scrollable — the LazyRow
                // below already resolves its own horizontal
                // scroll-into-view before a request reaches here. This
                // exists only to normalize the rect handed further up (see
                // calculateRectForParent).
            }
        }
    }

    Column(
        modifier = modifier
            .onSizeChanged { rowSize = it }
            .bringIntoViewResponder(rowBringIntoViewResponder)
    ) {
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
