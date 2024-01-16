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

import kotlin.jvm.JvmInline

@JvmInline
value class PathPattern(
    val template: String
) {
    init {
        checkNotNull(pathPatternRegex.matchEntire(template)) {
            "The pattern provided \"$template\" must be in the format of a URL path"
        }
    }
}

internal fun String.routeParams(
    pattern: PathPattern
) = RouteParams(
    route = this,
    pathArgs = pathArgs(pattern.template),
    queryParams = queryParams(),
)

/**
 * Optimistically parses url query parameters from a string.
 * Output is nonsensical for malformed urls.
 */
private fun String.queryParams(): Map<String, List<String>> {
    val queryString = split("?").last()
    val result = mutableMapOf<String, List<String>>()
    val pairs = queryString.split("&")
    pairs.forEach { keyValueString ->
        val keyValueArray = keyValueString.split("=")
        if (keyValueArray.size != 2) return@forEach
        val (key, value) = keyValueArray
        result[key] = result.getOrPut(key, ::listOf) + value
    }
    return result
}


private fun String.pathArgs(pattern: String): Map<String, String> {
    val path = split("?").first()
    val pathKeys = mutableListOf<String>()

    val basicPattern = pattern.replace(regex = pathRegex) {
        pathKeys.add(
            it.value.replace(
                regex = pathArgRegex,
                replacement = ""
            )
        )
        "(.*?)"
    }

    val result = basicPattern
        .toRegex()
        .matchEntire(input = path) ?: return emptyMap()

    return pathKeys
        .zip(result.groupValues.drop(1))
        .toMap()
}

// Android's icu regex implementation needs the escape
@Suppress("RegExpRedundantEscape")
private val pathRegex = "\\{(.*?)\\}".toRegex()
private val pathArgRegex = "[{}]".toRegex()

@Suppress("RegExpRedundantEscape")
private val pathPatternRegex = "^\\/[/.a-zA-Z0-9-{}*]+\$".toRegex()