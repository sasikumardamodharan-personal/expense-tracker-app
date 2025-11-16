package com.expensetracker.app.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Standard animation durations for consistent UI
 */
object AnimationDurations {
    const val FAST = 150
    const val NORMAL = 300
    const val SLOW = 500
}

/**
 * Standard easing curves
 */
object AnimationEasing {
    val FastOutSlowIn = FastOutSlowInEasing
    val EaseInOut = EaseInOutCubic
    val EaseOutCurve = androidx.compose.animation.core.EaseOut
}

/**
 * Fade in animation spec
 */
fun <T> fadeInSpec(): FiniteAnimationSpec<T> = tween(
    durationMillis = AnimationDurations.NORMAL,
    easing = AnimationEasing.EaseInOut
)

/**
 * Fade out animation spec
 */
fun <T> fadeOutSpec(): FiniteAnimationSpec<T> = tween(
    durationMillis = AnimationDurations.FAST,
    easing = AnimationEasing.EaseInOut
)

/**
 * Slide in from bottom animation
 */
fun slideInFromBottom(): EnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(
        durationMillis = AnimationDurations.NORMAL,
        easing = AnimationEasing.FastOutSlowIn
    )
) + fadeIn(animationSpec = fadeInSpec())

/**
 * Slide out to bottom animation
 */
fun slideOutToBottom(): ExitTransition = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(
        durationMillis = AnimationDurations.FAST,
        easing = AnimationEasing.EaseOutCurve
    )
) + fadeOut(animationSpec = fadeOutSpec())

/**
 * Slide in from right animation
 */
fun slideInFromRight(): EnterTransition = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(
        durationMillis = AnimationDurations.NORMAL,
        easing = AnimationEasing.FastOutSlowIn
    )
) + fadeIn(animationSpec = fadeInSpec())

/**
 * Slide out to left animation
 */
fun slideOutToLeft(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = tween(
        durationMillis = AnimationDurations.FAST,
        easing = AnimationEasing.EaseOutCurve
    )
) + fadeOut(animationSpec = fadeOutSpec())

/**
 * Scale in animation
 */
fun scaleIn(): EnterTransition = scaleIn(
    initialScale = 0.8f,
    animationSpec = tween(
        durationMillis = AnimationDurations.NORMAL,
        easing = AnimationEasing.FastOutSlowIn
    )
) + fadeIn(animationSpec = fadeInSpec())

/**
 * Scale out animation
 */
fun scaleOut(): ExitTransition = scaleOut(
    targetScale = 0.8f,
    animationSpec = tween(
        durationMillis = AnimationDurations.FAST,
        easing = AnimationEasing.EaseOutCurve
    )
) + fadeOut(animationSpec = fadeOutSpec())

/**
 * Modifier for animated visibility with scale
 */
@Composable
fun Modifier.animateScale(visible: Boolean): Modifier {
    val scale = animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Modifier for animated visibility with fade
 */
@Composable
fun Modifier.animateFade(visible: Boolean): Modifier {
    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationDurations.NORMAL,
            easing = AnimationEasing.EaseInOut
        ),
        label = "alpha"
    )
    
    return this.graphicsLayer {
        this.alpha = alpha.value
    }
}
