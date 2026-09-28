package fr.geoking.arthur.source

/**
 * Remote quote APIs for Ambient overlay.
 * [ZenQuotes] is English; [CitationLecog] is French (citation.lecog.fr).
 */
enum class QuoteProvider(val id: String) {
    ZenQuotes("zenquotes"),
    CitationLecog("citation_lecog"),
    ;

    companion object {
        fun fromId(id: String?): QuoteProvider? =
            entries.firstOrNull { it.id == id }

        /** French UI → Citation.lecog.fr; otherwise ZenQuotes. */
        fun defaultForLanguage(language: String): QuoteProvider =
            if (language.startsWith("fr", ignoreCase = true)) CitationLecog else ZenQuotes
    }
}
