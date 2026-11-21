package fr.geoking.arthur.billing

import android.content.Context
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import fr.geoking.arthur.BuildConfig

object RevenueCatInitializer {
    fun initialize(context: Context, apiKey: String) {
        if (Purchases.isConfigured) return
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        Purchases.configure(
            PurchasesConfiguration.Builder(context, apiKey)
                .appUserID(null)
                .build(),
        )
    }
}
