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

package com.tunjid.treenav

/**
 * An immutable navigation [Node] whose children are represented with a stack like data structure
 */
data class StackNav(
    val name: String,
    override val children: List<Node> = listOf()
) : Node {
    override val id: String get() = name
}

/**
 * Swaps the top of the stack with the specified [Node]
 */
fun StackNav.swap(node: Node): StackNav =
    if (children.lastOrNull() == node) this
    else copy(children = children.dropLast(1) + node)

/**
 * Pushes the [node] unto the top of the navigation stack if [current] is not equal to [node]
 */
fun StackNav.push(node: Node): StackNav =
    if (children.lastOrNull() == node) this
    else copy(children = children + node)

/**
 * Pops the top node off if the stack is larger than 1 or [popLast] is true, otherwise it no ops
 */
fun StackNav.pop(popLast: Boolean = false) = when {
    children.size == 1 && !popLast -> this
    else -> copy(children = children.dropLast(1))
}

/**
 * Pops every node in this [StackNav] up until the last one.
 */
fun StackNav.popToRoot() = copy(
    children = children.take(1)
)

/**
 * Indicates if there's a [Node] available to pop up to
 */
val StackNav.canPop get() = children.size > 1

val StackNav.current get() = children.lastOrNull()
