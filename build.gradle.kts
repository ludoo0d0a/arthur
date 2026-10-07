plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.version.catalog.update)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register<Exec>("copyWebsiteScreenshots") {
    group = "website"
    description = "Copy Roborazzi/Play screenshots into website/assets per website/screenshot-sources.json"
    workingDir = rootDir
    commandLine("scripts/fill_website_screenshots.py")
}

// Screen + framed share :androidApp:testFullDebugUnitTest with different filters /
// output dirs, so they cannot run in one Gradle invocation. Chain via Exec.
tasks.register<Exec>("generateWebsiteScreenshots") {
    group = "screenshots"
    description =
        "Generate phone (+ framed) screenshots and sync into website/assets " +
            "(-PscreenshotLocales=en,fr|all)"
    workingDir = rootDir
    val locales = (findProperty("screenshotLocales") as String?)?.trim().orEmpty()
        .ifEmpty { "en,fr" }
    commandLine(
        "bash",
        "-lc",
        """
        set -euo pipefail
        ./gradlew :androidApp:generatePhoneScreenshots -PscreenshotLocales=$locales
        ./gradlew :androidApp:generatePhoneScreenshotsFramed -PscreenshotLocales=$locales
        ./scripts/fill_website_screenshots.py
        """.trimIndent(),
    )
}
