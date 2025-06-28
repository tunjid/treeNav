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
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.Adaptation
import com.tunjid.treenav.compose.Adaptation.Swap
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.PaneMovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.rememberPaneMovableSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.transforms.PaneTransform
import com.tunjid.treenav.compose.transforms.paneRenderTransform

/**
 * A [PaneTransform] that applies semantics of movable shared elements to
 * [ThreePane] layouts.
 *
 * It is an opinionated implementation that always shows the movable shared element in
 * the [ThreePane.Primary] pane.
 *
 * Note: The movable shared element is never rendered in the following panes:
 * - [ThreePane.Secondary]
 * - [ThreePane.Tertiary]
 * - [ThreePane.Overlay]
 *
 * @param movableSharedElementHostState the host state for coordinating movable shared elements.
 * There should be one instance of this per [MultiPaneDisplay].
 */
fun <NavigationState : Node, Destination : Node>
        threePanedMovableSharedElementTransform(
    movableSharedElementHostState: MovableSharedElementHostState<ThreePane, Destination>,
): PaneTransform<NavigationState, Destination, ThreePane> =
    paneRenderTransform { destination, destinationPaneMapper ->
        val delegate = rememberPaneMovableSharedElementScope(
            movableSharedElementHostState = movableSharedElementHostState
        )
        delegate.paneScope = this

        val movableSharedElementScope = remember {
            ThreePaneMovableSharedElementScope(
                hostState = movableSharedElementHostState,
                delegate = delegate,
            )
        }

        destinationPaneMapper(movableSharedElementScope, destination)
    }

/**
 * Requires that this [PaneScope] is a [MovableSharedElementScope] specifically configured for
 * [ThreePane] layouts and returns it. This only succeeds if the [MultiPaneDisplayState] has the
 * [threePanedMovableSharedElementTransform] applied to it.
 *
 * In the case this [PaneScope] is not the [MovableSharedElementScope] requested, an exception
 * will be thrown.
 */
@Stable
fun <Destination : Node> PaneScope<
        ThreePane,
        Destination
        >.requireThreePaneMovableSharedElementScope(): MovableSharedElementScope {
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
    private val delegate: PaneMovableSharedElementScope<ThreePane, Destination>,
) : MovableSharedElementScope,
    PaneScope<ThreePane, Destination> {

    override val sharedTransitionScope: SharedTransitionScope
        get() = delegate.sharedTransitionScope

    override val transition: Transition<EnterExitState>
        get() = delegate.paneScope.transition

    override val paneState: PaneState<ThreePane, Destination>
        get() = delegate.paneScope.paneState

    override val isActive: Boolean
        get() = delegate.paneScope.isActive

    override val inPredictiveBack: Boolean
        get() = delegate.paneScope.inPredictiveBack

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        sharedContentState: SharedTransitionScope.SharedContentState,
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
        // Allow movable shared elements in the primary pane only
        ThreePane.Primary -> delegate.movableSharedElementOf(
            sharedContentState = sharedContentState,
            boundsTransform = boundsTransform,
            placeHolderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            alternateOutgoingSharedElement = alternateOutgoingSharedElement,
            sharedElement = sharedElement
        )

        // In the secondary pane allow shared elements only if certain conditions match
        ThreePane.Secondary -> when {
            canAnimateSecondary() -> { state, modifier ->
                with(hostState) {
                    Box(modifier) {
                        Box(
                            Modifier
                                .fillMaxConstraints()
                                .sharedElementWithCallerManagedVisibility(
                                    sharedContentState = sharedContentState,
                                    visible = false,
                                    placeHolderSize = placeHolderSize,
                                    renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                                    zIndexInOverlay = zIndexInOverlay,
                                    clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                                )
                        )
                        (alternateOutgoingSharedElement ?: sharedElement)(
                            state,
                            Modifier
                                .fillMaxConstraints(),
                        )
                    }
                }
            }

            else -> alternateOutgoingSharedElement ?: sharedElement
        }

        // In the other panes use the element as is
        ThreePane.Tertiary,
        ThreePane.Overlay,
            -> alternateOutgoingSharedElement ?: sharedElement
    }
}

private fun PaneScope<ThreePane, *>.canAnimateSecondary(): Boolean {
    if (inPredictiveBack) return false
    if (!paneState.adaptations.contains(PrimaryToSecondary)) return false
    if (paneState.adaptations.contains(Adaptation.Pop)) return false

    return true
}

private val PrimaryToSecondary = Swap(
    from = ThreePane.Primary,
    to = ThreePane.Secondary
)

private fun Modifier.fillMaxConstraints() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = when {
                    constraints.hasBoundedWidth -> constraints.maxWidth
                    else -> constraints.minWidth
                },
                minHeight = when {
                    constraints.hasBoundedHeight -> constraints.maxHeight
                    else -> constraints.minHeight
                }
            )
        )
        layout(
            width = placeable.width,
            height = placeable.height
        ) {
            placeable.place(0, 0)
        }
    }