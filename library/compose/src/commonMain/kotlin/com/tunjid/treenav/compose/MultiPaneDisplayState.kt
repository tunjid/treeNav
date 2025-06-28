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

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.navigation3.runtime.NavEntry
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
 * @param destinationPanes provides the strategy used to adapt the current
 * [Destination] to the panes available.
 * @param destinationContent the transform used to render a [Destination] in its pane.
 */
@Stable
class MultiPaneDisplayState<Pane, NavigationState : Node, Destination : Node> internal constructor(
    internal val panes: List<Pane>,
    internal val navigationState: State<NavigationState>,
    internal val backStackTransform: (NavigationState) -> List<Destination>,
    internal val destinationTransform: (NavigationState) -> Destination,
    internal val popTransform: (NavigationState) -> NavigationState,
    internal val onPopped: (NavigationState) -> Unit,
    internal val transitionSpec: MultiPaneDisplayScope<Pane, Destination>.() -> ContentTransform,
    internal val paneEntryProvider: (Destination) -> PaneEntry<Pane, Destination>,
    internal val destinationPanes: @Composable (Destination) -> Map<Pane, Destination?>,
    internal val destinationContent: @Composable PaneScope<Pane, Destination>.(PaneEntry<Pane, Destination>, Destination) -> Unit,
) {

    internal val navEntryProvider = { destination: Destination ->
        val paneEntry = paneEntryProvider(destination)
        NavEntry(
            key = destination,
            contentKey = destination.id,
            metadata = mapOf(
                ID_KEY to destination.id,
                DESTINATION_KEY to destination,
                CHILDREN_KEY to destination.children,
                PANE_ENTER_TRANSITION_KEY to paneEntry.enterTransition,
                PANE_EXIT_TRANSITION_KEY to paneEntry.exitTransition,
            ),
            content = { innerDestination ->
                destinationContent(localPaneScope(), paneEntry, innerDestination)
            },
        )
    }

    companion object {
        private const val ID_KEY = "com.tunjid.treenav.compose.id"
        private const val DESTINATION_KEY = "com.tunjid.treenav.compose.destination"
        private const val CHILDREN_KEY = "com.tunjid.treenav.compose.children"
        private const val PANE_ENTER_TRANSITION_KEY =
            "com.tunjid.treenav.compose.pane.enter.transition"
        private const val PANE_EXIT_TRANSITION_KEY =
            "com.tunjid.treenav.compose.pane.exit.transition"

        internal val NavEntry<*>.id get() = metadata[ID_KEY] as String
        internal val NavEntry<*>.children get() = metadata[CHILDREN_KEY]

        @Suppress("UNCHECKED_CAST")
        internal inline fun <T : Node> NavEntry<*>.destination() =
            metadata[DESTINATION_KEY] as T

        @Suppress("UNCHECKED_CAST")
        internal inline val NavEntry<*>.paneEnterTransition
            get() = metadata[PANE_ENTER_TRANSITION_KEY] as PaneScope<*, *>.() -> EnterTransition

        @Suppress("UNCHECKED_CAST")
        internal inline val NavEntry<*>.paneExitTransition
            get() = metadata[PANE_EXIT_TRANSITION_KEY] as PaneScope<*, *>.() -> ExitTransition
    }
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
    transforms: List<Transform<Pane, NavigationState, Destination>>,
    navigationState: State<NavigationState>,
    backStackTransform: (NavigationState) -> List<Destination>,
    destinationTransform: (NavigationState) -> Destination,
    popTransform: (NavigationState) -> NavigationState,
    onPopped: (NavigationState) -> Unit,
    transitionSpec: MultiPaneDisplayScope<Pane, Destination>.() -> ContentTransform = {
        NoContentTransform
    },
    entryProvider: (Destination) -> PaneEntry<Pane, Destination>,
) = transforms.fold(
    initial = MultiPaneDisplayState(
        panes = panes,
        navigationState = navigationState,
        backStackTransform = backStackTransform,
        destinationTransform = destinationTransform,
        popTransform = popTransform,
        onPopped = onPopped,
        transitionSpec = transitionSpec,
        paneEntryProvider = entryProvider,
        destinationPanes = { destination ->
            entryProvider(destination).paneMapping(destination)
        },
        destinationContent = transform@{ paneEntry, destination ->
            paneEntry.content(this@transform, destination)
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
        transitionSpec = transitionSpec,
        paneEntryProvider = paneEntryProvider,
        destinationPanes = when (transform) {
            is PaneTransform -> { destination ->
                transform.toPanesAndDestinations(
                    destination = destination,
                    previousTransform = destinationPanes,
                )
            }

            else -> destinationPanes
        },
        destinationContent = when (transform) {
            is RenderTransform -> { paneEntry, destination ->
                with(transform) {
                    Render(
                        destination = destination,
                        previousTransform = previous@{ innerDestination ->
                            destinationContent(
                                this@previous,
                                paneEntry,
                                innerDestination,
                            )
                        },
                    )
                }
            }

            else -> destinationContent
        },
    )

private val NoContentTransform = ContentTransform(
    targetContentEnter = EnterTransition.None,
    initialContentExit = ExitTransition.None,
    sizeTransform = null,
)