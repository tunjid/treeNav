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
 * A trie for looking up paths by segments
 */
internal class TrieNode<T> {
    val children: MutableMap<String, TrieNode<T>> = HashMap()
    var item: T? = null
}

internal fun <T> TrieNode<T>.insert(pattern: PathPattern, item: T) = insert(
    segments = pattern.segments,
    item = item,
    index = 0,
)

internal fun <T> TrieNode<T>.find(path: String): T? = find(
    segments = path.pathSegments(),
    index = 0,
)

private fun <T> TrieNode<T>.insert(segments: List<String>, item: T, index: Int) {
    if (index == segments.size) {
        this.item = item
        return
    }
    val segment = segments[index].asPathSegment
    if (segment !in children) children[segment] = TrieNode()

    children.getValue(segment).insert(
        segments = segments,
        item = item,
        index = index + 1
    )
}

private fun <T> TrieNode<T>.find(segments: List<String>, index: Int): T? {
    if (index == segments.size) return item
    val segment = segments[index]

    val child = children[segment]
        // Try matching against a parameter
        ?: children[PARAMETER_MATCHER]
        ?: return null

    return child.find(
        segments = segments,
        index = index + 1
    )
}

private fun String.pathSegments(): List<String> = split("?")
    .first()
    .split("/")
    .filter(String::isNotBlank)

private val String.asPathSegment
    get() = when {
        isPathParameter -> PARAMETER_MATCHER
        else -> this
    }

private val String.isPathParameter get() = first() == '{' && last() == '}'

private val PathPattern.segments get() = template.pathSegments()

private const val PARAMETER_MATCHER = "**PARAMETER**"
