package com.example.dynamicisland

import androidx.compose.runtime.mutableStateOf

enum class IslandMode {
    IDLE,
    MUSIC,
    CALL_RINGING,
    CALL_ACTIVE
}

data class CallInfo(
    val name: String = "Budi Santoso",
    val number: String = "+62 812-3456-7890",
    val avatarInitial: String = "B"
)

object IslandState {
    val mode = mutableStateOf(IslandMode.IDLE)
    val callInfo = mutableStateOf(CallInfo())
    val musicInfo = mutableStateOf(MusicInfo())   // ⬅️ BARU

    fun showIncomingCall(info: CallInfo = CallInfo()) {
        callInfo.value = info
        mode.value = IslandMode.CALL_RINGING
    }

    fun acceptCall() {
        mode.value = IslandMode.CALL_ACTIVE
    }

    fun endCall() {
        mode.value = IslandMode.IDLE
    }

    fun showMusic() {
        mode.value = IslandMode.MUSIC
    }

    // ⬇️ BARU: update dari MediaListenerService
    fun updateMusic(info: MusicInfo) {
        musicInfo.value = info
        mode.value = if (info.isPlaying && !info.isEmpty) {
            // Jangan timpa mode call kalau sedang telepon
            if (mode.value == IslandMode.CALL_RINGING || mode.value == IslandMode.CALL_ACTIVE) {
                mode.value
            } else {
                IslandMode.MUSIC
            }
        } else {
            if (mode.value == IslandMode.MUSIC) IslandMode.IDLE else mode.value
        }
    }

    fun reset() {
        mode.value = IslandMode.IDLE
    }
}
