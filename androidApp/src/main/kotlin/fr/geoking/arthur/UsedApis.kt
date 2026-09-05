package fr.geoking.arthur

/**
 * Remote Sources and open APIs used by Arthur. Shown in Settings → About.
 */
data class UsedApi(
    val name: String,
    val url: String,
)

val UsedApisList: List<UsedApi> = listOf(
    UsedApi("Rijksmuseum (Linked Art)", "https://data.rijksmuseum.nl"),
    UsedApi("The Met Collection API", "https://metmuseum.github.io/"),
    UsedApi("Art Institute of Chicago", "https://api.artic.edu/docs/"),
    UsedApi("Cleveland Museum of Art", "https://openaccess-api.clevelandart.org/"),
    UsedApi("Europeana", "https://www.europeana.eu/en/apis"),
    UsedApi("Harvard Art Museums", "https://github.com/harvardartmuseums/api-docs"),
    UsedApi("Smithsonian Open Access", "https://api.si.edu/openaccess"),
    UsedApi("Musée du Louvre Collections", "https://collections.louvre.fr/en/page/documentationJSON"),
    UsedApi("Pexels", "https://www.pexels.com/api/"),
    UsedApi("Unsplash", "https://unsplash.com/developers"),
)
