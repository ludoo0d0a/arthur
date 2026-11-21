package fr.geoking.arthur.billing

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data object Cancelled : PurchaseResult()
    data object Pending : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}
