package fr.geoking.arthur.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.R
import fr.geoking.arthur.UsedApisList
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorItem
import fr.geoking.arthur.shared.error.ErrorLogger
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.HttpCacheController
import fr.geoking.arthur.source.QuoteProvider
import fr.geoking.arthur.source.RotationSettings
import android.text.format.Formatter
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WebsiteUrl = "https://arthur.geoking.fr"
private const val PrivacyUrl = "https://arthur.geoking.fr/privacy.html"
private const val TermsUrl = "https://arthur.geoking.fr/terms.html"

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray,
): Modifier = drawWithContent {
    drawContent()
    val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: return@drawWithContent
    val totalItemsCount = state.layoutInfo.totalItemsCount
    if (totalItemsCount == 0) return@drawWithContent

    val visibleItemsCount = state.layoutInfo.visibleItemsInfo.size
    if (visibleItemsCount >= totalItemsCount) return@drawWithContent

    val elementHeight = size.height / totalItemsCount
    val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
    val scrollbarHeight = visibleItemsCount * elementHeight

    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
        size = Size(width.toPx(), scrollbarHeight),
        cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2),
    )
}

enum class SettingsScreenPage {
    Main,
    RotationInterval,
    PhoneRotationInterval,
    TvRotationInterval,
    AutoRotationInterval,
    QuoteProvider,
    DeviantArtCredentials,
    About,
    Licenses,
    Developer,
    DeveloperErrorLog,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onDismiss: () -> Unit,
    isPremium: Boolean = false,
    showDeveloper: Boolean = BuildConfig.DEBUG || BuildConfig.DEBUG_DEV,
    simulatePremium: Boolean = true,
    onSimulatePremiumChange: (Boolean) -> Unit = {},
    simulateAllPacks: Boolean = true,
    onSimulateAllPacksChange: (Boolean) -> Unit = {},
    verbose: Boolean = false,
    onVerboseChange: (Boolean) -> Unit = {},
    phoneIntervalMs: Long = AmbientAlbumArt.ROTATION_INTERVAL_MS,
    onPhoneIntervalChange: (Long) -> Unit = {},
    tvIntervalMs: Long = AmbientAlbumArt.ROTATION_INTERVAL_MS,
    onTvIntervalChange: (Long) -> Unit = {},
    autoIntervalMs: Long = AmbientAlbumArt.ROTATION_INTERVAL_MS,
    onAutoIntervalChange: (Long) -> Unit = {},
    wifiOnlyRemoteStills: Boolean = false,
    onWifiOnlyRemoteStillsChange: (Boolean) -> Unit = {},
    showQuotes: Boolean = true,
    onShowQuotesChange: (Boolean) -> Unit = {},
    quoteProvider: QuoteProvider = QuoteProvider.ZenQuotes,
    onQuoteProviderChange: (QuoteProvider) -> Unit = {},
    ambientSoundEnabled: Boolean = false,
    onAmbientSoundEnabledChange: (Boolean) -> Unit = {},
    deviantArtUsername: String = "",
    onDeviantArtUsernameChange: (String) -> Unit = {},
    deviantArtPassword: String = "",
    onDeviantArtPasswordChange: (String) -> Unit = {},
    onCheckForUpdate: (() -> Unit)? = null,
    onOpenMarketplace: (() -> Unit)? = null,
    initialScreenStack: List<SettingsScreenPage>? = null,
    onInitialRouteConsumed: () -> Unit = {},
    errorLogger: ErrorLogger? = null,
) {
    val activeErrorLogger = remember(errorLogger) {
        errorLogger ?: runCatching { GlobalContext.get().get<ErrorLogger>() }.getOrNull()
            ?: ErrorLogger()
    }
    var screenStack by remember { mutableStateOf(listOf(SettingsScreenPage.Main)) }
    val currentScreen = screenStack.last()

    LaunchedEffect(initialScreenStack) {
        val stack = initialScreenStack
        if (stack != null && stack.isNotEmpty()) {
            screenStack = stack
            onInitialRouteConsumed()
        }
    }

    BackHandler {
        if (screenStack.size > 1) {
            screenStack = screenStack.dropLast(1)
        } else {
            onDismiss()
        }
    }

    Scaffold(
        modifier = Modifier.testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            SettingsScreenPage.Main -> stringResource(R.string.screen_settings)
                            SettingsScreenPage.RotationInterval,
                            SettingsScreenPage.PhoneRotationInterval,
                            SettingsScreenPage.TvRotationInterval,
                            SettingsScreenPage.AutoRotationInterval ->
                                stringResource(R.string.screen_rotation_interval)
                            SettingsScreenPage.QuoteProvider ->
                                stringResource(R.string.settings_quote_provider)
                            SettingsScreenPage.DeviantArtCredentials -> "DeviantArt Credentials"
                            SettingsScreenPage.About -> stringResource(R.string.screen_about)
                            SettingsScreenPage.Licenses -> stringResource(R.string.screen_licenses)
                            SettingsScreenPage.Developer -> stringResource(R.string.screen_developer)
                            SettingsScreenPage.DeveloperErrorLog -> stringResource(R.string.dev_errors_list)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (screenStack.size > 1) {
                                screenStack = screenStack.dropLast(1)
                            } else {
                                onDismiss()
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (currentScreen) {
                SettingsScreenPage.Main -> MainMenu(
                    isPremium = isPremium,
                    showDeveloper = showDeveloper,
                    phoneIntervalMs = phoneIntervalMs,
                    tvIntervalMs = tvIntervalMs,
                    autoIntervalMs = autoIntervalMs,
                    wifiOnlyRemoteStills = wifiOnlyRemoteStills,
                    onWifiOnlyRemoteStillsChange = onWifiOnlyRemoteStillsChange,
                    showQuotes = showQuotes,
                    onShowQuotesChange = onShowQuotesChange,
                    quoteProvider = quoteProvider,
                    ambientSoundEnabled = ambientSoundEnabled,
                    onAmbientSoundEnabledChange = onAmbientSoundEnabledChange,
                    onCheckForUpdate = onCheckForUpdate,
                    onOpenMarketplace = onOpenMarketplace,
                    onNavigate = { screenStack = screenStack + it },
                )
                SettingsScreenPage.RotationInterval -> RotationIntervalSelectionMenu(
                    phoneIntervalMs = phoneIntervalMs,
                    tvIntervalMs = tvIntervalMs,
                    autoIntervalMs = autoIntervalMs,
                    onNavigate = { screenStack = screenStack + it },
                )
                SettingsScreenPage.PhoneRotationInterval -> RotationIntervalContent(
                    selectedMs = phoneIntervalMs,
                    onSelect = onPhoneIntervalChange,
                )
                SettingsScreenPage.TvRotationInterval -> RotationIntervalContent(
                    selectedMs = tvIntervalMs,
                    onSelect = onTvIntervalChange,
                )
                SettingsScreenPage.AutoRotationInterval -> RotationIntervalContent(
                    selectedMs = autoIntervalMs,
                    onSelect = onAutoIntervalChange,
                )
                SettingsScreenPage.QuoteProvider -> QuoteProviderContent(
                    selected = quoteProvider,
                    onSelect = onQuoteProviderChange,
                )
                SettingsScreenPage.DeviantArtCredentials -> DeviantArtCredentialsContent(
                    username = deviantArtUsername,
                    password = deviantArtPassword,
                    onUsernameChange = onDeviantArtUsernameChange,
                    onPasswordChange = onDeviantArtPasswordChange,
                )
                SettingsScreenPage.About -> AboutContent(
                    onOpenLicenses = { screenStack = screenStack + SettingsScreenPage.Licenses },
                )
                SettingsScreenPage.Licenses -> LicensesContent()
                SettingsScreenPage.Developer -> DeveloperContent(
                    simulatePremium = simulatePremium,
                    onSimulatePremiumChange = onSimulatePremiumChange,
                    simulateAllPacks = simulateAllPacks,
                    onSimulateAllPacksChange = onSimulateAllPacksChange,
                    verbose = verbose,
                    onVerboseChange = onVerboseChange,
                    onOpenErrorLog = { screenStack = screenStack + SettingsScreenPage.DeveloperErrorLog },
                )
                SettingsScreenPage.DeveloperErrorLog -> DeveloperErrorLogScreen(
                    errorLogger = activeErrorLogger,
                )
            }
        }
    }
}

