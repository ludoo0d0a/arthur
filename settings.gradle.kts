pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
    }
}

rootProject.name = "Arthur"
include(":shared")
include(":fractal")
include(":genart")
include(":androidApp")

val gkToolsRoot = System.getenv("GK_TOOLS")
    ?: listOf("geoking-tools", "../geoking-tools", "../../geoking-tools")
        .map { rootDir.resolve(it) }
        .firstOrNull { it.resolve("android").isDirectory }
        ?.absolutePath
    ?: error("geoking-tools not found; clone sibling or set GK_TOOLS")

// Same-build include (not includeBuild): Android Studio sync fails to map composite
// Android library modules ("Expected android module not found! … Path: :debug-bar").
val toolsAndroid = file(gkToolsRoot).resolve("android")
include(":debug-bar")
project(":debug-bar").projectDir = toolsAndroid.resolve("debug-bar")
include(":in-app-update")
project(":in-app-update").projectDir = toolsAndroid.resolve("in-app-update")
