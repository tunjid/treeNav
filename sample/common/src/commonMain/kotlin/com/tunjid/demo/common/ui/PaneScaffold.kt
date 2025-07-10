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

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.rememberThreePaneMovableElementSharedTransitionScope
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull

@Stable
class PaneScaffoldState internal constructor(
    private val splitPaneDisplayScope: SplitPaneDisplayScope,
    threePaneMovableElementSharedTransitionScope: ThreePaneMovableElementSharedTransitionScope<SampleDestination>,
) : ThreePaneMovableElementSharedTransitionScope<SampleDestination> by threePaneMovableElementSharedTransitionScope {

    internal val canShowNavigationBar get() = !splitPaneDisplayScope.isMediumScreenWidthOrWider

    internal val canShowNavigationRail
        get() = splitPaneDisplayScope.filteredPaneOrder.firstOrNull() == paneState.pane
                && splitPaneDisplayScope.isMediumScreenWidthOrWider

    internal val canUseMovableNavigationBar
        get() = canShowNavigationBar && isActive && paneState.pane == ThreePane.Primary

    internal val canUseMovableNavigationRail
        get() = canShowNavigationRail && isActive

    internal val hasSiblings get() = splitPaneDisplayScope.filteredPaneOrder.size > 1

    internal val defaultContainerColor: Color
        @Composable get() {
            val elevation by animateDpAsState(
                if (paneState.pane == ThreePane.Primary
                    && isActive
                    && inPredictiveBack
                ) 4.dp
                else 0.dp
            )

            return MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
        }
}

@Composable
fun PaneScope<ThreePane, SampleDestination>.rememberPaneScaffoldState(): PaneScaffoldState {
    val splitPaneDisplayScope = LocalSplitPaneDisplayScope.current
    val paneMovableElementSharedTransitionScope =
        rememberThreePaneMovableElementSharedTransitionScope()
    return remember(splitPaneDisplayScope, paneMovableElementSharedTransitionScope) {
        PaneScaffoldState(
            splitPaneDisplayScope = splitPaneDisplayScope,
            threePaneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        )
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = defaultContainerColor,
    snackBarMessages: List<String> = emptyList(),
    onSnackBarMessageConsumed: (String) -> Unit = {},
    topBar: @Composable PaneScaffoldState.() -> Unit = {},
    floatingActionButton: @Composable PaneScaffoldState.() -> Unit = {},
    navigationBar: @Composable PaneScaffoldState.() -> Unit = {},
    navigationRail: @Composable PaneScaffoldState.() -> Unit = {},
    content: @Composable PaneScaffoldState.(PaddingValues) -> Unit,
) {
    val appState = LocalAppState.current
    val snackbarHostState = remember { SnackbarHostState() }

    val canAnimatePane = remember { mutableStateOf(true) }.also {
        it.value = !appState.isInteractingWithPanes()
    }

    PaneNavigationRailScaffold(
        modifier = modifier,
        navigationRail = {
            navigationRail()
        },
        content = {
            Scaffold(
                modifier = Modifier
                    .animateBounds(
                        lookaheadScope = this,
                        boundsTransform = remember {
                            scaffoldBoundsTransform(
                                paneScaffoldState = this,
                                canAnimatePane = canAnimatePane::value
                            )
                        }
                    )
                    .padding(
                        horizontal = if (hasSiblings) 8.dp else 0.dp
                    ),
                containerColor = containerColor,
                topBar = {
                    topBar()
                },
                floatingActionButton = {
                    floatingActionButton()
                },
                bottomBar = {
                    navigationBar()
                },
                snackbarHost = {
                    SnackbarHost(snackbarHostState)
                },
                content = { paddingValues ->
                    content(paddingValues)
                },
            )
        }
    )
    val updatedMessages = rememberUpdatedState(snackBarMessages.firstOrNull())
    LaunchedEffect(Unit) {
        snapshotFlow { updatedMessages.value }
            .filterNotNull()
            .filterNot(String::isNullOrBlank)
            .collect { message ->
                snackbarHostState.showSnackbar(
                    message = message
                )
                onSnackBarMessageConsumed(message)
            }
    }
}

@Composable
private inline fun PaneNavigationRailScaffold(
    modifier: Modifier = Modifier,
    navigationRail: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .widthIn(max = 80.dp)
                    .zIndex(2f),
                content = {
                    navigationRail()
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                content = {
                    content()
                }
            )
        },
    )
}


@OptIn(ExperimentalSharedTransitionApi::class)
private fun scaffoldBoundsTransform(
    paneScaffoldState: PaneScaffoldState,
    canAnimatePane: () -> Boolean,
): BoundsTransform = BoundsTransform { _, _ ->
    when (paneScaffoldState.paneState.pane) {
        ThreePane.Primary,
        ThreePane.Secondary,
        ThreePane.Tertiary,
            -> if (canAnimatePane()) spring()
        else snap()

        ThreePane.Overlay,
        null,
            -> snap()
    }
}

