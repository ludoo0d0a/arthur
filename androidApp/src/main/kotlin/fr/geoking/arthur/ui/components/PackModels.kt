package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.MusicStyleIds
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.AudioPackCatalog
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.source.SourceCapabilities
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.source.AmbientAudioSettings

/** Top-level Spotify-style packs on the Control Plane home grid. */
enum class PackFamily(
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val coverRes: Int,
    val testTagSuffix: String,
) {
    Museum(R.string.pack_museum, R.drawable.pack_museum, "museum"),
    Genart(R.string.kind_genart, R.drawable.pack_genart, "genart"),
    VintageGames(R.string.pack_vintage_games, R.drawable.pack_vintage_games, "vintage_games"),
    Photo(R.string.kind_photo, R.drawable.pack_photo, "photo"),
    Personal(R.string.pack_personal, R.drawable.pack_photo, "personal"),
    Sound(R.string.pack_sound, R.drawable.pack_sound, "sound"),
    Video(R.string.kind_video, R.drawable.pack_video, "video"),
    Sculpture(R.string.kind_sculpture, R.drawable.pack_sculpture, "sculpture"),
    Painting(R.string.kind_painting, R.drawable.pack_painting, "painting"),
}

/** Sub-pack ids under [PackFamily.VintageGames]. */
object VintageGamesSub {
    const val AMBIENT = "ambient"
    const val VISUALS = "visuals"
    const val MUSIC = "music"
}

/**
 * Selected pack for Ambient: a [PackFamily] plus optional subcategory.
 * [subId] null means the family's home tile was started directly, with no sub-pack
 * chosen — Museum and most families behave like **Random** (Genart → All).
 */
data class PackSelection(
    val family: PackFamily,
    val subId: String? = null,
) {
    val isAll: Boolean get() = subId == null

    /** Stable identity for in-memory Ambient playlist resume. */
    fun sessionKey(): String = "${family.name}|${subId.orEmpty()}"
}

data class PackTile(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val coverRes: Int,
    val selection: PackSelection,
    val testTagSuffix: String,
    val itemCount: Int? = null,
    /** Marketplace SKU when this tile requires a purchase; null = free. */
    val sellablePackId: String? = null,
) {
    fun isLocked(ownership: PackOwnership): Boolean {
        // Home tile: locked only when neither visuals nor chiptune pack is owned.
        if (selection.family == PackFamily.VintageGames && selection.subId == null) {
            val ownsVisuals = ownership.ownsGenartTopic(
                fr.geoking.arthur.shared.marketplace.GenartPackTopics.VINTAGE,
            )
            val ownsAudio = ownership.ownsAudioPack(
                fr.geoking.arthur.shared.marketplace.AudioPackCatalog.ARCADE_CHIPS,
            )
            return !ownsVisuals && !ownsAudio
        }
        return sellablePackId != null && !ownership.owns(sellablePackId)
    }
}

/** Genart sub-pack order for the grid ([GenartTopic.All] leads). */
private val GenartSubTopics = listOf(
    GenartTopic.All,
    GenartTopic.Random,
    GenartTopic.Tapet,
    GenartTopic.Nature,
    GenartTopic.Weather,
    GenartTopic.Water,
    GenartTopic.Life,
    GenartTopic.Earth,
    GenartTopic.Planets,
    GenartTopic.SciFi,
    GenartTopic.Vintage,
    GenartTopic.Abstract,
    GenartTopic.Geometry,
    GenartTopic.Fractal,
    GenartTopic.Custom,
)

/** Default sub-pack id when opening a family on the Control Plane. */
fun PackFamily.defaultSubId(): String = when (this) {
    PackFamily.Genart -> GenartTopic.All.testTagSuffix
    PackFamily.VintageGames -> VintageGamesSub.AMBIENT
    PackFamily.Museum -> MuseumTopic.Random.testTagSuffix
    PackFamily.Personal -> "all"
    PackFamily.Sound -> AudioPackTopic.Essentials.testTagSuffix
    PackFamily.Photo,
    PackFamily.Video,
    PackFamily.Sculpture,
    PackFamily.Painting,
    -> "random"
}

