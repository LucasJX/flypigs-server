package com.flypigs.ntfyapp.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ──────────────────────────────────────────────
// M3 Expressive Animation Specs
// 页面切换: 300ms FastOutSlowIn
// 列表项入场: fadeIn + slideInVertically
// ──────────────────────────────────────────────

private const val PAGE_TRANSITION_DURATION = 300

// ── 页面切换动画 ──────────────────────────────

fun pageEnterTransition() = fadeIn(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    )
) + slideInVertically(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    ),
    initialOffsetY = { it / 20 }  // 轻微上移，5%
)

fun pageExitTransition() = fadeOut(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    )
)

fun pagePopEnterTransition() = fadeIn(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    )
) + slideInVertically(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    ),
    initialOffsetY = { -it / 20 }
)

fun pagePopExitTransition() = fadeOut(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    )
) + slideOutVertically(
    animationSpec = tween(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = FastOutSlowInEasing
    ),
    targetOffsetY = { it / 20 }
)

// ── 列表项入场动画 ────────────────────────────

fun listItemEnterTransition(index: Int): EnterTransition {
    val delay = (index * 30).coerceAtMost(300)  // 每项递增 30ms，最多 300ms
    return fadeIn(
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = delay,
            easing = FastOutSlowInEasing
        )
    ) + slideInVertically(
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = delay,
            easing = FastOutSlowInEasing
        ),
        initialOffsetY = { it / 8 }  // 12.5% 上移
    )
}

// ── AnimatedVisibility 扩展 (可选) ────────────

@Composable
fun AnimatedListVisibility(
    visible: Boolean,
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = listItemEnterTransition(index),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        content()
    }
}
