package com.example.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.export.ReportExportManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveBackupManager {

    /**
     * Package name of Google Drive official Android app.
     */
    private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"

    /**
     * Creates an encrypted backup file ready to upload to Google Drive.
     */
    fun createDriveBackupFile(
        context: Context,
        jsonPayload: String,
        customPassword: String? = null
    ): File {
        return CryptoBackupManager.createEncryptedBackupFile(
            context = context,
            jsonPayload = jsonPayload,
            userPassword = customPassword
        )
    }

    /**
     * Launches the Google Drive app's "Upload to Drive" sheet directly.
     * If the Google Drive app is not installed or errors, safely falls back to standard chooser.
     */
    fun uploadFileToGoogleDriveApp(
        context: Context,
        file: File,
        subjectTitle: String = "Track My Rupee Encrypted Backup"
    ): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Direct intent targeting Google Drive upload activity
            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage(GOOGLE_DRIVE_PACKAGE)
            }

            // Verify if Google Drive package is installed and can handle the intent
            val resolveInfos = context.packageManager.queryIntentActivities(driveIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                driveIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(driveIntent)
                true
            } else {
                // Fallback to general file share/chooser
                ReportExportManager.shareFile(
                    context = context,
                    file = file,
                    mimeType = "application/octet-stream",
                    title = "Upload Backup to Google Drive"
                )
                true
            }
        } catch (e: Exception) {
            // General fallback
            try {
                ReportExportManager.shareFile(
                    context = context,
                    file = file,
                    mimeType = "application/octet-stream",
                    title = "Upload Backup to Google Drive"
                )
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Writes backup file content to a user-selected Google Drive document URI via Storage Access Framework (SAF).
     */
    fun writeBackupToSafUri(
        context: Context,
        targetUri: Uri,
        backupFile: File
    ): Pair<Boolean, String> {
        return try {
            val resolver = context.contentResolver
            resolver.openOutputStream(targetUri)?.use { outputStream ->
                backupFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Pair(false, "Failed to open output stream to Google Drive document")
            Pair(true, "Successfully saved backup to Google Drive!")
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Failed to write backup to Google Drive")
        }
    }

    /**
     * Generates standard suggested file name for Google Drive backups.
     */
    fun generateSuggestedDriveBackupFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "TrackMyRupee_Backup_$timeStamp.enc"
    }
}
