package fr.geoking.arthur.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import org.koin.core.context.GlobalContext

/**
 * Phone Control Plane banner for playstore free tier.
 * Hidden when [PremiumEntitlement.isPremium] (including debug simulate-premium).
 */
@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val premium = remember {
        runCatching { GlobalContext.get().get<PremiumEntitlement>() }.getOrNull()
    }
    if (premium?.isPremium == true) return

    val context = LocalContext.current
    val adRequest = remember { AdRequest.Builder().build() }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.i("AdMobBanner", "Banner loaded (unitId=$adUnitId)")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(
                            "AdMobBanner",
                            "Banner failed (unitId=$adUnitId) code=${error.code} " +
                                "domain=${error.domain} message=${error.message}",
                        )
                    }
                }
                loadAd(adRequest)
            }
        },
        update = { view ->
            if (view.adUnitId != adUnitId) {
                view.adUnitId = adUnitId
                Log.i("AdMobBanner", "Banner unit id changed -> reload (unitId=$adUnitId)")
                view.loadAd(adRequest)
            }
        },
    )
}
