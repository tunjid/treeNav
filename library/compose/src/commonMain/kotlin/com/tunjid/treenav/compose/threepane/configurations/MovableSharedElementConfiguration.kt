package com.tunjid.treenav.compose.threepane.configurations

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.PaneStrategy
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.delegated
import com.tunjid.treenav.compose.moveablesharedelement.AdaptiveMovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane


/**
 * An [PanedNavHostConfiguration] that applies semantics of movable shared elements to
 * [ThreePane] layouts.
 *
 * @param movableSharedElementHostState the host state for coordinating movable shared elements.
 * There should be one instance of this per [PanedNavHost].
 */
fun <NavigationState : Node, Destination : Node> PanedNavHostConfiguration<
        ThreePane,
        NavigationState,
        Destination
        >.threePanedMovableSharedElementConfiguration(
    movableSharedElementHostState: MovableSharedElementHostState<ThreePane, Destination>,
): PanedNavHostConfiguration<ThreePane, NavigationState, Destination> =
    delegated { destination ->
        val originalStrategy =
            this@threePanedMovableSharedElementConfiguration.strategyTransform(destination)
        PaneStrategy(
            transitions = originalStrategy.transitions,
            paneMapper = originalStrategy.paneMapper,
            render = { paneDestination ->
                val delegate = remember {
                    AdaptiveMovableSharedElementScope(
                        paneScope = this,
                        movableSharedElementHostState = movableSharedElementHostState,
                    )
                }
                delegate.paneScope = this

                val movableSharedElementScope = remember {
                    ThreePaneMovableSharedElementScope(
                        hostState = movableSharedElementHostState,
                        delegate = delegate,
                    )
                }

                originalStrategy.render(movableSharedElementScope, paneDestination)
            },
        )
    }

fun <Destination : Node> PaneScope<ThreePane, Destination>.movableSharedElementScope(): MovableSharedElementScope {
    check(this is ThreePaneMovableSharedElementScope) {
        """
            The current AdaptivePaneScope (${this::class.qualifiedName}) is not an instance of
            a ThreePaneMovableSharedElementScope. You must configure your ThreePane AdaptiveNavHost with
            AdaptiveNavHostConfiguration.movableSharedElementConfiguration(movableSharedElementHostState).
           
        """.trimIndent()
    }
    return this
}

@Stable
private class ThreePaneMovableSharedElementScope<Destination : Node>(
    private val hostState: MovableSharedElementHostState<ThreePane, Destination>,
    private val delegate: AdaptiveMovableSharedElementScope<ThreePane, Destination>,
) : MovableSharedElementScope,
    PaneScope<ThreePane, Destination> by delegate.paneScope {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        val paneScope = delegate.paneScope
        return when (paneScope.paneState.pane) {
            null -> throw IllegalArgumentException(
                "Shared elements may only be used in non null panes"
            )
            // Allow shared elements in the primary or transient primary content only
            ThreePane.Primary -> when {
                // Show a blank space for shared elements between the destinations
                paneScope.isPreviewingBack && hostState.isCurrentlyShared(key) -> { _, modifier ->
                    Box(modifier)
                }
                // If previewing and it won't be shared, show the item as is
                paneScope.isPreviewingBack -> sharedElement
                // Share the element
                else -> delegate.movableSharedElementOf(
                    key = key,
                    boundsTransform = boundsTransform,
                    sharedElement = sharedElement
                )
            }
            // Share the element when in the transient pane
            ThreePane.TransientPrimary -> delegate.movableSharedElementOf(
                key = key,
                boundsTransform = boundsTransform,
                sharedElement = sharedElement
            )

            // In the other panes use the element as is
            ThreePane.Secondary,
            ThreePane.Tertiary,
            ThreePane.Overlay -> sharedElement
        }
    }
}

fun PaneState<ThreePane, *>?.canAnimateOnStartingFrames() =
    this?.pane != ThreePane.TransientPrimary

private val PaneScope<ThreePane, *>.isPreviewingBack: Boolean
    get() = paneState.pane == ThreePane.Primary
            && paneState.adaptations.contains(ThreePane.PrimaryToTransient)