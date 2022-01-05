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
 * A navigation node that represents a navigation destination on the screen.
 */
interface Route : Node

sealed class Order {
    object BreadthFirst : Order()
    object DepthFirst : Order()
}

/**
 * Traverses each node in the tree with [this] as a parent
 */

inline fun Node.traverse(order: Order, crossinline onNodeVisited: (Node) -> Unit) = when(order){
    Order.BreadthFirst -> bfs(onNodeVisited)
    Order.DepthFirst -> dfs(onNodeVisited)
}

inline fun Node.dfs(crossinline onNodeVisited: (Node) -> Unit) {
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        // Pop off end of stack.
        val node = stack.removeAt(stack.lastIndex)
        // Visit node.
        onNodeVisited(node)
        // Push child nodes on the stack.
        for (i in node.children.lastIndex downTo 0) stack.add(node.children[i])
    }
}

inline fun Node.bfs(crossinline onNodeVisited: (Node) -> Unit) {
    val queue = mutableListOf(this)
    while (queue.isNotEmpty()) {
        // deque.
        val node = queue.removeAt(0)
        // Visit node.
        onNodeVisited(node)
        // Push child nodes on the queue.
        for (element in node.children) queue.add(element)
    }
}

/**
 * Returns a [List] of all [Node]s in the tree with [this] as a parent
 */
fun Node.flatten(order: Order): List<Node> {
    val result = mutableListOf<Node>()
    traverse(order, result::add)
    return result
}

operator fun Node.minus(node: Node): Set<Node> {
    val left = mutableSetOf<Node>().let { set ->
        this.dfs(set::add)
        set - this
    }
    val right = mutableSetOf<Node>().let { set ->
        node.dfs(set::add)
        set - node
    }
    return left - right
}



