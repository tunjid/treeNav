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
import com.tunjid.demo.common.ui.SampleAppState.Companion.rememberAdaptiveNavHostState
import com.tunjid.demo.common.ui.chat.chatAdaptiveConfiguration
import com.tunjid.demo.common.ui.chatrooms.chatRoomPaneConfiguration
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.profile.profileAdaptiveConfiguration
import com.tunjid.demo.common.ui.settings.settingsPaneConfiguration
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.adaptive.threepane.configurations.threePaneAdaptiveConfiguration
import com.tunjid.treenav.current
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App(
    appState: SampleAppState = remember { SampleAppState() },
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            SampleDestination.NavTabs.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.title
                        )
                    },
                    label = { Text(it.title) },
                    selected = it == appState.currentNavigation.current,
                    onClick = { }
                )
            }
        }
    ) {
        SharedTransitionScope { sharedTransitionModifier ->
            val windowWidthDp = remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            AdaptiveNavHost(
                state = appState.rememberAdaptiveNavHostState {
                    this
                        .threePaneAdaptiveConfiguration(
                            windowWidthDpState = windowWidthDp
                        )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        windowWidthDp.value = (it.width / density.density).roundToInt()
                    }
                        then sharedTransitionModifier
            ) {
                ListDetailPaneScaffold(
                    directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
                    value = ThreePaneScaffoldValue(
                        primary = if (nodeFor(ThreePane.Primary) == null) PaneAdaptedValue.Hidden else PaneAdaptedValue.Expanded,
                        secondary = if (nodeFor(ThreePane.Secondary) == null) PaneAdaptedValue.Hidden else PaneAdaptedValue.Expanded,
                        tertiary = if (nodeFor(ThreePane.Tertiary) == null) PaneAdaptedValue.Hidden else PaneAdaptedValue.Expanded,
                    ),
                    listPane = {
                        Destination(ThreePane.Secondary)
                    },
                    detailPane = {
                        Destination(ThreePane.Primary)
                    },
                    extraPane = {
                        Destination(ThreePane.Tertiary)
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

    private val adaptiveNavHostConfiguration = sampleAppAdaptiveConfiguration(
        navigationState
    )

    companion object {
        @Composable
        fun SampleAppState.rememberAdaptiveNavHostState(
            configurationBlock: AdaptiveNavHostConfiguration<
                    ThreePane,
                    MultiStackNav,
                    SampleDestination
                    >.() -> AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, SampleDestination>
        ): SavedStateAdaptiveNavHostState<ThreePane, SampleDestination> {
            val adaptiveNavHostState = remember {
                SavedStateAdaptiveNavHostState(
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

private fun sampleAppAdaptiveConfiguration(
    multiStackNavState: State<MultiStackNav>
) = adaptiveNavHostConfiguration(
    navigationState = multiStackNavState,
    destinationTransform = { multiStackNav ->
        multiStackNav.current as? SampleDestination ?: throw IllegalArgumentException(
            "MultiStackNav leaf node ${multiStackNav.current} must be an AppDestination"
        )
    },
    strategyTransform = { destination ->
        when (destination) {
            SampleDestination.NavTabs.ChatRooms -> chatRoomPaneConfiguration()

            is SampleDestination.Chat -> chatAdaptiveConfiguration(destination)

            SampleDestination.NavTabs.Settings -> settingsPaneConfiguration()

            is SampleDestination.Profile -> profileAdaptiveConfiguration(destination)
        }
    }
)
