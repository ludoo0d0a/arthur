package fr.geoking.arthur.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.marketplace.SellablePack

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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(MarketplaceCatalog.all(), key = { it.id }) { pack ->
                    val owned = ownership.owns(pack.id).also { /* refreshEpoch gates recomposition */ }
                    @Suppress("UNUSED_EXPRESSION")
                    refreshEpoch
                    MarketplacePackRow(
                        pack = pack,
                        owned = owned,
                        highlighted = pack.id == highlightPackId,
                        canPurchase = canPurchaseOnDevice,
                        onUnlock = {
                            purchases.unlockEntitlement(pack.entitlementId)
                            refreshEpoch++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketplacePackRow(
    pack: SellablePack,
    owned: Boolean,
    highlighted: Boolean,
    canPurchase: Boolean,
    onUnlock: () -> Unit,
) {
    val title = marketplacePackTitle(pack)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("marketplace_pack_${pack.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (owned) {
                    stringResource(R.string.marketplace_owned)
                } else {
                    stringResource(R.string.marketplace_locked)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!owned && canPurchase) {
            Button(
                onClick = onUnlock,
                modifier = Modifier.testTag("marketplace_unlock_${pack.id}"),
            ) {
                Text(stringResource(R.string.marketplace_unlock))
            }
        }
    }
}

@Composable
private fun marketplacePackTitle(pack: SellablePack): String = when {
    pack.id == MarketplaceCatalog.PERSONAL_PHOTOS_ID ->
        stringResource(R.string.pack_personal)
    pack.genartTopicSuffix != null ->
        stringResource(R.string.marketplace_genart_pack, pack.genartTopicSuffix!!)
    else -> pack.id
}