@Composable
private fun MainMenu(
    isPremium: Boolean,
    showDeveloper: Boolean,
    phoneIntervalMs: Long,
    tvIntervalMs: Long,
    autoIntervalMs: Long,
    wifiOnlyRemoteStills: Boolean,
    onWifiOnlyRemoteStillsChange: (Boolean) -> Unit,
    showQuotes: Boolean,
    onShowQuotesChange: (Boolean) -> Unit,
    quoteProvider: QuoteProvider,
    ambientSoundEnabled: Boolean,
    onAmbientSoundEnabledChange: (Boolean) -> Unit,
    onCheckForUpdate: (() -> Unit)?,
    onOpenMarketplace: (() -> Unit)?,
    onNavigate: (SettingsScreenPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isPremium) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = Color(0xFFFACC15),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.premium_active),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.premium_ux_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        if (onOpenMarketplace != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                SettingsItem(
                    label = stringResource(R.string.marketplace_open),
                    value = stringResource(R.string.marketplace_subtitle),
                    onClick = onOpenMarketplace,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            SettingsItem(
                label = stringResource(R.string.screen_rotation_interval),
                value = "${stringResource(R.string.rotation_interval_phone)}: ${rotationIntervalLabel(phoneIntervalMs)}",
                onClick = { onNavigate(SettingsScreenPage.RotationInterval) },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_wifi_only_stills"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_wifi_only_stills),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_wifi_only_stills_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = wifiOnlyRemoteStills,
                    onCheckedChange = onWifiOnlyRemoteStillsChange,
                    modifier = Modifier.testTag("settings_wifi_only_stills_switch"),
                )
            }
            ImageCacheSettingsRow()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_show_quotes"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_show_quotes),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_show_quotes_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = showQuotes,
                    onCheckedChange = onShowQuotesChange,
                    modifier = Modifier.testTag("settings_show_quotes_switch"),
                )
            }
            SettingsItem(
                label = stringResource(R.string.settings_quote_provider),
                value = quoteProviderLabel(quoteProvider),
                onClick = { onNavigate(SettingsScreenPage.QuoteProvider) },
                modifier = Modifier.testTag("settings_quote_provider"),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_ambient_sound"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_ambient_sound),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_ambient_sound_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ambientSoundEnabled,
                    onCheckedChange = onAmbientSoundEnabledChange,
                    modifier = Modifier.testTag("settings_ambient_sound_switch"),
                )
            }
            SettingsItem(
                label = "DeviantArt Credentials",
                onClick = { onNavigate(SettingsScreenPage.DeviantArtCredentials) },
            )
            if (onCheckForUpdate != null) {
                SettingsItem(
                    label = stringResource(R.string.settings_check_update),
                    onClick = onCheckForUpdate,
                )
            }
            SettingsItem(
                label = stringResource(R.string.screen_about),
                value = stringResource(R.string.settings_about_subtitle),
                onClick = { onNavigate(SettingsScreenPage.About) },
            )
            if (showDeveloper) {
                SettingsItem(
                    label = stringResource(R.string.screen_developer),
                    value = stringResource(R.string.settings_developer_subtitle),
                    onClick = { onNavigate(SettingsScreenPage.Developer) },
                )
            }
        }
    }
}

