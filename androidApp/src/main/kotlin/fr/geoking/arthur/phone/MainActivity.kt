package fr.geoking.arthur.phone

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.geoking.arthur.billing.FakePurchasesGateway
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.fractal.CustomFractalStore
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.source.StockPhotoSettings
import fr.geoking.arthur.tv.AmbientActivity
import fr.geoking.arthur.ui.screens.ControlPlaneScreen
import fr.geoking.arthur.ui.screens.CustomFractalEditorScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val contentEngine: ContentEngine by inject()
    private val premium: PremiumEntitlement by inject()
    private val purchases: PurchasesGateway by inject()
    private val customFractalStore: CustomFractalStore by inject()
    private val stockPhotoSettings: StockPhotoSettings by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val isTelevision = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
        setContent {
            ArthurTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showEditor by remember { mutableStateOf(false) }
                    var catalogEpoch by remember { mutableStateOf(0) }
                    // Custom fractal authoring needs touch; TV uses remote only.
                    if (showEditor && !isTelevision) {
                        CustomFractalEditorScreen(
                            isPremium = premium.isPremium,
                            onSave = { params ->
                                val art = customFractalStore.save(params)
                                catalogEpoch++
                                art
                            },
                            onClose = { showEditor = false },
                            onRequestPremium = {
                                (purchases as? FakePurchasesGateway)?.setPremium(true)
                                showEditor = false
                                showEditor = true
                            },
                        )
                    } else {
                        key(catalogEpoch) {
                            ControlPlaneScreen(
                                contentEngine = contentEngine,
                                stockPhotoSettings = stockPhotoSettings,
                                onStartAmbient = { artwork ->
                                    startActivity(AmbientActivity.intent(this@MainActivity, artwork?.id))
                                },
                                onCreateCustomFractal = if (isTelevision) {
                                    null
                                } else {
                                    { showEditor = true }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
