package fr.geoking.arthur.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueCatPremiumEntitlementTest {
    @Test
    fun fakeGateway_togglesPremium() {
        val gateway = FakePurchasesGateway(premium = false)
        val entitlement = RevenueCatPremiumEntitlement(gateway)
        assertFalse(entitlement.isPremium)
        gateway.setPremium(true)
        assertTrue(entitlement.isPremium)
    }
}
