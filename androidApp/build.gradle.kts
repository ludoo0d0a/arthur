import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.aboutlibraries.android)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(key: String): String {
    val keys = listOf(key, key.lowercase(), key.uppercase()).distinct()
    for (k in keys) {
        val v = localProps.getProperty(k)
            ?: project.findProperty(k)?.toString()
            ?: System.getenv(k)
        if (!v.isNullOrBlank()) return v
    }
    return ""
}

fun secretFlag(key: String): Boolean =
    when (secret(key).trim().lowercase()) {
        "true", "1", "yes" -> true
        else -> false
    }

val versionProps = Properties().apply {
    val f = rootProject.file("playstore/version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "fr.geoking.arthur"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.geoking.arthur"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: versionProps.getProperty("versionCode") ?: "1").toInt()
        versionName = (System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }
            ?: versionProps.getProperty("versionName") ?: "1.0").removePrefix("v")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_API_KEY")}\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"${secret("WEB_CLIENT_ID")}\"")
        buildConfigField("String", "PEXELS_API_KEY", "\"${secret("PEXELS_API_KEY")}\"")
        // Unsplash Access Key = public Client-ID. Secret Key is OAuth-only — do not BuildConfig it into the APK.
        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"${secret("UNSPLASH_ACCESS_KEY")}\"")
        buildConfigField("String", "PIXABAY_API_KEY", "\"${secret("PIXABAY_API_KEY")}\"")
        buildConfigField("String", "COVERR_API_KEY", "\"${secret("COVERR_API_KEY")}\"")
        buildConfigField("String", "EUROPEANA_API_KEY", "\"${secret("EUROPEANA_API_KEY")}\"")
        buildConfigField("String", "HARVARD_API_KEY", "\"${secret("HARVARD_API_KEY")}\"")
        buildConfigField("String", "SMITHSONIAN_API_KEY", "\"${secret("SMITHSONIAN_API_KEY")}\"")
        buildConfigField("String", "DEVIANTART_CLIENT_ID", "\"${secret("DEVIANTART_CLIENT_ID")}\"")
        buildConfigField("String", "DEVIANTART_CLIENT_SECRET", "\"${secret("DEVIANTART_CLIENT_SECRET")}\"")
        // Opt-in developer UI on non-debug builds (local.properties / CI: DEBUG_DEV=true).
        buildConfigField("boolean", "DEBUG_DEV", secretFlag("DEBUG_DEV").toString())
        val buildDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    val keystorePath = secret("KEYSTORE_FILE")
    signingConfigs {
        create("release") {
            if (keystorePath.isNotBlank()) {
                storeFile = file(keystorePath)
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (keystorePath.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":fractal"))
    implementation(project(":genart"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.koin.android)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.car.app)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.revenuecat.purchases)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockwebserver)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.mockwebserver)
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
    if (!framedOnly && !screenOnly) {
        tasks.named<Test>("testDebugUnitTest").configure {
            filter {
                excludeTestsMatching("fr.geoking.arthur.preview.*")
            }
            // Live stock-photo e2e: -Pe2eStockPhotos=true (needs PEXELS_API_KEY / UNSPLASH_ACCESS_KEY)
            systemProperty(
                "e2e.stockPhotos",
                (findProperty("e2eStockPhotos") as String?)
                    ?: System.getenv("E2E_STOCK_PHOTOS")
                    ?: "",
            )
            // Live museum e2e: -Pe2eMuseumSources=true (needs EUROPEANA / HARVARD / SMITHSONIAN keys)
            systemProperty(
                "e2e.museumSources",
                (findProperty("e2eMuseumSources") as String?)
                    ?: System.getenv("E2E_MUSEUM_SOURCES")
                    ?: "",
            )
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
