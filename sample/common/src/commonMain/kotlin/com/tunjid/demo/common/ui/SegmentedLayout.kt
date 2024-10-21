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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.toSize
import com.tunjid.composables.scrollbars.scrollable.sumOf

@Stable
class SegmentedLayoutState(
    val count: Int,
    minWidth: Dp = 80.dp,
    val isIndexVisible: (Int) -> Boolean = { true },
) {
    var minWidth by mutableStateOf(minWidth)
    var size by mutableStateOf(DpSize.Zero)
        internal set

    private val weightMap = mutableStateMapOf<Int, Float>().apply {
        (0..<count).forEach { index -> put(index, 1f / count) }
    }

    fun weightAt(index: Int): Float = weightMap.getValue(index)

    fun setWeightAt(index: Int, weight: Float) {
        if (weight * size.width < minWidth) return

        val oldWeight = weightMap.getValue(index)
        val weightDifference = oldWeight - weight
        val adjustedIndex = (0..<count).firstNotNullOfOrNull search@{ i ->
            val searchIndex = (index + i) % count
            if (searchIndex == index) return@search null

            val adjustedWidth = (weightMap.getValue(searchIndex) + weightDifference) * size.width
            if (adjustedWidth < minWidth) return@search null

            searchIndex
        } ?: return

        weightMap[index] = weight
        weightMap[adjustedIndex] = weightMap.getValue(adjustedIndex) + weightDifference
    }

    fun dragBy(index: Int, delta: Dp) {
        val oldWeight = weightAt(index)
        val width = oldWeight * size.width
        val newWidth = width + delta
        val newWeight = newWidth / size.width
        setWeightAt(index = index, weight = newWeight)
    }

    companion object {

        inline val SegmentedLayoutState.visibleIndices get() = (0..<count).filter(isIndexVisible)

        val SegmentedLayoutState.weightSum get() = visibleIndices.sumOf(weightMap::getValue)
    }
}

@Composable
fun SegmentedLayout(
    state: SegmentedLayoutState,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Int) -> Unit,
) {
    val density = LocalDensity.current
    Row(
        modifier = modifier
            .onSizeChanged {
                state.size = with(density) { it.toSize().toDpSize() }
            },
    ) {
        for (index in 0..<state.count) {
            if (state.isIndexVisible(index)) Box(
                modifier = Modifier
                    .weight(state.weightAt(index))
            ) {
                itemContent(index)
            }
            else Spacer(Modifier.size(0.dp))
        }
    }
}
