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
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.composables.backpreview.BackPreviewState
import com.tunjid.composables.backpreview.backPreview
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.demo.common.ui.AppState.Companion.rememberMultiPaneDisplayState
import com.tunjid.demo.common.ui.chat.chatPaneStrategy
import com.tunjid.demo.common.ui.chatrooms.chatRoomPaneStrategy
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.me.mePaneStrategy
import com.tunjid.demo.common.ui.profile.profilePaneStrategy
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.backStack
import com.tunjid.treenav.compose.MultiPaneDisplay
import com.tunjid.treenav.compose.MultiPaneDisplayScope
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.transforms.backPreviewTransform
import com.tunjid.treenav.compose.threepane.transforms.threePanedAdaptiveTransform
import com.tunjid.treenav.compose.threepane.transforms.threePanedMovableSharedElementTransform
import com.tunjid.treenav.compose.transforms.Transform
import com.tunjid.treenav.compose.transforms.paneModifierTransform
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.popToRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App(
    appState: AppState = remember { AppState() },
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
            val backPreviewSurfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                animateDpAsState(if (appState.isPreviewingBack) 16.dp else 0.dp).value
            )
            val density = LocalDensity.current
            val movableSharedElementHostState = remember {
                MovableSharedElementHostState<ThreePane, SampleDestination>(
                    sharedTransitionScope = this
                )
            }

            var canAnimatePanes by remember { mutableStateOf(true) }
            val interactingWithPanes = appState.isInteractingWithPanes()
            LaunchedEffect(interactingWithPanes) {
                canAnimatePanes = !interactingWithPanes
            }

            MultiPaneDisplay(
                modifier = Modifier
                    .fillMaxSize(),
                state = appState.rememberMultiPaneDisplayState(
                    listOf(
                        threePanedAdaptiveTransform(
                            windowWidthState = remember {
                                derivedStateOf {
                                    appState.splitLayoutState.size
                                }
                            }
                        ),
                        backPreviewTransform(
                            isPreviewingBack = remember {
                                derivedStateOf {
                                    appState.isPreviewingBack
                                }
                            },
                            navigationStateBackTransform = MultiStackNav::pop,
                        ),
                        threePanedMovableSharedElementTransform(
                            movableSharedElementHostState = movableSharedElementHostState
                        ),
                        paneModifierTransform {
                            val modifier = Modifier.animateBounds(
                                lookaheadScope = this@SharedTransitionScope,
                                boundsTransform = { _, _ ->
                                    when (paneState.pane) {
                                        ThreePane.Primary,
                                        ThreePane.TransientPrimary,
                                        ThreePane.Secondary,
                                        ThreePane.Tertiary,
                                            -> if (canAnimatePanes) spring() else snap()

                                        null,
                                        ThreePane.Overlay,
                                            -> snap()
                                    }
                                }
                            )
                            if (paneState.pane == ThreePane.TransientPrimary) modifier
                                .fillMaxSize()
                                .backPreview(appState.backPreviewState)
                                .background(backPreviewSurfaceColor, RoundedCornerShape(16.dp))
                            else modifier
                                .fillMaxSize()
                        }
                    )
                ),
            ) {
                appState.displayScope = this
                appState.splitLayoutState.visibleCount = appState.filteredPaneOrder.size
                SplitLayout(
                    state = appState.splitLayoutState,
                    modifier = Modifier
                        .fillMaxSize()
                            then sharedTransitionModifier,
                    itemSeparators = { paneIndex, offset ->
                        PaneSeparator(
                            splitLayoutState = appState.splitLayoutState,
                            interactionSource = appState.paneInteractionSourceAt(paneIndex),
                            index = paneIndex,
                            density = density,
                            xOffset = offset,
                        )
                    },
                    itemContent = { index ->
                        val pane = appState.filteredPaneOrder[index]
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
class AppState(
    private val navigationRepository: NavigationRepository = NavigationRepository,
) {

    private val navigationState = mutableStateOf(
        navigationRepository.navigationStateFlow.value
    )

    private val paneInteractionSourceList = mutableStateListOf<MutableInteractionSource>()
    private val paneRenderOrder = listOf(
        ThreePane.Tertiary,
        ThreePane.Secondary,
        ThreePane.Primary,
    )

    val currentNavigation by navigationState
    val backPreviewState = BackPreviewState()
    val splitLayoutState = SplitLayoutState(
        orientation = Orientation.Horizontal,
        maxCount = paneRenderOrder.size,
        minSize = 10.dp,
        keyAtIndex = { index ->
            val indexDiff = paneRenderOrder.size - visibleCount
            paneRenderOrder[index + indexDiff]
        }
    )

    internal val isPreviewingBack
        get() = !backPreviewState.progress.isNaN()

    internal var displayScope by mutableStateOf<MultiPaneDisplayScope<ThreePane, SampleDestination>?>(
        null
    )

    val filteredPaneOrder: List<ThreePane> by derivedStateOf {
        paneRenderOrder.filter { displayScope?.destinationIn(it) != null }
    }

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

    fun goBack() {
        navigationRepository.navigate(MultiStackNav::pop)
    }

    companion object {
        @Composable
        fun AppState.rememberMultiPaneDisplayState(
            transforms: List<Transform<ThreePane, MultiStackNav, SampleDestination>>,
        ): MultiPaneDisplayState<ThreePane, MultiStackNav, SampleDestination> {
            val displayState = remember {
                MultiPaneDisplayState(
                    panes = ThreePane.entries.toList(),
                    navigationState = navigationState,
                    backStackTransform = { multiStackNav ->
                        multiStackNav.backStack(
                            includeCurrentDestinationChildren = true,
                            placeChildrenBeforeParent = true,
                        )
                            .filterIsInstance<SampleDestination>()
                    },
                    destinationTransform = {
                        it.current as? SampleDestination ?: throw IllegalArgumentException(
                            "MultiStackNav leaf node ${it.current} must be an AppDestination"
                        )
                    },
                    entryProvider = { destination ->
                        when (destination) {
                            SampleDestination.NavTabs.ChatRooms -> chatRoomPaneStrategy()

                            SampleDestination.NavTabs.Me -> mePaneStrategy()

                            is SampleDestination.Chat -> chatPaneStrategy()

                            is SampleDestination.Profile -> profilePaneStrategy()
                        }
                    },
                    transforms = transforms,
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
            return displayState
        }
    }
}

private val PaneSeparatorActiveWidthDp = 56.dp
private val PaneSeparatorTouchTargetWidthDp = 16.dp