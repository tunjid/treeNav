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

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.Adaptation
import com.tunjid.treenav.compose.Adaptation.Swap
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.PaneScope

/**
 * A [PaneEntry] for apps that display up to 3 major panes as once.
 * It also provides extra panes for transient content.
 */
enum class ThreePane {
    /**
     * The pane for the foremost content the user is interacting with. This is typically the
     * actual navigation destination.
     */
    Primary,

    /**
     * An optional pane for displaying a navigation destination alongside the [Primary] pane.
     * This is useful for list-detail, or supporting panels flows.
     */
    Secondary,

    /**
     * An optional pane to display an navigation destination alongside both the [Primary] and
     * [Secondary] panes. This is typically used when the screen is large enough to display 3
     * panes simultaneously.
     */
    Tertiary,

    /**
     * An optional pane for showing dialogs, or context sheets over existing panes.
     */
    Overlay;
}

/**
 * Provides a default [PaneEntry] for selectively running animations in
 * [ThreePane] [MultiPaneDisplay].
 *
 * @param enterTransition the [EnterTransition] used when this [PaneEntry] adapts in the display.
 * @param exitTransition the [ExitTransition] used when this [PaneEntry] adapts in the display.
 * @param paneMapping the [Destination]s that are shown alongside the [Destination] provided and
 * which of the [ThreePane]s they should show up in.
 * @param render the Composable for rendering the current destination.
 */
fun <Destination : Node> threePaneEntry(
    enterTransition: PaneScope<ThreePane, Destination>.() -> EnterTransition = {
        if (canAnimate()) DefaultFadeIn else EnterTransition.None
    },
    exitTransition: PaneScope<ThreePane, Destination>.() -> ExitTransition = {
        if (canAnimate()) DefaultFadeOut else ExitTransition.None
    },
    paneMapping: @Composable (Destination) -> Map<ThreePane, Destination?> = {
        mapOf(ThreePane.Primary to it)
    },
    render: @Composable (PaneScope<ThreePane, Destination>.(Destination) -> Unit),
) = PaneEntry(
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    paneMapping = paneMapping,
    content = render
)

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)

private val DefaultFadeIn = fadeIn(
    animationSpec = RouteTransitionAnimationSpec,
)

private val DefaultFadeOut = fadeOut(
    animationSpec = RouteTransitionAnimationSpec,
)

private fun PaneScope<ThreePane, *>.canAnimate() =
    when {
        transition.targetState == EnterExitState.PostExit -> true
        inPredictiveBack && isActive -> true
        paneState.adaptations.any { adaptation ->
            adaptation is Adaptation.Same
        } -> false

        paneState.adaptations.any { adaptation ->
            adaptation is Adaptation.Pop || adaptation is Adaptation.Change
        } && paneState.adaptations.none {
            it is Swap<*>
        } -> true

        else -> when (val pane = paneState.pane) {
            ThreePane.Primary,
            ThreePane.Secondary,
            ThreePane.Tertiary -> paneState.adaptations.any { adaptation ->
                adaptation is Swap<*> && adaptation.from == pane
            }

            ThreePane.Overlay,
            null -> true
        }
    }