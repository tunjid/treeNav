package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.utilities.DefaultBoundsTransform

/**
 * Creates movable shared elements that may be shared amongst different [PaneScope]
 * instances.
 */
@Stable
interface MovableSharedElementScope {

    /**
     * Creates a movable shared element that accepts a single argument [T] and a [Modifier].
     *
     * NOTE: It is an error to compose the movable shared element in different locations
     * simultaneously, and the behavior of the shared element is undefined in this case.
     *
     * @param key the shared element key to identify the movable shared element.
     * @param sharedElement a factory function to create the shared element if it does not
     * currently exist.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

/**
 * Convenience method for [MovableSharedElementScope.movableSharedElementOf] that invokes the
 * movable shared element with the latest values of [state] and [modifier].
 *
 * @see [MovableSharedElementScope.movableSharedElementOf].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T> MovableSharedElementScope.updatedMovableSharedElementOf(
    key: Any,
    state: T,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip,
    sharedElement: @Composable (T, Modifier) -> Unit
) = movableSharedElementOf(
    key = key,
    boundsTransform = boundsTransform,
    zIndexInOverlay = zIndexInOverlay,
    clipInOverlayDuringTransition = clipInOverlayDuringTransition,
    sharedElement = sharedElement
).invoke(
    state,
    modifier,
)

/**
 * State for managing movable shared elements within a single [PanedNavHost].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class MovableSharedElementHostState<Pane, Destination : Node>(
    private val sharedTransitionScope: SharedTransitionScope,
) : SharedTransitionScope by sharedTransitionScope {

    private val keysToMovableSharedElements =
        mutableStateMapOf<Any, MovableSharedElementState<*>>()

    /**
     * Returns true is a given shared element under a given key is currently being shared.
     */
    fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    /**
     * Returns true if a movable shared element has its shared element match found.
     *
     * @see [SharedContentState.isMatchFound]
     */
    fun isMatchFound(key: Any): Boolean =
        keysToMovableSharedElements[key]?.sharedContentState?.isMatchFound == true

    /**
     * Provides a movable shared element that can be rendered in a given [PaneScope].
     * It is the callers responsibility to perform other verifications on the ability
     * of the calling [PaneScope] to render the movable shared element.
     */
    @Suppress("UnusedReceiverParameter")
    fun <S> MovableSharedElementScope.createOrUpdateSharedElement(
        key: Any,
        sharedContentState: SharedContentState,
        sharedElement: @Composable (S, Modifier) -> Unit,
    ): @Composable (S, Modifier) -> Unit {
        val movableSharedElementState = keysToMovableSharedElements.getOrPut(key) {
            MovableSharedElementState(
                sharedContentState = sharedContentState,
                sharedElement = sharedElement,
                onRemoved = { keysToMovableSharedElements.remove(key) }
            )
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
class PanedMovableSharedElementScope<T, R : Node>(
    paneScope: PaneScope<T, R>,
    private val movableSharedElementHostState: MovableSharedElementHostState<T, R>,
) : MovableSharedElementScope {

    var paneScope by mutableStateOf(paneScope)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit = { state, modifier ->
        with(movableSharedElementHostState) {
            val sharedContentState = rememberSharedContentState(key)
            Box(
                modifier
                    .sharedElementWithCallerManagedVisibility(
                        sharedContentState = sharedContentState,
                        visible = paneScope.isActive,
                        boundsTransform = boundsTransform,
                        zIndexInOverlay = zIndexInOverlay,
                        clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                    )
            ) {
                when {
                    paneScope.isActive ->
                        createOrUpdateSharedElement(
                            key = key,
                            sharedContentState = sharedContentState,
                            sharedElement = sharedElement
                        )(state, Modifier.matchParentSize())

                    // This pane state is be transitioning out. Check if it should be displayed without
                    // shared element semantics.
                    else -> when {
                        movableSharedElementHostState.isCurrentlyShared(key) ->
                            // The element is being shared in its new destination, stop showing it
                            // in the in active one
                            if (movableSharedElementHostState.isMatchFound(key)) EmptyElement(
                                state,
                                Modifier.matchParentSize()
                            )
                            // The element is not being shared in its new destination, allow it run its exit
                            // transition
                            else sharedElement(state, Modifier.matchParentSize())
                        // Element isn't being shared anymore, show the element as is without sharing.
                        else -> sharedElement(state, Modifier.matchParentSize())
                    }
                }
            }
        }
    }
}

private val EmptyElement: @Composable (Any?, Modifier) -> Unit = { _, _ -> }

@ExperimentalSharedTransitionApi
private val ParentClip: OverlayClip =
    object : OverlayClip {
        override fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return state.parentSharedContentState?.clipPathInOverlay
        }
    }