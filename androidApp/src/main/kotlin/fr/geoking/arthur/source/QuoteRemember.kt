package fr.geoking.arthur.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koin.core.context.GlobalContext

/** Koin singleton when available; no-op http for previews / tests without Koin. */
@Composable
fun rememberQuoteRepository(): QuoteRepository {
    val context = LocalContext.current
    return remember(context) {
        runCatching { GlobalContext.get().get<QuoteRepository>() }
            .getOrElse {
                QuoteRepository(
                    context = context,
                    httpGet = { "[]" },
                    provider = { QuoteProvider.ZenQuotes },
                )
            }
    }
}

@Composable
fun rememberQuoteSettings(): QuoteSettings {
    val context = LocalContext.current
    return remember(context) {
        runCatching { GlobalContext.get().get<QuoteSettings>() }
            .getOrElse { QuoteSettings(context) }
    }
}
