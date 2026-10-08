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
    val musicInfo = mutableStateOf(MusicInfo())

    // ⬇️ BARU: state manual collapse dari user tap
    val isManuallyCollapsed = mutableStateOf(false)

    fun showIncomingCall(info: CallInfo = CallInfo()) {
        callInfo.value = info
        isManuallyCollapsed.value = false   // panggilan selalu muncul
        mode.value = IslandMode.CALL_RINGING
    }

    fun acceptCall() {
        mode.value = IslandMode.CALL_ACTIVE
    }

    fun endCall() {
        mode.value = IslandMode.IDLE
    }

    fun showMusic() {
        isManuallyCollapsed.value = false
        mode.value = IslandMode.MUSIC
    }

    fun updateMusic(info: MusicInfo) {
        musicInfo.value = info
        mode.value = if (info.isPlaying && !info.isEmpty) {
            if (mode.value == IslandMode.CALL_RINGING || mode.value == IslandMode.CALL_ACTIVE) {
                mode.value
            } else {
                IslandMode.MUSIC
            }
        } else {
            // Lagu berhenti → reset manual collapse
            isManuallyCollapsed.value = false
            if (mode.value == IslandMode.MUSIC) IslandMode.IDLE else mode.value
        }
    }

    // ⬇️ BARU: dipanggil saat user tap island
    fun toggleCollapse() {
        isManuallyCollapsed.value = !isManuallyCollapsed.value
    }

    // ⬇️ BARU: helper untuk cek apakah island harus tampil kecil
    fun shouldShowCollapsed(): Boolean {
        return isManuallyCollapsed.value && mode.value == IslandMode.MUSIC
    }

    fun reset() {
        mode.value = IslandMode.IDLE
        isManuallyCollapsed.value = false
    }
}
