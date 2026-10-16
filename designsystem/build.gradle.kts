import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Small shared look-and-feel kit: brand colours, tone colours and Chinese-text widgets
// (characters with pinyin on top, word meanings, chat bubbles). Screens and navigation
// are NOT here — each app arranges these pieces in its own platform-specific GUI.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    android {
        namespace = "app.lang.designsystem"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            api(libs.compose.material.icons.extended)
        }
    }
}
