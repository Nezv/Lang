import java.util.Properties

// Android app: phone-first GUI (bottom tabs, swipeable lesson pages, thumb-zone buttons).
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

fun localSetting(envName: String, propertyName: String): String {
    val fromEnv = System.getenv(envName)
    if (!fromEnv.isNullOrBlank()) return fromEnv.trim()
    val file = rootProject.file("secrets.properties")
    if (!file.isFile) return ""
    val props = Properties().apply { file.inputStream().use { load(it) } }
    return props.getProperty(propertyName, "").trim()
}

android {
    namespace = "app.lang.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Placeholder id: must be final before the first Play Store upload (it can never change).
        applicationId = "app.lang"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.2.0"

        // Private-beta AI tutor key: from the environment or the gitignored secrets.properties, compiled
        // into the build output only (never into source). Proper protection comes in Beta 2.
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localSetting("ANTHROPIC_API_KEY", "anthropic.api.key")}\"")
        buildConfigField("String", "TUTOR_MODEL", "\"${localSetting("LANG_TUTOR_MODEL", "tutor.model")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.designsystem)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
