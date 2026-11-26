package com.lxzrvi.crescendo.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Size-limited, off-main-thread local artwork loading with an embedded-art fallback. */
internal class AlbumArtworkLoader(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val pendingLimit = Semaphore(4)
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun load(song: Song, requestedPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val size = when {
            requestedPx <= 128 -> 128
            requestedPx <= 256 -> 256
            else -> 512
        }
        val key = "${song.id}:${song.dateModified}:$size"
        cache.get(key)?.let { return@withContext it }
        if (missing.contains(key)) return@withContext null

        pendingLimit.withPermit {
            cache.get(key)?.let { return@withPermit it }
            if (missing.contains(key)) return@withPermit null
            val bitmap = thumbnail(song, size) ?: albumArt(song, size) ?: embeddedArt(song, size)
            if (bitmap == null) missing.add(key) else cache.put(key, bitmap)
            bitmap
        }
    }

    private fun thumbnail(song: Song, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            resolver.loadThumbnail(song.uri, Size(size, size), null)
        } catch (_: Exception) {
            null
        }
    }

    private fun albumArt(song: Song, size: Int): Bitmap? {
        if (song.albumId < 0) return null
        val uri = ContentUris.withAppendedId(Uri.parse("content://media/${song.volumeName}/audio/albumart"), song.albumId)
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds, size) }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: Exception) {
            null
        }
    }

    private fun embeddedArt(song: Song, size: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, song.uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds, size) }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }

    private fun sampleSize(bounds: BitmapFactory.Options, requested: Int): Int {
        var sample = 1
        val shortest = minOf(bounds.outWidth, bounds.outHeight)
        while (shortest > 0 && shortest / (sample * 2) >= requested) sample *= 2
        return sample
    }
}
