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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.DecoratedNavEntryProvider
import androidx.navigation3.NavEntry
import androidx.navigation3.NavEntryDecorator
import androidx.navigation3.SaveableStateNavEntryDecorator
import androidx.navigation3.SavedStateNavEntryDecorator
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.tunjid.treenav.Node

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
    val panesToNodes = state.panesToDestinationsTransform(
        state.destinationTransform(navigationState)
    )

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
            CastPlatformViewModelStoreNavEntryDecorator,
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

@Stable
private val CastPlatformViewModelStoreNavEntryDecorator: NavEntryDecorator
    get() = PlatformViewModelStoreNavEntryDecorator as? NavEntryDecorator
        ?: DefaultViewModelStoreNavEntryDecorator

@Stable
internal expect val PlatformViewModelStoreNavEntryDecorator: Any?

/**
 * Provides the content of a [NavEntry] with a [ViewModelStoreOwner] and provides that
 * [ViewModelStoreOwner] as a [LocalViewModelStoreOwner] so that it is available within the content.
 *
 * This requires that usage of the [SavedStateNavEntryDecorator] to ensure that the [NavEntry]
 * scoped [ViewModel]s can properly provide access to [SavedStateHandle]s
 */
internal object DefaultViewModelStoreNavEntryDecorator : NavEntryDecorator {

    @Composable
    override fun DecorateBackStack(backStack: List<Any>, content: @Composable () -> Unit) {
        val entryViewModelStoreProvider = viewModel { EntryViewModel() }
        entryViewModelStoreProvider.ownerInBackStack.clear()
        entryViewModelStoreProvider.ownerInBackStack.addAll(backStack)
        val localInfo = remember { ViewModelStoreNavLocalInfo() }
        DisposableEffect(key1 = backStack) { onDispose { localInfo.refCount.clear() } }

//        val activity = LocalActivity.current
        backStack.forEachIndexed { index, key ->
            // We update here as part of composition to ensure the value is available to
            // DecorateEntry
            localInfo.refCount.getOrPut(key) { LinkedHashSet<Int>() }.add(getIdForKey(key, index))
            DisposableEffect(key1 = key) {
                localInfo.refCount
                    .getOrPut(key) { LinkedHashSet<Int>() }
                    .add(getIdForKey(key, index))
                onDispose {
                    // If the backStack count is less than the refCount for the key, remove the
                    // state since that means we removed a key from the backstack, and set the
                    // refCount to the backstack count.
                    val backstackCount = backStack.count { it == key }
                    val lastKeyCount = localInfo.refCount[key]?.size ?: 0
                    if (backstackCount < lastKeyCount) {
                        // The set of the ids associated with this key
                        @Suppress("PrimitiveInCollection") // The order of the element matters
                        val idsSet = localInfo.refCount[key]!!
                        val id = idsSet.last()
                        idsSet.remove(id)
                        if (!localInfo.idsInComposition.contains(id)) {
//                            if (activity?.isChangingConfigurations != true) {
                            entryViewModelStoreProvider
                                .removeViewModelStoreOwnerForKey(id)
                                ?.clear()
//                            }
                        }
                    }

                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key]?.isEmpty() == true) {
                        localInfo.refCount.remove(key)
                    }
                }
            }
        }

        CompositionLocalProvider(LocalViewModelStoreNavLocalInfo provides localInfo) {
            content.invoke()
        }
    }

    @Composable
    override fun <T : Any> DecorateEntry(entry: NavEntry<T>) {
        val key = entry.key
        val entryViewModelStoreProvider = viewModel { EntryViewModel() }

//        val activity = LocalActivity.current
        val localInfo = LocalViewModelStoreNavLocalInfo.current
        // Tracks whether the key is changed
        var keyChanged = false
        var id: Int =
            rememberSaveable(key) {
                keyChanged = true
                localInfo.refCount[key]!!.last()
            }
        id =
            rememberSaveable(localInfo.refCount[key]?.size) {
                // if the key changed, use the current id
                // If the key was not changed, and the current id is not in composition or on the
                // back stack then update the id with the last item from the backstack with the
                // associated key. This ensures that we can handle duplicates, both consecutive and
                // non-consecutive
                if (
                    !keyChanged &&
                    (!localInfo.idsInComposition.contains(id) ||
                            localInfo.refCount[key]?.contains(id) == true)
                ) {
                    localInfo.refCount[key]!!.last()
                } else {
                    id
                }
            }
        keyChanged = false

        val viewModelStore = entryViewModelStoreProvider.viewModelStoreForKey(id)

        DisposableEffect(key1 = key) {
            localInfo.idsInComposition.add(id)
            onDispose {
                if (localInfo.idsInComposition.remove(id) && !localInfo.refCount.contains(key)) {
//                    if (activity?.isChangingConfigurations != true) {
                    entryViewModelStoreProvider.removeViewModelStoreOwnerForKey(id)?.clear()
//                    }
                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key]?.isEmpty() == true) {
                        localInfo.refCount.remove(key)
                    }
                }
            }
        }

        val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
        val childViewModelOwner = remember {
            object :
                ViewModelStoreOwner,
                SavedStateRegistryOwner by savedStateRegistryOwner,
                HasDefaultViewModelProviderFactory {
                override val viewModelStore: ViewModelStore
                    get() = viewModelStore

                override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                    get() = SavedStateViewModelFactory()

                override val defaultViewModelCreationExtras: CreationExtras
                    get() =
                        MutableCreationExtras().also {
                            it[SAVED_STATE_REGISTRY_OWNER_KEY] = savedStateRegistryOwner
                            it[VIEW_MODEL_STORE_OWNER_KEY] = this
                        }

                init {
                    require(this.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
                        "The Lifecycle state is already beyond INITIALIZED. The " +
                                "ViewModelStoreNavEntryDecorator requires adding the " +
                                "SavedStateNavEntryDecorator to ensure support for " +
                                "SavedStateHandles."
                    }
                    enableSavedStateHandles()
                }
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides childViewModelOwner) {
            entry.content.invoke(key)
        }
    }
}

private class EntryViewModel : ViewModel() {
    private val owners = mutableMapOf<Any, ViewModelStore>()
    val ownerInBackStack = mutableListOf<Any>()

    fun viewModelStoreForKey(key: Any): ViewModelStore = owners.getOrPut(key) { ViewModelStore() }

    fun removeViewModelStoreOwnerForKey(key: Any): ViewModelStore? = owners.remove(key)

    override fun onCleared() {
        owners.forEach { (_, store) -> store.clear() }
    }
}

internal val LocalViewModelStoreNavLocalInfo =
    staticCompositionLocalOf<ViewModelStoreNavLocalInfo> {
        error(
            "CompositionLocal LocalViewModelStoreNavLocalInfo not present. You must call " +
                    "DecorateBackStack before calling DecorateEntry."
        )
    }

internal class ViewModelStoreNavLocalInfo {
    internal val refCount: MutableMap<Any, LinkedHashSet<Int>> = mutableMapOf()
    @Suppress("PrimitiveInCollection") // The order of the element matters
    internal val idsInComposition: LinkedHashSet<Int> = LinkedHashSet<Int>()
}

internal fun getIdForKey(key: Any, count: Int): Int = 31 * key.hashCode() + count