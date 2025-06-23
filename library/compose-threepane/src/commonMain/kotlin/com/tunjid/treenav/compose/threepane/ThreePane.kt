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

    companion object {
        val PrimaryToSecondary = Swap(
            from = Primary,
            to = Secondary
        )

        val SecondaryToPrimary = Swap(
            from = Secondary,
            to = Primary
        )
    }
}

/**
 * A [PaneEntry] for selectively running animations in [ThreePane] [MultiPaneDisplay]. When:
 * - A navigation destination moves between the [ThreePane.Primary] and [ThreePane.Secondary]
 *     panes, the pane animations are not run to provide a seamless movement experience.
 * - A navigation destination moves between the [ThreePane.Primary] and
 *     [ThreePane.TransientPrimary] panes, the pane animations are not run.
 *
 * @param enterTransition the transition to run for the entering pane.
 * @param exitTransition the transition to run for the exiting pane.
 * @param paneMapping the mapping of panes to navigation destinations.
 * @param render the Composable for rendering the current destination.
 */
fun <R : Node> threePaneEntry(
    enterTransition: PaneScope<ThreePane, R>.() -> EnterTransition = {
        if (canAnimate()) DefaultFadeIn else EnterTransition.None
    },
    exitTransition: PaneScope<ThreePane, R>.() -> ExitTransition = {
        if (canAnimate()) DefaultFadeOut else ExitTransition.None
    },
    paneMapping: @Composable (R) -> Map<ThreePane, R?> = {
        mapOf(ThreePane.Primary to it)
    },
    render: @Composable (PaneScope<ThreePane, R>.(R) -> Unit),
) = PaneEntry(
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    paneTransform = paneMapping,
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
        inPredictiveBack && isActive -> false
        paneState.adaptations.any { adaptation ->
            adaptation is Adaptation.Pop
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