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

package com.tunjid.treenav.compose.threepane.transforms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.transforms.DestinationTransform
import com.tunjid.treenav.compose.transforms.PaneTransform
import com.tunjid.treenav.compose.transforms.Transform
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * An [Transform] that moves the destination in a [ThreePane.Primary] pane, to
 * to the [ThreePane.TransientPrimary] pane when a predictive back gesture is in progress.
 *
 * @param isPreviewingBack provides the state of the predictive back gesture.
 * True if the gesture is ongoing.
 * @param backPreviewTransform provides the [NavigationState] if the predictive back gesture
 * were to be completed.
 */
inline fun <NavigationState : Node, reified Destination : Node>
        predictiveBackTransform(
    isPreviewingBack: State<Boolean>,
    crossinline backPreviewTransform: NavigationState.() -> NavigationState,
): Transform<ThreePane, NavigationState, Destination> {
    return object :
        DestinationTransform<ThreePane, NavigationState, Destination>,
        PaneTransform<ThreePane, Destination> {

        var lastPrimaryDestination by mutableStateOf<Destination?>(null)

        override fun toDestination(
            navigationState: NavigationState,
            previousTransform: (NavigationState) -> Destination,
        ): Destination {
            val previousDestination = previousTransform(navigationState)
            lastPrimaryDestination = previousDestination
            return if (isPreviewingBack.value) previousTransform(navigationState.backPreviewTransform())
            else previousDestination
        }

        @Composable
        override fun toPanesAndDestinations(
            destination: Destination,
            previousTransform: @Composable (Destination) -> Map<ThreePane, Destination?>,
        ): Map<ThreePane, Destination?> {
            val previousMapping = previousTransform(destination)
            val isPreviewing by isPreviewingBack
            if (!isPreviewing) return previousMapping
            // Back is being previewed, therefore the original mapping is already for back.
            // Pass the previous primary value into transient.
            val transientDestination = checkNotNull(lastPrimaryDestination) {
                "Attempted to show last destination without calling destination transform"
            }
            val paneMapping = previousTransform(transientDestination)
            val transient = paneMapping[ThreePane.Primary]
            return previousMapping + (ThreePane.TransientPrimary to transient)
        }
    }
}

