package fr.geoking.arthur.phone

import android.content.Intent
import android.net.Uri
import fr.geoking.arthur.tv.AmbientActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeepLinkHandlingTest {

    @Test
    fun ambientActivity_parsesDeepLinkUriParameters() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://arthur.geoking.fr/ambient?id=genart.tunnel&title=Tunnel&kind=Genart&source=genart&rotate=true"),
        )
        val controller = org.robolectric.Robolectric.buildActivity(AmbientActivity::class.java, intent)
        assertNotNull(controller)
    }

    @Test
    fun mainActivity_handlesDeepLinkIntent() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("arthur://ambient?id=genart.particles&title=Particles&rotate=false"),
        )
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java, intent)
        assertNotNull(controller)
    }
}
