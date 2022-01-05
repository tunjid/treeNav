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

data class TestRoute(val name: String) : Route {
    override val id: String get() = name
}

class StackNavTest {

    private lateinit var subject: StackNav

    @BeforeTest
    fun setup() {
        subject = StackNav("subject")
    }

    @Test
    fun testPushing() {
        val changed = subject.push(TestRoute("A"))
        assertTrue { changed.current == TestRoute("A") }
        assertEquals(setOf(TestRoute("A")), changed - subject)
    }
}