package fr.geoking.arthur

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import fr.geoking.arthur.billing.DevAwarePremiumEntitlement
import fr.geoking.arthur.billing.FakePurchasesGateway
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.billing.RevenueCatPremiumEntitlement
import fr.geoking.arthur.fractal.CustomFractalStore
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.source.CacheBypassInterceptor
import fr.geoking.arthur.source.DebugInterceptor
import fr.geoking.arthur.source.ForceCacheNetworkInterceptor
import fr.geoking.arthur.source.HttpCacheController
import java.io.File
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.DeviantArtSource
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.HarvardSource
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.PexelsVideoSource
import fr.geoking.arthur.shared.source.PixabayVideoSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.SmithsonianSource
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.DeveloperSettings
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.MuseumSearchSettings
import fr.geoking.arthur.source.QuoteRepository
import fr.geoking.arthur.source.QuoteSettings
import fr.geoking.arthur.source.RemoteStillNetworkGate
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.ScreensaverSettings
import fr.geoking.arthur.source.StockPhotoSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArthurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Avoid debug noise / timeouts; release builds still report.
        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
        stopKoin()
        startKoin {
            androidContext(this@ArthurApp)
            modules(appModule)
        }
    }
}

private suspend fun safeHttpGet(
    client: HttpClient,
    url: String,
    sourceId: String,
    errorLogger: ErrorLogger,
    configure: (HttpRequestBuilder.() -> Unit)? = null,
): String {
    return try {
        val response = client.get(url) {
            header("X-Source-Id", sourceId)
            configure?.invoke(this)
        }
        val statusCode = response.status.value
        if (statusCode !in 200..299) {
            val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
            val category = ErrorClassifier.classify(statusCode, null)
            val msg = "HTTP $statusCode for $sourceId"
            errorLogger.log(
                sourceId = sourceId,
                category = category,
                message = msg,
                details = bodyText.take(300),
                url = url,
                statusCode = statusCode,
            )
            throw ResponseException(response, bodyText)
        }
        response.bodyAsText()
    } catch (e: Throwable) {
        if (e is kotlinx.coroutines.CancellationException) {
            throw e
        }
        if (e !is ResponseException) {
            val category = ErrorClassifier.classify(null, e)
            errorLogger.log(
                sourceId = sourceId,
                category = category,
                message = e.message ?: "Request failed for $sourceId",
                details = e.stackTraceToString().take(300),
                url = url,
                throwable = e,
            )
        }
        throw e
    }
}

