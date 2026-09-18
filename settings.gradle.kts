pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
// }

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

if (gkToolsRoot != null) {
    includeBuild("$gkToolsRoot/android") {
        dependencySubstitution {
            substitute(module("fr.geoking.tools:debug-bar"))
                .using(project(":debug-bar"))
        }
    }
}
