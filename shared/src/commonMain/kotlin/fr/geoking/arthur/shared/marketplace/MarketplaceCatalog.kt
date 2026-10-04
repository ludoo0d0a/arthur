package fr.geoking.arthur.shared.marketplace

import fr.geoking.arthur.shared.source.GenartSource

/**
 * One Marketplace SKU: RevenueCat entitlement + Play product, linked to content gates.
 */
data class SellablePack(
    val id: String,
    val entitlementId: String,
    val productId: String,
    /** Genart topic suffix when this pack unlocks a Genart sub-pack; null otherwise. */
    val genartTopicSuffix: String? = null,
    /** Audio pack suffix when this pack unlocks music styles; null otherwise. */
    val audioPackSuffix: String? = null,
)

/**
 * Static v1 Marketplace catalog (no network offerings yet).
 */
object MarketplaceCatalog {
    const val PERSONAL_PHOTOS_ID = "personal_photos"
    const val PERSONAL_PHOTOS_ENTITLEMENT = "pack_personal_photos"
    const val PERSONAL_PHOTOS_PRODUCT = "arthur_pack_personal_photos"

    fun genartPackId(topicSuffix: String): String = "genart_$topicSuffix"
    fun genartEntitlementId(topicSuffix: String): String = "pack_genart_$topicSuffix"
    fun genartProductId(topicSuffix: String): String = "arthur_pack_genart_$topicSuffix"

    /**
     * Free Genart engines without any pack purchase (All / Random free subset).
     * Monetized topic engines unlock via [SellablePack] ownership.
     */
    val freeGenartEngineIds: Set<String> = setOf(
        GenartSource.PARTICLES,
        GenartSource.PSEUDO3D,
        GenartSource.SOFT_SHADOWS,
        GenartSource.TUNNEL,
        GenartSource.TONAL_GEOMETRY,
        GenartSource.SPHERE,
        GenartSource.WAVES,
        GenartSource.MICRO,
    )

    val personalPhotos: SellablePack = SellablePack(
        id = PERSONAL_PHOTOS_ID,
        entitlementId = PERSONAL_PHOTOS_ENTITLEMENT,
        productId = PERSONAL_PHOTOS_PRODUCT,
    )

    fun genartPack(topicSuffix: String): SellablePack = SellablePack(
        id = genartPackId(topicSuffix),
        entitlementId = genartEntitlementId(topicSuffix),
        productId = genartProductId(topicSuffix),
        genartTopicSuffix = topicSuffix,
    )

    fun audioPackId(packSuffix: String): String = "audio_$packSuffix"
    fun audioEntitlementId(packSuffix: String): String = "pack_audio_$packSuffix"
    fun audioProductId(packSuffix: String): String = "arthur_pack_audio_$packSuffix"

    fun audioPack(packSuffix: String): SellablePack = SellablePack(
        id = audioPackId(packSuffix),
        entitlementId = audioEntitlementId(packSuffix),
        productId = audioProductId(packSuffix),
        audioPackSuffix = packSuffix,
    )

    fun all(): List<SellablePack> = listOf(personalPhotos) +
        GenartPackTopics.monetizedTopicSuffixes.map { genartPack(it) } +
        AudioPackCatalog.monetizedPackSuffixes.map { audioPack(it) }

    fun byId(id: String): SellablePack? = all().firstOrNull { it.id == id }

    fun sellablePackIdForGenartTopic(topicSuffix: String): String? =
        if (topicSuffix in GenartPackTopics.monetizedTopicSuffixes) genartPackId(topicSuffix) else null

    fun sellablePackIdForAudioPack(packSuffix: String): String? =
        if (packSuffix in AudioPackCatalog.monetizedPackSuffixes) audioPackId(packSuffix) else null
}
