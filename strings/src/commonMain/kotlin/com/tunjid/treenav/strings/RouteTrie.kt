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

package com.tunjid.treenav.strings

/**
 * A class for looking up items that match a certain path pattern by route
 */
@Suppress("unused")
class RouteTrie<T> {

    private val rootTrieNode = TrieNode<T>()

    operator fun set(pattern: PathPattern, item: T) = rootTrieNode.insert(
        pattern = pattern,
        item = item,
    )

    operator fun get(route: Route): T? = rootTrieNode.find(
        path = route.id,
    )
}