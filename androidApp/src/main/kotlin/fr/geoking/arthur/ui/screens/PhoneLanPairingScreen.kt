package fr.geoking.arthur.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.pairing.LanPairingClient
import fr.geoking.arthur.pairing.LanPairingPrefs
import fr.geoking.arthur.pairing.LanPairingServer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.pairing.PairingCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phone Control Plane → TV Canvas LAN push.
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
    port: Int = LanPairingServer.DEFAULT_PORT,
) {
    BackHandler(onBack = onDismiss)
    var host by remember {
        mutableStateOf(initialHost?.takeIf { it.isNotBlank() } ?: pairingPrefs.lastHost)
    }
    var status by remember { mutableStateOf<PairingUiStatus>(PairingUiStatus.Idle) }
    var artworkCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pairing_phone_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text(stringResource(R.string.pairing_phone_host_label)) },
                placeholder = { Text(stringResource(R.string.pairing_phone_host_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_lan_pairing_host"),
            )
            Text(
                text = stringResource(R.string.pairing_phone_port_hint, port),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    val target = host.trim()
                    if (target.isEmpty()) {
                        status = PairingUiStatus.Error(R.string.pairing_phone_empty_host)
                        return@Button
                    }
                    status = PairingUiStatus.Working
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            pushPreparedRotation(
                                host = target,
                                port = port,
                                contentEngine = contentEngine,
                            )
                        }
                        when (result) {
                            is PushResult.Ok -> {
                                pairingPrefs.lastHost = target
                                artworkCount = result.count
                                status = PairingUiStatus.Success
                            }
                            is PushResult.Unreachable -> {
                                status = PairingUiStatus.Error(R.string.pairing_phone_unreachable)
                            }
                            is PushResult.EmptyCatalog -> {
                                status = PairingUiStatus.Error(R.string.pairing_phone_empty_catalog)
                            }
                            is PushResult.PushFailed -> {
                                status = PairingUiStatus.Error(R.string.pairing_phone_push_failed)
                            }
                        }
                    }
                },
                enabled = status != PairingUiStatus.Working,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_lan_pairing_push"),
            ) {
                Text(stringResource(R.string.pairing_phone_push))
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

private sealed interface PairingUiStatus {
    data object Idle : PairingUiStatus
    data object Working : PairingUiStatus
    data object Success : PairingUiStatus
    data class Error(val messageRes: Int) : PairingUiStatus
}

private sealed interface PushResult {
    data class Ok(val count: Int) : PushResult
    data object Unreachable : PushResult
    data object EmptyCatalog : PushResult
    data object PushFailed : PushResult
}

private suspend fun pushPreparedRotation(
    host: String,
    port: Int,
    contentEngine: ContentEngine,
): PushResult {
    val client = LanPairingClient(host = host, port = port)
    return try {
        if (!client.health()) return PushResult.Unreachable
        val catalog = contentEngine.catalog(
            PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()),
        )
        val pool = AmbientAlbumArt.sampleRotationPool(catalog)
        if (pool.isEmpty()) return PushResult.EmptyCatalog
        val json = PairingCodec.encodeManifest(pool)
        if (!client.pushManifest(json)) return PushResult.PushFailed
        PushResult.Ok(pool.size)
    } finally {
        client.close()
    }
}

/** Visible for unit tests — sample pool encoding path. */
internal fun artworksForPairingPush(catalog: List<Artwork>): List<Artwork> =
    AmbientAlbumArt.sampleRotationPool(catalog)
