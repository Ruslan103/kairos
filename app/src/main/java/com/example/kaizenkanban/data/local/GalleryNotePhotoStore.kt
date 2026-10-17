package com.example.kaizenkanban.data.local

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.util.UUID

/**
 * Saves note photos into the shared gallery (MediaStore) under Pictures/Kairos.
 * DB stores the resulting content URI string.
 */
class GalleryNotePhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    /**
     * Copies and compresses [source] into MediaStore.
     * @return content URI string
     */
    fun saveImageFromUri(source: Uri, displayName: String = UUID.randomUUID().toString()): String {
        val original = resolver.openInputStream(source)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IOException("Cannot decode image")
        val scaled = scaleDown(original, MAX_SIDE)
        if (scaled !== original) original.recycle()

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Kairos")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Cannot create MediaStore row")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    throw IOException("JPEG compress failed")
                }
            } ?: throw IOException("Cannot open MediaStore output")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            if (!scaled.isRecycled) scaled.recycle()
        }
        return uri.toString()
    }

    fun deleteUri(mediaUri: String) {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull() ?: return
        runCatching { resolver.delete(uri, null, null) }
    }

    fun uriExists(mediaUri: String): Boolean {
        val uri = runCatching { Uri.parse(mediaUri) }.getOrNull() ?: return false
        return runCatching {
            resolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= maxSide) return src
        val scale = maxSide.toFloat() / longest
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    companion object {
        const val MAX_SIDE = 1920
        const val JPEG_QUALITY = 85
    }
}
