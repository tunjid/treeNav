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

import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MovableSharedElementScope
import com.tunjid.treenav.compose.PaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneSharedTransitionScope
import com.tunjid.treenav.compose.rememberPaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.panedecorators.requireThreePaneMovableSharedElementScope

/**
 * An interface providing both [MovableSharedElementScope] and [PaneSharedTransitionScope] for
 * a [ThreePane] layout.
 */
typealias ThreePaneMovableElementSharedTransitionScope<Destination> =
    PaneMovableElementSharedTransitionScope<ThreePane, Destination>

/**
 * Remembers a [ThreePaneMovableElementSharedTransitionScope] in the composition.
 *
 * @param movableSharedElementScope The [MovableSharedElementScope] used create a
 * [PaneSharedTransitionScope] for this [PaneScope].
 *
 * If one is not provided, one is retrieved from this [PaneScope] using
 * [requireThreePaneMovableSharedElementScope].
 */

@Composable
fun <Destination : Node> PaneScope<
    ThreePane,
    Destination,
    >.rememberThreePaneMovableElementSharedTransitionScope(
    movableSharedElementScope: MovableSharedElementScope = requireThreePaneMovableSharedElementScope(),
): ThreePaneMovableElementSharedTransitionScope<Destination> {
    val paneSharedTransitionScope = rememberPaneSharedTransitionScope(
        movableSharedElementScope.sharedTransitionScope,
    )
    return rememberPaneMovableElementSharedTransitionScope(
        paneSharedTransitionScope = paneSharedTransitionScope,
        movableSharedElementScope = movableSharedElementScope,
    )
}
