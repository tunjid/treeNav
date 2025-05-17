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

package com.tunjid.treenav.compose.navigation3.decorators

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tunjid.treenav.compose.navigation3.NavEntryDecorator
import com.tunjid.treenav.compose.navigation3.navEntryDecorator

/** Returns a [MovableContentNavEntryDecorator] that is remembered across recompositions. */
@Composable
internal fun rememberMovableContentNavEntryDecorator(): NavEntryDecorator<Any> = remember {
    MovableContentNavEntryDecorator()
}

/**
 * A [NavEntryDecorator] that wraps each entry in a [movableContentOf] to allow nav displays to
 * arbitrarily place entries in different places in the composable call hierarchy and ensures that
 * the same entry content is not composed multiple times in different places of the hierarchy.
 *
 * This should likely be the first [NavEntryDecorator] to ensure that other [NavEntryDecorator]
 * calls that are stateful are moved properly inside the [movableContentOf].
 */
private fun MovableContentNavEntryDecorator(): NavEntryDecorator<Any> {
    val movableContentContentHolderMap: MutableMap<Any, MutableState<@Composable () -> Unit>> =
        mutableMapOf()
    val movableContentHolderMap: MutableMap<Any, @Composable () -> Unit> = mutableMapOf()
    return navEntryDecorator(
        onPop = {
            movableContentHolderMap.remove(it)
            movableContentContentHolderMap.remove(it)
        },
        decorator = { entry ->
            val key = entry.key
            movableContentContentHolderMap.getOrPut(key) {
                key(key) {
                    remember {
                        mutableStateOf(
                            @Composable {
                                error(
                                    "Should not be called, this should always be updated in" +
                                            "DecorateEntry with the real content"
                                )
                            }
                        )
                    }
                }
            }
            movableContentHolderMap.getOrPut(key) {
                key(key) {
                    remember {
                        movableContentOf {
                            // In case the key is removed from the backstack while this is still
                            // being rendered, we remember the MutableState directly to allow
                            // rendering it while we are animating out.
                            remember { movableContentContentHolderMap.getValue(key) }.value()
                        }
                    }
                }
            }

            key(key) {
                // In case the key is removed from the backstack while this is still
                // being rendered, we remember the MutableState directly to allow
                // updating it while we are animating out.
                val movableContentContentHolder = remember {
                    movableContentContentHolderMap.getValue(key)
                }
                // Update the state holder with the actual entry content
                movableContentContentHolder.value = { entry.content(key) }
                // In case the key is removed from the backstack while this is still
                // being rendered, we remember the movableContent directly to allow
                // rendering it while we are animating out.
                val movableContentHolder = remember { movableContentHolderMap.getValue(key) }
                // Finally, render the entry content via the movableContentOf
                movableContentHolder()
            }
        }
    )
}
