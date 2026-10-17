package com.example.kaizenkanban.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Saves task photos into the shared gallery (MediaStore) under Pictures/Kairos,
 * same as note photos. DB stores the content URI string in [relativePath].
 * Legacy rows may still hold a filesDir-relative path.
 */
class TaskAttachmentStore(context: Context) {
    private val appContext: Context = context.applicationContext
    private val filesDir: File = appContext.filesDir
    private val gallery = GalleryNotePhotoStore(appContext)

    fun absoluteFile(relativePath: String): File? {
        if (isContentUri(relativePath)) return null
        return File(filesDir, relativePath)
    }

    fun isContentUri(stored: String): Boolean =
        stored.startsWith("content://", ignoreCase = true)

    /**
     * Copies and compresses [uri] into MediaStore.
     * @return content URI string (stored in TaskAttachment.relativePath)
     */
    fun saveImageFromUri(
        uri: Uri,
        projectId: String,
        taskId: String,
        attachmentId: String = UUID.randomUUID().toString()
    ): String {
        return gallery.saveImageFromUri(uri, "task_${projectId.take(8)}_${taskId.take(8)}_$attachmentId")
    }

    fun deleteRelative(relativePath: String) {
        if (isContentUri(relativePath)) {
            gallery.deleteUri(relativePath)
            return
        }
        val file = File(filesDir, relativePath)
        if (file.exists()) file.delete()
    }

    fun deleteTaskFolder(projectId: String, taskId: String) {
        val dir = File(filesDir, "attachments/$projectId/$taskId")
        if (dir.exists()) dir.deleteRecursively()
    }
}
