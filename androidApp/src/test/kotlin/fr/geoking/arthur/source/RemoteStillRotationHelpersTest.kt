package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class InvalidArtworkStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun marksAndPersistsInvalidIds() {
        context.getSharedPreferences("arthur_invalid_artwork", Context.MODE_PRIVATE).edit().clear().commit()
        val store = InvalidArtworkStore(context)
        store.markInvalid("a1")
        assertTrue(store.isInvalid("a1"))
        assertTrue(InvalidArtworkStore(context).isInvalid("a1"))
        store.clear("a1")
        assertFalse(InvalidArtworkStore(context).isInvalid("a1"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RemoteStillNetworkGateTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun canDownloadWhenWifiOnlyDisabled() {
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        settings.setWifiOnlyRemoteStills(false)
        val gate = RemoteStillNetworkGate(context, settings)
        assertTrue(gate.canDownloadRemoteStill())
        assertFalse(gate.isCacheOnlyMode())
    }

    @Test
    fun cacheOnlyWhenWifiOnlyEnabledWithoutUnmetered() {
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        settings.setWifiOnlyRemoteStills(true)
        val gate = RemoteStillNetworkGate(context, settings)
        // Robolectric default: no active unmetered network → cache-only
        assertFalse(gate.canDownloadRemoteStill())
        assertTrue(gate.isCacheOnlyMode())
    }
}

class AmbientStillPickerTest {
    @Test
    fun prefersUnseenOverSeen() {
        val picked = AmbientStillPicker.pickNextRandom(
            poolIds = listOf("a", "b", "c"),
            currentId = "a",
            seenIds = setOf("a", "b"),
            recentIds = emptyList(),
            random = kotlin.random.Random(0),
        )
        assertEquals("c", picked)
    }

    @Test
    fun prefersNonRecentWhenAllSeen() {
        val picked = AmbientStillPicker.pickNextRandom(
            poolIds = listOf("a", "b", "c"),
            currentId = "a",
            seenIds = setOf("a", "b", "c"),
            recentIds = listOf("b", "a"),
            random = kotlin.random.Random(0),
        )
        assertEquals("c", picked)
    }

    @Test
    fun respectsEligibleFilterForCacheOnly() {
        val picked = AmbientStillPicker.pickNextRandom(
            poolIds = listOf("a", "b", "c"),
            currentId = "a",
            seenIds = emptySet(),
            recentIds = emptyList(),
            eligibleIds = setOf("b"),
            random = kotlin.random.Random(0),
        )
        assertEquals("b", picked)
    }

    @Test
    fun returnsNullWhenNoEligible() {
        val picked = AmbientStillPicker.pickNextRandom(
            poolIds = listOf("a", "b"),
            currentId = "a",
            seenIds = emptySet(),
            recentIds = emptyList(),
            eligibleIds = emptySet(),
        )
        assertEquals(null, picked)
    }
}

internal object TestStillBytes {
    fun jpeg(): ByteArray {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size) { i ->
            val x = i % size
            val y = i / size
            android.graphics.Color.rgb((x * 3) % 256, (y * 5) % 256, (x + y) % 256)
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        val out = ByteArrayOutputStream()
        // PNG is larger / more reliable than solid JPEG for MIN_BYTES
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        val bytes = out.toByteArray()
        require(bytes.size > 1_000) { "Still too small for cache MIN_BYTES (${bytes.size})" }
        return bytes
    }
}
