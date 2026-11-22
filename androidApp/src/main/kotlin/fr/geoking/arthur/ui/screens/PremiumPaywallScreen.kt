package fr.geoking.arthur.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.billing.PurchaseResult
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.billing.RevenueCatPremiumEntitlement
import kotlinx.coroutines.launch

/**
 * Lifetime Premium paywall (UX entitlement only — packs stay on Marketplace).
 * Full-screen so TV remotes can focus Buy / Restore / Dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    purchases: PurchasesGateway,
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    val isTelevision = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val prices by purchases.productPrices.collectAsState()
    val priceLabel = prices[RevenueCatPremiumEntitlement.PRODUCT_ID]
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pendingMessage = stringResource(R.string.purchase_payment_pending)
    val buyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        purchases.refresh()
        if (isTelevision) buyFocus.requestFocus()
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("premium_paywall_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium_paywall_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("premium_paywall_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (isTelevision) 48.dp else 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.premium_paywall_headline),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.premium_paywall_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.premium_paywall_packs_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("premium_paywall_error"),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        when (
                            val result = purchases.purchaseProduct(
                                activity,
                                RevenueCatPremiumEntitlement.PRODUCT_ID,
                            )
                        ) {
                            PurchaseResult.Success -> onPurchaseSuccess()
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
                    .then(if (isTelevision) Modifier.focusRequester(buyFocus) else Modifier)
                    .testTag("premium_paywall_buy"),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    val label = if (priceLabel != null) {
                        stringResource(R.string.premium_paywall_buy_priced, priceLabel)
                    } else {
                        stringResource(R.string.premium_paywall_buy)
                    }
                    Text(label)
                }
            }

            TextButton(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        when (val result = purchases.restorePurchases()) {
                            PurchaseResult.Success -> onPurchaseSuccess()
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
                    .testTag("premium_paywall_restore"),
            ) {
                Text(stringResource(R.string.action_restore_purchases))
            }

            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("premium_paywall_later"),
            ) {
                Text(stringResource(R.string.action_maybe_later))
            }
        }
    }
}
