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

package com.tunjid.demo.common.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.roundToInt

// Previews back content as specified by the material motion spec for Android predictive back:
// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs
fun Modifier.predictiveBackModifier(
    touchOffsetState: State<IntOffset>,
    progressState: State<Float>,
): Modifier = layout { measurable, constraints ->
    val touchOffset by touchOffsetState
    val progress by progressState
    val scale = 1f - (progress * 0.15F)

    val placeable = measurable.measure(
        if (progress.isNaN()) constraints
        else constraints.copy(
            maxWidth = (constraints.maxWidth * scale).roundToInt(),
            minWidth = (constraints.minWidth * scale).roundToInt(),
            maxHeight = (constraints.maxHeight * scale).roundToInt(),
            minHeight = (constraints.minHeight * scale).roundToInt(),
        )
    )
    val paneWidth = (placeable.width * scale).fastRoundToInt()
    val paneHeight = (placeable.height * scale).fastRoundToInt()

    if (progress.isNaN() || paneWidth == 0 || paneHeight == 0) return@layout layout(
        paneWidth,
        paneHeight
    ) {
        placeable.place(0, 0)
    }

    val scaledWidth = paneWidth * scale
    val spaceOnEachSide = (paneWidth - scaledWidth) / 2
    val margin = (BACK_PREVIEW_PADDING * progress).dp.roundToPx()
    val isFromLeft = true

    val xOffset = ((spaceOnEachSide - margin) * when {
        isFromLeft -> 1
        else -> -1
    }).toInt()

    val maxYShift = ((paneHeight / 20) - BACK_PREVIEW_PADDING)
    val isOrientedHorizontally = paneWidth > paneHeight
    val screenSize = when {
        isOrientedHorizontally -> paneWidth
        else -> paneHeight
    }.dp.roundToPx()
    val touchPoint = when {
        isOrientedHorizontally -> touchOffset.x
        else -> touchOffset.y
    }
    val verticalProgress = (touchPoint / screenSize) - 0.5f
    val yOffset = (verticalProgress * maxYShift).roundToInt()

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(x = xOffset, y = -yOffset)
    }
}

private const val BACK_PREVIEW_PADDING = 8
