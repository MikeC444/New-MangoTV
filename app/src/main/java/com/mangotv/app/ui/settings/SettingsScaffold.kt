package com.mangotv.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.mangotv.app.navigation.routeForNavLabel
import com.mangotv.app.ui.home.MangoNavItems
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary

/**
 * Shared shell for every Settings-family screen: the same persistent top nav
 * (with "Settings" highlighted) plus a title, so the D-pad focus-anchoring
 * fix used on Home (nav <-> first content row is a deterministic seam, not
 * left to the default spatial-search heuristic) is applied consistently
 * everywhere instead of being a one-off Home fix.
 */
@Composable
fun SettingsScaffold(
    title: String,
    onNavigate: (String) -> Unit,
    navFocusRequester: FocusRequester,
    firstContentFocusRequester: FocusRequester,
    content: @Composable ColumnScope.() -> Unit
) {
    val settingsIndex = remember { MangoNavItems.indexOf("Settings") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MangoBackground)
    ) {
        TopNavBar(
            transparentBackground = false,
            selectedIndex = settingsIndex,
            selectedItemFocusRequester = navFocusRequester,
            contentFocusRequester = firstContentFocusRequester,
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) }
        )
        Column(
            modifier = Modifier.padding(
                horizontal = MangoDimens.ScreenPaddingHorizontal,
                vertical = 28.dp
            )
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}
