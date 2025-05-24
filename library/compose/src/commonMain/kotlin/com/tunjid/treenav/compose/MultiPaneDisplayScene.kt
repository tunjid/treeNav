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

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.navigation3.decorators.rememberViewModelStoreNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.runtime.NavEntry
import com.tunjid.treenav.compose.navigation3.runtime.navEntryDecorator
import com.tunjid.treenav.compose.navigation3.runtime.rememberSavedStateNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.ui.LocalNavAnimatedContentScope
import com.tunjid.treenav.compose.navigation3.ui.NavDisplay
import com.tunjid.treenav.compose.navigation3.ui.Scene
import com.tunjid.treenav.compose.navigation3.ui.SceneStrategy
import com.tunjid.treenav.compose.navigation3.ui.rememberSceneSetupNavEntryDecorator

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <Pane, NavigationState : Node, Destination : Node> MultiPaneDisplay2(
    sharedTransitionScope: SharedTransitionScope,
    state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    pop: NavigationState.() -> NavigationState,
    goBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable MultiPaneDisplayScope<Pane, Destination>.() -> Unit,
) {
    val navigationState by state.navigationState
    val panesToDestinations = rememberUpdatedState(
        state.panesToDestinationsTransform(
            state.destinationTransform(navigationState)
        )
    )

    val backStack = remember { mutableStateListOf<Destination>() }.also { mutableBackStack ->
        state.backStackTransform(navigationState).let { currentBackStack ->
            mutableBackStack.clear()
            mutableBackStack.addAll(currentBackStack)
        }
    }

    val slots = remember {
        List(
            size = state.panes.size,
            init = ::Slot
        ).toSet()
    }

    val panedNavigationState = remember {
        mutableStateOf(
            value = SlotBasedPanedNavigationState.initial<Pane, Destination>(slots = slots)
                .adaptTo(
                    slots = slots,
                    panesToDestinations = panesToDestinations.value,
                    backStackIds = backStack.map(Node::id),
                )
        )
    }
        .also {
            it.updateOnChange(
                backStackIds = backStack.map(Node::id),
                panesToDestinations = panesToDestinations.value,
                slots = slots
            )
        }

    val sceneStrategy = remember {
        MultiPanePaneSceneStrategy(
            state = state,
            slots = slots,
            currentPanedNavigationState = panedNavigationState::value,
            pop = pop,
            content = content,
        )
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            goBack()
        },
        entryDecorators = listOf(
            navEntryDecorator { entry ->
                with(sharedTransitionScope) {
                    Box(
                        Modifier.sharedElement(
                            rememberSharedContentState(entry.key),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        ),
                    ) {
                        entry.content(entry.key)
                    }
                }
            },
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategy = sceneStrategy,
        entryProvider = { key ->
            NavEntry(
                key = key,
                content = { destination ->
                    val scope = LocalPaneScope.current
                    @Suppress("UNCHECKED_CAST")
                    state.renderTransform(scope as PaneScope<Pane, Destination>, destination)
                },
            )
        },
    )
}

private fun <Destination : Node, Pane> MutableState<SlotBasedPanedNavigationState<Pane, Destination>>.updateOnChange(
    backStackIds: List<String>,
    panesToDestinations: Map<Pane, Destination?>,
    slots: Set<Slot>
) {
    val backStackChanged = value.backStackIds != backStackIds
    val paneMappingChanged = value.panesToDestinations != panesToDestinations

    if (backStackChanged || paneMappingChanged) {
        value = value.adaptTo(
            slots = slots,
            panesToDestinations = panesToDestinations,
            backStackIds = backStackIds,
        )
    }
}


