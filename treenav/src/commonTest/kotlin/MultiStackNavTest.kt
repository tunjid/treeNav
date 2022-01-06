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

import com.tunjid.treenav.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

class MultiStackNavTest {

    private lateinit var subject: MultiStackNav

    @BeforeTest
    fun setup() {
        subject = MultiStackNav(
            name = "subject",
            stacks = listOf("0", "1", "2").map(::StackNav)
        )
    }

    @Test
    fun testFlattenDepthFirst() {
        val pushed = subject
            .push(TestRoute("A"))
            .push(TestRoute("B"))
            .push(TestRoute("C"))
            .switch(toIndex = 2)
            .push(TestRoute("D"))
            .push(TestRoute("E"))
            .switch(toIndex = 1)
            .push(TestRoute("F"))

        assertEquals(
            expected = listOf(
                pushed,
                StackNav(
                    name = "0",
                    routes = listOf("A", "B", "C").map(::TestRoute)
                )
            )
                + listOf("A", "B", "C").map(::TestRoute)
                + StackNav(
                name = "1",
                routes = listOf("F").map(::TestRoute)
            )
                + listOf("F").map(::TestRoute)
                + StackNav(
                name = "2",
                routes = listOf("D", "E").map(::TestRoute)
            )
                + listOf("D", "E").map(::TestRoute),
            actual = pushed.flatten(order = Order.DepthFirst)
        )
    }

    @Test
    fun testFlattenBreathFirst() {
        val pushed = subject
            .push(TestRoute("A"))
            .push(TestRoute("B"))
            .push(TestRoute("C"))
            .switch(toIndex = 2)
            .push(TestRoute("D"))
            .push(TestRoute("E"))
            .switch(toIndex = 1)
            .push(TestRoute("F"))

        assertEquals(
            expected = listOf(
                pushed,
                StackNav(
                    name = "0",
                    routes = listOf("A", "B", "C").map(::TestRoute)
                ),
                StackNav(
                    name = "1",
                    routes = listOf("F").map(::TestRoute)
                ),
                StackNav(
                    name = "2",
                    routes = listOf("D", "E").map(::TestRoute)
                )
            )
                + listOf("A", "B", "C").map(::TestRoute)
                + listOf("F").map(::TestRoute)
                + listOf("D", "E").map(::TestRoute),
            actual = pushed.flatten(order = Order.BreadthFirst)
        )
    }

    @Test
    fun testPopping() {
        val pushed = subject
            .push(TestRoute("A"))
            .push(TestRoute("B"))
            .push(TestRoute("C"))
            .switch(toIndex = 2)
            .push(TestRoute("D"))
            .push(TestRoute("E"))
            .switch(toIndex = 1)
            .push(TestRoute("F"))

        // Should switch stack but leave last route on stack "2"
        assertEquals(
            expected = MultiStackNav(
                name = "subject",
                indexHistory = listOf(0, 2),
                currentIndex = 2,
                stacks = listOf(
                    StackNav(
                        name = "0",
                        routes = listOf("A", "B", "C").map(::TestRoute)
                    ),
                    StackNav(
                        name = "1",
                        routes = listOf("F").map(::TestRoute)
                    ),
                    StackNav(
                        name = "2",
                        routes = listOf("D", "E").map(::TestRoute)
                    )
                )
            ),
            actual = pushed
                .pop()
        )

        // Should pop "E" off stack 2
        assertEquals(
            expected = MultiStackNav(
                name = "subject",
                indexHistory = listOf(0, 2),
                currentIndex = 2,
                stacks = listOf(
                    StackNav(
                        name = "0",
                        routes = listOf("A", "B", "C").map(::TestRoute)
                    ),
                    StackNav(
                        name = "1",
                        routes = listOf("F").map(::TestRoute)
                    ),
                    StackNav(
                        name = "2",
                        routes = listOf("D").map(::TestRoute)
                    )
                )
            ),
            actual = pushed
                .pop()
                .pop()
        )

        // Should switch to stack 0
        assertEquals(
            expected = MultiStackNav(
                name = "subject",
                indexHistory = listOf(0),
                currentIndex = 0,
                stacks = listOf(
                    StackNav(
                        name = "0",
                        routes = listOf("A", "B", "C").map(::TestRoute)
                    ),
                    StackNav(
                        name = "1",
                        routes = listOf("F").map(::TestRoute)
                    ),
                    StackNav(
                        name = "2",
                        routes = listOf("D").map(::TestRoute)
                    )
                )
            ),
            actual = pushed
                .pop()
                .pop()
                .pop()
        )

        // Should pop off stack "0"
        assertEquals(
            expected = MultiStackNav(
                name = "subject",
                currentIndex = 0,
                indexHistory = listOf(0),
                stacks = listOf(
                    StackNav(
                        name = "0",
                        routes = listOf("A", "B").map(::TestRoute)
                    ),
                    StackNav(
                        name = "1",
                        routes = listOf("F").map(::TestRoute)
                    ),
                    StackNav(
                        name = "2",
                        routes = listOf("D").map(::TestRoute)
                    )
                )
            ),
            actual = pushed
                .pop()
                .pop()
                .pop()
                .pop()
        )
    }

    @Test
    fun testSwapping() {
        val multiPush = subject
            .push(TestRoute(name = "A"))
            .push(TestRoute(name = "B"))
            .push(TestRoute(name = "C"))

        assertEquals(
            expected = listOf("A", "B", "D").map(::TestRoute),
            actual = multiPush.swap(TestRoute(name = "D"))
                .flatten(Order.DepthFirst)
                .filterIsInstance<TestRoute>()
        )
    }

    @Test
    fun testSubtraction() {
        val multiPush = subject
            .push(TestRoute(name = "A"))
            .push(TestRoute(name = "B"))
            .push(TestRoute(name = "C"))

        assertEquals(
            expected = listOf("A", "B", "C")
                .map(::TestRoute)
                .toSet(),
            actual = (multiPush - subject)
                .filterIsInstance<Route>()
                .toSet()
        )
    }
}
