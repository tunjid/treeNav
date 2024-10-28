/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.demo.common.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.demo.common.ui.SampleAppState.Companion.rememberPanedNavHostState
import com.tunjid.demo.common.ui.chat.chatPaneStrategy
import com.tunjid.demo.common.ui.chatrooms.chatRoomPaneStrategy
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.me.mePaneStrategy
import com.tunjid.demo.common.ui.profile.profilePaneStrategy
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.SavedStatePanedNavHostState
import com.tunjid.treenav.compose.configurations.animatePaneBoundsConfiguration
import com.tunjid.treenav.compose.configurations.paneModifierConfiguration
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.panedNavHostConfiguration
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.canAnimateOnStartingFrames
import com.tunjid.treenav.compose.threepane.configurations.predictiveBackConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedMovableSharedElementConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedNavHostConfiguration
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.popToRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SampleApp(
    appState: SampleAppState = remember { SampleAppState() },
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            SampleDestination.NavTabs.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.title) },
                    label = { Text(it.title) },
                    selected = it == appState.currentNavigation.current,
                    onClick = { appState.setTab(it) }
                )
            }
        }
    ) {
        SharedTransitionScope { sharedTransitionModifier ->
            val order = remember {
                listOf(
                    ThreePane.Tertiary,
                    ThreePane.Secondary,
                    ThreePane.Primary,
                )
            }
            val splitLayoutState = remember {
                SplitLayoutState(
                    orientation = Orientation.Horizontal,
                    maxCount = order.size,
                )
            }
            val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                animateDpAsState(if (appState.predictiveBackStatus.value) 16.dp else 0.dp).value
            )
            val density = LocalDensity.current
            val movableSharedElementHostState = remember {
                MovableSharedElementHostState(
                    sharedTransitionScope = this,
                    canAnimateOnStartingFrames = PaneState<ThreePane, SampleDestination>::canAnimateOnStartingFrames
                )
            }

            var canAnimatePanes by remember { mutableStateOf(true) }
            val interactingWithPanes = appState.isInteractingWithPanes()
            LaunchedEffect(interactingWithPanes) {
                canAnimatePanes = !interactingWithPanes
            }

            PanedNavHost(
                modifier = Modifier
                    .fillMaxSize(),
                state = appState.rememberPanedNavHostState {
                    this
                        .paneModifierConfiguration {
                            if (paneState.pane == ThreePane.TransientPrimary) Modifier
                                .fillMaxSize()
                                .predictiveBackModifier(
                                    touchOffsetState = appState.backTouchOffsetState,
                                    progressState = appState.backProgressFractionState
                                )
                                .background(surfaceColor, RoundedCornerShape(16.dp))
                            else Modifier
                                .fillMaxSize()
                        }
                        .threePanedNavHostConfiguration(
                            windowWidthDpState = derivedStateOf {
                                splitLayoutState.size.value.roundToInt()
                            }
                        )
                        .predictiveBackConfiguration(
                            isPreviewingBack = appState.predictiveBackStatus,
                            backPreviewTransform = MultiStackNav::pop,
                        )
                        .threePanedMovableSharedElementConfiguration(
                            movableSharedElementHostState = movableSharedElementHostState
                        )
                        .animatePaneBoundsConfiguration(
                            lookaheadScope = this@SharedTransitionScope,
                            shouldAnimatePane = {
                                when (paneState.pane) {
                                    ThreePane.Primary,
                                    ThreePane.TransientPrimary,
                                    ThreePane.Secondary,
                                    ThreePane.Tertiary -> canAnimatePanes

                                    null,
                                    ThreePane.Overlay -> false
                                }
                            }
                        )
                },
            ) {
                val filteredOrder by remember {
                    derivedStateOf { order.filter { nodeFor(it) != null } }
                }
                splitLayoutState.visibleCount = filteredOrder.size
                SplitLayout(
                    state = splitLayoutState,
                    modifier = Modifier
                        .fillMaxSize()
                            then movableSharedElementHostState.modifier
                            then sharedTransitionModifier,
                    itemSeparators = { paneIndex, offset ->
                        PaneSeparator(
                            splitLayoutState = splitLayoutState,
                            interactionSource = appState.paneInteractionSourceAt(paneIndex),
                            index = paneIndex,
                            density = density,
                            xOffset = offset,
                        )
                    },
                    itemContent = { index ->
                        val pane = filteredOrder[index]
                        Destination(pane)
                        if (pane == ThreePane.Primary) Destination(ThreePane.TransientPrimary)
                    }
                )
            }
        }
    }
}

