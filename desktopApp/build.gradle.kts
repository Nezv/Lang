import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Windows app: desktop-first GUI (sidebar, two-pane layouts, keyboard shortcuts like Anki).
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.designsystem)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "app.lang.desktop.MainKt"

        nativeDistributions {
            // The packaged app ships a trimmed Java runtime; these are the extra JDK modules it needs:
            // java.sql (SQLite driver), java.net.http + jdk.crypto.ec (HTTPS for the AI tutor), the rest
            // from `gradlew :desktopApp:suggestRuntimeModules`. A missing one shows "Failed to launch JVM".
            modules("java.instrument", "java.management", "java.net.http", "java.sql", "jdk.crypto.ec", "jdk.unsupported")
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Lang"
            packageVersion = "0.2.0"
            description = "Learn Chinese with lessons, spaced repetition and an AI tutor"
            vendor = "Lang"
            windows {
                menuGroup = "Lang"
                shortcut = true
                perUserInstall = true
                upgradeUuid = "6f1f6f1e-3c1b-4d6e-9a59-2f8f3b7f0a11"
            }
        }
    }
}

// Renders every desktop screen to PNG without opening a window (used to review the GUI).
tasks.register<JavaExec>("screenshots") {
    group = "verification"
    description = "Renders the desktop screens to build/screenshots/*.png"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.lang.desktop.tools.ScreenshotsKt")
    args(layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
}
