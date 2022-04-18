# TreeNav

[![JVM Tests](https://github.com/tunjid/treeNav/actions/workflows/tests.yml/badge.svg)](https://github.com/tunjid/treeNav/actions/workflows/tests.yml)
![TreeNav](https://img.shields.io/maven-central/v/com.tunjid.treenav/treenav?label=treenav)

Please note, this is not an official Google repository. It is a Kotlin multiplatform experiment that makes no guarantees
about API stability or long term support. None of the works presented here are production tested, and should not be
taken as anything more than its face value.

## Introduction

TreeNav is a kotlin multiplatform experiment for representing app navigation with tree like data structures. This
library is merely a declaration of interfaces and well tested types. Integration is open ended and left to the consuming
application. An example of such an implementation can be found [here](https://github.com/tunjid/me).

The basic type defined is the `Node` interface.

```kotlin
interface Node {
    val id: String

    val children: List<Node> get() = listOf()
}
```

`Nodes` may be:

* `Routes`: Simple destinations that represent a UI
* Parent `Nodes`: `Nodes` with specific navigation semantics, the library currently offers 2:
    * `StackNav`: Offers push pop semantics for stack based navigation.
        * `push`: Adds a `Route` to the `Node` by pushing a `Route` unto the stack.
        * `pop`: Removes a `Route` from the `Node` by popping it off the stack.
        * `swap`: Replaces a `Route` in the `Node` by switching out the top of the stack with it.
    * `MultiStackNav`: A compound `Node` comprising of multiple `StackNav` instances with the added convenience of being
      able to switch stacks as well as the behaviors from `StackNav` via proxy.
        * `switch`: Replaces the active index in `Node` by switching out the context of the aforementioned operations to
          the stack indicated by the index.

The above mentioned types are immutable, each action generates a new `Node` with the effects of the action applied.
Consuming applications typically observe the effects of the actions applied and react to the current `Node` of the
lowest branch of the tree.

The above makes it easy to represent the following navigation structure:

```
root_multi_stack_nav
├─ stack_nav_0/
│  ├─ article_list
│  ├─ article_detail_a
│  ├─ article_author_a
├─ stack_nav_1/
│  ├─ user_profile
│  ├─ edit_profile
├─ stack_nav_2
│  ├─ favorites
```

## Strings module

In multi-module applications, navigating between routes in other modules with strongly typed `Route` instances becomes a
challenge as one needs an instance of a `Route` from the other module in order to navigate to it. This cyclic dependency
issue can be avoided if `Route`s had canonical string representations.

To facilitate this, the `strings` module provides the `RouteParser` type, a functional interface declared as:

```kotlin
fun interface RouteParser<T : Route> {
    fun parse(routeString: String): T?
}
```

Which allows for the dynamic resolution of strings as `Route`s. A basic factory function for a `RouteParser` is provided
with the following:

```kotlin
fun <T : Route> routeParserFrom(
    vararg parsers: UrlRouteMatcher<T>
): RouteParser<T>
```

Where a `UrlRouteMatcher` extracts path and query parameters from url like strings. An example of its use is:

```kotlin
val routeParser = routeParserFrom(
    urlRouteMatcher(routePattern = "users/search") { params ->
        UserSearchRoute(
            query = params.queryArgs["query"]?.firstOrNull(),
            limit = params.queryArgs["limit"]?.firstOrNull(),
            filterIds = params.queryArgs["filterIds"]
        )
    },
    urlRouteMatcher(routePattern = "users/{id}") { params ->
        UserRoute(
            id = params.pathArgs.getValue("id"),
        )
    },
)
```

Note that the order of declaration of `UrlRouteMatcher` instances matter. For `UserSearchRoute`to be matched, it has to
be declared before the `UserRoute` as the `UserRoute` pattern also matches `UserSearchRoute`
pattern.


An example of this pattern being used to anchor the navigation for a multi-module KMP app, checkout the
[Me](https://github.com/tunjid/me/blob/main/common/feature-archive-edit/src/commonMain/kotlin/com/tunjid/me/archiveedit/FeatureDef.kt)
Kotlin multiplatform sample app.

## License

    Copyright 2021 Google LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
