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
 * Class holding pertinent information for a [String] representation of a [Route]
 */
data class RouteParams(
    /**
     * The full route represented as a string
     */
    val route: String,
    /**
     * Arguments for path variables in the string
     */
    val pathArgs: Map<String, String>,
    /**
     * Arguments for query parameters in the string
     */
    val queryArgs: Map<String, List<String>>,
)

fun interface RouteParser<T: Route> {
    fun parse(routeString: String): T?
}
