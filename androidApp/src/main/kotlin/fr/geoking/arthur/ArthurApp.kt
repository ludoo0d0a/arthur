package fr.geoking.arthur

import android.app.Application
import fr.geoking.arthur.billing.FakePurchasesGateway
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.billing.RevenueCatPremiumEntitlement
import fr.geoking.arthur.fractal.CustomFractalStore
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.UnsplashSource
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
        RijksmuseumSource(
            httpGet = { url -> client.get(url).bodyAsText() },
        )
    }
    single {
        val client = get<HttpClient>()
        MetSource(
            httpGet = { url -> client.get(url).bodyAsText() },
        )
    }
    single {
        val client = get<HttpClient>()
        val apiKey = BuildConfig.PEXELS_API_KEY
        PexelsSource(
            apiKey = apiKey,
            httpGet = { url ->
                client.get(url) {
                    header(HttpHeaders.Authorization, apiKey)
                }.bodyAsText()
            },
        )
    }
    single {
        val client = get<HttpClient>()
        // Access Key = Client-ID for public search. Secret Key is OAuth-only (not in BuildConfig).
        val accessKey = BuildConfig.UNSPLASH_ACCESS_KEY
        UnsplashSource(
            accessKey = accessKey,
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
                get<BundledPackSource>(),
                get<RijksmuseumSource>(),
                get<MetSource>(),
                get<PexelsSource>(),
                get<UnsplashSource>(),
                get<GenartSource>(),
                get<FractalSource>(),
                get<CustomFractalSource>(),
            ),
            entitlement = get(),
        )
    }
}
