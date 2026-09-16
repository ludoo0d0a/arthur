package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.source.StockPhotoCategory

/** Static cover drawables for pack / sub-pack tiles. */
object PackCovers {
    @DrawableRes
    fun museum(topic: MuseumTopic): Int = when (topic) {
        MuseumTopic.Random -> R.drawable.pack_museum
        MuseumTopic.Met -> R.drawable.pack_met
        MuseumTopic.Rijksmuseum -> R.drawable.pack_rijksmuseum
        MuseumTopic.Artic -> R.drawable.pack_artic
        MuseumTopic.Cleveland -> R.drawable.pack_cleveland
        MuseumTopic.Europeana -> R.drawable.pack_europeana
        MuseumTopic.Harvard -> R.drawable.pack_harvard
        MuseumTopic.Smithsonian -> R.drawable.pack_smithsonian
        MuseumTopic.Louvre -> R.drawable.pack_louvre
        MuseumTopic.WikimediaStreetArt -> R.drawable.pack_painting
    }

    @DrawableRes
    fun genart(topic: GenartTopic): Int = when (topic) {
        GenartTopic.Random -> R.drawable.pack_genart
        GenartTopic.Tapet -> R.drawable.pack_genart_abstract
        GenartTopic.Nature -> R.drawable.pack_genart_nature
        GenartTopic.Weather -> R.drawable.pack_genart_weather
        GenartTopic.Water -> R.drawable.pack_genart_nature
        GenartTopic.Life -> R.drawable.pack_genart_nature
        GenartTopic.Earth -> R.drawable.pack_genart_nature
        GenartTopic.Planets -> R.drawable.pack_genart_planets
        GenartTopic.SciFi -> R.drawable.pack_genart_geometry
        GenartTopic.Abstract -> R.drawable.pack_genart_abstract
        GenartTopic.Geometry -> R.drawable.pack_genart_geometry
        GenartTopic.Fractal -> R.drawable.pack_genart_fractal
        GenartTopic.Custom -> R.drawable.pack_genart
    }

    @DrawableRes
    fun photo(topic: StockPhotoCategory): Int = when (topic) {
        StockPhotoCategory.Nature -> R.drawable.pack_photo_nature
        StockPhotoCategory.Ocean -> R.drawable.pack_photo_ocean
        StockPhotoCategory.City -> R.drawable.pack_photo_city
        StockPhotoCategory.Mountains -> R.drawable.pack_photo_mountains
        StockPhotoCategory.Sky -> R.drawable.pack_photo_sky
        StockPhotoCategory.Abstract -> R.drawable.pack_photo_abstract
        StockPhotoCategory.Architecture -> R.drawable.pack_photo_architecture
        StockPhotoCategory.StreetArt -> R.drawable.pack_photo_streetart
        StockPhotoCategory.Random -> R.drawable.pack_photo
    }

    /** Themed official logo marks for photo providers. */
    @DrawableRes
    fun photoTopic(topic: PhotoTopic): Int = when (topic) {
        PhotoTopic.Pexels -> R.drawable.pack_pexels
        PhotoTopic.Unsplash -> R.drawable.pack_unsplash
        PhotoTopic.DeviantArt -> R.drawable.pack_photo
    }

    /** Themed official logo marks (gold-on-dark, same treatment as museum packs). */
    @DrawableRes
    fun video(topic: VideoTopic): Int = when (topic) {
        VideoTopic.Pexels -> R.drawable.pack_pexels
        VideoTopic.Unsplash -> R.drawable.pack_unsplash
        VideoTopic.Pixabay -> R.drawable.pack_pixabay
        VideoTopic.Coverr -> R.drawable.pack_coverr
    }
}
