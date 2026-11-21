package fr.geoking.arthur.billing

import android.content.Context
import android.os.Build
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import fr.geoking.arthur.BuildConfig

object RevenueCatInitializer {
    fun initialize(context: Context, apiKey: String) {
        if (Purchases.isConfigured || apiKey.isBlank()) return
        // Robolectric Application.onCreate runs before permissions are granted;
        // Purchases.configure require()-fails on INTERNET (PurchasesFactory L627).
        if (isRobolectric()) return
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        Purchases.configure(
            PurchasesConfiguration.Builder(context, apiKey)
                .appUserID(null)
                .build(),
        )
    }

    private fun isRobolectric(): Boolean =
        Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
}
