package fr.geoking.arthur.phone

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.play.core.install.model.InstallStatus
import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.fractal.CustomFractalStore
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.marketplace.GenartPackTopics
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.source.DeveloperSettings
import fr.geoking.arthur.source.HttpCacheController
import fr.geoking.arthur.source.MuseumSearchSettings
import fr.geoking.arthur.source.QuoteSettings
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.ScreensaverSettings
import fr.geoking.arthur.source.StockPhotoSettings
import fr.geoking.arthur.tv.AmbientActivity
import fr.geoking.arthur.tv.AmbientRotationLaunch
import fr.geoking.arthur.ui.UpdateAvailableDialog
import fr.geoking.arthur.ui.UpdateCheckFeedbackDialog
import fr.geoking.arthur.ui.UpdateInProgressBanner
import fr.geoking.arthur.ui.components.debug.ArthurDebugLogOverlay
import fr.geoking.arthur.ui.screens.ControlPlaneScreen
import fr.geoking.arthur.ui.screens.CustomFractalEditorScreen
import fr.geoking.arthur.ui.screens.MarketplaceScreen
import fr.geoking.arthur.ui.screens.SettingsScreen
import fr.geoking.arthur.update.CheckFeedback
import fr.geoking.arthur.update.InAppUpdateHelper
import org.koin.android.ext.android.inject
class MainActivity : ComponentActivity() {
    private val contentEngine: ContentEngine by inject()
    private val premium: PremiumEntitlement by inject()
    private val packOwnership: PackOwnership by inject()
    private val debugLogger: DebugLogger by inject()
    private val httpCacheController: HttpCacheController by inject()
    private val purchases: PurchasesGateway by inject()
    private val customFractalStore: CustomFractalStore by inject()
    private val stockPhotoSettings: StockPhotoSettings by inject()
    private val museumSearchSettings: MuseumSearchSettings by inject()
    private val developerSettings: DeveloperSettings by inject()
    private val rotationSettings: RotationSettings by inject()
    private val screensaverSettings: ScreensaverSettings by inject()
    private val quoteSettings: QuoteSettings by inject()

