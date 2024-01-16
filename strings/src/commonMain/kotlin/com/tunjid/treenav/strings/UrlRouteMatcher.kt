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
 * Matches route [String] representations into concrete [Route] instances
 */
interface UrlRouteMatcher<out T : Route> {
    val pattern: PathPattern
    fun route(params: RouteParams): T
}

/**
 * Creates a [RouteParser] from a list of [UrlRouteMatcher]s.
 * Note that the order of [UrlRouteMatcher]s matter; place more specific matchers first.
 */
fun <T : Route> routeParserFrom(
    vararg matchers: UrlRouteMatcher<T>
): RouteParser<T> = object : RouteParser<T> {

    private val rootTrieNode = matchers.fold(
        TrieNode<UrlRouteMatcher<T>>()
    ) { routeNode, matcher ->
        routeNode.insert(
            pattern = matcher.pattern,
            item = matcher,
        )
        routeNode
    }

    override fun parse(routeString: String): T? {
        val matcher = rootTrieNode.find(
            path = routeString,
        ) ?: return null

        return matcher.route(
            routeString.routeParams(matcher.pattern)
        )
    }
}

/**
 * Convenience function for creating a [UrlRouteMatcher] for parsing
 * URL-like [String] representations of [Route]s
 */
fun <T : Route> urlRouteMatcher(
    routePattern: String,
    routeMapper: (RouteParams) -> T
) = object : UrlRouteMatcher<T> {

    override val pattern = PathPattern(routePattern)

    override fun route(params: RouteParams): T = routeMapper(params)
}
