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

package com.tunjid.demo.common.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Node
import com.tunjid.treenav.StackNav
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface SampleDestination : Node {

    enum class NavTabs(
        val title: String,
    ) : SampleDestination {
        ChatRooms("Chat Rooms"),
        Me("Me");

        override val id: String get() = title

        val icon
            get() = when (this) {
                ChatRooms -> Icons.AutoMirrored.Filled.List
                Me -> Icons.Default.Person
            }
    }

    data class Chat(
        val roomName: String,
    ) : SampleDestination {

        override val id: String
            get() = roomName

        override val children: List<Node>
            get() = listOf(NavTabs.ChatRooms)
    }

    data class Profile(
        val profileName: String,
        val roomName: String?,
    ) : SampleDestination {

        override val id: String
            get() = "$profileName-$roomName"

        override val children: List<Node>
            get() = listOfNotNull(
                roomName?.let(::Chat),
                roomName?.let { NavTabs.ChatRooms }
            )
    }
}

fun interface NavigationAction {
    fun navigate(multiStackNav: MultiStackNav): MultiStackNav
}

fun navigationAction(
    block: MultiStackNav.() -> MultiStackNav
) = NavigationAction(block)

object NavigationRepository {
    private val mutableNavigationStateFlow = MutableStateFlow(InitialNavState)

    val navigationStateFlow: StateFlow<MultiStackNav> = mutableNavigationStateFlow.asStateFlow()

    fun navigate(action: NavigationAction) {
        mutableNavigationStateFlow.update(action::navigate)
    }
}

fun <T> NavigationRepository.navigationMutations(
    navigationActions: Flow<NavigationAction>
): Flow<Mutation<T>> =
    navigationActions.mapToManyMutations {
        navigate(it)
    }

private val InitialNavState = MultiStackNav(
    name = "Sample",
    stacks = listOf(
        StackNav(
            name = "chatrooms",
            children = listOf(
                SampleDestination.NavTabs.ChatRooms,
            )
        ),
        StackNav(
            name = "me",
            children = listOf(
                SampleDestination.NavTabs.Me,
            )
        ),
    )
)