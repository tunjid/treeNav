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
        subject.testAdaptTo(
            panesToNodes = mapOf(
                ThreePane.Primary to TestNode(name = "A"),
            )
        )
            .apply {
                assertEquals(
                    expected = destinationFor(ThreePane.Primary),
                    actual = TestNode(name = "A"),
                )
                assertEquals(
                    expected = adaptationsIn(ThreePane.Primary),
                    actual = setOf(Adaptation.Change),
                )
                assertEquals(
                    expected = slotFor(ThreePane.Primary),
                    actual = Slot(0),
                )
            }
    }

    @Test
    fun testFirstTriplePaneAdaptation() {
        subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.Secondary to TestNode(name = "B"),
                    ThreePane.Tertiary to TestNode(name = "C"),
                )
            )
            .apply {
                // Primary
                assertEquals(
                    expected = destinationFor(ThreePane.Primary),
                    actual = TestNode(name = "A")
                )
                assertEquals(
                    expected = adaptationsIn(ThreePane.Primary),
                    actual = setOf(Adaptation.Change)
                )
                assertEquals(
                    expected = slotFor(ThreePane.Primary),
                    actual = Slot(0)
                )

                // Secondary
                assertEquals(
                    expected = destinationFor(ThreePane.Secondary),
                    actual = TestNode(name = "B")
                )
                assertEquals(
                    expected = adaptationsIn(ThreePane.Secondary),
                    actual = setOf(Adaptation.Change)
                )
                assertEquals(
                    expected = slotFor(ThreePane.Secondary),
                    actual = Slot(1)
                )

                // Tertiary
                assertEquals(
                    expected = destinationFor(ThreePane.Tertiary),
                    actual = TestNode(name = "C")
                )
                assertEquals(
                    expected = adaptationsIn(ThreePane.Tertiary),
                    actual = setOf(Adaptation.Change)
                )
                assertEquals(
                    expected = slotFor(ThreePane.Tertiary),
                    actual = Slot(2)
                )
            }
    }

    @Test
    fun testSameAdaptationInSinglePane() {
        subject
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
            .apply {
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.Primary),
                )
            }
    }

    @Test
    fun testSameAdaptationInThreePanes() {
        subject
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
            .apply {
                // Primary
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.Primary),
                )

                // Secondary
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(ThreePane.Secondary),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(ThreePane.Secondary),
                )
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(ThreePane.Secondary),
                )

                // Tertiary
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(ThreePane.Tertiary),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(ThreePane.Tertiary),
                )
                assertEquals(
                    expected = Slot(2),
                    actual = slotFor(ThreePane.Tertiary),
                )
            }
    }

    @Test
    fun testChangeAdaptationInThreePanes() {
        subject
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
            .apply {
                // Primary
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Primary, to = ThreePane.Tertiary),
                        Adaptation.Swap(from = ThreePane.Secondary, to = ThreePane.Primary),
                    ),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(ThreePane.Primary),
                )

                // Secondary
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(ThreePane.Secondary),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Tertiary, to = ThreePane.Secondary),
                        Adaptation.Swap(from = ThreePane.Secondary, to = ThreePane.Primary),
                    ),
                    actual = adaptationsIn(ThreePane.Secondary),
                )
                assertEquals(
                    expected = Slot(2),
                    actual = slotFor(ThreePane.Secondary),
                )

                // Tertiary
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.Tertiary),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Tertiary, to = ThreePane.Secondary),
                        Adaptation.Swap(from = ThreePane.Primary, to = ThreePane.Tertiary),
                    ),
                    actual = adaptationsIn(ThreePane.Tertiary),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.Tertiary),
                )
            }
    }

    @Test
    fun testListToListDetailAdaptation() {
        subject
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
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.Secondary),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Primary, to = ThreePane.Secondary),
                    ),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(ThreePane.PrimaryToSecondary),
                    actual = adaptationsIn(ThreePane.Secondary),
                )

                // Slot assertions
                assertEquals(
                    // Secondary should reuse slot 0
                    expected = Slot(0),
                    actual = slotFor(ThreePane.Secondary),
                )
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(ThreePane.Primary),
                )
            }
    }

    @Test
    fun testSinglePanePredictiveBackAdaptation() {
        subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                )
            )
            .apply {
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.Primary),
                )
            }
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "B"),
                )
            )
            .apply {
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.Primary),
                )
            }
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.TransientPrimary to TestNode(name = "B"),
                )
            )
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(ThreePane.TransientPrimary),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(ThreePane.PrimaryToTransient),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(ThreePane.PrimaryToTransient),
                    actual = adaptationsIn(ThreePane.TransientPrimary),
                )

                // Slot assertions
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.TransientPrimary),
                )
            }
    }

    @Test
    fun testDoublePaneToSinglePanePredictiveBackAdaptation() {
        subject
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
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.TransientPrimary),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Primary, to = ThreePane.TransientPrimary),
                    ),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(ThreePane.PrimaryToTransient),
                    actual = adaptationsIn(ThreePane.TransientPrimary),
                )

                // Slot assertions
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.TransientPrimary),
                )
                assertEquals(
                    expected = null,
                    actual = slotFor(ThreePane.Secondary),
                )
            }
    }

    @Test
    fun testDoublePaneToDoublePanePredictiveBackAdaptation() {
        subject
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "A"),
                    ThreePane.Secondary to TestNode(name = "B"),
                )
            )
            .testAdaptTo(
                panesToNodes = mapOf(
                    ThreePane.Primary to TestNode(name = "C"),
                    ThreePane.Secondary to TestNode(name = "D"),
                    ThreePane.TransientPrimary to TestNode(name = "A"),
                )
            )
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = TestNode(name = "D"),
                    actual = destinationFor(ThreePane.Secondary),
                )
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(ThreePane.TransientPrimary),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Primary, to = ThreePane.TransientPrimary)
                    ),
                    actual = adaptationsIn(ThreePane.Primary),
                )
                assertEquals(
                    expected = setOf(Adaptation.Change),
                    actual = adaptationsIn(ThreePane.Secondary),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = ThreePane.Primary, to = ThreePane.TransientPrimary)
                    ),
                    actual = adaptationsIn(ThreePane.TransientPrimary),
                )

                // Slot assertions
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(ThreePane.Primary),
                )
                assertEquals(
                    expected = Slot(2),
                    actual = slotFor(ThreePane.Secondary),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(ThreePane.TransientPrimary),
                )
            }
    }

    private fun SlotBasedAdaptiveNavigationState<ThreePane, TestNode>.testAdaptTo(
        panesToNodes: Map<ThreePane, TestNode>
    ) = adaptTo(
        slots = slots,
        backStackIds = emptySet(),
        panesToNodes = panesToNodes
    )
}

