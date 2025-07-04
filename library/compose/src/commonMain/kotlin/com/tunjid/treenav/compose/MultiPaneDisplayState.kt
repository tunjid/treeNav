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
import com.tunjid.treenav.compose.panedecorators.PaneMappingDecorator
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import com.tunjid.treenav.compose.panedecorators.PaneRenderDecorator

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
class MultiPaneDisplayState<NavigationState : Node, Destination : Node, Pane> internal constructor(
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
                PANE_CONTENT_TRANSFORM_KEY to paneEntry.contentTransform,
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
        private const val PANE_CONTENT_TRANSFORM_KEY =
            "com.tunjid.treenav.compose.pane.enter.transition"

        internal val NavEntry<*>.id get() = metadata[ID_KEY] as String
        internal val NavEntry<*>.children get() = metadata[CHILDREN_KEY]

        @Suppress("UNCHECKED_CAST")
        internal inline fun <T : Node> NavEntry<*>.destination() =
            metadata[DESTINATION_KEY] as T

        @Suppress("UNCHECKED_CAST")
        internal inline val NavEntry<*>.paneContentTransform
            get() = metadata[PANE_CONTENT_TRANSFORM_KEY] as PaneScope<*, *>.() -> ContentTransform
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
 * @param entryProvider provides the [PaneDecorator]s and content needed to render
 * a [Destination] in its pane.
 * @param paneDecorators a list of decorators applied to every [Destination] before it is
 * rendered in its pane. Order matters; they are applied from last to first.
 */
fun <NavigationState : Node, Destination : Node, Pane> MultiPaneDisplayState(
    panes: List<Pane>,
    paneDecorators: List<PaneDecorator<NavigationState, Destination, Pane>>,
    navigationState: State<NavigationState>,
    backStackTransform: (NavigationState) -> List<Destination>,
    destinationTransform: (NavigationState) -> Destination,
    popTransform: (NavigationState) -> NavigationState,
    onPopped: (NavigationState) -> Unit,
    transitionSpec: MultiPaneDisplayScope<Pane, Destination>.() -> ContentTransform = {
        NoContentTransform
    },
    entryProvider: (Destination) -> PaneEntry<Pane, Destination>,
) = paneDecorators.fold(
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
    operation = MultiPaneDisplayState<NavigationState, Destination, Pane>::plus
)

private operator fun <NavigationState : Node, Destination : Node, Pane>
        MultiPaneDisplayState<NavigationState, Destination, Pane>.plus(
    transform: PaneDecorator<NavigationState, Destination, Pane>,
): MultiPaneDisplayState<NavigationState, Destination, Pane> =
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
            is PaneMappingDecorator -> { destination ->
                transform.toPanesAndDestinations(
                    destination = destination,
                    previousDecorator = destinationPanes,
                )
            }

            else -> destinationPanes
        },
        destinationContent = when (transform) {
            is PaneRenderDecorator -> { paneEntry, destination ->
                with(transform) {
                    Render(
                        destination = destination,
                        previousDecorator = previous@{ innerDestination ->
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