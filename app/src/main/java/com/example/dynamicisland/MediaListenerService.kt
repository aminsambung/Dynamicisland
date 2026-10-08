package com.example.dynamicisland

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MediaListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    // Package yang kita dengar notifikasinya
    private val monitoredPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.google.android.dialer",
        "com.android.phone",
        "com.android.server.telecom"
    )

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateFromController()
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateFromController()
        override fun onSessionDestroyed() {
            activeController?.unregisterCallback(this)
            activeController = null
            IslandState.updateMusic(MusicInfo())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MediaListener", "Listener connected")
        MediaControlBridge.attach(this)   // ⬅️ TAMBAHAN
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        refreshSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaControlBridge.detach()       // ⬅️ TAMBAHAN
        IslandState.updateMusic(MusicInfo())
    }

    // ============================================
    // MEDIA SESSION (musik)
    // ============================================
    private fun refreshSessions() {
        val mgr = mediaSessionManager ?: return
        try {
            val component = ComponentName(this, MediaListenerService::class.java)
            val sessions = mgr.getActiveSessions(component)

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
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING
        val art: Bitmap? = try {
            metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        } catch (_: Exception) { null }

        IslandState.updateMusic(
            MusicInfo(title, artist, art, isPlaying, c.packageName ?: "")
        )
    }

    // ============================================
    // NOTIFIKASI (WA + Panggilan)
    // ============================================
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        refreshSessions()

        val pkg = sbn.packageName ?: return
        if (pkg !in monitoredPackages) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("MediaListener", "Notif from $pkg: $title - $text")

        when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> handleWhatsApp(title, text)
            else -> handleCallNotification(title, text, pkg)
        }
    }

    private fun handleWhatsApp(title: String, text: String) {
        val isCall = text.contains("panggilan", ignoreCase = true) ||
                text.contains("call", ignoreCase = true) ||
                text.contains("video", ignoreCase = true) ||
                title.contains("panggilan", ignoreCase = true)

        if (isCall) {
            IslandState.showIncomingCall(
                CallInfo(
                    name = title.ifEmpty { "WhatsApp" },
                    number = "WhatsApp Call",
                    avatarInitial = title.firstOrNull()?.uppercase() ?: "W"
                )
            )
        } else if (title.isNotEmpty() && text.isNotEmpty()) {
            val initial = title.firstOrNull()?.uppercase()?.toString() ?: "W"
            IslandState.showChat(
                ChatInfo(
                    senderName = title,
                    message = text,
                    avatarInitial = initial,
                    packageName = "com.whatsapp"
                )
            )
        }
    }

    private fun handleCallNotification(title: String, text: String, pkg: String) {
        if (text.contains("panggilan", ignoreCase = true) ||
            text.contains("incoming", ignoreCase = true) ||
            text.contains("call", ignoreCase = true) ||
            title.contains("panggilan", ignoreCase = true)) {

            val callerName = when {
                title.contains(":") -> title.substringAfter(":").trim()
                else -> title.trim()
            }.ifEmpty { "Nomor Tidak Dikenal" }

            IslandState.showIncomingCall(
                CallInfo(
                    name = callerName,
                    number = "Panggilan Masuk",
                    avatarInitial = callerName.firstOrNull()?.uppercase()?.toString() ?: "?"
                )
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // ============================================
    // MEDIA CONTROL — untuk kontrol dari Dynamic Island
    // ============================================
    fun playPause() {
        val c = activeController ?: return
        try {
            val state = c.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                c.transportControls.pause()
            } else {
                c.transportControls.play()
            }
            Log.d("MediaListener", "playPause called")
        } catch (e: Exception) {
            Log.e("MediaListener", "playPause error", e)
        }
    }

    fun skipNext() {
        val c = activeController ?: return
        try {
            c.transportControls.skipToNext()
            Log.d("MediaListener", "skipNext called")
        } catch (e: Exception) {
            Log.e("MediaListener", "next error", e)
        }
    }

    fun skipPrevious() {
        val c = activeController ?: return
        try {
            c.transportControls.skipToPrevious()
            Log.d("MediaListener", "skipPrevious called")
        } catch (e: Exception) {
            Log.e("MediaListener", "prev error", e)
        }
    }
}
