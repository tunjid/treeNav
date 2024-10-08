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

package com.tunjid.treenav.adaptive

import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.threepane.ThreePane
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

data class TestNode(val name: String) : Node {
    override val id: String get() = name
}

class SlotBasedAdaptiveNavigationStateTest {

    private lateinit var subject: SlotBasedAdaptiveNavigationState<ThreePane, TestNode>
    private lateinit var panes: List<ThreePane>
    private lateinit var slots: Set<Slot>


    @BeforeTest
    fun setup() {
        panes = ThreePane.entries.toList()
        slots = List(size = panes.size, init = ::Slot).toSet()
        subject = SlotBasedAdaptiveNavigationState.initial(
            slots = slots
        )
    }

    @Test
    fun testFirstSinglePaneAdaptation() {
        val adapted = subject.testAdaptTo(
            panesToNodes = mapOf(
                ThreePane.Primary to TestNode(name = "A"),
            )
        )
        assertEquals(
            expected = adapted.destinationFor(ThreePane.Primary),
            actual = TestNode(name = "A"),
        )
        assertEquals(
            expected = adapted.adaptationIn(ThreePane.Primary),
            actual = Adaptation.Change,
        )
        assertEquals(
            expected = adapted.slotFor(ThreePane.Primary),
            actual = Slot(0),
        )
    }

    @Test
    fun testFirstTriplePaneAdaptation() {
        val adapted = subject.testAdaptTo(
            panesToNodes = mapOf(
                ThreePane.Primary to TestNode(name = "A"),
                ThreePane.Secondary to TestNode(name = "B"),
                ThreePane.Tertiary to TestNode(name = "C"),
            )
        )
        // Primary
        assertEquals(
            expected = adapted.destinationFor(ThreePane.Primary),
            actual = TestNode(name = "A")
        )
        assertEquals(
            expected = adapted.adaptationIn(ThreePane.Primary),
            actual = Adaptation.Change
        )
        assertEquals(
            expected = adapted.slotFor(ThreePane.Primary),
            actual = Slot(0)
        )

        // Secondary
        assertEquals(
            expected = adapted.destinationFor(ThreePane.Secondary),
            actual = TestNode(name = "B")
        )
        assertEquals(
            expected = adapted.adaptationIn(ThreePane.Secondary),
            actual = Adaptation.Change
        )
        assertEquals(
            expected = adapted.slotFor(ThreePane.Secondary),
            actual = Slot(1)
        )

        // Tertiary
        assertEquals(
            expected = adapted.destinationFor(ThreePane.Tertiary),
            actual = TestNode(name = "C")
        )
        assertEquals(
            expected = adapted.adaptationIn(ThreePane.Tertiary),
            actual = Adaptation.Change
        )
        assertEquals(
            expected = adapted.slotFor(ThreePane.Tertiary),
            actual = Slot(2)
        )
    }

