package fr.geoking.arthur.auto

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

        val pane = Pane.Builder()
            .addRow(row)
            .build()

        val header = Header.Builder()
            .setTitle("Arthur")
            .build()

        val paneTemplate = PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()

        assertNotNull(paneTemplate)
        assertEquals(1, paneTemplate.pane.rows.size)
        assertNotNull(paneTemplate.pane.rows[0].image)
    }

    @Test
    fun automotiveAppDesc_declaresTemplateSupport() {
        val file = File("src/main/res/xml/automotive_app_desc.xml")
        assertTrue("automotive_app_desc.xml must exist", file.exists())
        val content = file.readText()
        assertTrue("Must contain <uses name=\"template\"", content.contains("<uses name=\"template\""))
    }
}
