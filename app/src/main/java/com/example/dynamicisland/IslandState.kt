package com.example.dynamicisland

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
    val avatarInitial: String = "B"
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

    // ==================== CALL ====================
    fun showIncomingCall(info: CallInfo = CallInfo()) {
        callInfo.value = info
        isManuallyCollapsed.value = false
        mode.value = IslandMode.CALL_RINGING
    }

    fun acceptCall() {
        mode.value = IslandMode.CALL_ACTIVE
    }

    fun endCall() {
        mode.value = IslandMode.IDLE
    }

    // ==================== MUSIC ====================
    fun showMusic() {
        isManuallyCollapsed.value = false
        mode.value = IslandMode.MUSIC
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
            isManuallyCollapsed.value = false
            if (mode.value == IslandMode.MUSIC) IslandMode.IDLE else mode.value
        }
    }

    // ==================== CHAT (WhatsApp dll) ====================
    fun showChat(info: ChatInfo) {
        // Jangan timpa panggilan
        if (mode.value == IslandMode.CALL_RINGING ||
            mode.value == IslandMode.CALL_ACTIVE) return

        chatInfo.value = info
        isManuallyCollapsed.value = false
        mode.value = IslandMode.CHAT

        // Auto-dismiss chat setelah 6 detik
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (mode.value == IslandMode.CHAT &&
                chatInfo.value.senderName == info.senderName) {
                dismissChat()
            }
        }, 6000)
    }

    fun dismissChat() {
        if (mode.value == IslandMode.CHAT) {
            mode.value = if (musicInfo.value.isPlaying && !musicInfo.value.isEmpty) {
                IslandMode.MUSIC
            } else {
                IslandMode.IDLE
            }
        }
    }

    // ==================== MANUAL COLLAPSE ====================
    fun toggleCollapse() {
        isManuallyCollapsed.value = !isManuallyCollapsed.value
    }

    fun shouldShowCollapsed(): Boolean {
        return isManuallyCollapsed.value && mode.value == IslandMode.MUSIC
    }

    // ==================== RESET ====================
    fun reset() {
        mode.value = IslandMode.IDLE
        isManuallyCollapsed.value = false
    }
}
