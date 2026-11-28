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

/** Prominent colors from the visible square cover and each outer edge's average. */
internal data class ArtworkEdgePalette(
    val swatches: List<Int>,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int,
)

/** Size-limited, off-main-thread local artwork loading with an embedded-art fallback. */
internal class AlbumArtworkLoader(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val pendingLimit = Semaphore(4)
    private val paletteLimit = Semaphore(2)
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val edgeColors = LruCache<String, ArtworkEdgePalette>(512)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    private fun sizeBucket(requestedPx: Int): Int = when {
        requestedPx <= 128 -> 128
        requestedPx <= 256 -> 256
        else -> 512
    }

    private fun cacheKey(song: Song, size: Int) = "${song.id}:${song.dateModified}:$size"

    /** A cache hit is safe on the caller thread and avoids a dispatcher hop per visible row. */
    fun cached(song: Song, requestedPx: Int): Bitmap? = cache.get(cacheKey(song, sizeBucket(requestedPx)))

    suspend fun load(song: Song, requestedPx: Int): Bitmap? {
        val size = sizeBucket(requestedPx)
        val key = cacheKey(song, size)
        cache.get(key)?.let { return it }
        if (missing.contains(key)) return null
        return withContext(Dispatchers.IO) {
            pendingLimit.withPermit {
                cache.get(key)?.let { return@withPermit it }
                if (missing.contains(key)) return@withPermit null
                val bitmap = thumbnail(song, size) ?: albumArt(song, size) ?: embeddedArt(song, size)
                if (bitmap == null) missing.add(key) else cache.put(key, bitmap)
                bitmap
            }
        }
    }

    /** Sample the actual square crop and its edges off the UI thread, once per cover. */
    suspend fun edgePalette(song: Song, bitmap: Bitmap): ArtworkEdgePalette? = withContext(Dispatchers.Default) {
        val key = "${song.id}:${song.dateModified}"
        edgeColors.get(key)?.let { return@withContext it }
        paletteLimit.withPermit {
            edgeColors.get(key)?.let { return@withPermit it }
            var software: Bitmap? = null
            var sample: Bitmap? = null
            val result = try {
                software = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
                val source = software ?: return@withPermit null
                // ContentScale.Crop displays the center square, not necessarily the full bitmap.
                val edge = minOf(source.width, source.height)
                val left = (source.width - edge) / 2
                val top = (source.height - edge) / 2
                val pixels = IntArray(64 * 64)
                // Full-cover sampling finds distinct prominent colors even when the
                // outer border is almost entirely black, white or transparent.
                for (y in 0..63) for (x in 0..63) {
                    pixels[y * 64 + x] = source.getPixel(left + x * (edge - 1) / 63, top + y * (edge - 1) / 63)
                }
                val red = LongArray(4)
                val green = LongArray(4)
                val blue = LongArray(4)
                val alpha = LongArray(4)
                // Four 64 x 16 strips from the outer 12% of the displayed cover.
                for (side in 0..3) for (depth in 0..15) for (along in 0..63) {
                    val p = along * (edge - 1) / 63
                    val d = (depth * (edge - 1) * 0.12f / 15f).toInt()
                    val x = left + when (side) { 0 -> p; 1 -> edge - 1 - d; 2 -> p; else -> d }
                    val y = top + when (side) { 0 -> d; 1 -> p; 2 -> edge - 1 - d; else -> p }
                    val pixel = source.getPixel(x, y)
                    val weight = android.graphics.Color.alpha(pixel).toLong()
                    alpha[side] += weight
                    red[side] += android.graphics.Color.red(pixel) * weight
                    green[side] += android.graphics.Color.green(pixel) * weight
                    blue[side] += android.graphics.Color.blue(pixel) * weight
                }
                val coverSample = Bitmap.createBitmap(pixels, 64, 64, Bitmap.Config.ARGB_8888)
                sample = coverSample
                val swatches = selectProminentCoverColors(
                    Palette.from(coverSample).clearFilters().maximumColorCount(24).generate().swatches
                        .map { CoverColorCandidate(it.rgb, it.population) },
                )
                if (swatches.isEmpty()) null else {
                    fun average(side: Int): Int = if (alpha[side] == 0L) swatches.first() else android.graphics.Color.rgb(
                        (red[side] / alpha[side]).toInt(), (green[side] / alpha[side]).toInt(),
                        (blue[side] / alpha[side]).toInt(),
                    )
                    ArtworkEdgePalette(swatches, average(0), average(1), average(2), average(3))
                }
            } catch (_: Exception) {
                null
            } finally {
                sample?.recycle()
                if (software != null && software !== bitmap) software?.recycle()
            }
            if (result != null) edgeColors.put(key, result)
            result
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
