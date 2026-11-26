@file:Suppress("DEPRECATION")

package com.lxzrvi.crescendo.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Copies the picker result privately; access does not depend on a persistent gallery grant. */
internal class BackgroundPhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "crescendo_background_photo")

    suspend fun importPhoto(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val temporary = File(appContext.filesDir, "crescendo_background_photo.pending")
        try {
            val input = appContext.contentResolver.openInputStream(uri) ?: return@withContext false
            input.use { source ->
                FileOutputStream(temporary).use { target ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 32L * 1024 * 1024) { "Image is too large" }
                        target.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not a supported image" }
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } catch (_: Exception) {
            temporary.delete()
            false
        }
    }

    fun remove() { file.delete() }

    fun load(): Bitmap? = try {
        if (!file.exists()) null else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
            if (decoded == null) null else {
                val orientation = try {
                    ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
                val transform = Matrix().apply {
                    when (orientation) {
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                        ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                        ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                        ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                        ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                    }
                }
                if (transform.isIdentity) decoded else {
                    val corrected = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true)
                    if (corrected !== decoded) decoded.recycle()
                    corrected
                }
            }
        }
    } catch (_: Exception) { null }

    /** Android 11 and older have no Compose RenderEffect blur; use a bounded background pass. */
    fun blurLegacy(bitmap: Bitmap, radius: Float): Bitmap {
        if (radius < 0.2f) return bitmap
        var resized: Bitmap? = null
        var result: Bitmap? = null
        var rs: RenderScript? = null
        var input: Allocation? = null
        var output: Allocation? = null
        var script: ScriptIntrinsicBlur? = null
        return try {
            val factor = (640f / max(bitmap.width, bitmap.height)).coerceAtMost(1f)
            resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt().coerceAtLeast(1), (bitmap.height * factor).toInt().coerceAtLeast(1), true)
            result = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
            rs = RenderScript.create(appContext)
            input = Allocation.createFromBitmap(rs, resized)
            output = Allocation.createTyped(rs, input.type)
            script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(radius.coerceIn(0.1f, 25f))
            script.setInput(input)
            script.forEach(output)
            output.copyTo(result)
            result
        } catch (_: Exception) {
            result?.recycle()
            bitmap
        } finally {
            script?.destroy()
            output?.destroy()
            input?.destroy()
            rs?.destroy()
            if (resized != null && resized !== bitmap) resized?.recycle()
        }
    }
}

@Composable
internal fun AppPhotoBackground(appearance: NavigationAppearance) {
    if (appearance.backgroundPhotoVersion <= 0L) return
    val context = LocalContext.current
    val store = remember(context) { BackgroundPhotoStore(context) }
    val bitmap by produceState<Bitmap?>(null, appearance.backgroundPhotoVersion) {
        value = null
        value = withContext(Dispatchers.IO) { store.load() }
    }
    // Debouncing only the CPU fallback keeps old devices responsive while dragging the slider.
    val settledBlur by produceState(appearance.photoBlur, appearance.photoBlur) {
        delay(140)
        value = appearance.photoBlur
    }
    val display by produceState(bitmap, bitmap, settledBlur) {
        value = bitmap
        val source = bitmap
        if (source != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && settledBlur >= 0.2f) {
            value = withContext(Dispatchers.Default) { store.blurLegacy(source, settledBlur) }
        }
    }
    val image = display ?: return
    Image(
        bitmap = image.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(appearance.photoBlur.dp) else Modifier,
        ),
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = appearance.photoDim)))
}
