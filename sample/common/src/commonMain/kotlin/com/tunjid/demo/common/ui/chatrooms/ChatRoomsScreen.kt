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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeader
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.demo.common.ui.SampleTopAppBar
import com.tunjid.demo.common.ui.data.ChatRoom
import com.tunjid.demo.common.ui.rememberAppBarCollapsingHeaderState
import kotlin.math.roundToInt

@Composable
fun ChatRoomsScreen(
    state: State,
    onAction: (Action) -> Unit,
) {
    val headerState = rememberAppBarCollapsingHeaderState(200.dp)

    CollapsingHeader(
        state = headerState,
        headerContent = {
            Header(headerState)
        },
        body = {
            ChatRooms(state, onAction)
        }
    )
}

@Composable
private fun Header(headerState: CollapsingHeaderState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .offset {
                IntOffset(
                    x = 0,
                    y = -headerState.translation.roundToInt()
                )
            }
    ) {
        SampleTopAppBar(
            title = "Chat Rooms",
            onBackPressed = null
        )
    }
}

@Composable
private fun ChatRooms(
    state: State,
    onAction: (Action) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = state.chatRooms,
            key = ChatRoom::name,
            itemContent = { room ->
                ChatRoomListItem(
                    roomName = room.name,
                    onRoomClicked = {
                        onAction(Action.Navigation.ToRoom(roomName = it))
                    }
                )
            }
        )
    }
}

@Composable
fun ChatRoomListItem(
    roomName: String,
    modifier: Modifier = Modifier,
    onRoomClicked: (String) -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = { onRoomClicked(roomName) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = roomName,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}