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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node

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

    fun nodeFor(
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
 * matches [MultiPaneDisplayScope.nodeFor] in the visible [Pane].
 *
 * @param state the driving [MultiPaneDisplayState] that applies adaptive semantics and
 * strategies for each navigation destination shown in the [MultiPaneDisplay].
 */
@Composable
fun <Pane, NavigationState : Node, Destination : Node> MultiPaneDisplay(
    state: MultiPaneDisplayState<Pane, NavigationState, Destination>,
    modifier: Modifier = Modifier,
    content: @Composable MultiPaneDisplayScope<Pane, Destination>.() -> Unit,
) {
    val backStack by remember {
        derivedStateOf {
            state.backStackTransform(state.navigationState.value)
        }
    }

    Box(
        modifier = modifier
    ) {
        val panesToNodes = state.panesToDestinations()
        val saveableStateHolder = rememberPanedSaveableStateHolder()
        val displayScope = remember {
            SlottedMultiPaneDisplayScope(
                panes = state.panes,
                initialBackStack = backStack,
                initialPanesToNodes = panesToNodes,
                saveableStateHolder = saveableStateHolder,
                paneRenderer = {
                    val currentDestination = remember(paneState.currentDestination) {
                        paneState.currentDestination
                    }
                    currentDestination?.let { destination ->
                        state.renderTransform(this, destination)
                    }
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
    }
}

/**
 * The current pane mapping to use in the [MultiPaneDisplay].
 */
@Composable
private fun <Pane, Destination : Node>
        MultiPaneDisplayState<Pane, *, Destination>.panesToDestinations(): Map<Pane, Destination?> {
    val current by currentDestination
    return panesToDestinationsTransform(current)
}