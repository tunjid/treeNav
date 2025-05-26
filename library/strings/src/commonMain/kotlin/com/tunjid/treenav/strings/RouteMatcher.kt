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
 * Matches a specific [pattern] to a specific [Route].
 */
interface RouteMatcher {

    /**
     * The [pattern] describing the general form of the [Route] to match.
     */
    val pattern: PathPattern

    /**
     * Creates a [Route] from the parsed parameter parameters.
     */
    fun route(params: RouteParams): Route
}

/**
 * Parses urls to create [Route] instances.
 */
fun interface RouteParser {

    /**
     * Given a url [pathAndQueries] defining a navigation [Route], this method attempts to create
     * the matching [Route] for it, otherwise it returns null.
     */
    fun parse(pathAndQueries: String): Route?
}

/**
 * Creates a [RouteParser] from a list of [RouteMatcher]s.
 * Note that the order of [RouteMatcher]s matter; place more specific matchers first.
 */
fun routeParserFrom(
    vararg matchers: RouteMatcher
): RouteParser = object : RouteParser {

    private val rootTrieNode = matchers.fold(
        TrieNode<RouteMatcher>()
    ) { routeNode, matcher ->
        routeNode.insert(
            pattern = matcher.pattern,
            item = matcher,
        )
        routeNode
    }

    override fun parse(pathAndQueries: String): Route? {
        val matcher = rootTrieNode.find(
            path = pathAndQueries,
        ) ?: return null

        return matcher.route(
            pathAndQueries.routeParams(matcher.pattern)
        )
    }
}

/**
 * Convenience function for creating a [RouteMatcher] for parsing
 * URL-like [String] representations of [Route]s
 */
fun urlRouteMatcher(
    routePattern: String,
    routeMapper: (RouteParams) -> Route
) = object : RouteMatcher {

    override val pattern = PathPattern(routePattern)

    override fun route(params: RouteParams): Route = routeMapper(params)
}
