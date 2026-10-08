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
        MediaControlBridge.attach(this)
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        refreshSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaControlBridge.detach()
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

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""

        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
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

        // Ambil title & text dari berbagai key (WA kadang pakai key berbeda)
        val title = extras.getCharSequence("android.title")?.toString()
            ?: extras.getCharSequence("android.conversationTitle")?.toString()
            ?: ""
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.subText")?.toString()
            ?: ""

        Log.d("MediaListener", "Notif from $pkg: [$title] [$text]")

        when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> handleWhatsApp(title, text, pkg)
            else -> handleCallNotification(title, text, pkg)
        }
    }

    // ============================================
    // WHATSAPP HANDLER
    // ============================================
    private fun handleWhatsApp(title: String, text: String, pkg: String) {
        // Deteksi panggilan masuk WA
        val isIncomingCall = text.contains("panggilan masuk", ignoreCase = true) ||
                text.contains("incoming call", ignoreCase = true) ||
                text.contains("video call", ignoreCase = true) ||
                text.contains("panggilan video", ignoreCase = true) ||
                (title.contains("panggilan", ignoreCase = true) &&
                        !text.contains("tak terjawab", ignoreCase = true))

        // Deteksi misscall WA
        val isMissedCall = text.contains("panggilan tak terjawab", ignoreCase = true) ||
                text.contains("missed call", ignoreCase = true) ||
                text.contains("tidak terjawab", ignoreCase = true) ||
                text.contains("tak dijawab", ignoreCase = true) ||
                title.contains("panggilan tak terjawab", ignoreCase = true) ||
                title.contains("missed call", ignoreCase = true)

        when {
            isIncomingCall -> {
                Log.d("MediaListener", "WA incoming call: $title")
                IslandState.showIncomingCall(
                    CallInfo(
                        name = title.ifEmpty { "WhatsApp" },
                        number = "WhatsApp Call",
                        avatarInitial = title.firstOrNull()?.uppercase()?.toString() ?: "W",
                        packageName = "com.whatsapp"
                    )
                )
            }

            isMissedCall -> {
                Log.d("MediaListener", "WA misscall: $title")
                val initial = title.firstOrNull()?.uppercase()?.toString() ?: "W"
                IslandState.showChat(
                    ChatInfo(
                        senderName = title.ifEmpty { "WhatsApp" },
                        message = "📞 Panggilan tak terjawab",
                        avatarInitial = initial,
                        packageName = "com.whatsapp"
                    )
                )
            }

            title.isNotEmpty() && text.isNotEmpty() -> {
                // Pesan WA biasa
                Log.d("MediaListener", "WA message: $title - $text")
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
    }

    // ============================================
    // CALL HANDLER (panggilan telepon biasa)
    // ============================================
    private fun handleCallNotification(title: String, text: String, pkg: String) {
        // Deteksi panggilan telepon masuk
        val isCall = text.contains("panggilan", ignoreCase = true) ||
                text.contains("incoming", ignoreCase = true) ||
                text.contains("call", ignoreCase = true) ||
                text.contains("missed", ignoreCase = true) ||
                title.contains("panggilan", ignoreCase = true)

        if (!isCall) return

        // Misscall telepon biasa
        val isMissed = text.contains("tak terjawab", ignoreCase = true) ||
                text.contains("missed", ignoreCase = true) ||
                title.contains("missed", ignoreCase = true)

        val callerName = when {
            title.contains(":") -> title.substringAfter(":").trim()
            else -> title.trim()
        }.ifEmpty { "Nomor Tidak Dikenal" }

        val initial = callerName.firstOrNull()?.uppercase()?.toString() ?: "?"

        if (isMissed) {
            IslandState.showChat(
                ChatInfo(
                    senderName = callerName,
                    message = "📞 Panggilan tak terjawab",
                    avatarInitial = initial,
                    packageName = pkg
                )
            )
        } else {
            IslandState.showIncomingCall(
                CallInfo(
                    name = callerName,
                    number = "Panggilan Masuk",
                    avatarInitial = initial,
                    packageName = pkg
                )
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Opsional: auto-dismiss kalau notif hilang
        // if (sbn?.packageName == "com.whatsapp" && IslandState.mode.value == IslandMode.CHAT) {
        //     IslandState.dismissChat()
        // }
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
