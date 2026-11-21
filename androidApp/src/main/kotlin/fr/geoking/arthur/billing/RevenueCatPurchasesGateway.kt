package fr.geoking.arthur.billing

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetProducts
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live RevenueCat adapter: CustomerInfo → entitlement set, purchase/restore by Play product id.
 */
class RevenueCatPurchasesGateway : PurchasesGateway {
    private val _activeEntitlementIds = MutableStateFlow<Set<String>>(emptySet())
    override val activeEntitlementIds: StateFlow<Set<String>> = _activeEntitlementIds.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    fun attachListener() {
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> applyCustomerInfo(info) }
    }

    override fun hasEntitlement(id: String): Boolean = id in _activeEntitlementIds.value

    override suspend fun refresh() {
        try {
            applyCustomerInfo(Purchases.sharedInstance.awaitCustomerInfo())
            refreshPrices()
        } catch (_: PurchasesException) {
            // Keep last known entitlements / prices.
        }
    }

    override suspend fun purchaseProduct(activity: Activity, productId: String): PurchaseResult {
        return try {
            val params = purchaseParamsFor(activity, productId)
                ?: return PurchaseResult.Error("Product not available")
            val result = Purchases.sharedInstance.awaitPurchase(params)
            applyCustomerInfo(result.customerInfo)
            PurchaseResult.Success
        } catch (e: PurchasesTransactionException) {
            mapPurchaseException(e)
        } catch (e: PurchasesException) {
            PurchaseResult.Error(userFacingMessage(e.error))
        }
    }

    override suspend fun restorePurchases(): PurchaseResult {
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            applyCustomerInfo(info)
            if (info.entitlements.active.isEmpty()) {
                PurchaseResult.Error("No purchases found for this Google account.")
            } else {
                PurchaseResult.Success
            }
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) PurchaseResult.Cancelled else PurchaseResult.Error(userFacingMessage(e.error))
        } catch (e: PurchasesException) {
            PurchaseResult.Error(userFacingMessage(e.error))
        }
    }

    private suspend fun purchaseParamsFor(
        activity: Activity,
        productId: String,
    ): PurchaseParams? {
        val offerings = Purchases.sharedInstance.awaitOfferings()
        val pkg = offerings.current?.availablePackages?.firstOrNull { it.product.id == productId }
            ?: offerings.all.values.flatMap { it.availablePackages }
                .firstOrNull { it.product.id == productId }
        if (pkg != null) {
            return PurchaseParams.Builder(activity, pkg).build()
        }
        val products = Purchases.sharedInstance.awaitGetProducts(
            productIds = listOf(productId),
            type = ProductType.INAPP,
        )
        val product = products.firstOrNull() ?: return null
        return PurchaseParams.Builder(activity, product).build()
    }

    private suspend fun refreshPrices() {
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val prices = buildMap {
                offerings.all.values.flatMap { it.availablePackages }.forEach { pkg ->
                    put(pkg.product.id, pkg.product.price.formatted)
                }
            }
            _productPrices.value = prices
        } catch (_: PurchasesException) {
            // Leave cached prices.
        }
    }

    private fun applyCustomerInfo(info: CustomerInfo) {
        _activeEntitlementIds.value = info.entitlements.active.keys
    }

    private suspend fun mapPurchaseException(e: PurchasesTransactionException): PurchaseResult {
        if (e.userCancelled) return PurchaseResult.Cancelled
        return when (e.code) {
            PurchasesErrorCode.PaymentPendingError -> PurchaseResult.Pending
            PurchasesErrorCode.ProductAlreadyPurchasedError -> {
                refresh()
                PurchaseResult.Success
            }
            else -> PurchaseResult.Error(userFacingMessage(e.error))
        }
    }

    companion object {
        fun userFacingMessage(error: PurchasesError): String = when (error.code) {
            PurchasesErrorCode.NetworkError ->
                "Please check your internet connection and try again."
            PurchasesErrorCode.StoreProblemError ->
                "There was a problem with Google Play. Please try again."
            PurchasesErrorCode.ProductAlreadyPurchasedError ->
                "You already own this purchase."
            PurchasesErrorCode.PaymentPendingError ->
                "Your payment is being processed. Access unlocks when it completes."
            PurchasesErrorCode.PurchaseNotAllowedError ->
                "Purchases are not allowed on this device or account."
            PurchasesErrorCode.ProductNotAvailableForPurchaseError ->
                "This product is not available for purchase right now."
            else -> "Something went wrong. Please try again."
        }
    }
}
