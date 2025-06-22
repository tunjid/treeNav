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


import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.composables.collapsingheader.CollapsingHeaderState
import com.tunjid.composables.collapsingheader.rememberCollapsingHeaderState

@Composable
fun rememberAppBarCollapsingHeaderState(
    expandedHeight: Dp
): CollapsingHeaderState {
    val statusBars = WindowInsets.statusBars
    return rememberCollapsingHeaderState(
        collapsedHeight = {
            56.dp.toPx() +
                    statusBars.getTop(this).toFloat() +
                    statusBars.getBottom(this).toFloat()
        },
        initialExpandedHeight = { expandedHeight.toPx() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTopAppBar(
    title: @Composable () -> Unit,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            if (onBackPressed != null) IconButton(
                onClick = onBackPressed,
                content = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier,
    )
}

@Composable
fun SampleTopAppBar(
    title: String,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SampleTopAppBar(
        title = {
            Text(text = title)
        },
        onBackPressed = onBackPressed,
        modifier = modifier,
    )
}