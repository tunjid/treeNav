package com.tunjid.treenav.adaptive.threepane.configurations

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.adaptivePaneStrategy
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.threepane.ThreePane

/**
 * An [AdaptiveNavHostConfiguration] that selectively displays panes for a [ThreePane] layout
 * based on the space available determined by the [WindowSizeClass].
 *
 * @param windowWidthDpState provides the current width of the display in Dp.
 */
fun <NavigationState : Node, Destination : Node> AdaptiveNavHostConfiguration<ThreePane, NavigationState, Destination>.threePaneAdaptiveConfiguration(
    windowWidthDpState: State<Int>,
): AdaptiveNavHostConfiguration<ThreePane, NavigationState, Destination> = delegated { node ->
    val originalStrategy = this@threePaneAdaptiveConfiguration.strategyTransform(node)
    adaptivePaneStrategy(
        render = originalStrategy.render,
        transitions = originalStrategy.transitions,
        paneMapping = { inner ->
            // Consider navigation state different if window size class changes
            val windowWidthDp by windowWidthDpState
            val originalMapping = originalStrategy.paneMapper(inner)
            val primaryNode = originalMapping[ThreePane.Primary]
            mapOf(
                ThreePane.Primary to primaryNode,
                ThreePane.Secondary to originalMapping[ThreePane.Secondary].takeIf { secondaryDestination ->
                    secondaryDestination?.id != primaryNode?.id
                            && windowWidthDp >= PRIMARY_PANE_MIN_WIDTH_BREAKPOINT_DP
                },
                ThreePane.Tertiary to originalMapping[ThreePane.Tertiary].takeIf { tertiaryDestination ->
                    tertiaryDestination?.id != primaryNode?.id
                            && windowWidthDp >= TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP
                },
            )
        }
    )
}

private const val PRIMARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 600
private const val TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 1200