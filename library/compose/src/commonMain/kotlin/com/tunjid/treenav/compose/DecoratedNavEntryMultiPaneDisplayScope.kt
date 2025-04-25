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
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator
import androidx.navigation3.DecoratedNavEntryProvider
import androidx.navigation3.NavEntry
import androidx.navigation3.SaveableStateNavEntryDecorator
import androidx.navigation3.SavedStateNavEntryDecorator
import com.tunjid.treenav.Node

@Composable
internal fun <Destination : Node, NavigationState : Node, Pane> DecoratedNavEntryMultiPaneDisplayScope(
    state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    content: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) {
    val backStack = remember { mutableStateListOf<Destination>() }.also { mutableBackStack ->
        state.backStackTransform(state.navigationState.value).let { currentBackStack ->
            mutableBackStack.clear()
            mutableBackStack.addAll(currentBackStack)
        }
    }
    val panesToNodes = state.panesToDestinationsTransform(state.currentDestination.value)

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
            SaveableStateNavEntryDecorator,
            SavedStateNavEntryDecorator,
            ViewModelStoreNavEntryDecorator,
        ),
        content = { entries ->
            val updatedEntries by rememberUpdatedState(entries)
            val displayScope = remember {
                Navigation3MultiPaneDisplayScope(
                    panes = state.panes,
                    initialBackStack = backStack,
                    initialPanesToNodes = panesToNodes,
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
            DisposableEffect(backStack, panesToNodes) {
                displayScope.onBackStackChanged(
                    backStack = backStack,
                    panesToNodes = panesToNodes
                )
                onDispose { }
            }

            displayScope.content()
        },
    )
}

@Stable
private class Navigation3MultiPaneDisplayScope<Pane, Destination : Node>(
    panes: List<Pane>,
    initialBackStack: List<Destination>,
    initialPanesToNodes: Map<Pane, Destination?>,
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
                panesToNodes = initialPanesToNodes,
                backStackIds = initialBackStack.ids(),
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
        backStack: List<Destination>,
        panesToNodes: Map<Pane, Destination?>,
    ) {
        updateAdaptiveNavigationState {
            adaptTo(
                slots = slots.toSet(),
                panesToNodes = panesToNodes,
                backStackIds = backStack.ids()
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

    private fun List<Destination>.ids(): MutableSet<String> =
        fold(mutableSetOf()) { set, destination ->
            set.add(destination.id)
            set
        }
}

private val LocalPaneScope = staticCompositionLocalOf<PaneScope<*, *>> {
    TODO()
}
