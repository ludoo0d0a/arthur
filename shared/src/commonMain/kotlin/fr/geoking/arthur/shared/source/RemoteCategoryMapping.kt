package fr.geoking.arthur.shared.source

/**
 * Remote catalog providers that accept a search facet.
 * UX chips stay stable; each provider gets its own tokens via [RemoteCategoryMapping].
 */
enum class RemoteProvider {
    Pexels,
    Unsplash,
    Met,
    Artic,
    Cleveland,
    Rijksmuseum,
    Europeana,
    Harvard,
    Smithsonian,
    Louvre,
}

/**
 * Provider-specific museum search tokens derived from the unified [MuseumSearchKind] UX facet.
 *
 * | Provider     | Fields used                                      |
 * |--------------|--------------------------------------------------|
 * | Met          | [query] (`q`) + [medium] (`Paintings`, …)        |
 * | Artic        | [query] (`q`)                                    |
 * | Cleveland    | [type] (`Painting`, `Sculpture`, …)              |
 * | Rijksmuseum  | [type] (`painting`, `sculpture`, `photograph`)   |
 * | Europeana    | [query] (`painting`, `sculpture`)                |
 * | Harvard      | [type] classification (`Paintings`, `Sculpture`) |
 * | Smithsonian  | [query] Solr clause with images                  |
 * | Louvre       | curated ARK lists (no live search tokens)        |
 */
data class MuseumApiParams(
    val query: String? = null,
    val type: String? = null,
    val medium: String? = null,
)

/**
 * Maps Arthur’s unified Control Plane categories to each Remote Source’s API vocabulary.
 *
 * UX ids (`StockPhotoCategory.query`, [MuseumSearchKind]) never change for persistence / chips.
 * Only the mapped tokens sent on the wire differ per provider.
 */
object RemoteCategoryMapping {

    /**
     * Stock-photo topic → provider search `query`.
     * Returns null for curated [StockPhotoCategory.Suggestions] (no remote call).
     * [StockPhotoCategory.Random] resolves to a random concrete remote topic each call.
     */
    fun stockQuery(category: StockPhotoCategory, provider: RemoteProvider): String? {
        if (!category.isRemoteSearch) return null
        val resolved =
            if (category == StockPhotoCategory.Random) {
                StockPhotoCategory.remoteSearchTopics.random()
            } else {
                category
            }
        return when (provider) {
            RemoteProvider.Pexels -> pexelsStockQuery(resolved)
            RemoteProvider.Unsplash -> unsplashStockQuery(resolved)
            else -> resolved.query
        }
    }

    /** Museum Painting / Sculpture / All → provider search params. */
    fun museumParams(kind: MuseumSearchKind, provider: RemoteProvider): MuseumApiParams =
        when (provider) {
            RemoteProvider.Met -> metParams(kind)
            RemoteProvider.Artic -> articParams(kind)
            RemoteProvider.Cleveland -> clevelandParams(kind)
            RemoteProvider.Rijksmuseum -> rijksParams(kind)
            RemoteProvider.Europeana -> europeanaParams(kind)
            RemoteProvider.Harvard -> harvardParams(kind)
            RemoteProvider.Smithsonian -> smithsonianParams(kind)
            RemoteProvider.Louvre -> MuseumApiParams()
            else -> MuseumApiParams(query = kind.name.lowercase())
        }

    /** Expand [MuseumSearchKind.All] into concrete kinds to request. */
    fun museumTargets(kind: MuseumSearchKind): List<MuseumSearchKind> = when (kind) {
        MuseumSearchKind.All -> listOf(MuseumSearchKind.Painting, MuseumSearchKind.Sculpture)
        else -> listOf(kind)
    }

    // --- Stock: Pexels free-text (docs examples use plain English topics) ---

    private fun pexelsStockQuery(category: StockPhotoCategory): String = when (category) {
        StockPhotoCategory.Suggestions,
        StockPhotoCategory.Random,
        -> category.query
        StockPhotoCategory.Nature -> "nature"
        StockPhotoCategory.City -> "city"
        StockPhotoCategory.Ocean -> "ocean"
        StockPhotoCategory.Mountains -> "mountains"
        StockPhotoCategory.Abstract -> "abstract"
        StockPhotoCategory.Architecture -> "architecture"
        StockPhotoCategory.Sky -> "sky"
    }

    // --- Stock: Unsplash free-text (slightly more descriptive for relevance) ---

    private fun unsplashStockQuery(category: StockPhotoCategory): String = when (category) {
        StockPhotoCategory.Suggestions,
        StockPhotoCategory.Random,
        -> category.query
        StockPhotoCategory.Nature -> "nature landscape"
        StockPhotoCategory.City -> "city urban"
        StockPhotoCategory.Ocean -> "ocean sea"
        StockPhotoCategory.Mountains -> "mountains peak"
        StockPhotoCategory.Abstract -> "abstract texture"
        StockPhotoCategory.Architecture -> "architecture building"
        StockPhotoCategory.Sky -> "sky clouds"
    }

    // --- Museum: Met Collection API (`q` + `medium`) ---

    private fun metParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(
            query = "painting",
            medium = "Paintings",
        )
        MuseumSearchKind.Sculpture -> MuseumApiParams(
            query = "sculpture",
            medium = "Sculpture",
        )
    }

    // --- Museum: Art Institute (`q` full-text) ---

    private fun articParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(query = "painting")
        MuseumSearchKind.Sculpture -> MuseumApiParams(query = "sculpture")
    }

    // --- Museum: Cleveland Open Access (`type` title-case codes) ---

    private fun clevelandParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(type = "Painting")
        MuseumSearchKind.Sculpture -> MuseumApiParams(type = "Sculpture")
    }

    // --- Museum: Rijksmuseum Linked Art (`type` lowercase codes) ---

    private fun rijksParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(type = "painting")
        MuseumSearchKind.Sculpture -> MuseumApiParams(type = "sculpture")
    }

    // --- Museum: Europeana Search (`query` free-text + open reusability) ---

    private fun europeanaParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(query = "painting")
        MuseumSearchKind.Sculpture -> MuseumApiParams(query = "sculpture")
    }

    // --- Museum: Harvard Art Museums (`classification` title-case) ---

    private fun harvardParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(type = "Paintings")
        MuseumSearchKind.Sculpture -> MuseumApiParams(type = "Sculpture")
    }

    // --- Museum: Smithsonian Open Access (Solr query) ---

    private fun smithsonianParams(kind: MuseumSearchKind): MuseumApiParams = when (kind) {
        MuseumSearchKind.All, MuseumSearchKind.Painting -> MuseumApiParams(
            query = "online_media_type:Images AND unit_code:SAAM AND object_type:Paintings",
        )
        MuseumSearchKind.Sculpture -> MuseumApiParams(
            query = "online_media_type:Images AND unit_code:SAAM AND sculpture",
        )
    }
}