@Composable
private fun ImageCacheSettingsRow() {
    val context = LocalContext.current
    val imageCache = remember {
        runCatching { GlobalContext.get().get<ArtworkImageCache>() }.getOrNull()
    }
    val httpCache = remember {
        runCatching { GlobalContext.get().get<HttpCacheController>() }.getOrNull()
    }
    var revision by remember { mutableStateOf(0) }
    val entryCount = remember(revision) { imageCache?.entryCount() ?: 0 }
    val totalBytes = remember(revision) {
        (imageCache?.totalBytes() ?: 0L) + (httpCache?.stats()?.sizeBytes ?: 0L)
    }
    val sizeLabel = remember(totalBytes) {
        Formatter.formatShortFileSize(context, totalBytes)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_image_cache"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = stringResource(R.string.settings_image_cache),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    R.string.settings_image_cache_subtitle,
                    sizeLabel,
                    entryCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("settings_image_cache_stats"),
            )
        }
        TextButton(
            onClick = {
                imageCache?.clearAll()
                httpCache?.clear()
                revision++
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_image_cache_cleared),
                    Toast.LENGTH_SHORT,
                ).show()
            },
            enabled = imageCache != null,
            modifier = Modifier.testTag("settings_image_cache_clear"),
        ) {
            Text(stringResource(R.string.settings_image_cache_clear))
        }
    }
}

