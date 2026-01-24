package com.tunjid.treenav.compose.panedecorators

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneScope

/**
 * Provides APIs for adjusting what is presented in a [MultiPaneDisplay].
 */
sealed interface PaneDecorator<in NavigationState : Node, Destination : Node, Pane>

/**
 * A [PaneDecorator] that allows for changing which [Destination] shows in which [Pane].
 */
internal fun interface PaneMappingDecorator<Destination : Node, Pane> : PaneDecorator<Node, Destination, Pane> {

    /**
     * Given the current [Destination], provide what [Destination] to show in a [Pane].
     * Each [Destination] in the returned mapping must already exist in the
     * back stack of the [MultiPaneDisplayState.navigationState].
     *
     * @param destination the current [Destination] to display.
     * @param previousDecorator a [PaneDecorator] that when invoked, returns the pane to destination
     * mapping for the current [Destination] pre-transform that can then be composed with new logic.
     */
    @Composable
    fun toPanesAndDestinations(
        destination: Destination,
        previousDecorator: @Composable (Destination) -> Map<Pane, Destination?>,
    ): Map<Pane, Destination?>
}

/**
 * A [PaneDecorator] that allows for the rendering semantics of a [Destination] in a given
 * [PaneScope].
 */
internal fun interface PaneRenderDecorator<Destination : Node, Pane> : PaneDecorator<Node, Destination, Pane> {

    /**
     * Given the current [Destination], and its [PaneScope], compose additional presentation
     * logic.
     *
     * @param destination the current [Destination] to display in the provided [PaneScope].
     * @param previousDecorator a [PaneDecorator] that when invoked, renders the [Destination]
     * for the [PaneScope ]pre-decoration that can then be composed with new logic.
     */
    @Composable
    fun PaneScope<Pane, Destination>.Render(
        destination: Destination,
        previousDecorator: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
    )
}

/**
 * Given the current [Destination], provide what [Destination]s to show in each of the [Pane]s
 * available.
 *
 * Each [Destination] in the returned mapping must already exist in the
 * back stack derived from the [MultiPaneDisplayState.navigationState].
 *
 * @param mappingTransform a lambda providing the mapping. It has two arguments:
 * - destination: The [Destination] for which it panes will be displayed.
 * - destinationPaneMapper: A lambda that when invoked, returns the pane to destination
 * mapping for the current [Destination] pre-transform that can then be composed with new logic.
 */
fun <NavigationState : Node, Destination : Node, Pane> paneMappingDecorator(
    mappingTransform: @Composable (
        destination: Destination,
        destinationPaneDecorator: @Composable (Destination) -> Map<Pane, Destination?>,
    ) -> Map<Pane, Destination?>,
): PaneDecorator<NavigationState, Destination, Pane> =
    PaneMappingDecorator { destination, previousTransform ->
        mappingTransform(destination, previousTransform)
    }

/**
 * A [PaneDecorator] that allows for adjusting the rendering semantics of a [Destination] in a
 * for a given [Pane] in the [PaneScope].
 *
 * @param renderTransform a lambda providing the Composable to render. It has two arguments:
 * - destination: The [Destination] being rendered in the provided [PaneScope].
 * - destinationContent: A lambda that when invoked, renders the [Destination] pre-transform
 */
fun <Pane, Destination : Node, NavigationState : Node> paneRenderDecorator(
    renderTransform: @Composable PaneScope<Pane, Destination>.(
        destination: Destination,
        destinationContent: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
    ) -> Unit,
): PaneDecorator<NavigationState, Destination, Pane> =
    PaneRenderDecorator { destination, previousTransform ->
        renderTransform(destination, previousTransform)
    }
