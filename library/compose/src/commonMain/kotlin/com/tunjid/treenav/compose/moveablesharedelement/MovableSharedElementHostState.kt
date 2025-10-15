package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.Defaults
import com.tunjid.treenav.compose.MultiPaneDisplay

/**
 * State for managing movable shared elements within a single [MultiPaneDisplay].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
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
    fun <T> MovableSharedElement(
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
                    Modifier.fillSharedElement(),
                )

        // This pane state is be transitioning out. Check if it should be displayed without
        // shared element semantics.
        else -> when {
            // The element is being shared in its new destination, stop showing it
            // in the in active one
            isCurrentlyShared(sharedContentState.key)
                    && isMatchFound(sharedContentState.key) -> Defaults.EmptyElement(
                state,
                Modifier.fillSharedElement()
            )
            // The element is not being shared in its new destination, allow it run its exit
            // transition
            else -> (alternateOutgoingSharedElement ?: sharedElement)(
                state,
                Modifier.fillSharedElement()
            )
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    inline fun <Pane, Destination : Node> MovableSharedElementHostState<Pane, Destination>.SharedElement(
        modifier: Modifier,
        sharedContentState: SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        crossinline content: @Composable MovableSharedElementHostState<Pane, Destination>.() -> Unit,
    ) {
        Box(modifier) {
            Box(
                Modifier
                    .sharedElement(
                        sharedContentState = sharedContentState,
                        animatedVisibilityScope = animatedVisibilityScope,
                        placeHolderSize = placeHolderSize,
                        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                        zIndexInOverlay = zIndexInOverlay,
                        clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                    )
                    .fillSharedElement()
            ) {
                content()
            }
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    inline fun <Pane, Destination : Node> MovableSharedElementHostState<Pane, Destination>.SharedElementWithCallerManagedVisibility(
        modifier: Modifier,
        sharedContentState: SharedContentState,
        placeHolderSize: PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: OverlayClip,
        isVisible: () -> Boolean,
        crossinline content: @Composable MovableSharedElementHostState<Pane, Destination>.() -> Unit,
    ) {
        Box(modifier) {
            val visible = isVisible()
            Box(
                Modifier
                    .sharedElementWithCallerManagedVisibility(
                        sharedContentState = sharedContentState,
                        visible = visible,
                        placeHolderSize = placeHolderSize,
                        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                        zIndexInOverlay = zIndexInOverlay,
                        clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                    )
                    .fillSharedElement()
            ) {
                if (visible) content()
            }
            if (!visible) content()
        }
    }

    fun Modifier.fillSharedElement() = this.then(FillSharedElement)

    companion object {
        val FillSharedElement = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = when {
                            constraints.hasBoundedWidth -> constraints.maxWidth
                            else -> constraints.minWidth
                        },
                        minHeight = when {
                            constraints.hasBoundedHeight -> constraints.maxHeight
                            else -> constraints.minHeight
                        }
                    )
                )
                layout(
                    width = placeable.width,
                    height = placeable.height
                ) {
                    placeable.place(0, 0)
                }
            }
    }
}



