package com.tunjid.treenav.compose.moveablesharedelement

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Stable
internal class MovableSharedElementState<State>
@OptIn(ExperimentalSharedTransitionApi::class)
constructor(
    val sharedContentState: SharedTransitionScope.SharedContentState,
    sharedElement: @Composable (State, Modifier) -> Unit,
    onRemoved: () -> Unit
) {

    private var composedRefCount by mutableIntStateOf(0)

    val moveableSharedElement: @Composable (Any?, Modifier) -> Unit =
        movableContentOf { state, modifier ->
            @Suppress("UNCHECKED_CAST")
            sharedElement(
                // The shared element composable will be created by the first screen and reused by
                // subsequent screens. This updates the state from other screens so changes are seen.
                state as State,
                modifier
            )

            DisposableEffect(Unit) {
                ++composedRefCount
                onDispose {
                    if (--composedRefCount <= 0) onRemoved()
                }
            }
        }
}

