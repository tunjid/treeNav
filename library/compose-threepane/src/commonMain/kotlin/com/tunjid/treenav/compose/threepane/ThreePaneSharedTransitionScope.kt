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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneSharedTransitionScope

/**
 * Creates and remembers a [PaneSharedTransitionScope] for [ThreePane] layouts with
 * opinionated semantics for how shared elements move between panes, especially with back previews.
 *
 * @param sharedTransitionScope the [SharedTransitionScope] to be delegated to for core
 * shared transition APIs.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <Destination : Node> PaneScope<
        ThreePane,
        Destination
        >.rememberPaneSharedTransitionScope(
    sharedTransitionScope: SharedTransitionScope,
): PaneSharedTransitionScope<ThreePane, Destination> =
    remember(this, sharedTransitionScope) {
        ThreePaneSharedTransitionScope(
            paneScope = this,
            sharedTransitionScope = sharedTransitionScope
        )
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
private class ThreePaneSharedTransitionScope<Destination : Node> @OptIn(
    ExperimentalSharedTransitionApi::class
) constructor(
    val paneScope: PaneScope<ThreePane, Destination>,
    val sharedTransitionScope: SharedTransitionScope,
) : PaneSharedTransitionScope<ThreePane, Destination>,
    PaneScope<ThreePane, Destination> by paneScope,
    SharedTransitionScope by sharedTransitionScope {

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun Modifier.paneSharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
    ): Modifier = when (paneScope.paneState.pane) {
        null -> throw IllegalArgumentException(
            "Shared elements may only be used in non null panes"
        )
        // Allow shared elements in the primary or transient primary content only
        ThreePane.Primary -> sharedElement(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = paneScope,
            boundsTransform = boundsTransform,
            placeHolderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
        )

        // In the other panes use the element as is
        ThreePane.Secondary,
        ThreePane.Tertiary,
        ThreePane.Overlay,
            -> this
    }

    override fun Modifier.paneStickySharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip
    ): Modifier = when (paneScope.paneState.pane) {
        null -> throw IllegalArgumentException(
            "Shared elements may only be used in non null panes"
        )
        // Allow shared elements in the primary or transient primary content only
        ThreePane.Primary -> sharedElementWithCallerManagedVisibility(
            sharedContentState = sharedContentState,
            visible = isActive,
            boundsTransform = boundsTransform,
            placeHolderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
        )

        // In the other panes use the element as is
        ThreePane.Secondary,
        ThreePane.Tertiary,
        ThreePane.Overlay,
            -> this
    }
}