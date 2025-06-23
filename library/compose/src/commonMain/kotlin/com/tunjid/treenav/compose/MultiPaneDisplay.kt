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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.children
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.id
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.paneEnterTransition
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.paneExitTransition
import com.tunjid.treenav.compose.navigation3.decorators.rememberViewModelStoreNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.runtime.NavEntry
import com.tunjid.treenav.compose.navigation3.runtime.rememberSavedStateNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.ui.LocalNavAnimatedContentScope
import com.tunjid.treenav.compose.navigation3.ui.NavDisplay
import com.tunjid.treenav.compose.navigation3.ui.NavigationEventHandler
import com.tunjid.treenav.compose.navigation3.ui.Scene
import com.tunjid.treenav.compose.navigation3.ui.SceneStrategy
import com.tunjid.treenav.compose.navigation3.ui.rememberSceneSetupNavEntryDecorator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/**
 * Scope that provides context about individual panes [Pane] in an [MultiPaneDisplay].
 */
@Stable
interface MultiPaneDisplayScope<Pane, Destination : Node> {

    val inPredictiveBack: Boolean

    val panes: Collection<Pane>

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
            isPreviewingBack = state.backPreviewState::value,
            content = content,
        )
    }

    val transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        state.transitionSpec(
            sceneStrategy.scenes.getValue(sceneDestinationKey).multiPaneDisplayScope
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
        transitionSpec = transitionSpec,
        popTransitionSpec = transitionSpec,
        predictivePopTransitionSpec = transitionSpec,
        entryProvider = state.navEntryProvider,
    )

    NavigationEventHandler(
        enabled = { true },
        passThrough = true,
    ) { progress ->
        try {
            state.backPreviewState.value = true
            progress.collect()
            state.backPreviewState.value = false
        } catch (e: CancellationException) {
            state.backPreviewState.value = false
        }
    }
}

@Stable
private class MultiPanePaneSceneStrategy<Destination : Node, NavigationState : Node, Pane>(
    private val state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    private val slots: Set<Slot>,
    private val isPreviewingBack: () -> Boolean,
    private val currentPanedNavigationState: () -> SlotBasedPanedNavigationState<Pane, Destination>,
    private val content: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : SceneStrategy<Destination> {

    val scenes = mutableMapOf<String, MultiPaneDisplayScene<Pane, Destination>>()

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
            val currentNavigationState = state.findNavigationStateMatching(
                backstackIds = backstackIds,
            )

            val destination = state.destinationTransform(currentNavigationState)

            val activeIds = destination.children.mapTo(mutableSetOf(), Node::id) + destination.id

            val poppedNavigationState = state.popTransform(currentNavigationState)

            val poppedBackstack =
                if (currentNavigationState == poppedNavigationState) emptyList()
                else state.backStackTransform(poppedNavigationState)

            val mutableEntries = entries.toMutableList()

            MultiPaneDisplayScene(
                backstackIds = backstackIds,
                destination = destination,
                slots = slots,
                isPreviewingBack = isPreviewingBack,
                panesToDestinations = state.panesToDestinationsTransform,
                onSceneDisposed = { scenes.remove(destination.id) },
                currentPanedNavigationState = currentPanedNavigationState(),
                entries = entries.filter { it.id in activeIds },
                // Try to match up NavEntries to state using their id and children.
                // Best case is O(n) where the backstack isn't shuffled.
                previousEntries = poppedBackstack.map { poppedDestination ->
                    val index = mutableEntries.indexOfFirst {
                        it.id == poppedDestination.id && it.children == poppedDestination.children
                    }
                    mutableEntries.removeAt(index)
                },
                scopeContent = content
            ).also {
                scenes[backstackIds.last()] = it
            }
        }
    }
}

private class MultiPaneDisplayScene<Pane, Destination : Node>(
    override val entries: List<NavEntry<Destination>>,
    override val previousEntries: List<NavEntry<Destination>>,
    private val backstackIds: List<String>,
    private val destination: Destination,
    private val slots: Set<Slot>,
    private val isPreviewingBack: () -> Boolean,
    private val panesToDestinations: @Composable (Destination) -> Map<Pane, Destination?>,
    private val onSceneDisposed: () -> Unit,
    private val currentPanedNavigationState: SlotBasedPanedNavigationState<Pane, Destination>,
    private val scopeContent: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : Scene<Destination> {

    private var panedNavigationState by mutableStateOf(currentPanedNavigationState)

    @Stable
    val multiPaneDisplayScope = object : MultiPaneDisplayScope<Pane, Destination> {

        override val inPredictiveBack: Boolean
            get() = isPreviewingBack()

        override val panes: Collection<Pane>
            get() = panedNavigationState.panesToDestinations.keys

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
                    isPreviewingBack = isPreviewingBack,
                    animatedContentScope = animatedContentScope,
                )
            }.also { it.paneState = paneState }

            CompositionLocalProvider(
                LocalPaneScope provides scope
            ) {
                with(scope) {
                    val enterTransition = entry.paneEnterTransition(this)
                    val exitTransition = entry.paneExitTransition(this)
                    val shouldAnimate = enterTransition != EnterTransition.None
                            || exitTransition != ExitTransition.None
                    Box(
                        modifier =
                            if (shouldAnimate) Modifier.animateEnterExit(
                                enterTransition,
                                exitTransition
                            )
                            else Modifier,
                        content = {
                            entry.Content()
                        }
                    )
                }
            }
        }

        override fun adaptationsIn(pane: Pane): Set<Adaptation> =
            panedNavigationState.adaptationsIn(pane)

        override fun destinationIn(pane: Pane): Destination? =
            panedNavigationState.destinationFor(pane)
    }

    override val key: Any = destination.id

    override val content: @Composable () -> Unit = {

        currentPanedNavigationState.rememberUpdatedPanedNavigationState(
            backStackIds = backstackIds,
            panesToDestinations = panesToDestinations(destination),
            slots = slots,
        ).also { panedNavigationState = it.value }

        multiPaneDisplayScope.scopeContent()

        DisposableEffect(Unit) {
            onDispose(onSceneDisposed)
        }
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
private fun <Destination : Node, Pane> SlotBasedPanedNavigationState<Pane, Destination>.rememberUpdatedPanedNavigationState(
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

private val AnimatedContentTransitionScope<*>.sceneDestinationKey: String
    get() {
        val target = targetState as Pair<*, *>
        return target.second as String
    }

internal val LocalPaneScope = staticCompositionLocalOf<PaneScope<*, *>> {
    throw IllegalArgumentException(
        "PaneScope should not be read until provided in the composition"
    )
}

@Composable
internal fun <Pane, Destination : Node> localPaneScope(): PaneScope<Pane, Destination> {
    @Suppress("UNCHECKED_CAST")
    return LocalPaneScope.current as PaneScope<Pane, Destination>
}