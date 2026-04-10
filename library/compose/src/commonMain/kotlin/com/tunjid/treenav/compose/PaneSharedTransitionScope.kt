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
import androidx.compose.animation.SharedTransitionDefaults
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.ContentSize
import androidx.compose.runtime.Composable
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
    PaneScope<Pane, Destination>,
    SharedTransitionScope {

    /**
     * Creates a shared element transition where the shared element is seekable
     * with the transition with the transition driving the [PaneScope].
     *
     * This is a pane specific implementation of [SharedTransitionScope.sharedElement], where
     * implementations can specify extra logic for how the shared element behaves when
     * rendered concurrently in multiple panes.
     *
     * Implementations must override [CMP9841.PaneSharedElementImpl], not this method. See
     * [CMP9841] for why.
     *
     * @see [SharedTransitionScope.sharedElement].
     */
    @Composable
    fun PaneSharedElement(
        modifier: Modifier = Modifier,
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform = SharedTransitionDefaults.BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize = ContentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
        content: @Composable MinConstraintBoxScope.() -> Unit,
    ) = with(CMP9841) {
        PaneSharedElementImpl(
            modifier = modifier,
            sharedContentState = sharedContentState,
            boundsTransform = boundsTransform,
            placeholderSize = placeholderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            content = content,
        )
    }

    /**
     * Override point for [PaneSharedElement]. See [CMP9841].
     */
    @Composable
    fun CMP9841.PaneSharedElementImpl(
        modifier: Modifier,
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        content: @Composable MinConstraintBoxScope.() -> Unit,
    )

    /**
     * Creates a shared element transition where the shared element is __NOT__ seekable
     * with the transition. Instead, it always "sticks" to the "active" pane as specified by
     * [PaneScope.isActive].
     *
     * This is a pane specific implementation of [SharedTransitionScope.sharedElement], where
     * implementations can specify extra logic for how the shared element behaves when
     * rendered concurrently in multiple panes.
     *
     * Implementations must override [CMP9841.PaneStickySharedElementImpl], not this method. See
     * [CMP9841] for why.
     *
     * @see [SharedTransitionScope.sharedElement].
     */
    @Composable
    fun PaneStickySharedElement(
        modifier: Modifier = Modifier,
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform = SharedTransitionDefaults.BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize = ContentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
        content: @Composable MinConstraintBoxScope.() -> Unit,
    ) = with(CMP9841) {
        PaneStickySharedElementImpl(
            modifier = modifier,
            sharedContentState = sharedContentState,
            boundsTransform = boundsTransform,
            placeholderSize = placeholderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition,
            content = content,
        )
    }

    /**
     * Override point for [PaneStickySharedElement]. See [CMP9841].
     */
    @Composable
    fun CMP9841.PaneStickySharedElementImpl(
        modifier: Modifier,
        sharedContentState: SharedTransitionScope.SharedContentState,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        content: @Composable MinConstraintBoxScope.() -> Unit,
    )

    /**
     * Marker receiver for the override hooks that work around
     * [CMP-9841](https://youtrack.jetbrains.com/issue/CMP-9841).
     *
     * Overriding a default-paramed `@Composable` interface method on a class that also uses
     * interface delegation triggers a Kotlin/Native IR linkage crash at runtime on iOS. To
     * work around it, the real override hooks ([PaneSharedElementImpl] and
     * [PaneStickySharedElementImpl]) are declared as abstract member extensions on this
     * marker object — they have no default parameter values (so the Compose plugin does
     * not emit a `ComposeDefaultImpls.$default` trampoline for them) and their extension
     * receiver is [CMP9841] rather than [PaneSharedTransitionScope] itself, so they do not
     * appear in IDE autocomplete at user call sites. Implementers provide behavior by
     * overriding the member extensions; the default bodies of [PaneSharedElement] and
     * [PaneStickySharedElement] dispatch to them via `with(CMP9841) { ... }`.
     */
    companion object CMP9841
}
