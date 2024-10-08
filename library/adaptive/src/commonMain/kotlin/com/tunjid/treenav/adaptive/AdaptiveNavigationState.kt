package com.tunjid.treenav.adaptive

import com.tunjid.treenav.Node

/**
 * State providing details about data in each pane [Pane] it hosts.
 */
interface AdaptiveNavigationState<Pane, Destination : Node> {

    /**
     * The current [Destination] in this [pane].
     * @param pane the [Pane] to query.
     */
    fun destinationFor(
        pane: Pane,
    ): Destination?

    /**
     * Adaptations involving this [pane] after the last navigation state change.
     * @param pane the affected [Pane].
     */
    fun adaptationsIn(
        pane: Pane,
    ): Set<Adaptation>
}

/**
 * A description of the process that the layout undertook to adapt to its new configuration.
 */
sealed class Adaptation {

    /**
     * Destinations remained the same in the pane
     */
    data object Same : Adaptation()

    /**
     * Destinations were changed in the pane
     */
    data object Change : Adaptation()

    /**
     * Destinations were swapped in between panes
     */
    data class Swap<Pane>(
        val from: Pane,
        val to: Pane?,
    ) : Adaptation()

    /**
     * Checks if a [Swap] [Adaptation] involved [pane].
     */
    operator fun <Pane> Swap<Pane>.contains(pane: Pane?): Boolean = pane == from || pane == to

}
