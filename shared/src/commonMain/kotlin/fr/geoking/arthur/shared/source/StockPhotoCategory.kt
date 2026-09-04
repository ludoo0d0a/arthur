package fr.geoking.arthur.shared.source

/**
 * Stock-photo search topics for Pexels / Unsplash (ASCII query tokens).
 */
enum class StockPhotoCategory(val query: String) {
    Nature("nature"),
    City("city"),
    Ocean("ocean"),
    Mountains("mountains"),
    Abstract("abstract"),
    Architecture("architecture"),
    Sky("sky");

    companion object {
        fun fromQuery(query: String): StockPhotoCategory =
            entries.firstOrNull { it.query.equals(query, ignoreCase = true) } ?: Nature
    }
}
