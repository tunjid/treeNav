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

package com.tunjid.demo.common.ui.avatar

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.demo.common.ui.ProfilePhoto
import com.tunjid.demo.common.ui.ProfilePhotoArgs
import com.tunjid.demo.common.ui.dragToPop
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AvatarScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier
            .dragToPop()
            .fillMaxSize()
    ) {
        val profileName = state.profileName ?: state.profile?.name ?: ""
        movableSharedElementScope.updatedMovableSharedElementOf(
            key = "${state.roomName}-$profileName",
            state = ProfilePhotoArgs(
                profileName = profileName,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1f),
            sharedElement = { args: ProfilePhotoArgs, innerModifier: Modifier ->
                ProfilePhoto(args, innerModifier)
            }
        )
    }

}