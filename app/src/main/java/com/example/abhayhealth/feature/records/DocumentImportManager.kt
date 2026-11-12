package com.example.abhayhealth.feature.records

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.abhayhealth.core.security.SecurityAudit
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

sealed interface DocumentImportResult {
    data class Success(
        val title: String,
        val category: String,
        val localFilePath: String,
        val mimeType: String,
        val recordDate: Long = System.currentTimeMillis()
    ) : DocumentImportResult

    data class Error(val message: String) : DocumentImportResult
}

object DocumentImportManager {

    const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L // 20 MiB

    val ALLOWED_MIME_TYPES = setOf(
        "application/pdf",
        "application/x-pdf",
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    fun importDocument(
        uri: Uri,
        context: Context
    ): DocumentImportResult {
        try {
            val contentResolver = context.contentResolver

            // 1. Determine MIME type
            var mimeType = contentResolver.getType(uri)?.lowercase() ?: ""
            val displayName = queryDisplayName(context, uri) ?: "Medical Report"
            val extension = getExtensionFromFileName(displayName).lowercase()

            if (mimeType.isBlank() || mimeType == "application/octet-stream" || !ALLOWED_MIME_TYPES.contains(mimeType)) {
                val derivedMime = when (extension) {
                    "pdf" -> "application/pdf"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> ""
                }
                if (derivedMime.isNotBlank()) {
                    mimeType = derivedMime
                }
            }

            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                return DocumentImportResult.Error("Unsupported file type. Please select a PDF, JPEG, PNG, or WEBP document.")
            }

            // 2. Query reported size if available
            val reportedSize = queryFileSize(context, uri)
            if (reportedSize != null && reportedSize > MAX_FILE_SIZE_BYTES) {
                return DocumentImportResult.Error("Document exceeds the 20 MiB size limit.")
            }

            // 3. Prepare app-private medical_records directory
            val recordsDir = File(context.filesDir, "medical_records")
            if (!recordsDir.exists()) {
                recordsDir.mkdirs()
            }

            // Unique filenames
            val fileExt = if (extension.isNotBlank()) ".$extension" else getExtensionFromMimeType(mimeType)
            val uniqueId = UUID.randomUUID().toString()
            val tempFile = File(recordsDir, "temp_$uniqueId.tmp")
            val finalFile = File(recordsDir, "rec_$uniqueId$fileExt")

            // Path traversal guard
            if (!finalFile.canonicalPath.startsWith(recordsDir.canonicalPath)) {
                return DocumentImportResult.Error("Invalid document destination path.")
            }

            // 4. Stream file write with byte-limit enforcement
            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return DocumentImportResult.Error("Unable to open selected file stream.")

            var totalBytesCopied = 0L
            val buffer = ByteArray(8192)

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytesCopied += bytesRead
                        if (totalBytesCopied > MAX_FILE_SIZE_BYTES) {
                            tempFile.delete()
                            return DocumentImportResult.Error("Document exceeds the 20 MiB size limit.")
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            // 5. Atomic move
            if (tempFile.exists()) {
                if (!tempFile.renameTo(finalFile)) {
                    // Fallback copy
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
            }

            val sanitizedTitle = sanitizeTitle(displayName)

            return DocumentImportResult.Success(
                title = sanitizedTitle,
                category = "Medical Report",
                localFilePath = finalFile.absolutePath,
                mimeType = mimeType,
                recordDate = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            SecurityAudit.logError("DocumentImportManager", "Failed to import document", e)
            return DocumentImportResult.Error(e.localizedMessage ?: "Failed to import document. Please try again.")
        }
    }

    fun openRecord(
        record: MedicalRecord,
        context: Context,
        currentAccountUid: String?
    ): String? {
        if (currentAccountUid.isNullOrEmpty() || record.accountUid != currentAccountUid) {
            return "Unauthorized access: Record belongs to another account."
        }

        val filePath = record.localFilePath
        if (filePath.isNullOrEmpty()) {
            return "Document file path is missing."
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            return "Document file not found on device."
        }

        try {
            val recordsDir = File(context.filesDir, "medical_records")

            // Path traversal guard
            if (!file.canonicalPath.startsWith(recordsDir.canonicalPath)) {
                return "Security warning: File path outside app-private directory."
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, record.mimeType ?: "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            return null
        } catch (_: ActivityNotFoundException) {
            return "No compatible application found to view this document type."
        } catch (e: Exception) {
            SecurityAudit.logError("DocumentImportManager", "Failed to open document", e)
            return e.localizedMessage ?: "Failed to open document."
        }
    }

    fun deleteFile(
        filePath: String?,
        context: Context,
        record: MedicalRecord? = null,
        currentAccountUid: String? = null
    ): Boolean {
        if (filePath.isNullOrEmpty()) return false
        if (record != null && (currentAccountUid.isNullOrEmpty() || record.accountUid != currentAccountUid)) {
            return false
        }
        try {
            val file = File(filePath)
            if (!file.exists()) return true

            val recordsDir = File(context.filesDir, "medical_records")

            // Ensure path is inside recordsDir
            if (!file.canonicalPath.startsWith(recordsDir.canonicalPath)) {
                return false
            }

            return file.delete()
        } catch (e: Exception) {
            SecurityAudit.logError("DocumentImportManager", "Failed to delete local document file", e)
            return false
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (_: Exception) {}
        return name
    }

    private fun queryFileSize(context: Context, uri: Uri): Long? {
        var size: Long? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1 && !cursor.isNull(index)) {
                        size = cursor.getLong(index)
                    }
                }
            }
        } catch (_: Exception) {}
        return size
    }

    private fun sanitizeTitle(displayName: String): String {
        val withoutExtension = displayName.substringBeforeLast(".")
        return withoutExtension.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().ifBlank { "Medical Report" }
    }

    private fun getExtensionFromFileName(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substringAfterLast(".").lowercase()
        } else {
            ""
        }
    }

    private fun getMimeTypeFromExtension(extension: String): String {
        return when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> ""
        }
    }

    private fun getExtensionFromMimeType(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "application/pdf" -> ".pdf"
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".dat"
        }
    }
}
