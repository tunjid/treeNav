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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Node
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.adaptive.threepane.ThreePane
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SampleDestinations : Node {

    enum class NavTabs(
        val title: String,
    ) : SampleDestinations {
        ChatRooms("Chat Rooms"),
        Profile("Profile");

        override val id: String get() = title

        val icon get() = when(this) {
            ChatRooms -> Icons.AutoMirrored.Filled.List
            Profile -> Icons.Default.Person
        }
    }

    val threePaneMapping: Map<ThreePane, SampleDestinations> get() = emptyMap()

    data class Room(
        val roomName: String,
    ) : SampleDestinations {

        override val id: String
            get() = roomName

        override val threePaneMapping: Map<ThreePane, SampleDestinations> = mapOf(
            ThreePane.Primary to this,
            ThreePane.Secondary to NavTabs.ChatRooms,
        )

        override val children: List<Node>
            get() = threePaneMapping.values.toList()

    }
}

object NavigationRepository {
    private val mutableNavigationState = MutableStateFlow(InitialNavState)

    val navigationStateFlow: StateFlow<MultiStackNav> = mutableNavigationState.asStateFlow()
}

private val InitialNavState = MultiStackNav(
    name = "Sample",
    stacks = listOf(
        StackNav(
            name = "chatrooms",
            children = listOf(
                SampleDestinations.NavTabs.ChatRooms,
            )
        )
    )
)