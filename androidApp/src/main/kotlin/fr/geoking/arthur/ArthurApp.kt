package fr.geoking.arthur

import android.app.Application
import fr.geoking.arthur.shared.domain.FakePremiumEntitlement
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
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
    single<PremiumEntitlement> { FakePremiumEntitlement(isPremium = false) }
    single { BundledPackSource() }
    single {
        ContentEngine(
            sources = listOf(get<BundledPackSource>()),
            entitlement = get(),
        )
    }
}
