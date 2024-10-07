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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant

data class ChatData(
    val chatRooms: Map<String, ChatRoom>,
    val profiles: Map<String, Profile>
)

data class ChatRoom(
    val name: String,
    val messages: List<Message>
)

data class Message(
    val sender: String,
    val timestamp: Instant,
    val content: String
)

data class Profile(
    val name: String,
    val jobTitle: String,
    val location: String,
    val selfDescription: String
)

object ChatsRepository {
    val rooms: Flow<List<ChatRoom>> = flowOf(
        chatData.chatRooms.values.toList()
    )

    fun room(roomName: String): Flow<ChatRoom> = flowOf(
        chatData.chatRooms.getValue(roomName)
    )

    fun chatsFor(roomName: String): Flow<List<Message>> = flowOf(
        chatData.chatRooms[roomName]?.messages ?: emptyList()
    )
}

object ProfileRepository {
    val me: Flow<Profile> = flowOf(
        chatData.profiles.values.random()
    )
    fun profileFor(name: String): Flow<Profile> = flow {
        chatData.profiles[name]?.let { emit(it) }
    }
}


private val chatData = ChatData(
    chatRooms = mapOf(
        "SDK Design" to ChatRoom(
            name = "SDK Design",
            messages = listOf(
                Message(
                    sender = "Aisha",
                    timestamp = Instant.parse("2024-10-05T13:00:00Z"),
                    content = "Hey team, any thoughts on how we should handle message delivery confirmations in the SDK?"
                ),
                Message(
                    sender = "Bjorn",
                    timestamp = Instant.parse("2024-10-05T13:05:00Z"),
                    content = "I think we should provide callbacks for both client-side and server-side confirmations."
                ),
                Message(
                    sender = "Kenji",
                    timestamp = Instant.parse("2024-10-05T13:10:00Z"),
                    content = "Agreed. We should also consider adding an option for offline message storage."
                )
            )
        ),
        "API Integration" to ChatRoom(
            name = "API Integration",
            messages = listOf(
                Message(
                    sender = "Diego",
                    timestamp = Instant.parse("2024-10-05T14:00:00Z"),
                    content = "Has anyone started working on the API integration for user authentication?"
                ),
                Message(
                    sender = "Aisha",
                    timestamp = Instant.parse("2024-10-05T14:05:00Z"),
                    content = "I've been looking into it. I think OAuth 2.0 would be the best approach."
                )
            )
        ),
        "Testing" to ChatRoom(
            name = "Testing",
            messages = listOf(
                Message(
                    sender = "Aisha",
                    timestamp = Instant.parse("2024-10-05T15:00:00Z"),
                    content = "Bjorn, are you ready to start testing the message sending functionality?"
                ),
                Message(
                    sender = "Bjorn",
                    timestamp = Instant.parse("2024-10-05T15:05:00Z"),
                    content = "Almost! Just finishing up the unit tests."
                )
            )
        ),
        "Documentation" to ChatRoom(
            name = "Documentation",
            messages = listOf(
                Message(
                    sender = "Kenji",
                    timestamp = Instant.parse("2024-10-05T16:00:00Z"),
                    content = "Lin, can you start working on the documentation for the SDK?"
                ),
                Message(
                    sender = "Lin",
                    timestamp = Instant.parse("2024-10-05T16:05:00Z"),
                    content = "Sure, I'll get started on that today."
                )
            )
        ),
        "Random" to ChatRoom(
            name = "Random",
            messages = listOf(
                Message(
                    sender = "Diego",
                    timestamp = Instant.parse("2024-10-05T17:00:00Z"),
                    content = "Anyone want to grab coffee?"
                ),
                Message(
                    sender = "Aisha",
                    timestamp = Instant.parse("2024-10-05T17:05:00Z"),
                    content = "Sure, I'm in!"
                ),
                Message(
                    sender = "Bjorn",
                    timestamp = Instant.parse("2024-10-05T17:10:00Z"),
                    content = "Count me in too!"
                )
            )
        )
    ),
    profiles = mapOf(
        "Aisha" to Profile(
            name = "Aisha",
            jobTitle = "Lead Software Engineer",
            location = "Lagos, Nigeria",
            selfDescription = "Passionate about building scalable and reliable chat infrastructure."
        ),
        "Bjorn" to Profile(
            name = "Bjorn",
            jobTitle = "Software Engineer",
            location = "Stockholm, Sweden",
            selfDescription = "Enjoys tackling complex problems and writing clean, efficient code."
        ),
        "Kenji" to Profile(
            name = "Kenji",
            jobTitle = "QA Engineer",
            location = "Tokyo, Japan",
            selfDescription = "Dedicated to ensuring the quality and reliability of our chat SDK."
        ),
        "Diego" to Profile(
            name = "Diego",
            jobTitle = "Product Manager",
            location = "Buenos Aires, Argentina",
            selfDescription = "Driven by creating products that users love and that solve real problems."
        ),
        "Lin" to Profile(
            name = "Lin",
            jobTitle = "Technical Writer",
            location = "Beijing, China",
            selfDescription = "Loves explaining complex technical concepts in a clear and concise way."
        )
    )
)