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
import kotlin.test.assertTrue

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

data class TestNode(val name: String) : Node {
    override val id: String get() = name
}

class StackNavTest {

    private lateinit var subject: StackNav

    @BeforeTest
    fun setup() {
        subject = StackNav(name = "subject")
    }

    @Test
    fun testFlatten() {
        val pushed = subject
            .push(TestNode(name = "A"))
            .push(TestNode(name = "B"))
            .push(TestNode(name = "C"))

        assertEquals(
            expected = listOf(pushed) + listOf("A", "B", "C").map(::TestNode),
            actual = pushed.flatten(order = Order.DepthFirst)
        )
    }

    @Test
    fun testPushing() {
        val singlePush = subject.push(TestNode(name = "A"))
        assertTrue { singlePush.current == TestNode(name = "A") }
        assertEquals(setOf(TestNode(name = "A")), singlePush - subject)

        val multiPush = subject
            .push(TestNode(name = "A"))
            .push(TestNode(name = "B"))
            .push(TestNode(name = "C"))

        assertTrue { multiPush.current == TestNode(name = "C") }

        assertEquals(
            expected = setOf(TestNode(name = "A")),
            actual = singlePush - subject
        )
        assertEquals(
            expected = listOf("A", "B", "C").map(::TestNode).toSet(),
            actual = multiPush - subject
        )
        assertEquals(
            expected = listOf("B", "C").map(::TestNode).toSet(),
            actual = multiPush - singlePush
        )
    }

    @Test
    fun testPopping() {
        val multiPush = subject
            .push(TestNode(name = "A"))
            .push(TestNode(name = "B"))
            .push(TestNode(name = "C"))

        assertTrue { multiPush.current == TestNode(name = "C") }

        assertEquals(
            expected = listOf(TestNode(name = "A"), TestNode(name = "B")),
            actual = multiPush
                .pop()
                .children
        )
        assertEquals(
            expected = listOf(TestNode(name = "A")),
            actual = multiPush
                .pop()
                .pop()
                .children
        )
        assertEquals(
            expected = listOf(TestNode(name = "A")),
            actual = multiPush
                .pop()
                .pop()
                .pop()
                .children
        )
        assertEquals(
            expected = listOf(),
            actual = multiPush
                .pop()
                .pop()
                .pop(popLast = true)
                .children
        )
    }
}
