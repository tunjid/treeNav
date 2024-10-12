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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.tunjid.demo.common.ui.SampleAppState.Companion.rememberPanedNavHostState
import com.tunjid.demo.common.ui.chat.chatPaneStrategy
import com.tunjid.demo.common.ui.chatrooms.chatRoomPaneStrategy
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.profile.profilePaneStrategy
import com.tunjid.demo.common.ui.me.mePaneStrategy
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.SavedStatePanedNavHostState
import com.tunjid.treenav.compose.panedNavHostConfiguration
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.canAnimateOnStartingFrames
import com.tunjid.treenav.compose.threepane.configurations.threePanedMovableSharedElementConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedNavHostConfiguration
import com.tunjid.treenav.current
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
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
            val windowWidthDp = remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val movableSharedElementHostState = remember {
                MovableSharedElementHostState(
                    sharedTransitionScope = this,
                    canAnimateOnStartingFrames = PaneState<ThreePane, SampleDestination>::canAnimateOnStartingFrames
                )
            }
            PanedNavHost(
                state = appState.rememberPanedNavHostState {
                    this
                        .threePanedNavHostConfiguration(
                            windowWidthDpState = windowWidthDp
                        )
                        .threePanedMovableSharedElementConfiguration(
                            movableSharedElementHostState = movableSharedElementHostState
                        )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        windowWidthDp.value = (it.width / density.density).roundToInt()
                    }
            ) {
                ListDetailPaneScaffold(
                    modifier = Modifier
                        .fillMaxSize()
                            then movableSharedElementHostState.modifier
                            then sharedTransitionModifier,
                    directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
                    value = ThreePaneScaffoldValue(
                        primary = if (nodeFor(ThreePane.Primary) == null) PaneAdaptedValue.Hidden else PaneAdaptedValue.Expanded,
                        secondary = if (nodeFor(ThreePane.Secondary) == null) PaneAdaptedValue.Hidden else PaneAdaptedValue.Expanded,
                        tertiary = if (nodeFor(ThreePane.Tertiary) == null) PaneAdaptedValue.Hidden else PaneAdaptedValue.Expanded,
                    ),
                    listPane = {
                        if (nodeFor(ThreePane.Tertiary) == null) Destination(ThreePane.Secondary)
                        else Destination(ThreePane.Tertiary)
                    },
                    detailPane = {
                        if (nodeFor(ThreePane.Tertiary) == null) Destination(ThreePane.Primary)
                        else Destination(ThreePane.Secondary)
                    },
                    extraPane = {
                        if (nodeFor(ThreePane.Tertiary) == null) Destination(ThreePane.Tertiary)
                        else Destination(ThreePane.Primary)
                    }
                )
            }
        }
    }
}

@Stable
class SampleAppState(
    private val navigationRepository: NavigationRepository = NavigationRepository
) {

    private val navigationState = mutableStateOf(
        navigationRepository.navigationStateFlow.value
    )
    val currentNavigation by navigationState

    private val adaptiveNavHostConfiguration = sampleAppNavHostConfiguration(
        navigationState
    )

    fun setTab(destination: SampleDestination.NavTabs) {
        navigationRepository.navigate {
            it.copy(currentIndex = destination.ordinal)
        }
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
            val adaptiveNavHostState = remember {
                SavedStatePanedNavHostState(
                    panes = ThreePane.entries.toList(),
                    configuration = adaptiveNavHostConfiguration.configurationBlock(),
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
            return adaptiveNavHostState
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
