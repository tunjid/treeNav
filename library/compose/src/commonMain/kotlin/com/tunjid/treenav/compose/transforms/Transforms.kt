package com.tunjid.treenav.compose.transforms

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneScope

/**
 * Provides APIs for adjusting what is presented in a [MultiPaneDisplay].
 */
sealed interface PaneTransform<Pane, in NavigationState : Node, Destination : Node>

/**
 * A [PaneTransform] that allows for changing which [Destination] shows in which [Pane].
 */
internal fun interface PaneMappingTransform<Pane, Destination : Node>
    : PaneTransform<Pane, Node, Destination> {

    /**
     * Given the current [Destination], provide what [Destination] to show in a [Pane].
     * Each [Destination] in the returned mapping must already exist in the
     * back stack of the [MultiPaneDisplayState.navigationState].
     *
     * @param destination the current [Destination] to display.
     * @param previousTransform a [PaneTransform] that when invoked, returns the pane to destination
     * mapping for the current [Destination] pre-transform that can then be composed with new logic.
     */
    @Composable
    fun toPanesAndDestinations(
        destination: Destination,
        previousTransform: @Composable (Destination) -> Map<Pane, Destination?>,
    ): Map<Pane, Destination?>
}

/**
 * A [PaneTransform] that allows for the rendering semantics of a [Destination] in a given
 * [PaneScope].
 */
internal fun interface PaneRenderTransform<Pane, Destination : Node>
    : PaneTransform<Pane, Node, Destination> {

    /**
     * Given the current [Destination], and its [PaneScope], compose additional presentation
     * logic.
     *
     * @param destination the current [Destination] to display in the provided [PaneScope].
     * @param previousTransform a [PaneTransform] that when invoked, renders the [Destination]
     * for the [PaneScope ]pre-transform that can then be composed with new logic.
     */
    @Composable
    fun PaneScope<Pane, Destination>.Render(
        destination: Destination,
        previousTransform: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
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
fun <Pane, NavigationState : Node, Destination : Node> paneMappingTransform(
    mappingTransform: @Composable (
        destination: Destination,
        destinationPaneMapper: @Composable (Destination) -> Map<Pane, Destination?>
    ) -> Map<Pane, Destination?>
): PaneTransform<Pane, NavigationState, Destination> =
    PaneMappingTransform { destination, previousTransform ->
        mappingTransform(destination, previousTransform)
    }

/**
 * A [PaneTransform] that allows for adjusting the rendering semantics of a [Destination] in a
 * for a given [Pane] in the [PaneScope].
 *
 * @param renderTransform a lambda providing the Composable to render. It has two arguments:
 * - destination: The [Destination] being rendered in the provided [PaneScope].
 * - destinationContent: A lambda that when invoked, renders the [Destination] pre-transform
 */
fun <Pane, Destination : Node, NavigationState : Node> paneRenderTransform(
    renderTransform: @Composable PaneScope<Pane, Destination>.(
        destination: Destination,
        destinationContent: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit
    ) -> Unit
): PaneTransform<Pane, NavigationState, Destination> =
    PaneRenderTransform { destination, previousTransform ->
        renderTransform(destination, previousTransform)
    }
