/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.treenav.compose.utilities

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch


@ExperimentalSharedTransitionApi // Depends on BoundsTransform
internal fun Modifier.animateBounds(
    lookaheadScope: LookaheadScope,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    animateMotionFrameOfReference: Boolean = false,
): Modifier =
    this.then(
        BoundsAnimationElement(
            lookaheadScope = lookaheadScope,
            boundsTransform = boundsTransform,
            // Measure with original constraints.
            // The layout of this element will still be the animated lookahead size.
            resolveMeasureConstraints = { _, constraints -> constraints },
            animateMotionFrameOfReference = animateMotionFrameOfReference,
        )
    )
        .then(modifier)
        .then(
            BoundsAnimationElement(
                lookaheadScope = lookaheadScope,
                boundsTransform = boundsTransform,
                resolveMeasureConstraints = { animatedSize, _ ->
                    // For the target Layout, pass the animated size as Constraints.
                    Constraints.fixed(animatedSize.width, animatedSize.height)
                },
                animateMotionFrameOfReference = animateMotionFrameOfReference,
            )
        )

@ExperimentalSharedTransitionApi
internal data class BoundsAnimationElement(
    val lookaheadScope: LookaheadScope,
    val boundsTransform: BoundsTransform,
    val resolveMeasureConstraints: (animatedSize: IntSize, constraints: Constraints) -> Constraints,
    val animateMotionFrameOfReference: Boolean,
) : ModifierNodeElement<BoundsAnimationModifierNode>() {
    override fun create(): BoundsAnimationModifierNode {
        return BoundsAnimationModifierNode(
            lookaheadScope = lookaheadScope,
            boundsTransform = boundsTransform,
            onChooseMeasureConstraints = resolveMeasureConstraints,
            animateMotionFrameOfReference = animateMotionFrameOfReference,
        )
    }

    override fun update(node: BoundsAnimationModifierNode) {
        node.lookaheadScope = lookaheadScope
        node.boundsTransform = boundsTransform
        node.onChooseMeasureConstraints = resolveMeasureConstraints
        node.animateMotionFrameOfReference = animateMotionFrameOfReference
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "boundsAnimation"
        properties["lookaheadScope"] = lookaheadScope
        properties["boundsTransform"] = boundsTransform
        properties["onChooseMeasureConstraints"] = resolveMeasureConstraints
        properties["animateMotionFrameOfReference"] = animateMotionFrameOfReference
    }
}

/**
 * [Modifier.Node] implementation that handles the bounds animation with
 * [ApproachLayoutModifierNode].
 *
 * @param lookaheadScope The [LookaheadScope] to animate from.
 * @param boundsTransform Callback to produce [FiniteAnimationSpec] at every triggered animation
 * @param onChooseMeasureConstraints Callback to decide whether to measure the Modifier Layout with
 *   the current animated size value or the incoming constraints. This reflects on the
 *   [MeasureResult] of this Modifier Layout as well.
 * @param animateMotionFrameOfReference Whether to include changes under
 *   [LayoutCoordinates.introducesMotionFrameOfReference] to trigger animations.
 */
@ExperimentalSharedTransitionApi
internal class BoundsAnimationModifierNode(
    var lookaheadScope: LookaheadScope,
    var boundsTransform: BoundsTransform,
    var onChooseMeasureConstraints:
        (animatedSize: IntSize, constraints: Constraints) -> Constraints,
    var animateMotionFrameOfReference: Boolean,
) : ApproachLayoutModifierNode, Modifier.Node() {
    private val boundsAnimation = BoundsTransformDeferredAnimation()

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        // Update target size, it will serve to know if we expect an approach in progress
        boundsAnimation.updateTargetSize(lookaheadSize.toSize())

        return !boundsAnimation.isIdle
    }

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        // Once we can capture size and offset we may also start the animation
        boundsAnimation.updateTargetOffsetAndAnimate(
            lookaheadScope = lookaheadScope,
            placementScope = this,
            coroutineScope = coroutineScope,
            includeMotionFrameOfReference = animateMotionFrameOfReference,
            boundsTransform = boundsTransform,
        )
        return !boundsAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // The animated value is null on the first frame as we don't get the full bounds
        // information until placement, so we can safely use the current Size.
        val fallbackSize =
            if (boundsAnimation.currentSize.isUnspecified) {
                // When using Intrinsics, we may get measured before getting the approach check
                lookaheadSize.toSize()
            } else {
                boundsAnimation.currentSize
            }
        val animatedSize = (boundsAnimation.value?.size ?: fallbackSize).roundToIntSize()

        val chosenConstraints = onChooseMeasureConstraints(animatedSize, constraints)

        val placeable = measurable.measure(chosenConstraints)
        return layout(animatedSize.width, animatedSize.height) {
            val animatedBounds = boundsAnimation.value
            val positionInScope =
                with(lookaheadScope) {
                    coordinates?.let { coordinates ->
                        lookaheadScopeCoordinates.localPositionOf(
                            sourceCoordinates = coordinates,
                            relativeToSource = Offset.Zero,
                            includeMotionFrameOfReference = animateMotionFrameOfReference
                        )
                    }
                }

            val topLeft =
                if (animatedBounds != null) {
                    boundsAnimation.updateCurrentBounds(animatedBounds.topLeft, animatedBounds.size)
                    animatedBounds.topLeft
                } else {
                    boundsAnimation.currentBounds?.topLeft ?: Offset.Zero
                }
            val (x, y) = positionInScope?.let { topLeft - it } ?: Offset.Zero
            placeable.place(x.fastRoundToInt(), y.fastRoundToInt())
        }
    }
}