fun PackFamily.homeTile(catalog: List<Artwork> = emptyList()): PackTile = PackTile(
    id = "family_${testTagSuffix}",
    titleRes = titleRes,
    coverRes = coverRes,
    selection = PackSelection(this),
    testTagSuffix = testTagSuffix,
    itemCount = when {
        this == PackFamily.Genart && catalog.isNotEmpty() ->
            resolvePackPool(catalog, PackSelection(PackFamily.Genart)).size
        this == PackFamily.VintageGames && catalog.isNotEmpty() ->
            resolvePackPool(
                catalog,
                PackSelection(PackFamily.VintageGames, VintageGamesSub.AMBIENT),
            ).size
        else -> null
    },
    sellablePackId = when (this) {
        PackFamily.Personal -> MarketplaceCatalog.PERSONAL_PHOTOS_ID
        PackFamily.VintageGames -> MarketplaceCatalog.genartPackId(fr.geoking.arthur.shared.marketplace.GenartPackTopics.VINTAGE)
        else -> null
    },
)

fun PackFamily.subPackTiles(catalog: List<Artwork> = emptyList()): List<PackTile> = when (this) {
    PackFamily.Museum -> {
        val random = PackTile(
            id = "sub_museum_${MuseumTopic.Random.testTagSuffix}",
            titleRes = MuseumTopic.Random.labelRes,
            coverRes = PackCovers.museum(MuseumTopic.Random),
            selection = PackSelection(PackFamily.Museum, MuseumTopic.Random.testTagSuffix),
            testTagSuffix = "museum_${MuseumTopic.Random.testTagSuffix}",
        )
        val institutions = MuseumTopic.institutions.map { topic ->
            PackTile(
                id = "sub_museum_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Museum, topic.testTagSuffix),
                testTagSuffix = "museum_${topic.testTagSuffix}",
            )
        }
        listOf(random) + institutions
    }
    PackFamily.Genart -> GenartSubTopics.map { topic ->
        val selection = PackSelection(PackFamily.Genart, topic.testTagSuffix)
        PackTile(
            id = "sub_genart_${topic.testTagSuffix}",
            titleRes = topic.labelRes,
            coverRes = PackCovers.genart(topic),
            selection = selection,
            testTagSuffix = "genart_${topic.testTagSuffix}",
            itemCount = if (catalog.isNotEmpty()) resolvePackPool(catalog, selection).size else null,
            sellablePackId = MarketplaceCatalog.sellablePackIdForGenartTopic(topic.testTagSuffix),
        )
    }
    PackFamily.Personal -> emptyList()
    PackFamily.Photo -> {
        val providerSources = PhotoTopic.entries.map { topic ->
            PackTile(
                id = "sub_photo_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.photoTopic(topic),
                selection = PackSelection(PackFamily.Photo, topic.testTagSuffix),
                testTagSuffix = "photo_${topic.testTagSuffix}",
            )
        }
        val topics = StockPhotoCategory.entries.map { topic ->
            PackTile(
                id = "sub_photo_${topic.query}",
                titleRes = topic.packLabelRes(),
                coverRes = PackCovers.photo(topic),
                selection = PackSelection(PackFamily.Photo, topic.query),
                testTagSuffix = "photo_${topic.query}",
            )
        }
        providerSources + topics
    }
    PackFamily.Video -> {
        val sources = VideoTopic.entries.map { topic ->
            PackTile(
                id = "sub_video_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.video(topic),
                selection = PackSelection(PackFamily.Video, topic.testTagSuffix),
                testTagSuffix = "video_${topic.testTagSuffix}",
            )
        }
        val keywords = videoStockTopics().map { topic ->
            PackTile(
                id = "sub_video_${topic.query}",
                titleRes = topic.packLabelRes(),
                coverRes = PackCovers.photo(topic),
                selection = PackSelection(PackFamily.Video, topic.query),
                testTagSuffix = "video_${topic.query}",
            )
        }
        sources + keywords
    }
    PackFamily.Sculpture -> {
        val random = PackTile(
            id = "sub_sculpture_${MuseumTopic.Random.testTagSuffix}",
            titleRes = MuseumTopic.Random.labelRes,
            coverRes = PackCovers.museum(MuseumTopic.Random),
            selection = PackSelection(PackFamily.Sculpture, MuseumTopic.Random.testTagSuffix),
            testTagSuffix = "sculpture_${MuseumTopic.Random.testTagSuffix}",
        )
        val institutions = MuseumTopic.paintingSculptureInstitutions.map { topic ->
            PackTile(
                id = "sub_sculpture_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Sculpture, topic.testTagSuffix),
                testTagSuffix = "sculpture_${topic.testTagSuffix}",
            )
        }
        listOf(random) + institutions
    }
    PackFamily.Painting -> {
        val random = PackTile(
            id = "sub_painting_${MuseumTopic.Random.testTagSuffix}",
            titleRes = MuseumTopic.Random.labelRes,
            coverRes = PackCovers.museum(MuseumTopic.Random),
            selection = PackSelection(PackFamily.Painting, MuseumTopic.Random.testTagSuffix),
            testTagSuffix = "painting_${MuseumTopic.Random.testTagSuffix}",
        )
        val institutions = MuseumTopic.paintingSculptureInstitutions.map { topic ->
            PackTile(
                id = "sub_painting_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Painting, topic.testTagSuffix),
                testTagSuffix = "painting_${topic.testTagSuffix}",
            )
        }
        listOf(random) + institutions
    }
    PackFamily.Sound -> AudioPackTopic.entries.map { topic ->
        PackTile(
            id = "sub_sound_${topic.testTagSuffix}",
            titleRes = topic.labelRes,
            coverRes = PackCovers.audio(topic),
            selection = PackSelection(PackFamily.Sound, topic.testTagSuffix),
            testTagSuffix = "sound_${topic.testTagSuffix}",
            sellablePackId = topic.sellablePackId,
        )
    }
    PackFamily.VintageGames -> listOf(
        PackTile(
            id = "sub_vintage_${VintageGamesSub.AMBIENT}",
            titleRes = R.string.vintage_sub_ambient,
            coverRes = R.drawable.pack_vintage_games,
            selection = PackSelection(PackFamily.VintageGames, VintageGamesSub.AMBIENT),
            testTagSuffix = "vintage_${VintageGamesSub.AMBIENT}",
            itemCount = if (catalog.isNotEmpty()) {
                resolvePackPool(
                    catalog,
                    PackSelection(PackFamily.VintageGames, VintageGamesSub.AMBIENT),
                ).size
            } else {
                null
            },
            sellablePackId = MarketplaceCatalog.genartPackId(
                fr.geoking.arthur.shared.marketplace.GenartPackTopics.VINTAGE,
            ),
        ),
        PackTile(
            id = "sub_vintage_${VintageGamesSub.VISUALS}",
            titleRes = R.string.vintage_sub_visuals,
            coverRes = R.drawable.pack_genart_vintage,
            selection = PackSelection(PackFamily.VintageGames, VintageGamesSub.VISUALS),
            testTagSuffix = "vintage_${VintageGamesSub.VISUALS}",
            itemCount = if (catalog.isNotEmpty()) {
                resolvePackPool(
                    catalog,
                    PackSelection(PackFamily.VintageGames, VintageGamesSub.VISUALS),
                ).size
            } else {
                null
            },
            sellablePackId = MarketplaceCatalog.genartPackId(
                fr.geoking.arthur.shared.marketplace.GenartPackTopics.VINTAGE,
            ),
        ),
        PackTile(
            id = "sub_vintage_${VintageGamesSub.MUSIC}",
            titleRes = R.string.vintage_sub_music,
            coverRes = PackCovers.audio(AudioPackTopic.ArcadeChips),
            selection = PackSelection(PackFamily.VintageGames, VintageGamesSub.MUSIC),
            testTagSuffix = "vintage_${VintageGamesSub.MUSIC}",
            sellablePackId = AudioPackTopic.ArcadeChips.sellablePackId,
        ),
    )
}

