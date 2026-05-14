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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.lang.management.ManagementFactory

private const val TAG = "HeapDump"

@Composable
actual fun rememberHeapDumper(): () -> Unit = remember {
    {
        System.gc()
        System.gc()

        val workingDir = File(System.getProperty("user.dir") ?: ".")
        val file = File(workingDir, "demoapp-${System.currentTimeMillis()}.hprof")
        // shark-cli's path tracer requires android.os.Build; a JVM dump won't have
        // it. The dump is still usable via `shark-cli interactive` (instance/class
        // browsing) and via Eclipse MAT / VisualVM.
        try {
            val server = ManagementFactory.getPlatformMBeanServer()
            // Use ObjectName-based invocation to avoid importing the com.sun.*
            // type, which isn't part of standard Java but is available on
            // HotSpot / OpenJDK. live=true asks the VM to dump only reachable
            // objects, which is what we want for leak triage.
            server.invoke(
                javax.management.ObjectName("com.sun.management:type=HotSpotDiagnostic"),
                "dumpHeap",
                arrayOf(
                    file.absolutePath,
                    /* live = */
                    true,
                ),
                arrayOf(
                    "java.lang.String",
                    "boolean",
                ),
            )
            println("[$TAG] Wrote heap dump to ${file.absolutePath}")
        } catch (t: Throwable) {
            System.err.println("[$TAG] Failed to dump heap: ${t.message}")
            t.printStackTrace()
        }
    }
}
