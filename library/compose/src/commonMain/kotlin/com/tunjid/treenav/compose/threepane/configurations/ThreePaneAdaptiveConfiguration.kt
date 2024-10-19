package com.tunjid.treenav.compose.threepane.configurations

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.delegated
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * An [PanedNavHostConfiguration] that selectively displays panes for a [ThreePane] layout
 * based on the space available determined by the [windowWidthDpState].
 *
 * @param windowWidthDpState provides the current width of the display in Dp.
 */
fun <NavigationState : Node, Destination : Node> PanedNavHostConfiguration<
        ThreePane,
        NavigationState,
        Destination
        >.threePanedNavHostConfiguration(
    windowWidthDpState: State<Int>,
    secondaryPaneBreakPoint: State<Int> = mutableStateOf(SECONDARY_PANE_MIN_WIDTH_BREAKPOINT_DP),
    tertiaryPaneBreakPoint: State<Int> = mutableStateOf(TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP),
): PanedNavHostConfiguration<ThreePane, NavigationState, Destination> = delegated { destination ->
    val originalStrategy = strategyTransform(destination)
    originalStrategy.delegated(
        paneMapping = { navigationDestinationToMap ->
            // Consider navigation state different if window size class changes
            val windowWidthDp by windowWidthDpState
            val originalMapping = originalStrategy.paneMapper(navigationDestinationToMap)
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
    )
}

private const val SECONDARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 600
private const val TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 1200