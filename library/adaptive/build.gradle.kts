plugins {
    kotlin("multiplatform")
    id("publishing-library-convention")
    id("kotlin-jvm-convention")
    id("maven-publish")
    signing
    id("org.jetbrains.dokka")
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
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
            baseName = "treenav-adaptive"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":library:treenav"))
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.foundation.layout)
                implementation(libs.jetbrains.compose.material3.adaptive)
            }
        }
        val jvmMain by getting
        val jvmTest by getting

        val jsMain by getting
        val jsTest by getting

        all {
            languageSettings.apply {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.FlowPreview")
            }
        }
    }
}
