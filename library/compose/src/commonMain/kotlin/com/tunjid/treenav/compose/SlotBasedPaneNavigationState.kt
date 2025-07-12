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

interface PaneNavigationState<Pane, Destination : Node> {

    /**
     * The id of the [Destination] that produced this [PaneNavigationState].
     */
    val destinationId: String

    /**
     * Provides the set of adaptations in the provided [Pane].
     */
    fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation>

    /**
     * Returns the [Destination] in the provided [Pane].
     */
    fun destinationIn(
        pane: Pane,
    ): Destination?
}

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
@Immutable
internal data class SlotBasedPaneNavigationState<Pane, Destination : Node>(
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
) : PaneNavigationState<Pane, Destination> {
    companion object {
        internal fun <Pane, Destination : Node> initial(
            slots: Collection<Slot>,
        ): SlotBasedPaneNavigationState<Pane, Destination> = SlotBasedPaneNavigationState(
            isPop = false,
            swapAdaptations = emptySet(),
            panesToDestinations = emptyMap(),
            destinationIdsToAdaptiveSlots = slots.associateBy(
                keySelector = Slot::toString
            ),
            backStackIds = emptyList(),
            previousPanesToDestinations = emptyMap(),
        )
    }

    override val destinationId: String
        get() = backStackIds.last()

    internal fun paneStateFor(
        slot: Slot,
    ): PaneState<Pane, Destination> {
        val node = destinationIn(slot)
        val pane = node?.let(::paneFor)
        return SlotPaneState(
            slot = slot,
            currentDestination = node,
            previousDestination = previousPanesToDestinations[pane],
            pane = pane,
            adaptations = pane?.let(::adaptationsIn) ?: emptySet(),
        )
    }

    internal fun slotFor(
        pane: Pane,
    ): Slot? = destinationIdsToAdaptiveSlots[
        panesToDestinations[pane]?.id
    ]

    internal fun paneFor(
        destination: Node,
    ): Pane? = panesToDestinations.firstNotNullOfOrNull { (pane, paneDestination) ->
        if (paneDestination?.id == destination.id) pane else null
    }

    private fun destinationIn(
        slot: Slot,
    ): Destination? = destinationIdsToAdaptiveSlots.firstNotNullOfOrNull { (nodeId, nodeSlot) ->
        if (nodeSlot == slot) panesToDestinations.firstNotNullOfOrNull { (_, node) ->
            if (node?.id == nodeId) node
            else null
        }
        else null
    }

    override fun destinationIn(
        pane: Pane,
    ): Destination? = panesToDestinations[pane]

    override fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation> {
        val adaptations = when {
            swapAdaptations.any { pane in it } -> swapAdaptations.filterTo(mutableSetOf()) {
                pane in it
            }

            else -> when (panesToDestinations[pane]?.id) {
                previousPanesToDestinations[pane]?.id -> SameAdaptations
                else -> ChangeAdaptations
            }
        }
        return if (isPop) adaptations + Adaptation.Pop else adaptations
    }
}

private val SameAdaptations = setOf(Adaptation.Same)
private val ChangeAdaptations = setOf(Adaptation.Change)

/**
 * A method that adapts changes in navigation to different panes while allowing for them
 * to be animated easily.
 */
internal fun <Pane, Destination : Node> SlotBasedPaneNavigationState<Pane, Destination>.adaptTo(
    slots: Set<Slot>,
    panesToDestinations: Map<Pane, Destination?>,
    backStackIds: List<String>,
): SlotBasedPaneNavigationState<Pane, Destination> {
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
    val swapAdaptations = mutableSetOf<Adaptation.Swap<Pane>>()

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

    return SlotBasedPaneNavigationState(
        isPop = backStackIds.let popCheck@{ ids ->
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
            valueSelector = previous::destinationIn
        ),
        destinationIdsToAdaptiveSlots = nodeIdsToAdaptiveSlots,
        backStackIds = backStackIds,
        panesToDestinations = panesToDestinations,
    )

}