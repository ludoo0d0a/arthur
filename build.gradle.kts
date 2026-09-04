plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register<Exec>("copyWebsiteScreenshots") {
    group = "website"
    description = "Copy Roborazzi/Play screenshots into website/assets per website/screenshot-sources.json"
    workingDir = rootDir
    commandLine("python3", "scripts/fill_website_screenshots.py")
}

tasks.register("generateWebsiteScreenshots") {
    group = "screenshots"
    description =
        "Generate framed phone screenshots and sync into website/assets " +
            "(-PscreenshotLocales=en,fr|all)"
    dependsOn(
        ":androidApp:generatePhoneScreenshots",
        ":androidApp:generatePhoneScreenshotsFramed",
    )
    finalizedBy("copyWebsiteScreenshots")
}
