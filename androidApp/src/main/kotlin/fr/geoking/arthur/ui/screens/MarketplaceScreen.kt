package fr.geoking.arthur.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.FeatureFlag
import fr.geoking.arthur.R
import fr.geoking.arthur.billing.PurchaseResult
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.marketplace.SellablePack
import fr.geoking.arthur.ui.components.AudioPackTopic
import fr.geoking.arthur.ui.components.GenartTopic
import fr.geoking.arthur.ui.components.PackCovers
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import kotlinx.coroutines.launch

/**
 * Marketplace browse / purchase. Phone + TV only — never Android Auto.
 * Pack SKUs map to RevenueCat product ids from [MarketplaceCatalog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    ownership: PackOwnership,
    purchases: PurchasesGateway,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    highlightPackId: String? = null,
    canPurchaseOnDevice: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    val isTelevision = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val entitlements by purchases.activeEntitlementIds.collectAsState()
    val prices by purchases.productPrices.collectAsState()
    var selectedPackId by remember(highlightPackId) {
        mutableStateOf(highlightPackId)
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pendingMessage = stringResource(R.string.purchase_payment_pending)
    val tiles = remember { marketplacePackTiles() }
    val selectedTile = tiles.firstOrNull { it.sellablePackId == selectedPackId }
    val selectedPack = selectedPackId?.let { MarketplaceCatalog.byId(it) }
    val firstTileFocus = remember { FocusRequester() }
    val buyFocus = remember { FocusRequester() }

    @Suppress("UNUSED_VARIABLE")
    val entitlementRevision = entitlements

    LaunchedEffect(Unit) {
        purchases.refresh()
        if (isTelevision) {
            firstTileFocus.requestFocus()
        }
    }

    LaunchedEffect(selectedPackId, isTelevision) {
        if (isTelevision && selectedPack != null &&
            !ownership.owns(selectedPack.id) && canPurchaseOnDevice
        ) {
            buyFocus.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("marketplace_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.marketplace_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("marketplace_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!canPurchaseOnDevice) {
                Text(
                    text = stringResource(R.string.marketplace_buy_on_phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            PackGrid(
                tiles = tiles,
                selected = selectedTile?.selection,
                onTileClick = { tile ->
                    selectedPackId = tile.sellablePackId
                    error = null
                },
                isLocked = { it.isLocked(ownership) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("marketplace_pack_grid"),
                contentPadding = PaddingValues(
                    horizontal = if (isTelevision) 48.dp else 16.dp,
                    vertical = 16.dp,
                ),
                firstTileFocusRequester = firstTileFocus,
                selectOnFocus = isTelevision,
                onTileFocused = { tile ->
                    selectedPackId = tile.sellablePackId
                    error = null
                },
            )
            if (selectedPack != null) {
                val owned = ownership.owns(selectedPack.id)
                val price = prices[selectedPack.productId]
                Text(
                    text = marketplacePackTitle(selectedPack),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = if (isTelevision) 48.dp else 16.dp)
                        .testTag("marketplace_pack_${selectedPack.id}"),
                )
                if (selectedPack.id == MarketplaceCatalog.DIAMOND_ID) {
                    Text(
                        text = stringResource(R.string.pack_diamond_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = if (isTelevision) 48.dp else 16.dp,
                            vertical = 2.dp,
                        ),
                    )
                }
                Text(
                    text = if (owned) {
                        stringResource(R.string.marketplace_owned)
                    } else {
                        stringResource(R.string.marketplace_locked)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = if (isTelevision) 48.dp else 16.dp,
                        vertical = 4.dp,
                    ),
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = if (isTelevision) 48.dp else 16.dp)
                            .testTag("marketplace_error"),
                    )
                }
                if (!owned && canPurchaseOnDevice) {
                    Button(
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                when (
                                    val result = purchases.purchaseProduct(
                                        activity,
                                        selectedPack.productId,
                                    )
                                ) {
                                    PurchaseResult.Success -> busy = false
                                    PurchaseResult.Cancelled -> busy = false
                                    PurchaseResult.Pending -> {
                                        error = pendingMessage
                                        busy = false
                                    }
                                    is PurchaseResult.Error -> {
                                        error = result.message
                                        busy = false
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isTelevision) 48.dp else 16.dp)
                            .then(if (isTelevision) Modifier.focusRequester(buyFocus) else Modifier)
                            .testTag("marketplace_unlock_${selectedPack.id}"),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            val label = if (price != null) {
                                stringResource(R.string.marketplace_buy_priced, price)
                            } else {
                                stringResource(R.string.marketplace_unlock)
                            }
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

internal fun marketplacePackTiles(): List<PackTile> =
    MarketplaceCatalog.all()
        .filter { pack ->
            FeatureFlag.personalPhotosEnabled ||
                pack.id != MarketplaceCatalog.PERSONAL_PHOTOS_ID
        }
        .map { pack ->
            when {
                pack.id == MarketplaceCatalog.DIAMOND_ID -> PackTile(
                    id = "market_${pack.id}",
                    titleRes = R.string.pack_diamond,
                    coverRes = R.drawable.pack_random,
                    selection = PackSelection(PackFamily.Museum),
                    testTagSuffix = "market_${pack.id}",
                    sellablePackId = pack.id,
                )
                pack.id == MarketplaceCatalog.PERSONAL_PHOTOS_ID -> PackTile(
                    id = "market_${pack.id}",
                    titleRes = R.string.pack_personal,
                    coverRes = R.drawable.pack_photo,
                    selection = PackSelection(PackFamily.Personal),
                    testTagSuffix = "market_${pack.id}",
                    sellablePackId = pack.id,
                )
                pack.genartTopicSuffix != null -> {
                    val topic = GenartTopic.entries.firstOrNull {
                        it.testTagSuffix == pack.genartTopicSuffix
                    } ?: GenartTopic.Nature
                    PackTile(
                        id = "market_${pack.id}",
                        titleRes = topic.labelRes,
                        coverRes = PackCovers.genart(topic),
                        selection = PackSelection(PackFamily.Genart, topic.testTagSuffix),
                        testTagSuffix = "market_${pack.id}",
                        sellablePackId = pack.id,
                    )
                }
                pack.audioPackSuffix != null -> {
                    val topic = AudioPackTopic.entries.firstOrNull {
                        it.packSuffix == pack.audioPackSuffix
                    } ?: AudioPackTopic.HearthWeather
                    val family = when {
                        topic == AudioPackTopic.DownloadedMusic -> PackFamily.DownloadedMusic
                        topic.isNeuralDedicated -> PackFamily.Neural
                        else -> PackFamily.Sound
                    }
                    PackTile(
                        id = "market_${pack.id}",
                        titleRes = topic.labelRes,
                        coverRes = PackCovers.audio(topic),
                        selection = PackSelection(family, topic.testTagSuffix),
                        testTagSuffix = "market_${pack.id}",
                        sellablePackId = pack.id,
                    )
                }
                else -> PackTile(
                    id = "market_${pack.id}",
                    titleRes = R.string.marketplace_title,
                    coverRes = R.drawable.pack_random,
                    selection = PackSelection(PackFamily.Museum),
                    testTagSuffix = "market_${pack.id}",
                    sellablePackId = pack.id,
                )
            }
        }

@Composable
internal fun marketplacePackTitle(pack: SellablePack): String = when {
    pack.id == MarketplaceCatalog.DIAMOND_ID ->
        stringResource(R.string.pack_diamond)
    pack.id == MarketplaceCatalog.PERSONAL_PHOTOS_ID ->
        stringResource(R.string.pack_personal)
    pack.genartTopicSuffix != null ->
        stringResource(R.string.marketplace_genart_pack, pack.genartTopicSuffix!!)
    pack.audioPackSuffix != null -> {
        val topic = AudioPackTopic.entries.firstOrNull { it.packSuffix == pack.audioPackSuffix }
        if (topic != null) {
            stringResource(R.string.marketplace_audio_pack, stringResource(topic.labelRes))
        } else {
            stringResource(R.string.marketplace_audio_pack, pack.audioPackSuffix!!)
        }
    }
    else -> pack.id
}
