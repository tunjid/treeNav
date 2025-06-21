package com.tunjid.treenav.compose.transforms

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneScope

/**
 * Provides APIs for adjusting what is presented in a [MultiPaneDisplay].
 */
sealed interface Transform<Pane, in NavigationState : Node, Destination : Node>

/**
 * A [Transform] that allows for changing which [Destination] shows in which [Pane].
 */
fun interface PaneTransform<Pane, Destination : Node>
    : Transform<Pane, Node, Destination> {

    /**
     * Given the current [Destination], provide what [Destination] to show in a [Pane].
     * Each [Destination] in the returned mapping must already exist in the
     * back stack of the [MultiPaneDisplayState.navigationState].
     *
     * @param destination the current [Destination] to display.
     * @param previousTransform a [Transform] that when invoked, returns the pane to destination
     * mapping for the current [Destination] pre-transform that can then be composed with new logic.
     */
    @Composable
    fun toPanesAndDestinations(
        destination: Destination,
        previousTransform: @Composable (Destination) -> Map<Pane, Destination?>,
    ): Map<Pane, Destination?>
}

/**
 * A [Transform] that allows for the rendering semantics of a [Destination] in a given
 * [PaneScope].
 */
fun interface RenderTransform<Pane, Destination : Node>
    : Transform<Pane, Node, Destination> {

    /**
     * Given the current [Destination], and its [PaneScope], compose additional presentation
     * logic.
     *
     * @param destination the current [Destination] to display in the provided [PaneScope].
     * @param previousTransform a [Transform] that when invoked, renders the [Destination]
     * for the [PaneScope ]pre-transform that can then be composed with new logic.
     */
    @Composable
    fun PaneScope<Pane, Destination>.Render(
        destination: Destination,
        previousTransform: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
    )
}
