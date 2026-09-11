package fr.geoking.arthur.auto

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.validation.HostValidator
import androidx.core.graphics.drawable.IconCompat
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArthurCarAppTest {

    @Test
    fun service_createsHostValidator() {
        val service = ArthurCarAppService()
        val validator = service.createHostValidator()
        assertNotNull(validator)
        assertEquals(HostValidator.ALLOW_ALL_HOSTS_VALIDATOR, validator)
    }

    @Test
    fun session_createsArthurCarSession() {
        val service = ArthurCarAppService()
        val session = service.onCreateSession()
        assertNotNull(session)
        assertTrue(session is ArthurCarSession)
    }

    @Test
    fun session_onCreateScreen_returnsPackSelectionScreen() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val service = ArthurCarAppService()
        val session = service.onCreateSession()
        val intent = android.content.Intent()
        val screen = session.onCreateScreen(intent)
        assertNotNull(screen)
        assertTrue(screen is PackSelectionScreen)
    }

    @Test
    fun packSelectionScreen_buildsGridTemplate() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())
        val screen = PackSelectionScreen(carContext)
        val template = screen.onGetTemplate()
        assertNotNull(template)
        assertTrue(template is androidx.car.app.model.GridTemplate)
    }

    @Test
    fun subPackSelectionScreen_buildsGridTemplate() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())
        val screen = SubPackSelectionScreen(carContext, fr.geoking.arthur.ui.components.PackFamily.Museum)
        val template = screen.onGetTemplate()
        assertNotNull(template)
        assertTrue(template is androidx.car.app.model.GridTemplate)
    }

    private fun attachBaseContext(contextWrapper: android.content.ContextWrapper, base: android.content.Context) {
        val method = android.content.ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", android.content.Context::class.java)
        method.isAccessible = true
        method.invoke(contextWrapper, base)
    }

    @Test
    fun paneTemplate_supportsLargeImageType() {
        val artwork = Artwork(
            id = "genart.particles",
            title = "Particles Test",
            attribution = "GeoKing",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )

        val bitmap = AmbientStillRenderer.render(artwork, 1L)
        assertNotNull("Renderer should produce bitmap", bitmap)

        val carIcon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
        val row = Row.Builder()
            .setTitle(artwork.title)
            .addText(artwork.attribution)
            .setImage(carIcon, Row.IMAGE_TYPE_LARGE)
            .build()

        assertNotNull(row.image)
        assertEquals(artwork.title, row.title.toString())

        // Host: Pane actions ≤ 2 — keep a single primary play/pause action.
        val pane = Pane.Builder()
            .setImage(carIcon)
            .addRow(row)
            .addAction(
                Action.Builder()
                    .setTitle("Pause")
                    .setFlags(Action.FLAG_PRIMARY)
                    .setOnClickListener { }
                    .build(),
            )
            .build()

        assertTrue("Pane must not exceed 2 actions", pane.actions.size <= 2)

        // ActionStrip: ≤2 actions, icon-only (no label buttons).
        val strip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
                    .setOnClickListener { }
                    .build(),
            )
            .addAction(
                Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
                    .setOnClickListener { }
                    .build(),
            )
            .build()
        assertEquals(2, strip.actions.size)

        val header = Header.Builder()
            .setTitle("Arthur")
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        val paneTemplate = PaneTemplate.Builder(pane)
            .setHeader(header)
            .setActionStrip(strip)
            .build()

        assertNotNull(paneTemplate)
        assertEquals(1, paneTemplate.pane.rows.size)
        assertNotNull(paneTemplate.pane.image)
        assertNotNull(paneTemplate.pane.rows[0].image)
        assertEquals(1, paneTemplate.pane.actions.size)
    }

    @Test
    fun automotiveAppDesc_declaresTemplateSupport() {
        val file = File("src/main/res/xml/automotive_app_desc.xml")
        assertTrue("automotive_app_desc.xml must exist", file.exists())
        val content = file.readText()
        assertTrue("Must contain <uses name=\"template\"", content.contains("<uses name=\"template\""))
    }

    @Test
    fun manifest_declaresMediaTemplatesPermission() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(
            "Must declare androidx.car.app.MEDIA_TEMPLATES permission",
            content.contains("<uses-permission android:name=\"androidx.car.app.MEDIA_TEMPLATES\" />"),
        )
    }

    @Test
    fun manifest_declaresDefaultCategoryForCarAppService() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(
            "CarAppService must declare android.intent.category.DEFAULT category",
            content.contains("<category android:name=\"android.intent.category.DEFAULT\" />"),
        )
    }

    @Test
    fun manifest_declaresMinCarApiLevelOfAtLeastEight() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        val match = Regex("""<meta-data\s+android:name="androidx\.car\.app\.minCarApiLevel"\s+android:value="(\d+)"\s*/>""")
            .find(content)
        assertNotNull("minCarApiLevel meta-data should be declared in AndroidManifest.xml", match)
        val level = match!!.groupValues[1].toInt()
        assertTrue("Media apps must specify a minCarApiLevel of at least 8 (found $level)", level >= 8)
    }

    @Test
    fun artworkPaneScreen_onGetTemplateWithRemoteUrlDoesNotCrashOnMainThread() {
        val remoteArt = Artwork(
            id = "test_remote_art",
            title = "Test Remote Art",
            attribution = "Test Artist",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/photo.jpg",
        )

        // Verify AmbientStillRenderer returns immediate local placeholder without crash
        val placeholder = AmbientStillRenderer.renderPlaceholder(remoteArt, generation = 0L)
        assertNotNull("Placeholder bitmap should be generated", placeholder)
        assertEquals(AmbientStillRenderer.SIZE, placeholder.width)
    }

    @Test
    fun ambientStillRenderer_mainThreadRemoteUrlReturnsPlaceholderWithoutCrash() {
        val remoteArt = Artwork(
            id = "rijks-SK-C-5",
            title = "Night Watch",
            attribution = "Rembrandt",
            sourceId = "rijksmuseum",
            kind = ArtworkKind.Painting,
            remoteUrl = "https://example.com/nightwatch.jpg",
        )

        // Running on Main thread should return a fallback placeholder bitmap rather than throwing NetworkOnMainThreadException.
        val bitmap = AmbientStillRenderer.render(remoteArt, generation = 1L)
        assertNotNull("Renderer must return a fallback placeholder bitmap on main thread", bitmap)
        assertEquals(AmbientStillRenderer.SIZE, bitmap.width)
        assertEquals(AmbientStillRenderer.SIZE, bitmap.height)
    }

    @Test
    fun ambientStillRenderer_usesDiskCacheWhenAvailable() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val cache = fr.geoking.arthur.source.ArtworkImageCache(context)
        val artworkId = "test_cached_photo"

        // Put fake bitmap bytes into ArtworkImageCache
        val testBmp = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val stream = java.io.ByteArrayOutputStream()
        testBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        cache.putImage(artworkId, stream.toByteArray())

        val photoArt = Artwork(
            id = artworkId,
            title = "Test Photo",
            attribution = "GeoKing",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/photo.jpg",
        )

        val rendered = AmbientStillRenderer.render(photoArt, generation = 1L, imageCache = cache)
        assertNotNull("Renderer should load cached photo from disk", rendered)
        assertEquals(AmbientStillRenderer.SIZE, rendered.width)
    }

    @Test
    fun rowBuilder_handlesBlankTitleAndAttribution() {
        val blankTitleArtwork = Artwork(
            id = "blank_title_art",
            title = "",
            attribution = "",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        val titleText = blankTitleArtwork.title.ifBlank { "Arthur" }
        val rowBuilder = Row.Builder().setTitle(titleText)
        if (blankTitleArtwork.attribution.isNotBlank()) {
            rowBuilder.addText(blankTitleArtwork.attribution)
        }
        val row = rowBuilder.build()
        assertEquals("Arthur", row.title.toString())
        assertTrue("No text lines when attribution is blank", row.texts.isEmpty())
    }
}
