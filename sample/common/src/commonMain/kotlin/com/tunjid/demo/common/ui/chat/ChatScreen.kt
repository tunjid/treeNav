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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tunjid.demo.common.ui.ProfilePhoto
import com.tunjid.demo.common.ui.ProfilePhotoArgs
import com.tunjid.demo.common.ui.SampleTopAppBar
import com.tunjid.demo.common.ui.data.Message
import com.tunjid.demo.common.ui.data.Profile
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementScope
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ChatScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    onAction: (Action) -> Unit,
) {
    val scrollState = rememberLazyListState()
    Column(
        Modifier.fillMaxSize()
    ) {
        SampleTopAppBar(
            title = state.room?.name ?: "",
            onBackPressed = { onAction(Action.Navigation.Pop) },
        )
        Messages(
            me = state.me,
            roomName = state.room?.name,
            messages = state.chats,
            navigateToProfile = onAction,
            modifier = Modifier.weight(1f),
            scrollState = scrollState,
            movableSharedElementScope = movableSharedElementScope,
        )
    }
}


@Composable
fun Messages(
    me: Profile?,
    roomName: String?,
    messages: List<MessageItem>,
    navigateToProfile: (Action.Navigation.GoToProfile) -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    movableSharedElementScope: MovableSharedElementScope,
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(
                count = messages.size
            ) { index ->
                val prevAuthor = messages.getOrNull(index - 1)?.sender
                val nextAuthor = messages.getOrNull(index + 1)?.sender
                val content = messages[index]
                val isFirstMessageByAuthor = prevAuthor != content.sender
                val isLastMessageByAuthor = nextAuthor != content.sender

                Message(
                    onAuthorClick = navigateToProfile,
                    roomName = roomName,
                    item = content,
                    isUserMe = content.sender.name == me?.name,
                    isFirstMessageByAuthor = isFirstMessageByAuthor,
                    isLastMessageByAuthor = isLastMessageByAuthor,
                    movableSharedElementScope = movableSharedElementScope,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Message(
    onAuthorClick: (Action.Navigation.GoToProfile) -> Unit,
    item: MessageItem,
    roomName: String?,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    movableSharedElementScope: MovableSharedElementScope
) {
    val borderColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier

    Row(modifier = spaceBetweenAuthors) {
        if (isLastMessageByAuthor) {
            val sharedImage = movableSharedElementScope.movableSharedElementOf(
                key = item.sender.name,
                sharedElement = { args: ProfilePhotoArgs, innerModifier: Modifier ->
                    ProfilePhoto(args, innerModifier)
                }
            )
            // Avatar
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(42.dp)
                    .border(1.5.dp, borderColor, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.Top)
                    .clickable {
                        roomName?.let {
                            onAuthorClick(
                                Action.Navigation.GoToProfile(
                                    profileName = item.sender.name,
                                    roomName = it
                                )
                            )
                        }
                    },
            ) {
                sharedImage(
                    ProfilePhotoArgs(
                        profileName = item.sender.name,
                        contentScale = ContentScale.Crop,
                        cornerRadius = 42.dp,
                        contentDescription = null,
                    ),
                    Modifier.matchParentSize(),
                )
            }
        } else {
            // Space under avatar
            Spacer(modifier = Modifier.width(74.dp))
        }
        AuthorAndTextMessage(
            item = item,
            isUserMe = isUserMe,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    item: MessageItem,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(item)
        }
        ChatItemBubble(item, isUserMe)
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(
    item: MessageItem
) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = item.sender.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.message.timestamp.toTimestamp(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alignBy(LastBaseline),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatItemBubble(
    item: MessageItem,
    isUserMe: Boolean
) {
    val backgroundBubbleColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column {
        Surface(
            color = backgroundBubbleColor,
            shape = ChatBubbleShape
        ) {
            ChatMessage(
                message = item.message,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun ChatMessage(
    message: Message,
) {
    Text(
        text = message.content,
        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
        modifier = Modifier.padding(16.dp),
    )
}

fun Instant.toTimestamp(): String {
    // Convert Instant to LocalDateTime in the system's default time zone
    val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

    val minute = if (localDateTime.minute < 10) "0${localDateTime.minute}" else localDateTime.minute
    val amOrPm = if (localDateTime.hour > 12) "PM" else "AM"
    return "${localDateTime.hour}.$minute $amOrPm"
}

private val ChatBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
