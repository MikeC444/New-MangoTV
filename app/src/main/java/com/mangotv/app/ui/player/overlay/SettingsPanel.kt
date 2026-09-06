package com.mangotv.app.ui.player.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.PlayerPreferences
import com.mangotv.app.ui.components.MangoButton
import com.mangotv.app.ui.components.MangoButtonStyle

/** Fixed 0.5x-2x set — Media3 supports pitch-preserving speed uniformly for
 * direct/HLS/DASH VOD, so no runtime capability check is needed here. */
private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun nextPlaybackSpeed(current: Float): Float {
    val index = PLAYBACK_SPEEDS.indexOf(current).takeIf { it >= 0 } ?: PLAYBACK_SPEEDS.indexOf(1f)
    return PLAYBACK_SPEEDS[(index + 1) % PLAYBACK_SPEEDS.size]
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"

/**
 * Aggregates the player's secondary controls: links into the Subtitles/
 * Audio/Quality menus (only the ones actually worth showing — see the
 * showX params, same ">1 real option" rule that gates their icons in the
 * bottom row), a cycle-on-click playback speed row (same "no popup
 * component exists, cycle through options on click" pattern
 * SourceFilterBar's sort pill already established), Autoplay/Skip Intro
 * toggles, read-only source info, and Change Source.
 */
@Composable
fun SettingsPanel(
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    preferences: PlayerPreferences,
    onAutoplayChange: (Boolean) -> Unit,
    onSkipIntroChange: (Boolean) -> Unit,
    showSubtitles: Boolean,
    showAudio: Boolean,
    showQuality: Boolean,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenSourceInfo: () -> Unit,
    onChangeSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocusRequester.requestFocus() } }

    var requestedFirstFocus = false
    fun consumeFirstFocusRequester(): FocusRequester? =
        if (!requestedFirstFocus) { requestedFirstFocus = true; firstFocusRequester } else null

    MenuOverlayScaffold(title = "Settings", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (showSubtitles) {
                MenuOptionRow(label = "Subtitles", isSelected = false, onClick = onOpenSubtitles, focusRequester = consumeFirstFocusRequester())
            }
            if (showAudio) {
                MenuOptionRow(label = "Audio", isSelected = false, onClick = onOpenAudio, focusRequester = consumeFirstFocusRequester())
            }
            if (showQuality) {
                MenuOptionRow(label = "Quality", isSelected = false, onClick = onOpenQuality, focusRequester = consumeFirstFocusRequester())
            }
            MenuOptionRow(
                label = "Playback speed",
                isSelected = false,
                supportingText = formatSpeed(playbackSpeed),
                onClick = { onPlaybackSpeedChange(nextPlaybackSpeed(playbackSpeed)) },
                focusRequester = consumeFirstFocusRequester()
            )
            MenuOptionRow(
                label = "Autoplay next episode",
                isSelected = false,
                supportingText = if (preferences.autoplayNextEpisode) "On" else "Off",
                onClick = { onAutoplayChange(!preferences.autoplayNextEpisode) },
                focusRequester = consumeFirstFocusRequester()
            )
            MenuOptionRow(
                label = "Skip intro",
                isSelected = false,
                supportingText = if (preferences.skipIntroEnabled) "On" else "Off",
                onClick = { onSkipIntroChange(!preferences.skipIntroEnabled) },
                focusRequester = consumeFirstFocusRequester()
            )
            MenuOptionRow(label = "Source info", isSelected = false, onClick = onOpenSourceInfo, focusRequester = consumeFirstFocusRequester())
        }

        Spacer(Modifier.height(20.dp))

        MangoButton(
            text = "Change Source",
            icon = Icons.Filled.SwapHoriz,
            onClick = onChangeSource,
            style = MangoButtonStyle.GLASS
        )
    }
}
