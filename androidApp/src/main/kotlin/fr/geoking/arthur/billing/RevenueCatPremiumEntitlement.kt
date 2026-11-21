package fr.geoking.arthur.billing

import android.app.Activity
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val activeEntitlementIds: StateFlow<Set<String>>
    val productPrices: StateFlow<Map<String, String>>

    fun hasEntitlement(id: String): Boolean

    suspend fun refresh() {}

    suspend fun purchaseProduct(activity: Activity, productId: String): PurchaseResult

    suspend fun restorePurchases(): PurchaseResult

    /** Fake / debug unlock only; real RC adapter ignores this. */
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

    private val _activeEntitlementIds = MutableStateFlow(entitlements.toSet())
    override val activeEntitlementIds: StateFlow<Set<String>> = _activeEntitlementIds.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    fun setPremium(value: Boolean) {
        if (value) {
            entitlements.add(RevenueCatPremiumEntitlement.ENTITLEMENT_ID)
        } else {
            entitlements.remove(RevenueCatPremiumEntitlement.ENTITLEMENT_ID)
        }
        publish()
    }

    fun setPackEntitlements(ids: Set<String>) {
        entitlements.removeAll { it != RevenueCatPremiumEntitlement.ENTITLEMENT_ID && it.startsWith("pack_") }
        entitlements.addAll(ids)
        publish()
    }

    fun setProductPrice(productId: String, formatted: String) {
        _productPrices.value = _productPrices.value + (productId to formatted)
    }

    override fun unlockEntitlement(id: String) {
        entitlements.add(id)
        publish()
    }

    override fun hasEntitlement(id: String): Boolean = id in entitlements

    override suspend fun purchaseProduct(activity: Activity, productId: String): PurchaseResult {
        val entitlementId = entitlementIdForProduct(productId)
            ?: return PurchaseResult.Error("Unknown product")
        unlockEntitlement(entitlementId)
        return PurchaseResult.Success
    }

    override suspend fun restorePurchases(): PurchaseResult {
        return if (entitlements.isEmpty()) {
            PurchaseResult.Error("No purchases found for this Google account.")
        } else {
            PurchaseResult.Success
        }
    }

    private fun publish() {
        _activeEntitlementIds.value = entitlements.toSet()
    }

    companion object {
        fun entitlementIdForProduct(productId: String): String? {
            if (productId == RevenueCatPremiumEntitlement.PRODUCT_ID) {
                return RevenueCatPremiumEntitlement.ENTITLEMENT_ID
            }
            return MarketplaceCatalog.all().firstOrNull { it.productId == productId }?.entitlementId
        }
    }
}

/**
 * Marketplace pack ownership via RevenueCat entitlement ids from [MarketplaceCatalog].
 */
class RevenueCatPackOwnership(
    private val gateway: PurchasesGateway,
) : PackOwnership {
    override fun owns(packId: String): Boolean {
        val pack = MarketplaceCatalog.byId(packId) ?: return false
        if (gateway.hasEntitlement(pack.entitlementId)) return true
        // Diamond once-for-all: unlocks every content pack (not Premium UX).
        if (!MarketplaceCatalog.isDiamond(packId) &&
            gateway.hasEntitlement(MarketplaceCatalog.DIAMOND_ENTITLEMENT)
        ) {
            return true
        }
        return false
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
