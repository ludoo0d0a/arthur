package fr.geoking.arthur.auto

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.source.Quote

/** Title / artist lines shown by the AA media player chrome. */
internal data class AmbientMediaDisplayLines(
    val title: String,
    val artist: String,
)

/**
 * Prefer the famous quote over the artwork title so the host media UI surfaces
 * the citation. Uses the two available lines (title + artist/singer):
 * - quote text on title, author on artist when available
 * - long author-less quotes are split across both lines at a word boundary
 */
internal fun ambientMediaDisplayLines(
    art: Artwork,
    quote: Quote?,
    extraArtistSuffix: String? = null,
): AmbientMediaDisplayLines {
    val (title, artistBase) = if (quote != null && quote.text.isNotBlank()) {
        when {
            quote.author.isNotBlank() ->
                "\u201C${quote.text}\u201D" to "\u2014 ${quote.author}"
            quote.text.length > TITLE_SOFT_MAX ->
                splitQuoteAcrossLines(quote.text)
            else ->
                "\u201C${quote.text}\u201D" to art.attribution
        }
    } else {
        art.title to art.attribution
    }

    val artist = buildString {
        append(artistBase)
        if (!extraArtistSuffix.isNullOrBlank()) {
            if (isNotEmpty()) append(' ')
            append(extraArtistSuffix)
        }
    }
    return AmbientMediaDisplayLines(title = title, artist = artist)
}

private const val TITLE_SOFT_MAX = 48

private fun splitQuoteAcrossLines(text: String): Pair<String, String> {
    val softMax = TITLE_SOFT_MAX.coerceAtMost(text.length - 1)
    val minBreak = (softMax / 3).coerceAtLeast(1)
    val breakAt = text.lastIndexOf(' ', softMax).takeIf { it >= minBreak } ?: softMax
    val first = text.substring(0, breakAt).trimEnd()
    val second = text.substring(breakAt).trimStart()
    return "\u201C$first" to "$second\u201D"
}
