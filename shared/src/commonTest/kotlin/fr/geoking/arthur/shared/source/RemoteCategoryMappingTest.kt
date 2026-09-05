package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteCategoryMappingTest {

    @Test
    fun suggestionsMapsToNullForStockProviders() {
        assertNull(
            RemoteCategoryMapping.stockQuery(StockPhotoCategory.Suggestions, RemoteProvider.Pexels),
        )
        assertNull(
            RemoteCategoryMapping.stockQuery(StockPhotoCategory.Suggestions, RemoteProvider.Unsplash),
        )
    }

    @Test
    fun stockNatureUsesProviderSpecificQueries() {
        assertEquals(
            "nature",
            RemoteCategoryMapping.stockQuery(StockPhotoCategory.Nature, RemoteProvider.Pexels),
        )
        assertEquals(
            "nature landscape",
            RemoteCategoryMapping.stockQuery(StockPhotoCategory.Nature, RemoteProvider.Unsplash),
        )
    }

    @Test
    fun stockArchitectureUsesProviderSpecificQueries() {
        assertEquals(
            "architecture",
            RemoteCategoryMapping.stockQuery(StockPhotoCategory.Architecture, RemoteProvider.Pexels),
        )
        assertEquals(
            "architecture building",
            RemoteCategoryMapping.stockQuery(StockPhotoCategory.Architecture, RemoteProvider.Unsplash),
        )
    }

    @Test
    fun museumPaintingMapsToDistinctProviderCodes() {
        assertEquals(
            MuseumApiParams(query = "painting", medium = "Paintings"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Painting, RemoteProvider.Met),
        )
        assertEquals(
            MuseumApiParams(query = "painting"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Painting, RemoteProvider.Artic),
        )
        assertEquals(
            MuseumApiParams(type = "Painting"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Painting, RemoteProvider.Cleveland),
        )
        assertEquals(
            MuseumApiParams(type = "painting"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Painting, RemoteProvider.Rijksmuseum),
        )
    }

    @Test
    fun museumSculptureMapsToDistinctProviderCodes() {
        assertEquals(
            MuseumApiParams(query = "sculpture", medium = "Sculpture"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Sculpture, RemoteProvider.Met),
        )
        assertEquals(
            MuseumApiParams(query = "sculpture"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Sculpture, RemoteProvider.Artic),
        )
        assertEquals(
            MuseumApiParams(type = "Sculpture"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Sculpture, RemoteProvider.Cleveland),
        )
        assertEquals(
            MuseumApiParams(type = "sculpture"),
            RemoteCategoryMapping.museumParams(MuseumSearchKind.Sculpture, RemoteProvider.Rijksmuseum),
        )
    }

    @Test
    fun museumAllExpandsToPaintingAndSculptureTargets() {
        assertEquals(
            listOf(MuseumSearchKind.Painting, MuseumSearchKind.Sculpture),
            RemoteCategoryMapping.museumTargets(MuseumSearchKind.All),
        )
        assertEquals(
            listOf(MuseumSearchKind.Sculpture),
            RemoteCategoryMapping.museumTargets(MuseumSearchKind.Sculpture),
        )
    }

    @Test
    fun sourceSearchUrlsUseMappedTokens() {
        assertEquals(
            "https://collectionapi.metmuseum.org/public/collection/v1/search" +
                "?q=sculpture&medium=Sculpture&hasImages=true&isPublicDomain=true",
            MetSource.searchUrl(MuseumSearchKind.Sculpture),
        )
        assertEquals(
            "https://openaccess-api.clevelandart.org/api/artworks/" +
                "?cc0=1&has_image=1&limit=20&type=Sculpture",
            ClevelandSource.searchUrl(kind = MuseumSearchKind.Sculpture),
        )
        assertEquals(
            "https://data.rijksmuseum.nl/search/collection?type=sculpture&imageAvailable=true",
            RijksmuseumSource.searchUrl(MuseumSearchKind.Sculpture),
        )
        assertEquals(
            "https://api.unsplash.com/search/photos" +
                "?query=nature%20landscape&orientation=landscape&per_page=20",
            UnsplashSource.searchUrl(),
        )
    }
}
