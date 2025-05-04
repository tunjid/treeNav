plugins {
    kotlin("multiplatform")
    id("publishing-library-convention")
    id("android-library-convention")
    id("kotlin-jvm-convention")
    id("kotlin-library-convention")
    id("maven-publish")
    signing
    id("org.jetbrains.dokka")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
}

android {
    buildFeatures {
        compose = true
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":library:treenav"))
                implementation(project(":library:compose"))

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.foundation.layout)
            }
        }
    }
}

