/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.treenav.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.transforms.PaneTransform
import com.tunjid.treenav.compose.transforms.RenderTransform
import com.tunjid.treenav.compose.transforms.Transform

/**
 * Class for configuring a [MultiPaneDisplay] for selecting, adapting and placing navigation
 * destinations into different panes from an arbitrary [navigationState].
 *
 * @param panes a list of panes that is possible to show in the [MultiPaneDisplay] in all
 * possible configurations. The panes should consist of enum class instances, or a sealed class
 * hierarchy of kotlin objects.
 * @param navigationState the navigation state to be adapted into various panes.
 * @param backStackTransform a transform to read the back stack of the navigation state.
 * @param destinationTransform a transform of the [navigationState] to its current destination.
 * @param popTransform a transform of the [navigationState] when back is pressed.
 * @param onPopped an action to perform when the navigation state has been popped to a new state.
 * @param panesToDestinationsTransform provides the strategy used to adapt the current
 * [Destination] to the panes available.
 * @param renderTransform the transform used to render a [Destination] in its pane.
 */
class MultiPaneDisplayState<Pane, NavigationState : Node, Destination : Node> internal constructor(
    internal val panes: List<Pane>,
    internal val navigationState: State<NavigationState>,
    internal val backStackTransform: (NavigationState) -> List<Destination>,
    internal val destinationTransform: (NavigationState) -> Destination,
    internal val popTransform: (NavigationState) -> NavigationState,
    internal val onPopped: (NavigationState) -> Unit,
    internal val panesToDestinationsTransform: @Composable (Destination) -> Map<Pane, Destination?>,
    internal val renderTransform: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
) {
    internal val backPreviewState = mutableStateOf(false)
}

/**
 * Provides an [MultiPaneDisplayState] for configuring a [MultiPaneDisplay] for
 * showing different navigation destinations into different panes from an arbitrary
 * [navigationState].
 *
 * @param panes a list of panes that is possible to show in the [MultiPaneDisplay] in all
 * possible configurations. The panes should consist of enum class instances, or a sealed class
 * hierarchy of kotlin objects.
 * @param navigationState the navigation state to be adapted into various panes.
 * @param backStackTransform a transform to read the back stack of the navigation state.
 * @param destinationTransform a transform of the [navigationState] to its current destination.
 * @param popTransform a transform of the [navigationState] when back is pressed.
 * @param onPopped an action to perform when the navigation state has been popped to a new state.
 * @param entryProvider provides the [Transform]s and content needed to render
 * a [Destination] in its pane.
 * @param transforms a list of transforms applied to every [Destination] before it is
 * rendered in its pane. Order matters; they are applied from last to first.
 */
fun <Pane, NavigationState : Node, Destination : Node> MultiPaneDisplayState(
    panes: List<Pane>,
    navigationState: State<NavigationState>,
    backStackTransform: (NavigationState) -> List<Destination>,
    destinationTransform: (NavigationState) -> Destination,
    popTransform: (NavigationState) -> NavigationState,
    onPopped: (NavigationState) -> Unit,
    entryProvider: (Destination) -> PaneEntry<Pane, Destination>,
    transforms: List<Transform<Pane, NavigationState, Destination>>,
) = transforms.fold(
    initial = MultiPaneDisplayState(
        panes = panes,
        navigationState = navigationState,
        backStackTransform = backStackTransform,
        destinationTransform = destinationTransform,
        popTransform = popTransform,
        onPopped = onPopped,
        panesToDestinationsTransform = { destination ->
            entryProvider(destination).paneTransform(destination)
        },
        renderTransform = { destination ->
            val nav = entryProvider(destination)
            nav.content(this, destination)
        }
    ),
    operation = MultiPaneDisplayState<Pane, NavigationState, Destination>::plus
)

private operator fun <Pane, NavigationState : Node, Destination : Node>
        MultiPaneDisplayState<Pane, NavigationState, Destination>.plus(
    transform: Transform<Pane, NavigationState, Destination>,
): MultiPaneDisplayState<Pane, NavigationState, Destination> =
    MultiPaneDisplayState(
        panes = panes,
        navigationState = navigationState,
        backStackTransform = backStackTransform,
        popTransform = popTransform,
        onPopped = onPopped,
        destinationTransform = destinationTransform,
        panesToDestinationsTransform = when (transform) {
            is PaneTransform -> { destination ->
                transform.toPanesAndDestinations(
                    destination = destination,
                    previousTransform = panesToDestinationsTransform,
                )
            }

            else -> panesToDestinationsTransform
        },
        renderTransform = when (transform) {
            is RenderTransform -> { destination ->
                with(transform) {
                    Render(
                        destination = destination,
                        previousTransform = renderTransform,
                    )
                }
            }

            else -> renderTransform
        },
    )
