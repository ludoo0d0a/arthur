plugins {
    alias(libs.plugins.androidLibrary)
}
android {
    namespace = "fr.geoking.tools.inappupdate"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
