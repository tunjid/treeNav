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

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.delegated
import com.tunjid.treenav.compose.paneStrategy

/**
 * A [PanedNavHostConfiguration] that allows for centrally defining the [Modifier] for
 * each [Pane] displayed within it.
 *
 * @param paneModifier a lambda for specifying the [Modifier] for each [Pane] in a [PaneScope].
 */
fun <Pane, NavigationState : Node, Destination : Node> PanedNavHostConfiguration<
        Pane,
        NavigationState,
        Destination
        >.paneModifierConfiguration(
    paneModifier: PaneScope<Pane, Destination>.() -> Modifier = { Modifier },
): PanedNavHostConfiguration<Pane, NavigationState, Destination> = delegated {
    val originalTransform = strategyTransform(it)
    paneStrategy(
        transitions = originalTransform.transitions,
        paneMapping = originalTransform.paneMapper,
        render = render@{ destination ->
            Box(
                modifier = paneModifier()
            ) {
                originalTransform.render(this@render, destination)
            }
        }
    )
}