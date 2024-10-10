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

package com.tunjid.demo.common.ui.chat

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import com.tunjid.demo.common.ui.data.ChatRoom
import com.tunjid.demo.common.ui.data.ChatsRepository
import com.tunjid.demo.common.ui.data.Message
import com.tunjid.demo.common.ui.data.NavigationAction
import com.tunjid.demo.common.ui.data.NavigationRepository
import com.tunjid.demo.common.ui.data.Profile
import com.tunjid.demo.common.ui.data.ProfileRepository
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.demo.common.ui.data.navigationAction
import com.tunjid.demo.common.ui.data.navigationMutations
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.swap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class ChatViewModel(
    coroutineScope: LifecycleCoroutineScope,
    chatsRepository: ChatsRepository,
    profileRepository: ProfileRepository,
    navigationRepository: NavigationRepository = NavigationRepository,
    chat: SampleDestination.Chat,
) : ViewModel() {
    private val mutator = coroutineScope.actionStateFlowMutator<Action, State>(
        initialState = State(),
        inputs = listOf(
            profileRepository.meMutations(),
            chatsRepository.chatRoomMutations(chat),
            chatLoadMutations(
                chat = chat,
                chatsRepository = chatsRepository,
                profileRepository = profileRepository
            )
        ),
        actionTransform = { actions ->
            actions.toMutationStream(
                keySelector = Action::key
            ) {
                when (val type = type()) {
                    is Action.UpdateInPrimaryPane -> type.flow.updateInPrimaryPaneMutations()
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

private fun ProfileRepository.meMutations(): Flow<Mutation<State>> =
    me.mapToMutation { copy(me = it) }

private fun ChatsRepository.chatRoomMutations(
    chat: SampleDestination.Chat
): Flow<Mutation<State>> =
    room(roomName = chat.roomName)
        .mapToMutation { copy(room = it) }

private fun chatLoadMutations(
    chat: SampleDestination.Chat,
    chatsRepository: ChatsRepository,
    profileRepository: ProfileRepository,
): Flow<Mutation<State>> =
    chatsRepository.chatsFor(chat.roomName).flatMapLatest { chats ->
        combine(
            flows = chats.map { message -> profileRepository.profileFor(message.sender) }
        ) { profiles ->
            val namesToProfiles = profiles.associateBy(Profile::name)
            chats.map { message ->
                MessageItem(
                    message = message,
                    sender = namesToProfiles.getValue(message.sender)
                )
            }
        }
    }
        .mapToMutation {
            copy(chats = it)
        }

private fun Flow<Action.UpdateInPrimaryPane>.updateInPrimaryPaneMutations(): Flow<Mutation<State>> =
    mapToMutation { copy(isInPrimaryPane = it.isInPrimaryPane) }

data class State(
    val me: Profile? = null,
    val room: ChatRoom? = null,
    val isInPrimaryPane: Boolean = true,
    val chats: List<MessageItem> = emptyList()
)

data class MessageItem(
    val message: Message,
    val sender: Profile,
)

sealed class Action(
    val key: String
) {

    data class UpdateInPrimaryPane(
        val isInPrimaryPane: Boolean
    ) : Action("UpdateInPrimaryPane")

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data object Pop : Navigation(), NavigationAction by navigationAction(
            MultiStackNav::pop
        )

        data class GoToProfile(
            val profileName: String,
            val roomName: String,
            val isInPrimaryPane: Boolean,
        ) : Navigation(), NavigationAction by navigationAction(
            {
                val profileDestination = SampleDestination.Profile(
                    profileName = profileName,
                    roomName = roomName,
                )
                if (isInPrimaryPane) push(profileDestination)
                else swap(profileDestination)
            }
        )
    }
}