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

package com.tunjid.treenav.compose

import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.backStack
import com.tunjid.treenav.push
import com.tunjid.treenav.switch
import kotlin.test.Test
import kotlin.test.assertEquals

class StackNavExtTest {

    @Test
    fun testStackNavMultiPaneDisplayBackstack() {
        val subject = StackNav(name = "subject")

        val pushed = subject
            .push(TestNode("A", children = listOf(TestNode("1"))))
            .push(TestNode("B"))
            .push(TestNode("C"))
            .push(TestNode("D"))
            .push(TestNode("E", children = listOf(TestNode("1"), TestNode("2"))))
            .push(TestNode("F"))

        assertEquals(
            expected = pushed.backStack(
                includeCurrentDestinationChildren = true,
                placeChildrenBeforeParent = true,
                distinctDestinations = false,
            ),
            actual = pushed.multiPaneDisplayBackstack(),
        )
    }

    @Test
    fun testMultiStackNavMultiPaneDisplayBackstack() {
        val subject = MultiStackNav(
            name = "subject",
            stacks = listOf("0", "1", "2").map(::StackNav),
        )

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
            expected = pushed.backStack(
                includeCurrentDestinationChildren = true,
                placeChildrenBeforeParent = true,
                distinctDestinations = false,
            ),
            actual = pushed.multiPaneDisplayBackstack(),
        )
    }
}
