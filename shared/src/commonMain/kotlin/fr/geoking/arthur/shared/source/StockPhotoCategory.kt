package fr.geoking.arthur.shared.source

/**
 * Unified Control Plane stock-photo topics (Photo subcategory chips).
 *
 * [query] is the stable UX / persistence id — not necessarily the string sent to an API.
 * Provider search tokens come from [RemoteCategoryMapping.stockQuery].
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
