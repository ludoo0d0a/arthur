package fr.geoking.arthur

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import fr.geoking.arthur.billing.FakePurchasesGateway
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.billing.RevenueCatPremiumEntitlement
import fr.geoking.arthur.fractal.CustomFractalStore
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.MuseumSearchSettings
import fr.geoking.arthur.source.StockPhotoSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        stopKoin()
        startKoin {
            androidContext(this@ArthurApp)
            modules(appModule)
        }
    }
}

val appModule = module {
    single { FakePurchasesGateway(premium = false) }
    single<PurchasesGateway> { get<FakePurchasesGateway>() }
    single<PremiumEntitlement> { RevenueCatPremiumEntitlement(get()) }
    single { CustomFractalStore(androidContext()) }
    single { StockPhotoSettings(androidContext()) }
    single { MuseumSearchSettings() }
    single { ArtworkImageCache(androidContext()) }
    single { HttpClient(OkHttp) }
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
        RijksmuseumSource(
            httpGet = { url -> client.get(url).bodyAsText() },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        MetSource(
            httpGet = { url -> client.get(url).bodyAsText() },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        ArticSource(
            httpGet = { url -> client.get(url).bodyAsText() },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val museum = get<MuseumSearchSettings>()
        ClevelandSource(
            httpGet = { url -> client.get(url).bodyAsText() },
            kind = { museum.kind },
        )
    }
    single {
        val client = get<HttpClient>()
        val apiKey = BuildConfig.PEXELS_API_KEY
        val settings = get<StockPhotoSettings>()
        val cache = get<ArtworkImageCache>()
        PexelsSource(
            apiKey = apiKey,
            category = { settings.category },
            offlineFallback = { cache.loadCached(settings.category.query, PexelsSource.ID) },
            onLoaded = { arts -> cache.remember(arts, settings.category.query) },
            httpGet = { url ->
                client.get(url) {
                    header(HttpHeaders.Authorization, apiKey)
                }.bodyAsText()
            },
        )
    }
    single {
        val client = get<HttpClient>()
        val accessKey = BuildConfig.UNSPLASH_ACCESS_KEY
        val settings = get<StockPhotoSettings>()
        val cache = get<ArtworkImageCache>()
        UnsplashSource(
            accessKey = accessKey,
            category = { settings.category },
            offlineFallback = { cache.loadCached(settings.category.query, UnsplashSource.ID) },
            onLoaded = { arts -> cache.remember(arts, settings.category.query) },
            httpGet = { url ->
                client.get(url) {
                    header(HttpHeaders.Authorization, "Client-ID $accessKey")
                    header("Accept-Version", "v1")
                }.bodyAsText()
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
                get<RijksmuseumSource>(),
                get<MetSource>(),
                get<ArticSource>(),
                get<ClevelandSource>(),
                get<GenartSource>(),
                get<FractalSource>(),
                get<CustomFractalSource>(),
            ),
            entitlement = get(),
        )
    }
}