    @Test
    fun testSameAdaptationInSinglePane() {
        val adapted = subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                )
            )
        assertEquals(
            expected = TestNode(name = "A"),
            actual = adapted.destinationFor(ThreePane.Primary),
        )
        assertEquals(
            expected = Adaptation.Same,
            actual = adapted.adaptationIn(ThreePane.Primary),
        )
        assertEquals(
            expected = Slot(0),
            actual = adapted.slotFor(ThreePane.Primary),
        )
    }

    @Test
    fun testSameAdaptationInThreePanes() {
        val adapted = subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.Secondary to TestNode(name = "B"),
                    ThreePane.Tertiary to TestNode(name = "C"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.Secondary to TestNode(name = "B"),
                    ThreePane.Tertiary to TestNode(name = "C"),
                )
            )
        // Primary
        assertEquals(
            expected = TestNode(name = "A"),
            actual = adapted.destinationFor(ThreePane.Primary),
        )
        assertEquals(
            expected = Adaptation.Same,
            actual = adapted.adaptationIn(ThreePane.Primary),
        )
        assertEquals(
            expected = Slot(0),
            actual = adapted.slotFor(ThreePane.Primary),
        )

        // Secondary
        assertEquals(
            expected = TestNode(name = "B"),
            actual = adapted.destinationFor(ThreePane.Secondary),
        )
        assertEquals(
            expected = Adaptation.Same,
            actual = adapted.adaptationIn(ThreePane.Secondary),
        )
        assertEquals(
            expected = Slot(1),
            actual = adapted.slotFor(ThreePane.Secondary),
        )

        // Tertiary
        assertEquals(
            expected = TestNode(name = "C"),
            actual = adapted.destinationFor(ThreePane.Tertiary),
        )
        assertEquals(
            expected = Adaptation.Same,
            actual = adapted.adaptationIn(ThreePane.Tertiary),
        )
        assertEquals(
            expected = Slot(2),
            actual = adapted.slotFor(ThreePane.Tertiary),
        )
    }

    @Test
    fun testChangeAdaptationInThreePanes() {
        val adapted = subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.Secondary to TestNode(name = "B"),
                    ThreePane.Tertiary to TestNode(name = "C"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "B"),
                    ThreePane.Secondary to TestNode(name = "C"),
                    ThreePane.Tertiary to TestNode(name = "A"),
                )
            )
        // Primary
        assertEquals(
            expected = TestNode(name = "B"),
            actual = adapted.destinationFor(ThreePane.Primary),
        )
        assertEquals(
            expected = Adaptation.Change,
            actual = adapted.adaptationIn(ThreePane.Primary),
        )
        assertEquals(
            expected = Slot(0),
            actual = adapted.slotFor(ThreePane.Primary),
        )

        // Secondary
        assertEquals(
            expected = TestNode(name = "C"),
            actual = adapted.destinationFor(ThreePane.Secondary),
        )
        assertEquals(
            expected = Adaptation.Change,
            actual = adapted.adaptationIn(ThreePane.Secondary),
        )
        assertEquals(
            expected = Slot(1),
            actual = adapted.slotFor(ThreePane.Secondary),
        )

        // Tertiary
        assertEquals(
            expected = TestNode(name = "A"),
            actual = adapted.destinationFor(ThreePane.Tertiary),
        )
        assertEquals(
            expected = Adaptation.Change,
            actual = adapted.adaptationIn(ThreePane.Tertiary),
        )
        assertEquals(
            expected = Slot(2),
            actual = adapted.slotFor(ThreePane.Tertiary),
        )
    }

    @Test
    fun testListToListDetailAdaptation() {
        val adapted = subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "B"),
                    ThreePane.Secondary to TestNode(name = "A"),
                )
            )

        // Destination assertions
        assertEquals(
            expected = TestNode(name = "B"),
            actual = adapted.destinationFor(ThreePane.Primary),
        )
        assertEquals(
            expected = TestNode(name = "A"),
            actual = adapted.destinationFor(ThreePane.Secondary),
        )

        // Adaptation assertions
        assertEquals(
            expected = Adaptation.Change,
            actual = adapted.adaptationIn(ThreePane.Primary),
        )
        assertEquals(
            expected = ThreePane.PrimaryToSecondary,
            actual = adapted.adaptationIn(ThreePane.Secondary),
        )

        // Slot assertions
        assertEquals(
            // Secondary should reuse slot 0
            expected = Slot(0),
            actual = adapted.slotFor(ThreePane.Secondary),
        )
        assertEquals(
            expected = Slot(1),
            actual = adapted.slotFor(ThreePane.Primary),
        )
    }

    @Test
    fun testSinglePanePredictiveBackAdaptation() {
        val adapted = subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "B"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.TransientPrimary to TestNode(name = "B"),
                )
            )

        // Destination assertions
        assertEquals(
            expected = TestNode(name = "A"),
            actual = adapted.destinationFor(ThreePane.Primary),
        )
        assertEquals(
            expected = TestNode(name = "B"),
            actual = adapted.destinationFor(ThreePane.Secondary),
        )

        // Adaptation assertions
        assertEquals(
            expected = Adaptation.Change,
            actual = adapted.adaptationIn(ThreePane.Primary),
        )
        assertEquals(
            expected = ThreePane.PrimaryToTransient,
            actual = adapted.adaptationIn(ThreePane.TransientPrimary),
        )

        // Slot assertions
        assertEquals(
            // Secondary should reuse slot 0
            expected = Slot(0),
            actual = adapted.slotFor(ThreePane.Primary),
        )
        assertEquals(
            expected = Slot(1),
            actual = adapted.slotFor(ThreePane.TransientPrimary),
        )
    }

    @Test
    fun testDoublePaneToSinglePanePredictiveBackAdaptation() {
        val adapted = subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.Secondary to TestNode(name = "B"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "C"),
                    ThreePane.TransientPrimary to TestNode(name = "A"),
                )
            )

        // Destination assertions
        assertEquals(
            expected = TestNode(name = "C"),
            actual = adapted.destinationFor(ThreePane.Primary),
        )
        assertEquals(
            expected = TestNode(name = "A"),
            actual = adapted.destinationFor(ThreePane.TransientPrimary),
        )

        // Adaptation assertions
        assertEquals(
            expected = Adaptation.Change,
            actual = adapted.adaptationIn(ThreePane.Primary),
        )
        assertEquals(
            expected = ThreePane.PrimaryToTransient,
            actual = adapted.adaptationIn(ThreePane.TransientPrimary),
        )

        // Slot assertions
        assertEquals(
            // Secondary should reuse slot 0
            expected = Slot(0),
            actual = adapted.slotFor(ThreePane.Primary),
        )
        assertEquals(
            expected = Slot(1),
            actual = adapted.slotFor(ThreePane.TransientPrimary),
        )
        assertEquals(
            expected = null,
            actual = adapted.slotFor(ThreePane.Secondary),
        )
    }

    private fun SlotBasedAdaptiveNavigationState<ThreePane, TestNode>.testAdaptTo(
        panesToNodes: Map<ThreePane, TestNode>
    ) = adaptTo(
        slots = slots,
        backStackIds = emptySet(),
        panesToNodes = panesToNodes
    )
}