fun resolvePackPool(catalog: List<Artwork>, selection: PackSelection): List<Artwork> {
    val pool = when (selection.family) {
        PackFamily.Museum -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Random
            val museumKinds = catalog.filter { art ->
                art.kind == ArtworkKind.Painting || art.kind == ArtworkKind.Sculpture
            }
            when (topic) {
                // Museum Random spans every institution, including Louvre.
                MuseumTopic.Random ->
                    museumKinds.filter { it.sourceId in MuseumTopic.institutionSourceIds }
                else -> museumKinds.filter { matchesMuseumTopic(it, topic) }
            }
        }
        PackFamily.Genart -> {
            val topic = selection.genartTopicOrNull() ?: GenartTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.GENART,
                genartTopic = topic,
            )
        }
        PackFamily.VintageGames -> catalog.filterByCategoryAndSources(
            category = CategoryFilter.GENART,
            genartTopic = GenartTopic.Vintage,
        )
        PackFamily.Personal -> catalog.filter { it.kind == ArtworkKind.PersonalPhoto }
        PackFamily.Photo -> {
            val photoSource = selection.photoSourceOrNull()
            if (photoSource != null) {
                catalog.filter { art ->
                    CategoryFilter.PHOTO.matches(art.kind) && matchesPhotoSource(art, photoSource)
                }
            } else {
                val topic = selection.stockCategoryOrNull() ?: StockPhotoCategory.Random
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.PHOTO,
                    stockCategory = topic,
                )
            }
        }
        PackFamily.Video -> {
            val source = selection.videoSourceOrNull()
            if (source != null) {
                catalog.filter { art ->
                    CategoryFilter.VIDEO.matches(art.kind) && matchesVideoSource(art, source)
                }
            } else {
                val topic = selection.stockCategoryOrNull() ?: StockPhotoCategory.Random
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.VIDEO,
                    stockCategory = topic,
                )
            }
        }
        PackFamily.Sculpture -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.SCULPTURE,
                museumTopic = topic,
            )
        }
        PackFamily.Painting -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.PAINTING,
                museumTopic = topic,
            )
        }
        PackFamily.Sound -> emptyList()
    }

    val isRandom = when (selection.family) {
        PackFamily.Museum -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
        PackFamily.Personal -> true
        PackFamily.Sound -> true
        PackFamily.VintageGames -> true
        PackFamily.Genart -> (selection.genartTopicOrNull() ?: GenartTopic.Random) == GenartTopic.Random
        PackFamily.Photo ->
            selection.photoSourceOrNull() != null ||
                (selection.stockCategoryOrNull() ?: StockPhotoCategory.Random) == StockPhotoCategory.Random
        PackFamily.Video ->
            selection.videoSourceOrNull() != null ||
                (selection.stockCategoryOrNull() ?: StockPhotoCategory.Random) == StockPhotoCategory.Random
        PackFamily.Sculpture -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
        PackFamily.Painting -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
    }

    return if (isRandom) pool.shuffled() else pool
}

