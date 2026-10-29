package fr.geoking.arthur.shared.audio.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RemoteAudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val durationSeconds: Int = 0,
    val license: String = "Royalty-Free",
    val artworkUrl: String? = null,
    val sourceId: String,
)

interface RemoteAudioSource {
    val id: String
    val displayName: String

    suspend fun fetchZenTracks(
        query: String = "zen calm ambient",
        limit: Int = 20,
    ): Result<List<RemoteAudioTrack>>
}

private val json = Json { ignoreUnknownKeys = true }

// --- Jamendo Music Provider ---

class JamendoMusicSource(
    private val httpGet: suspend (url: String) -> String,
    private val clientId: String = "",
) : RemoteAudioSource {
    override val id: String = ID
    override val displayName: String = "Jamendo Music"

    override suspend fun fetchZenTracks(query: String, limit: Int): Result<List<RemoteAudioTrack>> = runCatching {
        if (clientId.isBlank()) error("Missing API key: JAMENDO_CLIENT_ID")
        val formattedQuery = query.replace(" ", "+")
        val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$clientId&format=json&tags=$formattedQuery&limit=$limit&audioformat=mp32"
        val body = httpGet(url)
        val response = json.decodeFromString<JamendoResponse>(body)
        response.results.mapNotNull { hit ->
            val streamUrl = hit.audio?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            RemoteAudioTrack(
                id = "$ID-${hit.id}",
                title = hit.name?.takeIf { it.isNotBlank() } ?: "Jamendo Track ${hit.id}",
                artist = hit.artistName?.takeIf { it.isNotBlank() } ?: "Jamendo Artist",
                streamUrl = streamUrl,
                durationSeconds = hit.duration ?: 0,
                license = hit.licenseCcUrl?.takeIf { it.isNotBlank() } ?: "Creative Commons",
                artworkUrl = hit.image?.takeIf { it.isNotBlank() },
                sourceId = ID,
            )
        }
    }

    companion object {
        const val ID = "jamendo"
    }
}

@Serializable
private data class JamendoResponse(
    val results: List<JamendoTrack> = emptyList(),
)

@Serializable
private data class JamendoTrack(
    val id: String,
    val name: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    val audio: String? = null,
    val duration: Int? = null,
    @SerialName("license_ccurl") val licenseCcUrl: String? = null,
    val image: String? = null,
)

// --- Freesound Audio Provider ---

class FreesoundMusicSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String = "",
) : RemoteAudioSource {
    override val id: String = ID
    override val displayName: String = "Freesound"

    override suspend fun fetchZenTracks(query: String, limit: Int): Result<List<RemoteAudioTrack>> = runCatching {
        if (apiKey.isBlank()) error("Missing API key: FREESOUND_API_KEY")
        val formattedQuery = query.replace(" ", "+")
        val url = "https://freesound.org/apiv2/search/text/?token=$apiKey&query=$formattedQuery&page_size=$limit&fields=id,name,username,previews,duration,license"
        val body = httpGet(url)
        val response = json.decodeFromString<FreesoundSearchResponse>(body)
        response.results.mapNotNull { sound ->
            val streamUrl = sound.previews?.previewHqMp3?.takeIf { it.isNotBlank() }
                ?: sound.previews?.previewLqMp3?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            RemoteAudioTrack(
                id = "$ID-${sound.id}",
                title = sound.name?.takeIf { it.isNotBlank() } ?: "Freesound Sound ${sound.id}",
                artist = sound.username?.takeIf { it.isNotBlank() } ?: "Freesound User",
                streamUrl = streamUrl,
                durationSeconds = sound.duration?.toInt() ?: 0,
                license = sound.license?.takeIf { it.isNotBlank() } ?: "Creative Commons",
                sourceId = ID,
            )
        }
    }

    companion object {
        const val ID = "freesound"
    }
}

@Serializable
private data class FreesoundSearchResponse(
    val results: List<FreesoundSound> = emptyList(),
)

@Serializable
private data class FreesoundSound(
    val id: Long,
    val name: String? = null,
    val username: String? = null,
    val previews: FreesoundPreviews? = null,
    val duration: Double? = null,
    val license: String? = null,
)

