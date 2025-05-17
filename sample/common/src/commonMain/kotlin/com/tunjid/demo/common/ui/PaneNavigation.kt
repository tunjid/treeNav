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

package com.tunjid.demo.common.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.treenav.compose.moveablesharedelement.rememberPaneMovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import com.tunjid.treenav.compose.rememberPaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.current

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneBottomAppBar(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }),
) {
    val paneScaffoldNavigationState = rememberUpdatedPaneScaffoldNavigationState(
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        canShow = canShowBottomNavigation
    )
    paneScaffoldNavigationState.updatedMovableSharedElementOf(
        key = BottomNavSharedElementKey,
        modifier = modifier,
        zIndexInOverlay = NavigationSharedElementZIndex,
        state = paneScaffoldNavigationState,
        sharedElement = { state, innerModifier ->
            state.PaneBottomAppBar(innerModifier)
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { -it }),
    exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { -it }),
) {
    val paneScaffoldNavigationState = rememberUpdatedPaneScaffoldNavigationState(
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        canShow = canShowNavRail
    )
    paneScaffoldNavigationState.updatedMovableSharedElementOf(
        key = NavRailSharedElementKey,
        modifier = modifier,
        zIndexInOverlay = NavigationSharedElementZIndex,
        state = paneScaffoldNavigationState,
        sharedElement = { state, innerModifier ->
            state.PaneNavigationRail(innerModifier)
        }
    )
}

@Composable
private fun NavigationBarState.PaneBottomAppBar(
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
private fun NavigationBarState.PaneNavigationRail(
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
private fun PaneScaffoldState.rememberUpdatedPaneScaffoldNavigationState(
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    canShow: Boolean,
): NavigationBarState {

    val appState = LocalAppState.current

    val paneMovableElementSharedTransitionScope =
        rememberPaneMovableElementSharedTransitionScope(
            paneSharedTransitionScope = this,
            movableSharedElementScope = rememberPaneMovableSharedElementScope(
                movableSharedElementHostState = appState.movableSharedElementHostState
            ),
        )

    return remember {
        NavigationBarState(
            delegate = paneMovableElementSharedTransitionScope,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            canShow = canShow,
        )
    }.also {
        it.enterTransition = enterTransition
        it.exitTransition = exitTransition
        it.canShow = canShow
    }
}

@Stable
private class NavigationBarState(
    delegate: ThreePaneMovableElementSharedTransitionScope<SampleDestination>,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    canShow: Boolean,
) : ThreePaneMovableElementSharedTransitionScope<SampleDestination> by delegate {
    var enterTransition by mutableStateOf(enterTransition)
    var exitTransition by mutableStateOf(exitTransition)
    var canShow by mutableStateOf(canShow)
}

private data object BottomNavSharedElementKey
private data object NavRailSharedElementKey

private const val NavigationSharedElementZIndex = 2f
