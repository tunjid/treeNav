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

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class RouteDelegate<T>(
    private val isOptional: Boolean,
    private val isPath: Boolean,
    private val default: T? = null,
    private val mapper: (String) -> T,
) : ReadOnlyProperty<Route, T> {
    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(
        thisRef: Route,
        property: KProperty<*>
    ): T = when (val value = when {
        isPath -> thisRef.routeParams.pathArgs[property.name]
        else -> thisRef.routeParams.queryParams[property.name]?.firstOrNull()
    }) {
        null -> when {
            isOptional -> default
            else -> default ?: throw NoSuchElementException(
                "There is no value for the property '${property.name}' in the Route's ${if (isPath) "path arguments" else "query parameters"}."
            )
        }

        else -> mapper(value)
    } as T
}

fun routePath(
    default: String? = null
): ReadOnlyProperty<Route, String> = RouteDelegate(
    isOptional = false,
    isPath = true,
    default = default,
    mapper = { it }
)

fun <T> mappedRoutePath(
    default: T? = null,
    mapper: (String) -> T,
): ReadOnlyProperty<Route, T> = RouteDelegate(
    isOptional = false,
    isPath = true,
    default = default,
    mapper = mapper
)

fun routeQuery(
    default: String? = null
): ReadOnlyProperty<Route, String> = RouteDelegate(
    isOptional = false,
    isPath = false,
    default = default,
    mapper = { it }
)

fun <T> mappedRouteQuery(
    default: T? = null,
    mapper: (String) -> T,
): ReadOnlyProperty<Route, T> = RouteDelegate(
    isOptional = false,
    isPath = false,
    default = default,
    mapper = mapper
)

fun optionalRouteQuery(
    default: String? = null
): ReadOnlyProperty<Route, String?> = RouteDelegate(
    isOptional = true,
    isPath = false,
    default = default,
    mapper = { it }
)

fun <T> optionalMappedRouteQuery(
    default: T? = null,
    mapper: (String) -> T,
): ReadOnlyProperty<Route, T?> = RouteDelegate(
    isOptional = true,
    isPath = false,
    default = default,
    mapper = mapper
)