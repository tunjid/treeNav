package com.tunjid.treenav.compose.transforms

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.MultiPaneDisplayState

/**
 * Provides APIs for adjusting what is presented in a [MultiPaneDisplay].
 */
sealed interface Transform<Pane, in NavigationState : Node, Destination : Node>

/**
 * A [Transform] that allows for changing the current [Destination] in the [MultiPaneDisplay]
 * sees without actually modifying the backing [NavigationState].
 */
fun interface DestinationTransform<Pane, NavigationState : Node, Destination : Node>
    : Transform<Pane, NavigationState, Destination> {

    /**
     * Given a [NavigationState], provide the current [Destination] to show. The [Destination]
     * returned must already exist in the back stack of the [MultiPaneDisplayState.navigationState].
     *
     * @param navigationState the current navigation state.
     * @param previousTransform a [Transform] that when invoked, returns the [Destination] that
     * would have been shown pre-transform that can then be composed with new logic.
     */
    fun toDestination(
        navigationState: NavigationState,
        previousTransform: (NavigationState) -> Destination,
    ): Destination
}

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

internal class CompoundTransform<Pane, NavigationState : Node, Destination : Node>(
    destinationTransform: DestinationTransform<Pane, NavigationState, Destination>?,
    paneTransform: PaneTransform<Pane, Destination>?,
    renderTransform: RenderTransform<Pane, Destination>?,
) : Transform<Pane, NavigationState, Destination> {
    val transforms = listOfNotNull(
        destinationTransform,
        paneTransform,
        renderTransform,
    )
}

/**
 * Creates a transform that an aggregation of the transforms provided to it.
 *
 * @see DestinationTransform
 * @see PaneTransform
 * @see RenderTransform
 */
fun <Pane, NavigationState : Node, Destination : Node> compoundTransform(
    destinationTransform: DestinationTransform<Pane, NavigationState, Destination>? = null,
    paneTransform: PaneTransform<Pane, Destination>? = null,
    renderTransform: RenderTransform<Pane, Destination>? = null,
): Transform<Pane, NavigationState, Destination> = CompoundTransform(
    destinationTransform = destinationTransform,
    paneTransform = paneTransform,
    renderTransform = renderTransform,
)