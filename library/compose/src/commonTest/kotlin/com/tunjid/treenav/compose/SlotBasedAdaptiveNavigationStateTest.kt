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

import com.tunjid.treenav.Node
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.backStack
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.requireCurrent
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class TestNode(
    val name: String,
    override val children: List<TestNode> = emptyList(),
) : Node {
    override val id: String get() = name
}

enum class TestPane {
    One,
    Two,
    Three;
}

class SlotBasedAdaptiveNavigationStateTest {

    private lateinit var subject: SlotBasedPanedNavigationState<TestPane, TestNode>
    private lateinit var panes: List<TestPane>
    private lateinit var slots: Set<Slot>


    @BeforeTest
    fun setup() {
        panes = TestPane.entries.toList()
        slots = List(size = panes.size, init = ::Slot).toSet()
        subject = SlotBasedPanedNavigationState.initial(
            slots = slots
        )
    }

    @Test
    fun testFirstSinglePaneAdaptation() {
        subject.testAdaptTo(
            navState = EmptyNavState,
            panesToDestinations = mapOf(
                TestPane.One to TestNode(name = "A"),
            )
        )
            .apply {
                assertEquals(
                    expected = destinationFor(TestPane.One),
                    actual = TestNode(name = "A"),
                )
                assertEquals(
                    expected = adaptationsIn(TestPane.One),
                    actual = setOf(Adaptation.Change),
                )
                assertEquals(
                    expected = slotFor(TestPane.One),
                    actual = Slot(0),
                )
            }
    }

