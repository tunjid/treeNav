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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeader
import com.tunjid.demo.common.ui.ProfilePhoto
import com.tunjid.demo.common.ui.ProfilePhotoArgs
import com.tunjid.demo.common.ui.SampleTopAppBar
import com.tunjid.demo.common.ui.rememberAppBarCollapsingHeaderState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    onAction: (Action) -> Unit,
) {
    val headerState = rememberAppBarCollapsingHeaderState(400.dp)

    CollapsingHeader(
        state = headerState,
        headerContent = {
            ProfileHeader(
                state = state,
                movableSharedElementScope = movableSharedElementScope,
                onBackPressed = remember(state.profileName) {
                    if (state.profileName != null) return@remember {
                        onAction(Action.Navigation.Pop)
                    } else null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = -headerState.translation.roundToInt()
                        )
                    }
            )
        },
        body = {
            ProfileDetails(state)
        }
    )
}

@Composable
private fun ProfileHeader(
    state: State,
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    onBackPressed: (() -> Unit)?,
) {
    Box(
        modifier = Modifier.heightIn(min = 400.dp)
    ) {
        ProfilePhoto(
            state = state,
            movableSharedElementScope = movableSharedElementScope,
            modifier = modifier
        )
        SampleTopAppBar(
            title = if (state.profileName == null) "Me" else "Profile",
            onBackPressed = onBackPressed,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfilePhoto(
    state: State,
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
) {
    val profileName = state.profileName ?: state.profile?.name
    if (profileName != null) {
        val sharedImage = movableSharedElementScope.movableSharedElementOf(
            key = profileName,
            sharedElement = { args: ProfilePhotoArgs, innerModifier: Modifier ->
                ProfilePhoto(args, innerModifier)
            }
        )
        sharedImage(
            ProfilePhotoArgs(
                profileName = profileName,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            ),
            modifier,
        )
    }
}

@Composable
private fun ProfileDetails(
    state: State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = state.profile?.name ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.profile?.jobTitle ?: "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.profile?.location ?: "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.profile?.selfDescription ?: "",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
