package com.tunjid.treenav.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.treenav.Node

/**
 * Provides the logic used to select, configure and place a navigation [Destination] for each
 * pane [Pane] for the current active navigation [Destination].
 */
@Stable
class PaneStrategy<Pane, Destination : Node> internal constructor(
    val transitions: PaneScope<Pane, Destination>.() -> PaneScope.Transitions,
    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    val paneMapper: @Composable (Destination) -> Map<Pane, Destination?>,
    val render: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit
)

/**
 * Allows for defining the logic used to select, configure and place a navigation
 * [Destination] for each pane [Pane] for the current active navigation [Destination].
 *
 * @param transitions the transitions to run within each [PaneScope].
 * @param paneMapping provides the mapping of panes to destinations for a given destination [Destination].
 * @param render defines the Composable rendered for each destination
 * in a given [PaneScope].
 */
fun <Pane, Destination : Node> paneStrategy(
    transitions: PaneScope<Pane, Destination>.() -> PaneScope.Transitions = { NoTransition },
    paneMapping: @Composable (Destination) -> Map<Pane, Destination?> = { emptyMap() },
    render: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit
) = PaneStrategy(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val NoTransition = PaneScope.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)