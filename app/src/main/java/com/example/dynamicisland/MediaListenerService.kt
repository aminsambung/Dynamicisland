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
        "com.whatsapp",                  // WhatsApp
        "com.whatsapp.w4b",              // WhatsApp Business
        "com.android.dialer",            // Dialer (panggilan biasa)
        "com.samsung.android.dialer",    // Samsung Dialer
        "com.google.android.dialer",     // Google Dialer
        "com.android.phone",             // Phone
        "com.android.server.telecom"     // Telecom
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
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        refreshSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
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
        refreshSessions()  // tetap cek media session juga

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
        // Deteksi panggilan WA masuk
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
            // Notifikasi pesan WA
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
        // Deteksi panggilan telepon biasa
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
        // Kalau notif WA hilang, kembalikan mode ke IDLE (opsional)
        // Kalau mau auto-dismiss chat setelah baca, aktifkan ini:
        // if (sbn?.packageName in monitoredPackages) IslandState.dismissChat()
    }
}
