/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.tyler

import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.tunjid.demo.common.ui.AppTheme
import com.tunjid.demo.common.ui.SampleApp
import com.tunjid.demo.common.ui.SampleAppState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val appState = remember { SampleAppState() }
                SampleApp(appState)

                PredictiveBackHandler { progress: Flow<BackEventCompat> ->
                    try {
                        progress.collectIndexed { index, backEvent ->
//                            if (index == 0) onStarted()
                            val touchOffset = Offset(backEvent.touchX, backEvent.touchY)
                            val progressFraction = backEvent.progress
                            appState.updatePredictiveBack(touchOffset, progressFraction)
                        }
                        appState.goBack()
                    } catch (e: CancellationException) {
                        appState.cancelPredictiveBack()
                    }
                }
            }
        }
    }
}
