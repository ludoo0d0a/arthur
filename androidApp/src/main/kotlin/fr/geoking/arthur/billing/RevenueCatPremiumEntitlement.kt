package fr.geoking.arthur.billing

import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership

/**
 * RevenueCat-backed entitlement. Uses [PurchasesGateway] so unit tests stay fake.
 */
class RevenueCatPremiumEntitlement(
    private val gateway: PurchasesGateway,
) : PremiumEntitlement {
    override val isPremium: Boolean
        get() = gateway.hasEntitlement(ENTITLEMENT_ID)

    companion object {
        const val ENTITLEMENT_ID = "premium"
        const val PRODUCT_ID = "arthur_premium_lifetime"
    }
}

/**
 * ORs a real entitlement with an optional developer override (debug simulate premium).
 * Premium is global UX only — it does not unlock Marketplace packs.
 */
class DevAwarePremiumEntitlement(
    private val delegate: PremiumEntitlement,
    private val simulatePremium: () -> Boolean,
) : PremiumEntitlement {
    override val isPremium: Boolean
        get() = delegate.isPremium || simulatePremium()
}

interface PurchasesGateway {
    fun hasEntitlement(id: String): Boolean

    /** Fake / debug unlock; real RC adapter no-ops until purchase flow ships. */
    fun unlockEntitlement(id: String) {}
}

/**
 * In-memory gateway for tests and debug. Tracks Premium and pack entitlements independently.
 */
class FakePurchasesGateway(
    premium: Boolean = false,
    packEntitlements: Set<String> = emptySet(),
) : PurchasesGateway {
    private val entitlements = mutableSetOf<String>().apply {
        if (premium) add(RevenueCatPremiumEntitlement.ENTITLEMENT_ID)
        addAll(packEntitlements)
    }

    fun setPremium(value: Boolean) {
        if (value) {
            entitlements.add(RevenueCatPremiumEntitlement.ENTITLEMENT_ID)
        } else {
            entitlements.remove(RevenueCatPremiumEntitlement.ENTITLEMENT_ID)
        }
    }

    fun setPackEntitlements(ids: Set<String>) {
        entitlements.removeAll { it != RevenueCatPremiumEntitlement.ENTITLEMENT_ID && it.startsWith("pack_") }
        entitlements.addAll(ids)
    }

    override fun unlockEntitlement(id: String) {
        entitlements.add(id)
    }

    override fun hasEntitlement(id: String): Boolean = id in entitlements
}

/**
 * Marketplace pack ownership via RevenueCat entitlement ids from [MarketplaceCatalog].
 */
class RevenueCatPackOwnership(
    private val gateway: PurchasesGateway,
) : PackOwnership {
    override fun owns(packId: String): Boolean {
        val pack = MarketplaceCatalog.byId(packId) ?: return false
        return gateway.hasEntitlement(pack.entitlementId)
    }
}

/**
 * ORs pack ownership with optional developer unlock-all (debug).
 */
class DevAwarePackOwnership(
    private val delegate: PackOwnership,
    private val simulateAllPacks: () -> Boolean = { false },
) : PackOwnership {
    override fun owns(packId: String): Boolean =
        simulateAllPacks() || delegate.owns(packId)
}
