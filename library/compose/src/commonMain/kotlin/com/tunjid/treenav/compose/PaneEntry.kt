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
class PaneEntry<Pane, Destination : Node>(
    /**
     * The [EnterTransition] used when this [PaneEntry] adapts to its current [Pane].
     */
    internal val enterTransition: PaneScope<Pane, Destination>.() -> EnterTransition,
    /**
     * The [ExitTransition] used when this [PaneEntry] adapts to its current [Pane].
     */
    internal val exitTransition: PaneScope<Pane, Destination>.() -> ExitTransition,
    /**
     * Provides the [Destination]s that are shown alongside the [Destination] provided and
     * what [Pane]s they should show up in.
     */
    internal val paneMapping: @Composable (Destination) -> Map<Pane, Destination?>,
    /**
     * Provides the content to show for the given [Destination].
     */
    internal val content: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
)