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

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.children
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.destination
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.id
import com.tunjid.treenav.compose.MultiPaneDisplayState.Companion.paneContentTransform
import com.tunjid.treenav.compose.navigation3.decorators.rememberViewModelStoreNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.runtime.NavEntry
import com.tunjid.treenav.compose.navigation3.runtime.rememberSavedStateNavEntryDecorator
import com.tunjid.treenav.compose.navigation3.ui.LocalNavAnimatedContentScope
import com.tunjid.treenav.compose.navigation3.ui.NavDisplay
import com.tunjid.treenav.compose.navigation3.ui.NavigationEventHandler
import com.tunjid.treenav.compose.navigation3.ui.Scene
import com.tunjid.treenav.compose.navigation3.ui.SceneStrategy
import com.tunjid.treenav.compose.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import kotlinx.coroutines.CancellationException

/**
 * Scope that provides context about individual panes [Pane] in an [MultiPaneDisplay].
 */
@Stable
interface MultiPaneDisplayScope<Pane, Destination : Node> {

    /**
     * All possible panes in the [MultiPaneDisplayScope].
     */
    val panes: Collection<Pane>

    /**
     * Renders the given [Destination] in the provided [Pane].
     */
    @Composable
    fun Destination(
        pane: Pane,
    )

    /**
     * Provides the set of adaptations in the provided [Pane].
     */
    fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation>

    /**
     * Returns the [Destination] in the provided [Pane].
     */
    fun destinationIn(
        pane: Pane,
    ): Destination?
}

/**
 * A Display that adapts the [MultiPaneDisplayState.navigationState] to
 * the [MultiPaneDisplayState.panes] available depending on the [PaneDecorator]s the
 * [MultiPaneDisplayState] has been configured with.
 *
 *
 * @param state the driving [MultiPaneDisplayState] that applies adaptive semantics and
 * decorators for each navigation destination shown in the [MultiPaneDisplay].
 * @param modifier optional [Modifier] for the display.
 * @param content the content that should be displayed the receiving [MultiPaneDisplayScope].
 */
@Composable
fun <NavigationState : Node, Destination : Node, Pane> MultiPaneDisplay(
    state: MultiPaneDisplayState<NavigationState, Destination, Pane>,
    modifier: Modifier = Modifier,
    content: @Composable MultiPaneDisplayScope<Pane, Destination>.() -> Unit,
) {
    val navigationState by state.navigationState

    val backStatusState = remember {
        mutableStateOf<BackStatus>(BackStatus.Completed.Commited)
    }

    val panesToDestinations = rememberUpdatedState(
        state.destinationPanes(
            state.destinationTransform(navigationState)
        )
    )

    val backStack = remember { mutableStateListOf<Destination>() }.also { mutableBackStack ->
        state.backStackTransform(navigationState).let { currentBackStack ->
            val sameBackStack = currentBackStack == mutableBackStack
            if (sameBackStack) return@let

            Snapshot.withMutableSnapshot {
                mutableBackStack.clear()
                mutableBackStack.addAll(currentBackStack)
                backStatusState.value = BackStatus.Completed.Commited
            }
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

    // The latest PanedNavigationState of the display
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
            backStatus = backStatusState::value,
            content = content,
        )
    }

    val transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = remember {
        val displayScope = NonRenderingMultiPaneDisplayScope(
            panedNavigationState = panedNavigationState,
        )
        return@remember {
            state.transitionSpec(displayScope)
        }
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
        enabled = state::canPop,
        passThrough = true,
    ) { progress ->
        try {
            progress.collect {
                backStatusState.value = BackStatus.Seeking
            }
            backStatusState.value = BackStatus.Completed.Commited
        } catch (e: CancellationException) {
            backStatusState.value = BackStatus.Completed.Cancelled
        }
    }
}

