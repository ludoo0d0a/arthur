package fr.geoking.arthur.shared.source

/**
 * Unified Control Plane stock-photo topics (Photo subcategory chips).
 *
 * [query] is the stable UX / persistence id — not necessarily the string sent to an API.
 * Provider search tokens come from [RemoteCategoryMapping.stockQuery].
 */
enum class StockPhotoCategory(val query: String) {
    /** Easy start: each load picks a random remote topic for Pexels / Unsplash. */
    Random("random"),
    Nature("nature"),
    City("city"),
    Ocean("ocean"),
    Mountains("mountains"),
    Abstract("abstract"),
    Architecture("architecture"),
    Sky("sky"),
    StreetArt("streetart");

    companion object {
        /** Concrete remote topics [Random] may resolve to (excludes Random itself). */
        val remoteSearchTopics: List<StockPhotoCategory>
            get() = entries.filter { it != Random }

        fun fromQuery(query: String): StockPhotoCategory =
            entries.firstOrNull { it.query.equals(query, ignoreCase = true) } ?: Random
    }
}
