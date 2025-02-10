package com.tunjid.treenav.compose.threepane.transforms

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.transforms.PaneTransform
import com.tunjid.treenav.compose.transforms.Transform
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * An [Transform] that selectively displays panes for a [ThreePane] layout
 * based on the space available determined by the [windowWidthState].
 *
 * @param windowWidthState provides the current width of the display in Dp.
 */
fun <NavigationState : Node, Destination : Node>
        threePanedAdaptiveTransform(
    windowWidthState: State<Dp>,
    secondaryPaneBreakPoint: State<Dp> = mutableStateOf(SECONDARY_PANE_MIN_WIDTH_BREAKPOINT_DP),
    tertiaryPaneBreakPoint: State<Dp> = mutableStateOf(TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP),
): Transform<ThreePane, NavigationState, Destination> =
    PaneTransform { destination, previousTransform ->
        // Consider navigation state different if window size class changes
        val windowWidthDp by windowWidthState
        val originalMapping = previousTransform(destination)
        val primaryNode = originalMapping[ThreePane.Primary]
        mapOf(
            ThreePane.Primary to primaryNode,
            ThreePane.Secondary to originalMapping[ThreePane.Secondary].takeIf { secondaryDestination ->
                secondaryDestination?.id != primaryNode?.id
                        && windowWidthDp >= secondaryPaneBreakPoint.value
            },
            ThreePane.Tertiary to originalMapping[ThreePane.Tertiary].takeIf { tertiaryDestination ->
                tertiaryDestination?.id != primaryNode?.id
                        && windowWidthDp >= tertiaryPaneBreakPoint.value
            },
        )
    }

private val SECONDARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 600.dp
private val TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 1200.dp