@Serializable
private data class FreesoundPreviews(
    @SerialName("preview-hq-mp3") val previewHqMp3: String? = null,
    @SerialName("preview-lq-mp3") val previewLqMp3: String? = null,
)

// --- Archive.org Audio Provider ---

class ArchiveOrgMusicSource(
    private val httpGet: suspend (url: String) -> String,
) : RemoteAudioSource {
    override val id: String = ID
    override val displayName: String = "Internet Archive Audio"

    override suspend fun fetchZenTracks(query: String, limit: Int): Result<List<RemoteAudioTrack>> = runCatching {
        val formattedQuery = query.replace(" ", "+")
        val url = "https://archive.org/advancedsearch.php?q=mediatype:audio+AND+subject:($formattedQuery)&fl[]=identifier,title,creator,licenseurl&rows=$limit&output=json"
        val body = httpGet(url)
        val response = json.decodeFromString<ArchiveOrgResponse>(body)
        response.response?.docs.orEmpty().mapNotNull { doc ->
            val identifier = doc.identifier?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val mp3Name = resolveFirstMp3(identifier) ?: return@mapNotNull null
            RemoteAudioTrack(
                id = "$ID-$identifier",
                title = doc.title?.takeIf { it.isNotBlank() } ?: "Archive.org $identifier",
                artist = doc.creator?.takeIf { it.isNotBlank() } ?: "Internet Archive",
                streamUrl = "https://archive.org/download/$identifier/${mp3Name.replace(" ", "%20")}",
                durationSeconds = 180,
                license = doc.licenseUrl?.takeIf { it.isNotBlank() } ?: "Public Domain / CC",
                artworkUrl = "https://archive.org/services/img/$identifier",
                sourceId = ID,
            )
        }
    }

    /** Resolve a real downloadable .mp3 from item metadata (guessing `$id/$id.mp3` is often wrong). */
    private suspend fun resolveFirstMp3(identifier: String): String? {
        val metaBody = httpGet("https://archive.org/metadata/$identifier")
        val meta = runCatching { json.decodeFromString<ArchiveOrgMetadata>(metaBody) }.getOrNull()
            ?: return null
        return meta.files
            .asSequence()
            .mapNotNull { it.name?.takeIf { name -> name.endsWith(".mp3", ignoreCase = true) } }
            .firstOrNull()
    }

    companion object {
        const val ID = "archive_org"
    }
}

@Serializable
private data class ArchiveOrgResponse(
    val response: ArchiveOrgBody? = null,
)

@Serializable
private data class ArchiveOrgBody(
    val docs: List<ArchiveOrgDoc> = emptyList(),
)

@Serializable
private data class ArchiveOrgDoc(
    val identifier: String? = null,
    val title: String? = null,
    val creator: String? = null,
    @SerialName("licenseurl") val licenseUrl: String? = null,
)

@Serializable
private data class ArchiveOrgMetadata(
    val files: List<ArchiveOrgFile> = emptyList(),
)

@Serializable
private data class ArchiveOrgFile(
    val name: String? = null,
)

// --- Pixabay Audio Provider ---

class PixabayMusicSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String = "",
) : RemoteAudioSource {
    override val id: String = ID
    override val displayName: String = "Pixabay Music"

    override suspend fun fetchZenTracks(query: String, limit: Int): Result<List<RemoteAudioTrack>> = runCatching {
        if (apiKey.isBlank()) error("Missing API key: PIXABAY_API_KEY")
        val formattedQuery = query.replace(" ", "+")
        val url = "https://pixabay.com/api/audio/?key=$apiKey&q=$formattedQuery&per_page=$limit"
        val body = httpGet(url)
        val response = json.decodeFromString<PixabayAudioResponse>(body)
        response.hits.mapNotNull { hit ->
            val streamUrl = hit.audioUrl?.takeIf { it.isNotBlank() }
                ?: hit.audio?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            RemoteAudioTrack(
                id = "$ID-${hit.id}",
                title = hit.title?.takeIf { it.isNotBlank() } ?: "Pixabay Track ${hit.id}",
                artist = hit.user?.takeIf { it.isNotBlank() } ?: "Pixabay Artist",
                streamUrl = streamUrl,
                durationSeconds = hit.duration ?: 0,
                license = "Pixabay License",
                sourceId = ID,
            )
        }
    }

    companion object {
        const val ID = "pixabay_music"
    }
}