/** Helper class to keep track of the BoundsAnimation state for [ApproachLayoutModifierNode]. */
@OptIn(ExperimentalSharedTransitionApi::class)
internal class BoundsTransformDeferredAnimation {
    private var animatable: Animatable<Rect, AnimationVector4D>? = null

    private var targetSize: Size = Size.Unspecified
    private var targetOffset: Offset = Offset.Unspecified

    private var isPending = false

    /**
     * Captures lookahead size, updates current size for the first pass and marks the animation as
     * pending.
     */
    fun updateTargetSize(size: Size) {
        if (targetSize.isSpecified && size.roundToIntSize() != targetSize.roundToIntSize()) {
            // Change in target, animation is pending
            isPending = true
        }
        targetSize = size

        if (currentSize.isUnspecified) {
            currentSize = size
        }
    }

    /**
     * Captures lookahead position, updates current position for the first pass and marks the
     * animation as pending.
     */
    private fun updateTargetOffset(offset: Offset) {
        if (targetOffset.isSpecified && offset.round() != targetOffset.round()) {
            isPending = true
        }
        targetOffset = offset

        if (currentPosition.isUnspecified) {
            currentPosition = offset
        }
    }

    // We capture the current bounds parameters individually to avoid unnecessary Rect allocations
    private var currentPosition: Offset = Offset.Unspecified
    var currentSize: Size = Size.Unspecified

    val currentBounds: Rect?
        get() {
            val size = currentSize
            val position = currentPosition
            return if (position.isSpecified && size.isSpecified) {
                Rect(position, size)
            } else {
                null
            }
        }

    fun updateCurrentBounds(position: Offset, size: Size) {
        currentPosition = position
        currentSize = size
    }

    val isIdle: Boolean
        get() = !isPending && animatable?.isRunning != true

    private var animatedValue: Rect? by mutableStateOf(null)

    val value: Rect?
        get() = if (isIdle) null else animatedValue

    private var directManipulationParents: MutableList<LayoutCoordinates>? = null
    private var additionalOffset: Offset = Offset.Zero

    fun updateTargetOffsetAndAnimate(
        lookaheadScope: LookaheadScope,
        placementScope: Placeable.PlacementScope,
        coroutineScope: CoroutineScope,
        includeMotionFrameOfReference: Boolean,
        boundsTransform: BoundsTransform,
    ) {
        placementScope.coordinates?.let { coordinates ->
            with(lookaheadScope) {
                val lookaheadScopeCoordinates = placementScope.lookaheadScopeCoordinates

                var delta = Offset.Zero
                if (!includeMotionFrameOfReference) {
                    // As the Layout changes, we need to keep track of the accumulated offset up
                    // the hierarchy tree, to get the proper Offset accounting for scrolling.
                    val parents = directManipulationParents ?: mutableListOf()
                    var currentCoords = coordinates
                    var index = 0

                    // Find the given lookahead coordinates by traversing up the tree
                    while (currentCoords.toLookaheadCoordinates() != lookaheadScopeCoordinates) {
                        if (currentCoords.introducesMotionFrameOfReference) {
                            if (parents.size == index) {
                                parents.add(currentCoords)
                                delta += currentCoords.positionInParent()
                            } else if (parents[index] != currentCoords) {
                                delta -= parents[index].positionInParent()
                                parents[index] = currentCoords
                                delta += currentCoords.positionInParent()
                            }
                            index++
                        }
                        currentCoords = currentCoords.parentCoordinates ?: break
                    }

                    for (i in parents.size - 1 downTo index) {
                        delta -= parents[i].positionInParent()
                        parents.removeAt(parents.size - 1)
                    }
                    directManipulationParents = parents
                }
                additionalOffset += delta

                val targetOffset =
                    lookaheadScopeCoordinates.localLookaheadPositionOf(
                        sourceCoordinates = coordinates,
                        includeMotionFrameOfReference = includeMotionFrameOfReference
                    )
                updateTargetOffset(targetOffset + additionalOffset)

                animatedValue =
                    animate(coroutineScope = coroutineScope, boundsTransform = boundsTransform)
                        .translate(-(additionalOffset))
            }
        }
    }

    private fun animate(
        coroutineScope: CoroutineScope,
        boundsTransform: BoundsTransform,
    ): Rect {
        if (targetOffset.isSpecified && targetSize.isSpecified) {
            // Initialize Animatable when possible, we might not use it but we need to have it
            // instantiated since at the first pass the lookahead information will become the
            // initial bounds when we actually need an animation.
            val target = Rect(targetOffset, targetSize)
            val anim = animatable ?: Animatable(target, Rect.VectorConverter)
            animatable = anim

            // This check should avoid triggering an animation on the first pass, as there would not
            // be enough information to have a distinct current and target bounds.
            if (isPending) {
                isPending = false
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Dispatch right away to make sure approach callbacks are accurate on `isIdle`
                    anim.animateTo(target, boundsTransform.transform(currentBounds!!, target))
                }
            }
        }
        return animatable?.value ?: Rect.Zero
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val DefaultBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold
    )
}