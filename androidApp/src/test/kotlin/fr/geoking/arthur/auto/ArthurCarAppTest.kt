package fr.geoking.arthur.auto

import androidx.car.app.model.Action
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
    fun createAmbientScreen_routesBySoundSetting() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, app)
        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        val audio = fr.geoking.arthur.source.AmbientAudioSettings(app)
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { audio }
                    single { fr.geoking.arthur.source.RotationSettings(app) }
                    single { fr.geoking.arthur.source.QuoteSettings(app) }
                    single { fr.geoking.arthur.source.DeveloperSettings(app) }
                    single { fr.geoking.arthur.source.ArtworkImageCache(app) }
                    single { fr.geoking.arthur.source.InvalidArtworkStore(app) }
                    single { fr.geoking.arthur.source.QuoteRepository(app, httpGet = { "[]" }, provider = { fr.geoking.arthur.source.QuoteProvider.ZenQuotes }) }
                    single {
                        fr.geoking.arthur.shared.engine.ContentEngine(
                            sources = listOf(fr.geoking.arthur.shared.source.GenartSource()),
                            packOwnership = fr.geoking.arthur.shared.marketplace.FakePackOwnership().also { it.unlockAll() },
                        )
                    }
                },
            )
        }
        try {
            audio.setEnabled(false)
            assertTrue(createAmbientScreen(carContext) is ArtworkPaneScreen)
            audio.setEnabled(true)
            assertTrue(createAmbientScreen(carContext) is MediaAmbientPlaybackScreen)
            val media = createAmbientScreen(carContext) as MediaAmbientPlaybackScreen
            val template = media.onGetTemplate()
            assertTrue(
                "Sound-on ambient should use MediaPlaybackTemplate",
                template is androidx.car.app.media.model.MediaPlaybackTemplate,
            )
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun session_onCreateScreen_withArtworkId_returnsArtworkPaneScreenWithInitialId() {
        val service = ArthurCarAppService()
        val session = service.onCreateSession()
        val intent = android.content.Intent().apply {
            putExtra("artwork_id", "genart.particles")
            putExtra("pack_family", "Genart")
        }
        val screen = session.onCreateScreen(intent)
        assertNotNull(screen)
        assertTrue(screen is ArtworkPaneScreen)
        val paneScreen = screen as ArtworkPaneScreen
        assertEquals("genart.particles", paneScreen.initialArtworkId)
        assertEquals(fr.geoking.arthur.ui.components.PackFamily.Genart, paneScreen.packSelection.family)
    }

    @Test
    fun session_onCreateScreen_withDeepLinkData_returnsArtworkPaneScreenWithInitialId() {
        val service = ArthurCarAppService()
        val session = service.onCreateSession()
        val intent = android.content.Intent().apply {
            data = android.net.Uri.parse("arthur://ambient?artwork_id=rijks-SK-C-5&family=Museum")
        }
        val screen = session.onCreateScreen(intent)
        assertNotNull(screen)
        assertTrue(screen is ArtworkPaneScreen)
        val paneScreen = screen as ArtworkPaneScreen
        assertEquals("rijks-SK-C-5", paneScreen.initialArtworkId)
        assertEquals(fr.geoking.arthur.ui.components.PackFamily.Museum, paneScreen.packSelection.family)
    }

    @Test
    fun packSelectionScreen_buildsSectionedGridWithExtraLargeItems() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())
        val screen = PackSelectionScreen(carContext)
        val template = screen.onGetTemplate()
        assertNotNull(template)
        assertTrue(template is androidx.car.app.model.SectionedItemTemplate)
        val sectioned = template as androidx.car.app.model.SectionedItemTemplate
        assertEquals(1, sectioned.sections.size)
        val section = sectioned.sections[0]
        assertTrue(section is androidx.car.app.model.GridSection)
        val gridSection = section as androidx.car.app.model.GridSection
        assertEquals(
            androidx.car.app.model.GridSection.ITEM_SIZE_EXTRA_LARGE,
            gridSection.itemSize,
        )
        assertTrue(
            "Home grid should be hard-capped at $MAX_HOME_GRID_ITEMS",
            gridSection.itemsDelegate.size <= MAX_HOME_GRID_ITEMS,
        )
        assertTrue(gridSection.itemsDelegate.size > 0)
    }

    @Test
    fun subPackSelectionScreen_buildsSectionedGridWithExtraLargeItems() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())
        val screen = SubPackSelectionScreen(carContext, fr.geoking.arthur.ui.components.PackFamily.Museum)
        val template = screen.onGetTemplate()
        assertNotNull(template)
        assertTrue(template is androidx.car.app.model.SectionedItemTemplate)
        val sectioned = template as androidx.car.app.model.SectionedItemTemplate
        assertEquals(1, sectioned.sections.size)
        val section = sectioned.sections[0]
        assertTrue(section is androidx.car.app.model.GridSection)
        val gridSection = section as androidx.car.app.model.GridSection
        assertEquals(
            androidx.car.app.model.GridSection.ITEM_SIZE_EXTRA_LARGE,
            gridSection.itemSize,
        )
        assertTrue(
            "Sub-pack grid should be capped at $MAX_SUB_GRID_ITEMS or host limit",
            gridSection.itemsDelegate.size <= MAX_SUB_GRID_ITEMS,
        )
        assertTrue(gridSection.itemsDelegate.size > 0)
    }

    @Test
    fun packSelectionScreen_includesSoundFamily() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())
        val screen = PackSelectionScreen(carContext)
        val template = screen.onGetTemplate() as androidx.car.app.model.SectionedItemTemplate
        val gridSection = template.sections[0] as androidx.car.app.model.GridSection
        // Museum, Genart, Photo, Sound, Sculpture, Painting (Video/Personal filtered).
        assertEquals(6, gridSection.itemsDelegate.size)
        assertTrue(gridSection.itemsDelegate.size <= MAX_HOME_GRID_ITEMS)
    }

    @Test
    fun soundSubPack_showsFreeEssentialsOnlyWithoutOwnership() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, app)
        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single<fr.geoking.arthur.shared.marketplace.PackOwnership> {
                        fr.geoking.arthur.shared.marketplace.PackOwnership.NONE
                    }
                },
            )
        }
        try {
            val screen = SubPackSelectionScreen(
                carContext,
                fr.geoking.arthur.ui.components.PackFamily.Sound,
            )
            val template = screen.onGetTemplate() as androidx.car.app.model.SectionedItemTemplate
            val gridSection = template.sections[0] as androidx.car.app.model.GridSection
            assertEquals(
                "Without ownership only free Essentials is listed",
                1,
                gridSection.itemsDelegate.size,
            )
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun soundPlayerCarScreen_buildsMediaPlaybackTemplate() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, app)
        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.AmbientAudioSettings(app) }
                },
            )
        }
        try {
            val screen = SoundPlayerCarScreen(
                carContext,
                fr.geoking.arthur.ui.components.PackSelection(
                    fr.geoking.arthur.ui.components.PackFamily.Sound,
                    fr.geoking.arthur.ui.components.AudioPackTopic.Essentials.testTagSuffix,
                ),
            )
            val template = screen.onGetTemplate()
            assertTrue(
                "Sound player should use MediaPlaybackTemplate",
                template is androidx.car.app.media.model.MediaPlaybackTemplate,
            )
            val header = (template as androidx.car.app.media.model.MediaPlaybackTemplate).header
            assertNotNull("Sound player should have a header", header)
            assertEquals(
                "Header end actions: mute + change playlist / sound pack",
                2,
                header!!.endHeaderActions.size,
            )
            header.endHeaderActions.forEach { action ->
                assertNotNull("Mute/playlist should be icon-only", action.icon)
            }
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun carSettingsScreen_buildsListTemplateWithIntervalOptions() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())

        // Setup Koin DI context for RotationSettings injection safely
        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.RotationSettings(org.robolectric.RuntimeEnvironment.getApplication()) }
                    single { fr.geoking.arthur.source.QuoteSettings(org.robolectric.RuntimeEnvironment.getApplication()) }
                    single { fr.geoking.arthur.source.AmbientAudioSettings(org.robolectric.RuntimeEnvironment.getApplication()) }
                },
            )
        }

        try {
            val screen = CarSettingsScreen(carContext)
            val template = screen.onGetTemplate()
            assertNotNull(template)
            assertTrue(template is androidx.car.app.model.ListTemplate)
            val listTemplate = template as androidx.car.app.model.ListTemplate
            val list = listTemplate.singleList
            assertNotNull(list)
            // Check update + quotes + provider + sound + duration entry (options live in sub-screen).
            assertEquals(5, list!!.items.size)
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun carRotationIntervalScreen_listsAllDurationOptions() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, app)
        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.RotationSettings(app) }
                },
            )
        }
        try {
            val screen = CarRotationIntervalScreen(carContext)
            val template = screen.onGetTemplate() as androidx.car.app.model.ListTemplate
            assertEquals(
                fr.geoking.arthur.source.RotationSettings.OPTIONS_MS.size,
                template.singleList!!.items.size,
            )
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun carSettingsScreen_togglesQuoteSettings() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())

        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val quoteSettings = fr.geoking.arthur.source.QuoteSettings(app)
        quoteSettings.setShowQuotes(true)

        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.RotationSettings(app) }
                    single { quoteSettings }
                    single { fr.geoking.arthur.source.AmbientAudioSettings(app) }
                },
            )
        }

        try {
            val screen = CarSettingsScreen(carContext)
            val listTemplate = screen.onGetTemplate() as androidx.car.app.model.ListTemplate
            // check update (0), quotes (1), …
            val item = listTemplate.singleList!!.items[1] as androidx.car.app.model.Row
            item.onClickDelegate!!.sendClick(object : androidx.car.app.OnDoneCallback {
                override fun onSuccess(response: androidx.car.app.serialization.Bundleable?) {}
                override fun onFailure(response: androidx.car.app.serialization.Bundleable) {}
            })
            assertEquals(false, quoteSettings.showQuotes.value)
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun artworkPaneScreen_formatsQuotesAndDevModeSlidePosition() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())

        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val devSettings = fr.geoking.arthur.source.DeveloperSettings(app)
        devSettings.setVerbose(true)
        val quoteSettings = fr.geoking.arthur.source.QuoteSettings(app)
        quoteSettings.setShowQuotes(true)

        val quoteRepo = fr.geoking.arthur.source.QuoteRepository(
            context = app,
            httpGet = { """[{"q": "Be yourself", "a": "Oscar Wilde"}]""" },
            provider = { fr.geoking.arthur.source.QuoteProvider.ZenQuotes },
        )

        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.RotationSettings(app) }
                    single { quoteSettings }
                    single { fr.geoking.arthur.source.AmbientAudioSettings(app) }
                    single { devSettings }
                    single { quoteRepo }
                    single { fr.geoking.arthur.source.ArtworkImageCache(app) }
                    single { fr.geoking.arthur.source.InvalidArtworkStore(app) }
                    single {
                        fr.geoking.arthur.shared.engine.ContentEngine(
                            sources = listOf(fr.geoking.arthur.shared.source.GenartSource()),
                            packOwnership = fr.geoking.arthur.shared.marketplace.FakePackOwnership().also { it.unlockAll() },
                        )
                    }
                },
            )
        }

        try {
            val screen = ArtworkPaneScreen(
                carContext = carContext,
                packSelection = fr.geoking.arthur.ui.components.PackSelection(fr.geoking.arthur.ui.components.PackFamily.Genart),
            )
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            fun requirePane(): PaneTemplate {
                val template = screen.onGetTemplate()
                if (template is PaneTemplate) return template
                val msg = (template as? androidx.car.app.model.MessageTemplate)?.message?.toString()
                throw AssertionError("Expected PaneTemplate, got ${template::class.java.simpleName}: $msg")
            }
            var reloadedTemplate = requirePane()
            repeat(40) {
                if (reloadedTemplate.pane.isLoading || screen.currentArtwork() == null || reloadedTemplate.pane.rows.size < 2) {
                    Thread.sleep(50)
                    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
                    org.robolectric.shadows.ShadowLooper.idleMainLooper()
                    reloadedTemplate = requirePane()
                }
            }
            assertTrue("Pane should not be loading", !reloadedTemplate.pane.isLoading)
            val row = reloadedTemplate.pane.rows[0]
            assertNotNull(row)
            assertTrue(row.texts.isNotEmpty())
            val fullText = row.texts.joinToString(" ") { it.toCharSequence().toString() }
            assertTrue("Should contain slide position in dev mode", fullText.contains("["))

            assertTrue("Quote should be on a second pane row", reloadedTemplate.pane.rows.size >= 2)
            val quoteRow = reloadedTemplate.pane.rows[1]
            assertEquals("“Be yourself”", quoteRow.title.toString())
            assertEquals(1, quoteRow.texts.size)
            assertEquals("— Oscar Wilde", quoteRow.texts[0].toString())

            // PaneTemplate: primary play/pause in pane; sound toggle + next as header end actions.
            // Verbose adds Debug as the second pane action (≤2).
            assertEquals(
                "Pane body should have play/pause (+ Debug when verbose)",
                2,
                reloadedTemplate.pane.actions.size,
            )
            val playPause = reloadedTemplate.pane.actions[0]
            assertNotNull("Play/pause should be icon-only", playPause.icon)
            assertTrue(
                "Play/pause should be primary",
                (playPause.flags and Action.FLAG_PRIMARY) == Action.FLAG_PRIMARY,
            )
            assertTrue(
                "Play/pause should have no title",
                playPause.title == null || playPause.title.toString().isBlank(),
            )
            val header = reloadedTemplate.header
            assertNotNull("Pane template should have a Header", header)
            assertEquals(
                "Header end actions should contain sound toggle + next",
                2,
                header!!.endHeaderActions.size,
            )
            header.endHeaderActions.forEach { action ->
                assertNotNull("Sound/next should be icon-only", action.icon)
            }
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun mediaAmbientPlaybackScreen_headerHasSoundAndPlaylistActionsAndAutoRotationDoesNotPause() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())

        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val audioSettings = fr.geoking.arthur.source.AmbientAudioSettings(app)
        audioSettings.setEnabled(true)

        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.RotationSettings(app) }
                    single { fr.geoking.arthur.source.QuoteSettings(app) }
                    single { audioSettings }
                    single { fr.geoking.arthur.source.DeveloperSettings(app) }
                    single { fr.geoking.arthur.source.QuoteRepository(context = app, httpGet = { fr.geoking.arthur.source.QuoteRepository.encodeQuotes(emptyList()) }, provider = { fr.geoking.arthur.source.QuoteProvider.ZenQuotes }) }
                    single { fr.geoking.arthur.source.ArtworkImageCache(app) }
                    single { fr.geoking.arthur.source.InvalidArtworkStore(app) }
                    single {
                        fr.geoking.arthur.shared.engine.ContentEngine(
                            sources = listOf(fr.geoking.arthur.shared.source.GenartSource()),
                            packOwnership = fr.geoking.arthur.shared.marketplace.FakePackOwnership().also { it.unlockAll() },
                        )
                    }
                },
            )
        }

        try {
            val screen = MediaAmbientPlaybackScreen(carContext)
            val template = screen.onGetTemplate() as androidx.car.app.media.model.MediaPlaybackTemplate
            val header = template.header
            assertNotNull("MediaPlaybackTemplate should have a header", header)
            assertEquals(
                "Header end actions: mute + change playlist (≤2, ActionStrip successor)",
                2,
                header!!.endHeaderActions.size,
            )
            header.endHeaderActions.forEach { action ->
                assertNotNull("Mute/playlist should be icon-only", action.icon)
            }

            assertTrue("Initially should be playing", screen.isPlaying())
            screen.advance(+1, isAuto = true)
            assertTrue("Should still be playing after 1st auto-rotation", screen.isPlaying())
            screen.advance(+1, isAuto = true)
            assertTrue("Media player ambient should NOT pause after 2nd auto-rotation", screen.isPlaying())
            screen.advance(+1, isAuto = true)
            assertTrue("Media player ambient should NOT pause after 3rd auto-rotation", screen.isPlaying())
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun artworkPaneScreen_autoRotationPausesAfter3PhotosAndManualNavigationResets() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())

        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val rotationSettings = fr.geoking.arthur.source.RotationSettings(app)

        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { rotationSettings }
                    single { fr.geoking.arthur.source.QuoteSettings(app) }
                    single { fr.geoking.arthur.source.AmbientAudioSettings(app) }
                    single { fr.geoking.arthur.source.DeveloperSettings(app) }
                    single { fr.geoking.arthur.source.QuoteRepository(context = app, httpGet = { fr.geoking.arthur.source.QuoteRepository.encodeQuotes(emptyList()) }, provider = { fr.geoking.arthur.source.QuoteProvider.ZenQuotes }) }
                    single { fr.geoking.arthur.source.ArtworkImageCache(app) }
                    single { fr.geoking.arthur.source.InvalidArtworkStore(app) }
                    single {
                        fr.geoking.arthur.shared.engine.ContentEngine(
                            sources = listOf(fr.geoking.arthur.shared.source.GenartSource()),
                            packOwnership = fr.geoking.arthur.shared.marketplace.FakePackOwnership().also { it.unlockAll() },
                        )
                    }
                },
            )
        }

        try {
            val screen = ArtworkPaneScreen(
                carContext = carContext,
                packSelection = fr.geoking.arthur.ui.components.PackSelection(fr.geoking.arthur.ui.components.PackFamily.Genart),
            )
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            org.robolectric.shadows.ShadowLooper.idleMainLooper()

            assertTrue("Initially should be playing", screen.isPlaying())

            screen.advance(+1, isAuto = true)
            assertTrue("Should still be playing after 1 auto-rotation (2nd image)", screen.isPlaying())

            screen.advance(+1, isAuto = true)
            assertTrue("Should pause on the 3rd image (2nd auto-rotation)", !screen.isPlaying())

            screen.advance(-1, isAuto = false)
            assertTrue("Manual step resumes playing and resets counter", screen.isPlaying())
        } finally {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun gridContentLimit_hardCapsBelowHostLimit() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())
        // ConstraintManager under Robolectric returns default limit 6.
        // minOf enforces host limit and hard cap.
        assertEquals(MAX_HOME_GRID_ITEMS, gridContentLimit(carContext, MAX_HOME_GRID_ITEMS))
        assertEquals(6, gridContentLimit(carContext, MAX_SUB_GRID_ITEMS))
        assertEquals(3, gridContentLimit(carContext, 3))
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

        // Host: Pane actions ≤ 2 — keep a single primary play/pause action (icon-only).
        val pane = Pane.Builder()
            .setImage(carIcon)
            .addRow(row)
            .addAction(
                Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
                    .setFlags(Action.FLAG_PRIMARY)
                    .setOnClickListener { }
                    .build(),
            )
            .build()

        assertTrue("Pane must not exceed 2 actions", pane.actions.size <= 2)

        // Header end actions ≤ 2: icon-only sound toggle + next (preferred over deprecated ActionStrip).
        val header = Header.Builder()
            .setTitle("Arthur")
            .setStartHeaderAction(Action.APP_ICON)
            .addEndHeaderAction(
                Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
                    .setOnClickListener { }
                    .build(),
            )
            .addEndHeaderAction(
                Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
                    .setOnClickListener { }
                    .build(),
            )
            .build()
        assertEquals(2, header.endHeaderActions.size)

        val paneTemplate = PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()

        assertNotNull(paneTemplate)
        assertEquals(1, paneTemplate.pane.rows.size)
        assertNotNull(paneTemplate.pane.image)
        assertNotNull(paneTemplate.pane.rows[0].image)
        assertEquals(1, paneTemplate.pane.actions.size)
        assertEquals(2, paneTemplate.header!!.endHeaderActions.size)
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
    fun carSettingsScreen_displaysCheckForUpdatesRow() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
        }
        val carContext = androidx.car.app.CarContext.create(owner.lifecycle)
        attachBaseContext(carContext, org.robolectric.RuntimeEnvironment.getApplication())

        if (org.koin.core.context.GlobalContext.getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
        val app = org.robolectric.RuntimeEnvironment.getApplication()

        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { fr.geoking.arthur.source.RotationSettings(app) }
                    single { fr.geoking.arthur.source.QuoteSettings(app) }
                    single { fr.geoking.arthur.source.AmbientAudioSettings(app) }
                },
            )
        }

        try {
            val screen = CarSettingsScreen(carContext)
            val template = screen.onGetTemplate() as androidx.car.app.model.ListTemplate
            val list = template.singleList
            assertNotNull(list)
            // check update (0) + quotes (1) + quote provider (2) + ambient sound (3) + intervals…
            val checkUpdateRow = list!!.items[0] as androidx.car.app.model.Row
            assertNotNull(checkUpdateRow.title)
            assertEquals(
                carContext.getString(fr.geoking.arthur.R.string.settings_check_update),
                checkUpdateRow.title.toString(),
            )
            assertEquals(
                carContext.getString(fr.geoking.arthur.R.string.screen_settings),
                template.header?.title.toString(),
            )
        } finally {
            org.koin.core.context.stopKoin()
        }
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
