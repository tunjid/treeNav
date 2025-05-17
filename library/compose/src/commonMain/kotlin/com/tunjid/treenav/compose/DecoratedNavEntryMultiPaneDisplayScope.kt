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


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.navigation3.DecoratedNavEntryProvider
import com.tunjid.treenav.compose.navigation3.NavEntry
import com.tunjid.treenav.compose.navigation3.decorators.rememberMovableContentNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.decorators.rememberSavedStateNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.decorators.rememberViewModelStoreNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.decorators.transitionAwareLifecycleNavEntryDecorator

@Composable
internal fun <Destination : Node, NavigationState : Node, Pane> DecoratedNavEntryMultiPaneDisplayScope(
    state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    content: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) {
    val navigationState by state.navigationState
    val backStack = remember { mutableStateListOf<Destination>() }.also { mutableBackStack ->
        state.backStackTransform(navigationState).let { currentBackStack ->
            mutableBackStack.clear()
            mutableBackStack.addAll(currentBackStack)
        }
    }
    val panesToDestinations = state.panesToDestinationsTransform(
        state.destinationTransform(navigationState)
    )

    val transitionAwareLifecycleNavEntryDecorator =
        transitionAwareLifecycleNavEntryDecorator(backStack, true)

    DecoratedNavEntryProvider(
        backStack = backStack,
        entryProvider = { node ->
            NavEntry(
                key = node,
                content = { destination ->
                    val scope = LocalPaneScope.current
                    @Suppress("UNCHECKED_CAST")
                    state.renderTransform(scope as PaneScope<Pane, Destination>, destination)
                }
            )
        },
        entryDecorators = listOf(
            rememberMovableContentNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            transitionAwareLifecycleNavEntryDecorator,
            rememberViewModelStoreNavEntryDecorator(),
        ),
        content = { entries ->
            val updatedEntries by rememberUpdatedState(entries)
            val displayScope = remember {
                DecoratedNavEntryMultiPaneDisplayScope(
                    panes = state.panes,
                    initialBackStack = backStack,
                    initialPanesToDestinations = panesToDestinations,
                    paneRenderer = {
                        val currentEntry = remember(paneState.currentDestination?.id) {
                            updatedEntries.findLast {
                                it.key.id == paneState.currentDestination?.id
                            }
                        }
                        checkNotNull(currentEntry) {
                            "There is no entry for the current navigation destination with id ${paneState.currentDestination?.id}"
                        }.content(currentEntry.key)
                    },
                )
            }
            DisposableEffect(navigationState, panesToDestinations) {
                displayScope.onBackStackChanged(
                    backStackIds = backStack.map { it.id },
                    panesToDestinations = panesToDestinations
                )
                onDispose { }
            }

            displayScope.content()
        },
    )
}

@Stable
private class DecoratedNavEntryMultiPaneDisplayScope<Pane, Destination : Node>(
    panes: List<Pane>,
    initialBackStack: List<Destination>,
    initialPanesToDestinations: Map<Pane, Destination?>,
    private val paneRenderer: @Composable (PaneScope<Pane, Destination>.() -> Unit),
) : MultiPaneDisplayScope<Pane, Destination> {

    private val slots = List(
        size = panes.size,
        init = ::Slot
    ).toSet()

    var panedNavigationState by mutableStateOf(
        value = SlotBasedPanedNavigationState.initial<Pane, Destination>(slots = slots)
            .adaptTo(
                slots = slots,
                panesToDestinations = initialPanesToDestinations,
                backStackIds = initialBackStack.map { it.id },
            )
    )

    private val slotsToRoutes =
        mutableStateMapOf<Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            slots.forEach { slot ->
                map[slot] = movableContentOf { Render(slot) }
            }
        }

    @Composable
    override fun Destination(pane: Pane) {
        val slot = panedNavigationState.slotFor(pane)
        slotsToRoutes[slot]?.invoke()
    }

    override fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation> = panedNavigationState.adaptationsIn(pane)

    override fun destinationIn(
        pane: Pane,
    ): Destination? = panedNavigationState.destinationFor(pane)

    fun onBackStackChanged(
        backStackIds: List<String>,
        panesToDestinations: Map<Pane, Destination?>,
    ) {
        updateAdaptiveNavigationState {
            adaptTo(
                slots = slots.toSet(),
                panesToDestinations = panesToDestinations,
                backStackIds = backStackIds,
            )
        }
    }

    /**
     * Renders [slot] into its pane with scopes that allow for animations
     * and shared elements.
     */
    @Composable
    private fun Render(
        slot: Slot,
    ) {
        val paneTransition = updateTransition(
            targetState = panedNavigationState.paneStateFor(slot),
            label = "$slot-PaneTransition",
        )
        paneTransition.AnimatedContent(
            contentKey = { it.currentDestination?.id },
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                    sizeTransform = null,
                )
            }
        ) { targetPaneState ->
            val scope = remember {
                AnimatedPaneScope(
                    paneState = targetPaneState,
                    activeState = derivedStateOf {
                        val activePaneState = panedNavigationState.paneStateFor(slot)
                        activePaneState.currentDestination?.id == targetPaneState.currentDestination?.id
                    },
                    animatedContentScope = this@AnimatedContent,
                )
            }

            // While technically a backwards write, it stabilizes and ensures the values are
            // correct at first composition
            scope.paneState = targetPaneState

            val destination = targetPaneState.currentDestination
            if (destination != null) {
                CompositionLocalProvider(
                    LocalPaneScope provides scope
                ) {
                    scope.paneRenderer()
                }
            }
        }
    }

    private inline fun updateAdaptiveNavigationState(
        block: SlotBasedPanedNavigationState<Pane, Destination>.() -> SlotBasedPanedNavigationState<Pane, Destination>,
    ) {
        panedNavigationState = panedNavigationState.block()
    }
}

private val LocalPaneScope = staticCompositionLocalOf<PaneScope<*, *>> {
    throw IllegalArgumentException(
        "PaneScope should not be read until provided in the composition"
    )
}


