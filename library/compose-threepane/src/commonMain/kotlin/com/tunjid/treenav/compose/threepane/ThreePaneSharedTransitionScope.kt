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

package com.tunjid.treenav.compose.threepane

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.visible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.MinConstraintBox
import com.tunjid.treenav.compose.MinConstraintBoxScope
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneSharedTransitionScope
import com.tunjid.treenav.compose.SharedElement
import com.tunjid.treenav.compose.SharedElementWithCallerManagedVisibility

/**
 * Creates and remembers a [PaneSharedTransitionScope] for [ThreePane] layouts with
 * opinionated semantics for how shared elements move between panes, especially with back previews.
 *
 * @param sharedTransitionScope the [SharedTransitionScope] to be delegated to for core
 * shared transition APIs.
 */

@Composable
fun <Destination : Node> PaneScope<
    ThreePane,
    Destination,
    >.rememberPaneSharedTransitionScope(
    sharedTransitionScope: SharedTransitionScope,
): PaneSharedTransitionScope<ThreePane, Destination> =
    remember(this, sharedTransitionScope) {
        ThreePaneSharedTransitionScope(
            paneScope = this,
            sharedTransitionScope = sharedTransitionScope,
        )
    }

@Stable
private class ThreePaneSharedTransitionScope<Destination : Node>(
    val paneScope: PaneScope<ThreePane, Destination>,
    val sharedTransitionScope: SharedTransitionScope,
) : PaneSharedTransitionScope<ThreePane, Destination>,
    PaneScope<ThreePane, Destination> by paneScope,
    SharedTransitionScope by sharedTransitionScope {

    // Overrides the member extension on PaneSharedTransitionScope.CMP9841 rather than
    // PaneSharedElement directly. See PaneSharedTransitionScope.CMP9841 for why.
    @Composable
    override fun PaneSharedTransitionScope.CMP9841.PaneSharedElementImpl(
        modifier: Modifier,
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        content: @Composable MinConstraintBoxScope.() -> Unit,
    ) = when (val pane = paneState.pane) {
        null -> throw IllegalArgumentException(
            "Shared elements may only be used in non null panes",
        )
        // Allow movable shared elements in the primary pane only
        ThreePane.Primary,
        ThreePane.Secondary,
        -> SharedElement(
            modifier = modifier,
            sharedContentState = sharedContentState,
            animatedVisibilityScope =
            if (pane == ThreePane.Primary) paneScope
            else rememberStaticExitedAnimatedVisibilityScope(),
            placeholderSize = placeholderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            content = {
                // TODO: Maybe fade content out
                val isInvisible = pane == ThreePane.Primary &&
                    sharedContentState.isMatchFound &&
                    paneScope.transition.targetState != EnterExitState.Visible

                Box(
                    modifier = Modifier
                        .fillParentAxisIfFixedOrWrap()
                        .visible(visible = !isInvisible),
                ) {
                    content()
                }
            },
        )
        // In the other panes use the element as is
        ThreePane.Tertiary,
        ThreePane.Overlay,
        -> MinConstraintBox(modifier) {
            content()
        }
    }

    // Overrides the member extension on PaneSharedTransitionScope.CMP9841 rather than
    // PaneStickySharedElement directly. See PaneSharedTransitionScope.CMP9841 for why.
    @Composable
    override fun PaneSharedTransitionScope.CMP9841.PaneStickySharedElementImpl(
        modifier: Modifier,
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        content: @Composable MinConstraintBoxScope.() -> Unit,
    ) = when (val pane = paneState.pane) {
        null -> throw IllegalArgumentException(
            "Shared elements may only be used in non null panes",
        )

        ThreePane.Primary,
        ThreePane.Secondary,
        -> if (pane == ThreePane.Secondary && !canAnimateSecondary()) MinConstraintBox(
            modifier,
        ) {
            content()
        }
        else SharedElementWithCallerManagedVisibility(
            modifier = modifier,
            sharedContentState = sharedContentState,
            placeholderSize = placeholderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            areBoundsTracked = {
                isActive && pane == ThreePane.Primary
            },
            content = {
                // TODO: Maybe fade content out
                val isInvisible = pane == ThreePane.Primary &&
                    sharedContentState.isMatchFound &&
                    !isActive

                Box(
                    modifier = Modifier
                        .fillParentAxisIfFixedOrWrap()
                        .visible(visible = !isInvisible),
                ) {
                    content()
                }
            },
        )

        ThreePane.Tertiary,
        ThreePane.Overlay,
        -> MinConstraintBox(modifier) {
            content()
        }
    }
}
