package com.example.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class FileLogManager(private val context: Context) {

    private val sentFile: File
        get() = File(context.filesDir, "sent.txt")

    private val errorLogFile: File
        get() = File(context.filesDir, "error.log")

    init {
        if (!sentFile.exists()) {
            sentFile.createNewFile()
        }
        if (!errorLogFile.exists()) {
            errorLogFile.createNewFile()
        }
    }

    /**
     * Reads all emails already present in sent.txt (trimmed, lowercase)
     */
    fun getSentEmails(): Set<String> {
        return try {
            if (!sentFile.exists()) return emptySet()
            sentFile.readLines()
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (e: Exception) {
            Log.e("FileLogManager", "Error reading sent.txt", e)
            emptySet()
        }
    }

    /**
     * Checks if email was already sent according to sent.txt
     */
    fun isAlreadySent(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return getSentEmails().contains(cleanEmail)
    }

    /**
     * Appends a successfully sent email to sent.txt
     */
    fun recordSentEmail(email: String) {
        try {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isNotEmpty()) {
                FileOutputStream(sentFile, true).use { stream ->
                    stream.write("$cleanEmail\n".toByteArray())
                }
            }
        } catch (e: Exception) {
            Log.e("FileLogManager", "Error recording to sent.txt", e)
        }
    }

    /**
     * Appends an error to error.log
     */
    fun recordError(email: String, errorMessage: String) {
        try {
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val logLine = "[$timestamp] [ERROR] [$email] -> $errorMessage\n"
            FileOutputStream(errorLogFile, true).use { stream ->
                stream.write(logLine.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("FileLogManager", "Error recording to error.log", e)
        }
    }

    /**
     * Reads sent.txt as raw text
     */
    fun readSentFileContent(): String {
        return try {
            if (sentFile.exists()) sentFile.readText() else ""
        } catch (e: Exception) {
            "خطأ أثناء قراءة ملف sent.txt: ${e.message}"
        }
    }

    /**
     * Reads error.log as raw text
     */
    fun readErrorLogContent(): String {
        return try {
            if (errorLogFile.exists()) errorLogFile.readText() else ""
        } catch (e: Exception) {
            "خطأ أثناء قراءة ملف error.log: ${e.message}"
        }
    }

    fun clearSentFile() {
        try {
            if (sentFile.exists()) {
                sentFile.writeText("")
            }
        } catch (e: Exception) {
            Log.e("FileLogManager", "Error clearing sent.txt", e)
        }
    }

    fun clearErrorLog() {
        try {
            if (errorLogFile.exists()) {
                errorLogFile.writeText("")
            }
        } catch (e: Exception) {
            Log.e("FileLogManager", "Error clearing error.log", e)
        }
    }
}