    private val inAppUpdateHelper by lazy { InAppUpdateHelper(applicationContext) }

    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { /* cancel / failure: no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val isTelevision = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION

        inAppUpdateHelper.checkForUpdate()
        handleDeepLinkIntent(intent)

        setContent {
            ArthurTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val updateAvailable by inAppUpdateHelper.updateAvailable.collectAsState()
                    val installStatus by inAppUpdateHelper.installStatus.collectAsState()
                    val checkFeedback by inAppUpdateHelper.checkFeedback.collectAsState()
                    val isUpdateInProgress = installStatus == InstallStatus.PENDING ||
                        installStatus == InstallStatus.DOWNLOADING ||
                        installStatus == InstallStatus.INSTALLING

                    Box(modifier = Modifier.fillMaxSize()) {
                        var showEditor by remember { mutableStateOf(false) }
                        var showSettings by remember { mutableStateOf(false) }
                        var showMarketplace by remember { mutableStateOf(false) }
                        var marketplaceHighlight by remember { mutableStateOf<String?>(null) }
                        var catalogEpoch by remember { mutableStateOf(0) }
                        val simulatePremium by developerSettings.simulatePremium.collectAsState()
                        val simulateAllPacks by developerSettings.simulateAllPacks.collectAsState()
                        val verbose by developerSettings.verbose.collectAsState()
                        val phoneIntervalMs by rotationSettings.phoneIntervalMs.collectAsState()
                        val tvIntervalMs by rotationSettings.tvIntervalMs.collectAsState()
                        val autoIntervalMs by rotationSettings.autoIntervalMs.collectAsState()
                        val wifiOnlyRemoteStills by rotationSettings.wifiOnlyRemoteStills.collectAsState()
                        val showQuotes by quoteSettings.showQuotes.collectAsState()
                        val isPremium = premium.isPremium
                        // Custom fractal authoring needs touch; TV uses remote only.
                        when {
                            showMarketplace -> {
                                MarketplaceScreen(
                                    ownership = packOwnership,
                                    purchases = purchases,
                                    onDismiss = {
                                        showMarketplace = false
                                        marketplaceHighlight = null
                                        catalogEpoch++
                                    },
                                    highlightPackId = marketplaceHighlight,
                                    canPurchaseOnDevice = true,
                                )
                            }
                            showSettings -> {
                                SettingsScreen(
                                    onDismiss = { showSettings = false },
                                    isPremium = isPremium,
                                    showDeveloper = BuildConfig.DEBUG || BuildConfig.DEBUG_DEV,
                                    simulatePremium = simulatePremium,
                                    onSimulatePremiumChange = developerSettings::setSimulatePremium,
                                    simulateAllPacks = simulateAllPacks,
                                    onSimulateAllPacksChange = developerSettings::setSimulateAllPacks,
                                    verbose = verbose,
                                    onVerboseChange = developerSettings::setVerbose,
                                    phoneIntervalMs = phoneIntervalMs,
                                    onPhoneIntervalChange = rotationSettings::setPhoneIntervalMs,
                                    tvIntervalMs = tvIntervalMs,
                                    onTvIntervalChange = rotationSettings::setTvIntervalMs,
                                    autoIntervalMs = autoIntervalMs,
                                    onAutoIntervalChange = rotationSettings::setAutoIntervalMs,
                                    wifiOnlyRemoteStills = wifiOnlyRemoteStills,
                                    onWifiOnlyRemoteStillsChange = rotationSettings::setWifiOnlyRemoteStills,
                                    showQuotes = showQuotes,
                                    onShowQuotesChange = quoteSettings::setShowQuotes,
                                    deviantArtUsername = stockPhotoSettings.deviantArtUsername,
                                    onDeviantArtUsernameChange = { stockPhotoSettings.deviantArtUsername = it },
                                    deviantArtPassword = stockPhotoSettings.deviantArtPassword,
                                    onDeviantArtPasswordChange = { stockPhotoSettings.deviantArtPassword = it },
                                    onCheckForUpdate = {
                                        inAppUpdateHelper.checkForUpdate(manual = true)
                                    },
                                    onOpenMarketplace = {
                                        showSettings = false
                                        marketplaceHighlight = null
                                        showMarketplace = true
                                    },
                                )
                            }
                            showEditor && !isTelevision -> {
                                CustomFractalEditorScreen(
                                    isPremium = packOwnership.ownsCustomFractal(),
                                    onSave = { params ->
                                        val art = customFractalStore.save(params)
                                        catalogEpoch++
                                        art
                                    },
                                    onClose = { showEditor = false },
                                    onRequestPremium = {
                                        marketplaceHighlight = MarketplaceCatalog.genartPackId(
                                            GenartPackTopics.CUSTOM,
                                        )
                                        showEditor = false
                                        showMarketplace = true
                                    },
                                )
                            }
                            else -> {
                                key(catalogEpoch, isPremium, packOwnership.ownsPersonalPhotos(), simulateAllPacks) {
                                    ControlPlaneScreen(
                                        contentEngine = contentEngine,
                                        stockPhotoSettings = stockPhotoSettings,
                                        museumSearchSettings = museumSearchSettings,
                                        screensaverSettings = screensaverSettings,
                                        packOwnership = packOwnership,
                                        onOpenMarketplace = { packId ->
                                            marketplaceHighlight = packId
                                            showMarketplace = true
                                        },
                                        onStartAmbient = { artwork, pool, renewSourceIds ->
                                            AmbientRotationLaunch.prepare(pool, renewSourceIds)
                                            startActivity(
                                                AmbientActivity.intent(
                                                    this@MainActivity,
                                                    artwork,
                                                    rotate = pool.size >= 2,
                                                ),
                                            )
                                        },
                                        onCreateCustomFractal = if (isTelevision) {
                                            null
                                        } else {
                                            { showEditor = true }
                                        },
                                        onOpenSettings = { showSettings = true },
                                    )
                                }
                            }
                        }

                        if (isUpdateInProgress) {
                            UpdateInProgressBanner(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth(),
                            )
                        }

                        if (verbose) {
                            ArthurDebugLogOverlay(
                                debugLogger = debugLogger,
                                cacheController = httpCacheController,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 80.dp),
                            )
                        }
                    }

                    updateAvailable?.let { info ->
                        UpdateAvailableDialog(
                            onCancel = { inAppUpdateHelper.dismissUpdate() },
                            onUpdate = {
                                inAppUpdateHelper.startUpdate(info, updateResultLauncher)
                            },
                        )
                    }

                    when (val feedback = checkFeedback) {
                        is CheckFeedback.UpToDate -> {
                            UpdateCheckFeedbackDialog(
                                isError = false,
                                onDismiss = { inAppUpdateHelper.resetCheckFeedback() },
                            )
                        }
                        is CheckFeedback.Error -> {
                            UpdateCheckFeedbackDialog(
                                isError = true,
                                errorMessage = feedback.message,
                                onDismiss = { inAppUpdateHelper.resetCheckFeedback() },
                            )
                        }
                        CheckFeedback.None -> Unit
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (intent.action == android.content.Intent.ACTION_VIEW) {
            val isAmbient = uri.host == "ambient" ||
                uri.path?.contains("/ambient") == true ||
                uri.getQueryParameter("mode") == "ambient" ||
                uri.getQueryParameter("artwork_id") != null ||
                uri.getQueryParameter("id") != null
            if (isAmbient) {
                val ambientIntent = android.content.Intent(this, AmbientActivity::class.java).apply {
                    data = uri
                }
                startActivity(ambientIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (inAppUpdateHelper.installStatus.value == InstallStatus.DOWNLOADED) {
            inAppUpdateHelper.completeUpdate()
        }
    }

    override fun onDestroy() {
        inAppUpdateHelper.unregister()
        super.onDestroy()
    }
}
