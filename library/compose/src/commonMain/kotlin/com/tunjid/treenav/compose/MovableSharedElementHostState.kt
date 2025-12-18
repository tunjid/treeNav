package com.tunjid.treenav.compose

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

/**
 * State for managing movable shared elements within a single [MultiPaneDisplay].
 */

@Stable
class MovableSharedElementHostState<Pane, Destination : Node>(
    val sharedTransitionScope: SharedTransitionScope,
) : SharedTransitionScope by sharedTransitionScope {

    private val keysToMovableSharedElements =
        mutableStateMapOf<Any, MovableSharedElementState<*>>()

    /**
     * Returns true is a given shared element under a given key is currently being shared.
     */
    private fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    /**
     * Returns true if a movable shared element has its shared element match found.
     *
     * @see [SharedContentState.isMatchFound]
     */
    private fun isMatchFound(key: Any): Boolean =
        keysToMovableSharedElements[key]?.sharedContentState?.isMatchFound == true

    @Composable
    fun <T> MinConstraintBoxScope.MovableSharedElement(
        sharedContentState: SharedContentState,
        state: T,
        useMovableContent: () -> Boolean,
        sharedElement: @Composable (T, Modifier) -> Unit,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?
    ) = when {
        useMovableContent() ->
            keysToMovableSharedElements.getOrPut(sharedContentState.key) {
                MovableSharedElementState(
                    sharedContentState = sharedContentState,
                    sharedElement = sharedElement,
                    onRemoved = { keysToMovableSharedElements.remove(sharedContentState.key) }
                )
            }
                .also { it.sharedContentState = sharedContentState }
                .moveableSharedElement(
                    state,
                    Modifier.fillParentAxisIfFixedOrWrap(),
                )

        // This pane state is be transitioning out. Check if it should be displayed without
        // shared element semantics.
        else -> when {
            // The element is being shared in its new destination, stop showing it
            // in the in active one
            isCurrentlyShared(sharedContentState.key)
                    && isMatchFound(sharedContentState.key) -> Defaults.EmptyElement(
                state,
                Modifier.fillParentAxisIfFixedOrWrap()
            )
            // The element is not being shared in its new destination, allow it run its exit
            // transition
            else -> (alternateOutgoingSharedElement ?: sharedElement)(
                state,
                Modifier.fillParentAxisIfFixedOrWrap()
            )
        }
    }
}
