/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":library:treenav"))
                implementation(project(":library:strings"))
                implementation(project(":library:compose"))
                implementation(project(":library:compose-threepane"))

                implementation(compose.components.resources)

                implementation(libs.androidx.navigation.event)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.foundation.layout)
                implementation(libs.compose.multiplatform.ui.backhandler)

                implementation(libs.lifecycle.multiplatform.runtime)
                implementation(libs.lifecycle.multiplatform.runtime.compose)
                implementation(libs.lifecycle.multiplatform.viewmodel)
                implementation(libs.lifecycle.multiplatform.viewmodel.compose)

                implementation(libs.compose.multiplatform.material.icons.core)
                implementation(libs.compose.multiplatform.material.icons.extended)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
                implementation(libs.tunjid.composables)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.compose.animation)
            }
        }
    }
}
