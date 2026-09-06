package com.mangotv.app.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.Episode
import com.mangotv.app.data.model.Stream
import com.mangotv.app.ui.components.FullScreenErrorState
import com.mangotv.app.ui.components.HeroIconButton
import com.mangotv.app.ui.player.overlay.PlaybackErrorOverlay
import com.mangotv.app.ui.theme.MangoAmber
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onChangeSource: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel()
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackPhase by viewModel.playbackPhase.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = screenState) {
            is PlayerScreenUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                    color = MangoAmber
                )
            }
            is PlayerScreenUiState.Error -> {
                FullScreenErrorState(message = state.message, onRetry = viewModel::load)
            }
            is PlayerScreenUiState.Ready -> {
                PlaybackContent(
                    content = state.content,
                    episode = state.episode,
                    stream = state.stream,
                    phase = playbackPhase,
                    onPhaseChanged = viewModel::onPlaybackPhaseChanged,
                    onBack = onBack,
                    onChangeSource = onChangeSource
                )
            }
        }
    }
}

/**
 * Phase 1's crude control set: play/pause, a flat 10s seek, a minimal top/
 * bottom bar, and a plain error card. Replaced by the full control layout
 * (PlayerTopBar/PlayerBottomControls/PlayerTimeline/QuickActionIndicator)
 * in the next phase — this exists so the player is genuinely usable end to
 * end before any of that polish lands.
 */
@Composable
private fun PlaybackContent(
    content: Content,
    episode: Episode?,
    stream: Stream,
    phase: PlaybackPhase,
    onPhaseChanged: (PlaybackPhase) -> Unit,
    onBack: () -> Unit,
    onChangeSource: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember { buildExoPlayer(context) }

    DisposableEffect(exoPlayer) {
        val listener = PlayerListenerBridge(onPhaseChanged)
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Pause (not release) when the Activity stops — simpler and faster to
    // resume than a full release/reprepare; the DisposableEffect above
    // still handles a genuine release when this screen leaves composition.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun startPlayback() {
        val mediaItem = stream.toMediaItemOrNull()
        if (mediaItem == null) {
            onPhaseChanged(
                PlaybackPhase.Error(
                    PlaybackErrorType.TORRENT_UNSUPPORTED,
                    "This source requires torrent streaming, which isn't supported yet. Try a different source."
                )
            )
        } else {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    LaunchedEffect(stream.id) { startPlayback() }

    var controlsVisible by remember { mutableStateOf(false) }
    var seekIndicatorText by remember { mutableStateOf<String?>(null) }
    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    // Drives focus in both directions, not just the auto-hide timer: when
    // controls appear, focus must move onto the (now visible) play/pause
    // button or it stays stuck on the invisible root anchor and the on-
    // screen buttons become unreachable by D-pad; when controls hide again
    // (and those buttons leave composition), focus must move back onto the
    // root anchor so LEFT/RIGHT/SELECT keep being captured at all. Also
    // covers requesting the very first focus on initial composition, since
    // controlsVisible starts false.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            runCatching { playPauseFocusRequester.requestFocus() }
            delay(4000)
            controlsVisible = false
        } else {
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(seekIndicatorText) {
        if (seekIndicatorText != null) {
            delay(800)
            seekIndicatorText = null
        }
    }

    fun seek(deltaMs: Long) {
        val duration = exoPlayer.duration
        val target = exoPlayer.currentPosition + deltaMs
        exoPlayer.seekTo(if (duration > 0) target.coerceIn(0, duration) else target.coerceAtLeast(0))
        seekIndicatorText = if (deltaMs < 0) "«« 10 seconds" else "10 seconds »»"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { seek(-10_000); true }
                    Key.DirectionRight -> { seek(10_000); true }
                    Key.DirectionCenter, Key.Enter -> {
                        if (!controlsVisible) { controlsVisible = true; true } else false
                    }
                    else -> false
                }
            }
    ) {
        PlayerSurface(exoPlayer = exoPlayer, modifier = Modifier.fillMaxSize())

        if (phase is PlaybackPhase.Loading || phase is PlaybackPhase.Buffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                color = MangoAmber
            )
        }

        if (phase is PlaybackPhase.Error) {
            PlaybackErrorOverlay(
                message = phase.message,
                onTryAgain = ::startPlayback,
                onChangeSource = onChangeSource,
                onBack = onBack
            )
        }

        seekIndicatorText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(text = text, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = content.title, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        val subtitle = episode?.let { "S${it.seasonNumber} E${it.episodeNumber} • ${it.title}" }
                        if (subtitle != null) {
                            Text(text = subtitle, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPlaying = phase is PlaybackPhase.Playing
                    HeroIconButton(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        focusRequester = playPauseFocusRequester
                    )
                    Spacer(Modifier.width(16.dp))
                    CrudeProgressLine(exoPlayer = exoPlayer, phase = phase, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    BackHandler {
        if (controlsVisible) controlsVisible = false else onBack()
    }
}

/**
 * Position/duration text + a plain (non-focusable) fill bar. Polls locally
 * inside this leaf rather than through the shared PlaybackPhase state, so a
 * tick only recomposes this small row instead of the whole player screen.
 * Replaced by the real D-pad-focusable PlayerTimeline in the next phase.
 */
@Composable
private fun CrudeProgressLine(
    exoPlayer: ExoPlayer,
    phase: PlaybackPhase,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(phase) {
        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0)
            durationMs = exoPlayer.duration.coerceAtLeast(0)
            delay(500)
        }
    }

    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatTimestamp(positionMs), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(percent = 50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(MangoAmber, RoundedCornerShape(percent = 50))
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text = formatTimestamp(durationMs), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
