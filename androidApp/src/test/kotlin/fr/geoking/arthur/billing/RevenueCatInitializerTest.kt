package fr.geoking.arthur.billing

import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.Purchases
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RevenueCatInitializerTest {

    @Test
    fun initialize_skipsConfigureUnderRobolectric() {
        assertFalse(Purchases.isConfigured)
        RevenueCatInitializer.initialize(
            ApplicationProvider.getApplicationContext(),
            apiKey = "goog_unit_test_key_that_must_not_configure",
        )
        assertFalse(
            "Purchases.configure must not run under Robolectric (INTERNET require)",
            Purchases.isConfigured,
        )
    }
}
