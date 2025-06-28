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

                implementation(libs.androidx.collection)
                implementation(libs.androidx.navigation.event)

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.foundation.layout)

                implementation(libs.jetbrains.lifecycle.runtime)
                implementation(libs.jetbrains.lifecycle.runtime.compose)
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.jetbrains.lifecycle.viewmodel.compose)

//                implementation(libs.androidx.navigation3)
                implementation(libs.jetbrains.savedstate.compose)

            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
//                implementation(libs.androidx.viewmodel.navigation3)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        all {
            languageSettings.apply {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.FlowPreview")
            }
        }
    }
}