@Stable
private class MultiPanePaneSceneStrategy<Destination : Node, NavigationState : Node, Pane>(
    private val state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    private val slots: Set<Slot>,
    private val currentPanedNavigationState: () -> SlotBasedPanedNavigationState<Pane, Destination>,
    private val pop: NavigationState.() -> NavigationState,
    private val content: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : SceneStrategy<Destination> {

    @Composable
    override fun calculateScene(
        entries: List<NavEntry<Destination>>,
        onBack: (count: Int) -> Unit
    ): Scene<Destination> {

        val backstackIds = entries.map { it.key.id }

        // Calculate the scene for the entries specified.
        // Since there might be a predictive back gesture, pop until the right navigation state
        // is found
        val current = remember(backstackIds) {
            var navigationState = state.navigationState.value
            while (state.backStackTransform(navigationState).map { it.id } != backstackIds) {
                navigationState = navigationState.pop()
            }
            navigationState
        }

        val activeIds = remember(backstackIds) {
            state.destinationTransform(current)
                .let { destination ->
                    destination.children.mapTo(mutableSetOf(), Node::id) + destination.id
                }
        }

        val poppedBackstackIds = remember(backstackIds) {
            state.backStackTransform(current.pop())
                .mapTo(
                    destination = mutableSetOf(),
                    transform = Node::id
                )
        }

        return remember(backstackIds) {
            MultiPaneDisplayScene(
                destination = state.destinationTransform(current),
                slots = slots,
                panesToDestinations = state.panesToDestinationsTransform,
                currentPanedNavigationState = currentPanedNavigationState,
                entries = entries.filter { it.key.id in activeIds },
                previousEntries = entries.filter { it.key.id in poppedBackstackIds },
                scopeContent = content
            )
        }
    }
}

private class MultiPaneDisplayScene<Pane, Destination : Node>(
    private val destination: Destination,
    private val slots: Set<Slot>,
    private val panesToDestinations: @Composable (Destination) -> Map<Pane, Destination?>,
    private val currentPanedNavigationState: () -> SlotBasedPanedNavigationState<Pane, Destination>,
    override val entries: List<NavEntry<Destination>>,
    override val previousEntries: List<NavEntry<Destination>>,
    private val scopeContent: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : Scene<Destination> {

    override val key: Any = destination.id

    override val content: @Composable () -> Unit = {

        val panedNavigationState by remember {
            mutableStateOf(currentPanedNavigationState())
        }.also {
            it.updateOnChange(
                backStackIds = entries.map { destinationNavEntry -> destinationNavEntry.key.id },
                panesToDestinations = panesToDestinations(destination),
                slots = slots,
            )
        }

        val multiPaneDisplayScope: MultiPaneDisplayScope<Pane, Destination> = remember {
            object : MultiPaneDisplayScope<Pane, Destination> {

                @Composable
                override fun Destination(pane: Pane) {
                    val id = panedNavigationState.destinationFor(pane)?.id
                    val entry = entries.firstOrNull { it.key.id == id } ?: return

                    val paneState = panedNavigationState.slotFor(pane)
                        ?.let(panedNavigationState::paneStateFor) ?: return

                    val animatedContentScope = LocalNavAnimatedContentScope.current

                    val scope = remember {
                        AnimatedPaneScope(
                            paneState = paneState,
                            activeState = derivedStateOf {
                                animatedContentScope.transition.targetState == EnterExitState.Visible
                            },
                            animatedContentScope = animatedContentScope,
                        )
                    }.also { it.paneState = paneState }

                    CompositionLocalProvider(
                        LocalPaneScope provides scope
                    ) {
                        entry.content(entry.key)
                    }
                }

                override fun adaptationsIn(pane: Pane): Set<Adaptation> =
                    panedNavigationState.adaptationsIn(pane)

                override fun destinationIn(pane: Pane): Destination? =
                    panedNavigationState.destinationFor(pane)
            }
        }
        multiPaneDisplayScope.scopeContent()
    }
}

private val LocalPaneScope = staticCompositionLocalOf<PaneScope<*, *>> {
    throw IllegalArgumentException(
        "PaneScope should not be read until provided in the composition"
    )
}