fun PackSelection.stockCategoryOrNull(): StockPhotoCategory? =
    if (subId == null) {
        null
    } else {
        when (family) {
            PackFamily.Photo ->
                if (photoSourceOrNull() != null) {
                    null
                } else {
                    StockPhotoCategory.fromQuery(subId)
                }
            PackFamily.Video -> {
                if (videoSourceOrNull() != null) {
                    null
                } else {
                    StockPhotoCategory.entries.firstOrNull {
                        it.query.equals(subId, ignoreCase = true)
                    }
                }
            }
            else -> null
        }
    }

fun PackSelection.photoSourceOrNull(): PhotoTopic? =
    if (family != PackFamily.Photo || subId == null) {
        null
    } else {
        PhotoTopic.entries.firstOrNull { it.testTagSuffix == subId }
    }

fun PackSelection.genartTopicOrNull(): GenartTopic? =
    if (family != PackFamily.Genart || subId == null) {
        null
    } else {
        GenartTopic.entries.firstOrNull { it.testTagSuffix == subId }
    }

fun PackSelection.audioPackTopicOrNull(): AudioPackTopic? =
    when {
        family == PackFamily.Sound && subId != null ->
            AudioPackTopic.entries.firstOrNull { it.testTagSuffix == subId }
        family == PackFamily.VintageGames && subId == VintageGamesSub.MUSIC ->
            AudioPackTopic.ArcadeChips
        else -> null
    }

