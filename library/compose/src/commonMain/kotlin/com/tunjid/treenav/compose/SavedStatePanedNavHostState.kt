package com.tunjid.treenav.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.Order
import com.tunjid.treenav.compose.lifecycle.DestinationViewModelStoreCreator
import com.tunjid.treenav.compose.lifecycle.rememberDestinationLifecycleOwner
import com.tunjid.treenav.traverse


/**
 * A host for adaptive navigation for panes [Pane] and destinations [Destination].
 */
@Stable
interface PanedNavHostState<Pane, Destination : Node> {

    /**
     * Creates the scope that provides context about individual panes [Pane] in an [PanedNavHost].
     */
    @Composable
    fun scope(): PanedNavHostScope<Pane, Destination>
}

/**
 * Scope that provides context about individual panes [Pane] in an [PanedNavHost].
 */
@Stable
interface PanedNavHostScope<Pane, Destination : Node> {

    @Composable
    fun Destination(
        pane: Pane
    )

    fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation>

    fun nodeFor(
        pane: Pane,
    ): Destination?
}

/**
 * An implementation of an [PanedNavHostState] that provides a [SaveableStateHolder] for each
 * navigation destination that shows up in its panes.
 *
 * @param panes a list of panes that is possible to show in the [PanedNavHost] in all
 * possible configurations. The panes should consist of enum class instances, or a sealed class
 * hierarchy of kotlin objects.
 * @param configuration the [PanedNavHostConfiguration] that applies adaptive semantics and
 * strategies for each navigation destination shown in the [PanedNavHost].
 */
@Stable
class SavedStatePanedNavHostState<Pane, Destination : Node>(
    private val panes: List<Pane>,
    private val configuration: PanedNavHostConfiguration<Pane, *, Destination>,
) : PanedNavHostState<Pane, Destination> {

    @Composable
    override fun scope(): PanedNavHostScope<Pane, Destination> {
        val navigationState by configuration.navigationState
        val panesToNodes = configuration.paneMapping()
        val saveableStateHolder = rememberSaveableStateHolder()

        val panedContentScope = remember {
            SavedStatePanedNavHostScope(
                panes = panes,
                navHostConfiguration = configuration,
                initialPanesToNodes = panesToNodes,
                saveableStateHolder = saveableStateHolder,
            )
        }

        LaunchedEffect(navigationState, panesToNodes) {
            panedContentScope.onNewNavigationState(
                navigationState = navigationState,
                panesToNodes = panesToNodes
            )
        }

        return panedContentScope
    }

    companion object {
        @Stable
        private class SavedStatePanedNavHostScope<Pane, Destination : Node>(
            panes: List<Pane>,
            initialPanesToNodes: Map<Pane, Destination?>,
            saveableStateHolder: SaveableStateHolder,
            val navHostConfiguration: PanedNavHostConfiguration<Pane, *, Destination>,
        ) : PanedNavHostScope<Pane, Destination>, SaveableStateHolder by saveableStateHolder {

            private val destinationViewModelStoreCreator = DestinationViewModelStoreCreator(
                rootNodeProvider = navHostConfiguration.navigationState::value
            )

            val slots = List(
                size = panes.size,
                init = ::Slot
            ).toSet()

            var panedNavigationState by mutableStateOf(
                value = SlotBasedPanedNavigationState.initial<Pane, Destination>(slots = slots)
                    .adaptTo(
                        slots = slots,
                        panesToNodes = initialPanesToNodes,
                        backStackIds = navHostConfiguration.navigationState.value.backStackIds(),
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
                pane: Pane
            ): Set<Adaptation> = panedNavigationState.adaptationsIn(pane)

            override fun nodeFor(
                pane: Pane
            ): Destination? = panedNavigationState.destinationFor(pane)

            fun onNewNavigationState(
                navigationState: Node,
                panesToNodes: Map<Pane, Destination?>,
            ) {
                updateAdaptiveNavigationState {
                    adaptTo(
                        slots = slots.toSet(),
                        panesToNodes = panesToNodes,
                        backStackIds = navigationState.backStackIds()
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
                        val destinationLifecycleOwner = rememberDestinationLifecycleOwner(
                            destination
                        )
                        val destinationViewModelOwner = destinationViewModelStoreCreator
                            .viewModelStoreOwnerFor(destination)

                        CompositionLocalProvider(
                            LocalLifecycleOwner provides destinationLifecycleOwner,
                            LocalViewModelStoreOwner provides destinationViewModelOwner,
                        ) {
                            SaveableStateProvider(destination.id) {
                                navHostConfiguration.Destination(paneScope = scope)

                                DisposableEffect(Unit) {
                                    onDispose {
                                        val backstackIds = panedNavigationState.backStackIds
                                        if (!backstackIds.contains(destination.id)) removeState(
                                            destination.id
                                        )
                                    }
                                }

                                val hostLifecycleState by destinationLifecycleOwner.hostLifecycleState.currentStateAsState()
                                DisposableEffect(
                                    hostLifecycleState,
                                    scope.isActive,
                                    panedNavigationState,
                                ) {
                                    destinationLifecycleOwner.update(
                                        hostLifecycleState = hostLifecycleState,
                                        paneScope = scope,
                                        panedNavigationState = panedNavigationState
                                    )
                                    onDispose {
                                        destinationLifecycleOwner.update(
                                            hostLifecycleState = hostLifecycleState,
                                            paneScope = scope,
                                            panedNavigationState = panedNavigationState
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add destination ids that are animating out
                    LaunchedEffect(transition.isRunning) {
                        if (transition.targetState == EnterExitState.PostExit) {
                            val destinationId = targetPaneState.currentDestination?.id
                                ?: return@LaunchedEffect
                            updateAdaptiveNavigationState {
                                copy(destinationIdsAnimatingOut = destinationIdsAnimatingOut + destinationId)
                            }
                        }
                    }
                    // Remove route ids that have animated out
                    DisposableEffect(Unit) {
                        onDispose {
                            val routeId = targetPaneState.currentDestination?.id ?: return@onDispose
                            updateAdaptiveNavigationState {
                                copy(destinationIdsAnimatingOut = destinationIdsAnimatingOut - routeId).prune()
                            }
                            targetPaneState.currentDestination?.let(destinationViewModelStoreCreator::clearStoreFor)
                        }
                    }
                }
            }

            private inline fun updateAdaptiveNavigationState(
                block: SlotBasedPanedNavigationState<Pane, Destination>.() -> SlotBasedPanedNavigationState<Pane, Destination>
            ) {
                panedNavigationState = panedNavigationState.block()
            }
        }

        private fun Node.backStackIds() =
            mutableSetOf<String>().apply {
                traverse(Order.DepthFirst) { add(it.id) }
            }
    }
}
