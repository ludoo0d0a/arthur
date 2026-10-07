package fr.geoking.arthur.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.pairing.LanPairingServer
import fr.geoking.arthur.pairing.PairingDeepLink
import fr.geoking.arthur.pairing.PairingManifestMapper
import fr.geoking.arthur.pairing.PairingNsdAdvertiser
import fr.geoking.arthur.pairing.PairingQrEncoder
import fr.geoking.arthur.pairing.preferredLanIpv4
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.pairing.PairingCodec

/**
 * TV Canvas host: shows QR + NSD, waits for a Control Plane push.
 * Dedicated screen — does not alter Control Plane / Ambient layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvLanPairingHostScreen(
    onDismiss: () -> Unit,
    onRotationReceived: (List<Artwork>) -> Unit,
    modifier: Modifier = Modifier,
    port: Int = LanPairingServer.DEFAULT_PORT,
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val hostIp = remember { preferredLanIpv4() }
    val pairingUrl = remember(hostIp, port) {
        hostIp?.let { PairingDeepLink.build(host = it, port = port) }
    }
    val qrBitmap: Bitmap? = remember(pairingUrl) {
        pairingUrl?.let { PairingQrEncoder.encode(it, sizePx = 640) }
    }
    val server = remember {
        LanPairingServer(port = port, deviceName = "Arthur TV")
    }
    val nsd = remember {
        PairingNsdAdvertiser(context = context, port = port, deviceName = "Arthur TV")
    }
    var started by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf<String?>(null) }
    val manifestJson by server.latestManifestJson.collectAsState()
    val received = remember(manifestJson) {
        manifestJson?.let { raw ->
            runCatching {
                PairingManifestMapper.toArtworks(PairingCodec.decodeManifest(raw))
            }.getOrNull()
        }
    }

    LaunchedEffect(server) {
        try {
            server.start()
            nsd.start()
            started = true
            startError = null
        } catch (e: Exception) {
            started = false
            startError = e.message ?: e::class.simpleName
        }
    }

    DisposableEffect(server, nsd) {
        onDispose {
            runCatching { nsd.stop() }
            runCatching { server.stop() }
        }
    }

    LaunchedEffect(manifestJson) {
        val pool = received
        if (pool != null && pool.isNotEmpty()) {
            onRotationReceived(pool)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_lan_pairing_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pairing_tv_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("tv_lan_pairing_back")) {
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.pairing_tv_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (startError != null) {
                Text(
                    text = stringResource(R.string.pairing_tv_error, startError!!),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("tv_lan_pairing_error"),
                )
            } else if (started) {
                Text(
                    text = stringResource(R.string.pairing_tv_waiting),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("tv_lan_pairing_waiting"),
                )
            }
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.pairing_tv_qr_cd),
                    modifier = Modifier
                        .size(280.dp)
                        .testTag("tv_lan_pairing_qr"),
                )
            } else {
                Text(
                    text = stringResource(R.string.pairing_tv_no_ip),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.pairing_tv_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (hostIp != null) {
                Text(
                    text = "$hostIp:$port",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.testTag("tv_lan_pairing_endpoint"),
                )
            }
            if (received != null) {
                Text(
                    text = stringResource(R.string.pairing_tv_received, received.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("tv_lan_pairing_received"),
                )
                Button(
                    onClick = { onRotationReceived(received) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_lan_pairing_start_ambient"),
                ) {
                    Text(stringResource(R.string.pairing_tv_start_ambient))
                }
            }
        }
    }
}
