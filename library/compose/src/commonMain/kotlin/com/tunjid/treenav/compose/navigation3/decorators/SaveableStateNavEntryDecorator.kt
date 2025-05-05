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

import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.treenav.compose.navigation3.NavEntry
import com.tunjid.treenav.compose.navigation3.NavEntryDecorator

/**
 * Wraps the content of a [NavEntry] with a [SaveableStateHolder.SaveableStateProvider] to ensure
 * that calls to [rememberSaveable] within the content work properly and that state can be saved.
 *
 * This [NavEntryDecorator] is the only one that is **required** as saving state is considered a
 * non-optional feature.
 */
internal object SaveableStateNavEntryDecorator : NavEntryDecorator {

    @Composable
    override fun DecorateBackStack(backStack: List<Any>, content: @Composable () -> Unit) {
        val localInfo = remember { SaveableStateNavLocalInfo() }
        DisposableEffect(key1 = backStack) { onDispose { localInfo.refCount.clear() } }

        localInfo.savedStateHolder = rememberPanedSaveableStateHolder()
        backStack.forEachIndexed { index, key ->
            // We update here as part of composition to ensure the value is available to
            // DecorateEntry
            localInfo.refCount.getOrPut(key) { LinkedHashSet<Int>() }.add(getIdForKey(key, index))
            DisposableEffect(key1 = key) {
                // We update here at the end of composition in case the backstack changed and
                // everything was cleared.
                localInfo.refCount
                    .getOrPut(key) { LinkedHashSet<Int>() }
                    .add(getIdForKey(key, index))
                onDispose {
                    // If the backStack count is less than the refCount for the key, remove the
                    // state since that means we removed a key from the backstack, and set the
                    // refCount to the backstack count.
                    val backstackCount = backStack.count { it == key }
                    val lastKeyCount = localInfo.refCount[key]?.size ?: 0
                    if (backstackCount < lastKeyCount) {
                        // The set of the ids associated with this key
                        @Suppress("PrimitiveInCollection") // The order of the element matters
                        val idsSet = localInfo.refCount[key]!!
                        val id = idsSet.last()
                        idsSet.remove(id)
                        if (!localInfo.idsInComposition.contains(id)) {
                            localInfo.savedStateHolder!!.removeState(id)
                        }
                    }
                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key]?.isEmpty() == true) {
                        localInfo.refCount.remove(key)
                    }
                }
            }
        }

        CompositionLocalProvider(LocalSaveableStateNavLocalInfo provides localInfo) {
            content.invoke()
        }
    }

    @Composable
    public override fun <T : Any> DecorateEntry(entry: NavEntry<T>) {
        val localInfo = LocalSaveableStateNavLocalInfo.current
        val key = entry.key
        // Tracks whether the key is changed
        var keyChanged = false
        var id: Int =
            rememberSaveable(key) {
                keyChanged = true
                localInfo.refCount[key]!!.last()
            }
        id =
            rememberSaveable(localInfo.refCount[key]?.size) {
                // if the key changed, use the current id
                // If the key was not changed, and the current id is not in composition or on the
                // back
                // stack then update the id with the last item from the backstack with the
                // associated
                // key. This ensures that we can handle duplicates, both consecutive and
                // non-consecutive
                if (
                    !keyChanged &&
                    (!localInfo.idsInComposition.contains(id) ||
                            localInfo.refCount[key]?.contains(id) == true)
                ) {
                    localInfo.refCount[key]!!.last()
                } else {
                    id
                }
            }
        keyChanged = false
        DisposableEffect(key1 = key) {
            localInfo.idsInComposition.add(id)
            onDispose {
                if (localInfo.idsInComposition.remove(id) && !localInfo.refCount.contains(key)) {
                    localInfo.savedStateHolder!!.removeState(id)
                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key]?.isEmpty() == true) {
                        localInfo.refCount.remove(key)
                    }
                }
            }
        }

        localInfo.savedStateHolder?.SaveableStateProvider(id) { entry.content.invoke(key) }
    }
}

internal val LocalSaveableStateNavLocalInfo =
    staticCompositionLocalOf<SaveableStateNavLocalInfo> {
        error(
            "CompositionLocal LocalSaveableStateNavLocalInfo not present. You must call " +
                    "DecorateBackStack before calling DecorateEntry."
        )
    }

internal class SaveableStateNavLocalInfo {
    internal var savedStateHolder: SaveableStateHolder? = null
    internal val refCount: MutableMap<Any, LinkedHashSet<Int>> = mutableMapOf()
    @Suppress("PrimitiveInCollection") // The order of the element matters
    internal val idsInComposition: LinkedHashSet<Int> = LinkedHashSet<Int>()
}

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
    private val savedStates: MutableMap<Any, Map<String, List<Any?>>> = mutableMapOf()
) : SaveableStateHolder {
    private val registries = mutableScatterMapOf<Any, SaveableStateRegistry>()
    var parentSaveableStateRegistry: SaveableStateRegistry? = null
    private val canBeSaved: (Any) -> Boolean = {
        parentSaveableStateRegistry?.canBeSaved(it) ?: true
    }

    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        ReusableContent(key) {
            val registry = remember {
                require(canBeSaved(key)) {
                    "Type of the key $key is not supported. On Android you can only use types " +
                            "which can be stored inside the Bundle."
                }
                // With multiple panes co-existing, its possible for an existing destination
                // to have a new registryHolder created in this remember block as it enters
                // a new pane before onDispose is called in the DisposableEffect of the old pane,
                // yet somehow before the DisposableEffect block that
                // calls 'require(key !in registryHolders)' called.

                // This makes sure that state is saved a little earlier so the incoming block
                registries[key]?.saveTo(savedStates, key)
                SaveableStateRegistry(savedStates[key], canBeSaved)
            }
            CompositionLocalProvider(
                LocalSaveableStateRegistry provides registry,
                content = content
            )
            DisposableEffect(Unit) {
                require(key !in registries) { "Key $key was used multiple times " }
                savedStates -= key
                registries[key] = registry
                onDispose {
                    if (registries.remove(key) === registry) {
                        registry.saveTo(savedStates, key)
                    }
                }
            }
        }
    }

    private fun saveAll(): MutableMap<Any, Map<String, List<Any?>>>? {
        val map = savedStates
        registries.forEach { key, registry -> registry.saveTo(map, key) }
        return map.ifEmpty { null }
    }

    override fun removeState(key: Any) {
        if (registries.remove(key) == null) {
            savedStates -= key
        }
    }

    private fun SaveableStateRegistry.saveTo(
        map: MutableMap<Any, Map<String, List<Any?>>>,
        key: Any
    ) {
        val savedData = performSave()
        if (savedData.isEmpty()) {
            map -= key
        } else {
            map[key] = savedData
        }
    }

    companion object {
        val Saver: Saver<PanedSavableStateHolder, *> =
            Saver(save = { it.saveAll() }, restore = { PanedSavableStateHolder(it) })
    }
}
