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

package com.tunjid.treenav.compose.threepane

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.threepane.transforms.requireMovableSharedElementScope

/**
 * An interface providing both [MovableSharedElementScope] and [PaneSharedTransitionScope] for
 * a [ThreePane] layout.
 */
@Stable
interface PaneMovableElementSharedTransitionScope<Destination : Node> :
    PaneSharedTransitionScope<ThreePane, Destination>, MovableSharedElementScope

/**
 * Remembers a [PaneMovableElementSharedTransitionScope] in the composition.
 *
 * @param movableSharedElementScope The [MovableSharedElementScope] used create a
 * [PaneSharedTransitionScope] for this [PaneScope].
 *
 * If one is not provided, one is retrieved from this [PaneScope] using
 * [requireMovableSharedElementScope].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <Destination : Node> PaneScope<
        ThreePane,
        Destination
        >.rememberPaneMovableElementSharedTransitionScope(
    movableSharedElementScope: MovableSharedElementScope = requireMovableSharedElementScope()
): PaneMovableElementSharedTransitionScope<Destination> {
    val paneSharedTransitionScope = rememberPaneSharedTransitionScope(
        movableSharedElementScope.sharedTransitionScope
    )
    return remember {
        DelegatingPaneMovableElementSharedTransitionScope(
            paneSharedTransitionScope = paneSharedTransitionScope,
            movableSharedElementScope = movableSharedElementScope,
        )
    }
}

@Stable
private class DelegatingPaneMovableElementSharedTransitionScope<Destination : Node>(
    val paneSharedTransitionScope: PaneSharedTransitionScope<ThreePane, Destination>,
    val movableSharedElementScope: MovableSharedElementScope,
) : PaneMovableElementSharedTransitionScope<Destination>,
    PaneSharedTransitionScope<ThreePane, Destination> by paneSharedTransitionScope,
    MovableSharedElementScope by movableSharedElementScope

