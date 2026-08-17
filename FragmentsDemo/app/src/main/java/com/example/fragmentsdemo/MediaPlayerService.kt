package com.example.fragmentsdemo

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MediaPlayerService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        private const val TAG = "MediaPlayerService"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing ExoPlayer and MediaSession")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exoPlayer

        mediaSession = MediaSession.Builder(this, exoPlayer).build()

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle("Sample Music")
            .setArtist("Fragments Demo")
            .setAlbumTitle("Background Music")
            .setArtworkUri(Uri.parse("android.resource://$packageName/${R.mipmap.ic_launcher}"))
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId("sample_music")
            .setUri(Uri.parse("android.resource://$packageName/${R.raw.sample_music}"))
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Releasing resources")
        player?.let {
            it.stop()
            it.release()
        }
        mediaSession?.let {
            it.release()
        }
        player = null
        mediaSession = null
        super.onDestroy()
    }
}
