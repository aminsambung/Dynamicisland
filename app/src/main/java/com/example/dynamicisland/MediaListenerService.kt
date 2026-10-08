package com.example.dynamicisland

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.graphics.drawable.toBitmap

class MediaListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController()
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController()
        }
        override fun onSessionDestroyed() {
            activeController?.unregisterCallback(this)
            activeController = null
            IslandState.updateMusic(MusicInfo())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MediaListener", "Listener connected")
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        refreshSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        IslandState.updateMusic(MusicInfo())
    }

    private fun refreshSessions() {
        val mgr = mediaSessionManager ?: return
        try {
            val component = ComponentName(this, MediaListenerService::class.java)
            val sessions = mgr.getActiveSessions(component)

            // Prioritaskan session yang sedang playing
            val playing = sessions.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: sessions.firstOrNull()

            if (playing == null) {
                IslandState.updateMusic(MusicInfo())
                return
            }

            if (playing != activeController) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = playing
                playing.registerCallback(controllerCallback)
            }
            updateFromController()
        } catch (e: SecurityException) {
            Log.e("MediaListener", "No permission", e)
            IslandState.updateMusic(MusicInfo())
        }
    }

    private fun updateFromController() {
        val c = activeController ?: run {
            IslandState.updateMusic(MusicInfo())
            return
        }

        val metadata = c.metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING

        val art: Bitmap? = try {
            metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        } catch (_: Exception) { null }

        IslandState.updateMusic(
            MusicInfo(
                title = title,
                artist = artist,
                albumArt = art,
                isPlaying = isPlaying,
                packageName = c.packageName ?: ""
            )
        )
    }

    // Saat user buka app musik lain / ganti track
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshSessions()
    }
}