    @Test
    fun testFirstTriplePaneAdaptation() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Two to TestNode(name = "B"),
                    TestPane.Three to TestNode(name = "C"),
                )
            )
            .apply {
                // Primary
                assertEquals(
                    expected = destinationFor(TestPane.One),
                    actual = TestNode(name = "A")
                )
                assertEquals(
                    expected = adaptationsIn(TestPane.One),
                    actual = setOf(Adaptation.Change)
                )
                assertEquals(
                    expected = slotFor(TestPane.One),
                    actual = Slot(0)
                )

                // Secondary
                assertEquals(
                    expected = destinationFor(TestPane.Two),
                    actual = TestNode(name = "B")
                )
                assertEquals(
                    expected = adaptationsIn(TestPane.Two),
                    actual = setOf(Adaptation.Change)
                )
                assertEquals(
                    expected = slotFor(TestPane.Two),
                    actual = Slot(1)
                )

                // Tertiary
                assertEquals(
                    expected = destinationFor(TestPane.Three),
                    actual = TestNode(name = "C")
                )
                assertEquals(
                    expected = adaptationsIn(TestPane.Three),
                    actual = setOf(Adaptation.Change)
                )
                assertEquals(
                    expected = slotFor(TestPane.Three),
                    actual = Slot(2)
                )
            }
    }

    @Test
    fun testSameAdaptationInSinglePane() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                )
            )
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                )
            )
            .apply {
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.One),
                )
            }
    }

    @Test
    fun testSameAdaptationInTestPanes() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Two to TestNode(name = "B"),
                    TestPane.Three to TestNode(name = "C"),
                )
            )
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Two to TestNode(name = "B"),
                    TestPane.Three to TestNode(name = "C"),
                )
            )
            .apply {
                // Primary
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.One),
                )

                // Secondary
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(TestPane.Two),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(TestPane.Two),
                )
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(TestPane.Two),
                )

                // Tertiary
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(TestPane.Three),
                )
                assertEquals(
                    expected = setOf(Adaptation.Same),
                    actual = adaptationsIn(TestPane.Three),
                )
                assertEquals(
                    expected = Slot(2),
                    actual = slotFor(TestPane.Three),
                )
            }
    }

    @Test
    fun testChangeAdaptationInTestPanes() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Two to TestNode(name = "B"),
                    TestPane.Three to TestNode(name = "C"),
                )
            )
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "B"),
                    TestPane.Two to TestNode(name = "C"),
                    TestPane.Three to TestNode(name = "A"),
                )
            )
            .apply {
                // Primary
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.One, to = TestPane.Three),
                        Adaptation.Swap(from = TestPane.Two, to = TestPane.One),
                    ),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(TestPane.One),
                )

                // Secondary
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(TestPane.Two),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.Three, to = TestPane.Two),
                        Adaptation.Swap(from = TestPane.Two, to = TestPane.One),
                    ),
                    actual = adaptationsIn(TestPane.Two),
                )
                assertEquals(
                    expected = Slot(2),
                    actual = slotFor(TestPane.Two),
                )

                // Tertiary
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.Three),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.Three, to = TestPane.Two),
                        Adaptation.Swap(from = TestPane.One, to = TestPane.Three),
                    ),
                    actual = adaptationsIn(TestPane.Three),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.Three),
                )
            }
    }

    @Test
    fun testListToListDetailAdaptation() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                )
            )
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "B"),
                    TestPane.Two to TestNode(name = "A"),
                )
            )
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.Two),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.One, to = TestPane.Two),
                    ),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = setOf(Adaptation.Swap(TestPane.One, TestPane.Two)),
                    actual = adaptationsIn(TestPane.Two),
                )

                // Slot assertions
                assertEquals(
                    // Secondary should reuse slot 0
                    expected = Slot(0),
                    actual = slotFor(TestPane.Two),
                )
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(TestPane.One),
                )
            }
    }

    @Test
    fun testSinglePanePredictiveBackAdaptation() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                )
            )
            .apply {
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.One),
                )
            }
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "B"),
                )
            )
            .apply {
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.One),
                )
            }
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Three to TestNode(name = "B"),
                )
            )
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = TestNode(name = "B"),
                    actual = destinationFor(TestPane.Three),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(Adaptation.Swap(TestPane.One, TestPane.Three)),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = setOf(Adaptation.Swap(TestPane.One, TestPane.Three)),
                    actual = adaptationsIn(TestPane.Three),
                )

                // Slot assertions
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(TestPane.One),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.Three),
                )
            }
    }

    @Test
    fun testDoublePaneToSinglePanePredictiveBackAdaptation() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Two to TestNode(name = "B"),
                )
            )
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "C"),
                    TestPane.Three to TestNode(name = "A"),
                )
            )
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.Three),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.One, to = TestPane.Three),
                    ),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = setOf(Adaptation.Swap(TestPane.One, TestPane.Three)),
                    actual = adaptationsIn(TestPane.Three),
                )

                // Slot assertions
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(TestPane.One),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.Three),
                )
                assertEquals(
                    expected = null,
                    actual = slotFor(TestPane.Two),
                )
            }
    }

    @Test
    fun testDoublePaneToDoublePanePredictiveBackAdaptation() {
        subject
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "A"),
                    TestPane.Two to TestNode(name = "B"),
                )
            )
            .testAdaptTo(
                navState = EmptyNavState,
                panesToDestinations = mapOf(
                    TestPane.One to TestNode(name = "C"),
                    TestPane.Two to TestNode(name = "D"),
                    TestPane.Three to TestNode(name = "A"),
                )
            )
            .apply {
                // Destination assertions
                assertEquals(
                    expected = TestNode(name = "C"),
                    actual = destinationFor(TestPane.One),
                )
                assertEquals(
                    expected = TestNode(name = "D"),
                    actual = destinationFor(TestPane.Two),
                )
                assertEquals(
                    expected = TestNode(name = "A"),
                    actual = destinationFor(TestPane.Three),
                )

                // Adaptation assertions
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.One, to = TestPane.Three)
                    ),
                    actual = adaptationsIn(TestPane.One),
                )
                assertEquals(
                    expected = setOf(Adaptation.Change),
                    actual = adaptationsIn(TestPane.Two),
                )
                assertEquals(
                    expected = setOf(
                        Adaptation.Swap(from = TestPane.One, to = TestPane.Three)
                    ),
                    actual = adaptationsIn(TestPane.Three),
                )

                // Slot assertions
                assertEquals(
                    expected = Slot(1),
                    actual = slotFor(TestPane.One),
                )
                assertEquals(
                    expected = Slot(2),
                    actual = slotFor(TestPane.Two),
                )
                assertEquals(
                    expected = Slot(0),
                    actual = slotFor(TestPane.Three),
                )
            }
    }

    @Test
    fun testIsPop() {
        val navStates = (1..3).fold(listOf(StartNavState)) { navStateList, index ->
            navStateList + navStateList.last().push(TestNode(index.toString()))
        }

        subject = navStates.indices
            .fold(subject) { foldedSubject, index ->
                foldedSubject
                    .testAdaptTo(
                        navState = navStates[index],
                        panesToDestinations = mapOf(
                            TestPane.One to navStates[index].requireCurrent(),
                        )
                    )
                    .apply {
                        assertFalse(isPop)
                    }
            }

        val poppedNavStates = navStates.map { it.pop(popLast = true) }

        poppedNavStates.indices
            .reversed()
            .fold(subject) { foldedSubject, index ->
                foldedSubject
                    .testAdaptTo(
                        navState = poppedNavStates[index],
                        panesToDestinations = mapOf(
                            TestPane.One to navStates[index].requireCurrent(),
                        )
                    )
                    .apply {
                        assertTrue(isPop)
                    }
            }
    }

    private fun SlotBasedPanedNavigationState<TestPane, TestNode>.testAdaptTo(
        navState: StackNav,
        panesToDestinations: Map<TestPane, TestNode?>,
    ) = adaptTo(
        slots = slots,
        backStackIds = navState.backStack(
            includeCurrentDestinationChildren = true,
            placeChildrenBeforeParent = true,
        )
            .filterIsInstance<TestNode>()
            .map { it.id },
        panesToDestinations = panesToDestinations
    )
}

private val EmptyNavState = StackNav(
    name = "TestNavState",
    children = emptyList(),
)

private val StartNavState = EmptyNavState.push(TestNode("0"))