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


@file:Suppress("unused")

package com.tunjid.treenav.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.tunjid.treenav.Node

/**
 * A type alias for [PaneMovableElementSharedTransitionScope] for usages where the generic types
 * are not required.
 */
typealias MovableElementSharedTransitionScope = PaneMovableElementSharedTransitionScope<*, *>

/**
 * An interface providing both [MovableSharedElementScope] and [PaneSharedTransitionScope]
 * semantics.
 */
@Stable
interface PaneMovableElementSharedTransitionScope<Pane, Destination : Node> :
    PaneSharedTransitionScope<Pane, Destination>, MovableSharedElementScope

/**
 * Remembers a [PaneMovableElementSharedTransitionScope] in the composition.
 *
 * @param paneSharedTransitionScope the backing [PaneSharedTransitionScope] for this [PaneScope].
 * @param movableSharedElementScope the backing [MovableSharedElementScope] for this [PaneScope].
 */
@Composable
fun <Pane, Destination : Node> rememberPaneMovableElementSharedTransitionScope(
    paneSharedTransitionScope: PaneSharedTransitionScope<Pane, Destination>,
    movableSharedElementScope: MovableSharedElementScope,
): PaneMovableElementSharedTransitionScope<Pane, Destination> {
    return remember(paneSharedTransitionScope, movableSharedElementScope) {
        DelegatingPaneMovableElementSharedTransitionScope(
            paneSharedTransitionScope = paneSharedTransitionScope,
            movableSharedElementScope = movableSharedElementScope,
        )
    }
}

@Stable
private class DelegatingPaneMovableElementSharedTransitionScope<Pane, Destination : Node>(
    val paneSharedTransitionScope: PaneSharedTransitionScope<Pane, Destination>,
    val movableSharedElementScope: MovableSharedElementScope,
) : PaneMovableElementSharedTransitionScope<Pane, Destination>,
    PaneSharedTransitionScope<Pane, Destination> by paneSharedTransitionScope,
    MovableSharedElementScope by movableSharedElementScope
