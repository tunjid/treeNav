package com.tunjid.treenav.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.scene.SceneInfo
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
fun rememberNavigationEventStatus(): State<NavigationEventStatus> {
    val navigationEventDispatcher = LocalNavigationEventDispatcherOwner.current!!
        .navigationEventDispatcher

    val lastSceneKey = remember {
        mutableStateOf<Any?>(null)
    }
    val navigationEventStatusState = remember {
        mutableStateOf<NavigationEventStatus>(NavigationEventStatus.Completed.Commited)
    }

    LaunchedEffect(navigationEventDispatcher) {
        navigationEventDispatcher.history.collect { history ->
            history.currentSceneKey?.let(lastSceneKey::value::set)
        }
    }

    LaunchedEffect(navigationEventDispatcher) {
        navigationEventDispatcher.transitionState.collect { transitionState ->
            navigationEventStatusState.value = when (transitionState) {
                NavigationEventTransitionState.Idle -> when (lastSceneKey.value) {
                    navigationEventDispatcher.history.value.currentSceneKey -> NavigationEventStatus.Completed.Cancelled
                    else -> NavigationEventStatus.Completed.Commited
                }

                is NavigationEventTransitionState.InProgress -> NavigationEventStatus.Seeking
            }
        }
    }
    return navigationEventStatusState
}

private val NavigationEventHistory.currentSceneKey
    get() = when (val navigationEventInfo = mergedHistory.getOrNull(currentIndex)) {
        is SceneInfo<*> -> navigationEventInfo.scene.key
        else -> null
    }

sealed class NavigationEventStatus {
    data object Seeking : NavigationEventStatus()
    sealed class Completed : NavigationEventStatus() {
        data object Commited : Completed()
        data object Cancelled : Completed()
    }
}