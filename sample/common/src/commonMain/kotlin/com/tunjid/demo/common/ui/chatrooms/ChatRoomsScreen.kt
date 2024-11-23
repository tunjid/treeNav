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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderLayout
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.demo.common.ui.ProfilePhoto
import com.tunjid.demo.common.ui.ProfilePhotoArgs
import com.tunjid.demo.common.ui.SampleTopAppBar
import com.tunjid.demo.common.ui.data.ChatRoom
import com.tunjid.demo.common.ui.data.Message
import com.tunjid.demo.common.ui.rememberAppBarCollapsingHeaderState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlin.math.roundToInt

@Composable
fun ChatRoomsScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerState = rememberAppBarCollapsingHeaderState(200.dp)

    CollapsingHeaderLayout(
        state = headerState,
        modifier = modifier,
        headerContent = {
            Header(headerState)
        },
        body = {
            ChatRooms(
                movableSharedElementScope = movableSharedElementScope,
                state = state,
                onAction = onAction,
            )
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
    movableSharedElementScope: MovableSharedElementScope,
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
                    movableSharedElementScope = movableSharedElementScope,
                    roomName = room.name,
                    participants = room.messages
                        .map(Message::sender)
                        .distinct()
                        .take(3),
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
    movableSharedElementScope: MovableSharedElementScope,
    roomName: String,
    participants: List<String>,
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
                .height(68.dp)
                .padding(
                    horizontal = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatRoomParticipants(
                movableSharedElementScope = movableSharedElementScope,
                participants = participants,
                roomName = roomName,
            )
            Text(
                text = roomName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ChatRoomParticipants(
    movableSharedElementScope: MovableSharedElementScope,
    participants: List<String>,
    roomName: String,
) = with(movableSharedElementScope) {
    FlowRow(
        modifier = Modifier
            .width(64.dp)
            .rotate(
                if (participants.size == 2) 45f
                else 0f
            ),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
    ) {
        participants.forEach { profileName ->
            updatedMovableSharedElementOf(
                key = "$roomName-${profileName}",
                state = ProfilePhotoArgs(
                    profileName = profileName,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    cornerRadius = 20.dp,
                ),
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .rotate(
                        if (participants.size == 2) -45f
                        else 0f
                    ),
                sharedElement = { args: ProfilePhotoArgs, innerModifier: Modifier ->
                    ProfilePhoto(args, innerModifier)
                }
            )
        }
    }
}