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

import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("multiplatform") version "1.6.10"
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.6.10"
}

group = "com.tunjid.treenav"
version = "0.0.0-alpha01"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }


    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set(project.name)
                description.set("A kotlin multiplatform experiment for representing app navigation with tree like data structures")
                url.set("https://github.com/tunjid/treeNav")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/tunjid/treeNav/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("tunjid")
                        name.set("Adetunji Dahunsi")
                        email.set("tjdah100@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/tunjid/treeNav.git")
                    developerConnection.set("scm:git:ssh://github.com/tunjid/treeNav.git")
                    url.set("https://github.com/tunjid/treeNavtreeNav/tree/main")
                }
            }

        }
    }
    repositories {
        val localProperties = localProps()

        val publishUrl = localProperties.getProperty("publishUrl")
        if (publishUrl != null) {
            maven {
                name = localProperties.getProperty("repoName")
                url = uri(localProperties.getProperty("publishUrl"))
                credentials {
                    username = localProperties.getProperty("username")
                    password = localProperties.getProperty("password")
                }
            }
        }
    }
}

signing {
    val localProperties = localProps()

    val signingKey = localProperties.getProperty("signingKey")
    val signingPassword = localProperties.getProperty("signingPassword")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

fun localProps(): Properties = Properties().apply {
    file("local.properties").let { file ->
        if (file.exists()) load(FileInputStream(file))
    }
}
