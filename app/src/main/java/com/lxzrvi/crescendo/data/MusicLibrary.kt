package com.lxzrvi.crescendo.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** A local MediaStore item. No absolute path or uploaded copy is needed. */
internal data class Song(
    // Row IDs are only unique within a volume; retain both for SD cards.
    val id: String,
    val uri: Uri,
    val volumeName: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
    val dateModified: Long,
    // Android 11+ indexes genre; older providers may not expose it.
    val genre: String? = null,
) {
    // Limit accidental count transfers if MediaStore reuses a row ID.
    val historyKey: String get() = "$id:$dateAdded:$durationMs"
}

internal sealed interface MusicLibraryState {
    data object Loading : MusicLibraryState
    data class Ready(val songs: List<Song>) : MusicLibraryState
    data class Error(val message: String) : MusicLibraryState
}

/** Reload on resume or MediaStore changes; scanning never blocks the UI thread. */
@Composable
internal fun rememberMusicLibrary(): MusicLibraryState {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revision by remember(context) { mutableIntStateOf(0) }

    DisposableEffect(context, lifecycle) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { revision++ }
            override fun onChange(selfChange: Boolean, uri: Uri?) { revision++ }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val state by produceState<MusicLibraryState>(MusicLibraryState.Loading, context, revision) {
        try {
            // Coalesce the bursts emitted by the system media scanner.
            if (revision > 0) delay(180)
            val songs = withContext(Dispatchers.IO) { queryDeviceSongs(context) }
            value = MusicLibraryState.Ready(songs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            value = MusicLibraryState.Error("Music permission is needed to read your library.")
        } catch (_: Exception) {
            value = MusicLibraryState.Error("Couldn't load your music library. Try reopening the app.")
        }
    }
    return state
}

private fun queryDeviceSongs(context: Context): List<Song> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.ALBUM_ID)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.DATE_ADDED)
        add(MediaStore.Audio.Media.DATE_MODIFIED)
        add(MediaStore.Audio.Media.DISPLAY_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.VOLUME_NAME)
    }.toTypedArray()
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
    // Some vendor media providers omit optional columns even on newer Android.
    // Retry without genre rather than losing access to the entire library.
    val cursor = try {
        val withGenre = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            projection + MediaStore.Audio.Media.GENRE else projection
        context.contentResolver.query(collection, withGenre, selection, null, order)
    } catch (unsupported: Exception) {
        if (unsupported is SecurityException || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) throw unsupported
        context.contentResolver.query(collection, projection, selection, null, order)
    } ?: error("MediaStore returned no cursor")

    return cursor.use { rows ->
        val idColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val addedColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val modifiedColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val nameColumn = rows.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            rows.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
        val volumeColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            rows.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME) else -1
        buildList {
            while (rows.moveToNext()) {
                val rowId = rows.getLong(idColumn)
                val volume = if (volumeColumn >= 0) rows.getString(volumeColumn).orEmpty().ifBlank { "external" }
                else "external"
                val volumeCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volume != "external")
                    MediaStore.Audio.Media.getContentUri(volume) else collection
                val displayName = rows.getString(nameColumn).orEmpty().substringBeforeLast('.')
                add(
                    Song(
                        id = "$volume:$rowId",
                        uri = ContentUris.withAppendedId(volumeCollection, rowId),
                        volumeName = volume,
                        title = readableMetadata(rows.getString(titleColumn), displayName.ifBlank { "Unknown title" }),
                        artist = readableMetadata(rows.getString(artistColumn), "Unknown artist"),
                        album = readableMetadata(rows.getString(albumColumn), "Unknown album"),
                        albumId = if (rows.isNull(albumIdColumn)) -1 else rows.getLong(albumIdColumn),
                        durationMs = rows.getLong(durationColumn).coerceAtLeast(0L),
                        dateAdded = rows.getLong(addedColumn),
                        dateModified = rows.getLong(modifiedColumn),
                        genre = if (genreColumn >= 0) rows.getString(genreColumn)?.trim()
                            ?.takeIf { it.isNotEmpty() && it != MediaStore.UNKNOWN_STRING } else null,
                    ),
                )
            }
        }
    }
}

private fun readableMetadata(value: String?, fallback: String): String =
    value?.trim()?.takeIf { it.isNotEmpty() && it != MediaStore.UNKNOWN_STRING } ?: fallback

/** Play counts are private to Crescendo: Android doesn't expose other apps' listening history. */
internal class ListeningHistory(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("listening_history", Context.MODE_PRIVATE)

    fun counts(): Map<String, Int> = preferences.all.mapNotNull { (key, value) ->
        if (!key.startsWith("play_")) return@mapNotNull null
        val count = (value as? Int) ?: return@mapNotNull null
        key.removePrefix("play_") to count
    }.toMap()

    fun recentTimes(): Map<String, Long> = preferences.all.mapNotNull { (key, value) ->
        if (!key.startsWith("recent_")) return@mapNotNull null
        val playedAt = (value as? Long) ?: return@mapNotNull null
        key.removePrefix("recent_") to playedAt
    }.toMap()

    fun liked(): Set<String> = preferences.getStringSet("liked_songs", emptySet())?.toSet().orEmpty()

    fun toggleLiked(song: Song): Set<String> {
        val updated = liked().toMutableSet()
        if (!updated.add(song.historyKey)) updated.remove(song.historyKey)
        preferences.edit().putStringSet("liked_songs", updated.toSet()).apply()
        return updated
    }

    fun record(song: Song): Map<String, Int> {
        val key = "play_${song.historyKey}"
        val current = preferences.getInt(key, 0)
        preferences.edit()
            .putInt(key, if (current == Int.MAX_VALUE) current else current + 1)
            .putLong("recent_${song.historyKey}", System.currentTimeMillis())
            .apply()
        return counts()
    }
}
