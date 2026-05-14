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

package com.tunjid.demo.common.ui

import android.os.Debug
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

private const val TAG = "HeapDump"

@Composable
actual fun rememberHeapDumper(): () -> Unit {
    val context = LocalContext.current.applicationContext
    val packageName = context.packageName
    return remember(context) {
        {
            // Encourage the runtime to release anything it would release naturally,
            // so the dump shows objects that are actually retained rather than
            // objects that just happen to not have been collected yet.
            System.gc()
            System.runFinalization()
            System.gc()

            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "demoapp-${System.currentTimeMillis()}.hprof")
            try {
                Debug.dumpHprofData(file.absolutePath)
                val message = buildString {
                    appendLine("Wrote heap dump to ${file.absolutePath}")
                    appendLine("Pull with:")
                    append("  adb pull ${file.absolutePath}")
                    if (file.absolutePath.startsWith("/storage/emulated/0/Android/data/")) {
                        // Some shells block direct pulls from app-private external
                        // dirs; provide a run-as fallback that works on debug builds.
                        appendLine()
                        append("  # or: adb exec-out run-as $packageName cat ")
                        append(file.absolutePath.substringAfter("/Android/data/$packageName/"))
                        append(" > ${file.name}")
                    }
                }
                Log.i(TAG, message)
                println("[$TAG] $message")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to dump heap", t)
                println("[$TAG] Failed to dump heap: ${t.message}")
            }
        }
    }
}
