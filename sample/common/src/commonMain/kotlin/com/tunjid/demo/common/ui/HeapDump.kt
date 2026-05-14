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

/**
 * Returns a callback that, when invoked, forces a GC then writes a heap dump for
 * later analysis with `shark-cli` (or Android Studio's profiler).
 *
 * The callback prints (via `println`) the absolute path it wrote to, plus an
 * `adb pull` hint on Android, so the path is visible in logcat / the terminal.
 * On iOS the callback is a no-op aside from a log line — JVM heap dumps don't
 * apply to the Kotlin/Native runtime.
 *
 * Intended for debugging memory retention issues (e.g. predictive-back leak
 * triage). Not safe for production: the dump is large, blocks the calling
 * thread, and writes unencrypted heap contents to disk.
 */
@Composable
expect fun rememberHeapDumper(): () -> Unit
