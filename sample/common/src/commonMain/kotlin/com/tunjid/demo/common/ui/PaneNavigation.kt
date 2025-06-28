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
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.Modifier
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.treenav.compose.Adaptation
import com.tunjid.treenav.current

@Composable
fun PaneScaffoldState.PaneNavigationBar(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInVertically(initialOffsetY = { it }),
    exitTransition: ExitTransition = slideOutVertically(targetOffsetY = { it }),
) {
    AnimatedVisibility(
        modifier = modifier
            .sharedElement(
                sharedContentState = rememberSharedContentState(NavigationBarSharedElementKey),
                animatedVisibilityScope = this,
                zIndexInOverlay = NavigationSharedElementZIndex,
            ),
        visible = canShowNavigationBar,
        enter = enterTransition,
        exit = exitTransition,
        content = {
            val appState = LocalAppState.current
            if (canUseMovableNavigationBar) appState.movableNavigationBar(Modifier)
            else appState.PaneNavigationBar(Modifier)
        }
    )
}

@Composable
fun PaneScaffoldState.PaneNavigationRail(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { -it }),
    exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { -it }),
) {
    AnimatedVisibility(
        modifier = modifier
            .sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(NavigationRailSharedElementKey),
                visible = canShowNavigationRail,
                zIndexInOverlay = NavigationSharedElementZIndex,
                boundsTransform = NavigationRailBoundsTransform,
            ),
        visible = canShowNavigationRail,
        enter = if (canShowNavigationRail
            && paneState.adaptations.none { it is Adaptation.Swap<*> }
        ) enterTransition else EnterTransition.None,
        exit = if (canShowNavigationRail) exitTransition else ExitTransition.None,
    ) {
        val appState = LocalAppState.current
        if (canUseMovableNavigationRail) appState.movableNavigationRail(Modifier)
        else appState.PaneNavigationRail(Modifier)
    }
}

@Composable
internal fun AppState.PaneNavigationBar(
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        SampleDestination.NavTabs.entries.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                    )
                },
                selected = item == currentNavigation.current,
                onClick = { setTab(item) }
            )
        }
    }
}

@Composable
internal fun AppState.PaneNavigationRail(
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
    ) {
        SampleDestination.NavTabs.entries.forEach { item ->
            NavigationRailItem(
                selected = item == currentNavigation.current,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                    )
                },
                onClick = { setTab(item) }
            )
        }
    }
}

private data object NavigationBarSharedElementKey
private data object NavigationRailSharedElementKey

private const val NavigationSharedElementZIndex = 2f

private val NavigationRailBoundsTransform = BoundsTransform { _, _ -> snap() }
