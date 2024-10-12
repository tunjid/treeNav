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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

/**
 * Class for configuring a [PanedNavHost] for selecting, adapting and placing navigation
 * destinations into different panes from an arbitrary [navigationState].
 *
 * @param navigationState the navigation state to be adapted into various panes.
 * @param destinationTransform a transform of the [navigationState] to its current destination.
 * @param strategyTransform provides the strategy used to adapt the current destination to the
 * panes available.
 */
@Stable
class PanedNavHostConfiguration<Pane, NavigationState : Node, Destination : Node> internal constructor(
    val navigationState: State<NavigationState>,
    val destinationTransform: (NavigationState) -> Destination,
    val strategyTransform: (destination: Destination) -> PaneStrategy<Pane, Destination>
) {
    internal val currentDestination: State<Destination> = derivedStateOf {
        destinationTransform(navigationState.value)
    }
}

/**
 * Provides an [PanedNavHostConfiguration] for configuring an [PanedNavHost] for
 * showing different navigation destinations into different panes from an arbitrary
 * [navigationState].
 *
 * @param navigationState the navigation state to with destinations [Destination] various
 * panes [Pane].
 * @param destinationTransform a transform of the [navigationState] to its current destination.
 * It is read inside a [derivedStateOf] block, so reads of snapshot
 * state objects will be observed.
 * @param strategyTransform provides the strategy used to adapt the current destination to the
 * panes available.
 */
fun <Pane, NavigationState : Node, Destination : Node> panedNavHostConfiguration(
    navigationState: State<NavigationState>,
    destinationTransform: (NavigationState) -> Destination,
    strategyTransform: (destination: Destination) -> PaneStrategy<Pane, Destination>
) = PanedNavHostConfiguration(
    navigationState = navigationState,
    destinationTransform = destinationTransform,
    strategyTransform = strategyTransform,
)

/**
 * Creates a new [PanedNavHost] by delegating to [this] and  rendering destinations into different panes.
 *
 * @param destinationTransform a transform of [PanedNavHostConfiguration.navigationState]
 * to its current destination. It is read inside a [derivedStateOf] block, so reads of snapshot
 * state objects will be observed.
 * @param strategyTransform provides the strategy used to adapt the current destination to the
 * panes available.
 */
fun <Pane, NavigationState : Node, Destination : Node> PanedNavHostConfiguration<
        Pane,
        NavigationState,
        Destination
        >.delegated(
    destinationTransform: (NavigationState) -> Destination = this@delegated.destinationTransform,
    strategyTransform: (destination: Destination) -> PaneStrategy<Pane, Destination>
) = panedNavHostConfiguration(
    navigationState = this@delegated.navigationState,
    destinationTransform = destinationTransform,
    strategyTransform = strategyTransform,
)

/**
 * The current destination in a given [paneScope].
 */
@Composable
internal fun <Pane, Destination : Node> PanedNavHostConfiguration<
        Pane,
        *,
        Destination
        >.Destination(
    paneScope: PaneScope<Pane, Destination>
) {
    val current = remember(paneScope.paneState.currentDestination) {
        paneScope.paneState.currentDestination
    } ?: return
    with(strategyTransform(current)) {
        val enterAndExit = transitions(paneScope)
        with(paneScope) {
            Box(
                modifier = Modifier.animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            ) {
                render(current)
            }
        }
    }
}

/**
 * THe current pane mapping to use in the [PanedNavHost].
 */
@Composable
internal fun <Pane, Destination : Node> PanedNavHostConfiguration<
        Pane,
        *,
        Destination
        >.paneMapping(): Map<Pane, Destination?> {
    val current by currentDestination
    return current.let {
        strategyTransform(it).paneMapper(it)
    }
}