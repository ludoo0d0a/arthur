package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.source.StockPhotoCategory

/** Static cover drawables for pack / sub-pack tiles. */
object PackCovers {
    /** Shared Random tile cover for every family (Museum / Photo / Video / Genart / …). */
    @get:DrawableRes
    val random: Int = R.drawable.pack_random

    @DrawableRes
    fun museum(topic: MuseumTopic): Int = when (topic) {
        MuseumTopic.Random -> random
        MuseumTopic.Met -> R.drawable.pack_met
        MuseumTopic.Rijksmuseum -> R.drawable.pack_rijksmuseum
        MuseumTopic.Artic -> R.drawable.pack_artic
        MuseumTopic.Cleveland -> R.drawable.pack_cleveland
        MuseumTopic.Europeana -> R.drawable.pack_europeana
        MuseumTopic.Harvard -> R.drawable.pack_harvard
        MuseumTopic.Smithsonian -> R.drawable.pack_smithsonian
        MuseumTopic.Louvre -> R.drawable.pack_louvre
    }

    @DrawableRes
    fun genart(topic: GenartTopic): Int = when (topic) {
        GenartTopic.All -> R.drawable.pack_genart
        GenartTopic.Random -> random
        GenartTopic.Tapet -> R.drawable.pack_genart_tapet
        GenartTopic.Nature -> R.drawable.pack_genart_nature
        GenartTopic.Weather -> R.drawable.pack_genart_weather
        GenartTopic.Water -> R.drawable.pack_genart_water
        GenartTopic.Life -> R.drawable.pack_genart_life
        GenartTopic.Earth -> R.drawable.pack_genart_earth
        GenartTopic.Planets -> R.drawable.pack_genart_planets
        GenartTopic.SciFi -> R.drawable.pack_genart_scifi
        GenartTopic.Vintage -> R.drawable.pack_genart_vintage
        GenartTopic.Abstract -> R.drawable.pack_genart_abstract
        GenartTopic.Geometry -> R.drawable.pack_genart_geometry
        GenartTopic.Fractal -> R.drawable.pack_genart_fractal
        GenartTopic.Custom -> R.drawable.pack_genart_custom
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
        StockPhotoCategory.Random -> random
    }

    /** Themed official logo marks for photo providers. */
    @DrawableRes
    fun photoTopic(topic: PhotoTopic): Int = when (topic) {
        PhotoTopic.Pexels -> R.drawable.pack_pexels
        PhotoTopic.Unsplash -> R.drawable.pack_unsplash
        PhotoTopic.DeviantArt -> R.drawable.pack_deviantart
        PhotoTopic.Wikimedia -> R.drawable.pack_photo_streetart
    }

    /** Themed official logo marks (gold-on-dark, same treatment as museum packs). */
    @DrawableRes
    fun video(topic: VideoTopic): Int = when (topic) {
        VideoTopic.Pexels -> R.drawable.pack_pexels
        VideoTopic.Unsplash -> R.drawable.pack_unsplash
        VideoTopic.Pixabay -> R.drawable.pack_pixabay
        VideoTopic.Coverr -> R.drawable.pack_coverr
    }

    @DrawableRes
    fun audio(topic: AudioPackTopic): Int = when (topic) {
        AudioPackTopic.Essentials -> R.drawable.pack_audio_essentials
        AudioPackTopic.HearthWeather -> R.drawable.pack_audio_hearth_weather
        AudioPackTopic.DawnChorus -> R.drawable.pack_audio_dawn_chorus
        AudioPackTopic.TempleResonance -> R.drawable.pack_audio_temple_resonance
        AudioPackTopic.WindGarden -> R.drawable.pack_audio_wind_garden
        AudioPackTopic.CosmicDrift -> R.drawable.pack_audio_cosmic_drift
        AudioPackTopic.JazzAfterDark -> R.drawable.pack_audio_jazz_after_dark
        AudioPackTopic.WorldPulse -> R.drawable.pack_audio_world_pulse
        AudioPackTopic.SalonClassique -> R.drawable.pack_audio_salon_classique
        AudioPackTopic.GrandOrchestra -> R.drawable.pack_audio_grand_orchestra
        AudioPackTopic.SoloViolin -> R.drawable.pack_audio_solo_violin
        AudioPackTopic.RockBallad -> R.drawable.pack_audio_rock_ballad
        AudioPackTopic.BassOnly -> R.drawable.pack_audio_bass_only
        AudioPackTopic.MidnightBallad -> R.drawable.pack_audio_midnight_ballad
        AudioPackTopic.HawaiianBreeze -> R.drawable.pack_audio_hawaiian_breeze
        AudioPackTopic.ArcadeChips -> R.drawable.pack_audio_arcade_chips
    }
}
