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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
     * Whether or not this [PaneScope] is active in its current pane. It is inactive when
     * it is animating out of its [AnimatedVisibilityScope].
     */
    val isActive: Boolean

    /**
     * Describes how a destination transitions after an adaptive change
     */
    data class Transitions(
        val enter: EnterTransition,
        val exit: ExitTransition,
    )
}

/**
 * An implementation of [PaneScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedPaneScope<Pane, Destination : Node>(
    paneState: PaneState<Pane, Destination>,
    activeState: State<Boolean>,
    val animatedContentScope: AnimatedContentScope
) : PaneScope<Pane, Destination>, AnimatedVisibilityScope by animatedContentScope {

    override var paneState by mutableStateOf(paneState)

    override val isActive: Boolean by activeState
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
 * A spot taken by an [PaneStrategy] that may be moved in from pane to pane.
 */
@JvmInline
internal value class Slot internal constructor(val index: Int)
