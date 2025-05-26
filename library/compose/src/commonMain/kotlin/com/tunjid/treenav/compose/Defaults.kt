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
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Stable
internal object Defaults {

    val EmptyElement: @Composable (Any?, Modifier) -> Unit = { _, _ -> }

    @ExperimentalSharedTransitionApi
    val DefaultBoundsTransform = BoundsTransform { _, _ -> DefaultSpring }

    @ExperimentalSharedTransitionApi
    val ParentClip: OverlayClip =
        object : OverlayClip {
            override fun getClipPath(
                sharedContentState: SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Path? {
                return sharedContentState.parentSharedContentState?.clipPathInOverlay
            }
        }

}

private val DefaultSpring = spring(
    stiffness = StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)
