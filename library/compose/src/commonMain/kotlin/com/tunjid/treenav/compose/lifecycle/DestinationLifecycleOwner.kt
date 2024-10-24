package com.tunjid.treenav.compose.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.SlotBasedPanedNavigationState

@Composable
internal fun rememberDestinationLifecycleOwner(
    destination: Node,
): DestinationLifecycleOwner {
    val hostLifecycleOwner = LocalLifecycleOwner.current
    val destinationLifecycleOwner = remember(hostLifecycleOwner) {
        DestinationLifecycleOwner(
            destination = destination,
            host = hostLifecycleOwner
        )
    }
    return destinationLifecycleOwner
}

@Stable
internal class DestinationLifecycleOwner(
    private val destination: Node,
    private val host: LifecycleOwner
) : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    val hostLifecycleState = host.lifecycle

    fun update(
        hostLifecycleState: State,
        paneScope: PaneScope<*, *>,
        panedNavigationState: SlotBasedPanedNavigationState<*, *>,
    ) {
        val active = paneScope.isActive
        val exists = panedNavigationState.backStackIds.contains(
            destination.id
        )
        val derivedLifecycleState = when {
            !exists -> State.DESTROYED
            !active -> State.STARTED
            else -> hostLifecycleState
        }
        lifecycleRegistry.currentState =
            if (host.lifecycle.currentState.ordinal < derivedLifecycleState.ordinal) hostLifecycleState
            else derivedLifecycleState
    }
}