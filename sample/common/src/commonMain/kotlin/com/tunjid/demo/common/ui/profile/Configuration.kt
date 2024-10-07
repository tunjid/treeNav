/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.demo.common.ui.profile

import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.data.SampleDestination.NavTabs
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.adaptive.threepane.configurations.movableSharedElementScope
import com.tunjid.treenav.adaptive.threepane.threePaneAdaptiveNodeConfiguration

fun profileAdaptiveConfiguration(
    destination: SampleDestination.Profile
) = threePaneAdaptiveNodeConfiguration<SampleDestination>(
    render = {
        val scope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<ProfileViewModel> {
            ProfileViewModel(
                coroutineScope = scope,
                destination = destination,
            )
        }
        ProfileScreen(
            movableSharedElementScope = movableSharedElementScope(),
            state = viewModel.state.collectAsStateWithLifecycle().value,
            onAction = viewModel.accept
        )
    },
    paneMapping = {
        mapOf(
            ThreePane.Primary to destination,
            ThreePane.Secondary to destination.roomName?.let(SampleDestination::Chat),
            ThreePane.Tertiary to destination.roomName?.let { NavTabs.ChatRooms },
        )
    }
)