/** True when this selection opens Sound Player instead of Ambient. */
fun PackSelection.opensSoundPlayer(): Boolean =
    family == PackFamily.Sound ||
        (family == PackFamily.VintageGames && subId == VintageGamesSub.MUSIC)

/** True when Ambient start should enable the Arcade Chips chiptune pack. */
fun PackSelection.appliesArcadeChipsOnAmbientStart(): Boolean =
    family == PackFamily.VintageGames &&
        (subId == null || subId == VintageGamesSub.AMBIENT)

/**
 * Enables ambient sound and sets style preference for a Sound pack tile.
 * Does not change the artwork rotation pool.
 */
fun PackSelection.applySoundPack(
    settings: AmbientAudioSettings,
    styleOverride: MusicStyle? = null,
) {
    val topic = when {
        family == PackFamily.Sound -> audioPackTopicOrNull()
        family == PackFamily.VintageGames && appliesArcadeChipsOnAmbientStart() ->
            AudioPackTopic.ArcadeChips
        family == PackFamily.VintageGames && subId == VintageGamesSub.MUSIC ->
            AudioPackTopic.ArcadeChips
        else -> return
    }
    settings.setEnabled(true)
    val style: MusicStyle? = styleOverride ?: when {
        topic == null -> MusicStyle.JazzPiano
        else -> MusicStyleIds.fromSuffix(topic.primaryStyleSuffix())
    }
    settings.setStylePreference(style)
    // Atmosphere packs benefit from Atmosphere character; music packs stay Melody-friendly.
    when (topic) {
        AudioPackTopic.HearthWeather, AudioPackTopic.DawnChorus, AudioPackTopic.TempleResonance,
        AudioPackTopic.WindGarden, AudioPackTopic.CosmicDrift,
        -> settings.setCharacter(fr.geoking.arthur.source.AmbientAudioCharacter.Atmosphere)
        else -> Unit
    }
}

fun AudioPackTopic.stylesInPack(): List<MusicStyle> =
    styleSuffixes().mapNotNull { MusicStyleIds.fromSuffix(it) }

fun AudioPackTopic.primaryStyle(): MusicStyle =
    MusicStyleIds.fromSuffix(primaryStyleSuffix()) ?: MusicStyle.JazzPiano

fun MusicStyle.labelRes(): Int = when (this) {
    MusicStyle.JazzPiano -> R.string.settings_ambient_sound_style_jazz
    MusicStyle.Zen -> R.string.settings_ambient_sound_style_zen
    MusicStyle.SoftGuitar -> R.string.settings_ambient_sound_style_guitar
    MusicStyle.BarAmbience -> R.string.settings_ambient_sound_style_bar
    MusicStyle.NightLounge -> R.string.settings_ambient_sound_style_lounge
    MusicStyle.AfricanPulse -> R.string.settings_ambient_sound_style_african
    MusicStyle.WindChimes -> R.string.settings_ambient_sound_style_chimes
    MusicStyle.TibetanBowl -> R.string.settings_ambient_sound_style_tibetan
    MusicStyle.OceanWaves -> R.string.settings_ambient_sound_style_ocean
    MusicStyle.SoftRain -> R.string.settings_ambient_sound_style_rain
    MusicStyle.WindAmbience -> R.string.settings_ambient_sound_style_wind
    MusicStyle.Fireplace -> R.string.settings_ambient_sound_style_fireplace
    MusicStyle.Songbirds -> R.string.settings_ambient_sound_style_songbirds
    MusicStyle.CosmicDrone -> R.string.settings_ambient_sound_style_cosmic
    MusicStyle.ClassicalPiano -> R.string.settings_ambient_sound_style_classical
    MusicStyle.OrchestraPads -> R.string.settings_ambient_sound_style_orchestra
    MusicStyle.OrchestraSwell -> R.string.settings_ambient_sound_style_orchestra_swell
    MusicStyle.ViolinLead -> R.string.settings_ambient_sound_style_violin
    MusicStyle.RockBallad -> R.string.settings_ambient_sound_style_rock_ballad
    MusicStyle.BassOnly -> R.string.settings_ambient_sound_style_bass_only
    MusicStyle.PianoBallad -> R.string.settings_ambient_sound_style_piano_ballad
    MusicStyle.HawaiianUkulele -> R.string.settings_ambient_sound_style_hawaiian_ukulele
    MusicStyle.Chiptune -> R.string.settings_ambient_sound_style_chiptune
    MusicStyle.ChipArp -> R.string.settings_ambient_sound_style_chip_arp
    MusicStyle.ArcadeGlow -> R.string.settings_ambient_sound_style_arcade_glow
}

