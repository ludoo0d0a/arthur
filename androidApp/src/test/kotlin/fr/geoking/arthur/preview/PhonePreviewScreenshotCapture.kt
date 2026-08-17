package fr.geoking.arthur.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.background
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.inspectionMode
import java.io.File

internal object PhonePreviewScreenshotCapture {
    val PhoneExportWidth = PhoneScreenContentWidth
    val PhoneExportHeight = PhoneScreenContentHeight

    private val outputRoot: File
        get() {
            val configured = System.getProperty("screenshot.outputDir")
            check(!configured.isNullOrBlank()) {
                "screenshot.outputDir not set. Run :androidApp:generatePhoneScreenshots " +
                    "or :androidApp:generatePhoneScreenshotsFramed"
            }
            return File(configured)
        }

    private val baseQualifiers: String
        get() = System.getProperty("screenshot.baseQualifiers") ?: "w411dp-h891dp-xxhdpi"

    @OptIn(ExperimentalRoborazziApi::class)
    fun capture(
        fileName: String,
        withFrame: Boolean = false,
        localized: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val root = outputRoot
        check(root.mkdirs() || root.isDirectory) { "Cannot create screenshot directory: $root" }
        val allLocales = ScreenshotLocales.requested()
        val captureLocales = if (localized) allLocales else listOf("en")

        for (lang in captureLocales) {
            ScreenshotLocales.apply(baseQualifiers, lang)
            val dir = ScreenshotLocales.outputDir(root, lang)
            check(dir.mkdirs() || dir.isDirectory) { "Cannot create screenshot directory: $dir" }
            val file = File(dir, fileName)
            captureRoboImage(
                filePath = file.absolutePath,
                roborazziComposeOptions =
                    RoborazziComposeOptions {
                        background(showBackground = false)
                        inspectionMode(true)
                    },
            ) {
                if (withFrame) {
                    PhoneDeviceScreenshotFrame {
                        content()
                    }
                } else {
                    Box(
                        modifier = Modifier.size(PhoneExportWidth, PhoneExportHeight),
                    ) {
                        content()
                    }
                }
            }
        }

        if (!localized) {
            ScreenshotLocales.mirrorEnglishToOtherLocales(root, fileName, allLocales)
        }
    }
}
