package fr.geoking.arthur.audio.stem

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakePlayer(looper: Looper) : SimpleBasePlayer(looper) {
    var played = false
    var paused = false
    var setVolumeValue = 1.0f
    private val playlist = mutableListOf<MediaItemData>()

    override fun getState(): State {
        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_PREPARE,
                        Player.COMMAND_SET_VOLUME
                    )
                    .build()
            )
            .setPlaylist(if (playlist.isEmpty()) listOf(MediaItemData.Builder("uid").build()) else playlist)
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(played, Player.PLAYBACK_SUPPRESSION_REASON_NONE)
            .setVolume(setVolumeValue)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): com.google.common.util.concurrent.ListenableFuture<*> {
        played = playWhenReady
        paused = !playWhenReady
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(volume: Float): com.google.common.util.concurrent.ListenableFuture<*> {
        setVolumeValue = volume
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): com.google.common.util.concurrent.ListenableFuture<*> {
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): com.google.common.util.concurrent.ListenableFuture<*> {
        playlist.clear()
        mediaItems.forEach { item ->
            playlist.add(MediaItemData.Builder(item).build())
        }
        return com.google.common.util.concurrent.Futures.immediateVoidFuture()
    }
}

@RunWith(RobolectricTestRunner::class)
class MusicDirectorTest {

    private lateinit var fakePlayer1: FakePlayer
    private lateinit var fakePlayer2: FakePlayer
    private var playerIndex = 0

    private val testArrangement = StemArrangement(
        id = "test_arr",
        title = "Test Stem Arrangement",
        bpm = 90,
        loopDurationSeconds = 4.0,
        tracks = listOf(
            StemTrack(
                id = "track_1",
                name = "Lead Keys",
                category = StemCategory.JAZZ_MUSIC,
                filename = "keys.opus",
                defaultGain = 0.8f,
                isOptional = false,
                activationProbability = 1.0f
            ),
            StemTrack(
                id = "track_2",
                name = "Nature Rain",
                category = StemCategory.NATURE_FX,
                filename = "rain.opus",
                defaultGain = 0.5f,
                isOptional = true,
                activationProbability = 0.0f
            )
        )
    )

    @Before
    fun setup() {
        val looper = Looper.getMainLooper()
        fakePlayer1 = FakePlayer(looper)
        fakePlayer2 = FakePlayer(looper)
        playerIndex = 0
    }

    @Test
    fun testPlayAndStopState() {
        val director = MusicDirector(
            context = org.robolectric.RuntimeEnvironment.getApplication(),
            arrangementLoader = { testArrangement },
            playerFactory = {
                playerIndex++
                if (playerIndex == 1) fakePlayer1 else fakePlayer2
            }
        )

        director.loadArrangement(testArrangement)
        assertEquals(DirectorState.STOPPED, director.directorState.value)

        director.play(randomSeed = 42L)
        assertEquals(DirectorState.PLAYING, director.directorState.value)

        assertTrue(fakePlayer1.played)
        assertTrue(fakePlayer2.played)

        val statuses = director.trackStatuses.value
        assertEquals(2, statuses.size)

        director.stop()
        assertEquals(DirectorState.STOPPED, director.directorState.value)
    }

    @Test
    fun testTrackVolumeControl() {
        val director = MusicDirector(
            context = org.robolectric.RuntimeEnvironment.getApplication(),
            arrangementLoader = { testArrangement },
            playerFactory = { fakePlayer1 }
        )

        director.loadArrangement(testArrangement)
        director.play()

        director.setTrackVolume("track_1", 0.4f)
        assertEquals(0.4f, fakePlayer1.setVolumeValue, 0.001f)

        val track1Status = director.trackStatuses.value.find { it.trackId == "track_1" }
        assertEquals(0.4f, track1Status?.volume ?: 0f, 0.001f)
    }
}
