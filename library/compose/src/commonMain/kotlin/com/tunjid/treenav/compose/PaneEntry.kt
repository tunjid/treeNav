package com.tunjid.treenav.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.treenav.Node
import com.tunjid.treenav.compose.configurations.RenderTransform

/**
 * Provides the logic used to select, configure and place a navigation [Destination] for each
 * pane [Pane] for the current active navigation [Destination].
 */
@Stable
class PaneEntry<Pane, Destination : Node>(
    internal val renderTransform: RenderTransform<Pane, Destination>,
    internal val paneTransform:  @Composable (Destination) -> Map<Pane, Destination?>,
    internal val content: @Composable PaneScope<Pane, Destination>.(Destination) -> Unit,
)