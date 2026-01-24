package com.tunjid.treenav.compose

/**
 * A description of the process that the layout undertook to adapt to the present
 * pane in its new configuration.
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
     * The current back stack is a sublist of a previously displayed back stack.
     */
    data object Pop : Adaptation()

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
