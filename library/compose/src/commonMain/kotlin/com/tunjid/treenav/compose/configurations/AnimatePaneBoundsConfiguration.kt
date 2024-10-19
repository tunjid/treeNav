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

package com.tunjid.treenav.compose.configurations

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.delegated
import com.tunjid.treenav.compose.utilities.AnimatedBoundsState
import com.tunjid.treenav.compose.utilities.AnimatedBoundsState.Companion.animateBounds
import com.tunjid.treenav.compose.utilities.DefaultBoundsTransform

/**
 * A [PanedNavHostConfiguration] that animates the bounds of each [Pane] displayed within it.
 * This is useful for scenarios where the panes move within a layout hierarchy to accommodate
 * other panes.
 *
 * @param lookaheadScope the root [LookaheadScope] where the panes are rendered in.
 * @param paneBoundsTransform a lambda providing the [BoundsTransform] for each [Pane].
 * @param shouldAnimatePane a lambda for toggling when the pane can be animated. It allows for
 * skipping an animation in progress.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun <Pane, NavigationState : Node, Destination : Node> PanedNavHostConfiguration<
        Pane,
        NavigationState,
        Destination
        >.animatePaneBoundsConfiguration(
    lookaheadScope: LookaheadScope,
    paneBoundsTransform: PaneScope<Pane, Destination>.() -> BoundsTransform = { DefaultBoundsTransform },
    shouldAnimatePane: PaneScope<Pane, Destination>.() -> Boolean = { true },
): PanedNavHostConfiguration<Pane, NavigationState, Destination> = delegated { navigationDestination ->
    val originalStrategy = strategyTransform(navigationDestination)
    originalStrategy.delegated(
        render = render@{ paneDestination ->
            Box(
                modifier = Modifier.animateBounds(
                    state = remember {
                        AnimatedBoundsState(
                            lookaheadScope = lookaheadScope,
                            boundsTransform = paneBoundsTransform(),
                            inProgress = { shouldAnimatePane() }
                        )
                    }
                )
            ) {
                originalStrategy.render(this@render, paneDestination)
            }
        }
    )
}