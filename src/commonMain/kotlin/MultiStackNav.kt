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
 * A compound immutable navigation [Node] whose [children] are comprised of independent [StackNav]
 * instances
 */
data class MultiStackNav(
    val name: String,
    val indexHistory: List<Int> = listOf(0),
    val currentIndex: Int = 0,
    val stacks: List<StackNav> = listOf()
) : Node {
    override val id: String get() = name
    override val children: List<Node> get() = stacks
}

/**
 * Switches out the [current] for [Route]
 */
fun MultiStackNav.swap(route: Route) = atCurrentIndex { swap(route) }

/**
 * Pushes [route] on top of the [StackNav] at [MultiStackNav.currentIndex]
 */
fun MultiStackNav.push(route: Route) = atCurrentIndex { push(route) }

/**
 * Tries to pop the active [StackNav]. If unsuccessful, it tries to go to the last
 * [MultiStackNav.currentIndex] before the active [MultiStackNav.currentIndex]
 */
fun MultiStackNav.pop() = when (val changed = atCurrentIndex(StackNav::pop)) {
    // There was nothing to pop, try switching the active index
    this -> indexHistory.dropLast(1).let { newIndexHistory ->
        when (val newIndex = newIndexHistory.lastOrNull()) {
            null -> this
            else -> copy(
                indexHistory = newIndexHistory,
                currentIndex = newIndex
            )
        }
    }
    else -> changed
}

/**
 * Switches the [MultiStackNav.currentIndex] to [toIndex]
 */
fun MultiStackNav.switch(toIndex: Int) = copy(
    currentIndex = toIndex,
    indexHistory = (indexHistory - toIndex) + toIndex
)

/**
 * Performs the given [operation] with the [StackNav] at [MultiStackNav.currentIndex]
 */
private fun MultiStackNav.atCurrentIndex(operation: StackNav.() -> StackNav) = copy(
    stacks = stacks.mapIndexed { index, stack ->
        if (index == currentIndex) operation(stack)
        else stack
    }
)

val MultiStackNav.current get() = stacks.getOrNull(currentIndex)?.routes?.lastOrNull()
