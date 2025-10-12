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

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.foundation.layout)

                implementation(libs.lifecycle.multiplatform.runtime)
                implementation(libs.lifecycle.multiplatform.runtime.compose)
                implementation(libs.lifecycle.multiplatform.viewmodel)
                implementation(libs.lifecycle.multiplatform.viewmodel.compose)

                implementation(libs.savedstate.multiplatform.compose)

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
