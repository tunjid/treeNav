package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.toOffset
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.utilities.AnimatedBoundsState
import com.tunjid.treenav.compose.utilities.AnimatedBoundsState.Companion.animateBounds
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

@Stable
@OptIn(ExperimentalSharedTransitionApi::class)
internal class MovableSharedElementState<State, Pane, Destination : Node>(
    paneScope: PaneScope<Pane, Destination>,
    sharedTransitionScope: SharedTransitionScope,
    sharedElement: @Composable (State, Modifier) -> Unit,
    onRemoved: () -> Unit,
    boundsTransform: BoundsTransform,
    private val canAnimateOnStartingFrames: PaneState<Pane, Destination>.() -> Boolean
) : SharedElementOverlay, SharedTransitionScope by sharedTransitionScope {

    var paneScope by mutableStateOf(paneScope)

    private var composedRefCount by mutableIntStateOf(0)

    private var layer: GraphicsLayer? = null
    var animInProgress by mutableStateOf(false)
        private set

    private val canDrawInOverlay get() = animInProgress
    private val panesKeysToSeenCount = mutableStateMapOf<String, Unit>()

    private val animatedBoundsState = AnimatedBoundsState(
        lookaheadScope = this,
        boundsTransform = boundsTransform,
        inProgress = { animInProgress }
    )

    val moveableSharedElement: @Composable (Any?, Modifier) -> Unit =
        movableContentOf { state, modifier ->
            animInProgress = isInProgress()
            val layer = rememberGraphicsLayer().also {
                this.layer = it
            }
            @Suppress("UNCHECKED_CAST")
            sharedElement(
                // The shared element composable will be created by the first screen and reused by
                // subsequent screens. This updates the state from other screens so changes are seen.
                state as State,
                modifier
                    .animateBounds(
                        state = animatedBoundsState
                    )
                    .drawWithContent {
                        layer.record {
                            this@drawWithContent.drawContent()
                        }
                        if (!canDrawInOverlay) {
                            drawLayer(layer)
                        }
                    },
            )

            DisposableEffect(Unit) {
                ++composedRefCount
                onDispose {
                    if (--composedRefCount <= 0) onRemoved()
                }
            }
        }

    override fun ContentDrawScope.drawInOverlay() {
        if (!canDrawInOverlay) return
        val overlayLayer = layer ?: return
        val (x, y) = animatedBoundsState.targetOffset.toOffset()
        translate(x, y) {
            drawLayer(overlayLayer)
        }
    }

    private fun updatePaneStateSeen(
        paneState: PaneState<*, *>
    ) {
        panesKeysToSeenCount[paneState.key] = Unit
    }

    private val hasBeenShared get() = panesKeysToSeenCount.size > 1

    companion object {

        @Composable
        private fun <Pane, Destination : Node> MovableSharedElementState<*, Pane, Destination>.isInProgress(): Boolean {
            val paneState = paneScope.paneState.also(::updatePaneStateSeen)

            val (laggingScopeKey, animationInProgressTillFirstIdle) = produceState(
                initialValue = Pair(
                    paneState.key,
                    paneState.canAnimateOnStartingFrames()
                ),
                key1 = paneState.key
            ) {
                value = Pair(
                    paneState.key,
                    paneState.canAnimateOnStartingFrames()
                )
                value = snapshotFlow { animatedBoundsState.isIdle }
                    .debounce { if (it) 10 else 0 }
                    .first(true::equals)
                    .let { value.first to false }
            }.value


            if (!hasBeenShared) return false

            val isLagging = laggingScopeKey != paneScope.paneState.key
            val canAnimateOnStartingFrames = paneScope.paneState.canAnimateOnStartingFrames()

            if (isLagging) return canAnimateOnStartingFrames

            return animationInProgressTillFirstIdle
        }
    }
}

private val PaneState<*, *>.key get() = "${currentDestination?.id}-$pane"
