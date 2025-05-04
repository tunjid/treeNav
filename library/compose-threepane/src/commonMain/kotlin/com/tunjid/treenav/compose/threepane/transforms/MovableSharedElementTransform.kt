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

package com.tunjid.treenav.compose.threepane.transforms

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.transforms.RenderTransform
import com.tunjid.treenav.compose.transforms.Transform
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.PanedMovableSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * A [Transform] that applies semantics of movable shared elements to
 * [ThreePane] layouts.
 *
 * @param movableSharedElementHostState the host state for coordinating movable shared elements.
 * There should be one instance of this per [MultiPaneDisplay].
 */
fun <NavigationState : Node, Destination : Node>
        threePanedMovableSharedElementTransform(
    movableSharedElementHostState: MovableSharedElementHostState<ThreePane, Destination>,
): Transform<ThreePane, NavigationState, Destination> =
    RenderTransform { destination, previousTransform ->
        val delegate = remember {
            PanedMovableSharedElementScope(
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

        previousTransform(movableSharedElementScope, destination)
    }

fun <Destination : Node> PaneScope<
        ThreePane,
        Destination
        >.requireMovableSharedElementScope(): MovableSharedElementScope {
    check(this is ThreePaneMovableSharedElementScope) {
        """
            The current PaneScope (${this::class.qualifiedName}) is not an instance of
            a ThreePaneMovableSharedElementScope. You must configure your ThreePane MultiPaneDisplay with
            threePanedMovableSharedElementTransform().
        """.trimIndent()
    }
    return this
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
private class ThreePaneMovableSharedElementScope<Destination : Node>(
    private val hostState: MovableSharedElementHostState<ThreePane, Destination>,
    private val delegate: PanedMovableSharedElementScope<ThreePane, Destination>,
) : MovableSharedElementScope,
    PaneScope<ThreePane, Destination> by delegate.paneScope {

    override val sharedTransitionScope: SharedTransitionScope
        get() = delegate.sharedTransitionScope

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit = when (paneState.pane) {
        null -> throw IllegalArgumentException(
            "Shared elements may only be used in non null panes"
        )
        // Allow shared elements in the primary or transient primary content only
        ThreePane.Primary -> when {
            // Show a blank space for shared elements between the destinations
            isPreviewingBack && hostState.isCurrentlyShared(key) -> EmptyElement
            // If previewing and it won't be shared, show the item as is
            isPreviewingBack -> sharedElement
            // Share the element
            else -> delegate.movableSharedElementOf(
                key = key,
                boundsTransform = boundsTransform,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                alternateOutgoingSharedElement = alternateOutgoingSharedElement,
                sharedElement = sharedElement
            )
        }
        // Share the element when in the transient pane
        ThreePane.TransientPrimary -> delegate.movableSharedElementOf(
            key = key,
            boundsTransform = boundsTransform,
            placeHolderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            alternateOutgoingSharedElement = alternateOutgoingSharedElement,
            sharedElement = sharedElement
        )

        // In the other panes use the element as is
        ThreePane.Secondary,
        ThreePane.Tertiary,
        ThreePane.Overlay,
            -> alternateOutgoingSharedElement ?: sharedElement
    }
}

private val PaneScope<ThreePane, *>.isPreviewingBack: Boolean
    get() = paneState.pane == ThreePane.Primary
            && paneState.adaptations.contains(ThreePane.PrimaryToTransient)

// An empty element representing blank space
private val EmptyElement: @Composable (Any?, Modifier) -> Unit = { _, modifier ->
    Box(modifier)
}