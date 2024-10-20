package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.utilities.DefaultBoundsTransform

/**
 * Creates movable shared elements that may be shared amongst different [PaneScope]
 * instances.
 */
interface MovableSharedElementScope {

    /**
     * Creates a movable shared element that accepts a single argument [T] and a [Modifier].
     *
     * NOTE: It is an error to compose the movable shared element in different locations
     * simultaneously, and the behavior of the shared element is undefined in this case.
     *
     * @see [SharedTransitionScope.sharedElement]
     * @see [SharedTransitionScope.renderInSharedTransitionScopeOverlay]
     *
     * @param key the shared element key to identify the movable shared element.
     * @param zIndexInOverlay the elevation of the movable shared element relative to other
     * shared elements.
     * @param clipInOverlayDuringTransition supports a custom clip path if clipping is desired.
     * @param sharedElement a factory function to create the shared element if it does not
     * currently exist.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? =
            DefaultClipInOverlayDuringTransition,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

/**
 * State for managing movable shared elements within a single [PanedNavHost].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class MovableSharedElementHostState<Pane, Destination : Node>(
    private val sharedTransitionScope: SharedTransitionScope,
    private val canAnimateOnStartingFrames: (PaneState<Pane, Destination>) -> Boolean,
) {

    private val keysToMovableSharedElements =
        mutableStateMapOf<Any, MovableSharedElementState<*, Pane, Destination>>()

    /**
     * Returns true is a given shared element under a given key is currently being shared.
     */
    fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    /**
     * Returns true if a movable shared element is animating.
     */
    fun isInProgress(key: Any): Boolean =
        keysToMovableSharedElements[key]?.animInProgress == true

    /**
     * Provides a movable shared element that can be rendered in a given [PaneScope].
     * It is the callers responsibility to perform other verifications on the ability
     * of the calling [PaneScope] to render the movable shared element.
     */
    fun <S> PaneScope<Pane, Destination>.createOrUpdateSharedElement(
        key: Any,
        boundsTransform: BoundsTransform,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path?,
        sharedElement: @Composable (S, Modifier) -> Unit,
    ): @Composable (S, Modifier) -> Unit {
        val movableSharedElementState = keysToMovableSharedElements.getOrPut(key) {
            MovableSharedElementState(
                paneScope = this,
                sharedTransitionScope = sharedTransitionScope,
                sharedElement = sharedElement,
                boundsTransform = boundsTransform,
                canAnimateOnStartingFrames = canAnimateOnStartingFrames,
                onRemoved = { keysToMovableSharedElements.remove(key) }
            )
        }.also {
            it.paneScope = this
            it.zIndexInOverlay = zIndexInOverlay
            it.clipInOverlayDuringTransition = clipInOverlayDuringTransition
        }

        // Can't really guarantee that the caller will use the same key for the right type
        return movableSharedElementState.moveableSharedElement
    }
}

/**
 * An implementation of [MovableSharedElementScope] that ensures shared elements are only rendered
 * in an [PaneScope] when it is active.
 *
 * Other implementations of [MovableSharedElementScope] may delegate to this for their own
 * movable shared element implementations.
 */
@Stable
internal class AdaptiveMovableSharedElementScope<T, R : Node>(
    paneScope: PaneScope<T, R>,
    private val movableSharedElementHostState: MovableSharedElementHostState<T, R>,
) : MovableSharedElementScope {

    var paneScope by mutableStateOf(paneScope)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path?,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit = when {
        paneScope.isActive -> with(movableSharedElementHostState) {
            paneScope.createOrUpdateSharedElement(
                key = key,
                boundsTransform = boundsTransform,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                sharedElement = sharedElement,
            )
        }
        // This pane state is be transitioning out. Check if it should be displayed without
        // shared element semantics.
        else -> when {
            movableSharedElementHostState.isCurrentlyShared(key) ->
                // The element is being shared in its new destination, stop showing it
                // in the in active one
                if (movableSharedElementHostState.isInProgress(key)) EmptyElement
                // The element is not being shared in its new destination, allow it run its exit
                // transition
                else sharedElement
            // Element isn't being shared anymore, show the element as is without sharing.
            else -> sharedElement
        }
    }
}

private val EmptyElement: @Composable (Any?, Modifier) -> Unit = { _, _ -> }

private val DefaultClipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? =
    { _, _ -> null }