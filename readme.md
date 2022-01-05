# TreeNav

[![JVM Tests](https://github.com/tunjid/treeNav/actions/workflows/tests.yml/badge.svg)](https://github.com/tunjid/treeNav/actions/workflows/tests.yml)
![TreeNav](https://img.shields.io/maven-central/v/com.tunjid.treeNav/treenav?label=treenav)

Please note, this is not an official Google repository. It is a Kotlin multiplatform experiment
that makes no guarantees about API stability or long term support. None of the works presented here
are production tested, and should not be taken as anything more than its face value.

## Introduction

TreeNav is a kotlin multiplatform experiment for representing app navigation with tree like data
structures. This library is merely a declaration of interfaces and well tested types. Integration is open ended and
left to the consuming application. An example of such an implementation can be found [here](https://github.com/tunjid/me).


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
    * `MultiStackNav`: A compound `Node` comprising of multiple `StackNav` instances with the added
    convenience of being able to switch stacks as well as the push and pop behaviors from `StackNav`.

The above makes it easy to represent the following navigation structure:

```
root_multi_stack_nav
├─ tab_1/
│  ├─ article_list
│  ├─ article_detail_a
│  ├─ article_author_a
├─ tab_2/
│  ├─ user_profile
│  ├─ edit_profile
├─ tab_3
│  ├─ favorites
```

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