val appModule = module {
    single { ErrorLogger(clock = { System.currentTimeMillis() }) }
    single { DebugLogger(clock = { System.currentTimeMillis() }) }
    single { FakePurchasesGateway(premium = false) }
    single<PurchasesGateway> { get<FakePurchasesGateway>() }
    single { DeveloperSettings(androidContext()) }
    single { RotationSettings(androidContext()) }
    single { ScreensaverSettings(androidContext()) }
    single { QuoteSettings(androidContext()) }
    single { InvalidArtworkStore(androidContext()) }
    single { RemoteStillNetworkGate(androidContext(), get()) }
    single {
        val client = get<HttpClient>()
        val errorLogger = get<ErrorLogger>()
        QuoteRepository(
            context = androidContext(),
            httpGet = { url ->
                safeHttpGet(client, url, QuoteRepository.SOURCE_ID, errorLogger)
            },
        )
    }
    single<PremiumEntitlement> {
        val gatewayEntitlement = RevenueCatPremiumEntitlement(get())
        val developerSettings = get<DeveloperSettings>()
        DevAwarePremiumEntitlement(
            delegate = gatewayEntitlement,
            simulatePremium = {
                (BuildConfig.DEBUG || BuildConfig.DEBUG_DEV) &&
                    developerSettings.simulatePremium.value
            },
        )
    }
    single { CustomFractalStore(androidContext()) }
    single { StockPhotoSettings(androidContext()) }
    single { MuseumSearchSettings() }
    single { ArtworkImageCache(androidContext(), get()) }
    single {
        val httpCacheDir = File(androidContext().cacheDir, "http_cache").also { it.mkdirs() }
        okhttp3.Cache(httpCacheDir, 50 * 1024 * 1024L)
    }
    single { HttpCacheController(get()) }
    single {
        val debugLogger = get<DebugLogger>()
        val cacheController = get<HttpCacheController>()
        val okHttpCache = get<okhttp3.Cache>()
        HttpClient(OkHttp) {
            engine {
                config {
                    cache(okHttpCache)
                    addInterceptor(CacheBypassInterceptor(cacheController))
                    addInterceptor(DebugInterceptor(debugLogger))
                    addNetworkInterceptor(ForceCacheNetworkInterceptor())
                }
            }
        }
    }
    single { BundledPackSource() }
    single { GenartSource() }
    single { FractalSource() }
    single {
        CustomFractalSource(
            loadArtworks = { get<CustomFractalStore>().list() },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val errorLogger = get<ErrorLogger>()
        RijksmuseumSource(
            httpGet = { url -> safeHttpGet(client, url, RijksmuseumSource.ID, errorLogger) },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val errorLogger = get<ErrorLogger>()
        MetSource(
            httpGet = { url -> safeHttpGet(client, url, MetSource.ID, errorLogger) },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val errorLogger = get<ErrorLogger>()
        ArticSource(
            httpGet = { url -> safeHttpGet(client, url, ArticSource.ID, errorLogger) },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val errorLogger = get<ErrorLogger>()
        ClevelandSource(
            httpGet = { url -> safeHttpGet(client, url, ClevelandSource.ID, errorLogger) },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val apiKey = BuildConfig.EUROPEANA_API_KEY
        val errorLogger = get<ErrorLogger>()
        EuropeanaSource(
            apiKey = apiKey,
            httpGet = { url ->
                safeHttpGet(client, url, EuropeanaSource.ID, errorLogger) {
                    if (apiKey.isNotBlank()) {
                        header("X-Api-Key", apiKey)
                    }
                }
            },
            kind = { museum.kind },
            errorLogger = errorLogger,
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val apiKey = BuildConfig.HARVARD_API_KEY
        val errorLogger = get<ErrorLogger>()
        HarvardSource(
            apiKey = apiKey,
            httpGet = { url -> safeHttpGet(client, url, HarvardSource.ID, errorLogger) },
            kind = { museum.kind },
            errorLogger = errorLogger,
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val apiKey = BuildConfig.SMITHSONIAN_API_KEY
        val errorLogger = get<ErrorLogger>()
        SmithsonianSource(
            apiKey = apiKey,
            httpGet = { url -> safeHttpGet(client, url, SmithsonianSource.ID, errorLogger) },
            kind = { museum.kind },
            errorLogger = errorLogger,
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        val errorLogger = get<ErrorLogger>()
        LouvreSource(
            httpGet = { url -> safeHttpGet(client, url, LouvreSource.ID, errorLogger) },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val errorLogger = get<ErrorLogger>()
        WikimediaStreetArtSource(
            httpGet = { url ->
                safeHttpGet(client, url, WikimediaStreetArtSource.ID, errorLogger) {
                    header(HttpHeaders.UserAgent, "Arthur/1.0 (Android; fr.geoking.arthur)")
                }
            },
        )
    }
    single {
        val client = get<HttpClient>()
        val apiKey = BuildConfig.PEXELS_API_KEY
        val settings = get<StockPhotoSettings>()
        val cache = get<ArtworkImageCache>()
        val errorLogger = get<ErrorLogger>()
        PexelsSource(
            apiKey = apiKey,
            category = { settings.category },
            offlineFallback = {
                cache.loadCachedStock(settings.category, PexelsSource.ID)
            },
            onLoaded = { arts -> cache.remember(arts, settings.category.query) },
            httpGet = { url ->
                safeHttpGet(client, url, PexelsSource.ID, errorLogger) {
                    header(HttpHeaders.Authorization, apiKey)
                }
            },
        )
    }
    single {
        val client = get<HttpClient>()
        val accessKey = BuildConfig.UNSPLASH_ACCESS_KEY
        val settings = get<StockPhotoSettings>()
        val cache = get<ArtworkImageCache>()
        val errorLogger = get<ErrorLogger>()
        UnsplashSource(
            accessKey = accessKey,
            category = { settings.category },
            kind = { settings.contentKind },
            offlineFallback = {
                cache.loadCachedStock(settings.category, UnsplashSource.ID)
            },
            onLoaded = { arts -> cache.remember(arts, settings.category.query) },
            httpGet = { url ->
                safeHttpGet(client, url, UnsplashSource.ID, errorLogger) {
                    header(HttpHeaders.Authorization, "Client-ID $accessKey")
                    header("Accept-Version", "v1")
                }
            },
        )
    }
    single {
        val client = get<HttpClient>()
        val clientId = BuildConfig.DEVIANTART_CLIENT_ID
        val clientSecret = BuildConfig.DEVIANTART_CLIENT_SECRET
        val settings = get<StockPhotoSettings>()
        val cache = get<ArtworkImageCache>()
        val errorLogger = get<ErrorLogger>()
        DeviantArtSource(
            clientId = { clientId },
            clientSecret = { clientSecret },
            username = { settings.deviantArtUsername },
            password = { settings.deviantArtPassword },
            category = { settings.category },
            offlineFallback = {
                cache.loadCachedStock(settings.category, DeviantArtSource.ID)
            },
            onLoaded = { arts -> cache.remember(arts, settings.category.query) },
            httpGet = { url -> safeHttpGet(client, url, DeviantArtSource.ID, errorLogger) },
            errorLogger = errorLogger,
        )
    }
    single {
        val client = get<HttpClient>()
        val apiKey = BuildConfig.PEXELS_API_KEY
        val settings = get<StockPhotoSettings>()
        val errorLogger = get<ErrorLogger>()
        PexelsVideoSource(
            apiKey = apiKey,
            category = { settings.category },
            httpGet = { url ->
                safeHttpGet(client, url, PexelsVideoSource.ID, errorLogger) {
                    header(HttpHeaders.Authorization, apiKey)
                }
            },
        )
    }
    single {
        val client = get<HttpClient>()
        val apiKey = BuildConfig.PIXABAY_API_KEY
        val settings = get<StockPhotoSettings>()
        val errorLogger = get<ErrorLogger>()
        PixabayVideoSource(
            apiKey = apiKey,
            category = { settings.category },
            httpGet = { url -> safeHttpGet(client, url, PixabayVideoSource.ID, errorLogger) },
        )
    }
    single {
        val client = get<HttpClient>()
        val apiKey = BuildConfig.COVERR_API_KEY
        val settings = get<StockPhotoSettings>()
        val errorLogger = get<ErrorLogger>()
        CoverrSource(
            apiKey = apiKey,
            category = { settings.category },
            httpGet = { url ->
                safeHttpGet(client, url, CoverrSource.ID, errorLogger) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                }
            },
        )
    }
    single {
        ContentEngine(
            sources = listOf(
                // Bundled + stock first so free-tier still slots are displayable photos.
                get<BundledPackSource>(),
                get<PexelsSource>(),
                get<UnsplashSource>(),
                get<DeviantArtSource>(),
                get<PexelsVideoSource>(),
                get<PixabayVideoSource>(),
                get<CoverrSource>(),
                get<RijksmuseumSource>(),
                get<MetSource>(),
                get<ArticSource>(),
                get<ClevelandSource>(),
                get<EuropeanaSource>(),
                get<HarvardSource>(),
                get<SmithsonianSource>(),
                get<LouvreSource>(),
                get<WikimediaStreetArtSource>(),
                get<GenartSource>(),
                get<FractalSource>(),
                get<CustomFractalSource>(),
            ),
            entitlement = get(),
        )
    }
}
