package fr.geoking.arthur.shared.source

/**
 * Stock-photo search topics for Pexels / Unsplash (ASCII query tokens).
 * [Suggestions] is curated offline photos — not a remote search query.
 */
enum class StockPhotoCategory(val query: String) {
    Suggestions("suggestions"),
    Nature("nature"),
    City("city"),
    Ocean("ocean"),
    Mountains("mountains"),
    Abstract("abstract"),
    Architecture("architecture"),
    Sky("sky");

    /** False for curated topics that must not hit Pexels / Unsplash. */
    val isRemoteSearch: Boolean
        get() = this != Suggestions

    companion object {
        fun fromQuery(query: String): StockPhotoCategory =
            entries.firstOrNull { it.query.equals(query, ignoreCase = true) } ?: Suggestions
    }
}
