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
            .push(TestNode("A"))
            .push(TestNode("B"))
            .push(TestNode("C"))
            .switch(toIndex = 2)
            .push(TestNode("D"))
            .push(TestNode("E"))
            .switch(toIndex = 1)
            .push(TestNode("F"))

        assertEquals(
            expected = listOf(
                pushed,
                StackNav(
                    name = "0",
                    children = listOf("A", "B", "C").map(::TestNode)
                )
            )
                    + listOf("A", "B", "C").map(::TestNode)
                    + StackNav(
                name = "1",
                children = listOf("F").map(::TestNode)
            )
                    + listOf("F").map(::TestNode)
                    + StackNav(
                name = "2",
                children = listOf("D", "E").map(::TestNode)
            )
                    + listOf("D", "E").map(::TestNode),
            actual = pushed.flatten(order = Order.DepthFirst)
        )
    }

    @Test
    fun testFlattenBreathFirst() {
        val pushed = subject
            .push(TestNode("A"))
            .push(TestNode("B"))
            .push(TestNode("C"))
            .switch(toIndex = 2)
            .push(TestNode("D"))
            .push(TestNode("E"))
            .switch(toIndex = 1)
            .push(TestNode("F"))

        assertEquals(
            expected = listOf(
                pushed,
                StackNav(
                    name = "0",
                    children = listOf("A", "B", "C").map(::TestNode)
                ),
                StackNav(
                    name = "1",
                    children = listOf("F").map(::TestNode)
                ),
                StackNav(
                    name = "2",
                    children = listOf("D", "E").map(::TestNode)
                )
            )
                    + listOf("A", "B", "C").map(::TestNode)
                    + listOf("F").map(::TestNode)
                    + listOf("D", "E").map(::TestNode),
            actual = pushed.flatten(order = Order.BreadthFirst)
        )
    }

    @Test
    fun testPopping() {
        val pushed = subject
            .push(TestNode("A"))
            .push(TestNode("B"))
            .push(TestNode("C"))
            .switch(toIndex = 2)
            .push(TestNode("D"))
            .push(TestNode("E"))
            .switch(toIndex = 1)
            .push(TestNode("F"))

        // Should switch stack but leave last route on stack "2"
        assertEquals(
            expected = MultiStackNav(
                name = "subject",
                indexHistory = listOf(0, 2),
                currentIndex = 2,
                stacks = listOf(
                    StackNav(
                        name = "0",
                        children = listOf("A", "B", "C").map(::TestNode)
                    ),
                    StackNav(
                        name = "1",
                        children = listOf("F").map(::TestNode)
                    ),
                    StackNav(
                        name = "2",
                        children = listOf("D", "E").map(::TestNode)
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
                        children = listOf("A", "B", "C").map(::TestNode)
                    ),
                    StackNav(
                        name = "1",
                        children = listOf("F").map(::TestNode)
                    ),
                    StackNav(
                        name = "2",
                        children = listOf("D").map(::TestNode)
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
                        children = listOf("A", "B", "C").map(::TestNode)
                    ),
                    StackNav(
                        name = "1",
                        children = listOf("F").map(::TestNode)
                    ),
                    StackNav(
                        name = "2",
                        children = listOf("D").map(::TestNode)
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
                        children = listOf("A", "B").map(::TestNode)
                    ),
                    StackNav(
                        name = "1",
                        children = listOf("F").map(::TestNode)
                    ),
                    StackNav(
                        name = "2",
                        children = listOf("D").map(::TestNode)
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
            .push(TestNode(name = "A"))
            .push(TestNode(name = "B"))
            .push(TestNode(name = "C"))

        assertEquals(
            expected = listOf("A", "B", "D").map(::TestNode),
            actual = multiPush.swap(TestNode(name = "D"))
                .flatten(Order.DepthFirst)
                .filterIsInstance<TestNode>()
        )
    }

    @Test
    fun testSubtraction() {
        val multiPush = subject
            .push(TestNode(name = "A"))
            .push(TestNode(name = "B"))
            .push(TestNode(name = "C"))

        assertEquals(
            expected = listOf("A", "B", "C")
                .map(::TestNode)
                .toSet(),
            actual = (multiPush - subject)
                .filterIsInstance<TestNode>()
                .toSet()
        )
    }

    @Test
    fun testPoppingToRoot() {
        val multiPush = subject
            .push(TestNode(name = "A"))
            .push(TestNode(name = "B"))
            .push(TestNode(name = "C"))
            .copy(currentIndex = 1)
            .push(TestNode(name = "D"))
            .push(TestNode(name = "E"))
            .push(TestNode(name = "F"))

        assertEquals(
            expected = setOf(
                TestNode(name = "A"),
                TestNode(name = "B"),
                TestNode(name = "C"),
                TestNode(name = "D"),
            ),
            actual = multiPush
                .popToRoot()
                .minus(subject)
                .filterIsInstance<TestNode>()
                .toSet()
        )

        assertEquals(
            expected = setOf(
                TestNode(name = "A"),
                TestNode(name = "D"),
                TestNode(name = "E"),
                TestNode(name = "F"),
            ),
            actual = multiPush
                .copy(currentIndex = 0)
                .popToRoot()
                .minus(subject)
                .filterIsInstance<TestNode>()
                .toSet()
        )
    }
}
