package com.mangotv.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mangotv.app.ui.theme.FocusBorder
import com.mangotv.app.ui.theme.MangoMotion

/**
 * The single building block behind every focusable tile in Mango TV (cards,
 * buttons, nav items). It owns the focus -> scale/glow/border animation so
 * every part of the app reacts to the D-pad the same way — at several
 * metres' viewing distance the focused element must always be obvious.
 *
 * Note: [Modifier.clickable] already makes its target focusable and reacts
 * to DPAD_CENTER/Enter when focused, so no separate `.focusable()` call is
 * needed here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    focusedScale: Float = MangoMotion.FocusScale,
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null,
    focusRequester: FocusRequester? = null,
    bringIntoViewOnFocus: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = MangoMotion.focusTween,
        label = "focusScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = MangoMotion.focusTween,
        label = "focusBorder"
    )

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
        if (isFocused && bringIntoViewOnFocus) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    var boxModifier = modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    if (isFocused) {
        boxModifier = boxModifier.shadow(elevation = 18.dp, shape = shape, clip = false)
    }
    if (focusRequester != null) {
        boxModifier = boxModifier.focusRequester(focusRequester)
    }
    boxModifier = boxModifier.clip(shape)
    boxModifier = if (backgroundBrush != null) {
        boxModifier.background(backgroundBrush)
    } else {
        boxModifier.background(backgroundColor)
    }
    boxModifier = boxModifier
        .border(BorderStroke(2.dp, FocusBorder.copy(alpha = borderAlpha)), shape)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )

    Box(modifier = boxModifier, content = content)
}
