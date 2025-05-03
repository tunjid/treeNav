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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

/**
 * A [SharedTransitionScope] that is aware of the relationship between the [Pane]s in
 * its [MultiPaneDisplay]. This allows for defining the semantics of
 * shared element behavior when shared elements move in between [Pane]s during the
 * transition.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
interface PaneSharedTransitionScope<Pane, Destination : Node> :
    PaneScope<Pane, Destination>, SharedTransitionScope {

    /**
     * Conceptual equivalent of [SharedTransitionScope.sharedElement], with the exception
     * of a key being passed instead of [SharedTransitionScope.SharedContentState]. This is because
     * each [PaneState.pane] may need its own [SharedTransitionScope.SharedContentState] and
     * will need to be managed by the implementation of this method.
     *
     * @see [SharedTransitionScope.sharedElement].
     */
    fun Modifier.panedSharedElement(
        key: Any,
        boundsTransform: BoundsTransform = Defaults.DefaultBoundsTransform,
        placeHolderSize: PlaceHolderSize = contentSize,
        renderInOverlayDuringTransition: Boolean = true,
        visible: Boolean? = null,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
    ): Modifier
}
