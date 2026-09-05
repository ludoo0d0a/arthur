package fr.geoking.arthur.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.R
import fr.geoking.arthur.UsedApisList
import fr.geoking.arthur.phone.theme.ArthurTheme

private const val WebsiteUrl = "https://arthur.geoking.fr"
private const val PrivacyUrl = "https://arthur.geoking.fr/privacy.html"
private const val TermsUrl = "https://arthur.geoking.fr/terms.html"

enum class SettingsScreenPage {
    Main,
    About,
    Licenses,
    Developer,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onDismiss: () -> Unit,
    isPremium: Boolean = false,
    showDeveloper: Boolean = BuildConfig.DEBUG,
    simulatePremium: Boolean = true,
    onSimulatePremiumChange: (Boolean) -> Unit = {},
    initialScreenStack: List<SettingsScreenPage>? = null,
    onInitialRouteConsumed: () -> Unit = {},
) {
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
                            SettingsScreenPage.About -> stringResource(R.string.screen_about)
                            SettingsScreenPage.Licenses -> stringResource(R.string.screen_licenses)
                            SettingsScreenPage.Developer -> stringResource(R.string.screen_developer)
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
                    onNavigate = { screenStack = screenStack + it },
                )
                SettingsScreenPage.About -> AboutContent(
                    onOpenLicenses = { screenStack = screenStack + SettingsScreenPage.Licenses },
                )
                SettingsScreenPage.Licenses -> LicensesContent()
                SettingsScreenPage.Developer -> DeveloperContent(
                    simulatePremium = simulatePremium,
                    onSimulatePremiumChange = onSimulatePremiumChange,
                )
            }
        }
    }
}

@Composable
private fun MainMenu(
    isPremium: Boolean,
    showDeveloper: Boolean,
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
private fun DeveloperContent(
    simulatePremium: Boolean,
    onSimulatePremiumChange: (Boolean) -> Unit,
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