@Composable
private fun RotationIntervalSelectionMenu(
    phoneIntervalMs: Long,
    tvIntervalMs: Long,
    autoIntervalMs: Long,
    onNavigate: (SettingsScreenPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_rotation_interval_menu"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_rotation_interval_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            SettingsItem(
                label = stringResource(R.string.rotation_interval_phone),
                value = rotationIntervalLabel(phoneIntervalMs),
                onClick = { onNavigate(SettingsScreenPage.PhoneRotationInterval) },
            )
            SettingsItem(
                label = stringResource(R.string.rotation_interval_tv),
                value = rotationIntervalLabel(tvIntervalMs),
                onClick = { onNavigate(SettingsScreenPage.TvRotationInterval) },
            )
            SettingsItem(
                label = stringResource(R.string.rotation_interval_auto),
                value = rotationIntervalLabel(autoIntervalMs),
                onClick = { onNavigate(SettingsScreenPage.AutoRotationInterval) },
            )
        }
    }
}

@Composable
private fun RotationIntervalContent(
    selectedMs: Long,
    onSelect: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_rotation_interval"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_rotation_interval_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            RotationSettings.OPTIONS_MS.forEach { ms ->
                val selected = ms == selectedMs
                ListItem(
                    modifier = Modifier
                        .clickable { onSelect(ms) }
                        .testTag("rotation_interval_$ms"),
                    headlineContent = {
                        Text(
                            rotationIntervalLabel(ms),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    trailingContent = {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun rotationIntervalLabel(ms: Long): String {
    return if (ms < 60_000L) {
        stringResource(R.string.rotation_interval_seconds, (ms / 1_000L).toInt())
    } else {
        stringResource(R.string.rotation_interval_minutes, (ms / 60_000L).toInt())
    }
}

@Composable
private fun DeveloperContent(
    simulatePremium: Boolean,
    onSimulatePremiumChange: (Boolean) -> Unit,
    simulateAllPacks: Boolean = true,
    onSimulateAllPacksChange: (Boolean) -> Unit = {},
    verbose: Boolean = false,
    onVerboseChange: (Boolean) -> Unit = {},
    onOpenErrorLog: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_developer"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.dev_simulate_premium),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.dev_simulate_premium_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = simulatePremium,
                    onCheckedChange = onSimulatePremiumChange,
                    modifier = Modifier.testTag("dev_simulate_premium"),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.dev_simulate_all_packs),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.dev_simulate_all_packs_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = simulateAllPacks,
                    onCheckedChange = onSimulateAllPacksChange,
                    modifier = Modifier.testTag("dev_simulate_all_packs"),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.dev_verbose),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.dev_verbose_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = verbose,
                    onCheckedChange = onVerboseChange,
                    modifier = Modifier.testTag("dev_verbose"),
                )
            }

            SettingsItem(
                label = stringResource(R.string.dev_errors_list),
                value = stringResource(R.string.dev_errors_list_subtitle),
                onClick = onOpenErrorLog,
            )
        }
    }
}

@Composable
fun DeveloperErrorLogScreen(
    errorLogger: ErrorLogger,
) {
    val errors by errorLogger.errors.collectAsState()
    var selectedCategory by remember { mutableStateOf<ErrorCategory?>(null) }
    var selectedSource by remember { mutableStateOf<String?>(null) }

    val filteredErrors = remember(errors, selectedCategory, selectedSource) {
        errors.filter { err ->
            (selectedCategory == null || err.category == selectedCategory) &&
                (selectedSource == null || err.sourceId.equals(selectedSource, ignoreCase = true))
        }
    }

    val sources = remember(errors) {
        errors.map { it.sourceId }.distinct().sorted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("developer_error_log_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${filteredErrors.size} / ${errors.size} errors",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { errorLogger.clearAll() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                enabled = errors.isNotEmpty(),
                modifier = Modifier.testTag("btn_clear_all_errors"),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.dev_clear_errors))
            }
        }

        // Category Filter Chips (Single Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                modifier = Modifier.testTag("chip_category_all"),
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All Categories") },
            )
            ErrorCategory.entries.forEach { cat ->
                FilterChip(
                    modifier = Modifier.testTag("chip_category_${cat.name}"),
                    selected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = if (selectedCategory == cat) null else cat
                    },
                    label = { Text(cat.name) },
                )
            }
        }

        // Source Filter Chips (Single Row)
        if (sources.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedSource == null,
                    onClick = { selectedSource = null },
                    label = { Text("All Sources") },
                )
                sources.forEach { src ->
                    FilterChip(
                        modifier = Modifier.testTag("chip_source_$src"),
                        selected = selectedSource.equals(src, ignoreCase = true),
                        onClick = {
                            selectedSource = if (selectedSource.equals(src, ignoreCase = true)) null else src
                        },
                        label = { Text(src) },
                    )
                }
            }
        }

        if (filteredErrors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.dev_no_errors),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredErrors, key = { it.id }) { item ->
                    ErrorItemCard(item)
                }
            }
        }
    }
}