@Stable
private class MultiPanePaneSceneStrategy<NavigationState : Node, Destination : Node, Pane>(
    private val state: MultiPaneDisplayState<NavigationState, Destination, Pane>,
    private val slots: Set<Slot>,
    private val backStatus: () -> BackStatus,
    private val currentPanedNavigationState: () -> SlotBasedPanedNavigationState<Pane, Destination>,
    private val content: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : SceneStrategy<Destination> {

    @Composable
    override fun calculateScene(
        entries: List<NavEntry<Destination>>,
        onBack: (count: Int) -> Unit
    ): Scene<Destination> {

        val backstackIds = remember(entries.identityHash()) {
            entries.map { it.id }
        }

        return remember(backstackIds) {

            // Calculate the scene for the entries specified.
            // Since there might be a predictive back gesture, pop until the right navigation state
            // is found
            val currentNavigationState = state.findNavigationStateMatching(
                backstackIds = backstackIds,
            )

            val panedNavigationState = currentPanedNavigationState()

            val destination = state.destinationTransform(currentNavigationState)

            val activeIds = destination.children.mapTo(mutableSetOf(), Node::id) + destination.id

            val poppedNavigationState = state.popTransform(currentNavigationState)

            val poppedBackstack =
                if (currentNavigationState == poppedNavigationState) emptyList()
                else state.backStackTransform(poppedNavigationState)

            val mutableEntries = entries.toMutableList()

            val sceneKey = MultiPaneSceneKey(
                ids = backstackIds,
                isPreviewingBack = backstackIds != panedNavigationState.backStackIds
            )

            MultiPaneDisplayScene(
                destination = destination,
                sceneKey = sceneKey,
                slots = slots,
                backStatus = backStatus,
                panesToDestinations = state.destinationPanes,
                currentPanedNavigationState = panedNavigationState,
                eligibleSceneEntries = entries.filter { it.id in activeIds },
                // Try to match up NavEntries to state using their id and children.
                // Best case is O(n) where the backstack isn't shuffled.
                previousEntries = poppedBackstack.map { poppedDestination ->
                    val index = mutableEntries.indexOfFirst {
                        it.id == poppedDestination.id && it.children == poppedDestination.children
                    }
                    mutableEntries.removeAt(index)
                },
                scopeContent = content
            )
        }
    }
}

@Stable
private class MultiPaneDisplayScene<Pane, Destination : Node>(
    override val previousEntries: List<NavEntry<Destination>>,
    private val eligibleSceneEntries: List<NavEntry<Destination>>,
    private val sceneKey: MultiPaneSceneKey,
    private val destination: Destination,
    private val slots: Set<Slot>,
    private val currentPanedNavigationState: SlotBasedPanedNavigationState<Pane, Destination>,
    backStatus: () -> BackStatus,
    private val panesToDestinations: @Composable (Destination) -> Map<Pane, Destination?>,
    private val scopeContent: @Composable (MultiPaneDisplayScope<Pane, Destination>.() -> Unit),
) : Scene<Destination> {

    private val panedNavigationState = mutableStateOf(currentPanedNavigationState)

    @Stable
    val multiPaneDisplayScope = PaneDestinationMultiPaneDisplayScope(
        panedNavigationState = panedNavigationState,
        currentEntries = ::entries,
        backStatus = backStatus,
    )

    override val key: Any = sceneKey

    override val entries: List<NavEntry<Destination>>
        get() = when {
            // Filtering of duplicates is already handled in NavDisplay
            sceneKey.isPreviewingBack -> eligibleSceneEntries
            // Since the display may adapt, the actual entries to show are a subset of all eligible
            // entries that can show.
            // This is so destinations animating out are shown by the SceneSetupNavEntryDecorator.
            // Otherwise, they will be removed immediately and not animate.
            else -> panedNavigationState.value.let { state ->
                eligibleSceneEntries.filter { navEntry ->
                    state.paneFor(navEntry.destination()) != null
                }
            }
        }

    override val content: @Composable () -> Unit = {
        currentPanedNavigationState.rememberUpdatedPanedNavigationState(
            backStackIds = sceneKey.ids,
            panesToDestinations = panesToDestinations(destination),
            slots = slots,
        ).also { panedNavigationState.value = it.value }

        multiPaneDisplayScope.scopeContent()
    }

    @Stable
    class PaneDestinationMultiPaneDisplayScope<Pane, Destination : Node>(
        panedNavigationState: State<SlotBasedPanedNavigationState<Pane, Destination>>,
        private val currentEntries: () -> List<NavEntry<Destination>>,
        private val backStatus: () -> BackStatus,
    ) : MultiPaneDisplayScope<Pane, Destination> {

        private val panedNavigationState by panedNavigationState

        override val panes: Collection<Pane>
            get() = panedNavigationState.panesToDestinations.keys

        @Composable
        override fun Destination(pane: Pane) {
            val id = panedNavigationState.destinationFor(pane)?.id
            val entry = currentEntries().firstOrNull { it.id == id } ?: return

            val paneState = remember(panedNavigationState.identityHash()) {
                panedNavigationState.slotFor(pane)?.let(panedNavigationState::paneStateFor)
            } ?: return

            val animatedContentScope = LocalNavAnimatedContentScope.current

            val scope = remember {
                AnimatedPaneScope(
                    backStatus = backStatus,
                    metadata = entry.metadata,
                    paneState = paneState,
                    animatedContentScope = animatedContentScope,
                )
            }.also {
                it.paneState = paneState
            }

            CompositionLocalProvider(
                LocalPaneScope provides scope
            ) {
                with(scope) {
                    val paneModifier = remember(
                        isActive,
                        inPredictiveBack,
                        panedNavigationState.identityHash(),
                        animatedContentScope.transition.targetState,
                    ) {
                        val contentTransform = entry.paneContentTransform(this)
                        val shouldAnimate =
                            contentTransform.targetContentEnter != EnterTransition.None
                                    || contentTransform.initialContentExit != ExitTransition.None

                        if (shouldAnimate) Modifier.animateEnterExit(
                            enter = contentTransform.targetContentEnter,
                            exit = contentTransform.initialContentExit,
                        )
                        else Modifier
                    }

                    Box(
                        modifier = paneModifier,
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
}

@Stable
private class NonRenderingMultiPaneDisplayScope<Pane, Destination : Node>(
    panedNavigationState: State<SlotBasedPanedNavigationState<Pane, Destination>>,
) : MultiPaneDisplayScope<Pane, Destination> {

    private val panedNavigationState by panedNavigationState

    override val panes: Collection<Pane>
        get() = panedNavigationState.panesToDestinations.keys

    @Composable
    override fun Destination(pane: Pane) = throw IllegalStateException(
        "This MultiPaneDisplayScope cannot render panes"
    )

    override fun adaptationsIn(pane: Pane): Set<Adaptation> =
        panedNavigationState.adaptationsIn(pane)

    override fun destinationIn(pane: Pane): Destination? =
        panedNavigationState.destinationFor(pane)
}

private fun <NavigationState : Node> MultiPaneDisplayState<NavigationState, *, *>.findNavigationStateMatching(
    backstackIds: List<String>,
): NavigationState {
    var state = navigationState.value
    while (backStackTransform(state).map(Node::id) != backstackIds) {
        state = popTransform(state)
    }
    return state
}

/**
 * Keep track of changes to the paned navigation state.
 */
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

/**
 * Scene key with a flag if it was created for a predictive back gesture.
 * NOTE: its equals and hashcode are completely independent of this predictive back flag.
 * This is to let [NavDisplay] find the appropriate scene to go back to with this key. The
 * flag is only used internally.
 */
internal class MultiPaneSceneKey(
    val ids: List<String>,
    val isPreviewingBack: Boolean,
) {

    private val idsHash = ids.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MultiPaneSceneKey

        return ids == other.ids
    }

    override fun hashCode(): Int {
        return idsHash
    }

    override fun toString(): String {
        return "MultiPaneSceneKey(ids = $ids, isPreviewingBack = $isPreviewingBack)"
    }
}

internal sealed class BackStatus {
    data object Seeking : BackStatus()
    sealed class Completed : BackStatus() {
        data object Commited : Completed()
        data object Cancelled : Completed()
    }
}

private val LocalPaneScope = staticCompositionLocalOf<PaneScope<*, *>> {
    throw IllegalArgumentException(
        "PaneScope should not be read until provided in the composition"
    )
}

@Composable
internal fun <Pane, Destination : Node> localPaneScope(): PaneScope<Pane, Destination> {
    @Suppress("UNCHECKED_CAST")
    return LocalPaneScope.current as PaneScope<Pane, Destination>
}