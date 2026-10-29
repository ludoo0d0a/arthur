package fr.geoking.arthur.audio.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.MainActivity
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Started service that downloads royalty-free tracks with HTTP Range resume.
 *
 * Not a foreground service: Play Console must declare `FOREGROUND_SERVICE_DATA_SYNC`
 * before shipping an FGS (see `doc/playstore/music-download-fgs.md`). Until then,
 * downloads run as a normal service from the foreground UI; partial files are kept
 * under filesDir/downloaded_music for resume.
 */
class MusicDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = ConcurrentLinkedQueue<RemoteAudioTrack>()
    private val workerRunning = AtomicBoolean(false)
    private lateinit var manager: DownloadedMusicManager

    override fun onCreate() {
        super.onCreate()
        manager = DownloadedMusicManager(applicationContext)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENQUEUE -> {
                val track = trackFromIntent(intent)
                if (track == null) {
                    maybeStop()
                    return START_NOT_STICKY
                }
                when {
                    manager.isDownloaded(track.id) -> {
                        MusicDownloadTracker.setCompleted(track.id)
                        maybeStop()
                    }
                    MusicDownloadTracker.isActive(track.id) -> {
                        // Already queued/running — keep service alive
                    }
                    else -> {
                        MusicDownloadTracker.setDownloading(track.id, 0f)
                        queue.offer(track)
                        drainQueue()
                    }
                }
            }
            ACTION_STOP -> {
                clearProgressNotification()
                stopSelf()
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        clearProgressNotification()
        super.onDestroy()
    }

    private fun drainQueue() {
        if (!workerRunning.compareAndSet(false, true)) return
        scope.launch {
            try {
                while (true) {
                    val track = queue.poll() ?: break
                    updateNotification(track.title, progressPercent = 0, indeterminate = true)
                    MusicDownloadTracker.setDownloading(track.id, 0f)
                    val result = manager.downloadTrack(track) { progress ->
                        MusicDownloadTracker.setDownloading(track.id, progress)
                        updateNotification(
                            title = track.title,
                            progressPercent = (progress * 100).toInt().coerceIn(0, 100),
                            indeterminate = progress <= 0f,
                        )
                    }
                    result.fold(
                        onSuccess = {
                            MusicDownloadTracker.setCompleted(track.id)
                            showCompletedNotification(track.title)
                        },
                        onFailure = { error ->
                            MusicDownloadTracker.setFailed(
                                track.id,
                                error.message ?: "Download failed",
                            )
                            showFailedNotification(track.title, error.message)
                        },
                    )
                }
            } finally {
                workerRunning.set(false)
                if (queue.isEmpty()) {
                    clearProgressNotification()
                    stopSelf()
                } else {
                    drainQueue()
                }
            }
        }
    }

    private fun maybeStop() {
        if (queue.isEmpty() && !workerRunning.get()) {
            clearProgressNotification()
            stopSelf()
        }
    }

    private fun updateNotification(title: String, progressPercent: Int, indeterminate: Boolean) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(
            NOTIFICATION_ID,
            buildProgressNotification(title, progressPercent, indeterminate),
        )
    }

    private fun clearProgressNotification() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.cancel(NOTIFICATION_ID)
    }

    private fun showCompletedNotification(title: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(
            NOTIFICATION_ID + 1,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(getString(R.string.music_download_complete_title))
                .setContentText(title)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent())
                .build(),
        )
    }

    private fun showFailedNotification(title: String, message: String?) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(
            NOTIFICATION_ID + 2,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(getString(R.string.music_download_failed_title))
                .setContentText(message?.takeIf { it.isNotBlank() } ?: title)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent())
                .build(),
        )
    }

    private fun buildProgressNotification(
        title: String,
        progressPercent: Int,
        indeterminate: Boolean,
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.music_download_notification_title))
            .setContentText(title)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progressPercent, indeterminate)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.music_download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.music_download_channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_ENQUEUE = "fr.geoking.arthur.action.ENQUEUE_MUSIC_DOWNLOAD"
        const val ACTION_STOP = "fr.geoking.arthur.action.STOP_MUSIC_DOWNLOAD"

        private const val CHANNEL_ID = "music_downloads"
        private const val NOTIFICATION_ID = 4401

        private const val EXTRA_ID = "track_id"
        private const val EXTRA_TITLE = "track_title"
        private const val EXTRA_ARTIST = "track_artist"
        private const val EXTRA_STREAM_URL = "track_stream_url"
        private const val EXTRA_DURATION = "track_duration"
        private const val EXTRA_LICENSE = "track_license"
        private const val EXTRA_SOURCE_ID = "track_source_id"
        private const val EXTRA_ARTWORK = "track_artwork"

        fun enqueue(context: Context, track: RemoteAudioTrack) {
            val intent = Intent(context, MusicDownloadService::class.java).apply {
                action = ACTION_ENQUEUE
                putExtra(EXTRA_ID, track.id)
                putExtra(EXTRA_TITLE, track.title)
                putExtra(EXTRA_ARTIST, track.artist)
                putExtra(EXTRA_STREAM_URL, track.streamUrl)
                putExtra(EXTRA_DURATION, track.durationSeconds)
                putExtra(EXTRA_LICENSE, track.license)
                putExtra(EXTRA_SOURCE_ID, track.sourceId)
                putExtra(EXTRA_ARTWORK, track.artworkUrl)
            }
            context.startService(intent)
        }

        private fun trackFromIntent(intent: Intent): RemoteAudioTrack? {
            val id = intent.getStringExtra(EXTRA_ID) ?: return null
            val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: return null
            val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID) ?: return null
            return RemoteAudioTrack(
                id = id,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                artist = intent.getStringExtra(EXTRA_ARTIST).orEmpty(),
                streamUrl = streamUrl,
                durationSeconds = intent.getIntExtra(EXTRA_DURATION, 0),
                license = intent.getStringExtra(EXTRA_LICENSE) ?: "Royalty-Free",
                artworkUrl = intent.getStringExtra(EXTRA_ARTWORK),
                sourceId = sourceId,
            )
        }
    }
}
