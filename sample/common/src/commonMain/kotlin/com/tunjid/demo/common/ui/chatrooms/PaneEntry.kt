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

package com.tunjid.demo.common.ui.chatrooms

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.demo.common.ui.PaneNavigationBar
import com.tunjid.demo.common.ui.PaneNavigationRail
import com.tunjid.demo.common.ui.PaneScaffold
import com.tunjid.demo.common.ui.data.ChatsRepository
import com.tunjid.demo.common.ui.predictiveBackBackgroundModifier
import com.tunjid.demo.common.ui.rememberPaneScaffoldState
import com.tunjid.demo.common.ui.viewModelCoroutineScope
import com.tunjid.treenav.compose.threepane.threePaneEntry

fun chatRoomPaneEntry(
) = threePaneEntry(
    render = {
        val viewModel = viewModel<ChatRoomsViewModel> {
            ChatRoomsViewModel(
                coroutineScope = viewModelCoroutineScope(),
                chatsRepository = ChatsRepository
            )
        }
        rememberPaneScaffoldState().PaneScaffold(
            modifier = Modifier
                .predictiveBackBackgroundModifier(this)
                .fillMaxSize(),
            content = {
                ChatRoomsScreen(
                    paneScaffoldState = this,
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    onAction = viewModel.accept,
                )
            },
            navigationBar = {
                PaneNavigationBar()
            },
            navigationRail = {
                PaneNavigationRail()
            },
        )
    }
)