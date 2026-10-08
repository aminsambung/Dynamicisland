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

    fun reset() {
        mode.value = IslandMode.IDLE
    }
}