package fr.geoking.arthur.pairing

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PairingDeepLinkTest {

    @Test
    fun build_and_parse_roundTrip() {
        val raw = PairingDeepLink.build(host = "192.168.1.20", port = 8742)
        assertEquals("arthur://pair?host=192.168.1.20&port=8742", raw)
        val target = PairingDeepLink.parse(raw)
        assertNotNull(target)
        assertEquals("192.168.1.20", target!!.host)
        assertEquals(8742, target.port)
    }

    @Test
    fun parse_defaultsPort() {
        val target = PairingDeepLink.parse(Uri.parse("arthur://pair?host=10.0.0.5"))
        assertEquals(LanPairingServer.DEFAULT_PORT, target!!.port)
    }

    @Test
    fun parse_rejectsMissingHost() {
        assertNull(PairingDeepLink.parse(Uri.parse("arthur://pair")))
    }

    @Test
    fun wantsPairing_arthurScheme() {
        assertTrue(PairingDeepLink.wantsPairing(Uri.parse("arthur://pair?host=1.2.3.4")))
        assertTrue(PairingDeepLink.wantsPairing(Uri.parse("arthur://pairing")))
        assertFalse(PairingDeepLink.wantsPairing(Uri.parse("arthur://ambient?id=x")))
    }

    @Test
    fun qrEncoder_producesNonEmptyBitmap() {
        val bmp = PairingQrEncoder.encode(PairingDeepLink.build("192.168.0.1"), sizePx = 128)
        assertEquals(128, bmp.width)
        assertEquals(128, bmp.height)
    }
}
