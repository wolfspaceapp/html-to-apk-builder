package com.htmltoapk.builder.template

import android.content.Context
import android.util.Log
import com.htmltoapk.builder.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object TemplateManager {
    private const val TAG = "TemplateManager"
    private const val TEMPLATE_ASSET_NAME = "template.apk"
    private var instance: TemplateManager? = null

    @Volatile
    private var extractedTemplateDir: File? = null

    fun getInstance(context: Context): TemplateManager {
        return instance ?: synchronized(this) {
            instance ?: TemplateManager(context).also { instance = it }
        }
    }

    private constructor(context: Context) {
        extractTemplate(context)
    }

    private fun extractTemplate(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val templateDir = File(cacheDir, "template_apk")

            if (templateDir.exists()) {
                deleteRecursively(templateDir)
            }
            templateDir.mkdirs()

            context.assets.open(TEMPLATE_ASSET_NAME).use { input ->
                val zipInput = ZipInputStream(input)
                var entry: ZipEntry? = zipInput.nextEntry
                while (entry != null) {
                    val entryFile = File(templateDir, entry.name)
                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        FileOutputStream(entryFile).use { output ->
                            zipInput.copyTo(output)
                        }
                    }
                    entry = zipInput.nextEntry
                }
                zipInput.closeEntry()
            }

            extractedTemplateDir = templateDir
            Log.d(TAG, "Template extracted to: ${templateDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract template", e)
        }
    }

    fun getExtractedTemplateDir(): File? = extractedTemplateDir

    fun isTemplateAvailable(): Boolean = extractedTemplateDir?.exists() == true

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}