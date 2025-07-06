/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.demo.common.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.composables.backpreview.backPreview
import com.tunjid.demo.common.ui.data.SampleDestination
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.adaptTo

@Composable
fun Modifier.predictiveBackBackgroundModifier(
    paneScope: PaneScope<ThreePane, *>,
): Modifier = with(paneScope) {
    val appState = LocalAppState.current
    if (paneState.pane == ThreePane.Primary
        && inPredictiveBack
        && isActive
        && !appState.dragToPopState.isDraggingToPop
    ) backPreview(appState.backPreviewState)
    else this@predictiveBackBackgroundModifier
}

val predictiveBackContentTransform: PaneScope<ThreePane, SampleDestination>.() -> ContentTransform =
    {
        ContentTransform(
            fadeIn(),
            fadeOut(targetAlpha = if (inPredictiveBack) 0.9f else 0f),
        ).adaptTo(paneScope = this)
    }
