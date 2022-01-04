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

/**
 * A representation of a navigation node with child nodes [children]
 */
interface Node {
    val id: String

    val children: List<Node> get() = listOf()
}

/**
 * Recursively traverses each node in the tree with [this] as a parent
 */
 fun Node.traverse2( onNodeVisited: (Node) -> Unit) {
    if (children.isEmpty()) return onNodeVisited(this)
    for(child in children) child.traverse(onNodeVisited)
}

inline fun Node.traverse(crossinline onNodeVisited: (Node) -> Unit) {
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        // Pop off end of stack.
        val node = stack.removeAt(stack.lastIndex)
        // Do stuff.
        onNodeVisited(node)
        // Push other objects on the stack as needed.
        for(i in node.children.lastIndex downTo 0)  stack.add(node.children[i])
    }
}

/**
 * Returns a [List] of all [Node]s in the tree with [this] as a parent
 */
fun Node.flatten(): List<Node> {
    val result = mutableListOf<Node>()
    traverse(result::add)
    return result
}

operator fun Node.minus(node: Node): Set<Node> =
    (mutableSetOf<Node>().also { traverse(it::add) }) - (mutableSetOf<Node>().also { node.traverse(it::add) })

/**
 * A navigation node that represents a navigation destination on the screen. The UI representing
 * the screen is emitted via the [Render] function.
 */
interface Route : Node



