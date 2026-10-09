package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Member
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object FileExportHelper {

    private const val TAG = "FileExportHelper"

    /**
     * Saves file stream returned from API into user's device Downloads/Documents storage.
     * Compatible with Android 10+ (Scoped Storage via MediaStore) and legacy storage.
     * Returns the Uri of the saved file or null if saving failed.
     */
    fun saveApiFileToDevice(
        context: Context,
        inputStream: InputStream,
        suggestedFileName: String,
        mimeType: String = "text/csv"
    ): Uri? {
        val fileName = if (suggestedFileName.isBlank()) {
            "members_export_${System.currentTimeMillis()}.csv"
        } else suggestedFileName

        val cleanMimeType = if (mimeType.contains("excel") || fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        } else {
            "text/csv"
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, cleanMimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MemberOnDesk")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collectionUri, contentValues)

                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    Log.i(TAG, "File successfully saved to Downloads via MediaStore: $itemUri")
                    itemUri
                } else {
                    fallbackSaveToInternalCache(context, inputStream, fileName)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFolder = File(downloadsDir, "MemberOnDesk")
                if (!destFolder.exists()) destFolder.mkdirs()

                val destFile = File(destFolder, fileName)
                FileOutputStream(destFile).use { out ->
                    inputStream.copyTo(out)
                }
                Log.i(TAG, "File successfully saved to legacy Downloads path: ${destFile.absolutePath}")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: ${e.message}. Attempting fallback cache save.", e)
            fallbackSaveToInternalCache(context, inputStream, fileName)
        }
    }

    /**
     * Fallback to save into the app's accessible files directory with FileProvider URI.
     */
    private fun fallbackSaveToInternalCache(context: Context, inputStream: InputStream, fileName: String): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                inputStream.copyTo(out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed fallback save to cache: ${e.message}")
            null
        }
    }

    /**
     * Generates a standard CSV representation of local members.
     * Used when the remote API endpoint is not yet reachable or fails,
     * ensuring the user's export action succeeds immediately.
     */
    fun generateLocalMembersCsv(members: List<Member>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        // Header
        sb.append("Member ID,Member Code,Full Name,Phone,Email,Status,Plan,Registration Date,Address,Emergency Contact,Notes\n")

        for (m in members) {
            val regDate = sdf.format(Date(m.registrationDate))
            val safeName = escapeCsv(m.name)
            val safePhone = escapeCsv(m.phone)
            val safeEmail = escapeCsv(m.email)
            val safeAddress = escapeCsv(m.address)
            val safeEmergency = escapeCsv(m.emergencyContact)
            val safeNotes = escapeCsv(m.notes)
            val safePlan = escapeCsv(m.planName)

            sb.append("${m.id},${m.memberCode},$safeName,$safePhone,$safeEmail,${m.status.name},$safePlan,$regDate,$safeAddress,$safeEmergency,$safeNotes\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Opens a system viewer/chooser or share dialog for the exported file.
     */
    fun openOrShareFile(context: Context, uri: Uri, fileName: String) {
        try {
            val mime = if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            } else {
                "text/csv"
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Open or share exported file: $fileName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open/share file: ${e.message}")
        }
    }
}
