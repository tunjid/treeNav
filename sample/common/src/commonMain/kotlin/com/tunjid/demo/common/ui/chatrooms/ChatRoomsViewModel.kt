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

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import com.tunjid.demo.common.ui.data.ChatRoom
import com.tunjid.demo.common.ui.data.ChatsRepository
import com.tunjid.demo.common.ui.data.NavigationAction
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.data.navigationAction
import com.tunjid.demo.common.ui.data.navigationMutations
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.push
import kotlinx.coroutines.flow.Flow

class ChatRoomsViewModel(
    coroutineScope: LifecycleCoroutineScope,
    chatsRepository: ChatsRepository = ChatsRepository,
    navigationRepository: NavigationRepository = NavigationRepository,
) : ViewModel() {
    private val mutator = coroutineScope.actionStateFlowMutator<Action, State>(
        initialState = State(),
        inputs = listOf(
            chatsRepository.loadMutations()
        ),
        actionTransform = { actions ->
            actions.toMutationStream(
                keySelector = Action::key
            ) {
                when (val type = type()) {
                    is Action.Navigation -> navigationRepository.navigationMutations(
                        type.flow
                    )
                }
            }
        }
    )

    val state = mutator.state

    val accept = mutator.accept
}

private fun ChatsRepository.loadMutations(): Flow<Mutation<State>> = rooms.mapToMutation {
    copy(chatRooms = it)
}


data class State(
    val chatRooms: List<ChatRoom> = emptyList()
)

sealed class Action(
    val key: String
) {
    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class ToRoom(
            val roomName: String,
        ) : Navigation(), NavigationAction by navigationAction(
            { push(SampleDestination.Chat(roomName)) }
        )
    }
}