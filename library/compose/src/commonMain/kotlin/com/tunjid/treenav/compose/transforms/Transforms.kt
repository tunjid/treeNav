package com.tunjid.treenav.compose.transforms

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope


interface Transform<Pane, in NavigationState : Node, Destination : Node>

fun interface DestinationTransform<Pane, NavigationState : Node, Destination : Node>
    : Transform<Pane, NavigationState, Destination> {
    fun toDestination(
        navigationState: NavigationState,
        previousTransform: (NavigationState) -> Destination,
    ): Destination
}

fun interface PaneTransform<Pane, Destination : Node>
    : Transform<Pane, Node, Destination> {
    @Composable
    fun toPanesAndDestinations(
        destination: Destination,
        previousTransform: @Composable (Destination) -> Map<Pane, Destination?>,
    ): Map<Pane, Destination?>
}

fun interface RenderTransform<Pane, Destination : Node>
    : Transform<Pane, Node, Destination> {
    @Composable
    fun PaneScope<Pane, Destination>.Render(
        destination: Destination,
        previousTransform: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
    )
}

