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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
     * Whether or not this [PaneScope] is active in its current pane. It is active when
     * the current navigation destination is rendering this pane.
     *
     * This means that during predictive back animations, the outgoing panes, i.e the panes
     * whose [AnimatedVisibilityScope.transition] report [EnterExitState.Visible] are considered
     * active.
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
internal class AnimatedPaneScope<Pane, Destination : Node> private constructor(
    private val slotPaneState: SlotPaneState<Pane, Destination>,
    val isPreviewingBack: () -> Boolean,
    val animatedContentScope: AnimatedContentScope
) : PaneScope<Pane, Destination>, AnimatedVisibilityScope by animatedContentScope {

    override val paneState: PaneState<Pane, Destination>
        get() = slotPaneState

    override val isActive: Boolean by derivedStateOf {
        val isEntering = animatedContentScope.transition.targetState == EnterExitState.Visible
        if (inPredictiveBack) !isEntering
        else isEntering
    }

    override val inPredictiveBack: Boolean
        get() = isPreviewingBack()

    companion object {
        /**
         * [Slot] based implementation of [PaneState]
         */
        @Stable
        private class SlotPaneState<Pane, Destination : Node>(
            panedNavigationStateHash: Int,
            slot: Slot?,
            previousDestination: Destination?,
            currentDestination: Destination?,
            pane: Pane?,
            adaptations: Set<Adaptation>,
        ) : PaneState<Pane, Destination> {
            var slot: Slot? by mutableStateOf(slot)
            val previousDestination: Destination? by mutableStateOf(previousDestination)

            override val currentDestination: Destination? by mutableStateOf(currentDestination)
            override var pane: Pane? by mutableStateOf(pane)
            override var adaptations: Set<Adaptation> by mutableStateOf(adaptations)

            var lastPanedNavigationStateHash by mutableIntStateOf(panedNavigationStateHash)
        }

        fun <Pane, Destination : Node> SlotBasedPanedNavigationState<Pane, Destination>.paneScope(
            slot: Slot,
            isPreviewingBack: () -> Boolean,
            animatedContentScope: AnimatedContentScope
        ) = withPaneAndDestination(slot) { pane, destination ->
            AnimatedPaneScope(
                slotPaneState = SlotPaneState(
                    panedNavigationStateHash = this@paneScope.identityHash(),
                    slot = slot,
                    currentDestination = destination,
                    previousDestination = previousPanesToDestinations[pane],
                    pane = pane,
                    adaptations = pane?.let(::adaptationsIn) ?: emptySet(),
                ),
                isPreviewingBack = isPreviewingBack,
                animatedContentScope = animatedContentScope
            )
        }

        fun <Pane, Destination : Node> SlotBasedPanedNavigationState<Pane, Destination>.update(
            animatedPaneScope: AnimatedPaneScope<Pane, Destination>,
            slot: Slot,
        ) {
            withPaneAndDestination(slot) { pane, _ ->
                val state = animatedPaneScope.slotPaneState
                val panedNavigationStateHash = this@update.identityHash()

                if (state.slot == slot
                    && state.pane == pane
                    && state.lastPanedNavigationStateHash == panedNavigationStateHash
                ) return@withPaneAndDestination

                Snapshot.withMutableSnapshot {
                    state.slot = slot
                    state.pane = pane
                    state.adaptations = pane?.let(::adaptationsIn) ?: emptySet()
                    state.lastPanedNavigationStateHash = panedNavigationStateHash
                }
            }
        }
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
 * A spot taken by an [PaneEntry] that may be moved in from pane to pane.
 */
@JvmInline
internal value class Slot internal constructor(val index: Int)

internal expect fun Any.identityHash(): Int
