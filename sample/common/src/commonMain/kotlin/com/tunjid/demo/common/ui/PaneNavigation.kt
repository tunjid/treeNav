/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.tunjid.demo.common.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.current

@Composable
fun PaneScaffoldState.PaneNavigationBar(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }),
) = withUpdatedPaneScaffoldNavigationState(
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    canShow = canShowBottomNavigation,
    content = content@{
        Box(
            modifier = modifier
                .navigationSharedElement(
                    sharedContentState = rememberSharedContentState(BottomNavSharedElementKey),
                )
        ) {
            if (canUseMovableContent) LocalAppState.current.movableNavigationBar(
                this@content,
                Modifier.fillMaxConstraints()
            )
            else PaneNavigationBar(Modifier.fillMaxConstraints())
        }
    }
)

@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { -it }),
    exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { -it }),
) = withUpdatedPaneScaffoldNavigationState(
    enterTransition = if (canShowNavRail) enterTransition else EnterTransition.None,
    exitTransition = if (canShowNavRail) exitTransition else ExitTransition.None,
    canShow = canShowNavRail,
    content = content@{
        Box(
            modifier = modifier
                .navigationSharedElement(
                    sharedContentState = rememberSharedContentState(NavRailSharedElementKey),
                )
        ) {
            if (canUseMovableContent) LocalAppState.current.movableNavigationRail(
                this@content,
                Modifier.fillMaxConstraints()
            )
            else PaneNavigationRail(Modifier.fillMaxConstraints())
        }
    }
)

@Composable
internal fun NavigationBarState.PaneNavigationBar(
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    AnimatedVisibility(
        modifier = modifier,
        visible = canShow,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            NavigationBar {
                SampleDestination.NavTabs.entries.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                            )
                        },
                        selected = item == appState.currentNavigation.current,
                        onClick = { appState.setTab(item) }
                    )
                }
            }
        },
    )
}

@Composable
internal fun NavigationBarState.PaneNavigationRail(
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    AnimatedVisibility(
        modifier = modifier,
        visible = canShow,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            NavigationRail {
                SampleDestination.NavTabs.entries.forEach { item ->
                    NavigationRailItem(
                        selected = item == appState.currentNavigation.current,
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                            )
                        },
                        onClick = { appState.setTab(item) }
                    )
                }
            }
        }
    )
}

@Composable
private fun PaneScaffoldState.withUpdatedPaneScaffoldNavigationState(
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    canShow: Boolean,
    content: @Composable NavigationBarState.() -> Unit
) {
    val state = remember {
        NavigationBarState(
            delegate = this,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            canShow = canShow,
        )
    }.also {
        it.enterTransition = enterTransition
        it.exitTransition = exitTransition
        it.canShow = canShow
    }

    state.content()
}

@Stable
internal class NavigationBarState(
    private val delegate: PaneScaffoldState,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    canShow: Boolean,
) : ThreePaneMovableElementSharedTransitionScope<SampleDestination> by delegate {
    var enterTransition by mutableStateOf(enterTransition)
    var exitTransition by mutableStateOf(exitTransition)
    var canShow by mutableStateOf(canShow)

    val canUseMovableContent
        get() = canShow && when {
            isActive && isPreviewingBack && paneState.pane == ThreePane.TransientPrimary -> true
            isActive && !isPreviewingBack && paneState.pane == ThreePane.Primary -> true
            else -> false
        }

    private val isPreviewingBack: Boolean
        get() = paneState.adaptations.contains(ThreePane.PrimaryToTransient)


    @OptIn(ExperimentalSharedTransitionApi::class)
    fun Modifier.navigationSharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
    ) = sharedElementWithCallerManagedVisibility(
        sharedContentState = sharedContentState,
        visible = canShow && delegate.isActive,
        zIndexInOverlay = NavigationSharedElementZIndex,
    )
}

private fun Modifier.fillMaxConstraints() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight
            )
        )
        layout(
            width = placeable.width,
            height = placeable.height
        ) {
            placeable.place(0, 0)
        }
    }

private data object BottomNavSharedElementKey
private data object NavRailSharedElementKey

private const val NavigationSharedElementZIndex = 2f
