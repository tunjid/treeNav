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

import com.tunjid.treenav.Route
import com.tunjid.treenav.strings.routeParserFrom
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

data class TestRoute(
    val name: String,
    val queryParams: Map<String, List<String>> = mapOf()
) : Route {
    override val id: String get() = name
}

class UrlRouteMatcherTest {

    @Test
    fun testSimpleUrlRouteMatching() {
        val routeParser = routeParserFrom(
            urlRouteMatcher(routePattern = "users/{id}") { params ->
                TestRoute(name = params.pathArgs.getValue("id"))
            }
        )
        val route = routeParser.parse("users/jeff")

        assertEquals(
            expected = "jeff",
            actual = route?.name
        )
    }

    @Test
    fun testSimpleUrlRouteMatchingWithQueryParams() {
        val routeParser = routeParserFrom(
            urlRouteMatcher(routePattern = "users/{id}") { params ->
                TestRoute(
                    name = params.pathArgs.getValue("id"),
                    queryParams = params.queryArgs
                )
            }
        )
        val route = routeParser.parse("users/jeff?age=27&job=dev&hobby=running&hobby=reading")

        assertEquals(
            expected = "jeff",
            actual = route?.name
        )
        assertEquals(
            expected = "27",
            actual = route?.queryParams?.get("age")?.first()
        )
        assertEquals(
            expected = "dev",
            actual = route?.queryParams?.get("job")?.first()
        )
        assertEquals(
            expected = listOf("running", "reading"),
            actual = route?.queryParams?.get("hobby")
        )
    }
}
