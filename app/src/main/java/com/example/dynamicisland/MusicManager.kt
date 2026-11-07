package com.example.dynamicisland

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val duration: Long
)

object MusicManager {
    var player: ExoPlayer? = null
    var songs: List<Song> = emptyList()
    var currentIndex = 0
    var isPlaying = false

    fun initPlayer(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (songs.isNotEmpty()) {
                            val currentSong = songs[currentIndex]
                            // Pastikan IslandState dan MusicInfo ada di project Anda
                            IslandState.updateMusic(
                                MusicInfo(
                                    title = currentSong.title,
                                    artist = currentSong.artist,
                                    isPlaying = playing
                                )
                            )
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            playNext(context)
                        }
                    }
                })
            }
        }
    }

    fun scanMusic(context: Context): List<Song> {
        val list = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown Title"
                val artist = it.getString(artistCol) ?: "Unknown Artist"
                val duration = it.getLong(durationCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                list.add(Song(id, title, artist, uri, duration))
            }
        }
        songs = list
        return list
    }

    fun playSong(context: Context, index: Int) {
        if (index in songs.indices) {
            currentIndex = index
            val song = songs[index]
            
            player?.let { exo ->
                exo.setMediaItem(MediaItem.fromUri(song.uri))
                exo.prepare()
                exo.play()
            }
            
            IslandState.updateMusic(
                MusicInfo(
                    title = song.title,
                    artist = song.artist,
                    isPlaying = true
                )
            )
        }
    }

    fun playNext(context: Context) {
        if (songs.isEmpty()) return
        val nextIndex = (currentIndex + 1) % songs.size
        playSong(context, nextIndex)
    }

    fun playPrev(context: Context) {
        if (songs.isEmpty()) return
        val prevIndex = if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        playSong(context, prevIndex)
    }

    fun togglePlayPause(context: Context) {
        player?.let { exo ->
            if (exo.isPlaying) {
                exo.pause()
            } else {
                exo.play()
            }
        }
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        isPlaying = false
    }
}
