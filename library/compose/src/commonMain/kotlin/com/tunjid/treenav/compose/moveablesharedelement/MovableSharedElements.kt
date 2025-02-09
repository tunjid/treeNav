package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
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
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.utilities.DefaultBoundsTransform

/**
 * Creates movable shared elements that may be shared amongst different [PaneScope]
 * instances.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
interface MovableSharedElementScope : SharedTransitionScope {

    /**
     * Creates a movable shared element that accepts a single argument [T] and a [Modifier].
     *
     * NOTE: It is an error to compose the movable shared element in different locations
     * simultaneously, and the behavior of the shared element is undefined in this case.
     *
     * @param key The shared element key to identify the movable shared element.
     * @param boundsTransform Allows for customizing the animation for the bounds of
     * the [sharedElement].
     * @param placeHolderSize Allows for adjusting the reported size to the parent layout during
     * the transition.
     * @param renderInOverlayDuringTransition Is true by default. In some rare use cases, there may
     * be no clipping or layer transform (fade, scale, etc) in the application that prevents
     * shared elements from transitioning from one bounds to another without any clipping or
     * sudden alpha change. In such cases, [renderInOverlayDuringTransition] could be specified
     * to false.
     * @param zIndexInOverlay Can be specified to allow shared elements to render in a
     * different order than their placement/zOrder when not in the overlay.
     * @param clipInOverlayDuringTransition Can be used to specify the clipping for when the
     * shared element is going through an active transition towards a new target bounds.
     * @param alternateOutgoingSharedElement By default, a separate instance of the
     * [sharedElement] is rendered when content is being animated out. When specified, this
     * is rendered instead. This is useful for shared elements that can only be reasonable
     * rendered in one place at any one time like video.
     * @param sharedElement A factory function to create the movable shared element if it does not
     * currently exist.
     *
     * @see [SharedTransitionScope.sharedElementWithCallerManagedVisibility]
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

/**
 * Convenience method for [MovableSharedElementScope.movableSharedElementOf] that invokes the
 * movable shared element with the latest values of [state] and [modifier].
 *
 * @see [MovableSharedElementScope.movableSharedElementOf].
 *
 * @param key The shared element key to identify the movable shared element.
 * @param boundsTransform Allows for customizing the animation for the bounds of
 * the [sharedElement].
 * @param placeHolderSize Allows for adjusting the reported size to the parent layout during
 * the transition.
 * @param renderInOverlayDuringTransition Is true by default. In some rare use cases, there may
 * be no clipping or layer transform (fade, scale, etc) in the application that prevents
 * shared elements from transitioning from one bounds to another without any clipping or
 * sudden alpha change. In such cases, [renderInOverlayDuringTransition] could be specified
 * to false.
 * @param zIndexInOverlay Can be specified to allow shared elements to render in a
 * different order than their placement/zOrder when not in the overlay.
 * @param clipInOverlayDuringTransition Can be used to specify the clipping for when the
 * shared element is going through an active transition towards a new target bounds.
 * @param alternateOutgoingSharedElement By default, a separate instance of the
 * [sharedElement] is rendered when content is being animated out. When specified, this
 * is rendered instead. This is useful for shared elements that can only be reasonable
 * rendered in one place at any one time like video.
 * @param sharedElement A factory function to create the movable shared element if it does not
 * currently exist.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T> MovableSharedElementScope.updatedMovableSharedElementOf(
    key: Any,
    state: T,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip,
    alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)? = null,
    sharedElement: @Composable (T, Modifier) -> Unit
) = movableSharedElementOf(
    key = key,
    boundsTransform = boundsTransform,
    placeHolderSize = placeHolderSize,
    renderInOverlayDuringTransition = renderInOverlayDuringTransition,
    zIndexInOverlay = zIndexInOverlay,
    clipInOverlayDuringTransition = clipInOverlayDuringTransition,
    alternateOutgoingSharedElement = alternateOutgoingSharedElement,
    sharedElement = sharedElement
).invoke(
    state,
    modifier,
)

/**
 * State for managing movable shared elements within a single [MultiPaneDisplay].
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
        }.also { it.sharedContentState = sharedContentState }

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
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class PanedMovableSharedElementScope<T, R : Node>(
    paneScope: PaneScope<T, R>,
    private val movableSharedElementHostState: MovableSharedElementHostState<T, R>,
) : MovableSharedElementScope, SharedTransitionScope by movableSharedElementHostState {

    var paneScope by mutableStateOf(paneScope)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?,
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
                        placeHolderSize = placeHolderSize,
                        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
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
                        // The element is being shared in its new destination, stop showing it
                        // in the in active one
                        movableSharedElementHostState.isCurrentlyShared(key)
                                && movableSharedElementHostState.isMatchFound(key) -> EmptyElement(
                            state,
                            Modifier.matchParentSize()
                        )
                        // The element is not being shared in its new destination, allow it run its exit
                        // transition
                        else -> (alternateOutgoingSharedElement ?: sharedElement)(
                            state,
                            Modifier.matchParentSize()
                        )
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