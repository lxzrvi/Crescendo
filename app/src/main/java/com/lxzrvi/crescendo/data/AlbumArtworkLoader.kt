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
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** The most populous real swatches, in descending order of cover-pixel population. */
internal data class ArtworkPalette(val swatches: List<Int>) {
    val dominant: Int? get() = swatches.firstOrNull()
}

/** Size-limited, off-main-thread local artwork loading with an embedded-art fallback. */
internal class AlbumArtworkLoader(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val pendingLimit = Semaphore(4)
    private val paletteLimit = Semaphore(2)
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val colors = LruCache<String, ArtworkPalette>(512)
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

    /** Extract actual populous cover colors off the UI thread and reuse them across sections. */
    suspend fun palette(song: Song, bitmap: Bitmap): ArtworkPalette? = withContext(Dispatchers.Default) {
        val key = "${song.id}:${song.dateModified}"
        colors.get(key)?.let { return@withContext it }
        paletteLimit.withPermit {
            colors.get(key)?.let { return@withPermit it }
            var software: Bitmap? = null
            val result = try {
                // Palette reads pixels and cannot directly inspect a hardware bitmap.
                software = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false)
                else bitmap
                software?.let { source ->
                    val swatches = Palette.from(source).clearFilters().maximumColorCount(16).generate()
                        .swatches.sortedByDescending { it.population }.map { it.rgb }.distinct().take(5)
                    if (swatches.isEmpty()) null else ArtworkPalette(swatches)
                }
            } catch (_: Exception) {
                null
            } finally {
                if (software != null && software !== bitmap) software?.recycle()
            }
            if (result != null) colors.put(key, result)
            result
        }
    }

    suspend fun dominantColor(song: Song, bitmap: Bitmap): Int? = palette(song, bitmap)?.dominant

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
