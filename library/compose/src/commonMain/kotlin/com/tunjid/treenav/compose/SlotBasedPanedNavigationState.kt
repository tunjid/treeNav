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

import androidx.compose.runtime.Immutable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.Adaptation.Change.contains

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
@Immutable
internal data class SlotBasedPanedNavigationState<Pane, Destination : Node>(
    /**
     * True if this navigation change is as a result of popping the backStack.
     */
    val isPop: Boolean,
    /**
     * Moves between panes within a navigation sequence.
     */
    val swapAdaptations: Set<Adaptation.Swap<Pane>>,
    /**
     * A mapping of [Pane] to the nodes in them
     */
    val panesToDestinations: Map<Pane, Destination?>,
    /**
     * A mapping of adaptive pane to the nodes that were last in them.
     */
    val previousPanesToDestinations: Map<Pane, Destination?>,
    /**
     * A mapping of node ids to the adaptive slots they are currently in.
     */
    val destinationIdsToAdaptiveSlots: Map<String?, Slot>,
    /**
     * A set of node ids that may be returned to.
     */
    val backStackIds: List<String>,
    /**
     * A set of node ids that are animating out.
     */
    val destinationIdsAnimatingOut: Set<String>,
) {
    companion object {
        internal fun <T, R : Node> initial(
            slots: Collection<Slot>,
        ): SlotBasedPanedNavigationState<T, R> = SlotBasedPanedNavigationState(
            isPop = false,
            swapAdaptations = emptySet(),
            panesToDestinations = emptyMap(),
            destinationIdsToAdaptiveSlots = slots.associateBy(
                keySelector = Slot::toString
            ),
            backStackIds = emptyList(),
            destinationIdsAnimatingOut = emptySet(),
            previousPanesToDestinations = emptyMap(),
        )
    }

    internal inline fun <T> withPaneAndDestination(
        slot: Slot,
        crossinline block:
        SlotBasedPanedNavigationState<Pane, Destination>.(pane: Pane?, destination: Destination?) -> T
    ): T {
        val node = destinationFor(slot)
        val pane = node?.let(::paneFor)
        return block(pane, node)
    }

    internal fun slotFor(
        pane: Pane,
    ): Slot? = destinationIdsToAdaptiveSlots[
        panesToDestinations[pane]?.id
    ]

    private fun paneFor(
        node: Node,
    ): Pane? = panesToDestinations.firstNotNullOfOrNull { (pane, paneRoute) ->
        if (paneRoute?.id == node.id) pane else null
    }

    private fun destinationFor(
        slot: Slot,
    ): Destination? = destinationIdsToAdaptiveSlots.firstNotNullOfOrNull { (nodeId, nodeSlot) ->
        if (nodeSlot == slot) panesToDestinations.firstNotNullOfOrNull { (_, node) ->
            if (node?.id == nodeId) node
            else null
        }
        else null
    }

    fun destinationFor(
        pane: Pane,
    ): Destination? = panesToDestinations[pane]

    fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation> {
        val swaps = swapAdaptations.filter { pane in it }
        val adaptations = if (swaps.isEmpty()) when (panesToDestinations[pane]?.id) {
            previousPanesToDestinations[pane]?.id -> SameAdaptations
            else -> ChangeAdaptations
        }
        else swaps.toSet()
        return if (isPop) adaptations + Adaptation.Pop else adaptations
    }
}

private val SameAdaptations = setOf(Adaptation.Same)
private val ChangeAdaptations = setOf(Adaptation.Change)


/**
 * A method that adapts changes in navigation to different panes while allowing for them
 * to be animated easily.
 */
internal fun <T, R : Node> SlotBasedPanedNavigationState<T, R>.adaptTo(
    slots: Set<Slot>,
    panesToDestinations: Map<T, R?>,
    backStackIds: List<String>,
): SlotBasedPanedNavigationState<T, R> {
    val previous = this

    val previouslyUsedSlots = previous.destinationIdsToAdaptiveSlots
        .filter { it.key != null }
        .values
        .toSet()

    // Sort by most recently used to makes sure most recently used slots
    // are reused so animations run.
    val availableSlots = slots
        .sortedByDescending(previouslyUsedSlots::contains)
        .toMutableSet()

    val unplacedNodeIds = panesToDestinations.values.mapNotNull { it?.id }.toMutableSet()

    val nodeIdsToAdaptiveSlots = mutableMapOf<String?, Slot>()
    val swapAdaptations = mutableSetOf<Adaptation.Swap<T>>()

    // Process nodes that swapped panes from old to new
    for ((toPane, toNode) in panesToDestinations.entries) {
        if (toNode == null) continue
        for ((fromPane, fromNode) in previous.panesToDestinations.entries) {
            // Find a previous node from the last state
            if (toNode.id != fromNode?.id) continue
            val swap = Adaptation.Swap(
                from = fromPane,
                to = toPane
            )
            // The panes are different, a swap occurred
            if (toPane != fromPane) {
                swapAdaptations.add(swap)
            }

            // Since this node was swapped, preserve its existing slot
            val fromNodeId = checkNotNull(fromNode.id)
            check(unplacedNodeIds.remove(fromNodeId)) {
                "A swap cannot have occurred if the node did not exist in the previous state"
            }
            val reusedSlot = previous.destinationIdsToAdaptiveSlots.getValue(fromNodeId)
            check(availableSlots.remove(reusedSlot)) {
                "A swap cannot have occurred if the node did not exist in the previous state"
            }
            nodeIdsToAdaptiveSlots[fromNodeId] = reusedSlot
            break
        }
    }

    // All swaps have been processed; place remaining changes nodes in slots available.
    unplacedNodeIds.forEach { nodeId ->
        nodeIdsToAdaptiveSlots[nodeId] = availableSlots.first().also(availableSlots::remove)
    }

    return SlotBasedPanedNavigationState(
        backStackIds.let popCheck@{ ids ->
            if (ids.size >= previous.backStackIds.size) return@popCheck false
            if (ids.isEmpty()) return@popCheck true

            for (index in ids.indices) {
                if (ids[index] != previous.backStackIds[index]) return@popCheck false
            }
            true
        },
        // If the values of the nodes to panes are the same, no swaps occurred.
        swapAdaptations = when (previous.panesToDestinations.mapValues { it.value?.id }) {
            panesToDestinations.mapValues { it.value?.id } -> previous.swapAdaptations
            else -> swapAdaptations
        },
        previousPanesToDestinations = previous.panesToDestinations.keys.associateWith(
            valueSelector = previous::destinationFor
        ),
        destinationIdsToAdaptiveSlots = nodeIdsToAdaptiveSlots,
        backStackIds = backStackIds,
        panesToDestinations = panesToDestinations,
        destinationIdsAnimatingOut = previous.destinationIdsAnimatingOut,
    )

}