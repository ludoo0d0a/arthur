package fr.geoking.arthur.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.FeatureFlag
import fr.geoking.arthur.R
import fr.geoking.arthur.billing.PurchaseResult
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.shared.domain.ArtUniverse
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.marketplace.SellablePack
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.ui.components.AudioPackTopic
import fr.geoking.arthur.ui.components.GenartPackItemsSection
import fr.geoking.arthur.ui.components.GenartTopic
import fr.geoking.arthur.ui.components.PackCovers
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import fr.geoking.arthur.ui.components.resolvePackPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Boutique: packs + Premium / restore (ex-Compte). Phone + TV only — never Android Auto.
 *
 * - Not owned → open static still preview (+ buy).
 * - Owned → jump straight into the pack on the Control Plane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    ownership: PackOwnership,
    purchases: PurchasesGateway,
    contentEngine: ContentEngine,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onOpenOwnedPack: (PackSelection) -> Unit,
    onOpenPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    highlightPackId: String? = null,
    canPurchaseOnDevice: Boolean = true,
    activeUniverse: ArtUniverse = ArtUniverse.Gallery,
    onUniverseChange: ((ArtUniverse) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    val isTelevision = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val entitlements by purchases.activeEntitlementIds.collectAsState()
    val prices by purchases.productPrices.collectAsState()
    var browsingPackId by remember(highlightPackId) {
        mutableStateOf(highlightPackId)
    }
    var busy by remember { mutableStateOf(false) }
    var restoreBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var previewCatalog by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    // Deep-link / highlight opens full catalog so the pack is reachable.
    var showAllPacks by remember(highlightPackId) {
        mutableStateOf(!highlightPackId.isNullOrBlank())
    }
    val pendingMessage = stringResource(R.string.purchase_payment_pending)
    val restoreOk = stringResource(R.string.account_restore_success)
    val allTiles = remember(activeUniverse) { marketplacePackTiles(activeUniverse) }
    val featuredTiles = remember(allTiles) { marketplaceFeaturedTiles(allTiles) }
    val tiles = if (showAllPacks) allTiles else featuredTiles
    val browsingTile = allTiles.firstOrNull { it.sellablePackId == browsingPackId }
        ?: marketplacePackTiles(null).firstOrNull { it.sellablePackId == browsingPackId }
    val browsingPack = browsingPackId?.let { MarketplaceCatalog.byId(it) }
    val otherUniverse = when (activeUniverse) {
        ArtUniverse.Gallery -> ArtUniverse.Generative
        ArtUniverse.Generative -> ArtUniverse.Gallery
    }
    val seeOtherLabelRes = when (otherUniverse) {
        ArtUniverse.Gallery -> R.string.universe_marketplace_see_other_gallery
        ArtUniverse.Generative -> R.string.universe_marketplace_see_other_generative
    }
    val firstTileFocus = remember { FocusRequester() }
    val buyFocus = remember { FocusRequester() }
    val primaryFocus = remember { FocusRequester() }
    val hPad = if (isTelevision) 48.dp else 16.dp

    @Suppress("UNUSED_VARIABLE")
    val entitlementRevision = entitlements

    LaunchedEffect(Unit) {
        purchases.refresh()
        if (isTelevision && browsingPackId == null) {
            primaryFocus.requestFocus()
        }
    }

    LaunchedEffect(browsingPackId, isTelevision) {
        val pack = browsingPack
        if (isTelevision && pack != null && !ownership.owns(pack.id) && canPurchaseOnDevice) {
            buyFocus.requestFocus()
        } else if (isTelevision && browsingPackId == null) {
            firstTileFocus.requestFocus()
        }
    }

    LaunchedEffect(browsingPackId, browsingTile) {
        val tile = browsingTile
        val pack = browsingPack
        if (tile == null || pack == null || pack.genartTopicSuffix == null) {
            previewCatalog = emptyList()
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            contentEngine.browseCatalog(
                PreparedRotation(
                    sourceIds = listOf(GenartSource.ID, FractalSource.ID, CustomFractalSource.ID),
                    artworkIds = emptyList(),
                ),
            )
        }
        previewCatalog = resolvePackPool(loaded, tile.selection)
    }

    fun navigateBack() {
        if (browsingPackId != null) {
            browsingPackId = null
            error = null
        } else {
            onDismiss()
        }
    }

    BackHandler(onBack = ::navigateBack)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("marketplace_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (browsingPack != null) {
                            marketplacePackTitle(browsingPack)
                        } else {
                            stringResource(R.string.marketplace_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = ::navigateBack,
                        modifier = Modifier.testTag("marketplace_back"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (browsingPack != null && browsingTile != null) {
            MarketplacePackPreview(
                pack = browsingPack,
                tile = browsingTile,
                ownership = ownership,
                previewCatalog = previewCatalog,
                price = prices[browsingPack.productId],
                busy = busy,
                error = error,
                canPurchaseOnDevice = canPurchaseOnDevice,
                isTelevision = isTelevision,
                hPad = hPad,
                buyFocus = buyFocus,
                onBuy = {
                    busy = true
                    error = null
                    scope.launch {
                        when (
                            val result = purchases.purchaseProduct(
                                activity,
                                browsingPack.productId,
                            )
                        ) {
                            PurchaseResult.Success -> {
                                busy = false
                                if (browsingPack.id != MarketplaceCatalog.DIAMOND_ID) {
                                    onOpenOwnedPack(browsingTile.selection)
                                } else {
                                    browsingPackId = null
                                }
                            }
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                MarketplaceAccountHeader(
                    isPremium = isPremium,
                    restoreBusy = restoreBusy,
                    message = message,
                    isTelevision = isTelevision,
                    hPad = hPad,
                    primaryFocus = primaryFocus,
                    onOpenPaywall = onOpenPaywall,
                    onRestore = {
                        restoreBusy = true
                        message = null
                        scope.launch {
                            when (val result = purchases.restorePurchases()) {
                                PurchaseResult.Success -> {
                                    message = restoreOk
                                    restoreBusy = false
                                }
                                PurchaseResult.Cancelled -> restoreBusy = false
                                PurchaseResult.Pending -> {
                                    message = pendingMessage
                                    restoreBusy = false
                                }
                                is PurchaseResult.Error -> {
                                    message = result.message
                                    restoreBusy = false
                                }
                            }
                        }
                    },
                )
                if (!canPurchaseOnDevice) {
                    Text(
                        text = stringResource(R.string.marketplace_buy_on_phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = hPad, vertical = 8.dp),
                    )
                }
                if (!showAllPacks) {
                    Text(
                        text = stringResource(R.string.marketplace_featured),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = hPad, vertical = 4.dp)
                            .testTag("marketplace_featured_label"),
                    )
                }
                PackGrid(
                    tiles = tiles,
                    selected = null,
                    onTileClick = { tile ->
                        val packId = tile.sellablePackId ?: return@PackGrid
                        error = null
                        val owned = ownership.owns(packId)
                        if (owned && packId != MarketplaceCatalog.DIAMOND_ID) {
                            onOpenOwnedPack(tile.selection)
                        } else {
                            browsingPackId = packId
                        }
                    },
                    isLocked = { it.isLocked(ownership) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("marketplace_pack_grid"),
                    contentPadding = PaddingValues(
                        horizontal = hPad,
                        vertical = 16.dp,
                    ),
                    firstTileFocusRequester = firstTileFocus,
                    selectOnFocus = isTelevision,
                )
                if (allTiles.size > featuredTiles.size) {
                    TextButton(
                        onClick = { showAllPacks = !showAllPacks },
                        modifier = Modifier
                            .padding(horizontal = hPad, vertical = 4.dp)
                            .testTag("marketplace_browse_all"),
                    ) {
                        Text(
                            stringResource(
                                if (showAllPacks) {
                                    R.string.marketplace_show_featured
                                } else {
                                    R.string.marketplace_browse_all
                                },
                            ),
                        )
                    }
                }
                if (onUniverseChange != null) {
                    TextButton(
                        onClick = { onUniverseChange(otherUniverse) },
                        modifier = Modifier
                            .padding(horizontal = hPad, vertical = 8.dp)
                            .testTag("marketplace_see_other_universe"),
                    ) {
                        Text(stringResource(seeOtherLabelRes))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceAccountHeader(
    isPremium: Boolean,
    restoreBusy: Boolean,
    message: String?,
    isTelevision: Boolean,
    hPad: Dp,
    primaryFocus: FocusRequester,
    onOpenPaywall: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPad, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isPremium) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("marketplace_premium_card"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (isPremium) {
                        stringResource(R.string.premium_active)
                    } else {
                        stringResource(R.string.account_premium_inactive)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.premium_ux_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isPremium) {
                    Button(
                        onClick = onOpenPaywall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isTelevision) Modifier.focusRequester(primaryFocus) else Modifier,
                            )
                            .testTag("marketplace_buy_premium"),
                    ) {
                        Text(stringResource(R.string.premium_paywall_buy))
                    }
                }
            }
        }
        TextButton(
            onClick = onRestore,
            enabled = !restoreBusy,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isTelevision && isPremium) Modifier.focusRequester(primaryFocus) else Modifier,
                )
                .testTag("marketplace_restore"),
        ) {
            if (restoreBusy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_restore_purchases))
            }
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("marketplace_message"),
            )
        }
    }
}

@Composable
private fun MarketplacePackPreview(
    pack: SellablePack,
    tile: PackTile,
    ownership: PackOwnership,
    previewCatalog: List<Artwork>,
    price: String?,
    busy: Boolean,
    error: String?,
    canPurchaseOnDevice: Boolean,
    isTelevision: Boolean,
    hPad: Dp,
    buyFocus: FocusRequester,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val owned = ownership.owns(pack.id)
    val showStillPreviews = pack.genartTopicSuffix != null && previewCatalog.isNotEmpty()

    Column(modifier = modifier) {
        if (pack.id == MarketplaceCatalog.DIAMOND_ID) {
            Text(
                text = stringResource(R.string.pack_diamond_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = hPad, vertical = 4.dp),
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
            modifier = Modifier
                .padding(horizontal = hPad, vertical = 4.dp)
                .testTag("marketplace_pack_${pack.id}"),
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = hPad)
                    .testTag("marketplace_error"),
            )
        }
        if (!owned && canPurchaseOnDevice) {
            Button(
                onClick = onBuy,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = 8.dp)
                    .then(if (isTelevision) Modifier.focusRequester(buyFocus) else Modifier)
                    .testTag("marketplace_unlock_${pack.id}"),
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
        if (showStillPreviews) {
            GenartPackItemsSection(
                artworks = previewCatalog,
                packOwnership = ownership,
                packLocked = !owned,
                sellablePackId = pack.id,
                onBuyPack = null,
                onPlayArtwork = {},
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("marketplace_preview_grid"),
                contentPadding = PaddingValues(
                    start = hPad,
                    end = hPad,
                    bottom = 24.dp,
                ),
            )
        } else if (!owned) {
            Text(
                text = stringResource(tile.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = hPad, vertical = 8.dp),
            )
        }
    }
}

/**
 * Home vitrine: Diamond first, then curated featured packs present in [allTiles].
 */
internal fun marketplaceFeaturedTiles(allTiles: List<PackTile>): List<PackTile> {
    val byId = allTiles.associateBy { it.sellablePackId }
    val orderedIds = listOf(MarketplaceCatalog.DIAMOND_ID) +
        MarketplaceCatalog.featuredContentPackIds()
    val featured = orderedIds.mapNotNull { byId[it] }
    return featured.ifEmpty {
        allTiles.filter { it.sellablePackId == MarketplaceCatalog.DIAMOND_ID }
            .ifEmpty { allTiles.take(1) }
    }
}

/**
 * Marketplace tiles for [universe]. Diamond is always included.
 * Personal photos only in Gallery (when enabled). Genart packs only in Generative.
 * Audio packs omitted while [FeatureFlag.musicEnabled] is false.
 * Pass null to list every visible pack (preview deep-link lookup).
 *
 * Personal Photos stays gated by [FeatureFlag.personalPhotosEnabled] (deferred SKU).
 * Subscriptions are not offered — all products remain one-time IAPs.
 */
internal fun marketplacePackTiles(universe: ArtUniverse? = null): List<PackTile> =
    MarketplaceCatalog.all()
        .filter { pack ->
            when {
                pack.id == MarketplaceCatalog.DIAMOND_ID -> true
                pack.id == MarketplaceCatalog.PERSONAL_PHOTOS_ID ->
                    FeatureFlag.personalPhotosEnabled &&
                        (universe == null || universe == ArtUniverse.Gallery)
                pack.genartTopicSuffix != null ->
                    universe == null || universe == ArtUniverse.Generative
                pack.audioPackSuffix != null ->
                    FeatureFlag.musicEnabled && universe == null
                else -> universe == null
            }
        }
        .map { pack ->
            when {
                pack.id == MarketplaceCatalog.DIAMOND_ID -> PackTile(
                    id = "market_${pack.id}",
                    titleRes = R.string.pack_diamond,
                    coverRes = R.drawable.pack_diamond,
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