@Serializable
private data class PixabayAudioResponse(
    val hits: List<PixabayAudioHit> = emptyList(),
)

@Serializable
private data class PixabayAudioHit(
    val id: Long,
    val title: String? = null,
    val user: String? = null,
    val audio: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val duration: Int? = null,
)

// --- Incompetech (Kevin MacLeod) Provider ---

class IncompetechMusicSource(
    private val httpGet: suspend (url: String) -> String = { "" },
) : RemoteAudioSource {
    override val id: String = ID
    override val displayName: String = "Incompetech (Kevin MacLeod)"

    override suspend fun fetchZenTracks(query: String, limit: Int): Result<List<RemoteAudioTrack>> = runCatching {
        // Curated CC-BY 4.0 Kevin MacLeod tracks with verified working direct MP3 URLs.
        val curated = listOf(
            RemoteAudioTrack(
                id = "$ID-meditation-impromptu-01",
                title = "Meditation Impromptu 01",
                artist = "Kevin MacLeod",
                streamUrl = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Meditation%20Impromptu%2001.mp3",
                durationSeconds = 195,
                license = "CC BY 4.0 (incompetech.com)",
                sourceId = ID,
            ),
            RemoteAudioTrack(
                id = "$ID-meditation-impromptu-02",
                title = "Meditation Impromptu 02",
                artist = "Kevin MacLeod",
                streamUrl = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Meditation%20Impromptu%2002.mp3",
                durationSeconds = 200,
                license = "CC BY 4.0 (incompetech.com)",
                sourceId = ID,
            ),
            RemoteAudioTrack(
                id = "$ID-dreamy-flashback",
                title = "Dreamy Flashback",
                artist = "Kevin MacLeod",
                streamUrl = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Dreamy%20Flashback.mp3",
                durationSeconds = 150,
                license = "CC BY 4.0 (incompetech.com)",
                sourceId = ID,
            ),
            RemoteAudioTrack(
                id = "$ID-comfortable-mystery",
                title = "Comfortable Mystery",
                artist = "Kevin MacLeod",
                streamUrl = "https://incompetech.com/music/royalty-free/mp3-royaltyfree/Comfortable%20Mystery.mp3",
                durationSeconds = 180,
                license = "CC BY 4.0 (incompetech.com)",
                sourceId = ID,
            ),
        )
        curated.take(limit)
    }

    companion object {
        const val ID = "incompetech"
    }
}

// --- Music Provider Factory ---

class MusicProviderFactory(
    private val httpGet: suspend (url: String) -> String = { "" },
    private val jamendoClientId: String = "",
    private val freesoundApiKey: String = "",
    private val pixabayApiKey: String = "",
) {
    fun createAll(): List<RemoteAudioSource> = listOf(
        createJamendo(),
        createFreesound(),
        createArchiveOrg(),
        createPixabay(),
        createIncompetech(),
    )

    fun createJamendo(): RemoteAudioSource = JamendoMusicSource(httpGet, jamendoClientId)
    fun createFreesound(): RemoteAudioSource = FreesoundMusicSource(httpGet, freesoundApiKey)
    fun createArchiveOrg(): RemoteAudioSource = ArchiveOrgMusicSource(httpGet)
    fun createPixabay(): RemoteAudioSource = PixabayMusicSource(httpGet, pixabayApiKey)
    fun createIncompetech(): RemoteAudioSource = IncompetechMusicSource(httpGet)

    fun provider(id: String): RemoteAudioSource? = when (id) {
        JamendoMusicSource.ID -> createJamendo()
        FreesoundMusicSource.ID -> createFreesound()
        ArchiveOrgMusicSource.ID -> createArchiveOrg()
        PixabayMusicSource.ID -> createPixabay()
        IncompetechMusicSource.ID -> createIncompetech()
        else -> null
    }

    /** BuildConfig / local.properties keys that are blank (providers will fail until set). */
    fun missingApiKeys(): List<String> = buildList {
        if (jamendoClientId.isBlank()) add("JAMENDO_CLIENT_ID")
        if (freesoundApiKey.isBlank()) add("FREESOUND_API_KEY")
        if (pixabayApiKey.isBlank()) add("PIXABAY_API_KEY")
    }
}
