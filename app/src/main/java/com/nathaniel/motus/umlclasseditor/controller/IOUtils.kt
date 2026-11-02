package com.nathaniel.motus.umlclasseditor.controller

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException

object IOUtils {

    fun saveFileToInternalStorage(data: String?, file: File?) {
        if (data == null || file == null) return
        try {
            // Kotlin maneja la apertura, escritura y cierre del archivo en una sola línea
            file.writeText(data)
        } catch (e: IOException) {
            Log.i("TEST", "Saving failed: ${e.message}")
        }
    }

    fun getFileFromInternalStorage(file: File): String {
        return try {
            if (file.exists()) {
                // readText() lee todo el contenido y cierra el stream automáticamente
                file.readText()
            } else {
                ""
            }
        } catch (e: IOException) {
            Log.i("TEST", "Loading failed: ${e.message}")
            ""
        }
    }

    fun saveFileToExternalStorage(context: Context, data: String, externalStorageUri: Uri) {
        try {
            // El bloque 'use' asegura que el stream se cierre solo al terminar o fallar
            context.contentResolver.openOutputStream(externalStorageUri)?.use { outputStream ->
                outputStream.write(data.toByteArray())
                Log.i("TEST", "Project saved")
            }
        } catch (e: IOException) {
            Log.i("TEST", "Failed saving project: ${e.message}")
        }
    }

    fun readFileFromExternalStorage(context: Context, externalStorageUri: Uri): String {
        return try {
            context.contentResolver.openInputStream(externalStorageUri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: ""
        } catch (e: IOException) {
            Log.i("TEST", "Failed loading project: ${e.message}")
            ""
        }
    }

    fun sortedFiles(file: File): ArrayList<String> {
        // Transformamos el array de archivos a una lista de nombres ordenados alfabéticamente en 2 líneas
        val files = file.listFiles() ?: return ArrayList()
        return ArrayList(files.map { it.name }.sorted())
    }

    fun readRawHtmlFile(context: Context, rawId: Int): String {
        return try {
            context.resources.openRawResource(rawId).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    // **********************************************************************************************
    // Side utilities
    // **********************************************************************************************
    @Suppress("DEPRECATION")
    fun getAppVersionCode(context: Context): Int {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }
}