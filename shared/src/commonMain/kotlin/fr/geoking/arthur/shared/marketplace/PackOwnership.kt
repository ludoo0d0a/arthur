package fr.geoking.arthur.shared.marketplace

/**
 * Whether the user owns Marketplace pack SKUs. Adapters (RevenueCat) live on Android.
 * Premium does **not** imply pack ownership.
 */
interface PackOwnership {
    fun owns(packId: String): Boolean

    fun ownsPersonalPhotos(): Boolean = owns(MarketplaceCatalog.PERSONAL_PHOTOS_ID)

    fun ownsGenartTopic(topicSuffix: String): Boolean =
        MarketplaceCatalog.sellablePackIdForGenartTopic(topicSuffix)?.let { owns(it) } == true

    /** Genart engine unlocked by free allowlist or any owned covering topic pack. */
    fun allowsGenartEngine(engineId: String): Boolean {
        if (engineId in MarketplaceCatalog.freeGenartEngineIds) return true
        val covering = GenartPackTopics.topicsCoveringEngine(engineId)
        if (covering.isEmpty()) return false
        return covering.any { ownsGenartTopic(it) }
    }

    fun ownsAllFractalPresets(): Boolean = ownsGenartTopic(GenartPackTopics.FRACTAL)

    fun ownsCustomFractal(): Boolean = ownsGenartTopic(GenartPackTopics.CUSTOM)

    companion object {
        val NONE: PackOwnership = object : PackOwnership {
            override fun owns(packId: String): Boolean = false
        }
    }
}

class FakePackOwnership(
    private var owned: Set<String> = emptySet(),
) : PackOwnership {
    fun setOwned(packIds: Set<String>) {
        owned = packIds
    }

    fun unlock(packId: String) {
        owned = owned + packId
    }

    fun unlockAll() {
        owned = MarketplaceCatalog.all().map { it.id }.toSet()
    }

    override fun owns(packId: String): Boolean = packId in owned
}
