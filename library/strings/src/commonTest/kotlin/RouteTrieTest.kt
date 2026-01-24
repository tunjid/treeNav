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

import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.toRouteTrie
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTrieTest {

    @Test
    fun testReTrieVal() {
        val patterns = listOf(
            "/users",
            "/users/{id}",
            "/users/{id}/photos/grid",
            "/users/{id}/photos/pager",
            "/home",
            "/settings",
            "/sign-in",
        ).map(::PathPattern)

        val routeTrie = patterns
            .mapIndexed { index, pattern ->
                pattern to index
            }
            .toMap()
            .toRouteTrie()

        assertEquals(
            expected = 0,
            actual = routeTrie[routeOf("users")],
            message = "users path should be found at index 0",
        )
        assertEquals(
            expected = 1,
            actual = routeTrie[routeOf("users/1")],
            message = "users path with parameter should be found at index 1",
        )
        assertEquals(
            expected = null,
            actual = routeTrie[routeOf("users/1/photos")],
            message = "users path with photos suffix parameter should not be found",
        )
        assertEquals(
            expected = 2,
            actual = routeTrie[routeOf("users/1/photos/grid")],
            message = "users path with parameter and photos and grid segments should be found at index 2",
        )
        assertEquals(
            expected = 3,
            actual = routeTrie[routeOf("users/1/photos/pager")],
            message = "users path with parameter and photos and pager segments should be found at index 3",

        )
        assertEquals(
            expected = null,
            actual = routeTrie[routeOf("users/1/photos/pager/oops")],
            message = "users path with 'o' parameter suffix should not be found",
        )
        assertEquals(
            expected = 3,
            actual = routeTrie[routeOf("users/1/photos/pager?page=0")],
            message = "users path with parameter and photos and pager segments with queries should be found at index 3",
        )
    }
}
