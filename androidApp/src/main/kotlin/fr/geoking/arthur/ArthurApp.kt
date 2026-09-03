package fr.geoking.arthur

import android.app.Application
import fr.geoking.arthur.billing.FakePurchasesGateway
import fr.geoking.arthur.billing.PurchasesGateway
import fr.geoking.arthur.billing.RevenueCatPremiumEntitlement
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class ArthurApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
    single { HttpClient(OkHttp) }
    single { BundledPackSource() }
    single { GenartSource() }
    single { FractalSource() }
    single {
        val client = get<HttpClient>()
        RijksmuseumSource(
            httpGet = { url -> client.get(url).bodyAsText() },
        )
    }
    single {
        ContentEngine(
            sources = listOf(
                get<BundledPackSource>(),
                get<RijksmuseumSource>(),
                get<GenartSource>(),
                get<FractalSource>(),
            ),
            entitlement = get(),
        )
    }
}
