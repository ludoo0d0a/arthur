package fr.geoking.arthur.audio.banks

import fr.geoking.arthur.audio.MusicStyle
import org.junit.Test

class PatternBankTest {
    @Test
    fun everyStyleHasNonEmptyBanks() {
        for (style in MusicStyle.entries) {
            PatternBank.ensureNonEmpty(style)
        }
    }
}
