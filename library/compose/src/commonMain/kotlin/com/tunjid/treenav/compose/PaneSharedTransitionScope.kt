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

package com.tunjid.treenav.compose

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.ContentSize
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

/**
 * A [SharedTransitionScope] that is aware of the relationship between the [Pane]s in
 * its [MultiPaneDisplay]. This allows for defining the semantics of
 * shared element behavior when shared elements move in between [Pane]s during the
 * transition.
 */
@Stable
interface PaneSharedTransitionScope<Pane, Destination : Node> :
    PaneScope<Pane, Destination>, SharedTransitionScope {

    /**
     * Creates a shared element transition where the shared element is seekable
     * with the transition with the transition driving the [PaneScope].
     *
     * This is a pane specific implementation of [SharedTransitionScope.sharedElement], where
     * implementations can specify extra logic for how the shared element behaves when
     * rendered concurrently in multiple panes.
     *
     * @see [SharedTransitionScope.sharedElement].
     */
    fun Modifier.paneSharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform = Defaults.DefaultBoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize = ContentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
    ): Modifier

    /**
     * Creates a shared element transition where the shared element is __NOT__ seekable
     * with the transition. Instead, it always "sticks" to the "active" pane as specified by
     * [PaneScope.isActive].
     *
     * This is a pane specific implementation of [SharedTransitionScope.sharedElement], where
     * implementations can specify extra logic for how the shared element behaves when
     * rendered concurrently in multiple panes.
     *
     * @see [SharedTransitionScope.sharedElement].
     */
    fun Modifier.paneStickySharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform = Defaults.DefaultBoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize = ContentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
    ): Modifier
}
