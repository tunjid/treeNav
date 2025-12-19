package com.tunjid.treenav.compose

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionDefaults
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.ContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * A scope for children of [MinConstraintBox]
 */
@Stable
sealed interface MinConstraintBoxScope {
    /**
     * A [Modifier] that matches the size of the parent in each axis the parent has
     * it's bounds constrained in.
     */
    fun Modifier.fillParentAxisIfFixedOrWrap(): Modifier
}

/**
 * A layout that is effectively a [Box] where propagateMinConstraints is always true.
 */
@Composable
inline fun MinConstraintBox(
    modifier: Modifier = Modifier,
    crossinline content: @Composable MinConstraintBoxScope.() -> Unit,
) {
    Box(
        modifier = modifier,
        propagateMinConstraints = true,
    ) {
        MinConstraintBoxScopeInstance.content()
    }
}

/**
 * A higher level composable for shared elements that changes the rendering semantics when the
 * shared element has its bounds tracked or not.
 * - Elements with tracked bounds are rendered as children of a [MinConstraintBox] with
 * the shared element [Modifier] applied for bounds tracking.
 * - Untracked elements, i.e elements whose [AnimatedVisibilityScope.transition] do not report
 * a target state of [EnterExitState.Visible], are rendered as siblings so that they may still
 * be visible during the transition.
 *
 * This semantic distinction is helpful when building for adaptive layouts.
 */
@Composable
inline fun SharedTransitionScope.SharedElement(
    modifier: Modifier,
    sharedContentState: SharedTransitionScope.SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundsTransform: BoundsTransform = SharedTransitionDefaults.BoundsTransform,
    placeholderSize: SharedTransitionScope.PlaceholderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip,
    crossinline content: @Composable MinConstraintBoxScope.() -> Unit,
) {
    // 1. The Wrapper: Handles placement and sizing in the layout
    MinConstraintBox(
        modifier = modifier,
    ) {
        val boundsTracked = animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        MinConstraintBox(
            // 2. The Tracker: Holds the shared element key and bounds
            Modifier
                .sharedElement(
                    sharedContentState = sharedContentState,
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = boundsTransform,
                    placeholderSize = placeholderSize,
                    renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                    zIndexInOverlay = zIndexInOverlay,
                    clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                )
                .fillParentAxisIfFixedOrWrap(),
        ) {
            // 3a. The shared element if it is visible and animating
            if (boundsTracked) content()
        }
        // 3b. The shared element if it is just visible
        if (!boundsTracked) content()
    }
}

/**
 * A higher level composable for shared elements that changes the rendering semantics when the
 * shared element has its bounds tracked or not.
 * - Elements with tracked bounds are rendered as children of a [MinConstraintBox] with
 * the shared element [Modifier] applied for bounds tracking.
 * - Untracked elements, i.e elements whose [areBoundsTracked] returns true, are rendered as
 * siblings so that they may still be visible during the transition.
 *
 * This semantic distinction is helpful when building for adaptive layouts.
 */
@Composable
inline fun SharedTransitionScope.SharedElementWithCallerManagedVisibility(
    modifier: Modifier,
    sharedContentState: SharedTransitionScope.SharedContentState,
    placeholderSize: SharedTransitionScope.PlaceholderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip,
    crossinline areBoundsTracked: () -> Boolean,
    crossinline content: @Composable MinConstraintBoxScope.() -> Unit,
) {
    // 1. The Wrapper: Handles placement and sizing in the layout
    MinConstraintBox(
        modifier = modifier,
    ) {
        val boundsTracked = areBoundsTracked()
        MinConstraintBox(
            // 2. The Tracker: Holds the shared element key and bounds
            Modifier
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = sharedContentState,
                    visible = boundsTracked,
                    placeholderSize = placeholderSize,
                    renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                    zIndexInOverlay = zIndexInOverlay,
                    clipInOverlayDuringTransition = clipInOverlayDuringTransition,
                )

                .fillParentAxisIfFixedOrWrap(),
        ) {
            // 3a. The shared element if it is visible and animating
            if (boundsTracked) content()
        }
        // 3b. The shared element if it is just visible
        if (!boundsTracked) content()
    }
}

@PublishedApi
internal data object MinConstraintBoxScopeInstance :
    MinConstraintBoxScope {
    override fun Modifier.fillParentAxisIfFixedOrWrap(): Modifier =
        layout { measurable, constraints ->
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = when {
                        constraints.hasBoundedWidth && constraints.hasFixedWidth -> constraints.maxWidth
                        else -> constraints.minWidth
                    },
                    minHeight = when {
                        constraints.hasBoundedHeight && constraints.hasFixedHeight -> constraints.maxHeight
                        else -> constraints.minHeight
                    }
                )
            )
            layout(
                width = placeable.width,
                height = placeable.height,
            ) {
                placeable.place(0, 0)
            }
        }
}
