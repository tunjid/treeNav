package com.tunjid.treenav.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node

/**
 * Route implementation with adaptive semantics
 */
class AdaptiveRouteConfiguration<T, R : Node> internal constructor(
    private val paneMapper: (R) -> Map<T, R> = { emptyMap() },
    private val transitions: (AdaptivePaneState<T, R>) -> Adaptive.Transitions = { NoTransition },
    private val render: @Composable (R) -> Unit
) {

    @Composable
    fun Render(route: R) {
        render(route)
    }

    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    fun paneMapping(
        node: R
    ): Map<T, R> = paneMapper(node)

    fun transitionsFor(
        state: AdaptivePaneState<T, R>
    ): Adaptive.Transitions = transitions(state)

}

fun <T, R : Node> adaptiveRouteConfiguration(
    paneMapping: (R) -> Map<T, R> = { emptyMap() },
    transitions: (AdaptivePaneState<T, R>) -> Adaptive.Transitions = { NoTransition },
    render: @Composable (R) -> Unit
) = AdaptiveRouteConfiguration(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val NoTransition = Adaptive.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)