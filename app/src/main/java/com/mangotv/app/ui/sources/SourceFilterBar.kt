package com.mangotv.app.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.ui.components.TvFocusSurface
import com.mangotv.app.ui.theme.MangoAmber
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

enum class SourceFilter(val label: String, val tier: ResolutionTier?, val icon: ImageVector?) {
    ALL("All Sources", null, null),
    UHD_4K("4K", ResolutionTier.UHD_4K, Icons.Filled.HighQuality),
    FHD_1080P("1080p", ResolutionTier.FHD_1080P, Icons.Filled.HighQuality),
    HD_720P("720p", ResolutionTier.HD_720P, Icons.Filled.HighQuality),
    OTHER("Other", ResolutionTier.OTHER, Icons.Filled.Layers)
}

enum class SourceSort(val label: String) {
    QUALITY("Quality"),
    SEEDERS("Seeders"),
    SIZE("Size");

    fun next(): SourceSort = entries[(ordinal + 1) % entries.size]
}

@Composable
fun SourceFilterBar(
    selectedFilter: SourceFilter,
    onFilterChange: (SourceFilter) -> Unit,
    selectedSort: SourceSort,
    onSortChange: (SourceSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LazyRow rather than a plain Row: a plain Row's non-scrollable
        // children can end up squeezed narrower than their natural width
        // when the available space is tight (e.g. on a narrower physical
        // screen than expected), and without a scroll container there's
        // nowhere for the overflow to go — it forced text like "1080p" to
        // wrap character-by-character instead. A LazyRow always measures
        // each item at its natural width and simply scrolls if they don't
        // all fit, so every filter stays fully legible and reachable.
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SourceFilter.entries) { filter ->
                FilterPill(
                    label = filter.label,
                    icon = filter.icon,
                    selected = filter == selectedFilter,
                    onClick = { onFilterChange(filter) }
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // No overlay/dropdown component exists anywhere in this app, and
        // Compose's Material3 DropdownMenu has no built-in D-pad focus
        // handling — clicking this cycles through sort options instead of
        // opening a list, keeping the same pill+chevron look without
        // introducing the app's first popup on a D-pad-only device.
        SortPill(
            sort = selectedSort,
            onClick = { onSortChange(selectedSort.next()) }
        )
    }
}

@Composable
private fun FilterPill(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (focused || selected) TextPrimary else TextSecondary
    TvFocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        backgroundColor = if (selected) MangoAmber.copy(alpha = 0.25f) else MangoSurfaceHigh,
        onFocusChanged = { focused = it },
        bringIntoViewOnFocus = false
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.width(14.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun SortPill(sort: SourceSort, onClick: () -> Unit) {
    TvFocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        backgroundColor = MangoSurfaceHigh,
        bringIntoViewOnFocus = false
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by: ${sort.label}",
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Change sort order",
                tint = TextSecondary,
                modifier = Modifier.width(18.dp)
            )
        }
    }
}
