package fr.geoking.arthur.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
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

/**
 * Marketplace browse / fake unlock. Phone + TV only — never Android Auto.
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
    var refreshEpoch by remember { mutableIntStateOf(0) }
    var selectedPackId by remember(highlightPackId) {
        mutableStateOf(highlightPackId)
    }
    val tiles = remember { marketplacePackTiles() }
    val selectedTile = tiles.firstOrNull { it.sellablePackId == selectedPackId }
    val selectedPack = selectedPackId?.let { MarketplaceCatalog.byId(it) }
    @Suppress("UNUSED_EXPRESSION")
    refreshEpoch

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
                },
                isLocked = { it.isLocked(ownership) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("marketplace_pack_grid"),
                contentPadding = PaddingValues(16.dp),
            )
            if (selectedPack != null) {
                val owned = ownership.owns(selectedPack.id)
                Text(
                    text = marketplacePackTitle(selectedPack),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("marketplace_pack_${selectedPack.id}"),
                )
                Text(
                    text = if (owned) {
                        stringResource(R.string.marketplace_owned)
                    } else {
                        stringResource(R.string.marketplace_locked)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                if (!owned && canPurchaseOnDevice) {
                    Button(
                        onClick = {
                            purchases.unlockEntitlement(selectedPack.entitlementId)
                            refreshEpoch++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("marketplace_unlock_${selectedPack.id}"),
                    ) {
                        Text(stringResource(R.string.marketplace_unlock))
                    }
                }
            }
        }
    }
}

internal fun marketplacePackTiles(): List<PackTile> =
    MarketplaceCatalog.all().map { pack ->
        when {
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
                PackTile(
                    id = "market_${pack.id}",
                    titleRes = topic.labelRes,
                    coverRes = PackCovers.audio(topic),
                    selection = PackSelection(PackFamily.Sound, topic.testTagSuffix),
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
