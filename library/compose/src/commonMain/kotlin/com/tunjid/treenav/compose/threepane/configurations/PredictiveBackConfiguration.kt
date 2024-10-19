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

package com.tunjid.treenav.compose.threepane.configurations

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.delegated
import com.tunjid.treenav.compose.paneStrategy
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * An [PanedNavHostConfiguration] that moves the destination in a [ThreePane.Primary] pane, to
 * to the [ThreePane.TransientPrimary] pane when a predictive back gesture is in progress.
 *
 * @param isPreviewingBack provides the state of the predictive back gesture.
 * True if the gesture is ongoing.
 * @param backPreviewTransform provides the [NavigationState] if the predictive back gesture
 * were to be completed.
 */
inline fun <NavigationState : Node, reified Destination : Node> PanedNavHostConfiguration<
        ThreePane,
        NavigationState,
        Destination
        >.predictiveBackConfiguration(
    isPreviewingBack: State<Boolean>,
    crossinline backPreviewTransform: NavigationState.() -> NavigationState,
): PanedNavHostConfiguration<ThreePane, NavigationState, Destination> {
    var lastPrimaryDestination by mutableStateOf<Destination?>(null)
    return delegated(
        destinationTransform = { navigationState ->
            val current = destinationTransform(navigationState)
            lastPrimaryDestination = current
            if (isPreviewingBack.value) destinationTransform(navigationState.backPreviewTransform())
            else current
        },
        strategyTransform = { destination ->
            val originalStrategy = strategyTransform(destination)
            paneStrategy(
                transitions = originalStrategy.transitions,
                paneMapping = paneMapper@{ inner ->
                    val originalMapping = originalStrategy.paneMapper(inner)
                    val isPreviewing by isPreviewingBack
                    if (!isPreviewing) return@paneMapper originalMapping
                    // Back is being previewed, therefore the original mapping is already for back.
                    // Pass the previous primary value into transient.
                    val transientDestination = checkNotNull(lastPrimaryDestination) {
                        "Attempted to show last destination without calling destination transform"
                    }
                    val paneMapping = strategyTransform(transientDestination)
                        .paneMapper(transientDestination)
                    val transient = paneMapping[ThreePane.Primary]
                    originalMapping + (ThreePane.TransientPrimary to transient)
                },
                render = originalStrategy.render
            )
        }
    )
}
