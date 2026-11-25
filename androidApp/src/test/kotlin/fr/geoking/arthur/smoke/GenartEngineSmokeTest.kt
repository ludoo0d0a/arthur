package fr.geoking.arthur.smoke

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.geoking.arthur.genart.GenartEffectCanvas
import fr.geoking.arthur.genart.GenartEngineId
import fr.geoking.arthur.genart.GenartQuality
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders every genart engine live (Compose) to catch runtime crashes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class GenartEngineSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun everyEngineRendersWithoutCrash() {
        val filter = System.getenv("GENART_SMOKE_ENGINES")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
        val engines = GenartEngineId.values().filter { filter == null || it.name in filter }
        var current by mutableStateOf(engines.first())
        composeTestRule.setContent {
            Box(modifier = Modifier.size(360.dp)) {
                GenartEffectCanvas(
                    engine = current,
                    isActive = true,
                    modifier = Modifier.fillMaxSize(),
                    quality = GenartQuality.Medium,
                )
            }
        }
        val failures = mutableListOf<String>()
        for (engine in engines) {
            current = engine
            try {
                composeTestRule.mainClock.advanceTimeBy(1_200)
                composeTestRule.waitForIdle()
            } catch (t: Throwable) {
                failures += "${engine.name}: ${t::class.java.simpleName}: ${t.message}"
            }
        }
        check(failures.isEmpty()) { "Engine render failures:\n" + failures.joinToString("\n") }
    }
}
