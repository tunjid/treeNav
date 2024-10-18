package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.utilities.DefaultBoundsTransform

internal interface SharedElementOverlay {
    fun ContentDrawScope.drawInOverlay()
}

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
     * @param key the shared element key to identify the movable shared element.
     * @param sharedElement a factory function to create the shared element if it does not
     * currently exist.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
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

    // TODO: This should be unnecessary. Figure out a way to participate arbitrarily in the
    //  overlays already implemented in [SharedTransitionScope].
    /**
     * A [Modifier] for drawing the movable shared element in overlays over existing content.
     */
    val modifier = Modifier.drawWithContent {
        drawContent()
        overlays.forEach { overlay ->
            with(overlay) {
                drawInOverlay()
            }
        }
    }

    private val overlays: Collection<SharedElementOverlay>
        get() = keysToMovableSharedElements.values

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
        }.also { it.paneScope = this }

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
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit = when {
        paneScope.isActive -> with(movableSharedElementHostState) {
            paneScope.createOrUpdateSharedElement(
                key = key,
                boundsTransform = boundsTransform,
                sharedElement = sharedElement
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