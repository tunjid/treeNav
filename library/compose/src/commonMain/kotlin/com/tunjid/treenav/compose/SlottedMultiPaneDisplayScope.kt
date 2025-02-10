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
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.lifecycle.DestinationViewModelStoreCreator
import com.tunjid.treenav.compose.lifecycle.rememberDestinationLifecycleOwner

@Stable
internal class SlottedMultiPaneDisplayScope<Pane, Destination : Node>(
    panes: List<Pane>,
    initialBackStack: List<Destination>,
    initialPanesToNodes: Map<Pane, Destination?>,
    saveableStateHolder: SaveableStateHolder,
    val displayState: MultiPaneDisplayState<Pane, *, Destination>,
) : MultiPaneDisplayScope<Pane, Destination>, SaveableStateHolder by saveableStateHolder {

    private val slots = List(
        size = panes.size,
        init = ::Slot
    ).toSet()

    private var panedNavigationState by mutableStateOf(
        value = SlotBasedPanedNavigationState.initial<Pane, Destination>(slots = slots)
            .adaptTo(
                slots = slots,
                panesToNodes = initialPanesToNodes,
                backStackIds = initialBackStack.ids(),
            )
    )

    private val destinationViewModelStoreCreator = DestinationViewModelStoreCreator(
        validNodeIdsReader = { panedNavigationState.backStackIds + panedNavigationState.destinationIdsAnimatingOut }
    )

    private val slotsToRoutes =
        mutableStateMapOf<Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            slots.forEach { slot ->
                map[slot] = movableContentOf { Render(slot) }
            }
        }

    /**
     * Retrieves the a [ViewModelStoreOwner] for a given [destination]. All destinations
     * with the same [Node.id] share the same [ViewModelStoreOwner].
     *
     * The [destination] must be present in the navigation tree, otherwise an
     * [IllegalStateException] will be thrown.
     *
     * @param destination The destination for which the [ViewModelStoreOwner] should
     * be retrieved.
     */
    fun viewModelStoreOwnerFor(destination: Destination): ViewModelStoreOwner =
        destinationViewModelStoreCreator.viewModelStoreOwnerFor(destination)

    @Composable
    override fun Destination(pane: Pane) {
        val slot = panedNavigationState.slotFor(pane)
        slotsToRoutes[slot]?.invoke()
    }

    override fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation> = panedNavigationState.adaptationsIn(pane)

    override fun nodeFor(
        pane: Pane,
    ): Destination? = panedNavigationState.destinationFor(pane)

    internal fun onBackStackChanged(
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
                val destinationLifecycleOwner = rememberDestinationLifecycleOwner(
                    destination
                )
                val destinationViewModelOwner = remember(destination.id) {
                    destinationViewModelStoreCreator
                        .viewModelStoreOwnerFor(destination)
                }

                CompositionLocalProvider(
                    LocalLifecycleOwner provides destinationLifecycleOwner,
                    LocalViewModelStoreOwner provides destinationViewModelOwner,
                ) {
                    SaveableStateProvider(destination.id) {
                        displayState.Destination(paneScope = scope)

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

//fun <Pane, Destination : Node> PanedNavHostScope<
//        Pane,
//        Destination
//        >.requireSavedStatePanedNavHostScope(): SavedStatePanedNavHostState.Companion.NavHostScope<Pane, Destination> {
//    check(this is SavedStatePanedNavHostState.Companion.NavHostScope) {
//        "This PanedNavHostScope instance is not a SavedStatePanedNavHostScope"
//    }
//    return this
//}
