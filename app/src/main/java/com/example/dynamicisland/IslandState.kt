package com.example.dynamicisland

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf

enum class IslandMode {
    IDLE,
    MUSIC,
    CALL_RINGING,
    CALL_ACTIVE,
    CHAT,
    CHARGING,
    NAVIGATION,
    ALARM
}

data class CallInfo(
    val name: String = "Budi Santoso",
    val number: String = "+62 812-3456-7890",
    val avatarInitial: String = "B",
    val packageName: String = ""
)

data class ChatInfo(
    val senderName: String = "",
    val message: String = "",
    val avatarInitial: String = "",
    val packageName: String = ""
)

data class ChargingInfo(
    val percentage: Int = 0,
    val voltage: Float = 0f,
    val temperature: Float = 0f,
    val timeToFull: String = "",
    val chargeType: String = "",
    val isCharging: Boolean = false,
    val isFull: Boolean = false
)

data class NavigationInfo(
    val instruction: String = "",
    val distance: String = "",
    val duration: String = "",
    val distanceTotal: String = "",
    val eta: String = "",
    val appName: String = "Maps",
    val packageName: String = ""
)

data class AlarmInfo(
    val time: String = "06:00",
    val label: String = "Alarm",
    val minutesUntil: Int = 0,
    val isRinging: Boolean = false,
    val packageName: String = ""
)

object IslandState {
    val mode = mutableStateOf(IslandMode.IDLE)
    val callInfo = mutableStateOf(CallInfo())
    val musicInfo = mutableStateOf(MusicInfo())
    val chatInfo = mutableStateOf(ChatInfo())
    val chargingInfo = mutableStateOf(ChargingInfo())
    val navigationInfo = mutableStateOf(NavigationInfo())
    val alarmInfo = mutableStateOf(AlarmInfo())
    val isManuallyCollapsed = mutableStateOf(false)
    val isVisible = mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null
    private var chatDismissRunnable: Runnable? = null

    private const val IDLE_HIDE_DELAY_MS = 5_000L
    private const val PAUSE_HIDE_DELAY_MS = 3_000L
    private const val CHAT_AUTO_DISMISS_MS = 6_000L

    // ==================== VISIBILITY ====================
    fun showIsland() {
        isVisible.value = true
        cancelHideTimer()
    }

    fun hideIsland() {
        isVisible.value = false
        cancelHideTimer()
    }

    private fun scheduleHide(delayMs: Long) {
        cancelHideTimer()
        hideRunnable = Runnable {
            if (mode.value == IslandMode.IDLE ||
                (mode.value == IslandMode.MUSIC && !musicInfo.value.isPlaying)) {
                isVisible.value = false
            }
        }
        handler.postDelayed(hideRunnable!!, delayMs)
    }

