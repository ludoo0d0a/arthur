package fr.geoking.arthur.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import fr.geoking.arthur.R
import fr.geoking.arthur.pairing.DiscoveredPairingTv
import fr.geoking.arthur.pairing.LanPairingPrefs
import fr.geoking.arthur.pairing.LanPairingServer
import fr.geoking.arthur.pairing.PairingDeepLink
import fr.geoking.arthur.pairing.PairingNsdBrowser
import fr.geoking.arthur.pairing.PairingPushResult
import fr.geoking.arthur.pairing.PairingTvDiscovery
import fr.geoking.arthur.pairing.pushPreparedRotation
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.engine.ContentEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phone Control Plane → TV Canvas LAN push (NSD + QR, no IP typing).
 * Dedicated screen — does not alter Control Plane / Ambient layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLanPairingScreen(
    contentEngine: ContentEngine,
    pairingPrefs: LanPairingPrefs,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialHost: String? = null,
    initialPort: Int = LanPairingServer.DEFAULT_PORT,
    autoPush: Boolean = false,
    discovery: PairingTvDiscovery? = null,
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val lastTvLabel = stringResource(R.string.pairing_phone_last_tv)
    val scannedTvLabel = stringResource(R.string.pairing_phone_scanned_tv)
    val browser = remember(discovery) {
        discovery ?: PairingNsdBrowser(context)
    }
    val discovered by browser.tvs.collectAsState()
    var selected by remember { mutableStateOf<DiscoveredPairingTv?>(null) }
    var status by remember { mutableStateOf<PairingUiStatus>(PairingUiStatus.Idle) }
    var artworkCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var autoPushConsumed by remember { mutableStateOf(false) }

    DisposableEffect(browser) {
        browser.start()
        onDispose { browser.stop() }
    }

    LaunchedEffect(discovered) {
        if (selected == null && discovered.isNotEmpty()) {
            selected = discovered.first()
        }
    }

    fun runPush(host: String, port: Int) {
        if (host.isBlank()) {
            status = PairingUiStatus.Error(R.string.pairing_phone_no_tv)
            return
        }
        status = PairingUiStatus.Working
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                pushPreparedRotation(host = host, port = port, contentEngine = contentEngine)
            }
            when (result) {
                is PairingPushResult.Ok -> {
                    pairingPrefs.remember(host, port)
                    artworkCount = result.count
                    status = PairingUiStatus.Success
                }
                PairingPushResult.Unreachable -> {
                    status = PairingUiStatus.Error(R.string.pairing_phone_unreachable)
                }
                PairingPushResult.EmptyCatalog -> {
                    status = PairingUiStatus.Error(R.string.pairing_phone_empty_catalog)
                }
                PairingPushResult.PushFailed -> {
                    status = PairingUiStatus.Error(R.string.pairing_phone_push_failed)
                }
            }
        }
    }

    LaunchedEffect(autoPush, initialHost, initialPort) {
        if (!autoPush || autoPushConsumed) return@LaunchedEffect
        val host = initialHost?.trim().orEmpty()
        if (host.isEmpty()) return@LaunchedEffect
        autoPushConsumed = true
        runPush(host, initialPort)
    }

    // Prefill last successful TV for one-tap sync (no auto-push).
    LaunchedEffect(Unit) {
        if (autoPush) return@LaunchedEffect
        val last = pairingPrefs.lastHost.trim()
        if (last.isNotEmpty() && selected == null) {
            selected = DiscoveredPairingTv(
                serviceName = lastTvLabel,
                host = last,
                port = pairingPrefs.lastPort,
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("phone_lan_pairing_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pairing_phone_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("phone_lan_pairing_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pairing_phone_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (discovered.isEmpty() && selected == null) {
                Text(
                    text = stringResource(R.string.pairing_phone_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("phone_lan_pairing_searching"),
                )
            } else {
                Text(
                    text = stringResource(R.string.pairing_phone_found_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                val rows = buildList {
                    addAll(discovered)
                    val sel = selected
                    if (sel != null && discovered.none { it.host == sel.host && it.port == sel.port }) {
                        add(sel)
                    }
                }
                rows.forEach { tv ->
                    val isSelected = selected?.host == tv.host && selected?.port == tv.port
                    Text(
                        text = tv.serviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = tv }
                            .padding(vertical = 8.dp)
                            .testTag("phone_lan_pairing_tv_${tv.host}"),
                    )
                }
            }

            Button(
                onClick = {
                    val target = selected
                    if (target == null) {
                        status = PairingUiStatus.Error(R.string.pairing_phone_no_tv)
                        return@Button
                    }
                    runPush(target.host, target.port)
                },
                enabled = status != PairingUiStatus.Working && selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_lan_pairing_push"),
            ) {
                Text(stringResource(R.string.pairing_phone_push))
            }

            OutlinedButton(
                onClick = {
                    launchQrScan(context) { raw ->
                        val target = PairingDeepLink.parse(raw)
                        if (target == null) {
                            status = PairingUiStatus.Error(R.string.pairing_phone_qr_invalid)
                        } else {
                            selected = DiscoveredPairingTv(
                                serviceName = scannedTvLabel,
                                host = target.host,
                                port = target.port,
                            )
                            runPush(target.host, target.port)
                        }
                    }
                },
                enabled = status != PairingUiStatus.Working,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_lan_pairing_scan"),
            ) {
                Text(stringResource(R.string.pairing_phone_scan))
            }

            when (val s = status) {
                PairingUiStatus.Idle -> Unit
                PairingUiStatus.Working -> {
                    Text(
                        text = stringResource(R.string.pairing_phone_working),
                        modifier = Modifier.testTag("phone_lan_pairing_status"),
                    )
                }
                PairingUiStatus.Success -> {
                    Text(
                        text = stringResource(R.string.pairing_phone_success, artworkCount),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("phone_lan_pairing_status"),
                    )
                }
                is PairingUiStatus.Error -> {
                    Text(
                        text = stringResource(s.messageRes),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("phone_lan_pairing_status"),
                    )
                }
            }
        }
    }
}

private fun launchQrScan(context: Context, onRaw: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = GmsBarcodeScanning.getClient(context, options)
    scanner.startScan()
        .addOnSuccessListener { barcode ->
            val raw = barcode.rawValue
            if (!raw.isNullOrBlank()) onRaw(raw)
        }
        .addOnFailureListener {
            // User cancel / scanner unavailable — no-op (UI stays idle).
        }
}

private sealed interface PairingUiStatus {
    data object Idle : PairingUiStatus
    data object Working : PairingUiStatus
    data object Success : PairingUiStatus
    data class Error(val messageRes: Int) : PairingUiStatus
}

/** Visible for unit tests — sample pool encoding path. */
internal fun artworksForPairingPush(catalog: List<Artwork>): List<Artwork> =
    fr.geoking.arthur.pairing.artworksForPairingPush(catalog)
