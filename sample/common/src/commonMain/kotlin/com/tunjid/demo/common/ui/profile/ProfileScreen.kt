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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeader
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.demo.common.ui.ProfilePhoto
import com.tunjid.demo.common.ui.ProfilePhotoArgs
import com.tunjid.demo.common.ui.SampleTopAppBar
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementScope
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    movableSharedElementScope: MovableSharedElementScope,
    state: State,
    onAction: (Action) -> Unit,
) {
    val density = LocalDensity.current
    val collapsedHeight = with(density) { 56.dp.toPx() } +
            WindowInsets.statusBars.getTop(density).toFloat() +
            WindowInsets.statusBars.getBottom(density).toFloat()
    val headerState = remember {
        CollapsingHeaderState(
            collapsedHeight = collapsedHeight,
            initialExpandedHeight = with(density) { 400.dp.toPx() },
            decayAnimationSpec = splineBasedDecay(density)
        )
    }
    val animatedColor by animateColorAsState(
        MaterialTheme.colorScheme.primaryContainer.copy(
            alpha = max(
                1f - headerState.progress,
                0.6f
            )
        )
    )
    CollapsingHeader(
        state = headerState,
        headerContent = {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = -headerState.translation.roundToInt()
                            )
                        }
                        .background(animatedColor)
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
                            Modifier.fillMaxSize(),
                        )
                    }
                }
                SampleTopAppBar(
                    title = state.profile?.name ?: "",
                    onBackPressed = { onAction(Action.Navigation.Pop) },
                    modifier = Modifier.onSizeChanged {
                        headerState.collapsedHeight = it.height.toFloat()
                    }
                )
            }
        },
        body = {
        }
    )
}