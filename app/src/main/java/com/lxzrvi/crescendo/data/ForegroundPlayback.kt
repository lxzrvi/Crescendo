package com.lxzrvi.crescendo.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Small foreground player for local songs. It pauses when the activity stops;
 * background service, queue and lock-screen controls are intentionally separate work.
 */
internal class ForegroundPlayback(
    context: Context,
    private val onPlayStarted: (Song) -> Unit,
) {
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private var pendingCountId: String? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            val song = currentSong
            if (playing && song != null && pendingCountId == song.id) {
                // Count once when audio actually starts, not on selection or resume.
                pendingCountId = null
                onPlayStarted(song)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            isLoading = playbackState == Player.STATE_BUFFERING
        }

        override fun onPlayerError(error: PlaybackException) {
            pendingCountId = null
            isLoading = false
            errorMessage = "Can't play this file on this device."
        }
    }

    init { player.addListener(listener) }

    fun select(song: Song) {
        if (currentSong?.id == song.id) {
            toggle()
            return
        }
        pendingCountId = null
        player.stop()
        player.clearMediaItems()
        currentSong = song
        errorMessage = null
        pendingCountId = song.id
        player.setMediaItem(
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build(),
                )
                .build(),
        )
        player.prepare()
        player.play()
    }

    fun toggle() {
        val song = currentSong ?: return
        errorMessage = null
        when {
            player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING) -> player.pause()
            player.playbackState == Player.STATE_ENDED -> {
                pendingCountId = song.id
                player.seekTo(0)
                player.play()
            }
            player.playbackState == Player.STATE_IDLE -> {
                pendingCountId = song.id
                player.prepare()
                player.play()
            }
            else -> player.play()
        }
    }

    /** Next follows the actual alphabetically sorted device library, wrapping at the end. */
    fun next(songs: List<Song>) {
        if (songs.isEmpty()) return
        val index = songs.indexOfFirst { it.id == currentSong?.id }
        val next = songs[(index + 1).coerceAtLeast(0) % songs.size]
        if (currentSong?.id != next.id) {
            select(next)
        } else {
            // With a one-song library, Next restarts rather than merely toggling pause.
            errorMessage = null
            player.pause()
            pendingCountId = next.id
            player.seekTo(0)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun pauseForBackground() { player.pause() }

    fun close() {
        pendingCountId = null
        player.stop()
        player.clearMediaItems()
        currentSong = null
        isPlaying = false
        isLoading = false
        errorMessage = null
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
