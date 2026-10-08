package com.example.dynamicisland

import android.graphics.Bitmap

data class MusicInfo(
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val packageName: String = ""
) {
    val isEmpty: Boolean
        get() = title.isEmpty() && artist.isEmpty()
}
