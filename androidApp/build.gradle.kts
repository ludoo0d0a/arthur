import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
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
    compileSdk = 37

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
        // Developer UI: on for debug builds; off for release / Play Store by default.
        // Debug can still opt via local.properties DEBUG_DEV (redundant with BuildConfig.DEBUG).
        buildConfigField("boolean", "DEBUG_DEV", "false")
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
        getByName("debug") {
            // local.properties / CI: DEBUG_DEV=true (optional; BuildConfig.DEBUG already gates UI).
            buildConfigField("boolean", "DEBUG_DEV", secretFlag("DEBUG_DEV").toString())
        }
        getByName("release") {
            // Play Store AABs must not ship developer UI. Do not read DEBUG_DEV from
            // local.properties here — a local DEBUG_DEV=true would otherwise bake into release.
            buildConfigField("boolean", "DEBUG_DEV", "false")
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
        // Robolectric on JDK 17+ needs reflective access to OpenJDK internals
        // (FileDescriptor / SharedSecrets). See https://robolectric.org/getting-started/
        unitTests.all {
            it.jvmArgs(
                // Avoid HotSpot C2 "Field too big for insn" crashes on Apple Silicon
                // when Robolectric/Roborazzi exercises large Compose trees.
                "-XX:TieredStopAtLevel=1",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

androidComponents {
    // Always-increasing debug versionCode so local installs never collide with
    // whatever versionCode is already on the device (e.g. a Play track build).
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set((System.currentTimeMillis() / 1000).toInt())
        }
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
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material.icons.core)
    implementation(libs.koin.android)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.car.app)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.revenuecat.purchases)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation("fr.geoking.tools:debug-bar")
    implementation("fr.geoking.tools:in-app-update")
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

val genartScreenshotOutputDir =
    rootProject.layout.projectDirectory
        .dir("screenshots")
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
    val genartOnly = screenshotTasks.any { it.contains("generateGenartScreenshots") }

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
    if (genartOnly) {
        tasks.named<Test>("testDebugUnitTest").configure {
            filter {
                includeTestsMatching("fr.geoking.arthur.preview.GenartPreviewScreenshotTest")
            }
            systemProperty("screenshot.outputDir", genartScreenshotOutputDir)
            systemProperty(
                "genart.screenshot.size",
                (findProperty("genartScreenshotSize") as String?) ?: "720",
            )
            systemProperty(
                "genart.screenshot.generation",
                (findProperty("genartScreenshotGeneration") as String?) ?: "7",
            )
        }
    }
    if (!framedOnly && !screenOnly && !genartOnly) {
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

tasks.register("generateGenartScreenshots") {
    group = "screenshots"
    description =
        "Bakes genart still previews to screenshots/#N-Name.png " +
            "(-PgenartScreenshotSize=720, -PgenartScreenshotGeneration=7)"
    dependsOn("testDebugUnitTest")
}

val googleServices = file("google-services.json")
if (googleServices.exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
