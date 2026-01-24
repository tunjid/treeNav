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

package com.tunjid.treenav.compose.threepane.panedecorators

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import com.tunjid.treenav.compose.panedecorators.paneMappingDecorator
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * An [PaneDecorator] that selectively displays panes for a [ThreePane] layout
 * based on the space available determined by the [windowWidthState].
 *
 * @param windowWidthState provides the current width of the display in Dp.
 */
fun <NavigationState : Node, Destination : Node> threePaneAdaptiveDecorator(
    windowWidthState: State<Dp>,
    secondaryPaneBreakPoint: State<Dp> = mutableStateOf(SECONDARY_PANE_MIN_WIDTH_BREAKPOINT_DP),
    tertiaryPaneBreakPoint: State<Dp> = mutableStateOf(TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP),
): PaneDecorator<NavigationState, Destination, ThreePane> =
    paneMappingDecorator { destination, destinationPaneMapper ->
        val showSecondary by remember {
            derivedStateOf { windowWidthState.value >= secondaryPaneBreakPoint.value }
        }
        val showTertiary by remember {
            derivedStateOf { windowWidthState.value >= tertiaryPaneBreakPoint.value }
        }

        val originalMapping = destinationPaneMapper(destination)
        val primaryNode = originalMapping[ThreePane.Primary]
        mapOf(
            ThreePane.Primary to primaryNode,
            ThreePane.Secondary to originalMapping[ThreePane.Secondary].takeIf { secondaryDestination ->
                secondaryDestination?.id != primaryNode?.id && showSecondary
            },
            ThreePane.Tertiary to originalMapping[ThreePane.Tertiary].takeIf { tertiaryDestination ->
                tertiaryDestination?.id != primaryNode?.id && showTertiary
            },
        )
    }

private val SECONDARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 600.dp
private val TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 1200.dp
