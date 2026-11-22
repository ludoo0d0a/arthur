package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.billing.PurchaseResult
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import kotlinx.coroutines.launch

/**
 * Account / purchases management: Premium status, restore, owned packs.
 * Phone + TV (D-pad friendly full screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    purchases: PurchasesGateway,
    ownership: PackOwnership,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenMarketplace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isTelevision = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val entitlements by purchases.activeEntitlementIds.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val pendingMessage = stringResource(R.string.purchase_payment_pending)
    val restoreOk = stringResource(R.string.account_restore_success)
    val primaryFocus = remember { FocusRequester() }

    val ownsDiamond = ownership.ownsDiamond()
    val ownedPacks = remember(entitlements, ownership, ownsDiamond) {
        if (ownsDiamond) {
            listOf(MarketplaceCatalog.diamond)
        } else {
            MarketplaceCatalog.contentPacks().filter { ownership.owns(it.id) }
        }
    }

    LaunchedEffect(Unit) {
        purchases.refresh()
        if (isTelevision) primaryFocus.requestFocus()
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("account_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("account_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = if (isTelevision) 48.dp else 16.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
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
                        .testTag("account_premium_card"),
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
                                    .testTag("account_buy_premium"),
                            ) {
                                Text(stringResource(R.string.premium_paywall_buy))
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            when (val result = purchases.restorePurchases()) {
                                PurchaseResult.Success -> {
                                    message = restoreOk
                                    busy = false
                                }
                                PurchaseResult.Cancelled -> busy = false
                                PurchaseResult.Pending -> {
                                    message = pendingMessage
                                    busy = false
                                }
                                is PurchaseResult.Error -> {
                                    message = result.message
                                    busy = false
                                }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isTelevision && isPremium) Modifier.focusRequester(primaryFocus) else Modifier,
                        )
                        .testTag("account_restore"),
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_restore_purchases))
                    }
                }
            }

            if (message != null) {
                item {
                    Text(
                        text = message!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("account_message"),
                    )
                }
            }

            item {
                Button(
                    onClick = onOpenMarketplace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_open_marketplace"),
                ) {
                    Text(stringResource(R.string.marketplace_open))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.account_owned_packs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (ownsDiamond) {
                item {
                    Text(
                        text = stringResource(R.string.pack_diamond),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_owned_diamond"),
                    )
                    Text(
                        text = stringResource(R.string.pack_diamond_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (ownedPacks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.account_no_packs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("account_no_packs"),
                    )
                }
            } else {
                items(ownedPacks, key = { it.id }) { pack ->
                    Text(
                        text = marketplacePackTitle(pack),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("account_owned_${pack.id}"),
                    )
                }
            }

            item {
                Text(
                    text = stringResource(
                        R.string.account_entitlement_count,
                        entitlements.size,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
