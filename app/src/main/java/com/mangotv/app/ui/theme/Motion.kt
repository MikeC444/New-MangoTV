package com.mangotv.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

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
}
