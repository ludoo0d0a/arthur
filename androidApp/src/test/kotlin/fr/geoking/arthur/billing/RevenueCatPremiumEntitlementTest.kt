package fr.geoking.arthur.billing

import fr.geoking.arthur.shared.domain.FakePremiumEntitlement
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
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
    fun fakeGateway_unlocksPackEntitlementIndependently() {
        val gateway = FakePurchasesGateway(premium = false)
        val ownership = RevenueCatPackOwnership(gateway)
        assertFalse(ownership.ownsPersonalPhotos())
        gateway.unlockEntitlement(MarketplaceCatalog.PERSONAL_PHOTOS_ENTITLEMENT)
        assertTrue(ownership.ownsPersonalPhotos())
        assertFalse(RevenueCatPremiumEntitlement(gateway).isPremium)
    }

    @Test
    fun fakeGateway_unlocksAudioPackIndependentlyOfPremium() {
        val gateway = FakePurchasesGateway(premium = false)
        val ownership = RevenueCatPackOwnership(gateway)
        val hearth = MarketplaceCatalog.audioPack("hearth_weather")
        val violin = MarketplaceCatalog.audioPack("solo_violin")
        assertFalse(ownership.owns(hearth.id))
        gateway.unlockEntitlement(hearth.entitlementId)
        assertTrue(ownership.owns(hearth.id))
        assertTrue(ownership.allowsMusicStyle("ocean_waves"))
        assertFalse(ownership.owns(violin.id))
        assertFalse(RevenueCatPremiumEntitlement(gateway).isPremium)
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

    @Test
    fun devAwarePackOwnership_orsSimulateAllPacksOverride() {
        var simulatePacks = false
        val gateway = FakePurchasesGateway(premium = false)
        val ownership = DevAwarePackOwnership(
            delegate = RevenueCatPackOwnership(gateway),
            simulateAllPacks = { simulatePacks },
        )
        assertFalse(ownership.ownsPersonalPhotos())
        assertFalse(ownership.ownsCustomFractal())

        simulatePacks = true
        assertTrue(ownership.ownsPersonalPhotos())
        assertTrue(ownership.ownsCustomFractal())
    }
}
