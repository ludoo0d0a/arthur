package fr.geoking.arthur.ui.components

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackGridLayoutTest {

    @Test
    fun portraitPhone_usesThreeColumns() {
        val layout = packGridLayout(
            maxWidth = 360.dp,
            maxHeight = 640.dp,
            windowSizeClass = WindowSizeClass(widthDp = 360f, heightDp = 640f),
            horizontalPadding = 40.dp,
            verticalPadding = 32.dp,
        )
        assertEquals(3, layout.columns)
        assertTrue(layout.maxCoverSize >= 72.dp)
    }

    @Test
    fun landscapePhone_keepsTwoRowsVisibleViaCoverCapOrExtraColumns() {
        // Short height: covers must shrink (and/or columns grow) so ~2 rows fit.
        val layout = packGridLayout(
            maxWidth = 800.dp,
            maxHeight = 280.dp,
            windowSizeClass = WindowSizeClass(widthDp = 800f, heightDp = 360f),
            horizontalPadding = 40.dp,
            verticalPadding = 24.dp,
        )
        assertTrue(layout.columns >= 3)
        val twoRowsNeeded =
            layout.maxCoverSize * 2 + 40.dp * 2 + 16.dp
        assertTrue(
            "cover ${layout.maxCoverSize} too tall for 2 rows in 256dp budget",
            twoRowsNeeded <= 256.dp,
        )
    }

    @Test
    fun expandedTvWidth_prefersAtLeastFourColumns() {
        val layout = packGridLayout(
            maxWidth = 960.dp,
            maxHeight = 540.dp,
            windowSizeClass = WindowSizeClass(widthDp = 960f, heightDp = 540f),
            horizontalPadding = 0.dp,
            verticalPadding = 24.dp,
        )
        assertTrue(layout.columns >= 4)
    }
}
