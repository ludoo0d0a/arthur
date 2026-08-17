package fr.geoking.arthur.billing

import fr.geoking.arthur.shared.domain.PremiumEntitlement

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

interface PurchasesGateway {
    fun hasEntitlement(id: String): Boolean
}

class FakePurchasesGateway(
    private var premium: Boolean = false,
) : PurchasesGateway {
    fun setPremium(value: Boolean) {
        premium = value
    }

    override fun hasEntitlement(id: String): Boolean =
        premium && id == RevenueCatPremiumEntitlement.ENTITLEMENT_ID
}
