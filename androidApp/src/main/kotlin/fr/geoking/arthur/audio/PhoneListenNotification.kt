package fr.geoking.arthur.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.MainActivity

/**
 * Phone media-style notification (title + play/pause) for [SoundPlayerScreen].
 *
 * Not an FGS: Play Console has not declared `FOREGROUND_SERVICE_MEDIA_PLAYBACK` yet
 * (see `doc/playstore/foreground-service-media.md`). Posts a normal MediaStyle notification.
 */
@OptIn(UnstableApi::class)
class PhoneListenNotification(
    context: Context,
    private val onPlayPause: (playing: Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val bridge = PhoneListenBridgePlayer(Looper.getMainLooper()) { playing ->
        onPlayPause(playing)
    }
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var started = false

    fun update(title: String, subtitle: String?, playing: Boolean) {
        ensureStarted()
        bridge.publish(title = title, subtitle = subtitle, playing = playing)
    }

    fun release() {
        notificationManager?.setPlayer(null)
        notificationManager = null
        mediaSession?.release()
        mediaSession = null
        bridge.release()
        started = false
        appContext.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIFICATION_ID)
    }

    private fun ensureStarted() {
        if (started) return
        started = true
        ensureChannel()
        val session = MediaSession.Builder(appContext, bridge)
            .setId(SESSION_ID)
            .setSessionActivity(openAppPendingIntent())
            .build()
        mediaSession = session

        val manager = PlayerNotificationManager.Builder(
            appContext,
            NOTIFICATION_ID,
            CHANNEL_ID,
        )
            .setMediaDescriptionAdapter(DescriptionAdapter())
            .setSmallIconResourceId(android.R.drawable.ic_media_play)
            .setChannelNameResourceId(R.string.music_playback_channel_name)
            .setChannelDescriptionResourceId(R.string.music_playback_channel_desc)
            .build()
            .also { nm ->
                nm.setMediaSessionToken(session.platformToken)
                nm.setUsePlayPauseActions(true)
                nm.setUseNextAction(false)
                nm.setUsePreviousAction(false)
                nm.setUseFastForwardAction(false)
                nm.setUseRewindAction(false)
                nm.setUseStopAction(false)
                nm.setUseChronometer(false)
                nm.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                nm.setPlayer(bridge)
            }
        notificationManager = manager
    }

    private fun ensureChannel() {
        val nm = appContext.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.music_playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = appContext.getString(R.string.music_playback_channel_desc)
                setShowBadge(false)
            },
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private inner class DescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence =
            player.mediaMetadata.title?.toString()
                ?: appContext.getString(R.string.app_name)

        override fun createCurrentContentIntent(player: Player): PendingIntent =
            openAppPendingIntent()

        override fun getCurrentContentText(player: Player): CharSequence? =
            player.mediaMetadata.artist?.toString()
                ?: player.mediaMetadata.subtitle?.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? = null
    }

    companion object {
        const val CHANNEL_ID = "phone_music_playback"
        const val NOTIFICATION_ID = 4410
        private const val SESSION_ID = "ArthurPhoneListen"
    }
}

/** One-item Media3 player used only to drive the phone media notification. */
@OptIn(UnstableApi::class)
internal class PhoneListenBridgePlayer(
    looper: Looper,
    private val onPlayWhenReadyChanged: (Boolean) -> Unit,
) : SimpleBasePlayer(looper) {

    private var playWhenReady: Boolean = false
    private var item: MediaItemData? = null

    fun publish(title: String, subtitle: String?, playing: Boolean) {
        playWhenReady = playing
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setSubtitle(subtitle)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId("phone-listen")
            .setMediaMetadata(metadata)
            .build()
        item = MediaItemData.Builder("phone-listen")
            .setMediaItem(mediaItem)
            .build()
        invalidateState()
    }

    override fun getState(): State {
        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
            )
            .build()
        val playlist = listOfNotNull(item)
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(playWhenReady, Player.PLAYBACK_SUPPRESSION_REASON_NONE)
            .setPlaybackState(if (playlist.isEmpty()) Player.STATE_IDLE else Player.STATE_READY)
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(0)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        onPlayWhenReadyChanged(playWhenReady)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleStop(): ListenableFuture<*> {
        playWhenReady = false
        onPlayWhenReadyChanged(false)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
