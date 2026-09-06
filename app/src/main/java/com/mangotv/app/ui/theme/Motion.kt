package com.mangotv.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * Central motion tokens so every focus/transition animation across the app
 * feels like part of the same system rather than ad-hoc per-screen tuning.
 */
object MangoMotion {
    const val FocusScale = 1.08f
    const val FocusScaleHero = 1.05f

    const val FastMillis = 150
    const val MediumMillis = 280
    const val SlowMillis = 550
    const val HeroCrossfadeMillis = 900
    const val HeroKenBurnsMillis = 9000

    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    val focusTween = tween<Float>(durationMillis = FastMillis, easing = StandardEasing)
    val mediumTween = tween<Float>(durationMillis = MediumMillis, easing = StandardEasing)

    // Compose's default BringIntoViewSpec positioning ("scroll the minimum
    // amount needed to reveal the item") is kept as-is here — only the
    // animation itself speeds up, replacing Compose's slower default
    // spring with focusTween so a D-pad held down doesn't outrun it and
    // stutter (see HomeScreen.kt's own comment for the full story). Used
    // for horizontal card-to-card scrolling within a row, which shouldn't
    // re-center on every move.
    @OptIn(ExperimentalFoundationApi::class)
    val FastBringIntoViewSpec: BringIntoViewSpec = object : BringIntoViewSpec {
        override val scrollAnimationSpec: AnimationSpec<Float> = focusTween
    }

    // Re-centers the focused item in the middle of the viewport instead of
    // just scrolling the minimum amount to reveal it — used for Home's
    // row-to-row vertical scrolling. The clamp (same formula Android's own
    // Compose-for-TV guide documents) keeps the last few rows from
    // over-scrolling into empty space below the end of the list.
    //
    // Deliberately snap(), not a tween: centering means almost every
    // row-to-row move now triggers a scroll (unlike the old "only scroll
    // if not already visible" default, most moves were no-ops under
    // that), so a scroll fires on nearly every D-pad repeat instead of
    // occasionally. Even the fast 150ms tween tried first collided with
    // the next repeat often enough to reintroduce the exact stutter that
    // fix was for — a new request still cancels/restarts whatever's
    // mid-flight. snap() removes the failure mode outright: there's
    // nothing running long enough to ever get interrupted.
    @OptIn(ExperimentalFoundationApi::class)
    val SnapCenteredBringIntoViewSpec: BringIntoViewSpec = object : BringIntoViewSpec {
        override val scrollAnimationSpec: AnimationSpec<Float> = snap()

        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
            val centeredTarget = 0.5f * containerSize - 0.5f * size
            val target = if (size <= containerSize && (containerSize - centeredTarget) < size) {
                containerSize - size
            } else {
                centeredTarget
            }
            return offset - target
        }
    }
}
