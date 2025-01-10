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

package com.tunjid.treenav.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal fun rememberPanedSaveableStateHolder(): SaveableStateHolder =
    rememberSaveable(
        saver = PanedSavableStateHolder.Saver
    ) {
        PanedSavableStateHolder()
    }.apply {
        parentSaveableStateRegistry = LocalSaveableStateRegistry.current
    }

private class PanedSavableStateHolder(
    private val savedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableStateMapOf(),
) : SaveableStateHolder {
    private val registryHolders = mutableStateMapOf<Any, RegistryHolder>()
    var parentSaveableStateRegistry: SaveableStateRegistry? = null

    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        ReusableContent(key) {
            val registryHolder = remember {
                require(parentSaveableStateRegistry?.canBeSaved(key) ?: true) {
                    "Type of the key $key is not supported. On Android you can only use types " +
                            "which can be stored inside the Bundle."
                }
                // With multiple panes co-existing, its possible for an existing destination
                // to have a new registryHolder created in this remember block as it enters
                // a new pane before onDispose is called in the DisposableEffect of the old pane,
                // yet somehow before the DisposableEffect block that
                // calls 'require(key !in registryHolders)' called.

                // This makes sure that state is saved a little earlier so the incoming block
                // sees saved state.
                registryHolders[key]?.saveTo(savedStates)
                RegistryHolder(key)
            }
            CompositionLocalProvider(
                LocalSaveableStateRegistry provides registryHolder.registry,
                content = content,
            )
            DisposableEffect(Unit) {
                require(key !in registryHolders) { "Key $key was used multiple times " }
                savedStates -= key
                registryHolders[key] = registryHolder
                onDispose {
                    registryHolder.saveTo(savedStates)
                    registryHolders -= key
                }
            }
        }
    }

    private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
        val map = savedStates.toMutableMap()
        registryHolders.values.forEach { it.saveTo(map) }
        return map.ifEmpty { null }
    }

    override fun removeState(key: Any) {
        val registryHolder = registryHolders[key]
        if (registryHolder != null) {
            registryHolder.shouldSave = false
        } else {
            savedStates -= key
        }
    }

    inner class RegistryHolder(
        val key: Any,
    ) {
        var shouldSave = true
        val registry: SaveableStateRegistry = SaveableStateRegistry(savedStates[key]?.toMap()) {
            parentSaveableStateRegistry?.canBeSaved(it) ?: true
        }

        fun saveTo(map: MutableMap<Any, Map<String, List<Any?>>>) {
            if (shouldSave) {
                val savedData = registry.performSave()
                if (savedData.isEmpty()) {
                    map -= key
                } else {
                    map[key] = savedData
                }
            }
        }
    }

    companion object {
        val Saver: Saver<PanedSavableStateHolder, *> = Saver(
            save = { it.saveAll() },
            restore = { PanedSavableStateHolder(it) }
        )
    }
}
