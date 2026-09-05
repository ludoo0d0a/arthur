package fr.geoking.arthur.billing

import fr.geoking.arthur.shared.domain.FakePremiumEntitlement
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

    @Test
    fun devAware_orsSimulateOverride() {
        var simulate = false
        val entitlement = DevAwarePremiumEntitlement(
            delegate = FakePremiumEntitlement(isPremium = false),
            simulatePremium = { simulate },
        )
        assertFalse(entitlement.isPremium)
        simulate = true
        assertTrue(entitlement.isPremium)
    }

    @Test
    fun devAware_realPremiumIgnoresSimulateOff() {
        val entitlement = DevAwarePremiumEntitlement(
            delegate = FakePremiumEntitlement(isPremium = true),
            simulatePremium = { false },
        )
        assertTrue(entitlement.isPremium)
    }
}
