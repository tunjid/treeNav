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

import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeParserFrom
import com.tunjid.treenav.strings.routeString
import com.tunjid.treenav.strings.urlRouteMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

data class TestRoute(
    override val routeParams: RouteParams
) : Route()

class UrlRouteMatcherTest {

    @Test
    fun testSimpleUrlRouteMatching() {
        val routeParser = routeParserFrom(
            urlRouteMatcher(
                routePattern = "/users/{id}",
                routeMapper = ::TestRoute
            )
        )
        val route = routeParser.parse("/users/jeff")

        assertEquals(
            expected = "/users/jeff",
            actual = route?.id
        )
        assertEquals(
            expected = "jeff",
            actual = route?.routeParams?.pathArgs?.get("id")
        )
    }

    @Test
    fun testSimpleUrlRouteMatchingWithQueryParams() {
        val routeParser = routeParserFrom(
            urlRouteMatcher(
                routePattern = "/users/{id}",
                routeMapper = ::TestRoute
            )
        )
        val routeString = routeString(
            path = "/users/jeff",
            queryParams = mapOf(
                "age" to listOf("27"),
                "job" to listOf("dev"),
                "hobby" to listOf("running", "reading"),
            )
        )

        assertEquals(
            expected = "/users/jeff?age=27&job=dev&hobby=running&hobby=reading",
            actual = routeString,
        )

        val route = routeParser.parse(routeString)

        assertEquals(
            expected = "/users/jeff",
            actual = route?.id
        )
        assertEquals(
            expected = "jeff",
            actual = route?.routeParams?.pathArgs?.get("id")
        )
        assertEquals(
            expected = "27",
            actual = route?.routeParams?.queryParams?.get("age")?.first()
        )
        assertEquals(
            expected = "dev",
            actual = route?.routeParams?.queryParams?.get("job")?.first()
        )
        assertEquals(
            expected = listOf("running", "reading"),
            actual = route?.routeParams?.queryParams?.get("hobby")
        )
    }
}
