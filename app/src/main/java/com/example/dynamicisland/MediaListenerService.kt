package com.example.dynamicisland

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MediaListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val monitoredPackages = setOf(
        // WhatsApp
        "com.whatsapp", "com.whatsapp.w4b",
        // Dialer
        "com.android.dialer", "com.samsung.android.dialer",
        "com.google.android.dialer", "com.android.phone",
        "com.android.server.telecom",
        // Navigation
        "com.google.android.apps.maps", "com.waze",
        "com.sygic.aura", "com.here.app.maps",
        // Alarm
        "com.google.android.deskclock", "com.android.deskclock",
        "com.miui.clock", "com.samsung.android.app.clock",
        "com.sec.android.app.clockpackage", "com.coloros.alarmclock",
        "com.oppo.alarmclock", "com.vivo.alarmclock",
        "com.android.alarmclock"
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

    // ============================================
    // BATTERY RECEIVER
    // ============================================
    private var lastChargingState = false
    private var lastBatteryLevel = -1

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
            val isFull = status == BatteryManager.BATTERY_STATUS_FULL

            val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val voltage = if (voltageMv > 0) voltageMv / 1000f else 0f

            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val temperature = if (tempTenths > 0) tempTenths / 10f else 0f

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val chargeType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> ""
            }

            val timeToFull = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val bm = context?.getSystemService(android.content.Context.BATTERY_SERVICE)
                            as? BatteryManager
                    val seconds = bm?.computeChargeTimeRemaining() ?: -1
                    if (seconds > 0) {
                        val hours = seconds / 3600
                        val minutes = (seconds % 3600) / 60
                        "%02d:%02d".format(hours, minutes)
                    } else ""
                } catch (_: Exception) { "" }
            } else ""

            if (isCharging != lastChargingState || percent != lastBatteryLevel) {
                lastChargingState = isCharging || isFull
                lastBatteryLevel = percent

                IslandState.updateCharging(
                    ChargingInfo(
                        percentage = percent,
                        voltage = voltage,
                        temperature = temperature,
                        timeToFull = timeToFull,
                        chargeType = chargeType,
                        isCharging = isCharging || isFull,
                        isFull = isFull
                    )
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
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
    // MEDIA SESSION
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
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""

        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""

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
    // NOTIFICATION HANDLER
    // ============================================
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        refreshSessions()

        val pkg = sbn.packageName ?: return
        if (pkg !in monitoredPackages) return

        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString()
            ?: extras.getCharSequence("android.conversationTitle")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.subText")?.toString() ?: ""

        Log.d("MediaListener", "Notif from $pkg: [$title] [$text]")

        when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> handleWhatsApp(title, text, pkg)

            "com.google.android.apps.maps", "com.waze",
            "com.sygic.aura", "com.here.app.maps" -> handleNavigation(title, text, pkg)

            "com.google.android.deskclock", "com.android.deskclock",
            "com.miui.clock", "com.samsung.android.app.clock",
            "com.sec.android.app.clockpackage", "com.coloros.alarmclock",
            "com.oppo.alarmclock", "com.vivo.alarmclock",
            "com.android.alarmclock" -> handleAlarm(title, text, pkg)

            else -> handleCallNotification(title, text, pkg)
        }
    }

    // ============================================
    // WHATSAPP
    // ============================================
    private fun handleWhatsApp(title: String, text: String, pkg: String) {
        val isIncomingCall = text.contains("panggilan masuk", ignoreCase = true) ||
                text.contains("incoming call", ignoreCase = true) ||
                text.contains("video call", ignoreCase = true) ||
                text.contains("panggilan video", ignoreCase = true) ||
                (title.contains("panggilan", ignoreCase = true) &&
                        !text.contains("tak terjawab", ignoreCase = true))

        val isMissedCall = text.contains("panggilan tak terjawab", ignoreCase = true) ||
                text.contains("missed call", ignoreCase = true) ||
                text.contains("tidak terjawab", ignoreCase = true) ||
                text.contains("tak dijawab", ignoreCase = true) ||
                title.contains("panggilan tak terjawab", ignoreCase = true) ||
                title.contains("missed call", ignoreCase = true)

        when {
            isIncomingCall -> {
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
    // CALL (telepon biasa)
    // ============================================
    private fun handleCallNotification(title: String, text: String, pkg: String) {
        val isCall = text.contains("panggilan", ignoreCase = true) ||
                text.contains("incoming", ignoreCase = true) ||
                text.contains("call", ignoreCase = true) ||
                text.contains("missed", ignoreCase = true) ||
                title.contains("panggilan", ignoreCase = true)

        if (!isCall) return

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

    // ============================================
    // NAVIGATION
    // ============================================
    private fun handleNavigation(title: String, text: String, pkg: String) {
        val etaPattern = Regex("""(\d+\s*min)\s*·\s*([\d.,]+\s*\w+)\s*·\s*(\d{1,2}[:.]\d{2})\s*ETA""")
        val etaMatch = etaPattern.find(title)
        val duration = etaMatch?.groupValues?.get(1) ?: ""
        val distanceTotal = etaMatch?.groupValues?.get(2) ?: ""
        val eta = etaMatch?.groupValues?.get(3) ?: ""

        val parts = text.split("·").map { it.trim() }
        val distance = parts.getOrNull(0) ?: ""
        val instruction = parts.getOrNull(1) ?: text

        val appName = when (pkg) {
            "com.google.android.apps.maps" -> "Maps"
            "com.waze" -> "Waze"
            "com.sygic.aura" -> "Sygic"
            "com.here.app.maps" -> "HERE"
            else -> "Navigation"
        }

        if (instruction.isNotEmpty() || distance.isNotEmpty()) {
            IslandState.showNavigation(
                NavigationInfo(
                    instruction = instruction,
                    distance = distance,
                    duration = duration,
                    distanceTotal = distanceTotal,
                    eta = eta,
                    appName = appName,
                    packageName = pkg
                )
            )
        }
    }

    // ============================================
    // ALARM (saat berbunyi — update label + ringing)
    // ============================================
    private fun handleAlarm(title: String, text: String, pkg: String) {
        Log.d("MediaListener", "Alarm notif: [$title] [$text]")

        val timeMatch = Regex("""(\d{1,2}[:.]\d{2})""").find(title)
        val time = timeMatch?.groupValues?.get(1)?.replace(".", ":") ?: ""

        val label = when {
            time.isNotEmpty() && title.contains(time) -> {
                title.replace(time, "").trim(' ', '-', ':', '·')
            }
            else -> title
        }.ifEmpty { text.ifEmpty { "Alarm" } }

        val current = IslandState.alarmInfo.value
        IslandState.showAlarm(
            AlarmInfo(
                time = time.ifEmpty { current.time },
                label = label,
                minutesUntil = 0,
                isRinging = true,
                packageName = pkg
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // ============================================
    // MEDIA CONTROL
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
        } catch (e: Exception) {
            Log.e("MediaListener", "playPause error", e)
        }
    }

    fun skipNext() {
        val c = activeController ?: return
        try { c.transportControls.skipToNext() } catch (e: Exception) {
            Log.e("MediaListener", "next error", e)
        }
    }

    fun skipPrevious() {
        val c = activeController ?: return
        try { c.transportControls.skipToPrevious() } catch (e: Exception) {
            Log.e("MediaListener", "prev error", e)
        }
    }
}