@Composable
private fun PaneSeparator(
    splitLayoutState: SplitLayoutState,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    index: Int,
    density: Density,
    xOffset: Dp,
) {
    var alpha by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState {
        splitLayoutState.dragBy(
            index = index,
            delta = with(density) { it.toDp() }
        )
    }
    val active = interactionSource.isActive()
    Box(
        modifier = modifier
            .alpha(alpha)
            .offset(x = xOffset - (PaneSeparatorTouchTargetWidthDp / 2))
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                interactionSource = interactionSource,
            )
            .hoverable(interactionSource)
            .width(PaneSeparatorTouchTargetWidthDp)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(
                    color = animateColorAsState(
                        if (active) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    ).value,
                    shape = RoundedCornerShape(PaneSeparatorActiveWidthDp),
                )
                .width(animateDpAsState(if (active) PaneSeparatorActiveWidthDp else 1.dp).value)
                .height(PaneSeparatorActiveWidthDp)
        )
    }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1000),
            block = { value, _ -> alpha = value }
        )
    }
}

@Composable
fun InteractionSource.isActive(): Boolean {
    val hovered by collectIsHoveredAsState()
    val pressed by collectIsPressedAsState()
    val dragged by collectIsDraggedAsState()
    return hovered || pressed || dragged
}

@Stable
class SampleAppState(
    private val navigationRepository: NavigationRepository = NavigationRepository
) {

    private val navigationState = mutableStateOf(
        navigationRepository.navigationStateFlow.value
    )
    private val panedNavHostConfiguration = sampleAppNavHostConfiguration(
        navigationState
    )
    private val paneInteractionSourceList = mutableStateListOf<MutableInteractionSource>()

    val backTouchOffsetState = mutableStateOf(IntOffset.Zero)
    val backProgressFractionState = mutableFloatStateOf(Float.NaN)

    val currentNavigation by navigationState
    val predictiveBackStatus = derivedStateOf { !backProgressFractionState.value.isNaN() }

    fun setTab(destination: SampleDestination.NavTabs) {
        navigationRepository.navigate {
            if (it.currentIndex == destination.ordinal) it.popToRoot()
            else it.copy(currentIndex = destination.ordinal)
        }
    }

    fun paneInteractionSourceAt(index: Int): MutableInteractionSource {
        while (paneInteractionSourceList.lastIndex < index) {
            paneInteractionSourceList.add(MutableInteractionSource())
        }
        return paneInteractionSourceList[index]
    }

    @Composable
    fun isInteractingWithPanes(): Boolean =
        paneInteractionSourceList.any { it.isActive() }

    fun updatePredictiveBack(
        touchOffset: Offset,
        fraction: Float,
    ) {
        backTouchOffsetState.value = touchOffset.round()
        backProgressFractionState.value = fraction
    }

    fun cancelPredictiveBack() {
        backTouchOffsetState.value = IntOffset.Zero
        backProgressFractionState.value = Float.NaN
    }

    fun goBack() {
        cancelPredictiveBack()
        navigationRepository.navigate(MultiStackNav::pop)
    }

    companion object {
        @Composable
        fun SampleAppState.rememberPanedNavHostState(
            configurationBlock: PanedNavHostConfiguration<
                    ThreePane,
                    MultiStackNav,
                    SampleDestination
                    >.() -> PanedNavHostConfiguration<ThreePane, MultiStackNav, SampleDestination>
        ): SavedStatePanedNavHostState<ThreePane, SampleDestination> {
            val panedNavHostState = remember {
                SavedStatePanedNavHostState(
                    panes = ThreePane.entries.toList(),
                    configuration = panedNavHostConfiguration.configurationBlock(),
                )
            }
            DisposableEffect(Unit) {
                val job = CoroutineScope(Dispatchers.Main.immediate).launch {
                    navigationRepository.navigationStateFlow.collect { multiStackNav ->
                        navigationState.value = multiStackNav
                    }
                }
                onDispose { job.cancel() }
            }
            return panedNavHostState
        }
    }
}

private fun sampleAppNavHostConfiguration(
    multiStackNavState: State<MultiStackNav>
) = panedNavHostConfiguration(
    navigationState = multiStackNavState,
    destinationTransform = { multiStackNav ->
        multiStackNav.current as? SampleDestination ?: throw IllegalArgumentException(
            "MultiStackNav leaf node ${multiStackNav.current} must be an AppDestination"
        )
    },
    strategyTransform = { destination ->
        when (destination) {
            SampleDestination.NavTabs.ChatRooms -> chatRoomPaneStrategy()

            SampleDestination.NavTabs.Me -> mePaneStrategy()

            is SampleDestination.Chat -> chatPaneStrategy()

            is SampleDestination.Profile -> profilePaneStrategy()
        }
    }
)

private val PaneSeparatorActiveWidthDp = 56.dp
private val PaneSeparatorTouchTargetWidthDp = 16.dp