@Composable
private fun ErrorItemCard(item: ErrorItem) {
    var expanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.dev_error_copied)

    val formattedTime = remember(item.timestamp) {
        if (item.timestamp > 0) {
            SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(item.timestamp))
        } else ""
    }

    val fullCopyText = remember(item, formattedTime) {
        buildString {
            append("[${item.category.name}] Source: ${item.sourceId}")
            if (item.statusCode != null) append(" (HTTP ${item.statusCode})")
            if (formattedTime.isNotBlank()) append(" @ $formattedTime")
            append("\nMessage: ${item.message}")
            if (!item.url.isNullOrBlank()) append("\nURL: ${item.url}")
            if (!item.details.isNullOrBlank()) append("\nDetails:\n${item.details}")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("error_card_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = when (item.category) {
                            ErrorCategory.Authentication -> Color(0xFFDC2626)
                            ErrorCategory.RateLimit -> Color(0xFFD97706)
                            ErrorCategory.Network -> Color(0xFF2563EB)
                            ErrorCategory.Payload -> Color(0xFF7C3AED)
                            ErrorCategory.Unknown -> Color(0xFF4B5563)
                        },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = item.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = item.sourceId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    if (item.statusCode != null) {
                        Text(
                            text = "HTTP ${item.statusCode}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("error", fullCopyText).toClipEntry(),
                                )
                                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_copy_error_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.dev_copy_error),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val itemUrl = item.url
            if (!itemUrl.isNullOrBlank()) {
                Text(
                    text = itemUrl,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            val itemDetails = item.details
            if (expanded && !itemDetails.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = itemDetails,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutContent(
    onOpenLicenses: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("settings_about"),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AboutRow(stringResource(R.string.about_version_name), BuildConfig.VERSION_NAME)
        AboutRow(stringResource(R.string.about_version_code), BuildConfig.VERSION_CODE.toString())
        AboutRow(stringResource(R.string.about_build_date), BuildConfig.BUILD_DATE)

        Spacer(modifier = Modifier.height(16.dp))
        AboutRowClickable(stringResource(R.string.about_privacy)) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyUrl)))
        }
        AboutRowClickable(stringResource(R.string.about_terms)) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TermsUrl)))
        }
        AboutRowClickable(stringResource(R.string.about_licenses), onClick = onOpenLicenses)
        AboutRowClickable(stringResource(R.string.about_website)) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WebsiteUrl)))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.about_used_apis),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        UsedApisList.forEach { api ->
            AboutApiRow(
                name = api.name,
                url = api.url,
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(api.url)))
                },
            )
        }
    }
}

@Composable
private fun LicensesContent() {
    val context = LocalContext.current
    val libraries by produceLibraries {
        context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
    }
    LibrariesContainer(
        libraries = libraries,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_licenses"),
    )
}

@Composable
private fun AboutApiRow(
    name: String,
    url: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AboutRowClickable(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(label, style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            if (value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun QuoteProviderContent(
    selected: QuoteProvider,
    onSelect: (QuoteProvider) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_quote_provider_page"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_quote_provider_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            QuoteProvider.entries.forEach { provider ->
                val isSelected = provider == selected
                ListItem(
                    modifier = Modifier
                        .clickable { onSelect(provider) }
                        .testTag("quote_provider_${provider.id}"),
                    headlineContent = {
                        Text(
                            quoteProviderLabel(provider),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun quoteProviderLabel(provider: QuoteProvider): String = when (provider) {
    QuoteProvider.ZenQuotes -> stringResource(R.string.settings_quote_provider_zenquotes)
    QuoteProvider.CitationLecog -> stringResource(R.string.settings_quote_provider_lecog)
}

@Composable
private fun DeviantArtCredentialsContent(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "DeviantArt API Credentials",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Enter your DeviantArt account details to use the password grant type. If left blank, the app will use the default client credentials.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsScreenPreview() {
    ArthurTheme {
        SettingsScreen(onDismiss = {}, isPremium = true)
    }
}
