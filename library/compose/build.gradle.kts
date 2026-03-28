plugins {
    kotlin("multiplatform")
    id("publishing-library-convention")
    id("kotlin-jvm-convention")
    id("kotlin-library-convention")
    id("maven-publish")
    signing
    id("org.jetbrains.dokka")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":library:treenav"))

                implementation(libs.androidx.collection)
                implementation(libs.navigation.multiplatform.ui)
                implementation(libs.navigation.event.multiplatform.compose)

                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.foundation)
                implementation(libs.compose.multiplatform.foundation.layout)

                implementation(libs.lifecycle.multiplatform.runtime)
                implementation(libs.lifecycle.multiplatform.runtime.compose)

                implementation(libs.savedstate.multiplatform.compose)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
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
