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

plugins {
    kotlin("multiplatform")
    id("publishing-library-convention")
    id("kotlin-jvm-convention")
    id("maven-publish")
    signing
    id("org.jetbrains.dokka")
}

kotlin {
    applyDefaultHierarchyTemplate()
    js(IR) {
        nodejs()
        browser()
    }
    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "treenav"
            isStatic = true
        }
    }
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()
    tvosSimulatorArm64()
    watchosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":library:treenav"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting

        val jsMain by getting
        val jsTest by getting
    }
}
