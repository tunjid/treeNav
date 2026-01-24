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

import com.tunjid.treenav.Node

/**
 * A navigation node that represents a navigation destination on the screen.
 */
interface Route : Node {
    val routeParams: RouteParams

    override val id: String
        get() = routeParams.pathAndQueries.split("?").first()
}

/**
 * Class holding pertinent information for a [String] representation of a [Route]
 */
class RouteParams(
    /**
     * The path and query strings of a url concatenated
     */
    val pathAndQueries: String,
    /**
     * Arguments for path variables in the string
     */
    val pathArgs: Map<String, String>,
    /**
     * Arguments for query parameters in the string
     */
    val queryParams: Map<String, List<String>>,
)

fun routeString(
    path: String,
    queryParams: Map<String, List<String>>,
) = when {
    queryParams.isEmpty() -> path
    else -> {
        val queryParameters = queryParams.entries.joinToString(separator = "&") { (key, values) ->
            values.joinToString(separator = "&") { value ->
                "$key=$value"
            }
        }
        "$path?$queryParameters"
    }
}

/**
 * Creates a [Route] containing the specified [params] and [children].
 */
fun routeOf(
    params: RouteParams,
    children: List<Node> = emptyList(),
): Route = BasicRoute(
    routeParams = params,
    children = children,
)

/**
 * Creates a [Route] by combining the [path], [pathArgs] and [queryParams]
 * into [RouteParams] with the specified [children].
 */
@Suppress("unused")
fun routeOf(
    path: String,
    pathArgs: Map<String, String> = emptyMap(),
    queryParams: Map<String, List<String>> = emptyMap(),
    children: List<Node> = listOf(),
): Route = BasicRoute(
    path = path,
    pathArgs = pathArgs,
    queryParams = queryParams,
    children = children,
)

/**
 * Basic route definition
 */
private class BasicRoute(
    override val routeParams: RouteParams,
    override val children: List<Node> = emptyList(),
) : Route {

    constructor(
        path: String,
        pathArgs: Map<String, String> = emptyMap(),
        queryParams: Map<String, List<String>> = emptyMap(),
        children: List<Node> = emptyList(),
    ) : this(
        routeParams = RouteParams(
            pathAndQueries = path,
            pathArgs = pathArgs,
            queryParams = queryParams,
        ),
        children = children,
    )

    override fun toString(): String = id
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
