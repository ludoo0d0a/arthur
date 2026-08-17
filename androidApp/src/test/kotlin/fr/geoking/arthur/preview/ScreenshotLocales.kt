package fr.geoking.arthur.preview

import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Locales for Roborazzi screenshot runs (same pattern as Scora).
 *
 * Set via Gradle `-PscreenshotLocales=en,fr` (or `all`) → system property `screenshot.locales`.
 */
internal object ScreenshotLocales {
    fun requested(): List<String> {
        val raw = System.getProperty("screenshot.locales") ?: "en"
        return raw
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun apply(
        baseQualifiers: String,
        lang: String,
    ) {
        val qualifiers =
            if (lang.isBlank() || lang == "en") {
                baseQualifiers
            } else {
                "$lang-$baseQualifiers"
            }
        RuntimeEnvironment.setQualifiers(qualifiers)
    }

    fun outputDir(
        root: File,
        lang: String,
    ): File = File(root, lang)

    fun mirrorEnglishToOtherLocales(
        root: File,
        fileName: String,
        locales: List<String>,
    ) {
        val enFile = File(outputDir(root, "en"), fileName)
        if (!enFile.isFile) return
        for (lang in locales) {
            if (lang == "en") continue
            val dest = File(outputDir(root, lang), fileName)
            dest.parentFile?.mkdirs()
            enFile.copyTo(dest, overwrite = true)
        }
    }
}
