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

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tunjid.treenav.Node
import kotlin.jvm.JvmInline

/**
 * Scope for navigation destinations that can show up in an arbitrary pane.
 */
@Stable
interface PaneScope<Pane, Destination : Node> : AnimatedVisibilityScope {

    /**
     * Provides information about the adaptive context that created this [PaneScope].
     */
    val paneState: PaneState<Pane, Destination>

    /**
     * Whether or not this [PaneScope] is active in its current pane. It is active when this pane
     * matches the current navigation destination or any of its co-displayed panes in a given
     * scene.
     *
     * This means that during predictive back animations, the outgoing panes, i.e the panes
     * whose [AnimatedVisibilityScope.transition] have their [Transition.targetState]
     * NOT reporting [EnterExitState.Visible] are considered active.
     */
    val isActive: Boolean

    /**
     * Whether or not a predictive back gesture is in progress
     */
    val inPredictiveBack: Boolean
}

/**
 * An implementation of [PaneScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedPaneScope<Pane, Destination : Node>(
    val isPreviewingBack: () -> Boolean,
    val activeState: State<Boolean>,
    paneState: PaneState<Pane, Destination>,
    animatedContentScope: AnimatedContentScope,
) : PaneScope<Pane, Destination>, AnimatedVisibilityScope by animatedContentScope {

    private val isEntering
        get() = transition.targetState == EnterExitState.Visible

    override var paneState by mutableStateOf(paneState)

    override val isActive: Boolean
        get() = when {
            inPredictiveBack -> !activeState.value
            // Transition lagging predictive back for the enter state
            activeState.value && !isEntering -> true
            // Transition lagging predictive back for the exit state
            !activeState.value && isEntering -> false
            // Stabilized, the transition is the source of truth
            else -> activeState.value
        }

    override val inPredictiveBack: Boolean
        get() {
            val currentSize = transition.sceneCurrentDestinationKey?.ids?.size ?: 0
            val targetSize = transition.sceneTargetDestinationKey?.ids?.size ?: 0
            return isPreviewingBack() && (targetSize < currentSize)
        }
}

/**
 * Information about content in a pane
 */
@Stable
sealed interface PaneState<Pane, Destination : Node> {
    val currentDestination: Destination?
    val pane: Pane?
    val adaptations: Set<Adaptation>
}

/**
 * [Slot] based implementation of [PaneState]
 */
internal data class SlotPaneState<Pane, Destination : Node>(
    val slot: Slot?,
    val previousDestination: Destination?,
    override val currentDestination: Destination?,
    override val pane: Pane?,
    override val adaptations: Set<Adaptation>,
) : PaneState<Pane, Destination>

/**
 * A spot taken by an [PaneEntry] that may be moved in from pane to pane.
 */
@JvmInline
internal value class Slot internal constructor(val index: Int)

internal expect fun Any.identityHash(): Int
