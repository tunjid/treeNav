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
import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.Keys.id
import com.tunjid.treenav.compose.navigation3.decorators.rememberViewModelStoreNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.runtime.NavEntry
import com.tunjid.treenav.compose.navigation3.runtime.rememberSavedStateNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.ui.LocalNavAnimatedContentScope
import com.tunjid.treenav.compose.navigation3.ui.NavDisplay
import com.tunjid.treenav.compose.navigation3.ui.Scene
import com.tunjid.treenav.compose.navigation3.ui.SceneStrategy
import com.tunjid.treenav.compose.navigation3.ui.rememberSceneSetupNavEntryDecorator

/**
 * Scope that provides context about individual panes [Pane] in an [MultiPaneDisplay].
 */
@Stable
interface MultiPaneDisplayScope<Pane, Destination : Node> {

    @Composable
    fun Destination(
        pane: Pane,
    )

    fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation>

    fun destinationIn(
        pane: Pane,
    ): Destination?
}

/**
 * A Display that provides the following for each
 * navigation [Destination] that shows up in its panes:
 *
 * - A single [SaveableStateHolder] for each navigation [Destination] that shows up in its panes.
 * [SaveableStateHolder.SaveableStateProvider] is keyed on the [Destination]s [Node.id].
 *
 * - A [ViewModelStoreOwner] for each [Destination] via [LocalViewModelStoreOwner].
 * Once present in the navigation tree, a [Destination] will always use the same
 * [ViewModelStoreOwner], regardless of where in the tree it is, until its is removed from the tree.
 * [Destination]s are unique based on their [Node.id].
 *
 * - A [LifecycleOwner] for each [Destination] via [LocalLifecycleOwner]. This [LifecycleOwner]
 * follows the [Lifecycle] of its immediate parent, unless it is animating out or placed in the
 * backstack. This is defined by [PaneScope.isActive], which is a function of the backing
 * [AnimatedContent] for each [Pane] displayed and if the current [Destination]
 * matches [MultiPaneDisplayScope.destinationIn] in the visible [Pane].
 *
 * @param state the driving [MultiPaneDisplayState] that applies adaptive semantics and
 * transforms for each navigation destination shown in the [MultiPaneDisplay].
 */
