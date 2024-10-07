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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.demo.common.ui.chat.ChatRoomScreen
import com.tunjid.demo.common.ui.chat.ChatRoomViewModel
import com.tunjid.demo.common.ui.chatrooms.ChatRoomsScreen
import com.tunjid.demo.common.ui.chatrooms.ChatRoomsViewModel
import com.tunjid.demo.common.ui.data.ChatsRepository
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.ProfileRepository
import com.tunjid.demo.common.ui.data.SampleDestinations
import com.tunjid.demo.common.ui.profile.ProfileScreen
import com.tunjid.demo.common.ui.profile.ProfileViewModel
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.adaptive.threepane.threePaneAdaptiveNodeConfiguration
import com.tunjid.treenav.current
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun Root(
    appState: SampleAppState = remember { SampleAppState() },
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            SampleDestinations.NavTabs.entries.forEach {
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
            AdaptiveNavHost(
                state = appState.rememberAdaptiveNavHostState { this },
                modifier = Modifier
                    .fillMaxSize()
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

    @Composable
    fun rememberAdaptiveNavHostState(
        configurationBlock: AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, SampleDestinations>.() -> AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, SampleDestinations>
    ): SavedStateAdaptiveNavHostState<ThreePane, SampleDestinations> {
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

private fun sampleAppAdaptiveConfiguration(
    multiStackNavState: State<MultiStackNav>
) = adaptiveNavHostConfiguration(
    navigationState = multiStackNavState,
    destinationTransform = { multiStackNav ->
        multiStackNav.current as? SampleDestinations ?: throw IllegalArgumentException(
            "MultiStackNav leaf node ${multiStackNav.current} must be an AppDestination"
        )
    },
    strategyTransform = { destinations ->
        when (destinations) {
            SampleDestinations.NavTabs.ChatRooms -> threePaneAdaptiveNodeConfiguration(
                render = {
                    val scope = LocalLifecycleOwner.current.lifecycle.coroutineScope
                    val viewModel = viewModel<ChatRoomsViewModel> {
                        ChatRoomsViewModel(
                            coroutineScope = scope,
                            chatsRepository = ChatsRepository
                        )
                    }
                    ChatRoomsScreen(
                        state = viewModel.state.collectAsStateWithLifecycle().value,
                        onAction = viewModel.accept
                    )
                }
            )

            is SampleDestinations.Room -> threePaneAdaptiveNodeConfiguration(
                render = {
                    val scope = LocalLifecycleOwner.current.lifecycle.coroutineScope
                    val viewModel = viewModel<ChatRoomViewModel> {
                        ChatRoomViewModel(
                            coroutineScope = scope,
                            chatsRepository = ChatsRepository,
                            profileRepository = ProfileRepository,
                            room = destinations,
                        )
                    }
                    ChatRoomScreen(
                        state = viewModel.state.collectAsStateWithLifecycle().value,
                        onAction = viewModel.accept
                    )
                },
                paneMapping = {
                    destinations.threePaneMapping
                }
            )

            SampleDestinations.NavTabs.Profile -> threePaneAdaptiveNodeConfiguration(
                render = {
                    val scope = LocalLifecycleOwner.current.lifecycle.coroutineScope
                    val viewModel = viewModel<ProfileViewModel> {
                        ProfileViewModel(
                            coroutineScope = scope,
                            chatsRepository = ChatsRepository
                        )
                    }
                    ProfileScreen(
                        state = viewModel.state.collectAsStateWithLifecycle().value,
                        onAction = viewModel.accept
                    )
                }
            )
        }
    }
)
