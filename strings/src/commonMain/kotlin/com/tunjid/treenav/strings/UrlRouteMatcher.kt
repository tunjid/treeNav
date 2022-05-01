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

import com.tunjid.treenav.Route

/**
 * Matches route [String] representations into concrete [Route] instances
 */
interface UrlRouteMatcher<out T : Route> {
    val patterns: List<String>
    val pathKeys: List<String>
    fun route(params: RouteParams): T
}

/**
 * Creates a [RouteParser] from a list of [UrlRouteMatcher]s.
 * Note that the order of [UrlRouteMatcher]s matter; place more specific matchers first.
 */
fun <T : Route> routeParserFrom(
    vararg parsers: UrlRouteMatcher<T>
): RouteParser<T> = object : RouteParser<T> {
    val regexMap = parsers.fold(mapOf<Regex, UrlRouteMatcher<T>>()) { map, parser ->
        map + parser.patterns.map { Regex(it) to parser }
    }

    override fun parse(routeString: String): T? {
        val regex = regexMap.keys.firstOrNull { it.matches(input = routeString) } ?: return null
        val result = regex.matchEntire(input = routeString) ?: return null

        val routeParser = regexMap.getValue(regex)
        return routeParser.route(
            RouteParams(
                route = routeString,
                pathArgs = routeParser.pathKeys
                    .zip(result.groupValues.drop(1))
                    .toMap(),
                queryArgs = result.groupValues.lastOrNull()?.queryParams() ?: mapOf(),
            )
        )
    }
}

/**
 * Convenience function for creating a [UrlRouteMatcher] for parsing URL-like [String] representations of [Route]s
 */
fun <T : Route> urlRouteMatcher(
    routePattern: String,
    routeMapper: (RouteParams) -> T
) = object : UrlRouteMatcher<T> {
    init {
        pathKeys = mutableListOf()
        val basicPattern = routePattern.replace(regex = pathRegex) {
            pathKeys.add(
                it.value.replace(
                    regex = pathArgRegex,
                    replacement = ""
                )
            )
            "(.*?)"
        }
        val queryPattern = "$basicPattern\\?(.*?)"
        // Match the query pattern first
        patterns = listOf(queryPattern, basicPattern)
    }

    override val pathKeys: List<String>

    override val patterns: List<String>

    override fun route(params: RouteParams): T = routeMapper(params)
}

// Android's icu regex implementation needs the escape
@Suppress("RegExpRedundantEscape")
private val pathRegex = "\\{(.*?)\\}".toRegex()
private val pathArgRegex = "[{}]".toRegex()

/**
 * Optimistically parses url query parameters from a string.
 * Output is nonsensical for malformed urls.
 */
private fun String.queryParams(): Map<String, List<String>> {
    val result = mutableMapOf<String, List<String>>()
    val pairs = split("&")
    pairs.forEach { keyValueString ->
        val keyValueArray = keyValueString.split("=")
        if (keyValueArray.size != 2) return@forEach
        val (key, value) = keyValueArray
        result[key] = result.getOrPut(key, ::listOf) + value
    }
    return result
}