    private fun cancelHideTimer() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = null
    }

    // ==================== CALL ====================
    fun showIncomingCall(info: CallInfo) {
        callInfo.value = info
        isManuallyCollapsed.value = false
        mode.value = IslandMode.CALL_RINGING
        showIsland()
    }

    fun acceptCall() {
        mode.value = IslandMode.CALL_ACTIVE
        showIsland()
    }

    fun endCall() {
        mode.value = IslandMode.IDLE
        scheduleHide(IDLE_HIDE_DELAY_MS)
    }

    fun dismissCall() {
        mode.value = if (musicInfo.value.isPlaying && !musicInfo.value.isEmpty) {
            IslandMode.MUSIC
        } else {
            IslandMode.IDLE
        }
        scheduleHide(IDLE_HIDE_DELAY_MS)
    }

    // ==================== MUSIC ====================
    fun showMusic() {
        isManuallyCollapsed.value = false
        mode.value = IslandMode.MUSIC
        showIsland()
    }

    fun updateMusic(info: MusicInfo) {
        musicInfo.value = info
        mode.value = if (info.isPlaying && !info.isEmpty) {
            when (mode.value) {
                IslandMode.CALL_RINGING,
                IslandMode.CALL_ACTIVE,
                IslandMode.CHAT,
                IslandMode.ALARM -> mode.value
                else -> IslandMode.MUSIC
            }
        } else {
            if (info.isEmpty) {
                isManuallyCollapsed.value = false
                if (mode.value == IslandMode.MUSIC) IslandMode.IDLE else mode.value
            } else {
                if (mode.value == IslandMode.CALL_RINGING ||
                    mode.value == IslandMode.CALL_ACTIVE ||
                    mode.value == IslandMode.CHAT ||
                    mode.value == IslandMode.ALARM) {
                    mode.value
                } else {
                    IslandMode.MUSIC
                }
            }
        }

        if (info.isPlaying && !info.isEmpty) {
            showIsland()
        } else if (!info.isEmpty) {
            scheduleHide(PAUSE_HIDE_DELAY_MS)
        } else {
            scheduleHide(IDLE_HIDE_DELAY_MS)
        }
    }

    // ==================== CHAT ====================
    fun showChat(info: ChatInfo) {
        if (mode.value == IslandMode.CALL_RINGING ||
            mode.value == IslandMode.CALL_ACTIVE) return

        chatInfo.value = info
        isManuallyCollapsed.value = false
        mode.value = IslandMode.CHAT
        showIsland()

        cancelChatDismissTimer()
        chatDismissRunnable = Runnable {
            if (mode.value == IslandMode.CHAT &&
                chatInfo.value.senderName == info.senderName) {
                dismissChat()
            }
        }
        handler.postDelayed(chatDismissRunnable!!, CHAT_AUTO_DISMISS_MS)
    }

    fun dismissChat() {
        cancelChatDismissTimer()
        if (mode.value == IslandMode.CHAT) {
            mode.value = if (musicInfo.value.isPlaying && !musicInfo.value.isEmpty) {
                IslandMode.MUSIC
            } else {
                IslandMode.IDLE
            }
            scheduleHide(IDLE_HIDE_DELAY_MS)
        }
    }

    private fun cancelChatDismissTimer() {
        chatDismissRunnable?.let { handler.removeCallbacks(it) }
        chatDismissRunnable = null
    }

    // ==================== CHARGING ====================
    fun updateCharging(info: ChargingInfo) {
        chargingInfo.value = info
        if (info.isCharging || info.isFull) {
            if (mode.value != IslandMode.CHARGING) {
                mode.value = IslandMode.CHARGING
                showIsland()
            }
        } else {
            if (mode.value == IslandMode.CHARGING) {
                mode.value = if (musicInfo.value.isPlaying) IslandMode.MUSIC else IslandMode.IDLE
                scheduleHide(IDLE_HIDE_DELAY_MS)
            }
        }
    }

    // ==================== NAVIGATION ====================
    fun showNavigation(info: NavigationInfo) {
        navigationInfo.value = info
        if (mode.value == IslandMode.CALL_RINGING ||
            mode.value == IslandMode.CALL_ACTIVE) return
        mode.value = IslandMode.NAVIGATION
        showIsland()
    }

    fun dismissNavigation() {
        if (mode.value == IslandMode.NAVIGATION) {
            mode.value = if (musicInfo.value.isPlaying) IslandMode.MUSIC else IslandMode.IDLE
            scheduleHide(IDLE_HIDE_DELAY_MS)
        }
    }

    // ==================== ALARM ====================
    fun showAlarm(info: AlarmInfo) {
        alarmInfo.value = info
        isManuallyCollapsed.value = false
        mode.value = IslandMode.ALARM
        showIsland()
    }

    fun updateAlarmCountdown(minutesUntil: Int, time: String) {
        val current = alarmInfo.value
        alarmInfo.value = current.copy(
            minutesUntil = minutesUntil,
            time = time.ifEmpty { current.time },
            isRinging = minutesUntil <= 0
        )
    }

    fun dismissAlarm() {
        if (mode.value == IslandMode.ALARM) {
            mode.value = if (musicInfo.value.isPlaying) IslandMode.MUSIC else IslandMode.IDLE
            scheduleHide(IDLE_HIDE_DELAY_MS)
        }
    }

    fun clearAlarm() {
        if (mode.value == IslandMode.ALARM) {
            mode.value = if (musicInfo.value.isPlaying) IslandMode.MUSIC else IslandMode.IDLE
            scheduleHide(IDLE_HIDE_DELAY_MS)
        }
        alarmInfo.value = AlarmInfo()
    }

    // ==================== MANUAL COLLAPSE ====================
    fun toggleCollapse() {
        isManuallyCollapsed.value = !isManuallyCollapsed.value
        showIsland()
    }

    fun shouldShowCollapsed(): Boolean {
        return isManuallyCollapsed.value && mode.value == IslandMode.MUSIC
    }

    // ==================== RESET ====================
    fun reset() {
        mode.value = IslandMode.IDLE
        isManuallyCollapsed.value = false
        cancelChatDismissTimer()
        scheduleHide(IDLE_HIDE_DELAY_MS)
    }
}
