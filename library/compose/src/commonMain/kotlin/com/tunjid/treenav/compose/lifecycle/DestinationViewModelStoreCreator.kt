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

package com.tunjid.treenav.compose.lifecycle

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.Order
import com.tunjid.treenav.traverse

/**
 * A class that lazily loads a [ViewModelStoreOwner] for each destination in the navigation graph.
 * Each unique destination can only have a single [ViewModelStore], regardless of how many times
 * it appears in the navigation graph, or its depth at any point.
 */
@Stable
internal class DestinationViewModelStoreCreator(
    private val validNodeIdsReader: () -> Set<String>
) {
    private val nodeIdsToViewModelStoreOwner = mutableStateMapOf<String, ViewModelStoreOwner>()

    /**
     * Creates a [ViewModelStoreOwner] for a given [Node]
     */
    fun viewModelStoreOwnerFor(
        node: Node
    ): ViewModelStoreOwner {
        val existingIds = validNodeIdsReader()
        check(existingIds.contains(node.id)) {
            """
                Attempted to create a ViewModelStoreOwner for Node $node, but the Node is not
                present in the navigation tree
            """.trimIndent()
        }
        return nodeIdsToViewModelStoreOwner.getOrPut(
            node.id
        ) {
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }
        }
    }

    fun clearStoreFor(childNode: Node) {
        val existingIds = validNodeIdsReader()
        childNode.traverse(Order.BreadthFirst) {
            if (!existingIds.contains(it.id)) {
                nodeIdsToViewModelStoreOwner.remove(it.id)
                    ?.viewModelStore
                    ?.clear()
            }
        }
    }
}