package fr.geoking.arthur.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import fr.geoking.arthur.shared.domain.ArtUniverse

/** Lightweight analytics for universe picker / switcher. */
object UniverseAnalytics {
    private const val TAG = "UniverseAnalytics"

    fun selected(analytics: FirebaseAnalytics?, universe: ArtUniverse) {
        log(analytics, "universe_selected", universe)
    }

    fun switched(analytics: FirebaseAnalytics?, universe: ArtUniverse) {
        log(analytics, "universe_switched", universe)
    }

    private fun log(analytics: FirebaseAnalytics?, event: String, universe: ArtUniverse) {
        Log.d(TAG, "$event universe=${universe.storageKey}")
        val bundle = Bundle().apply {
            putString("universe", universe.storageKey)
        }
        runCatching {
            analytics?.logEvent(event, bundle)
        }
    }
}
