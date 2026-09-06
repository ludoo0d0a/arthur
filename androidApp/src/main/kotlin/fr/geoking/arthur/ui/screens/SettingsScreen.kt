package fr.geoking.arthur.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.R
import fr.geoking.arthur.UsedApisList
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorItem
import fr.geoking.arthur.shared.error.ErrorLogger
import fr.geoking.arthur.source.RotationSettings
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WebsiteUrl = "https://arthur.geoking.fr"
private const val PrivacyUrl = "https://arthur.geoking.fr/privacy.html"
private const val TermsUrl = "https://arthur.geoking.fr/terms.html"

enum class SettingsScreenPage {
    Main,
    RotationInterval,
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
    showDeveloper: Boolean = BuildConfig.DEBUG,
    simulatePremium: Boolean = true,
    onSimulatePremiumChange: (Boolean) -> Unit = {},
    rotationIntervalMs: Long = AmbientAlbumArt.ROTATION_INTERVAL_MS,
    onRotationIntervalChange: (Long) -> Unit = {},
    onCheckForUpdate: (() -> Unit)? = null,
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
                            SettingsScreenPage.RotationInterval ->
                                stringResource(R.string.screen_rotation_interval)
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
                    rotationIntervalMs = rotationIntervalMs,
                    onCheckForUpdate = onCheckForUpdate,
                    onNavigate = { screenStack = screenStack + it },
                )
                SettingsScreenPage.RotationInterval -> RotationIntervalContent(
                    selectedMs = rotationIntervalMs,
                    onSelect = onRotationIntervalChange,
                )
                SettingsScreenPage.About -> AboutContent(
                    onOpenLicenses = { screenStack = screenStack + SettingsScreenPage.Licenses },
                )
                SettingsScreenPage.Licenses -> LicensesContent()
                SettingsScreenPage.Developer -> DeveloperContent(
                    simulatePremium = simulatePremium,
                    onSimulatePremiumChange = onSimulatePremiumChange,
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
    rotationIntervalMs: Long,
    onCheckForUpdate: (() -> Unit)?,
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
                            stringResource(R.string.premium_thanks),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            SettingsItem(
                label = stringResource(R.string.screen_rotation_interval),
                value = rotationIntervalLabel(rotationIntervalMs),
                onClick = { onNavigate(SettingsScreenPage.RotationInterval) },
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

            SettingsItem(
                label = stringResource(R.string.dev_errors_list),
                value = stringResource(R.string.dev_errors_list_subtitle),
                onClick = onOpenErrorLog,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
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

        // Category Filter Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All Categories") },
            )
            ErrorCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = if (selectedCategory == cat) null else cat
                    },
                    label = { Text(cat.name) },
                )
            }
        }

        // Source Filter Chips
        if (sources.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedSource == null,
                    onClick = { selectedSource = null },
                    label = { Text("All Sources") },
                )
                sources.forEach { src ->
                    FilterChip(
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
    val formattedTime = remember(item.timestamp) {
        if (item.timestamp > 0) {
            SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(item.timestamp))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
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

                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
    LibrariesContainer(
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
            imageVector = Icons.Filled.KeyboardArrowRight,
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
            imageVector = Icons.Filled.KeyboardArrowRight,
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
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
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
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsScreenPreview() {
    ArthurTheme {
        SettingsScreen(onDismiss = {}, isPremium = true)
    }
}