@Composable
fun <Pane, NavigationState : Node, Destination : Node> MultiPaneDisplay(
    state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
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
            val sameBackStack = currentBackStack == mutableBackStack
            if (sameBackStack) return@let

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

    val initialPanedNavigationState = remember {
        SlotBasedPanedNavigationState.initial<Pane, Destination>(slots = slots)
            .adaptTo(
                slots = slots,
                panesToDestinations = panesToDestinations.value,
                backStackIds = backStack.map(Node::id),
            )
    }

    val panedNavigationState = initialPanedNavigationState.rememberUpdatedPanedNavigationState(
        backStackIds = backStack.map(Node::id),
        panesToDestinations = panesToDestinations.value,
        slots = slots
    )

    val sceneStrategy = remember {
        MultiPanePaneSceneStrategy(
            state = state,
            slots = slots,
            currentPanedNavigationState = panedNavigationState::value,
            content = content,
        )
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { count ->
            val poppedBackStackIds = state.backStackTransform(navigationState)
                .map(Node::id)
                .dropLast(count)

            val poppedNavigationState = state.findNavigationStateMatching(
                backstackIds = poppedBackStackIds,
            )
            state.onPopped(poppedNavigationState)
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategy = sceneStrategy,
        entryProvider = { key ->
            NavEntry(
                key = key,
                metadata = mapOf(
                    Keys.ID_KEY to key.id
                ),
                content = { destination ->
                    val scope = LocalPaneScope.current
                    @Suppress("UNCHECKED_CAST")
                    state.renderTransform(scope as PaneScope<Pane, Destination>, destination)
                },
            )
        },
    )
}


@Stable
private class MultiPanePaneSceneStrategy<Destination : Node, NavigationState : Node, Pane>(
    private val state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    private val slots: Set<Slot>,
    private val currentPanedNavigationState: () -> SlotBasedPanedNavigationState<Pane, Destination>,
    private val content: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : SceneStrategy<Destination> {

    @Composable
    override fun calculateScene(
        entries: List<NavEntry<Destination>>,
        onBack: (count: Int) -> Unit
    ): Scene<Destination> {

        val backstackIds = entries.map { it.id }

        return remember(backstackIds) {

            // Calculate the scene for the entries specified.
            // Since there might be a predictive back gesture, pop until the right navigation state
            // is found
            val current = state.findNavigationStateMatching(
                backstackIds = backstackIds,
            )

            val activeIds = state.destinationTransform(current)
                .let { destination ->
                    destination.children.mapTo(mutableSetOf(), Node::id) + destination.id
                }

            val poppedBackstackIds = state.backStackTransform(state.popTransform(current))
                .mapTo(
                    destination = mutableSetOf(),
                    transform = Node::id
                )

            MultiPaneDisplayScene(
                backstackIds = backstackIds,
                destination = state.destinationTransform(current),
                slots = slots,
                panesToDestinations = state.panesToDestinationsTransform,
                currentPanedNavigationState = currentPanedNavigationState(),
                entries = entries.filter { it.id in activeIds },
                previousEntries = entries.filter { it.id in poppedBackstackIds },
                scopeContent = content
            )
        }
    }
}

private class MultiPaneDisplayScene<Pane, Destination : Node>(
    override val entries: List<NavEntry<Destination>>,
    override val previousEntries: List<NavEntry<Destination>>,
    private val backstackIds: List<String>,
    private val destination: Destination,
    private val slots: Set<Slot>,
    private val panesToDestinations: @Composable (Destination) -> Map<Pane, Destination?>,
    private val currentPanedNavigationState: SlotBasedPanedNavigationState<Pane, Destination>,
    private val scopeContent: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : Scene<Destination> {

    override val key: Any = destination.id

    override val content: @Composable () -> Unit = {

        val panedNavigationState by currentPanedNavigationState.rememberUpdatedPanedNavigationState(
            backStackIds = backstackIds,
            panesToDestinations = panesToDestinations(destination),
            slots = slots,
        )

        val multiPaneDisplayScope: MultiPaneDisplayScope<Pane, Destination> = remember {
            object : MultiPaneDisplayScope<Pane, Destination> {

                @Composable
                override fun Destination(pane: Pane) {
                    val id = panedNavigationState.destinationFor(pane)?.id
                    val entry = entries.firstOrNull { it.id == id } ?: return

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
                        entry.Content()
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

private fun <NavigationState : Node> MultiPaneDisplayState<*, NavigationState, *>.findNavigationStateMatching(
    backstackIds: List<String>,
): NavigationState {
    var state = navigationState.value
    while (backStackTransform(state).map(Node::id) != backstackIds) {
        state = popTransform(state)
    }
    return state
}

@Composable
internal fun <Destination : Node, Pane> SlotBasedPanedNavigationState<Pane, Destination>.rememberUpdatedPanedNavigationState(
    backStackIds: List<String>,
    panesToDestinations: Map<Pane, Destination?>,
    slots: Set<Slot>
): State<SlotBasedPanedNavigationState<Pane, Destination>> =
    remember {
        mutableStateOf(this)
    }.also {
        val backStackChanged = it.value.backStackIds != backStackIds
        val paneMappingChanged = it.value.panesToDestinations != panesToDestinations

        if (backStackChanged || paneMappingChanged) {
            it.value = it.value.adaptTo(
                slots = slots,
                panesToDestinations = panesToDestinations,
                backStackIds = backStackIds,
            )
        }
    }

private val LocalPaneScope = staticCompositionLocalOf<PaneScope<*, *>> {
    throw IllegalArgumentException(
        "PaneScope should not be read until provided in the composition"
    )
}

internal object Keys {
    val ID_KEY = "com.tunjid.treenav.compose.id"

    val NavEntry<*>.id get() = metadata[ID_KEY] as String
}