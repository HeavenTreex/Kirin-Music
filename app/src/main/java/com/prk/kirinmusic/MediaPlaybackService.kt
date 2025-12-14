package com.prk.kirinmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import com.prk.kirinmusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaPlaybackService : Service() {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var lastLoadedBitmap: Bitmap? = null

    var currentSong: Song? = null
        private set

    var onNext: (() -> Unit)? = null
    var onPrev: (() -> Unit)? = null

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0

    val duration: Int
        get() = mediaPlayer?.duration ?: 0

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    companion object {
        const val CHANNEL_ID = "kirin_music_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.prk.kirinmusic.ACTION_PLAY"
        const val ACTION_PAUSE = "com.prk.kirinmusic.ACTION_PAUSE"
        const val ACTION_NEXT = "com.prk.kirinmusic.ACTION_NEXT"
        const val ACTION_PREV = "com.prk.kirinmusic.ACTION_PREV"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = togglePlayPause()
                override fun onPause() = togglePlayPause()
                override fun onSkipToNext() { onNext?.invoke() }
                override fun onSkipToPrevious() { onPrev?.invoke() }
                override fun onSeekTo(pos: Long) {
                    seekTo(pos.toInt())
                }
            })
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                onNext?.invoke()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> if (currentSong != null) togglePlayPause()
            ACTION_PAUSE -> if (currentSong != null) togglePlayPause()
            ACTION_NEXT -> onNext?.invoke()
            ACTION_PREV -> onPrev?.invoke()
        }
        return START_NOT_STICKY
    }

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun playNewSong(song: Song, startPlaying: Boolean = true) {
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(applicationContext, Uri.parse(song.uri))
            mediaPlayer?.prepare()
            currentSong = song

            if (startPlaying) {
                mediaPlayer?.start()
            }
            
            lastLoadedBitmap = null // Resetear portada anterior
            updateMediaSessionState()
            loadArtAndShowNotification(song)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
            } else {
                mp.start()
            }
            updateMediaSessionState()
            showNotification(currentSong, lastLoadedBitmap)
        }
    }

    private fun updateMediaSessionState() {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val speed = try {
            mediaPlayer?.playbackParams?.speed ?: 1f
        } catch (e: Exception) { 1f }

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(state, currentPosition.toLong(), speed)
                .build()
        )

        currentSong?.let { song ->
            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.toLong())

            if (lastLoadedBitmap != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, lastLoadedBitmap)
            }

            mediaSession.setMetadata(metadataBuilder.build())
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        updateMediaSessionState()
    }

    fun setPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                mediaPlayer?.playbackParams = params.setSpeed(speed)
                updateMediaSessionState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadArtAndShowNotification(song: Song) {
        serviceScope.launch {
            val loader = ImageLoader(applicationContext)
            val request = ImageRequest.Builder(applicationContext)
                .data(Uri.parse(song.albumArtUri))
                .error(android.R.drawable.ic_media_play)
                .fallback(android.R.drawable.ic_media_play)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            lastLoadedBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: BitmapFactory.decodeResource(resources, android.R.drawable.ic_media_play)

            updateMediaSessionState()
            showNotification(song, lastLoadedBitmap)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproductor KirinMusic",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de música"
                setSound(null, null)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(song: Song?, bitmap: Bitmap?) {
        if (song == null) return

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this, 0, Intent(this, MediaPlaybackService::class.java).setAction(ACTION_PREV), PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 1, Intent(this, MediaPlaybackService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseIntent = PendingIntent.getService(
            this, 2, Intent(this, MediaPlaybackService::class.java).setAction(playPauseAction), PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(bitmap)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevIntent)
            .addAction(playPauseIcon, "Play/Pause", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .setStyle(MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setOngoing(isPlaying)

        val notification = notificationBuilder.build()

        if (isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            // Si no está reproduciendo, no forzamos startForeground para permitir swipe si es pausado.
            // Pero para persistencia de notificación, muchos players la mantienen.
            // Si queremos que el usuario pueda descartarla cuando está en pausa, stopForeground(false) es la clave.
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            )
            stopForeground(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaSession.release()
        mediaPlayer = null
    }
}
