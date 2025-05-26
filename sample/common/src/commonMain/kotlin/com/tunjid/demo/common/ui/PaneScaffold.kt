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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.zIndex
import com.tunjid.composables.ui.skipIf
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePaneMovableElementSharedTransitionScope
import com.tunjid.treenav.compose.threepane.rememberThreePaneMovableElementSharedTransitionScope
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

@Stable
class PaneScaffoldState internal constructor(
    private val appState: AppState,
    threePaneMovableElementSharedTransitionScope: ThreePaneMovableElementSharedTransitionScope<SampleDestination>,
) : ThreePaneMovableElementSharedTransitionScope<SampleDestination> by threePaneMovableElementSharedTransitionScope {

    internal val canShowNavigationBar get() = !appState.isMediumScreenWidthOrWider

    internal val canShowNavigationRail
        get() = appState.filteredPaneOrder.firstOrNull() == paneState.pane
                && appState.isMediumScreenWidthOrWider

    val canUseMovableNavigationBar
        get() = canShowNavigationBar && when {
            isActive && isPreviewingBack && paneState.pane == ThreePane.TransientPrimary -> true
            isActive && !isPreviewingBack && paneState.pane == ThreePane.Primary -> true
            else -> false
        }

    val canUseMovableNavigationRail
        get() = canShowNavigationRail && isActive

    internal val canShowFab
        get() = when (paneState.pane) {
            ThreePane.Primary -> true
            ThreePane.TransientPrimary -> true
            ThreePane.Secondary -> false
            ThreePane.Tertiary -> false
            ThreePane.Overlay -> false
            null -> false
        }

    internal var scaffoldTargetSize by mutableStateOf(IntSize.Zero)
    internal var scaffoldCurrentSize by mutableStateOf(IntSize.Zero)

    internal fun hasMatchedSize(): Boolean =
        abs(scaffoldCurrentSize.width - scaffoldTargetSize.width) <= 2
                && abs(scaffoldCurrentSize.height - scaffoldTargetSize.height) <= 2

    private val isPreviewingBack: Boolean
        get() = paneState.adaptations.contains(ThreePane.PrimaryToTransient)
}

@Composable
fun PaneScope<ThreePane, SampleDestination>.rememberPaneScaffoldState(): PaneScaffoldState {
    val appState = LocalAppState.current
    val paneMovableElementSharedTransitionScope =
        rememberThreePaneMovableElementSharedTransitionScope()
    return remember(appState) {
        PaneScaffoldState(
            appState = appState,
            threePaneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        )
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaneScaffoldState.PaneScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
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
                        horizontal = if (appState.filteredPaneOrder.size > 1) 8.dp else 0.dp
                    )
                    .onSizeChanged {
                        scaffoldCurrentSize = it
                    },
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
): BoundsTransform = BoundsTransform { _, targetBounds ->
    paneScaffoldState.scaffoldTargetSize =
        targetBounds.size.roundToIntSize()

    when (paneScaffoldState.paneState.pane) {
        ThreePane.Primary,
        ThreePane.Secondary,
        ThreePane.Tertiary,
            -> if (canAnimatePane()) spring()
        else snap()

        ThreePane.TransientPrimary,
            -> spring<Rect>().skipIf(paneScaffoldState::hasMatchedSize)

        ThreePane.Overlay,
        null,
            -> snap()
    }
}

