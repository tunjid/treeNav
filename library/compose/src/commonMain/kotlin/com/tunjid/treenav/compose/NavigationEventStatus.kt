package com.tunjid.treenav.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation3.scene.SceneInfo
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
internal fun MultiPaneDisplayState<*, *, *>.rememberNavigationEventStatus(): State<NavigationEventStatus> {
    val navigationEventDispatcher = LocalNavigationEventDispatcherOwner.current!!
        .navigationEventDispatcher

    val state = remember(this) {
        NavigationEventStatusState(
            multiPaneDisplayState = this,
            history = navigationEventDispatcher.history.value,
        )
    }

    val scope = rememberCoroutineScope(Dispatchers.Main::immediate)

    DisposableEffect(Unit) {
        val job = scope.launch {
            combine(
                navigationEventDispatcher.history,
                navigationEventDispatcher.transitionState
                    .map { it is NavigationEventTransitionState.InProgress }
                    .distinctUntilChanged(),
                ::Pair,
            ).collectLatest { (history, isSeeking) ->
                state.onUpdate(history, isSeeking)
            }
        }
        onDispose(job::cancel)
    }

    return state.statusState
}

@Stable
private class NavigationEventStatusState(
    private val multiPaneDisplayState: MultiPaneDisplayState<*, *, *>,
    history: NavigationEventHistory,
) {
    val statusState = mutableStateOf<NavigationEventStatus>(
        NavigationEventStatus.Completed.Commited,
    )

    private var history by mutableStateOf(history)
    private var isSeeking by mutableStateOf(false)
    private var previousKeyAtSeek by mutableStateOf<Any?>(null)

    private var status by statusState

    fun onUpdate(
        history: NavigationEventHistory,
        isSeeking: Boolean,
    ) {
        this.history = history
        this.isSeeking = isSeeking

        when {
            isSeeking -> {
                status = NavigationEventStatus.Seeking
                previousKeyAtSeek = history.previousSceneKey
            }

            else -> {
                status = when (previousKeyAtSeek) {
                    null,
                    multiPaneDisplayState.displayCurrentSceneKey(),
                    -> NavigationEventStatus.Completed.Commited

                    else -> NavigationEventStatus.Completed.Cancelled
                }
                previousKeyAtSeek = null
            }
        }
    }
}

private val NavigationEventHistory.previousSceneKey
    get() = sceneKeyAt(currentIndex - 1)

private fun NavigationEventHistory.sceneKeyAt(
    index: Int,
) = when (val navigationEventInfo = mergedHistory.getOrNull(index)) {
    is SceneInfo<*> -> navigationEventInfo.scene.key
    else -> null
}

private fun MultiPaneDisplayState<*, *, *>.displayCurrentSceneKey(): MultiPaneSceneKey =
    MultiPaneSceneKey(
        ids = backStackTransform(navigationState.value)
            .map { it.id },
    )

sealed class NavigationEventStatus {
    data object Seeking : NavigationEventStatus()
    sealed class Completed : NavigationEventStatus() {
        data object Commited : Completed()
        data object Cancelled : Completed()
    }
}
