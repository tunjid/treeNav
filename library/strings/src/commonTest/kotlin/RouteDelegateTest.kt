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
import com.tunjid.treenav.strings.mappedRoutePath
import com.tunjid.treenav.strings.mappedRouteQuery
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.routeQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RouteDelegateTest {

    @Test
    fun testDelegateReading() {
        val route = routeOf(
            path = "users",
            pathArgs = mapOf(
                "userId" to "1",
            ),
            queryParams = mapOf(
                "page" to listOf("0"),
                "offset" to listOf("10"),
            ),
        )

        assertEquals(
            expected = 1,
            actual = route.userId,
        )

        assertEquals(
            expected = 0,
            actual = route.page,
        )

        assertEquals(
            expected = "10",
            actual = route.offset,
        )

        assertEquals(
            expected = 100,
            actual = route.count,
        )

        assertEquals(
            expected = "profilePictures",
            actual = route.category,
        )
    }

    @Test
    fun testDelegateDefaults() {
        val route = routeOf(
            path = "users",
        )

        assertEquals(
            expected = "2",
            actual = route.defaultProfileId,
        )
        assertEquals(
            expected = 20,
            actual = route.defaultPage,
        )

        assertEquals(
            expected = "200",
            actual = route.defaultOffset,
        )
    }

    @Test
    fun testDelegateExceptions() {
        val route = routeOf(
            path = "users",
        )

        assertFailsWith<NoSuchElementException> {
            route.userId
        }
        assertFailsWith<NoSuchElementException> {
            route.profileId
        }
        assertFailsWith<NoSuchElementException> {
            route.page
        }
        assertFailsWith<NoSuchElementException> {
            route.offset
        }

        assertEquals(
            expected = 100,
            actual = route.count,
        )

        assertEquals(
            expected = "profilePictures",
            actual = route.category,
        )
    }
}

private val Route.userId by mappedRoutePath(
    mapper = String::toInt,
)

private val Route.profileId by routePath()

private val Route.page by mappedRouteQuery(
    mapper = String::toInt,
)

private val Route.offset by routeQuery()

private val Route.count by optionalMappedRouteQuery(
    mapper = String::toInt,
    default = 100,
)

private val Route.category by optionalRouteQuery(
    default = "profilePictures",
)

private val Route.defaultProfileId by routePath("2")

private val Route.defaultPage by mappedRouteQuery(
    default = 20,
    mapper = String::toInt,
)

private val Route.defaultOffset by routeQuery(
    default = "200",
)
