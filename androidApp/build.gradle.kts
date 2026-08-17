plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "fr.geoking.arthur"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.geoking.arthur"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REVENUECAT_API_KEY", "\"${project.findProperty("REVENUECAT_API_KEY") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":fractal"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.koin.android)
    implementation(libs.androidx.media)
    implementation(libs.androidx.leanback)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.revenuecat.purchases)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.client.okhttp)
    // Phone @Preview screenshots (Robolectric + Roborazzi) — same stack as Scora
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
}

val phoneScreenshotOutputDir =
    rootProject.layout.projectDirectory
        .dir("screenshots/phone")
        .asFile.absolutePath

val phoneFramedScreenshotOutputDir =
    rootProject.layout.projectDirectory
        .dir("screenshots/phone/framed")
        .asFile.absolutePath

fun resolveScreenshotLocales(): String {
    val raw = (findProperty("screenshotLocales") as String?)?.trim().orEmpty()
    return when {
        raw.equals("all", ignoreCase = true) -> "en,fr"
        raw.isNotEmpty() -> raw
        else -> "en"
    }
}

afterEvaluate {
    val screenshotLocales = resolveScreenshotLocales()
    val screenshotTasks = gradle.startParameter.taskNames
    val framedOnly = screenshotTasks.any { it.contains("generatePhoneScreenshotsFramed") }
    val screenOnly =
        !framedOnly && screenshotTasks.any { it.contains("generatePhoneScreenshots") }

    if (framedOnly) {
        tasks.named<Test>("testDebugUnitTest").configure {
            filter {
                includeTestsMatching("fr.geoking.arthur.preview.PhonePreviewFramedScreenshotTest")
            }
            systemProperty("screenshot.outputDir", phoneFramedScreenshotOutputDir)
            systemProperty("screenshot.locales", screenshotLocales)
            systemProperty("screenshot.baseQualifiers", "w439dp-h919dp-xxhdpi")
            systemProperty("roborazzi.test.record", "true")
            systemProperty("roborazzi.test.compare", "false")
        }
    }
    if (screenOnly) {
        tasks.named<Test>("testDebugUnitTest").configure {
            filter {
                includeTestsMatching("fr.geoking.arthur.preview.PhonePreviewScreenshotTest")
            }
            systemProperty("screenshot.outputDir", phoneScreenshotOutputDir)
            systemProperty("screenshot.locales", screenshotLocales)
            systemProperty("screenshot.baseQualifiers", "w411dp-h891dp-xxhdpi")
            systemProperty("roborazzi.test.record", "true")
            systemProperty("roborazzi.test.compare", "false")
        }
    }
}

tasks.register("generatePhoneScreenshots") {
    group = "screenshots"
    description =
        "Renders phone key screens to screenshots/phone/{lang}/ " +
            "(screen-only; Robolectric + Roborazzi; -PscreenshotLocales=en,fr|all)"
    dependsOn("testDebugUnitTest")
}

tasks.register("generatePhoneScreenshotsFramed") {
    group = "screenshots"
    description =
        "Renders phone key screens to screenshots/phone/framed/{lang}/ " +
            "(device chassis; Robolectric + Roborazzi; -PscreenshotLocales=en,fr|all)"
    dependsOn("testDebugUnitTest")
}

val googleServices = file("google-services.json")
if (googleServices.exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
