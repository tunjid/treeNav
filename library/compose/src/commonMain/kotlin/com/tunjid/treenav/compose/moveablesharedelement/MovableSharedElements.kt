package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.Defaults
import com.tunjid.treenav.compose.PaneScope

/**
 * Creates movable shared elements that may be shared amongst different [PaneScope]
 * instances.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
interface MovableSharedElementScope {

    /**
     * The backing [SharedTransitionScope] for movable shared elements.
     */
    val sharedTransitionScope: SharedTransitionScope

    /**
     * Creates a movable shared element that accepts a single argument [T] and a [Modifier].
     *
     * NOTE: It is an error to compose the movable shared element in different locations
     * simultaneously, and the behavior of the shared element is undefined in this case.
     *
     * @param sharedContentState The shared element key to identify the movable shared element.
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
     * @see [SharedTransitionScope.sharedElement]
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun <T> movableSharedElementOf(
        sharedContentState: SharedContentState,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit

    /**
     * Creates a movable shared element that accepts a single argument [T] and a [Modifier].
     *
     * This method is similar to [movableSharedElementOf], with the exception that the shared
     * element transition is not seekable. Instead, the element will "stick" to where it is
     * while the transition is seeking, until it begins animating to its target state.
     *
     * NOTE: It is an error to compose the movable shared element in different locations
     * simultaneously, and the behavior of the shared element is undefined in this case.
     *
     * @param sharedContentState The shared element key to identify the movable shared element.
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
     * @see [SharedTransitionScope.sharedElement]
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun <T> movableStickySharedElementOf(
        sharedContentState: SharedContentState,
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
 * @param sharedContentState The shared element key to identify the movable shared element.
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
fun <T> MovableSharedElementScope.UpdatedMovableSharedElementOf(
    sharedContentState: SharedContentState,
    state: T,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = Defaults.DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
    alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)? = null,
    sharedElement: @Composable (T, Modifier) -> Unit
) = movableSharedElementOf(
    sharedContentState = sharedContentState,
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
 * Convenience method for [MovableSharedElementScope.UpdatedMovableStickySharedElementOf] that
 * invokes the movable shared element with the latest values of [state] and [modifier].
 *
 * @see [MovableSharedElementScope.movableStickySharedElementOf].
 *
 * @param sharedContentState The shared element key to identify the movable shared element.
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
fun <T> MovableSharedElementScope.UpdatedMovableStickySharedElementOf(
    sharedContentState: SharedContentState,
    state: T,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = Defaults.DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = Defaults.ParentClip,
    alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)? = null,
    sharedElement: @Composable (T, Modifier) -> Unit
) = movableStickySharedElementOf(
    sharedContentState = sharedContentState,
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

@Composable
fun <Pane, Destination : Node> PaneScope<Pane, Destination>.rememberPaneMovableSharedElementScope(
    movableSharedElementHostState: MovableSharedElementHostState<Pane, Destination>
): PaneMovableSharedElementScope<Pane, Destination> {
    val updatedPaneScope = rememberUpdatedState(this)
    return remember(movableSharedElementHostState) {
        PaneMovableSharedElementScope(
            currentPaneScope = updatedPaneScope::value,
            movableSharedElementHostState = movableSharedElementHostState
        )
    }
}

/**
 * An implementation of [MovableSharedElementScope] that ensures shared elements are only rendered
 * in a [PaneScope] when [PaneScope.isActive] is true.
 *
 * Other implementations of [MovableSharedElementScope] may delegate to this for their own
 * movable shared element implementations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class PaneMovableSharedElementScope<Pane, Destination : Node> internal constructor(
    private val currentPaneScope: () -> PaneScope<Pane, Destination>,
    private val movableSharedElementHostState: MovableSharedElementHostState<Pane, Destination>,
) : MovableSharedElementScope {

    override val sharedTransitionScope: SharedTransitionScope
        get() = movableSharedElementHostState

    val paneScope get() = currentPaneScope()

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableSharedElementOf(
        sharedContentState: SharedContentState,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit = { state, modifier ->
        with(movableSharedElementHostState) {
            SharedElement(
                modifier = modifier,
                sharedContentState = sharedContentState,
                animatedVisibilityScope = paneScope,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                content = {
                    MovableSharedElement(
                        sharedContentState = sharedContentState,
                        state = state,
                        useMovableContent = {
                            paneScope.transition.targetState == EnterExitState.Visible
                        },
                        sharedElement = sharedElement,
                        alternateOutgoingSharedElement = alternateOutgoingSharedElement
                    )
                }
            )
        }
    }


    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun <T> movableStickySharedElementOf(
        sharedContentState: SharedContentState,
        boundsTransform: BoundsTransform,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        alternateOutgoingSharedElement: (@Composable (T, Modifier) -> Unit)?,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit = with(movableSharedElementHostState) {
        { state, modifier ->
            SharedElementWithCallerManagedVisibility(
                modifier = modifier,
                sharedContentState = sharedContentState,
                placeHolderSize = placeHolderSize,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                zIndexInOverlay = zIndexInOverlay,
                clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                isVisible = { paneScope.isActive },
                content = {
                    MovableSharedElement(
                        sharedContentState = sharedContentState,
                        state = state,
                        useMovableContent = { paneScope.isActive },
                        alternateOutgoingSharedElement = alternateOutgoingSharedElement,
                        sharedElement = sharedElement
                    )
                },
            )
        }
    }
}
