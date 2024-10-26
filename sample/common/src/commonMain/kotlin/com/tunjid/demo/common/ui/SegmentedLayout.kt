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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.toSize

@Stable
class SegmentedLayoutState(
    val totalCount: Int,
    visibleCount: Int = totalCount,
    minWidth: Dp = 80.dp,
) {
    var visibleCount by mutableIntStateOf(visibleCount)
    var minWidth by mutableStateOf(minWidth)
    var width by mutableStateOf(DpSize.Zero.width)
        internal set

    private val weightMap = mutableStateMapOf<Int, Float>().apply {
        (0..<totalCount).forEach { index -> put(index, 1f / totalCount) }
    }

    fun weightAt(index: Int): Float = weightMap.getValue(index)

    fun setWeightAt(index: Int, weight: Float) {
        if (weight * width < minWidth) return

        val oldWeight = weightMap.getValue(index)
        val weightDifference = oldWeight - weight
        val adjustedIndex = (0..<totalCount).firstNotNullOfOrNull search@{ i ->
            val searchIndex = (index + i) % totalCount
            if (searchIndex == index) return@search null

            val adjustedWidth = (weightMap.getValue(searchIndex) + weightDifference) * width
            if (adjustedWidth < minWidth) return@search null

            searchIndex
        } ?: return

        weightMap[index] = weight
        weightMap[adjustedIndex] = weightMap.getValue(adjustedIndex) + weightDifference
    }

    fun isVisibleAt(index: Int) = index < visibleCount

    fun dragBy(index: Int, delta: Dp) {
        val oldWeight = weightAt(index)
        val width = oldWeight * width
        val newWidth = width + delta
        val newWeight = newWidth / this.width
        setWeightAt(index = index, weight = newWeight)
    }

    internal companion object SegmentedLayoutInstance {

        @Composable
        fun SegmentedLayoutState.Separators(
            separator: @Composable (paneIndex: Int, offset: Dp) -> Unit
        ) {
            val totalWeight by remember {
                derivedStateOf {
                    (0..<visibleCount).sumOf { weightAt(it).toDouble() }.toFloat()
                }
            }
            val widthLookup by remember {
                derivedStateOf {
                    (0..<visibleCount).map { index ->
                        val previousIndexOffset =
                            if (index == 0) 0.dp
                            else (weightAt(index - 1) / totalWeight) * width
                        val indexOffset = (weightAt(index) / totalWeight) * width
                        previousIndexOffset + indexOffset
                    }
                }
            }
            if (visibleCount > 1)
                for (index in 0..<visibleCount)
                    if (index != visibleCount - 1)
                        separator(index, widthLookup[index])
        }

        fun SegmentedLayoutState.updateSize(size: IntSize, density: Density) {
            this.width = with(density) { size.toSize().toDpSize().width }
        }
    }
}

@Composable
fun SegmentedLayout(
    state: SegmentedLayoutState,
    modifier: Modifier = Modifier,
    itemSeparators: @Composable (paneIndex: Int, offset: Dp) -> Unit = { _, _ -> },
    itemContent: @Composable (Int) -> Unit,
) = with(SegmentedLayoutState) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .onSizeChanged {
                state.updateSize(it, density)
            },
    ) {
        Row(
            modifier = Modifier
                .matchParentSize(),
        ) {
            for (index in 0..<state.totalCount) {
                if (state.isVisibleAt(index)) Box(
                    modifier = Modifier
                        .weight(state.weightAt(index))
                ) {
                    itemContent(index)
                }
                else Spacer(Modifier.size(0.dp))
            }
        }
        state.Separators(itemSeparators)
    }
}

