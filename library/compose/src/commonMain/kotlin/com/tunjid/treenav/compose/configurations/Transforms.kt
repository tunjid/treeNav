package com.tunjid.treenav.compose.configurations

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope


interface Transform<Pane, in NavigationState : Node, Destination : Node>

fun interface DestinationTransform<Pane, NavigationState : Node, Destination : Node>
    : Transform<Pane, NavigationState, Destination> {
    fun toDestination(
        navigationState: NavigationState,
        original: (NavigationState) -> Destination,
    ): Destination
}

fun interface PaneTransform<Pane, Destination : Node>
    : Transform<Pane, Node, Destination> {
    @Composable
    fun toPanesAndDestinations(
        destination: Destination,
        original: @Composable (Destination) -> Map<Pane, Destination?>,
    ): Map<Pane, Destination?>
}

fun interface RenderTransform<Pane, Destination : Node>
    : Transform<Pane, Node, Destination> {
    @Composable
    fun PaneScope<Pane, Destination>.Render(
        destination: Destination,
        original: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
    )
}

