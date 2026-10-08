package com.example.dynamicisland

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf

enum class IslandMode {
    IDLE,
    MUSIC,
    CALL_RINGING,
    CALL_ACTIVE,
    CHAT
}

data class CallInfo(
    val name: String = "Budi Santoso",
    val number: String = "+62 812-3456-7890",
    val avatarInitial: String = "B",
    val packageName: String = ""   // ⬅️ BARU: source package (WA / dialer)
)

data class ChatInfo(
    val senderName: String = "",
    val message: String = "",
    val avatarInitial: String = "",
    val packageName: String = ""
)

object IslandState {
    val mode = mutableStateOf(IslandMode.IDLE)
    val callInfo = mutableStateOf(CallInfo())
    val musicInfo = mutableStateOf(MusicInfo())
    val chatInfo = mutableStateOf(ChatInfo())
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
                IslandMode.CHAT -> mode.value
                else -> IslandMode.MUSIC
            }
        } else {
            if (info.isEmpty) {
                isManuallyCollapsed.value = false
                if (mode.value == IslandMode.MUSIC) IslandMode.IDLE else mode.value
            } else {
                if (mode.value == IslandMode.CALL_RINGING ||
                    mode.value == IslandMode.CALL_ACTIVE ||
                    mode.value == IslandMode.CHAT) {
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
        // Jangan timpa call
        if (mode.value == IslandMode.CALL_RINGING ||
            mode.value == IslandMode.CALL_ACTIVE) return

        chatInfo.value = info
        isManuallyCollapsed.value = false
        mode.value = IslandMode.CHAT
        showIsland()

        // Auto-dismiss chat setelah 6 detik
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
