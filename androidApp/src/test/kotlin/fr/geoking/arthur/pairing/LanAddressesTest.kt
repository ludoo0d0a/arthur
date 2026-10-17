package fr.geoking.arthur.pairing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class LanAddressesTest {

    @Test
    fun preferredLanIpv4_neverReturnsNullOrBlank() {
        val ip = preferredLanIpv4()
        assertNotNull(ip)
        assertFalse(ip.isBlank())
    }

    @Test
    fun lanIpv4Addresses_returnsNonEmptyList() {
        val addresses = lanIpv4Addresses()
        assertFalse(addresses.isEmpty())
    }
}
