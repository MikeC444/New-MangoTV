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
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary

@Composable
fun ContentRow(
    section: HomeSection,
    onItemClick: (Content) -> Unit,
    modifier: Modifier = Modifier
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
