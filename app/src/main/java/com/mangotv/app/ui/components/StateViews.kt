package com.mangotv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

@Composable
fun FullScreenErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangoBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.height(48.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
            Text(
                text = "Something went wrong",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(28.dp))
            MangoButton(
                text = "Retry",
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
                style = MangoButtonStyle.FILLED
            )
        }
    }
}

@Composable
fun HomeEmptyState(
    onBrowseAddons: () -> Unit,
    modifier: Modifier = Modifier,
    buttonFocusRequester: FocusRequester? = null,
    buttonFocusUp: FocusRequester? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangoBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Extension,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.height(48.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
            Text(
                text = "Your library is empty",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            Text(
                text = "Install an addon to bring movies and TV shows into Mango TV.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(28.dp))
            MangoButton(
                text = "Browse Addons",
                icon = Icons.Filled.Extension,
                onClick = onBrowseAddons,
                style = MangoButtonStyle.FILLED,
                focusRequester = buttonFocusRequester,
                focusUp = buttonFocusUp
            )
        }
    }
}
