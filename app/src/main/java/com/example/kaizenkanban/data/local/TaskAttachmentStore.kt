package com.example.kaizenkanban.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Saves task photos under filesDir/attachments/{projectId}/{taskId}/.
 * DB stores paths relative to filesDir.
 */
class TaskAttachmentStore(context: Context) {
    private val appContext: Context = context.applicationContext
    private val filesDir: File = appContext.filesDir

    fun absoluteFile(relativePath: String): File = File(filesDir, relativePath)

    /**
     * Copies and compresses [uri] into app storage.
     * @return relative path under filesDir
     */
    fun saveImageFromUri(
        uri: Uri,
        projectId: String,
        taskId: String,
        attachmentId: String = UUID.randomUUID().toString()
    ): String {
        val dir = File(filesDir, "attachments/$projectId/$taskId").apply { mkdirs() }
        val outFile = File(dir, "$attachmentId.jpg")
        val original = appContext.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Cannot decode image")
        val scaled = scaleDown(original, MAX_SIDE)
        if (scaled !== original) original.recycle()
        FileOutputStream(outFile).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (!scaled.isRecycled) scaled.recycle()
        return "attachments/$projectId/$taskId/$attachmentId.jpg"
    }

    fun deleteRelative(relativePath: String) {
        val file = absoluteFile(relativePath)
        if (file.exists()) file.delete()
    }

    fun deleteTaskFolder(projectId: String, taskId: String) {
        val dir = File(filesDir, "attachments/$projectId/$taskId")
        if (dir.exists()) dir.deleteRecursively()
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