fun PackSelection.museumTopicOrNull(): MuseumTopic? =
    if (subId == null) {
        null
    } else {
        when (family) {
            PackFamily.Museum,
            PackFamily.Sculpture,
            PackFamily.Painting,
            -> MuseumTopic.entries.firstOrNull { it.testTagSuffix == subId }
            else -> null
        }
    }

fun PackSelection.videoSourceOrNull(): VideoTopic? =
    if (family != PackFamily.Video || subId == null) {
        null
    } else {
        VideoTopic.entries.firstOrNull { it.testTagSuffix == subId }
    }

/**
 * Source ids to load for Ambient Start. Still packs load capability-matched providers
 * so free-tier slots are not eaten by unrelated Sources — and so we never fall back
 * to a genart engine like Particles. Genart uses the in-memory catalog (`null`).
 */
fun PackSelection.sourceIdsForAmbientLoad(): List<String>? = when (family) {
    PackFamily.Museum -> {
        when (val topic = museumTopicOrNull()) {
            null, MuseumTopic.Random -> MuseumTopic.institutions.mapNotNull { it.sourceId }
            else -> listOfNotNull(topic.sourceId)
        }
    }
    PackFamily.Painting -> stillKindAmbientIds(museumTopicOrNull())
    PackFamily.Sculpture -> stillKindAmbientIds(museumTopicOrNull())
    PackFamily.Photo -> {
        val photoSource = photoSourceOrNull()
        if (photoSource != null) {
            listOf(photoSource.sourceId)
        } else {
            SourceCapabilities.sourceIdsForPhotoProviders()
        }
    }
    PackFamily.Video -> when (val source = videoSourceOrNull()) {
        null -> SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Video)
        else -> listOf(source.sourceId)
    }
    PackFamily.Genart -> null
    PackFamily.VintageGames -> null
    PackFamily.Personal -> null
    PackFamily.Sound -> null
}

/** Painting / Sculpture pack: Random → museums without Louvre. */
private fun stillKindAmbientIds(topic: MuseumTopic?): List<String> =
    when (topic) {
        null, MuseumTopic.Random -> MuseumTopic.paintingSculptureInstitutions.mapNotNull { it.sourceId }
        else -> listOfNotNull(topic.sourceId)
    }

fun PackSelection.isGenartCustom(): Boolean =
    family == PackFamily.Genart && genartTopicOrNull() == GenartTopic.Custom

/** True when an empty pool may resolve a generative Artwork from the in-memory catalog. */
fun PackSelection.allowsGenerativeAmbientFallback(): Boolean =
    family == PackFamily.Genart || family == PackFamily.VintageGames

@StringRes
private fun StockPhotoCategory.packLabelRes(): Int = when (this) {
    StockPhotoCategory.Random -> R.string.stock_topic_random
    StockPhotoCategory.Nature -> R.string.stock_topic_nature
    StockPhotoCategory.City -> R.string.stock_topic_city
    StockPhotoCategory.Ocean -> R.string.stock_topic_ocean
    StockPhotoCategory.Mountains -> R.string.stock_topic_mountains
    StockPhotoCategory.Abstract -> R.string.stock_topic_abstract
    StockPhotoCategory.Architecture -> R.string.stock_topic_architecture
    StockPhotoCategory.Sky -> R.string.stock_topic_sky
    StockPhotoCategory.StreetArt -> R.string.stock_topic_streetart
}

/** Video keyword sub-packs: Random + remote topics (no curated fallback). */
private fun videoStockTopics(): List<StockPhotoCategory> =
    listOf(StockPhotoCategory.Random) + StockPhotoCategory.remoteSearchTopics
